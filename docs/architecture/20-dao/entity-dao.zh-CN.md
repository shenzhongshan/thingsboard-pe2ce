# 实体 DAO 编目

**本文是权威版。** 英文版 [entity-dao.md](entity-dao.md) 为对照上游 CE 的参照副本，改动请先落在本文。

`dao/src/main/java/org/thingsboard/server/dao/` 下每一个实体 DAO 包的完整编目，按领域分组。

---

## 核心实体

### Device

**包：** `dao/src/main/java/org/thingsboard/server/dao/device/`

设备、设备配置文件、凭证和连接状态的持久化。

| 文件 | 职责 |
|------|------|
| `DeviceDao.java` | 接口 —— CRUD，按 租户/客户/名字/类型/配置文件/edge 查找，OTA 过滤 |
| `DeviceProfileDao.java` | 接口 —— 设备配置文件 CRUD |
| `DeviceCredentialsDao.java` | 接口 —— 设备凭证（访问令牌、X.509、MQTT basic） |
| `DeviceServiceImpl.java` | service 实现 |
| `DeviceProfileServiceImpl.java` | 配置文件 service 实现 |
| `DeviceCredentialsServiceImpl.java` | 凭证 service 实现 |
| `DeviceConnectivityServiceImpl.java` | 设备在线/离线状态 |
| `DeviceProfileCaffeineCache.java` | 设备配置文件的 Caffeine 缓存 |
| `DeviceProfileRedisCache.java` | 设备配置文件的 Redis 缓存 |

**关键实体：** `Device`、`DeviceProfile`、`DeviceCredentials`、`ClaimDataInfo`

**关键 JPA 实现：** `dao/src/main/java/org/thingsboard/server/dao/sql/device/JpaDeviceDao.java`

### Asset

**包：** `dao/src/main/java/org/thingsboard/server/dao/asset/`

资产与资产配置文件的持久化。

| 文件 | 职责 |
|------|------|
| `AssetDao.java` | 接口 —— CRUD，按 租户/客户/名字/类型 查找 |
| `AssetProfileDao.java` | 接口 —— 资产配置文件 CRUD |
| `BaseAssetService.java` | service 实现 |
| `AssetProfileServiceImpl.java` | 配置文件 service 实现 |
| `AssetTypeFilter.java` | 资产类型过滤工具 |

**关键实体：** `Asset`、`AssetProfile`

### Alarm

**包：** `dao/src/main/java/org/thingsboard/server/dao/alarm/`

告警生命周期管理：创建、确认、清除、传播和评论。

| 文件 | 职责 |
|------|------|
| `AlarmDao.java` | 接口 —— 告警 CRUD，按 发起者/类型/严重级别/状态 查询，传播 |
| `AlarmCommentDao.java` | 接口 —— 告警评论 CRUD |
| `BaseAlarmService.java` | service 实现 |
| `BaseAlarmCommentService.java` | 评论 service 实现 |
| `AlarmTypesCaffeineCache.java` | 按租户缓存告警类型 |

**关键实体：** `Alarm`、`AlarmComment`、`AlarmInfo`、`EntityAlarm`

### Dashboard

**包：** `dao/src/main/java/org/thingsboard/server/dao/dashboard/`

仪表板与仪表板信息（列表用）的持久化。

| 文件 | 职责 |
|------|------|
| `DashboardDao.java` | 接口 —— CRUD，按 租户/客户/标题 查找 |
| `DashboardInfoDao.java` | 接口 —— 列表视图用的轻量仪表板信息 |
| `DashboardServiceImpl.java` | service 实现 |

**关键实体：** `Dashboard`、`DashboardInfo`

### Entity View

**包：** `dao/src/main/java/org/thingsboard/server/dao/entityview/`

实体视图（设备/资产遥测的虚拟子集）的持久化。

**关键实体：** `EntityView`

### Relation

**包：** `dao/src/main/java/org/thingsboard/server/dao/relation/`

实体关系存储：把设备连到资产、客户、仪表板等。

| 文件 | 职责 |
|------|------|
| `RelationDao.java` | 接口 —— 关系 CRUD，按 from/to 实体查找，关系类型过滤 |
| `BaseRelationService.java` | service 实现 |
| `RelationCaffeineCache.java` / `RelationRedisCache.java` | 多层缓存 |

**关键实体：** `EntityRelation`、`RelationCacheKey`、`RelationCacheValue`

---

