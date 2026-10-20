# Versioning & Upgrade Guide

How this fork numbers its releases, which upgrade path each release takes, and what each path can
and cannot do. **Read this before bumping a version or adding a database change.**

Everything below was measured against the code in this repository, not inherited from upstream
documentation. Where a claim is easy to get wrong, the source file and line are named so you can
re-check instead of trusting this page. For the raw schema mechanics (table inventory, packed
version format, timeseries layout) see [SQL Schema & Migration](20-dao/sql-schema.md).

---

## 1. Position

This is a CE-based fork with most PE features ported in. Two consequences shape everything here:

- **Version evolution keeps the upstream CE skeleton.** No bespoke migration chain, no custom
  `product` marker. `ProjectInfo.getProductType()` returns `"CE"`.
- **There is no CE-to-PE conversion path.** `upgradeDatabase()` takes no `fromCe` flag, and none of
  PE's entity-group backfill machinery exists here. Do not port it.

## 2. Version numbers

Four segments: `major.minor.maintenance.patch`. Stored in `tb_schema_settings.schema_version` as a
single bigint:

```
major * 1_000_000_000 + minor * 1_000_000 + maintenance * 1_000 + patch
```

So `4.4.0.101` is stored as `4004000101`. A legacy three-segment encoding
(`major * 1e6 + minor * 1e3 + maintenance`) is still decoded for databases older than 4.2.1.1 — see
`DefaultDatabaseSchemaSettingsService.getDbSchemaVersion()`.

**The number is monotonic and load-bearing.** Lowering a baseline makes the migration interval
`(db version, package version]` empty, which silently skips migrations rather than failing. A
downgrade is rejected explicitly (section 5).

## 3. The two upgrade paths

| | Hot patch | Offline upgrade |
|---|---|---|
| Entry point | `SystemPatchApplier` (`@PostConstruct`, background thread) | `ThingsboardInstallService` via `upgrade.sh` / `upgrade.bat` |
| Trigger | Normal startup, when the package version increased within the same `major.minor` | Explicit operator action, separate JVM that exits when done |
| Downtime | None — swap the jar and restart | Yes — service must be stopped |
| Cluster safety | `pg_try_advisory_lock`, one node wins | Operator runs it once |

`SystemPatchApplier.isVersionIncreased()` fires when `major` and `minor` are unchanged **and either
`maintenance` or `patch` grew**. That is the trap: bumping `maintenance` does *not* force the offline
path. **Only a `minor` bump does.**

### What each path can actually do

Measured, not assumed. `DefaultDataUpdateService` and `DefaultCacheCleanupService` are both
`@Profile("install")`, so those beans do not exist in a running server at all; `updateData()` and
`upgradeRuleNodes()` have exactly one call site each, in `ThingsboardInstallService`.

| Capability | Hot patch | Offline upgrade |
|---|---|---|
| Create tables / indexes | yes, via `upgrade/lts/schema_update.sql` | yes |
| Alter columns, run DML | yes, same file | yes |
| Rebuild views (`schema-views.sql`) | yes | yes |
| System widget types / bundles / images | yes (creates missing widget types too) | yes |
| System mail templates | yes | yes |
| **Rule node config migration** (`TbNode.upgrade()`) | **no** | yes |
| **Java-level data migration** (`updateData()`) | **no** | yes |
| Cache flush | no | yes |
| Advances `schema_version` | **yes** | yes |

The last two rows combined are the failure mode worth memorising:

> A release that changes `@RuleNode(version)` but only bumps the fourth segment will be picked up by
> the hot patch, which **cannot** migrate rule node configs yet **does** advance `schema_version`.
> The offline upgrade afterwards is then refused as *"already upgraded"*. Rule nodes stay on their
> old `configurationVersion` forever, and nothing logs an error.

## 4. Release decision rule

| This release contains | Version bump | Upgrade method |
|---|---|---|
| Only idempotent DDL, views, widgets, system resources, DML | fourth segment — `4.4.0.N` | swap jar, restart |
| Any `@RuleNode(version)` change, or anything in `updateData()` | **`minor`** — `4.5.0.0` | `upgrade.sh`, and whitelist the previous family |

