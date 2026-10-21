# SQL Schema 与迁移

**本文是权威版。** 英文版 [sql-schema.md](sql-schema.md) 为对照上游 CE 的参照副本，改动请先落在本文。

ThingsBoard 用的是**自建的 schema 管理体系** —— 没有 Liquibase，也没有 Flyway。schema 定义是启动时
加载的原始 SQL 文件，升级则由整体式的 SQL 脚本驱动。

## Schema SQL 文件

全部 schema SQL 文件位于 `dao/src/main/resources/sql/`：

| 文件 | 用途 |
|------|---------|
| `schema-entities.sql` | 核心实体表（device、asset、alarm、user、tenant、dashboard、rule chain 等），外加 `tb_schema_settings` 与 `key_dictionary` |
| `schema-entities-idx.sql` | 实体表的索引（告警查询的复合索引、关系查找、设备/资产查找） |
| `schema-entities-idx-psql-addon.sql` | 额外的 PostgreSQL 专属索引 |
| `schema-ts-psql.sql` | **纯 PostgreSQL** 模式的时序表：`ts_kv`（按 `ts` 范围分区）、`key_dictionary`，以及分区管理存储过程 |
| `schema-ts-latest-psql.sql` | 最新时序表：`ts_kv_latest`，用于快速取最新值。该表在 `schema-entities.sql` 里也有一份（实体 DDL 用），这里单独一份是给 Timescale 路径用的 |
| `schema-timescale.sql` | **TimescaleDB** 模式的时序表：`ts_kv`（TimescaleDB 超表）、`key_dictionary`、`ts_kv_latest`，以及 Timescale 专属函数（`to_uuid`、`delete_device_records_from_ts_kv` 等） |
| `schema-functions.sql` | PostgreSQL 存储函数：`create_or_update_active_alarm`，在数据库里实现告警生命周期状态机 |
| `schema-views.sql` | 数据库视图：`device_info_active_attribute_view`、`device_info_active_ts_view`、`device_info_view`、`alarm_info`（把告警与发起者名字关联） |

## Schema 加载架构

### 全新安装

首次启动时，带 `@Profile("install")` 的 service 按顺序加载 schema 文件：

1. `SqlEntityDatabaseSchemaService` —— 加载 `schema-entities.sql`、`schema-entities-idx.sql`、
   `schema-entities-idx-psql-addon.sql`、`schema-views.sql`、`schema-functions.sql`
2. 时序 schema service（取决于后端）：
   - **SQL 模式：** 加载 `schema-ts-psql.sql`、`schema-ts-latest-psql.sql`
   - **TimescaleDB 模式：** 加载 `schema-timescale.sql`

### schema 设置表的初始化

schema 创建后，`DefaultDatabaseSchemaSettingsService.createSchemaSettings()` 向
`tb_schema_settings` 插入一行，内含当前版本和 product 类型。

## 版本跟踪

### `tb_schema_settings` 表

```sql
CREATE TABLE IF NOT EXISTS tb_schema_settings (
    schema_version bigint NOT NULL,
    product varchar(2) NOT NULL,
    CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version)
);
```

只存一行：当前 schema 版本与 product 代码。本项目的 `ProjectInfo.getProductType()` 返回
**`"CE"`**（社区版），PE 为 `"PE"`。

### 打包的版本格式

版本以打包的 `bigint` 存储：

**老格式**（版本 < 1,000,000,000）：`MMM mmm ppp`（如 `4002001` = 4.2.1）

**新格式**（版本 >= 1,000,000,000）：`MMM mmm mmm ppp`（如 `4002001001` = 4.2.1.1）

计算方式：`major * 1,000,000,000 + minor * 1,000,000 + maintenance * 1000 + patch`

### Product 校验

`product` 列保证一个 CE 实例不会去升级 PE 的数据库（反之亦然）。product 不匹配会立即导致停机退出。

## 升级体系

### 升级脚本

```bash
application/src/main/data/upgrade/
  basic/
    schema_update.sql    # runs on the offline upgrade only
  lts/
    schema_update.sql    # runs on the offline upgrade AND the hot-patch path
```

这**不是**两种升级类型。两个文件在每次离线升级时都会按上述顺序执行。区别在于
`lts/schema_update.sql` **还会**在普通启动时经 `SystemPatchApplier` 执行（当包版本在同一
`major.minor` 族内变大时），而 `basic/schema_update.sql` 和 `schema-entities.sql` 不会。

