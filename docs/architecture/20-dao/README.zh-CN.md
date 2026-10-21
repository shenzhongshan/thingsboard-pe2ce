# DAO 层

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

数据访问对象层 —— ThingsBoard 中业务逻辑与持久化存储之间的桥梁。

## 架构

DAO 层采用经典的**接口/实现**模式：

- **接口**位于 `dao/src/main/java/org/thingsboard/server/dao/<entity>/`，为每种实体类型定义契约。
- **JPA 实现**位于 `dao/src/main/java/org/thingsboard/server/dao/sql/`（子包结构镜像接口包），
  经 Hibernate + Spring Data JPA 实现这些契约。
- **SQL repository**（Spring Data `JpaRepository` 的子接口）在每个 JPA DAO 之下提供实际的查询方法。

该模块含 **636 个 Java 文件**，分布在 **48 个 DAO 包**中。

```
dao/src/main/java/org/thingsboard/server/dao/
  Dao.java              -- root interface
  TenantEntityDao.java  -- tenant-scoped helper
  ExportableEntityDao.java -- external-ID-based lookup
  <entity>/             -- interfaces (DeviceDao, AssetDao, etc.)
  sql/
    JpaAbstractDao.java -- base class for all JPA DAOs
    <entity>/           -- JPA implementations (JpaDeviceDao, etc.)
      <Entity>Repository.java  -- Spring Data JPA repository
      <Entity>Entity.java      -- JPA entity (maps to DB table)
  nosql/                -- Cassandra-based DAOs (for timeseries)
```

## 根接口

### `Dao<T>`（根）

```java
public interface Dao<T> {
    List<T> find(TenantId tenantId);
    T findById(TenantId tenantId, UUID id);
    ListenableFuture<T> findByIdAsync(TenantId tenantId, UUID id);
    boolean existsById(TenantId tenantId, UUID id);
    T save(TenantId tenantId, T t);
    T saveAndFlush(TenantId tenantId, T t);
    void removeById(TenantId tenantId, UUID id);
    void removeAllByIds(Collection<UUID> ids);
    List<UUID> findIdsByTenantIdAndIdOffset(TenantId tenantId, UUID idOffset, int limit);
}
```

每个方法都以 `TenantId` 作为第一个参数以实现隔离（见下文"租户隔离"）。

### `TenantEntityDao<T>`

增加租户作用域的分页方法（`countByTenantId`、`findAllByTenantId`）。由直接隶属于租户的实体实现。

### `ExportableEntityDao<I, T>`

供可用外部 ID 导出/导入的实体使用。增加 `findByTenantIdAndExternalId`、`findByTenantIdAndName`
和 `findByTenantId`（分页）。

## JpaAbstractDao —— JPA 实现基类

`JpaAbstractDao<E extends BaseEntity<D>, D>` 继承 `JpaAbstractDaoListeningExecutorService`
并实现 `Dao<D>`。

关键成员：
- `@PersistenceContext EntityManager entityManager` —— 用于持久化操作的 Hibernate entity manager。
- `@Autowired JdbcTemplate jdbcTemplate` —— 用于自定义 SQL 的原生 JDBC。
- `save(TenantId, D)` —— 把领域对象转成 JPA 实体（通过反射）、设置创建时间，再委派给 `doSave()`。
- `doSave(entity, isNew, flush)` —— 新实体用 `persist()` 持久化，已有实体用 `merge()` 更新，
  并通过带版本的实体（`HasVersion`）处理乐观锁。

实体与领域对象的映射约定：
- 领域类（如 `Device`）位于 `common/data`。
- JPA 实体类（如 `DeviceEntity`）位于 `dao/sql/device/`。
- `DaoUtil.getData(entity)` 把实体转回领域对象。

## 租户隔离

多租户在 DAO 层强制执行：

1. **`TenantId` 作为第一个参数**出现在每个 DAO 方法上。每条查询都按 `tenant_id` 过滤。
2. **`tenant_id` 列**存在于每张实体表上（少数系统全局表例外，如 `tenant_profile`）。
3. **唯一性约束按租户限定**：例如 `CONSTRAINT device_name_unq_key UNIQUE (tenant_id, name)` ——
   同一个设备名可以在不同租户中同时存在。
4. **缓存键包含租户 ID**，防止跨租户的缓存污染。

## 双数据库架构

ThingsBoard 把存储拆成两个关注点：

| 关注点 | 后端 | 技术 |
|---------|---------|------------|
| **实体数据**（设备、资产、告警、仪表板、用户等） | SQL | 经 JPA/Hibernate 的 PostgreSQL |
| **时序数据**（遥测、属性、事件） | 可配置 | SQL（PostgreSQL 原生分区）、Cassandra 或 TimescaleDB |

