# SQL Schema & Migration

ThingsBoard uses a **custom schema management system** -- no Liquibase or Flyway. Schema definitions are raw SQL files loaded at startup, and upgrades are driven by monolithic SQL scripts.

## Schema SQL Files

All schema SQL files reside in `dao/src/main/resources/sql/`:

| File | Purpose |
|------|---------|
| `schema-entities.sql` | Core entity tables (device, asset, alarm, user, tenant, dashboard, rule chain, etc.) plus `tb_schema_settings` and `key_dictionary` |
| `schema-entities-idx.sql` | Indexes for entity tables (compound indexes for alarm queries, relation lookups, device/asset lookups) |
| `schema-entities-idx-psql-addon.sql` | Additional PostgreSQL-specific indexes |
| `schema-ts-psql.sql` | Timeseries tables for **plain PostgreSQL** mode: `ts_kv` (partitioned by range on `ts`), `key_dictionary`, and partition management stored procedures |
| `schema-ts-latest-psql.sql` | Latest-timeseries table: `ts_kv_latest` for fast latest-value lookups. Duplicated inside `schema-entities.sql` for the entities DDL but also standalone for Timescale path |
| `schema-timescale.sql` | Timeseries tables for **TimescaleDB** mode: `ts_kv` (TimescaleDB hypertable), `key_dictionary`, `ts_kv_latest`, and Timescale-specific functions (`to_uuid`, `delete_device_records_from_ts_kv`, etc.) |
| `schema-functions.sql` | PostgreSQL stored functions: `create_or_update_active_alarm` for the alarm lifecycle state machine in the database |
| `schema-views.sql` | Database views: `device_info_active_attribute_view`, `device_info_active_ts_view`, `device_info_view`, `alarm_info` (joins alarms with originator names) |

## Schema Loading Architecture

### Fresh Installation

On first startup, the `@Profile("install")` services load the schema files in order:

1. `SqlEntityDatabaseSchemaService` -- loads `schema-entities.sql`, `schema-entities-idx.sql`, `schema-entities-idx-psql-addon.sql`, `schema-views.sql`, `schema-functions.sql`
2. Timeseries schema service (backend-dependent):
   - **SQL mode:** loads `schema-ts-psql.sql`, `schema-ts-latest-psql.sql`
   - **TimescaleDB mode:** loads `schema-timescale.sql`

### Schema settings table initialization

After schema creation, `DefaultDatabaseSchemaSettingsService.createSchemaSettings()` inserts a row into `tb_schema_settings` with the current version and product type.

## Version Tracking

### `tb_schema_settings` table

```sql
CREATE TABLE IF NOT EXISTS tb_schema_settings (
    schema_version bigint NOT NULL,
    product varchar(2) NOT NULL,
    CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version)
);
```

Stores exactly one row: the current schema version and product code. In this project
`ProjectInfo.getProductType()` returns **`"CE"`** (Community Edition); PE uses `"PE"`.

### Packed Version Format

The version is stored as a packed `bigint`:

**Old format** (version < 1,000,000,000): `MMM mmm ppp` (e.g., `4002001` = 4.2.1)

**New format** (version >= 1,000,000,000): `MMM mmm mmm ppp` (e.g., `4002001001` = 4.2.1.1)

Calculated as: `major * 1,000,000,000 + minor * 1,000,000 + maintenance * 1000 + patch`

### Product Validation

The `product` column ensures a CE instance cannot upgrade a PE database (or vice versa). Mismatched
products cause an immediate shutdown.

## Upgrade System

### Upgrade Scripts

```bash
application/src/main/data/upgrade/
  basic/
    schema_update.sql    # runs on the offline upgrade only
  lts/
    schema_update.sql    # runs on the offline upgrade AND the hot-patch path
```

These are not two kinds of upgrade. Both files run, in this order, on every offline upgrade.
The difference is that `lts/schema_update.sql` *also* runs on normal startup via
`SystemPatchApplier` when the package version increases within the same `major.minor` family,
whereas `basic/schema_update.sql` and this file (`schema-entities.sql`) do not.

That makes `lts` the only place a schema change reaches a database that was upgraded by swapping
the jar. See [Versioning & Upgrade Guide](../04-upgrade-and-release.md) for the convention on where
to put a column, and for what the hot-patch path can and cannot do.

### SqlDatabaseUpgradeService

```java
@Service
@Profile("install")
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {
    @Override
    public void upgradeDatabase() {
        loadSql(getSchemaUpdateFile("basic"));
        loadSql(getSchemaUpdateFile("lts"));
    }
}
```

Loads and executes the `schema_update.sql` from both `basic/` and `lts/` directories. Each file is read as a single monolithic SQL string and executed via `jdbcTemplate.execute()`.