这使得 `lts` 成为"schema 变更能触达换 jar 升级过的库"的**唯一**去处。关于列该写在哪的约定、以及
热补丁路径能做什么不能做什么，见 [版本管理与系统升级指南](../04-upgrade-and-release.zh-CN.md)。

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

加载并执行 `basic/` 与 `lts/` 两个目录下的 `schema_update.sql`。每个文件被当作单个整体 SQL 字符串
读入，经 `jdbcTemplate.execute()` 执行。

### DefaultDatabaseSchemaSettingsService

负责：

1. **`validateSchemaSettings()`** —— 按顺序检查：
   - `SKIP_SCHEMA_VERSION_CHECK` 环境变量（跳过全部校验）
   - product 类型匹配（CE 对 PE）
   - 库版本不能已经等于当前包版本
   - 库版本不得**超过**包版本（降级被拒绝）
   - 库版本在受支持的升级列表内

2. **受支持的升级路径**（硬编码前缀，用 `startsWith` 匹配）：
   ```
   4.3.0  -> current
   4.3.1  -> current
   4.4.0  -> current   (same-family, enables offline upgrade between two patch releases)
   ```
   正是这条同族条目让上面的降级检查变得承重。理由以及下次 bump `minor` 时的发布清单项，见
   [版本管理与系统升级指南](../04-upgrade-and-release.zh-CN.md)。

3. **`getDbSchemaVersion()`** —— 从 `tb_schema_settings` 读出打包整数，解包成点分版本串（如 "4.3.1.0"）

4. **`updateSchemaVersion()`** —— 升级成功后更新 `tb_schema_settings`

### SKIP_SCHEMA_VERSION_CHECK

环境变量 / Java 属性，跳过全部 schema 版本校验。用于强制重跑升级或排障。

## 主要实体表

| 表 | 存什么 |
|-------|--------|
| `admin_settings` | 平台级管理配置的键值对 |
| `alarm` | 活跃/已清除告警，含严重级别、传播、确认、分派 |
| `alarm_comment` | 用户对告警的评论（按 `created_time` 分区） |
| `entity_alarm` | 把告警关联到其发起者实体 |
| `asset_profile` | 资产类型定义（默认规则链、仪表板、队列） |
| `asset` | 资产实例，带 `tenant_id`、`customer_id`、`asset_profile_id` |
| `attribute_kv` | 实体属性键值对（server/shared/client 作用域） |
| `audit_log` | 审计轨迹条目（按 `created_time` 分区） |
| `component_descriptor` | 供规则引擎注册表使用的规则节点组件元数据 |
| `customer` | 客户/子租户记录，含地址字段 |
| `dashboard` | 仪表板定义（JSON 配置） |
| `device_profile` | 设备类型定义（传输类型、开通、告警规则） |
| `device` | 设备实例，引用配置文件 |
| `device_credentials` | 设备认证凭证（访问令牌、X.509 证书） |
| `entity_view` | 虚拟实体视图（某设备/资产遥测的子集） |
| `error_event` | 错误事件日志（按 `ts` 分区） |
| `lc_event` | 生命周期事件日志（按 `ts` 分区） |
| `oauth2_client` | 用于 SSO 的 OAuth2 客户端配置 |
| `ota_package` | OTA 固件/软件包 |
| `queue` | 队列/topic 配置 |
| `relation` | 实体关系边（from 实体 -> 关系类型 -> to 实体） |
| `rule_chain` | 规则链定义（JSON 配置） |
| `rule_node` | 规则链内的单个规则节点 |
| `rule_node_state` | 有状态规则节点的按实体状态 |
| `rule_node_debug_event` | 规则节点的调试事件（按 `ts` 分区） |
| `rule_chain_debug_event` | 规则链的调试事件（按 `ts` 分区） |
| `stats_event` | 统计事件（按 `ts` 分区） |
| `tb_schema_settings` | Schema 版本跟踪（单行） |
| `tb_user` | 用户账号 |
| `tenant_profile` | 租户配置文件定义（JSON profile 数据） |
| `tenant` | 租户实例 |
| `user_credentials` | 用户登录凭证，含激活/重置令牌 |
| `widget_type` | Widget 类型定义，带 JSON 描述符 |
| `widgets_bundle` | Widget bundle 集合 |
| `widgets_bundle_widget` | 多对多关联：bundle 到 widget 类型 |

## 时序表

### `ts_kv`（SQL 模式 —— PostgreSQL）

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

