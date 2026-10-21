# Service 层

**本文是权威版。** 英文版 [services.md](services.md) 为对照上游 CE 的参照副本，改动请先落在本文。

`application/src/main/java/org/thingsboard/server/service/` 下的 service 层含 44 个 service 包，
实现应用的业务逻辑。service 位于 REST 控制器与 DAO 层之间，协调实体操作、安全和 actor 系统。

## Service 层架构

service 大致分三类：

1. **实体 service** —— 每种实体类型一个（`device/`、`asset/`、`alarm/` 等），负责 CRUD 编排、
   校验，以及与 actor 系统的集成
2. **系统 service** —— 基础设施类 service（`queue/`、`subscription/`、`telemetry/`、`security/`
   等），管理平台的运行时行为
3. **支撑 service** —— 面向特定功能的技术类 service（`executors/`、`install/`、`lwm2m/` 等）

### 实体 service（在 `service/entitiy/` 下）

与顶层 service 包不同，`service/entitiy/` 包提供的是一套结构化的实体 service 框架：

| 子包 | 关键类 | 用途 |
|-------------|-----------|---------|
| `device/` | `DefaultTbDeviceService`、`TbDeviceService` | 设备 CRUD、分配、认领 |
| `asset/` | `DefaultTbAssetService` | 资产 CRUD 与分配 |
| `alarm/` | `DefaultTbAlarmService` | 告警 CRUD 与生命周期 |
| `customer/` | `DefaultTbCustomerService` | 客户管理 |
| `tenant/` | `DefaultTbTenantService`、`TbTenantService` | 租户生命周期管理 |
| `user/` | `DefaultTbUserService` | 用户管理 |
| `dashboard/` | `DefaultTbDashboardService` | 仪表板 CRUD |
| `edge/` | `DefaultTbEdgeService` | Edge 实例管理 |
| `entityview/` | `DefaultTbEntityViewService`、`TbEntityViewService` | 实体视图 CRUD |
| `widgets/` | `DefaultTbWidgetsService` | Widget 管理 |
| `ota/` | `DefaultTbOtaPackageService` | OTA 包管理 |
| `queue/` | `DefaultTbQueueService` | 队列配置 |
| `ai/` | `DefaultTbAiModelService`、`TbAiModelService` | AI 模型管理 |
| `cf/` | `DefaultTbCalculatedFieldService` | 计算字段管理 |
| `domain/` | `DefaultTbDomainService` | 域名管理 |
| `mobile/` | `DefaultTbMobileAppService` | 移动应用管理 |
| `entity/` | `SimpleTbEntityService` | 通用实体操作 |
| `oauth2client/` | `DefaultTbOAuth2ClientService` | OAuth2 客户端管理 |

实体 service 的关键模式：
- `AbstractTbEntityService` —— 带审计日志的实体 service 通用基类
- `TbLogEntityActionService` + `EntityActionService` —— 为所有实体变更记审计日志
- `EntityStateSourcingListener` —— 监听实体状态事件

### 顶层 service 包

#### 面向实体的 service

| 包 | 关键类 | 用途 |
|---------|-------------|---------|
| `action/` | 动作处理 | 实体动作执行 |
| `ai/` | AI 模型运行时 | AI 模型推理编排 |
| `asset/` | `AssetBulkImportService` | 从 CSV 批量导入资产 |
| `cf/` | `CalculatedFieldProcessingService`、`CalculatedFieldStateService`、`CalculatedFieldQueueService`、`OwnerService` | 计算字段求值流水线 |
| `component/` | `ComponentDiscoveryService` | 规则节点组件注册与发现 |
| `device/` | `ClaimDevicesServiceImpl`、`DeviceBulkImportService`、`DeviceProvisionServiceImpl` | 设备认领、批量导入、开通 |
| `gateway_device/` | 网关设备管理 | 网关专属的设备操作 |
| `mail/` | `MailExecutorService` | 邮件发送执行 |
| `mobile/` | 移动应用 service | 移动应用运行时支持 |
| `notification/` | 通知 service | 推送/邮件通知投递 |
| `ota/` | `OtaPackageStateService` | OTA 包状态跟踪 |
| `resource/` | `TbResourceDataCache` | 资源数据缓存 |
| `rpc/` | `TbCoreDeviceRpcService`、`TbRuleEngineDeviceRpcService`、`TbRpcService` | RPC 请求处理（core、rule-engine、通用） |
| `rule/` | 规则链管理 | 规则链生命周期 |
| `ruleengine/` | 规则引擎运行时 | 规则引擎执行协调 |
| `script/` | 脚本执行 | JS/TBEL 脚本调用 |
| `sms/` | `SmsExecutorService` | 短信发送执行 |
| `user/` | 用户 service | 用户级操作 |

#### 系统 service