### DefaultDatabaseSchemaSettingsService

Handles:

1. **`validateSchemaSettings()`** -- checks, in order:
   - `SKIP_SCHEMA_VERSION_CHECK` env var (bypasses all validation)
   - Product type matches (CE vs PE)
   - Database version is not already at the current package version
   - Database version does not *exceed* the package version (downgrades are rejected)
   - Database version is in the supported upgrade list

2. **Supported upgrade paths** (hardcoded prefixes, matched with `startsWith`):
   ```
   4.3.0  -> current
   4.3.1  -> current
   4.4.0  -> current   (same-family, enables offline upgrade between two patch releases)
   ```
   The same-family entry is what makes the downgrade check above load-bearing. See
   [Versioning & Upgrade Guide](../04-upgrade-and-release.md) for the reasoning and for the
   release checklist item when `minor` is next bumped.

3. **`getDbSchemaVersion()`** -- reads the packed integer from `tb_schema_settings`, unpacks it to a dotted version string (e.g., "4.3.1.0")

4. **`updateSchemaVersion()`** -- updates `tb_schema_settings` after a successful upgrade

### SKIP_SCHEMA_VERSION_CHECK

Environment variable / Java property that bypasses all schema version validation. Useful for forcing re-upgrade or troubleshooting.

## Key Entity Tables