- 按时间范围分区（按天、按月或按年，取决于配置）。
- 使用**键字典**（`key_dictionary` 表）把字符串键名映射成整数 ID，以节省存储。
- 遥测值存在带类型的列里：`bool_v`、`str_v`、`long_v`、`dbl_v`、`json_v`。每行只有其中一个非 null。

### `ts_kv`（TimescaleDB 模式）

schema 相同，但经 `SELECT create_hypertable('ts_kv', 'ts', chunk_time_interval => ...)` 转成了
TimescaleDB 超表。

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

只存每个 实体+键 组合的最新值。用于仪表板快速加载和最新值查询。经 `version` 列做乐观锁，防止并发
更新之间的竞态。

### `key_dictionary`

```sql
CREATE TABLE IF NOT EXISTS key_dictionary (
    key varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key)
);
```

把字符串键映射成整数 ID。`ts_kv` 和 `ts_kv_latest` 的 `key` 列都存 `int`（即 `key_id`），需要键名时
再关联这张字典表。

## 索引与函数

### `schema-entities-idx.sql`

定义约 50 个索引，覆盖：
- 告警查询（`idx_alarm_originator_alarm_type`、`idx_alarm_originator_created_time`、仅活跃告警的
  过滤索引、按 租户+被分派人 的索引）
- 实体告警查找（带 `INCLUDE(alarm_id)` 的覆盖索引）
- 关系查询（`idx_relation_to_id`、`idx_relation_from_id`）
- 按 租户+客户+类型 的设备查找
- 客户 title 唯一性
- 审计日志的时间范围查询
- 规则链类型过滤
- 等等

### `schema-functions.sql`

PostgreSQL 存储过程。核心函数：
- `create_or_update_active_alarm()` —— 实现告警生命周期：为相同的 发起者+类型 查找已有活跃告警，
  没有则新建。用于避免重复的活跃告警。

### `schema-views.sql`

为便利而关联多表的数据库视图：
- `device_info_active_attribute_view` / `device_info_active_ts_view` —— 设备带客户 title 与活跃状态
- `device_info_view` —— 默认的设备信息视图（指向基于属性的活跃状态）
- `alarm_info` —— 告警带发起者名字（tenant/customer/device/asset）与算出的状态串
  （`CLEARED_ACK`、`CLEARED_UNACK`、`ACTIVE_ACK`、`ACTIVE_UNACK`）

## 启动流程小结

有三条不同的流程会触及 schema。只有前两条跑在 `@Profile("install")` 下。

**全新安装**（`install.sh` / `install.bat`，独立 JVM，跑完退出）：

```
1. SqlEntityDatabaseSchemaService.createDatabaseSchema()
   - Execute schema-entities.sql, schema-entities-idx.sql,
     schema-entities-idx-psql-addon.sql, schema-views.sql, schema-functions.sql
2. DefaultDatabaseSchemaSettingsService.createSchemaSettings()
   - Insert tb_schema_settings row
3. Timeseries schema service creates ts_kv/ts_kv_latest/key_dictionary
4. System data loading (sysadmin, tenant profiles, widgets, queues, ...)
```

**离线升级**（`upgrade.sh` / `upgrade.bat`，同样是独立 JVM）：

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

第 8 步放在最后，正是第 2-7 步在失败后可以重跑的原因。

**热补丁**（`SystemPatchApplier`，在**普通**服务启动时经 `@PostConstruct` 触发 —— 不是 install
profile，所以没有独立 JVM，也不需要停机）：

```
1. Skip unless the package version increased within the same major.minor
   (or SKIP_PATCH_VERSION_CHECK=true)
2. pg_try_advisory_lock -- one cluster node wins, the rest return
3. Execute lts/schema_update.sql          <- note: lts only, not basic, not schema-entities.sql
4. Rebuild views from schema-views.sql
5. System widget types / bundles / missing images / mail templates
6. updateSchemaVersion()
```

这条路径**做不到**规则节点配置迁移，也**不跑** `updateData()` —— 这两个 bean 在 install profile
之外根本不存在 —— 但它在第 6 步仍会推进 `schema_version`。这个组合是选版本号时的首要风险，
见 [版本管理与系统升级指南](../04-upgrade-and-release.zh-CN.md)。

## 相关文档

- [版本管理与系统升级指南](../04-upgrade-and-release.zh-CN.md) —— 某次发布走哪条升级路径、
  schema 变更该写在哪、发布清单
- [DAO 层总览](README.zh-CN.md) —— 架构与接口
- [实体 DAO 编目](entity-dao.zh-CN.md) —— 逐实体的 DAO 参考
