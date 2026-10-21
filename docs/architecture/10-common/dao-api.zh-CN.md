# Common DAO API（`common/dao-api`）

**本文是权威版。** 英文版 [dao-api.md](dao-api.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:dao-api`

`common/dao-api` 模块为所有实体类型定义 DAO（数据访问对象）接口和 service 级契约。它提供 repository
模式的抽象层、用于条件装配 bean 的数据库类型注解，以及基础 DAO 工具。具体实现位于 `dao/` 模块
（不在 `common/` 下）。

## 架构

### DAO 接口模式

每个实体类型都有一个专属 DAO 接口，遵循统一模式：

```java
public interface DeviceDao extends Dao<Device> {
    Device findByTenantIdAndName(TenantId tenantId, String name);
    PageData<Device> findByTenantId(TenantId tenantId, PageLink pageLink);
    // entity-specific queries...
}
```

基接口 `Dao<T>` 提供 CRUD 操作：
- `T findById(TenantId tenantId, UUID id)`
- `T save(TenantId tenantId, T entity)`
- `boolean removeById(TenantId tenantId, UUID id)`

### 数据库类型注解

模块用自定义注解，根据当前激活的数据库类型条件装配 DAO 实现。Spring 的条件 bean 在启动时路由到
正确的实现。

| 注解 | 条件 | 用于 |
|-----------|-----------|----------|
| `@SqlDao` | 使用 SQL 数据库时始终激活 | 主要实体 DAO（device、asset、customer 等） |
| `@NoSqlAnyDao` | `database.ts.type==cassandra` 或 `database.ts_latest.type==cassandra` | 基于 Cassandra 的 DAO |
| `@NoSqlTsDao` | `database.ts.type==cassandra` | Cassandra 时序 |
| `@NoSqlTsLatestDao` | `database.ts_latest.type==cassandra` | Cassandra 最新时序 |
| `@SqlTsDao` | SQL 时序激活 | SQL 时序 |
| `@SqlTsLatestDao` | SQL 最新时序激活 | SQL 最新值 |
| `@SqlTsLatestAnyDao` | 任意 SQL 最新时序 | 最新值（任意 SQL 后端） |
| `@SqlTsOrTsLatestAnyDao` | SQL TS 或 TS Latest | TS + latest 合并 |
| `@TimescaleDBTsDao` | `database.ts.type==timescale` | TimescaleDB 专属时序查询 |
| `@TimescaleDBTsLatestDao` | `database.ts_latest.type==timescale` | TimescaleDB 最新值 |

像 `@NoSqlAnyDao` 这类注解使用 `@ConditionalOnExpression`：

```java
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${database.ts.type}'=='cassandra' || '${database.ts_latest.type}'=='cassandra'")
public @interface NoSqlAnyDao {}
```

模块还提供 `TbAutoConfiguration`，按 profile 设置编排自动配置。

### 数据库类型信息

`DbTypeInfoComponent`（及其默认实现 `DefaultDbTypeInfoComponent`）提供当前数据库配置的运行时信息，
让代码能按系统正在用 SQL、NoSQL 还是 TimescaleDB 来分支。

## DAO 子包

`org.thingsboard.server.dao` 下的每个子包为某个特定领域实体定义接口：

| 子包 | 实体 | 主要接口 |
|-----------|--------|----------------|
| `device/` | Device | `DeviceDao`、`DeviceCredentialsDao` |
| `asset/` | Asset | `AssetDao` |
| `customer/` | Customer | `CustomerDao` |
| `tenant/` | Tenant | `TenantDao`、`TenantProfileDao` |
| `user/` | User | `UserDao`、`UserCredentialsDao` |
| `dashboard/` | Dashboard | `DashboardDao` |
| `alarm/` | Alarm | `AlarmDao`、`AlarmCommentDao` |
| `rule/` | Rule Chain、Rule Node | `RuleChainDao`、`RuleNodeDao` |
| `widget/` | Widget | `WidgetTypeDao`、`WidgetsBundleDao` |
| `edge/` | Edge | `EdgeDao`、`EdgeEventDao` |
| `entityview/` | Entity View | `EntityViewDao` |
| `relation/` | Relations | `RelationDao` |
| `attributes/` | Attributes | `AttributeDao` |
| `event/` | Events | `EventDao` |
| `audit/` | Audit Logs | `AuditLogDao` |
| `rpc/` | RPC | `RpcDao` |
| `resource/` | Resources | `ResourceDao` |
| `ota/` | OTA Packages | `OtaPackageDao` |
| `notification/` | Notifications | 通知相关 DAO |
| `ai/` | AI Models | `AiModelDao` |
| `domain/` | Domains | `DomainDao` |
| `mobile/` | Mobile Apps | `MobileAppDao`、`MobileAppBundleDao` |
| `oauth2/` | OAuth2 | OAuth2 客户端 DAO |
| `pat/` | PAT | 个人访问令牌 DAO |
| `component/` | Component Descriptors | `ComponentDescriptorDao` |
| `queue/` | Queue Config | 队列 DAO |
| `settings/` | Admin Settings | `AdminSettingsDao` |
| `timeseries/` | Timeseries | 时序 DAO |
| `usage/` | Usage Stats | 用量 DAO |
| `usagerecord/` | Usage Records | 用量记录 DAO |
| `job/` | Jobs | `JobDao` |
| `trendz/` | Trendz Analytics | Trendz 分析 DAO |
| `cf/` | Calculated Fields | `CalculatedFieldDao` |
| `entity/` | Entity Counts/Queries | 实体级计数/查询 service |

### AsyncTask 工具

`AsyncTask.java` 提供带重试支持的异步任务通用基类，被 DAO 实现用于长耗时或易失败的操作。

## 依赖关系

- **依赖：** `common/data`（实体模型、ID）
- **被谁依赖：** `dao/`（具体的 SQL 与 NoSQL 实现）、`application`（使用 DAO 的 service）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `dao-api`