`common/data` 的模型类、`Dao` 接口和 service 层把这一差异屏蔽掉了。实际的时序 DAO 实现在
`dao/src/main/java/org/thingsboard/server/dao/timeseries/` 和
`dao/src/main/java/org/thingsboard/server/dao/sqlts/`。

## 48 个 DAO 包

| # | 包 | 用途 |
|---|---------|---------|
| 1 | `ai/` | AI 模型配置（含 AI service） |
| 2 | `alarm/` | 告警生命周期、评论、告警类型缓存 |
| 3 | `aspect/` | AOP 切面（横切关注点） |
| 4 | `asset/` | Asset 与 AssetProfile 的 CRUD、缓存 |
| 5 | `attributes/` | 实体属性 KV 存储与缓存 |
| 6 | `audit/` | 审计日志持久化 |
| 7 | `cache/` | 缓存抽象（Caffeine + Redis） |
| 8 | `cf/` | 计算字段 |
| 9 | `component/` | 规则节点组件描述符 |
| 10 | `config/` | 系统配置 |
| 11 | `customer/` | 客户 CRUD |
| 12 | `dashboard/` | 仪表板与仪表板信息 |
| 13 | `device/` | Device、DeviceProfile、DeviceCredentials |
| 14 | `dictionary/` | 键字典（时序键映射） |
| 15 | `domain/` | Domain（移动应用域名） |
| 16 | `edge/` | Edge 实例与 edge 事件同步 |
| 17 | `entity/` | 通用实体查询 |
| 18 | `entityview/` | 实体视图 CRUD |
| 19 | `event/` | 生命周期事件、错误事件、统计事件 |
| 20 | `eventsourcing/` | 事件溯源支持 |
| 21 | `exception/` | DAO 层异常类 |
| 22 | `housekeeper/` | Housekeeping（基于 TTL 的数据清理） |
| 23 | `job/` | 定时 job |
| 24 | `mobile/` | 移动应用与移动应用 bundle |
| 25 | `model/` | JPA 实体基类（BaseEntity、ModelConstants） |
| 26 | `nosql/` | Cassandra 抽象 DAO（用于时序） |
| 27 | `notification/` | 通知规则、目标、模板、请求 |
| 28 | `oauth2/` | OAuth2 客户端配置 |
| 29 | `ota/` | OTA 包（固件/软件升级） |
| 30 | `pat/` | 个人访问令牌 / API key |
| 31 | `queue/` | 队列配置 |
| 32 | `relation/` | 实体关系与关系缓存 |
| 33 | `resource/` | 资源（图片、文件）与图片缓存 |
| 34 | `rpc/` | RPC（对设备的远程过程调用） |
| 35 | `rule/` | 规则链与规则节点持久化 |
| 36 | `service/` | service 级 DAO 辅助（如 validator） |
| 37 | `settings/` | 管理设置 |
| 38 | `sql/` | JPA 抽象 DAO、查询辅助、Hibernate 类型 |
| 39 | `sqlts/` | 基于 SQL 的时序 DAO 实现 |
| 40 | `tenant/` | Tenant 与 TenantProfile 的 CRUD、缓存 |
| 41 | `timeseries/` | 时序 DAO 接口与 Cassandra 实现 |
| 42 | `trendz/` | Trendz 分析 |
| 43 | `usage/` | API 用量跟踪 |
| 44 | `usagerecord/` | API 用量状态 / 限流 |
| 45 | `user/` | User、UserCredentials、UserAuthSettings、UserSettings |
| 46 | `util/` | DAO 工具 |
| 47 | `widget/` | Widget 类型与 widget bundle |

注意：`sql/`、`nosql/`、`sqlts/`、`model/`、`util/`、`exception/` 和 `service/` 这类包属于基础设施
包，而非实体专属的 DAO。

## 与 service 层的衔接

```
REST Controller --> EntityService (application/service/)
                       |
                       v
                   EntityDao (dao/<entity>/)
                       |
                       v
               JpaEntityDao (dao/sql/<entity>/)
                       |
                       v
              EntityRepository (Spring Data JPA)
                       |
                       v
                   PostgreSQL
```

- `application/.../controller/` 下的**控制器**处理 HTTP 请求。
- `application/.../service/` 下的 **service** 承载业务逻辑。大体上每个实体一个 service。
- **DAO** 作为构造器依赖注入到 service。service 调用 `dao.save(tenantId, entity)` 等。
- service 从不直接操作 JPA 实体 —— 只有 DAO 层知道实体/DTO 的映射。

## 相关文档

- [实体 DAO 编目](entity-dao.zh-CN.md) —— 全部实体 DAO 的详细编目
- [SQL Schema 与迁移](sql-schema.zh-CN.md) —— 数据库 schema 与升级体系
- [规则引擎架构](../30-rule-engine/README.zh-CN.md) —— 规则链在幕后如何使用 DAO