| 包 | 关键类 | 用途 |
|---------|-------------|---------|
| `apiusage/` | `TbApiUsageStateService` | API 用量跟踪与限流强制 |
| `executors/` | `DbCallbackExecutorService`、`ExternalCallExecutorService`、`NotificationExecutorService`、`PubSubRuleNodeExecutorProvider`、`SharedEventLoopGroupService`、`VersionControlExecutor`、`GrpcCallbackExecutorService` | 面向特定负载的专属线程池 |
| `housekeeper/` | housekeeping 任务 | 数据清理、TTL 强制 |
| `install/` | `DefaultSystemDataLoaderService`、`SqlEntityDatabaseSchemaService`、`CassandraKeyspaceService`、`DatabaseUpgradeService`、`DataUpdateService`、`CacheCleanupService` | 数据库 schema 创建、升级、迁移、初始数据 |
| `job/` | job 调度 | 后台 job 管理 |
| `partition/` | 分区管理 | 队列分区分配 |
| `profile/` | `TbDeviceProfileCache`、`TbAssetProfileCache` | 配置文件缓存 |
| `queue/` | `DefaultTbCoreConsumerService`、`DefaultTbRuleEngineConsumerService`、`DefaultTbEdgeConsumerService`、`DefaultTbCalculatedFieldConsumerService`、`DefaultQueueRoutingInfoService`、`DefaultTenantRoutingInfoService` | 队列消息消费、路由与处理 |
| `security/` | 认证 provider、JWT、权限、2FA、系统安全 | 见 [config-and-security.zh-CN.md](config-and-security.zh-CN.md) |
| `session/` | `DeviceSessionCacheService` | 设备传输会话管理 |
| `state/` | `DefaultDeviceStateService`、`DefaultDeviceStateManager` | 设备连接状态跟踪 |
| `stats/` | 统计采集 | 系统与租户统计 |
| `subscription/` | `DefaultSubscriptionManagerService`、`DefaultTbEntityDataSubscriptionService`、`DefaultTbLocalSubscriptionService` | 遥测、告警、属性的 WebSocket 数据订阅 |
| `sync/` | `ExportableEntitiesService`、`EntitiesVersionControlService` | Edge 同步、实体导出/导入、版本控制 |
| `system/` | 系统级操作 | 系统配置、健康 |
| `telemetry/` | `DefaultTbTelemetryService`、`DefaultAlarmSubscriptionService`、`DefaultTelemetrySubscriptionService`、`InternalTelemetryService`、`TbTelemetryService` | 时序与属性数据的读写、告警订阅管理 |
| `transport/` | `TbCoreToTransportService` | 从 core 到 transport 服务的通信 |
| `ttl/` | TTL 管理 | 生存期策略强制 |
| `update/` | 系统更新 | 软件/数据更新编排 |
| `ws/` | WebSocket service | WebSocket 会话与订阅管理 |

#### 支撑与集成 service

| 包 | 关键类 | 用途 |
|---------|-------------|---------|
| `lwm2m/` | LwM2M 支持 | LwM2M 协议集成 |
| `edqs/` | EDQS 集成 | 实体数据查询服务集成 |
| `query/` | 查询 service | 复杂实体数据查询 |

## Service 如何与 DAO 和 actor 交互

### 与 DAO 的交互

service 直接注入并调用 DAO 层接口（来自 `common/dao-api`）。DAO 层负责 SQL/NoSQL 持久化。service
在 DAO 操作之上叠加业务逻辑、校验、实体生命周期管理和审计日志。

保存设备的流程示例：
```
DeviceController -> TbDeviceService.save()
  -> validates device (checkEntity)
  -> calls DeviceService (DAO) to persist
  -> sends ComponentLifecycleMsg to actor system
  -> logs entity action via EntityActionService
```

### 与 actor 系统的交互

service 这样与 actor 通信：
1. 注入 `TbClusterService` 或 `TbQueueProducerProvider` 以向队列发送消息
2. 队列把消息路由到相应的分区/节点
3. 消费者 service 反序列化后，经 `ActorSystemContext.tell()` 路由给 actor

例如 `TbCoreToTransportService` 把消息发给 transport 节点以完成设备通信。

## 事务管理

需要原子数据库操作的 service 方法使用 Spring 的声明式事务管理（`@Transactional`）。事务边界通常
落在 service 方法这一层。

关键模式：
- 实体 service 先做校验，再委派给 DAO service 做持久化
- 实际的 `@Transactional` 边界由 DAO 层（`dao` 模块）处理
- service 在单个业务操作内协调多次 DAO 调用与 actor 消息
- 异步 DAO 操作使用 ListenableFuture/Guava future

## 相关文档

- [README](README.zh-CN.md) —— Application 模块总览
- [控制器](controllers.zh-CN.md) —— 调用 service 的 REST API 控制器
- [Actors](actors.zh-CN.md) —— service 与之交互的 actor 系统
- [配置与安全](config-and-security.zh-CN.md) —— 安全 service 细节
