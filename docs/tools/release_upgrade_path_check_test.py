#!/usr/bin/env python3
"""
release_upgrade_path_check.py 的自测：逐个触发它的每条判定分支。

重点不只是"该报错时报错"，更是"不该报错时闭嘴" —— 场景 3 专门用来抓
"检查器被写死成永远 FAIL"这种假阳性。

读写文件一律带 newline=''。第一版没带，把被测文件从 CRLF 悄悄改成了 LF，
git status 里凭空多出 4 个"已修改"文件（见 feedback_copy_to_stdout_not_a_snapshot
里同族的转义/换行教训）。

注意它会临时改写工作树里的真实文件（pom、规则节点、两个 schema_update.sql 等），
跑完在 finally 里还原。工作树有未提交改动时跑它是安全的，但别在它运行期间
并发编辑这些文件。

用法:
  python docs/tools/release_upgrade_path_check_test.py
"""
import io
import os
import subprocess
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

HERE = os.path.dirname(os.path.abspath(__file__))
# 本脚本位于 <仓库根>/docs/tools/，上推两级即仓库根。不要按目录名定位——仓库可以被改名。
CE = os.path.dirname(os.path.dirname(HERE))
CHECKER = os.path.join(HERE, "release_upgrade_path_check.py")
# 基线只需是个有历史的 ref：场景全靠改写工作树构造，与基线的具体内容无关。
BASELINE = os.environ.get("RELEASE_CHECK_TEST_BASELINE", "HEAD~5")

POM = os.path.join(CE, "pom.xml")
RULE_NODE = os.path.join(CE, "rule-engine", "rule-engine-components", "src", "main", "java",
                         "org", "thingsboard", "rule", "engine", "rest", "TbRestApiCallNode.java")
DATA_UPDATE = os.path.join(CE, "application", "src", "main", "java", "org", "thingsboard",
                           "server", "service", "install", "update", "DefaultDataUpdateService.java")
BASIC_SQL = os.path.join(CE, "application", "src", "main", "data", "upgrade", "basic", "schema_update.sql")
ENTITIES_SQL = os.path.join(CE, "dao", "src", "main", "resources", "sql", "schema-entities.sql")
LTS_SQL = os.path.join(CE, "application", "src", "main", "data", "upgrade", "lts", "schema_update.sql")
LTS_REL = "application/src/main/data/upgrade/lts/schema_update.sql"
SCHEMA_SETTINGS = os.path.join(CE, "application", "src", "main", "java", "org", "thingsboard",
                               "server", "service", "install", "DefaultDatabaseSchemaSettingsService.java")

TOUCHED = [POM, RULE_NODE, DATA_UPDATE, BASIC_SQL, LTS_SQL, ENTITIES_SQL, SCHEMA_SETTINGS]


def read(p):
    return io.open(p, encoding="utf-8", errors="replace", newline="").read()


def write(p, t):
    io.open(p, "w", encoding="utf-8", newline="").write(t)


def set_pom_version(v):
    t = read(POM)
    old = "<version>4.4.0-SNAPSHOT</version>"
    assert old in t, "pom version anchor missing"
    write(POM, t.replace(old, "<version>%s</version>" % v, 1))


def bump_rule_node():
    t = read(RULE_NODE)
    for anchor in ("\r\n        version = 4,\r\n", "\n        version = 4,\n"):
        if anchor in t:
            write(RULE_NODE, t.replace(anchor, anchor.replace("= 4,", "= 5,"), 1))
            return
    raise AssertionError("rule node version anchor missing")


def add_data_migration():
    t = read(DATA_UPDATE)
    for anchor in ("//TODO: should be cleaned after each release\r\n",
                   "//TODO: should be cleaned after each release\n"):
        if anchor in t:
            injected = anchor + "        someService.migrateEverything();" + anchor[-2:] \
                if anchor.endswith("\r\n") else anchor + "        someService.migrateEverything();\n"
            write(DATA_UPDATE, t.replace(anchor, injected, 1))
            return
    raise AssertionError("updateData anchor missing")


