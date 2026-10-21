# REST API 控制器

**本文是权威版。** 英文版 [controllers.md](controllers.md) 为对照上游 CE 的参照副本，改动请先落在本文。

应用通过 63 个控制器类暴露一整套 REST API，全部位于
`application/src/main/java/org/thingsboard/server/controller/`。所有控制器都标注了
`@RestController`、`@TbCoreComponent` 和 `@RequestMapping("/api")`。

## 基类

### BaseController

`BaseController.java` 是几乎所有控制器的抽象父类。它提供：

- **实体校验** —— `checkDeviceId()`、`checkAssetId()`、`checkTenantId()`、`checkUserId()` 等：
  取出实体、检查权限，然后返回它
- **通用实体检查** —— `checkEntityId(EntityId, Operation)`：按 `EntityType` 分派到正确的 check 方法
- **CRUD 日志** —— `doSaveAndLog()`、`doDeleteAndLog()`：包装实体的保存/删除，经
  `EntityActionService` 自动记审计日志
- **用户辅助方法** —— `getCurrentUser()`、`getTenantId()`：从 Spring Security 上下文中取出
  `SecurityUser`
- **分页** —— `createPageLink()`、`createTimePageLink()`：从请求参数构造 `PageLink` 对象
- **异常处理** —— `@ExceptionHandler` 方法，把异常转成结构化的 `ThingsboardException` 响应
- **全部 DAO 与 service 依赖** —— 约 50 个 `@Autowired` 字段，覆盖 DAO service、实体 service、
  缓存 service、队列 producer 等

### AbstractRpcController

`AbstractRpcController.java` 继承 `BaseController`，为 `RpcV1Controller` 和 `RpcV2Controller`
提供 RPC 专属功能。

## 控制器编目

### 实体 CRUD

| 控制器 | URL 路径 | 实体 | 权限 |
|------------|----------|--------|------|
| `DeviceController` | `/api/device/**` | Device | TENANT_ADMIN、CUSTOMER_USER |
| `AssetController` | `/api/asset/**` | Asset | TENANT_ADMIN、CUSTOMER_USER |
| `AlarmController` | `/api/alarm/**` | Alarm | TENANT_ADMIN、CUSTOMER_USER |
| `AlarmCommentController` | `/api/alarm/{alarmId}/comment/**` | AlarmComment | TENANT_ADMIN、CUSTOMER_USER |
| `AlarmRuleController` | `/api/alarm/rule/**` | AlarmRule | TENANT_ADMIN |
| `EntityViewController` | `/api/entityView/**` | EntityView | TENANT_ADMIN、CUSTOMER_USER |
| `EntityRelationController` | `/api/relation/**` | EntityRelation | TENANT_ADMIN、CUSTOMER_USER |
| `CustomerController` | `/api/customer/**` | Customer | TENANT_ADMIN |
| `TenantController` | `/api/tenant/**` | Tenant | SYS_ADMIN |
| `UserController` | `/api/user/**` | User | SYS_ADMIN、TENANT_ADMIN |
| `DashboardController` | `/api/dashboard/**` | Dashboard | TENANT_ADMIN、CUSTOMER_USER |
| `WidgetTypeController` | `/api/widgetType/**` | WidgetType | TENANT_ADMIN |
| `WidgetsBundleController` | `/api/widgetsBundle/**` | WidgetsBundle | TENANT_ADMIN |
| `EdgeController` | `/api/edge/**` | Edge | TENANT_ADMIN |
| `OtaPackageController` | `/api/otaPackage/**` | OtaPackage | TENANT_ADMIN |
| `TbResourceController` | `/api/resource/**` | TbResource | TENANT_ADMIN |
| `AiModelController` | `/api/ai/model/**` | AiModel | TENANT_ADMIN |
| `ApiKeyController` | `/api/apiKey/**` | ApiKey | TENANT_ADMIN |
| `CalculatedFieldController` | `/api/calculatedField/**` | CalculatedField | TENANT_ADMIN |
| `DomainController` | `/api/domain/**` | Domain | TENANT_ADMIN |
| `MobileAppController` | `/api/mobileApp/**` | MobileApp | TENANT_ADMIN、CUSTOMER_USER |
| `MobileAppBundleController` | `/api/mobileAppBundle/**` | MobileAppBundle | TENANT_ADMIN |
| `NotificationTargetController` | `/api/notification/target/**` | NotificationTarget | TENANT_ADMIN |
| `NotificationTemplateController` | `/api/notification/template/**` | NotificationTemplate | TENANT_ADMIN |
| `NotificationRuleController` | `/api/notification/rule/**` | NotificationRule | TENANT_ADMIN |
| `NotificationController` | `/api/notification/**` | Notification | TENANT_ADMIN、CUSTOMER_USER |
| `JobController` | `/api/job/**` | Job | TENANT_ADMIN |
| `MailConfigTemplateController` | `/api/mail/config/template/**` | MailConfigTemplate | SYS_ADMIN |
| `OAuth2ConfigTemplateController` | `/api/oauth2/config/template/**` | OAuth2ConfigTemplate | SYS_ADMIN |

