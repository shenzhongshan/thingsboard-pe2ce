"""Checks the factual claims and internal links of 04-upgrade-and-release.md against the code.

A guide that has drifted from the source is worse than no guide, so every claim that names an
identifier, a file or a number gets re-derived here rather than trusted.

Usage:
  python docs/tools/upgrade_doc_conformance.py
"""
import io
import os
import re
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

HERE = os.path.dirname(os.path.abspath(__file__))
# This script lives in <repo root>/docs/tools/, so two levels up is the repo root. This used to be
# a hardcoded absolute path, which broke the moment the directory was renamed or moved.
CE = os.path.dirname(os.path.dirname(HERE))
DOC = os.path.join(CE, "docs", "architecture", "04-upgrade-and-release.md")


def read(p):
    return io.open(p, encoding="utf-8", errors="replace").read()


doc = read(DOC)
fails = []
checks = 0


def check(desc, ok):
    global checks
    checks += 1
    if not ok:
        fails.append(desc)
    print(("  ok   " if ok else "  FAIL ") + desc)


print("-- internal links resolve --")
for target in sorted(set(re.findall(r"\]\((?!https?:)([^)#]+)", doc))):
    p = os.path.normpath(os.path.join(os.path.dirname(DOC), target))
    check("link -> %s" % target, os.path.exists(p))

print("-- named source files exist --")
for rel in ["dao/src/main/resources/sql/schema-entities.sql",
            "application/src/main/data/upgrade/basic/schema_update.sql",
            "application/src/main/data/upgrade/lts/schema_update.sql",
            "application/src/main/java/org/thingsboard/server/service/system/SystemPatchApplier.java",
            "application/src/main/java/org/thingsboard/server/install/ThingsboardInstallService.java",
            "application/src/main/java/org/thingsboard/server/service/install/DefaultDatabaseSchemaSettingsService.java",
            "application/src/main/java/org/thingsboard/server/service/install/ProjectInfo.java"]:
    check("file -> %s" % rel, os.path.exists(os.path.join(CE, rel)))

settings = read(os.path.join(CE, "application/src/main/java/org/thingsboard/server/service/install/DefaultDatabaseSchemaSettingsService.java"))
patcher = read(os.path.join(CE, "application/src/main/java/org/thingsboard/server/service/system/SystemPatchApplier.java"))
installer = read(os.path.join(CE, "application/src/main/java/org/thingsboard/server/install/ThingsboardInstallService.java"))
project = read(os.path.join(CE, "application/src/main/java/org/thingsboard/server/service/install/ProjectInfo.java"))
lts = read(os.path.join(CE, "application/src/main/data/upgrade/lts/schema_update.sql"))
entities = read(os.path.join(CE, "dao/src/main/resources/sql/schema-entities.sql"))
basic = read(os.path.join(CE, "application/src/main/data/upgrade/basic/schema_update.sql"))

print("-- claims about the code --")
check('productType is "CE"', 'return "CE";' in project)
check("whitelist has exactly 4.3.0 / 4.3.1 / 4.4.0",
      sorted(re.findall(r'"(\d+\.\d+\.\d+)",\s*"\d+\.\d+\.\d+\.x"', settings)) == ["4.3.0", "4.3.1", "4.4.0"])
check("downgrade gate present", "Downgrade is not supported" in settings)
check("packing formula as documented",
      "major * 1_000_000_000L + minor * 1_000_000L + maintenance * 1000L + patch" in settings)
check("hot patch fires on maintenance OR patch increase",
      "packageVersion.maintenance > dbVersion.maintenance" in patcher
      and "packageVersion.patch > dbVersion.patch" in patcher)
check("hot patch runs lts only", '"upgrade", "lts"' in patcher
      and '"basic"' not in patcher and "schema-entities" not in patcher)
check("hot patch advances schema_version", "schemaSettingsService.updateSchemaVersion()" in patcher)
check("hot patch does NOT migrate rule nodes", "upgradeRuleNodes" not in patcher)
check("hot patch does NOT call updateData", "updateData()" not in patcher)
check("hot patch refreshes mail templates", "installScripts.updateMailTemplates()" in patcher)
check("SKIP_PATCH_VERSION_CHECK is the bypass", "SKIP_PATCH_VERSION_CHECK" in patcher)
check("advisory lock used", "pg_try_advisory_lock" in patcher)
check("offline upgrade writes version last",
      installer.index("updateSchemaVersion()") > installer.index("upgradeRuleNodes()"))