## 配置文件

### Device Profile

（属于 `device/` 包 —— 见上文 Device）

**关键实体：** `DeviceProfile` —— 定义传输类型（MQTT/HTTP/CoAP/LwM2M/SNMP）、开通策略、默认规则链、
告警规则和队列设置。

### Asset Profile

（属于 `asset/` 包 —— 见上文 Asset）

**关键实体：** `AssetProfile` —— 为资产类型定义默认规则链、仪表板和队列。

### Tenant Profile

**包：** `dao/src/main/java/org/thingsboard/server/dao/tenant/`

| 文件 | 职责 |
|------|------|
| `TenantProfileDao.java` | 接口 —— 租户配置文件 CRUD |
| `TenantProfileServiceImpl.java` | service 实现 |
| `DefaultTbTenantProfileCache.java` | 配置文件缓存 |

**关键实体：** `TenantProfile` —— 基于 JSON 的 profile 数据，控制租户限额、功能开关、限流。

---

## 用户管理

### Tenant

**包：** `dao/src/main/java/org/thingsboard/server/dao/tenant/`

| 文件 | 职责 |
|------|------|
| `TenantDao.java` | 接口 —— 租户 CRUD，按 title/region 查找 |
| `TenantServiceImpl.java` | service 实现 |
| `TenantCaffeineCache.java` / `TenantRedisCache.java` | 租户缓存 |
| `TenantExistsCaffeineCache.java` / `TenantExistsRedisCache.java` | 存在性检查缓存 |

**关键实体：** `Tenant`

### Customer

**包：** `dao/src/main/java/org/thingsboard/server/dao/customer/`

| 文件 | 职责 |
|------|------|
| `CustomerDao.java` | 接口 —— CRUD，按 租户/标题 查找 |
| `CustomerServiceImpl.java` | service 实现 |

**关键实体：** `Customer` —— 用于分组设备与资产的子租户实体。

### User

**包：** `dao/src/main/java/org/thingsboard/server/dao/user/`

| 文件 | 职责 |
|------|------|
| `UserDao.java` | 接口 —— CRUD，按 租户/邮箱 查找 |
| `UserCredentialsDao.java` | 接口 —— 登录凭证，激活/重置令牌 |
| `UserAuthSettingsDao.java` | 接口 —— 2FA 与 OAuth 设置 |
| `UserSettingsDao.java` | 接口 —— 用户偏好 |
| `UserServiceImpl.java` | service 实现 |
| `UserSettingsServiceImpl.java` | 设置 service |

**关键实体：** `User`、`UserCredentials`、`UserAuthSettings`、`UserSettings`

---

## 配置

### Rule Chain

**包：** `dao/src/main/java/org/thingsboard/server/dao/rule/`

| 文件 | 职责 |
|------|------|
| `RuleChainDao.java` | 接口 —— 规则链 CRUD，根链查找 |
| `RuleChainDetailsDao.java` | 接口 —— 带节点数的规则链信息 |
| `RuleNodeDao.java` | 接口 —— 链内规则节点 CRUD |
| `RuleNodeStateDao.java` | 接口 —— 按节点按实体的状态持久化 |
| `BaseRuleChainService.java` | service 实现 |
| `BaseRuleNodeStateService.java` | 节点状态 service |

**关键实体：** `RuleChain`、`RuleNode`、`RuleNodeState`

### Widget

**包：** `dao/src/main/java/org/thingsboard/server/dao/widget/`

| 文件 | 职责 |
|------|------|
| `WidgetTypeDao.java` | 接口 —— widget 类型 CRUD，按 FQN 查找 |
| `WidgetsBundleDao.java` | 接口 —— widget bundle CRUD |
| `WidgetTypeServiceImpl.java` | service |
| `WidgetsBundleServiceImpl.java` | service |

**关键实体：** `WidgetType`、`WidgetsBundle`、`WidgetsBundleWidget`

### Resource

**包：** `dao/src/main/java/org/thingsboard/server/dao/resource/`

| 文件 | 职责 |
|------|------|
| `TbResourceDao.java` | 接口 —— 资源 CRUD（JS 函数、图片等） |
| `TbResourceInfoDao.java` | 接口 —— 轻量资源信息 |
| `BaseResourceService.java` | service 实现 |
| `BaseImageService.java` | 图片专属 service |
| `DefaultTbResourceDataCache.java` | 资源数据缓存 |

