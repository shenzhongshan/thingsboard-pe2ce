#!/usr/bin/env python3
"""
判定一次 thingsboard-ce 发布该走哪条升级路径，并检查版本号是否与之匹配。

背景：CE 有两条升级路径（见 feedback_two_upgrade_paths）。
  - SystemPatchApplier：普通启动时自动跑，条件是 major.minor 相同且 maintenance 或 patch 变大。
    它能跑 upgrade/lts/schema_update.sql、刷视图、更新系统 widget/图片/邮件模板。
  - ThingsboardInstallService（upgrade.sh）：离线跑，额外能做规则节点配置迁移和 Java 数据迁移。

危险在于：热补丁路径**做不到**规则节点迁移和 Java 数据迁移，却照样推进 schema_version。
于是"改了 @RuleNode(version) 但只 bump 了第 4 段"的发布会这样失败：
换 jar 重启 -> 热补丁跑完推进版本 -> 之后想补跑离线升级被 "already upgraded" 拒绝
-> 规则节点永久停在旧 configurationVersion，**全程零报错**。
要强制走离线升级，只能 bump minor（maintenance 变大仍然会被热补丁接走）。

用法:
  python docs/tools/release_upgrade_path_check.py [仓库根] [基线 git ref]

两个参数都可省：仓库根默认按本脚本位置上推两级（docs/tools/ -> 仓库根），
基线默认取最近一个 v* tag。基线一般用上一次发布的 tag。
退出码 0 = 可以发，1 = 有阻塞项。

规则与取舍见 docs/architecture/04-upgrade-and-release.md 第 4 节。
"""
import os
import re
import subprocess
import sys

# Windows 控制台默认 GBK，中文输出会乱码
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

RULE_NODE_BLOCK_RE = re.compile(r'@RuleNode\((.*?)\n\)', re.S)
VERSION_IN_BLOCK_RE = re.compile(r'^\s*version\s*=\s*(\d+)\s*,?\s*$', re.M)
POM_VERSION_RE = re.compile(r'<version>([^<]+)</version>')
WHITELIST_BLOCK_RE = re.compile(r'SUPPORTED_VERSIONS_FOR_UPGRADE\s*=\s*Map\.of\((.*?)\);', re.S)
WHITELIST_KEY_RE = re.compile(r'"([\d.]+)"\s*,')
# updateData() 的函数体；只有 log/注释/空行时视为"没有 Java 数据迁移"
UPDATE_DATA_RE = re.compile(r'public void updateData\(\)[^{]*\{(.*?)\n    \}', re.S)

SCHEMA_SETTINGS = 'application/src/main/java/org/thingsboard/server/service/install/DefaultDatabaseSchemaSettingsService.java'
DATA_UPDATE = 'application/src/main/java/org/thingsboard/server/service/install/update/DefaultDataUpdateService.java'
BASIC_SQL = 'application/src/main/data/upgrade/basic/schema_update.sql'
LTS_SQL = 'application/src/main/data/upgrade/lts/schema_update.sql'
ENTITIES_SQL = 'dao/src/main/resources/sql/schema-entities.sql'

ADD_COLUMN_RE = re.compile(
    r'ALTER\s+TABLE\s+(?:IF\s+EXISTS\s+)?(\w+)\s+ADD\s+COLUMN\s+IF\s+NOT\s+EXISTS\s+(\w+)([^;]*);', re.I)


def add_columns(path):
    """{(表, 列): {声明文本, ...}}。只认 ADD COLUMN IF NOT EXISTS —— 别的 ALTER 形状
    （加约束、改 identity 属性）没法可靠地按相等性比对，留给人工。"""
    if not os.path.exists(path):
        return {}
    text = open(path, encoding='utf-8', errors='replace').read()
    # 必须先剥掉 -- 行注释，否则注释里举例用的 DDL 会被当成真语句。
    # 第一版没剥，结果 schema-entities.sql 头部那段讲约定的注释把自己报成了违规。
    text = re.sub(r'--[^\n]*', '', text)
    out = {}
    for t, c, rest in ADD_COLUMN_RE.findall(text):
        out.setdefault((t.lower(), c.lower()), set()).add(re.sub(r'\s+', ' ', rest).strip().lower())
    return out