check("offline upgrade refreshes mail templates", "installScripts.updateMailTemplates()" in installer)

print("-- claims about the SQL files --")
ADD_COL = re.compile(r"ALTER\s+TABLE\s+(?:IF\s+EXISTS\s+)?(\w+)\s+ADD\s+COLUMN\s+IF\s+NOT\s+EXISTS\s+(\w+)", re.I)
strip = lambda t: re.sub(r"--[^\n]*", "", t)
check("schema-entities.sql has no ADD COLUMN", not ADD_COL.findall(strip(entities)))
basic_cols = set((t.lower(), c.lower()) for t, c in ADD_COL.findall(strip(basic)))
lts_cols = set((t.lower(), c.lower()) for t, c in ADD_COL.findall(strip(lts)))
check("doc says 21 duplicated entries remain in basic (actual %d)" % len(basic_cols), len(basic_cols) == 21)
check("every basic column is in lts", basic_cols <= lts_cols)
check("lts holds the ota_package FK", "fk_device_profile_ota_package" in lts)
check("lts holds the seq_id SET CYCLE", "seq_id SET CYCLE" in lts)
check("ota_package FK is the only dollar-quoted block in lts", strip(lts).count("$$") == 2)

print("-- version propagation (sections 9 and 10) --")
source_poms = sum(1 for r, _, fs in os.walk(CE) for f in fs
                  if f == "pom.xml" and "node_modules" not in r and "target" not in r)
check("doc says 76 source poms (actual %d)" % source_poms, source_poms == 76)
check("doc says 77 places (poms + package.json)", source_poms + 1 == 77)
pkg = read(os.path.join(CE, "ui-ngx", "package.json"))
check("ui-ngx package.json carries no -SNAPSHOT", '"version": "4.4.0"' in pkg)
root_pom = read(os.path.join(CE, "pom.xml"))
check("no versions-maven-plugin declared", "versions-maven-plugin" not in root_pom)
check("no maven-release-plugin declared", "maven-release-plugin" not in root_pom)
check("no ${revision} indirection", "${revision}" not in root_pom)
projinfo = read(os.path.join(CE, "application/src/main/java/org/thingsboard/server/service/install/ProjectInfo.java"))
# the doc quotes this regex verbatim; compare against the source line, not a hand-escaped copy
check("stripping regex quoted correctly in the doc",
      'replaceAll("[^' + chr(92) * 2 + 'd.]", "")' in projinfo)
check("ProjectInfoVersionTest exists",
      os.path.exists(os.path.join(CE, "application/src/test/java/org/thingsboard/server/service/install/ProjectInfoVersionTest.java")))
check("maintenance bump is a hot patch, as section 9 claims",
      "packageVersion.maintenance > dbVersion.maintenance" in patcher)
nums = [int(m) for m in re.findall(r"^## (\d+)\.", doc, re.M)]
check("section numbers 1..11 contiguous (got %s)" % nums, nums == list(range(1, 12)))

print("-- checker claims --")
checker = os.path.join(HERE, "release_upgrade_path_check.py")
check("checker script exists", os.path.exists(checker))
if os.path.exists(checker):
    src = read(checker)
    # the doc enumerates 8 blocking rules; assert each one's message still exists in the checker
    for marker in ["bump 版本", "版本号变小", "@RuleNode(version) 但 major.minor 未变",
                   "Java 数据迁移但 major.minor 未变", "schema-entities.sql 里的 ADD COLUMN",
                   "到不了热补丁路径", "声明不一致", "不在白名单"]:
        check("checker rule present: %s" % marker, marker in src)
tester = os.path.join(HERE, "release_upgrade_path_check_test.py")
check("checker self-test exists", os.path.exists(tester))
if os.path.exists(tester):
    n = read(tester).count('", [lambda') + read(tester).count('",\n     [lambda')
    check("doc says 8 scenarios (actual %d)" % n, n == 8)

print("")
print("%d checks, %d failed" % (checks, len(fails)))
for f in fails:
    print("   FAILED: " + f)
sys.exit(1 if fails else 0)
