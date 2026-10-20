# Release & upgrade tooling

Three scripts that enforce the rules in
[Versioning & Upgrade Guide](../architecture/04-upgrade-and-release.md). Plain Python 3, no
dependencies, no virtualenv. All three print UTF-8 regardless of the Windows console code page.

| Script | Run it when | Touches the working tree? |
|---|---|---|
| `release_upgrade_path_check.py` | before every release, and in CI | no — read-only |
| `release_upgrade_path_check_test.py` | after editing the checker | **yes** — see the warning below |
| `upgrade_doc_conformance.py` | after editing the guide or the upgrade code | no — read-only |

---

## `release_upgrade_path_check.py`

Decides whether the version number you have set is legal for the change set, and whether the change
set can reach every database. **This is the release gate.**

```bash
python docs/tools/release_upgrade_path_check.py [repo-root] [baseline-ref]

# typical use, from the repo root, against the previous release
python docs/tools/release_upgrade_path_check.py . v4.4.0.101
```

Both arguments are optional. The repo root defaults to two levels above the script; the baseline
defaults to the nearest reachable `v*` tag. **Pass the baseline explicitly for now** — the
repository's only tags are inherited from upstream (`v2.5`, `v3.x`), so the default resolves to
something meaningless like `v2.5`. It prints whichever tag it picked, so you will see it happen.

Comparison is baseline vs the **working tree**, not vs `HEAD`, so uncommitted changes are included
and "forgot to commit" cannot slip past the gate.

**Exit codes:** `0` the version matches the change set · `1` at least one blocking rule failed ·
`2` arguments unusable.

The eight blocking rules and the reasoning behind each are in
[section 4 of the guide](../architecture/04-upgrade-and-release.md). It also fails rather than
passing when it cannot read its own inputs (missing baseline `pom.xml`, failed `git diff`,
unrecognisable `updateData()` body, unparseable whitelist) — a check that cannot evaluate its input
must not report success.

## `release_upgrade_path_check_test.py`

Self-test for the checker: 8 scenarios, each constructing a condition and asserting the checker's
exit code and message.

```bash
python docs/tools/release_upgrade_path_check_test.py
```

> **It rewrites real files in the working tree** — `pom.xml`, `TbRestApiCallNode.java`,
> `DefaultDataUpdateService.java`, both `schema_update.sql` files and
> `DefaultDatabaseSchemaSettingsService.java` — and restores them from an in-memory backup in a
> `finally` block. Safe to run with uncommitted changes present, but **do not edit those files while
> it runs**, and if it is killed mid-run check `git status` before continuing.

Scenario 3 is the one that matters most: a `minor` bump plus a rule node change **must not** fail.
Without it, a checker hard-wired to always fail would look identical to a correct one.

Override the baseline ref with `RELEASE_CHECK_TEST_BASELINE` if `HEAD~5` is not a valid ref in your
clone. The scenarios construct their own conditions by editing the working tree, so the baseline's
content does not matter — it only needs to exist.

## `upgrade_doc_conformance.py`

Re-derives every factual claim in the guide from the source, so the document cannot quietly drift.

```bash
python docs/tools/upgrade_doc_conformance.py
```

52 assertions: internal links resolve, named source files exist, `productType` is `"CE"`, the
whitelist holds exactly the documented entries, the downgrade gate is present, the packing formula
matches character for character, the hot patch's four capability boundaries hold (runs `lts` only,
does not migrate rule nodes, never calls `updateData()`, still advances `schema_version`), the
offline path writes the version last, `schema-entities.sql` has no `ADD COLUMN`, `basic`'s entries
are all present in `lts`, `lts` has exactly one dollar-quoted block, section numbering is
contiguous, and each of the checker's eight rule messages still exists.

It has twice caught wrong numbers in the guide that would otherwise have sat there indefinitely —
once a rule count, once a `pom.xml` count inflated by a build artifact under `target/`. When it
fails, fix the document unless the code genuinely changed.

---

## Adding a rule

Both the checker and the guide have to be updated together, and the self-test is what keeps them
honest:

1. Add the condition to `release_upgrade_path_check.py` via `problems.append(...)`.
2. Add a row to the blocking-rules table in
   [section 4 of the guide](../architecture/04-upgrade-and-release.md).
3. Add a scenario to `release_upgrade_path_check_test.py` that triggers it — **and make sure some
   existing scenario still passes**, otherwise you cannot tell a real rule from a stuck one.
4. Add an assertion to `upgrade_doc_conformance.py` tying the rule's message to the guide.

Note the checker strips `--` comments before scanning SQL. The first version did not, and the
commented-out example DDL in `schema-entities.sql`'s own header reported itself as a violation.