### 配置文件（Profile）

| 控制器 | URL 路径 | 实体 | 权限 |
|------------|----------|--------|------|
| `DeviceProfileController` | `/api/deviceProfile/**` | DeviceProfile | TENANT_ADMIN |
| `AssetProfileController` | `/api/assetProfile/**` | AssetProfile | TENANT_ADMIN |
| `TenantProfileController` | `/api/tenantProfile/**` | TenantProfile | SYS_ADMIN |

### 用户管理

| 控制器 | URL 路径 | 实体 | 权限 |
|------------|----------|--------|------|
| `UserController` | `/api/user/**` | User | SYS_ADMIN、TENANT_ADMIN |
| `CustomerController` | `/api/customer/**` | Customer | TENANT_ADMIN |
| `TenantController` | `/api/tenant/**` | Tenant | SYS_ADMIN |
| `AdminController` | `/api/admin/**` | 系统管理 | SYS_ADMIN |

### 系统

| 控制器 | URL 路径 | 用途 | 权限 |
|------------|----------|---------|------|
| `SystemInfoController` | `/api/system/**` | 系统信息、版本 | 任何已认证用户 |
| `QueueController` | `/api/queues/**` | 队列管理 | SYS_ADMIN |
| `QueueStatsController` | `/api/queueStats/**` | 队列统计 | SYS_ADMIN |

### 通信

| 控制器 | URL 路径 | 用途 | 权限 |
|------------|----------|---------|------|
| `TelemetryController` | `/api/plugins/telemetry/**` | 时序与属性数据 | TENANT_ADMIN、CUSTOMER_USER |
| `RpcV1Controller` | `/api/rpc/**` | RPC（v1） | TENANT_ADMIN、CUSTOMER_USER |
| `RpcV2Controller` | `/api/rpc/v2/**` | RPC（v2，持久化） | TENANT_ADMIN、CUSTOMER_USER |
| `EntityQueryController` | `/api/entitiesQuery/**` | 实体数据查询 | TENANT_ADMIN、CUSTOMER_USER |

### 规则引擎与组件配置

| 控制器 | URL 路径 | 用途 | 权限 |
|------------|----------|---------|------|
| `RuleChainController` | `/api/ruleChain/**` | 规则链 CRUD | TENANT_ADMIN |
| `RuleEngineController` | `/api/rule-engine/**` | 规则引擎管理 | TENANT_ADMIN |
| `ComponentDescriptorController` | `/api/components/**` | 组件描述符编目 | TENANT_ADMIN |

### 认证与 OAuth2

| 控制器 | URL 路径 | 用途 | 权限 |
|------------|----------|---------|------|
| `AuthController` | `/api/auth/**`、`/api/noauth/**` | 登录、登出、密码重置、激活 | 混合 |
| `OAuth2Controller` | `/api/oauth2/**` | OAuth2 客户端管理 | SYS_ADMIN（配置）、TENANT_ADMIN（登录） |
| `TwoFactorAuthController` | `/api/2fa/**` | 双因子认证 | TENANT_ADMIN、CUSTOMER_USER |
| `TwoFactorAuthConfigController` | `/api/2fa/config/**` | 2FA 配置 | TENANT_ADMIN |

### 其他

| 控制器 | URL 路径 | 用途 | 权限 |
|------------|----------|---------|------|
| `AuditLogController` | `/api/audit/logs/**` | 审计日志查询 | SYS_ADMIN、TENANT_ADMIN |
| `EdgeEventController` | `/api/edge/event/**` | Edge 事件管理 | TENANT_ADMIN |
| `EventController` | `/api/events/**` | 调试事件 | TENANT_ADMIN |
| `ImageController` | `/api/image/**` | 图片上传/下载 | TENANT_ADMIN |
| `UiSettingsController` | `/api/ui/settings/**` | UI 设置 | TENANT_ADMIN、CUSTOMER_USER |
| `UsageInfoController` | `/api/usage/**` | API 用量信息 | TENANT_ADMIN |
| `DeviceConnectivityController` | `/api/device-connectivity/**` | 设备连通性证书 | 公开 |
| `Lwm2mController` | `/api/lwm2m/**` | LwM2M 设备管理 | TENANT_ADMIN |
| `QrCodeSettingsController` | `/api/qr-code/settings/**` | 二维码设置 | TENANT_ADMIN |
| `TrendzController` | `/api/trendz/**` | Trendz 分析 | TENANT_ADMIN |
| `AutoCommitController` | `/api/auto-commit/**` | 自动提交设置 | SYS_ADMIN |
| `EntitiesVersionControlController` | `/api/entities/vc/**` | 实体版本控制 | TENANT_ADMIN |