def git(root, *args):
    out = subprocess.run(['git', '-C', root] + list(args),
                         capture_output=True, text=True, encoding='utf-8', errors='replace')
    if out.returncode != 0:
        return None
    return out.stdout


def at_ref(root, ref, relpath):
    """基线侧的文件内容；文件在该 ref 不存在时返回 None。"""
    return git(root, 'show', '{}:{}'.format(ref, relpath))


def rule_node_versions(text):
    """一个 .java 源文件里 @RuleNode 块声明的 version。没写 version 的节点不进结果 ——
    它们不参与 TbNode.upgrade() 迁移，不该触发离线升级。"""
    found = {}
    for idx, block in enumerate(RULE_NODE_BLOCK_RE.findall(text or '')):
        m = VERSION_IN_BLOCK_RE.search(block)
        if m:
            found[idx] = int(m.group(1))
    return found


def normalize_version(raw):
    digits = re.sub(r'[^\d.]', '', raw or '')
    parts = [p for p in digits.split('.') if p != '']
    parts += ['0'] * (4 - len(parts))
    return tuple(int(p) for p in parts[:4])


def collect_rule_node_bumps(root, baseline):
    """返回 [(relpath, old, new)]，只收**已存在节点的 version 变大**。
    新增节点不算：rule_node 表里还没有它的行，没有东西需要迁移。"""
    bumps = []
    new_nodes = []
    changed = git(root, 'diff', '--name-only', baseline)
    if changed is None:
        return None, None
    for rel in [l.strip() for l in changed.splitlines() if l.strip().endswith('.java')]:
        abspath = os.path.join(root, rel)
        if not os.path.exists(abspath):
            continue
        head_text = open(abspath, encoding='utf-8', errors='replace').read()
        if '@RuleNode(' not in head_text:
            continue
        head_versions = rule_node_versions(head_text)
        if not head_versions:
            continue
        base_text = at_ref(root, baseline, rel)
        if base_text is None:
            new_nodes.append(rel)
            continue
        base_versions = rule_node_versions(base_text)
        for idx, new_v in head_versions.items():
            old_v = base_versions.get(idx)
            if old_v is not None and new_v != old_v:
                bumps.append((rel, old_v, new_v))
    return bumps, new_nodes


def has_java_data_migration(root):
    text = open(os.path.join(root, DATA_UPDATE), encoding='utf-8', errors='replace').read()
    m = UPDATE_DATA_RE.search(text)
    if not m:
        return None  # 签名变了，脚本需要跟着改，不要静默放过
    body = []
    for line in m.group(1).splitlines():
        s = line.strip()
        if not s or s.startswith('//') or s.startswith('*') or s.startswith('/*'):
            continue
        if s.startswith('log.'):
            continue
        body.append(s)
    return body


def whitelist_keys(root):
    text = open(os.path.join(root, SCHEMA_SETTINGS), encoding='utf-8', errors='replace').read()
    m = WHITELIST_BLOCK_RE.search(text)
    if not m:
        return None
    return WHITELIST_KEY_RE.findall(m.group(1))