Do not decide this from memory. Run the checker:

```bash
python _tooling/release_upgrade_path_check.py thingsboard-ce <baseline-ref>
```

It diffs the baseline against the **working tree** — uncommitted changes included, so "forgot to
commit" cannot slip through. Exit code 0 means the version number matches what the change actually
needs. The blocking rules:

| # | Rule | Why it blocks |
|---|---|---|
| 1 | Version must change | Neither upgrade path triggers otherwise |
| 2 | Version must not decrease | Migrations would run backwards |
| 3 | `@RuleNode(version)` changed ⇒ `minor` bump required | Hot patch cannot migrate rule node configs (section 3) |
| 4 | Non-empty `updateData()` ⇒ `minor` bump required | Hot patch never calls it |
| 5 | No `ADD COLUMN` in `schema-entities.sql` | No-op on fresh install, unreachable on hot patch |
| 6 | Every `basic` column also in `lts` | Otherwise missing on jar-swap upgrades |
| 7 | `basic` and `lts` declarations identical | Whichever runs first wins, the other is silently voided |
| 8 | Predecessor family whitelisted on a `minor` bump | `validateSchemaSettings()` would refuse the upgrade |

It also fails loudly rather than passing silently if it cannot read the baseline `pom.xml`, run
`git diff`, recognise the `updateData()` body, or parse `SUPPORTED_VERSIONS_FOR_UPGRADE` — a check
that cannot evaluate its input must not report success.

The script lives in the surrounding `pe_refactor/_tooling/` directory, which is not a git
repository, so it is not versioned with this source tree. It ships with
`release_upgrade_path_check_test.py` (8 scenarios, including a false-positive control that must
*not* fire).

## 5. Upgrade gates

`DefaultDatabaseSchemaSettingsService.validateSchemaSettings()`, in order:

1. `SKIP_SCHEMA_VERSION_CHECK=true` bypasses everything below.
2. `product` in the database must equal `ProjectInfo.getProductType()` (`"CE"`).
3. The database must not already be at the package version — otherwise *"already upgraded"*.
4. **The database version must not exceed the package version** — downgrades are rejected.
5. The database version prefix must appear in `SUPPORTED_VERSIONS_FOR_UPGRADE`.

```java
private static final Map<String, String> SUPPORTED_VERSIONS_FOR_UPGRADE = Map.of(
        "4.3.0", "4.3.0.x",
        "4.3.1", "4.3.1.x",
        "4.4.0", "4.4.0.x"   // same-family entry, see below
);
```

The keys are `major.minor.maintenance` prefixes matched with `startsWith` against the four-segment
database version.

**Why the current family is listed.** Without `4.4.0`, an offline upgrade *between two patch
releases of the current family* is impossible — exactly the recovery you need when a release turns
out to require rule node migration. **Why gate 4 exists.** That same entry would otherwise let
`4.4.0.5 -> 4.4.0.2` pass every other check and run the migrations backwards; nothing else in that
class compares version magnitudes.

**Release checklist item:** the first time you bump `minor` to `4.5.0.0`, add `"4.4.0"` as the
predecessor entry. Rule 7 of the checker catches a missing predecessor.

## 6. Where a database change goes

Three files can hold DDL, and they do **not** reach the same places:

| File | Fresh install | Offline upgrade | Hot patch |
|---|---|---|---|
| `dao/src/main/resources/sql/schema-entities.sql` | yes | yes | **no** |
| `application/src/main/data/upgrade/basic/schema_update.sql` | no | yes | **no** |
| `application/src/main/data/upgrade/lts/schema_update.sql` | no | yes | yes |

### The convention

**Every column is declared exactly twice, once per path:**

1. In its `CREATE TABLE` block in `schema-entities.sql` — serves fresh installs.
2. In `upgrade/lts/schema_update.sql` — serves existing databases on *both* upgrade paths.

