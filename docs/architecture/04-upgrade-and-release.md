# Versioning & Upgrade Guide

How this fork numbers its releases, which upgrade path each release takes, and what each path can
and cannot do. **Read this before bumping a version or adding a database change.**

> **The Chinese version [04-upgrade-and-release.zh-CN.md](04-upgrade-and-release.zh-CN.md) is
> authoritative.** This English copy exists for comparison against upstream CE. Land changes in the
> Chinese version first, then mirror them here. `docs/tools/upgrade_doc_conformance.py` validates
> the Chinese copy; `docs/tools/doc_translation_parity.py` checks that the two stay structurally
> aligned.

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
python docs/tools/release_upgrade_path_check.py [repo-root] [baseline-ref]
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

Both arguments are optional: the repo root defaults to two levels above the script, and the
baseline to the most recent `v*` tag. The repository has no release tags of its own yet, so for now
pass the baseline explicitly.

The script lives in [`docs/tools/`](../tools/README.md) alongside its self-test and
`upgrade_doc_conformance.py`, which re-derives every factual claim on this page. See
[docs/tools/README.md](../tools/README.md) for invocation details, exit codes, and the warning that
the self-test rewrites working-tree files.

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

## 9. Release lines and branches

### What the upgrade machinery forces

A branching model cannot be chosen freely here. Four properties of the code constrain it:

1. **`schema_version` is global and monotonic, and downgrades are rejected** (section 5). A database
   at `4.4.0.105` can never be served by a `4.4.0.103` build. Parallel release lines must therefore
   occupy **disjoint version ranges** — you cannot interleave them.
2. **Inside one `major.minor`, every release takes the hot-patch path** (section 3). A maintenance
   branch can only ship what the hot patch can do: no rule node config migration, no `updateData()`.
   A fix needing either of those cannot be released inside its own line.
3. **A `maintenance` bump is also a hot patch** (`SystemPatchApplier.isVersionIncreased()`). So
   `4.4.0.5 -> 4.4.1.0` does *not* start a new line, it looks like a patch. **Only `minor`
   identifies a line.**
4. **`lts/schema_update.sql` is cumulative and family-scoped.** Append-only files conflict on every
   cross-branch merge, so the merge direction has to be fixed by convention, not improvised.

### The model

```
major . minor . maintenance . patch
        ^^^^^   ^^^^^^^^^^^   ^^^^^
        line    frozen at 0   the only counter inside a line
```

| Branch | Lifetime | Purpose |
|---|---|---|
| `mydev-pe` | long-lived | development, where work lands first |
| `master` | long-lived | integration; `mydev-pe` merges here |
| `release/4.4` | long-lived | the 4.4 line — ships `4.4.0.101`, `.102`, ... |
| `release/4.5` | long-lived | cut from `master` when `minor` bumps |

Tag every release `v<full-version>` (e.g. `v4.4.0.101`) on its release branch. Tags matter more than
the branch: a release branch keeps moving as hotfixes land, so only a tag can take you back to
exactly what was shipped. The repository currently has 35 upstream tags and **zero** of its own, so
this is new discipline, not an existing one.

Prefer `release/<major>.<minor>` over the date-stamped `arelease-pro-<date>` branch that exists
today: a date cannot tell you which line it belongs to, and with multiple lines alive that is the
question you actually need answered.

### Fix propagation: always merge forward

Apply a fix to the **oldest line that needs it**, then merge forward:

```
release/4.4  ──┐
               ├─→ release/4.5 ──→ master ──→ mydev-pe
hotfix lands ──┘
```

Never cherry-pick a fix independently into several lines. Two independent cherry-picks of the same
change to `lts/schema_update.sql` produce two textually different append-only files that will
conflict forever and may drift in declaration (the failure mode rule 7 of the checker exists to
catch — see section 4).

### Per-line obligations

Each release branch carries its **own copy** of these, and they are not interchangeable:

| Item | Why it is per-line |
|---|---|
| `SUPPORTED_VERSIONS_FOR_UPGRADE` | `release/4.5` must whitelist `"4.4.0"` as its predecessor; `release/4.4` must not list `4.5` |
| `upgrade/basic/schema_update.sql` | On `release/4.5` this is the 4.4→4.5 delta; on `release/4.4` it is the 4.3→4.4 delta |
| `upgrade/lts/schema_update.sql` | Cumulative within that line only |

When cutting `release/4.5` from `master`: add `"4.4.0"` to the whitelist, and decide what `basic`
should contain for the new line. Checker rule 8 fails if the predecessor entry is missing.

### Customer customization branches

"Customer" here means the **delivery target** -- not the same sense as the "customer" running the
system in the next subsection, which is about upgrade paths between released versions. This
subsection is about how to branch for a specific customer's deliverable. The customer version suffix
format is in section 10.

Customer customization runs on a line parallel to the release branches, not a part of `release/*`:

- Named `customer/<customer-id>`, cut from the **release tag** it customizes (e.g. `v4.4.0.101`),
  **never from the HEAD of `release/*` or `master`** -- otherwise the customization branch quietly
  picks up changes on that line that have not shipped and the customer never accepted.
- When building a deliverable, change the pom version to the customer-suffixed form from section 10
  (e.g. `4.4.0.101-ACME.loginA`) and tag it `v4.4.0.101-ACME.loginA`.
- Changes on a customer branch fall into two kinds that merge in opposite directions: **general bug
  fixes** follow the "fix propagation" rule above and merge forward into the mainline; **customer-only
  customization** is **never merged into any release line** -- it stays on the customer branch, because
  it exists only for that one customer, and landing it on the mainline would saddle every other
  customer with a feature flag they never asked for.
- A customer branch does not maintain its own `SUPPORTED_VERSIONS_FOR_UPGRADE` whitelist or
  `upgrade/lts/schema_update.sql`: it inherits the entire upgrade machinery of the release line it was
  cut from. Because the customer suffix never enters `schema_version` (section 10), the database
  upgrade gates decide purely from the base version number -- they cannot tell, and do not need to
  tell, whether a build came off a customer branch.

### What a customer can and cannot do

| Move | Possible? |
|---|---|
| `4.4.0.101 -> 4.4.0.105` | yes, hot patch — swap the jar and restart |
| `4.4.0.105 -> 4.5.0.0` | yes, offline upgrade — `4.4.0` must be whitelisted on the 4.5 build |
| `4.5.0.0 -> 4.4.0.106` | **no** — rejected by the downgrade gate, and there is no supported path back |
| `4.4.0.101 -> 4.6.0.0` skipping 4.5 | only if `release/4.6` whitelists `4.4.0` as well |

That third row is the one to tell operators about before they upgrade: **moving a database to a new
line is one-way.** Snapshot the database first.

## 10. Finalising a version

The version is **not** centrally defined. It is repeated in **77 places**:

- 76 `pom.xml` files — the root `<version>` plus each module's `<parent><version>`, which Maven
  requires verbatim (there is no `${revision}` indirection configured). That is every source pom in
  the tree; a naive `grep` also hits a 77th copy under `msa/web-ui/target/`, which is a build
  artifact, not a file to edit.
- `ui-ngx/package.json` — currently `"4.4.0"`, note **without** the `-SNAPSHOT` suffix the poms carry

Neither `versions-maven-plugin` nor `maven-release-plugin` is declared in the build, so the bump is
either 77 manual edits or a plugin invoked by coordinates:

```bash
# all 76 poms at once; then edit ui-ngx/package.json by hand
mvn -q versions:set -DnewVersion=4.4.0.101 -DgenerateBackupPoms=false
```

### The qualifier carries no version information

`ProjectInfo.getProjectVersion()` strips every character that is not a digit or a dot, so:

| Maven version | Resulting schema version |
|---|---|
| `4.4.0.102-SNAPSHOT` | `4.4.0.102` |
| `4.4.0-SNAPSHOT` | `4.4.0.0` |
| `4.3.1.1-PE-SNAPSHOT` | `4.3.1.1` |