def main(root, baseline):
    root = os.path.abspath(root)
    if git(root, 'rev-parse', '--git-dir') is None:
        print('FAIL  {} 不是 git 仓库'.format(root))
        return 1
    if git(root, 'rev-parse', '--verify', baseline) is None:
        print('FAIL  基线 ref 不存在: {}'.format(baseline))
        return 1

    problems = []
    notes = []

    # --- 版本号分类 ---
    head_pom = open(os.path.join(root, 'pom.xml'), encoding='utf-8', errors='replace').read()
    base_pom = at_ref(root, baseline, 'pom.xml')
    head_v = normalize_version(POM_VERSION_RE.search(head_pom).group(1))
    base_v = normalize_version(POM_VERSION_RE.search(base_pom).group(1)) if base_pom else None
    print('版本     基线 {} -> 当前 {}'.format(
        '.'.join(map(str, base_v)) if base_v else '?', '.'.join(map(str, head_v))))

    if base_v is None:
        problems.append('读不到基线的 pom 版本')
        same_family = False
    else:
        # 热补丁接走的条件：major.minor 相同（maintenance 变大也会被它接走）
        same_family = head_v[0] == base_v[0] and head_v[1] == base_v[1]
        if head_v == base_v:
            problems.append('版本号未变更：发布必须 bump 版本，否则两条升级路径都不会触发')
        elif head_v < base_v:
            problems.append('版本号变小了：{} < {}'.format(head_v, base_v))

    path_label = '热补丁（换 jar 重启自动）' if same_family else '离线升级（upgrade.sh）'
    print('路径     major.minor {} -> 这次发布会走：{}'.format(
        '未变' if same_family else '变了', path_label))

    # --- 检查 1：规则节点 version ---
    bumps, new_nodes = collect_rule_node_bumps(root, baseline)
    if bumps is None:
        problems.append('git diff 失败，无法比对规则节点 version')
    else:
        if bumps:
            print('规则节点 {} 个节点的 version 变化：'.format(len(bumps)))
            for rel, old_v, new_v in bumps:
                print('           {} : {} -> {}'.format(os.path.basename(rel), old_v, new_v))
            if same_family:
                problems.append(
                    '改了 {} 处 @RuleNode(version) 但 major.minor 未变 —— 热补丁路径不做规则节点迁移，'
                    '却会推进 schema_version，之后离线升级会被 "already upgraded" 拒绝。请 bump minor。'
                    .format(len(bumps)))
        else:
            print('规则节点 无 version 变化')
        if new_nodes:
            notes.append('{} 个新增的带 version 节点（不触发迁移，rule_node 表里还没有它们的行）: {}'
                         .format(len(new_nodes), ', '.join(os.path.basename(n) for n in new_nodes)))

    # --- 检查 2：Java 数据迁移 ---
    body = has_java_data_migration(root)
    if body is None:
        problems.append('认不出 DefaultDataUpdateService.updateData() 的函数体，脚本需要同步更新')
    elif body:
        print('数据迁移 updateData() 有 {} 行实际语句'.format(len(body)))
        if same_family:
            problems.append(
                'updateData() 里有 Java 数据迁移但 major.minor 未变 —— 热补丁路径根本不调用它。请 bump minor。')
    else:
        print('数据迁移 updateData() 为空')

    # --- 检查 3：basic 改了但 lts 没改 ---
    changed = git(root, 'diff', '--name-only', baseline) or ''
    changed_set = set(l.strip() for l in changed.splitlines())
    if BASIC_SQL in changed_set and LTS_SQL not in changed_set:
        msg = 'basic/schema_update.sql 改了但 lts/schema_update.sql 没改 —— 热补丁路径只跑 lts，这些 DDL 到不了同族升级的库'
        if same_family:
            problems.append(msg)
        else:
            notes.append(msg + '（本次是跨 minor，basic 会跑，但下一个同族补丁就漏了）')
    elif BASIC_SQL in changed_set:
        print('增量 SQL  basic 与 lts 都已更新')

    # --- 检查 4：列变更是否在 lts 里（与基线无关的常设检查）---
    # schema-entities.sql 跑在"新装 + 离线升级"，upgrade/basic 只跑离线升级，
    # 两者 SystemPatchApplier 都不执行。所以任何只写在它们里面的列，
    # 同族补丁发布（换 jar 重启）的库永远拿不到。
    lts_cols = add_columns(os.path.join(root, LTS_SQL))
    basic_cols = add_columns(os.path.join(root, BASIC_SQL))
    entities_cols = add_columns(os.path.join(root, ENTITIES_SQL))

    # 4a. schema-entities.sql 不该出现 ADD COLUMN：新装由 CREATE TABLE 负责，存量库由 lts 负责。
    if entities_cols:
        print('DDL 约定 schema-entities.sql 里有 {} 个 ADD COLUMN：'.format(len(entities_cols)))
        for t, c in sorted(entities_cols):
            print('           {}.{}'.format(t, c))
        problems.append(
            'schema-entities.sql 里的 ADD COLUMN 在新装时是 no-op（CREATE TABLE 已建好列），'
            '对存量库又到不了热补丁路径。请把列写进对应的 CREATE TABLE 块，升级增量放 upgrade/lts。')
    else:
        print('DDL 约定 schema-entities.sql 只有 CREATE TABLE，无 ADD COLUMN')

    # 4b. 列变更必须能被热补丁路径送达，也就是必须在 lts 里。
    gap = sorted(set(basic_cols) - set(lts_cols))
    if gap:
        print('DDL 覆盖 {} 个列变更不在 lts 里：'.format(len(gap)))
        for t, c in gap:
            print('           {}.{}  （只在 upgrade/basic）'.format(t, c))
        problems.append(
            '{} 个列变更到不了热补丁路径。同族补丁发布的库会缺这些列，而且是静默的 —— '
            '补进 upgrade/lts/schema_update.sql。'.format(len(gap)))
    else:
        print('DDL 覆盖 basic 的列变更都在 lts 里')

    # 4c. basic 保留着与 lts 重复的条目（刻意：少动上游文件）。重复本身无害，
    #     两份声明**不一致**才有害 —— 那才是真正会咬人的失败模式。
    diverged = []
    for key, basic_decls in sorted(basic_cols.items()):
        lts_decls = lts_cols.get(key)
        if lts_decls and not basic_decls <= lts_decls:
            diverged.append((key, sorted(basic_decls), sorted(lts_decls)))
    if diverged:
        print('DDL 一致性 {} 个列在 basic 与 lts 里声明不同：'.format(len(diverged)))
        for (t, c), b, l in diverged:
            print('           {}.{}  basic={}  lts={}'.format(t, c, b, l))
        problems.append(
            '{} 个列在 basic 与 lts 里声明不一致。两个文件在离线升级路径上都会跑，'
            '先跑的那个定型，另一个因 IF NOT EXISTS 静默失效 —— 改一处就要改两处。'.format(len(diverged)))
    else:
        print('DDL 一致性 basic 与 lts 的重复声明逐字相同')

    # --- 检查 5：白名单覆盖 ---
    keys = whitelist_keys(root)
    if keys is None:
        problems.append('读不到 SUPPORTED_VERSIONS_FOR_UPGRADE')
    elif base_v is not None and not same_family:
        base_family = '{}.{}.{}'.format(*base_v[:3])
        if base_family in keys:
            print('白名单   {} 在 SUPPORTED_VERSIONS_FOR_UPGRADE 内'.format(base_family))
        else:
            problems.append(
                '离线升级的源版本族 {} 不在白名单 {} 内，validateSchemaSettings() 会直接拒绝'
                .format(base_family, keys))
    elif keys is not None:
        head_family = '{}.{}.{}'.format(*head_v[:3])
        if head_family in keys:
            print('白名单   {} 在白名单内（同族离线升级可行）'.format(head_family))
        else:
            notes.append('当前族 {} 不在白名单 {} 内：同族的离线升级会被拒绝，'
                         '一旦将来某个补丁需要规则节点迁移就无路可走'.format(head_family, keys))

    print('')
    for n in notes:
        print('NOTE  ' + n)
    if problems:
        for p in problems:
            print('FAIL  ' + p)
        return 1
    print('OK    版本号与升级路径一致')
    return 0


def default_root():
    """本脚本位于 <仓库根>/docs/tools/，所以上推两级就是仓库根。
    不要改成按目录名查找——仓库目录名是可以被重命名的。"""
    return os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def default_baseline(root):
    """最近一个 v* tag。本仓库目前只有上游 tag（v2.5.x / v3.x），还没有自己的
    发布 tag，所以这里很可能解析不到可用基线，届时需要显式传 ref。"""
    out = git(root, 'describe', '--tags', '--abbrev=0', '--match', 'v*')
    return out.strip() if out else None


if __name__ == '__main__':
    argv_root = sys.argv[1] if len(sys.argv) > 1 else default_root()
    if len(sys.argv) > 2:
        argv_baseline = sys.argv[2]
    else:
        argv_baseline = default_baseline(argv_root)
        if not argv_baseline:
            print('找不到可用的 v* tag 作为基线，请显式传入基线 ref。')
            print(__doc__)
            sys.exit(2)
        print('基线     未指定，取最近的 tag: {}'.format(argv_baseline))
    sys.exit(main(argv_root, argv_baseline))