**关键实体：** `TbResource` —— 被 widget/仪表板使用的代码模块、CSS、图片。

### Admin Settings

**包：** `dao/src/main/java/org/thingsboard/server/dao/settings/`

按租户的键值系统设置。存在 `admin_settings` 表里。

### Queue

**包：** `dao/src/main/java/org/thingsboard/server/dao/queue/`

队列配置持久化。为每个租户的消息队列定义 Kafka topic、分区、消费者设置和处理策略。

### Component Descriptor

**包：** `dao/src/main/java/org/thingsboard/server/dao/component/`

规则节点组件描述符持久化。为规则引擎组件注册表把 Java 类名映射到其元数据（类型、作用域、集群模式、
配置描述符）。

### OAuth2

**包：** `dao/src/main/java/org/thingsboard/server/dao/oauth2/`

用于 SSO 登录的 OAuth2 客户端配置。

**关键实体：** `OAuth2Client`

### Domain

**包：** `dao/src/main/java/org/thingsboard/server/dao/domain/`

移动应用域名配置。

---

## Edge

**包：** `dao/src/main/java/org/thingsboard/server/dao/edge/`

边缘计算支持，用于把实体从云端同步到 edge 实例。

| 文件 | 职责 |
|------|------|
| `EdgeDao.java` | 接口 —— edge 实例 CRUD |
| `EdgeEventDao.java` | 接口 —— edge 事件队列（待同步的变更） |
| `EdgeServiceImpl.java` | service 实现 |
| `BaseEdgeEventService.java` | 事件处理 service |
| `PostgresEdgeEventService.java` | PG 专属事件 service |
| `DefaultEdgeSynchronizationManager.java` | 同步状态管理 |

**关键实体：** `Edge`、`EdgeEvent`

---

## 通知

**包：** `dao/src/main/java/org/thingsboard/server/dao/notification/`

站内与外部通知体系。

| 文件 | 职责 |
|------|------|
| `NotificationDao.java` | 接口 —— 已发送通知 CRUD |
| `NotificationRuleDao.java` | 接口 —— 通知规则 CRUD |
| `NotificationTargetDao.java` | 接口 —— 通知目标（收件人）CRUD |
| `NotificationTemplateDao.java` | 接口 —— 通知模板 CRUD |
| `NotificationRequestDao.java` | 接口 —— 通知请求 CRUD |
| `DefaultNotificationService.java` | 核心通知 service |
| `DefaultNotificationRuleService.java` | 规则 service |
| `DefaultNotificationTargetService.java` | 目标 service |
| `DefaultNotificationTemplateService.java` | 模板 service |
| `DefaultNotificationRequestService.java` | 请求 service |
| `DefaultNotificationSettingsService.java` | 用户通知偏好 |

**关键实体：** `Notification`、`NotificationRule`、`NotificationTarget`、`NotificationTemplate`、
`NotificationRequest`

---

## OTA（空中升级）

**包：** `dao/src/main/java/org/thingsboard/server/dao/ota/`

固件与软件 OTA 包管理。

| 文件 | 职责 |
|------|------|
| `OtaPackageDao.java` | 接口 —— OTA 包 CRUD |
| `OtaPackageInfoDao.java` | 接口 —— 轻量包信息 |
| `BaseOtaPackageService.java` | service 实现 |
| `OtaPackageCaffeineCache.java` / `OtaPackageRedisCache.java` | 多层缓存 |

**关键实体：** `OtaPackage`

---

## 时序与遥测

### Timeseries

**包：** `dao/src/main/java/org/thingsboard/server/dao/timeseries/`

时序数据存储抽象。

| 文件 | 职责 |
|------|------|
| `TimeseriesDao.java` | 接口 —— 存/查时序键值数据 |
| `TimeseriesLatestDao.java` | 接口 —— 每实体每键的最新值 |
| `BaseTimeseriesService.java` | service 实现 |
| `CassandraBaseTimeseriesDao.java` | Cassandra 实现 |
| `AbstractCassandraBaseTimeseriesDao.java` | Cassandra 时序 DAO 的基类 |
| `SqlPartition.java` / `SqlTsPartitionDate.java` | SQL 分区支持 |
| `QueryCursor.java` / `TsKvQueryCursor.java` | 基于游标的查询迭代 |
| `TsLatestRedisCache.java` | 最新值的 Redis 缓存 |

**关键实体：** `TsKvEntry`、`TsKvLatest`