**Dropping `-SNAPSHOT` is therefore not a release.** Shipping `4.4.0-SNAPSHOT` and then `4.4.0`
is numerically the same version: no upgrade path triggers, and the offline path would refuse it as
*"already upgraded"*. The bump must be in the digits. Pinned by `ProjectInfoVersionTest`.

### Customer version suffix (never enters `schema_version`)

When a customer's deliverable needs a customization layered on top of an already-released base
version (a feature flag, a bespoke integration, UI copy) without folding that customization into the
mainline version sequence, append a **customer suffix** after the base version number:

```
{major}.{minor}.{maintenance}.{patch}-{customer-id}.{customization-iteration}
```

Example: `4.4.0.101-ACME.loginA` -- built on `4.4.0.101`, for customer `ACME`, one iteration of a
"login" customization. Branch and tag naming for customer deliverables is in section 9, "Customer
customization branches".

**Both the customer id and the customization-iteration must be letters only -- no digits anywhere.**
This is not a style preference, it is a correctness requirement, and it follows directly from the
stripping rule above:

> `ProjectInfo.getProjectVersion()` strips with `replaceAll("[^\d.]", "")` -- only digits and dots
> survive; letters and hyphens are removed regardless of context, and dots are kept regardless of
> context too.

Run the suffix format through that rule and the customer id and customization-iteration vanish
entirely, leaving just the one dot the suffix inserted: `4.4.0.101-ACME.loginA` strips down to
`4.4.0.101.`, and `split("\\.")` drops the trailing empty string, giving `["4","4","0","101"]` --
identical to parsing the version without any customer suffix at all.

**The no-digits rule must cover the whole suffix, not just the customer id while letting the
customization-iteration slide (or vice versa).** A digit on either side survives the strip intact:
best case `toDbSchemaVersion()` reads only the first four segments and silently drops a fifth one;
worst case a digit sitting right before the dot gets absorbed straight into the `patch` segment,
turning `101` into garbage like `1012` -- with no exception and no error message anywhere, surfacing
only the next time a version gets parsed into the wrong number.

The customer suffix **never enters the packed `schema_version`**: whatever
`tb_schema_settings.schema_version` stores is always the four-segment base version after stripping.
With or without the customer suffix, the upgrade gates (section 5) see the exact same version --
the suffix is a human-facing label, not an independent upgrade unit, and it never triggers an extra
`minor`/`maintenance` bump on its own (the release decision rule in section 4 still only looks at how
the base version number changes).

| Scenario | Version string |
|---|---|
| First login-flow customization for customer ACME on `4.4.0.101` | `4.4.0.101-ACME.loginA` |
| Second iteration, same customer, same base version | `4.4.0.101-ACME.loginB` |
| Same customer, re-customized on a new base version | `4.4.0.105-ACME.loginA` |

### Release sequence

```
1.  mvn versions:set -DnewVersion=4.4.0.101 -DgenerateBackupPoms=false
2.  edit ui-ngx/package.json to 4.4.0.101
3.  python docs/tools/release_upgrade_path_check.py . <previous-release-tag>
        -> must exit 0; if it demands a minor bump, go back to step 1 with 4.5.0.0
4.  mvn clean install -T6 -DskipTests          (add packaging flags per 01-build-system.md)
5.  commit, tag v4.4.0.101, push the tag
6.  mvn versions:set -DnewVersion=4.4.0.102-SNAPSHOT -DgenerateBackupPoms=false
7.  edit ui-ngx/package.json, commit as "back to snapshot"
```

Step 3 before step 4, not after: the checker decides whether the number you just set is even legal
for this change set, and building first wastes the build.

Step 6 matters because of the stripping rule above — leaving the poms at the released number makes
the next development build indistinguishable from the release, so a developer's local database gets
stamped with a version that was never shipped.

## 11. Related documents

- [SQL Schema & Migration](20-dao/sql-schema.md) — table inventory, packed version format, index and
  function files
- [Build System](01-build-system.md) — Maven reactor, packaging, where `upgrade.sh` comes from
- [Deployment Modes](02-deployment-modes.md) — the `install` profile and deployment topologies