### WebSocket 插件

`controller/plugin/` 子包含 WebSocket 处理类：

| 类 | 用途 |
|-------|---------|
| `TbWebSocketHandler` | WebSocket 连接处理器，服务实时遥测、告警和通知 |
| `TbWebSocketMsg` | WebSocket 消息基类 |
| `TbWebSocketMsgType` | WebSocket 消息类型枚举 |
| `TbWebSocketPingMsg` | 保活用的 ping 消息 |
| `TbWebSocketTextMsg` | 文本消息载荷 |

## URL 规律

所有控制器都在类级别使用 `@RequestMapping("/api")`。实体端点的 URL 路径前缀遵循如下约定：

| URL 形态 | 实体类型 |
|-------------|-------------|
| `/api/device/**` | Device |
| `/api/asset/**` | Asset |
| `/api/alarm/**` | Alarm |
| `/api/customer/**` | Customer |
| `/api/tenant/**` | Tenant |
| `/api/user/**` | User |
| `/api/edge/**` | Edge |
| `/api/dashboard/**` | Dashboard |
| `/api/ruleChain/**` | RuleChain |
| `/api/deviceProfile/**` | DeviceProfile |
| `/api/assetProfile/**` | AssetProfile |
| `/api/tenantProfile/**` | TenantProfile |
| `/api/entityView/**` | EntityView |
| `/api/relation/**` | EntityRelation |
| `/api/widgetType/**` | WidgetType |
| `/api/widgetsBundle/**` | WidgetsBundle |
| `/api/otaPackage/**` | OtaPackage |
| `/api/resource/**` | TbResource |
| `/api/queues/**` | Queue |

免认证端点走 `/api/noauth/**`（密码重置、账号激活）。设备 API 端点走 `/api/v1/**`。

## 控制器的通用模式

### 标准 CRUD 端点

每个实体控制器通常提供：
- `GET /api/{entity}/{id}` —— 按 ID 取单个
- `GET /api/{entity}/info/{id}` —— 取 info（含关联实体名）
- `POST /api/{entity}` —— 新建或更新（upsert）
- `DELETE /api/{entity}/{id}` —— 删除
- `GET /api/tenant/{entity}s` —— 列租户实体（分页）
- `GET /api/tenant/{entity}Infos` —— 列租户实体 info（分页）
- `GET /api/customer/{customerId}/{entity}s` —— 列客户实体（分页）

### 授权

控制器使用 `@PreAuthorize` 注解做基于角色的访问控制：
- `hasAuthority('SYS_ADMIN')` —— 仅系统管理员
- `hasAuthority('TENANT_ADMIN')` —— 仅租户管理员
- `hasAuthority('CUSTOMER_USER')` —— 仅客户用户
- `hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')` —— 租户管理员或客户用户

权限检查使用 `Operation` 枚举值：`READ`、`WRITE`、`CREATE`、`DELETE`、`ASSIGN_TO_CUSTOMER`、
`UNASSIGN_FROM_CUSTOMER`、`CLAIM_DEVICES`、`READ_CREDENTIALS`、`WRITE_CREDENTIALS` 等。

> **注意：`@PreAuthorize` 对同类内调用不生效。** Spring 的方法级安全靠代理实现，因此控制器内部
> 直接调用本类另一个方法时注解不会触发。把权限检查当成安全网之前，先确认调用是从外部进来的。
> 参见 `BaseController` 里显式的 `checkEntityId()` 调用——它们才是真正的强制点。

### 审计日志

所有实体变更都会经 `BaseController.doSaveAndLog()` 和 `doDeleteAndLog()` 自动记日志，这两个方法
调用 `EntityActionService.logEntityAction()`，并记录用户、实体、动作类型（ADDED、UPDATED、
DELETED）以及任何异常。

### 异步响应

长耗时操作（RPC、设备认领）使用 `DeferredResult<T>` 配 `ListenableFuture` 回调，让请求线程在操作
异步完成期间被释放。

## 相关文档

- [README](README.zh-CN.md) —— Application 模块总览
- [服务](services.zh-CN.md) —— 控制器委派到的 service 层
- [配置与安全](config-and-security.zh-CN.md) —— 安全架构与 JWT 认证