def touch_basic_sql():
    """场景 5 断言的是"basic 改了、lts 没改"，所以必须把 lts 压回基线内容 ——
    否则工作树里任何真实的 lts 改动都会让这个场景静默失效（实测踩过）。"""
    out = subprocess.run(['git', '-C', CE, 'show', '%s:%s' % (BASELINE, LTS_REL)],
                         capture_output=True, text=True, encoding='utf-8', errors='replace')
    assert out.returncode == 0, "cannot read baseline lts/schema_update.sql"
    write(LTS_SQL, out.stdout)
    write(BASIC_SQL, read(BASIC_SQL) + "\n-- release check probe\n")


def add_alter_to_entities():
    """违反约定：往 schema-entities.sql 尾部塞一条 ADD COLUMN。"""
    write(ENTITIES_SQL, read(ENTITIES_SQL)
          + "\nALTER TABLE customer ADD COLUMN IF NOT EXISTS probe_col uuid;\n")


def diverge_basic_from_lts():
    """违反一致性：把 basic 里某列的类型改掉，使其与 lts 不符。"""
    t = read(BASIC_SQL)
    old = "ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS notes varchar(1000000);"
    assert old in t, "basic rule_chain.notes anchor missing"
    write(BASIC_SQL, t.replace(old, "ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS notes varchar(255);", 1))


def drop_whitelist_entry():
    t = read(SCHEMA_SETTINGS)
    for anchor in (',\r\n            "4.4.0", "4.4.0.x"', ',\n            "4.4.0", "4.4.0.x"'):
        if anchor in t:
            write(SCHEMA_SETTINGS, t.replace(anchor, "", 1))
            return
    raise AssertionError("whitelist anchor missing")


def run():
    out = subprocess.run([sys.executable, CHECKER, CE, BASELINE],
                         capture_output=True, text=True, encoding="utf-8", errors="replace")
    return out.returncode, (out.stdout or "") + (out.stderr or "")


SCENARIOS = [
    ("1 clean patch release", [lambda: set_pom_version("4.4.0.101")], 0, "OK"),
    ("2 patch release + rule node bump", [lambda: set_pom_version("4.4.0.101"), bump_rule_node],
     1, "@RuleNode(version)"),
    ("3 minor release + rule node bump (must NOT fire)",
     [lambda: set_pom_version("4.5.0.0"), bump_rule_node], 0, "OK"),
    ("4 patch release + java data migration",
     [lambda: set_pom_version("4.4.0.101"), add_data_migration], 1, "Java 数据迁移"),
    ("5 patch release + basic sql only",
     [lambda: set_pom_version("4.4.0.101"), touch_basic_sql], 1, "lts/schema_update.sql 没改"),
    ("6 minor release + whitelist missing predecessor",
     [lambda: set_pom_version("4.5.0.0"), drop_whitelist_entry], 1, "不在白名单"),
    ("7 ADD COLUMN smuggled into schema-entities.sql",
     [lambda: set_pom_version("4.4.0.101"), add_alter_to_entities], 1, "schema-entities.sql"),
    ("8 basic and lts declare a column differently",
     [lambda: set_pom_version("4.4.0.101"), diverge_basic_from_lts], 1, "声明不一致"),
]


def main():
    backup = {p: read(p) for p in TOUCHED}
    results = []
    try:
        for name, setup, want_rc, want_text in SCENARIOS:
            for p, t in backup.items():
                write(p, t)
            for fn in setup:
                fn()
            rc, out = run()
            ok = (rc == want_rc) and (want_text in out)
            results.append((ok, name, rc, want_rc, want_text, out))
    finally:
        for p, t in backup.items():
            write(p, t)

    passed = 0
    for ok, name, rc, want_rc, want_text, out in results:
        print(("PASS  " if ok else "FAIL  ") + name)
        print("        exit=%d (want %d), looking for %r" % (rc, want_rc, want_text))
        if not ok:
            for line in out.strip().splitlines():
                print("        | " + line)
        passed += 1 if ok else 0
    print("")
    print("%d/%d scenarios behaved as specified" % (passed, len(results)))
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