### SQL Timeseries

**包：** `dao/src/main/java/org/thingsboard/server/dao/sqlts/`

SQL 专属的时序实现，使用 PostgreSQL 原生分区或 TimescaleDB 的 hyperchunk。

### Attributes

**包：** `dao/src/main/java/org/thingsboard/server/dao/attributes/`

| 文件 | 职责 |
|------|------|
| `AttributesDao.java` | 接口 —— 属性 CRUD（server、shared、client 作用域） |
| `BaseAttributesService.java` | service 实现 |
| `CachedAttributesService.java` | 带缓存的属性 service |
| `AttributeCaffeineCache.java` / `AttributeRedisCache.java` | 多层缓存 |

**关键实体：** `AttributeKvEntry`

---

## 事件与审计

### Events

**包：** `dao/src/main/java/org/thingsboard/server/dao/event/`

生命周期事件、错误事件和统计事件。

| 文件 | 职责 |
|------|------|
| `EventDao.java` | 接口 —— 事件 CRUD |
| `BaseEventService.java` | service 实现 |

**关键实体：** 各类事件类型（`Event`、`LifecycleEvent`、`ErrorEvent`、`StatsEvent`、
`RuleNodeDebugEvent`、`RuleChainDebugEvent`）

### Audit Log

**包：** `dao/src/main/java/org/thingsboard/server/dao/audit/`

审计轨迹持久化。

| 文件 | 职责 |
|------|------|
| `AuditLogDao.java` | 接口 —— 审计日志 CRUD，按时间范围分区 |
| `AuditLogServiceImpl.java` | service |
| `AuditLogLevelFilter.java` | 日志级别过滤（基于掩码） |
| `AuditLogLevelMask.java` | 掩码定义 |

**关键实体：** `AuditLog`

---

## 其他

### RPC

**包：** `dao/src/main/java/org/thingsboard/server/dao/rpc/`

| 文件 | 职责 |
|------|------|
| `RpcDao.java` | 接口 —— RPC 请求 CRUD |
| `BaseRpcService.java` | service 实现 |

**关键实体：** `Rpc`

### API Usage

**包：** `dao/src/main/java/org/thingsboard/server/dao/usagerecord/`

| 文件 | 职责 |
|------|------|
| `ApiUsageStateDao.java` | 接口 —— 按租户的 API 用量状态 |
| `ApiUsageStateServiceImpl.java` | service |
| `DefaultApiLimitService.java` | 限流实现 |

**关键实体：** `ApiUsageState`

### 个人访问令牌（PAT）

**包：** `dao/src/main/java/org/thingsboard/server/dao/pat/`

供程序化 API 访问使用的 API key / 个人访问令牌管理。

### AI

**包：** `dao/src/main/java/org/thingsboard/server/dao/ai/`

AI 模型配置持久化。

**关键实体：** `AiModel`

### Mobile

**包：** `dao/src/main/java/org/thingsboard/server/dao/mobile/`

移动应用与移动应用 bundle 的持久化。

| 文件 | 职责 |
|------|------|
| `MobileAppService.java` | service 接口 |
| `MobileAppBundleService.java` | bundle service 接口 |

### Job

**包：** `dao/src/main/java/org/thingsboard/server/dao/job/`

定时 job 持久化。支持周期性维护任务。

### Housekeeper

**包：** `dao/src/main/java/org/thingsboard/server/dao/housekeeper/`

基于 TTL 的数据清理。从时序表和事件表中移除过期分区与记录。

### Calculated Fields

**包：** `dao/src/main/java/org/thingsboard/server/dao/cf/`

计算字段支持 —— 由既有数据派生出的计算遥测值。

### Trendz

**包：** `dao/src/main/java/org/thingsboard/server/dao/trendz/`

分析/趋势数据持久化。

### 缓存基础设施

**包：** `dao/src/main/java/org/thingsboard/server/dao/cache/`

缓存抽象，为所有实体类型提供 Caffeine（进程内）与 Redis（分布式）双层缓存。

### Entity Query

**包：** `dao/src/main/java/org/thingsboard/server/dao/entity/`

通用实体查询支持 —— 供搜索和实体查询 API 使用的跨实体查找。

---

## 相关文档

- [DAO 层总览](README.zh-CN.md) —— DAO 层的架构与设计
- [SQL Schema 与迁移](sql-schema.zh-CN.md) —— 数据库表与迁移体系