Do **not** append `ALTER TABLE x ADD COLUMN IF NOT EXISTS y` to the tail of `schema-entities.sql`.
On a fresh install it is a no-op (the `CREATE TABLE` already made the column), and for an existing
database it never reaches the hot patch. It is the worst of both. The checker rejects it.

Do **not** add new column changes to `upgrade/basic`. It only runs on the offline upgrade, and `lts`
runs there too. The 21 duplicated entries still in `basic` are deliberately left alone — it is an
upstream file and deleting lines there creates merge conflicts, while the duplication itself is
harmless because `lts` runs after it on the same path. The checker enforces that the two agree
textually, which is the failure mode that actually bites: if they disagree, whichever runs first
wins and the other is silently voided by its own `IF NOT EXISTS`.

Everything in `lts` must be **idempotent** — it re-runs on every version increase within the family.
Use `IF NOT EXISTS`, `DROP ... IF EXISTS` before `ADD`, and `WHERE` guards on DML so an operator's
edits are not clobbered.

### Two statements that cannot follow the convention

Both stay in `schema-entities.sql` and are mirrored in `lts`:

- `ota_package`'s `fk_device_profile_ota_package`. `device_profile` and `ota_package` reference each
  other, so the constraint can only be added after both tables exist. It is wrapped in a
  `DO $$ ... IF NOT EXISTS ... $$` block. Note this is the only dollar-quoted block in `lts`, and
  the whole file is executed as one `jdbcTemplate.execute()` — pgJDBC must not treat the semicolons
  inside the block as statement separators. Verified by a live patch run.
- `edge_event.seq_id SET CYCLE`. Changes an existing identity column's sequence options rather than
  adding a column.

## 7. Failure modes to watch for

All of these are **silent** — they produce no error, which is what makes them expensive.

| Symptom | Cause |
|---|---|
| Rule nodes stuck on an old `configurationVersion` | Rule node version changed, only the fourth segment bumped (section 3) |
| A column missing only on jar-swap-upgraded databases | DDL landed in `schema-entities.sql` or `basic` but not `lts` |
| A column has different types on fresh vs upgraded databases | `basic` and `lts` declarations drifted apart |
| A new mail template never appears for existing tenants | Would happen if `updateMailTemplates()` were wired into only one of the two paths |
| EDQS answers queries for a newly resident entity type from an empty state | Would happen without the `syncedTypes` accounting in `DefaultEdqsService` |
| The entire hot patch is skipped | An exception in `applyPatchIfNeeded()` is caught and logged by `init()`; grep startup logs for `Failed to apply system data patch updates` and confirm `System data patch update completed successfully` is present |

The last row is the one to check first when a patch release "did nothing".

## 8. Verifying an upgrade change

Patterns that earned their place during this work:

- **Prove DDL equivalence with two scratch databases.** Apply the old and new `schema-entities.sql`
  to separate empty databases and diff a full `information_schema.columns` fingerprint (table,
  column, type, length, precision, nullability, default). Removing 19 redundant `ALTER`s was
  confirmed safe this way: 829 columns, identical line for line.
- **Checksum before restoring, not after.** Snapshot per-row `md5(column)` before touching data and
  `diff` after. When reconstructing a value offline, compute its checksum and compare to the
  baseline *before* writing it back. Note `COPY ... TO STDOUT` is escaped text — it is not a
  round-trippable snapshot.
- **Exercise the hot patch without a version bump** using `SKIP_PATCH_VERSION_CHECK=true`. Much
  narrower than a full offline upgrade: no rule node rewrite, no `updateData()`.
- **Add a negative control.** A check that only ever fires is indistinguishable from a check that is
  hard-wired to fire. For the release checker that is scenario 3 (minor bump plus a rule node
  change must *not* fail).
- **Mutate to prove tests are not vacuous.** Break the logic deliberately and confirm precisely the
  expected cases fail.

## 9. Related documents

- [SQL Schema & Migration](20-dao/sql-schema.md) — table inventory, packed version format, index and
  function files
- [Build System](01-build-system.md) — Maven reactor, packaging, where `upgrade.sh` comes from
- [Deployment Modes](02-deployment-modes.md) — the `install` profile and deployment topologies