| Table | Stores |
|-------|--------|
| `admin_settings` | Platform-wide admin configuration key-value pairs |
| `alarm` | Active/cleared alarms with severity, propagation, acknowledgement, assignment |
| `alarm_comment` | User comments on alarms (partitioned by `created_time`) |
| `entity_alarm` | Links alarms to their originator entities |
| `asset_profile` | Asset type definitions (default rule chains, dashboards, queues) |
| `asset` | Asset instances with `tenant_id`, `customer_id`, `asset_profile_id` |
| `attribute_kv` | Entity attribute key-value pairs (server/shared/client scopes) |
| `audit_log` | Audit trail entries (partitioned by `created_time`) |
| `component_descriptor` | Rule node component metadata for the rule engine registry |
| `customer` | Customer/sub-tenant records with address fields |
| `dashboard` | Dashboard definitions (JSON configuration) |
| `device_profile` | Device type definitions (transport type, provisioning, alarm rules) |
| `device` | Device instances with profile references |
| `device_credentials` | Device authentication credentials (access tokens, X.509 certs) |
| `entity_view` | Virtual entity views (subset of a device/asset's telemetry) |
| `error_event` | Error event log (partitioned by `ts`) |
| `lc_event` | Lifecycle event log (partitioned by `ts`) |
| `oauth2_client` | OAuth2 client configurations for SSO |
| `ota_package` | OTA firmware/software packages |
| `queue` | Queue/topic configurations |
| `relation` | Entity relationship edges (from entity -> relation type -> to entity) |
| `rule_chain` | Rule chain definitions (JSON configuration) |
| `rule_node` | Individual rule nodes within rule chains |
| `rule_node_state` | Per-entity state for stateful rule nodes |
| `rule_node_debug_event` | Debug events from rule nodes (partitioned by `ts`) |
| `rule_chain_debug_event` | Debug events from rule chains (partitioned by `ts`) |
| `stats_event` | Statistics events (partitioned by `ts`) |
| `tb_schema_settings` | Schema version tracking (single row) |
| `tb_user` | User accounts |
| `tenant_profile` | Tenant profile definitions (JSON profile data) |
| `tenant` | Tenant instances |
| `user_credentials` | User login credentials with activation/reset tokens |
| `widget_type` | Widget type definitions with JSON descriptors |
| `widgets_bundle` | Widget bundle collections |
| `widgets_bundle_widget` | Many-to-many join: bundles to widget types |

## Timeseries Tables

### `ts_kv` (SQL mode -- PostgreSQL)

```sql
CREATE TABLE IF NOT EXISTS ts_kv (
    entity_id uuid NOT NULL,
    key int NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v varchar(10000000),
    long_v bigint,
    dbl_v double precision,
    json_v json,
    CONSTRAINT ts_kv_pkey PRIMARY KEY (entity_id, key, ts)
) PARTITION BY RANGE (ts);
```

- Partitioned by time range (days, months, or years depending on configuration).
- Uses a **key dictionary** (`key_dictionary` table) to map string key names to integer IDs, saving storage space.
- Telemetry values are stored in typed columns: `bool_v`, `str_v`, `long_v`, `dbl_v`, `json_v`. Only one of these is non-null per row.

### `ts_kv` (TimescaleDB mode)

Same schema but converted to a TimescaleDB hypertable via `SELECT create_hypertable('ts_kv', 'ts', chunk_time_interval => ...)`.

### `ts_kv_latest`

```sql
CREATE TABLE IF NOT EXISTS ts_kv_latest (
    entity_id uuid NOT NULL,
    key int NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v varchar(10000000),
    long_v bigint,
    dbl_v double precision,
    json_v json,
    version bigint default 0,
    CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
);
```

Stores only the most recent value per entity+key combination. Used for fast dashboard loading and latest-value queries. Optimistic locking via the `version` column prevents race conditions between concurrent updates.

### `key_dictionary`

```sql
CREATE TABLE IF NOT EXISTS key_dictionary (
    key varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key)
);
```

Maps string keys to integer IDs. Both `ts_kv` and `ts_kv_latest` store `key` as `int` (the `key_id`), joining to this dictionary when the key name is needed.

## Indexes and Functions

### `schema-entities-idx.sql`

Defines ~50 indexes covering:
- Alarm queries (`idx_alarm_originator_alarm_type`, `idx_alarm_originator_created_time`, filtered indexes for active-only alarms, indexes by tenant + assignee)
- Entity alarm lookups (cover indexes with `INCLUDE(alarm_id)`)
- Relation queries (`idx_relation_to_id`, `idx_relation_from_id`)
- Device lookups by tenant + customer + type
- Customer title uniqueness
- Audit log time-range queries
- Rule chain type filtering
- And many more...

### `schema-functions.sql`

PostgreSQL stored procedures. Key function:
- `create_or_update_active_alarm()` -- implements the alarm lifecycle: finds an existing active alarm for the same originator+type or creates a new one. Used to avoid duplicate active alarms.

### `schema-views.sql`

Database views that join tables for convenience:
- `device_info_active_attribute_view` / `device_info_active_ts_view` -- device with customer title and active status
- `device_info_view` -- default device info view (points to attribute-based active status)
- `alarm_info` -- alarm with originator name (tenant/customer/device/asset) and computed status string (`CLEARED_ACK`, `CLEARED_UNACK`, `ACTIVE_ACK`, `ACTIVE_UNACK`)

## Startup Flow Summary

Three distinct flows touch the schema. Only the first two run under `@Profile("install")`.

**Fresh install** (`install.sh` / `install.bat`, a separate JVM that exits when done):

```
1. SqlEntityDatabaseSchemaService.createDatabaseSchema()
   - Execute schema-entities.sql, schema-entities-idx.sql,
     schema-entities-idx-psql-addon.sql, schema-views.sql, schema-functions.sql
2. DefaultDatabaseSchemaSettingsService.createSchemaSettings()
   - Insert tb_schema_settings row
3. Timeseries schema service creates ts_kv/ts_kv_latest/key_dictionary
4. System data loading (sysadmin, tenant profiles, widgets, queues, ...)
```

**Offline upgrade** (`upgrade.sh` / `upgrade.bat`, also a separate JVM):

```
1. DefaultDatabaseSchemaSettingsService.validateSchemaSettings()
   - product type, not-already-upgraded, not a downgrade, version whitelisted
2. CacheCleanupService.clearCache()
3. SqlDatabaseUpgradeService.upgradeDatabase()
   - Execute basic/schema_update.sql then lts/schema_update.sql
4. createDatabaseSchema(false) / views / device_info view / indexes
5. DataUpdateService.updateData()        <- Java-level data migration
6. DataUpdateService.upgradeRuleNodes()  <- rule node config migration
7. System widgets, LwM2M resources, images, mail templates
8. DefaultDatabaseSchemaSettingsService.updateSchemaVersion()   <- version written LAST
```

Step 8 running last is what makes steps 2-7 re-runnable after a failure.

**Hot patch** (`SystemPatchApplier`, `@PostConstruct` on a *normal* server startup — not an install
profile, so there is no separate JVM and no downtime):

```
1. Skip unless the package version increased within the same major.minor
   (or SKIP_PATCH_VERSION_CHECK=true)
2. pg_try_advisory_lock -- one cluster node wins, the rest return
3. Execute lts/schema_update.sql          <- note: lts only, not basic, not schema-entities.sql
4. Rebuild views from schema-views.sql
5. System widget types / bundles / missing images / mail templates
6. updateSchemaVersion()
```

This path **cannot** migrate rule node configs or run `updateData()` — neither bean exists outside
the install profile — yet it still advances `schema_version` at step 6. That combination is the
main hazard when choosing a version number; see
[Versioning & Upgrade Guide](../04-upgrade-and-release.md).

## Related Documents

- [Versioning & Upgrade Guide](../04-upgrade-and-release.md) -- which upgrade path a release takes,
  where to put a schema change, release checklist
- [DAO Layer Overview](README.md) -- architecture and interfaces
- [Entity DAO Catalog](entity-dao.md) -- entity-by-entity DAO reference
