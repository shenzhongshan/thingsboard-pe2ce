# Common Data（`common/data`）

**本文是权威版。** 英文版 [data.md](data.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:data`

`common/data` 是最大的公共模块。它定义了 ThingsBoard 各层用到的全部实体模型、数据传输对象（DTO）、
实体 ID、枚举、常量和校验规则，是整个项目的基石依赖。

## 架构

所有类都位于基础包 `org.thingsboard.server.common.data` 下。模块按领域关注点划分子包，每个主要
实体类型都有自己的子包，内含实体类、相关 DTO 和配套类型。

### 实体 ID 体系

ThingsBoard 中每个实体都由一个基于 UUID 的 ID 标识，该 ID 实现 `EntityId` 接口：

```java
public interface EntityId extends HasUUID, Serializable {
    UUID getId();
    EntityType getEntityType();
}
```

`EntityIdFactory` 由 `EntityType` 枚举与 UUID 的组合反序列化出 ID。`EntityId` 接口用自定义的
`@JsonDeserialize`/`@JsonSerialize` 注解（`EntityIdDeserializer`、`EntityIdSerializer`）实现多态
JSON 序列化。

`EntityType` 枚举定义了 30+ 种实体类型，包括：`TENANT`、`CUSTOMER`、`USER`、`DEVICE`、`ASSET`、
`DASHBOARD`、`ALARM`、`RULE_CHAIN`、`RULE_NODE`、`DEVICE_PROFILE`、`ASSET_PROFILE`、
`TENANT_PROFILE`、`ENTITY_VIEW`、`EDGE`、`WIDGETS_BUNDLE`、`WIDGET_TYPE`、`TB_RESOURCE`、
`OTA_PACKAGE`、`NOTIFICATION`、`NOTIFICATION_TARGET`、`NOTIFICATION_TEMPLATE`、
`NOTIFICATION_REQUEST`、`NOTIFICATION_RULE`、`QUEUE`、`QUEUE_STATS`、`RPC`、`API_USAGE_STATE`、
`OAUTH2_CLIENT`、`DOMAIN`、`MOBILE_APP`、`MOBILE_APP_BUNDLE`、`CALCULATED_FIELD`、`JOB`、
`ADMIN_SETTINGS`、`AI_MODEL`、`API_KEY`。

### DTO 模式

实体类（如 `Device`、`Asset`、`Dashboard`）是带 Jackson 注解的可序列化 Java record/POJO，代表持久化
模型。DTO 与实体是分开的 —— 请求类 DTO（如 `DeviceInfo`）只包含 API 输入/输出所需的字段，从而让
内部模型与 REST 契约解耦。

## 包结构

### 核心实体子包

| 子包 | 说明 | 主要文件 |
|-----------|-------------|-----------|
| `device/` | 设备实体、凭证 | `Device.java`、`DeviceInfo.java`、`DeviceTransportType.java` |
| `asset/` | 资产实体 | `Asset.java`、`AssetInfo.java` |
| `alarm/` | 告警实体、评论 | `Alarm.java`、`AlarmComment.java`、`AlarmSeverity.java` |
| `tenant/` | 租户实体、配置文件 | `Tenant.java`、`TenantProfile.java`、`DefaultTenantProfileConfiguration.java` |
| `customer/` | 客户实体 | `Customer.java`、`CustomerInfo.java` |
| `user/` | 用户实体、凭证 | `User.java`、`UserCredentials.java` |
| `dashboard/` | 仪表板实体 | `Dashboard.java`、`DashboardInfo.java` |
| `rule/` | 规则链、规则节点实体 | `RuleChain.java`、`RuleNode.java`、`RuleChainType.java` |
| `edge/` | Edge 实体 | `Edge.java`、`EdgeInfo.java` |
| `entityview/` | 实体视图实体 | `EntityView.java`、`EntityViewInfo.java` |
| `widget/` | Widget 类型与 bundle | `WidgetType.java`、`WidgetsBundle.java` |
| `resource/` | 资源实体（JS、图片、CSS） | `TbResource.java`、`TbResourceInfo.java` |
| `ota/` | OTA 包实体 | `OtaPackage.java`、`OtaPackageInfo.java` |
| `notification/` | 通知实体 | 通知目标、模板、规则、请求 |
| `ai/` | AI 模型实体 | `AiModel.java` |
| `domain/` | 域名实体 | `Domain.java` |
| `mobile/` | 移动应用实体 | `MobileApp.java`、`MobileAppBundle.java` |

### 支撑子包

| 子包 | 说明 | 主要文件 |
|-----------|-------------|-----------|
| `id/` | 50+ 个实体 ID 类 | `EntityId.java`、`DeviceId.java`、`TenantId.java`、`EntityIdFactory.java` |
| `kv/` | 遥测用的键值数据类型 | `BaseTsKvQuery.java`、`TsKvEntry.java`、`AttributeKvEntry.java` |
| `query/` | 查询定义 | `EntityDataQuery.java`、`EntityCountQuery.java`、`TsValue.java` |
| `relation/` | 实体关系类型 | `EntityRelation.java`、`RelationTypeGroup.java` |
| `rpc/` | RPC 消息类型 | `Rpc.java`、`RpcStatus.java` |
| `event/` | 事件类型 | `Event.java`、`EventType.java` |
| `exception/` | 公共异常 | `ThingsboardException.java`、`ThingsboardErrorCode.java` |
| `page/` | 分页 DTO | `PageData.java`、`TimePageData.java`、`TimePageLink.java` |
| `audit/` | 审计日志类型 | `AuditLog.java`、`ActionType.java` |
| `security/` | 安全 DTO | `Authority.java`、`UserCredentials.java` |
| `oauth2/` | OAuth2 配置类型 | `OAuth2ClientInfo.java`、`OAuth2ParamsInfo.java` |
| `permission/` | 权限定义 | `PermissionChecker.java` |
| `transport/` | 传输配置 DTO | 设备传输配置枚举 |
| `script/` | 脚本元数据类型 | `ScriptLanguage.java` |
| `settings/` | 管理设置 DTO | `AdminSettings.java` |
| `sms/` | 短信配置 | `SmsProviderConfiguration.java` |
| `mail/` | 邮件配置 | `MailConfiguration.java` |
| `queue/` | 队列配置 | `QueueConfig.java`、`QueueInfo.java` |
| `job/` | Job 实体 | `Job.java`、`JobInfo.java` |
| `pat/` | 个人访问令牌类型 | `PersonalAccessToken.java` |
| `plugin/` | 插件生命周期类型 | `ComponentLifecycleEvent.java` |
| `housekeeper/` | 数据清理（housekeeping）类型 | `HousekeeperTask.java` |
| `limit/` | 限流类型 | `RateLimits.java` |
| `sync/` | 版本控制同步类型 | `GitSyncSettings.java` |
| `trendz/` | Trendz 分析类型 | 分析相关的数据类 |
| `cf/` | 计算字段类型 | `CalculatedField.java`、`CalculatedFieldType.java` |
| `edqs/` | EDQS 专属数据类型 | EDQS 实体数据类 |
| `lwm2m/` | LwM2M 传输类型 | LwM2M 专属的设备配置文件配置 |
| `debug/` | 调试类型 | 调试配置类型 |
| `validation/` | 校验工具 | 内置校验器 |
| `util/` | 数据工具类 | 数据辅助类 |

## 枚举

本模块定义的主要枚举：
- `EntityType` —— 系统中的全部实体类型
- `DeviceTransportType` —— `DEFAULT`、`MQTT`、`HTTP`、`COAP`、`LWM2M`、`SNMP`
- `AlarmSeverity` —— `CRITICAL`、`MAJOR`、`MINOR`、`WARNING`、`INDETERMINATE`
- `ComponentLifecycleEvent` —— `CREATED`、`STARTED`、`ACTIVATED`、`SUSPENDED`、`UPDATED`、`STOPPED`、`DELETED`、`FAILED`、`DEACTIVATED`
- `EntityViewType` —— 视图行为类型
- `ScriptLanguage` —— `TBEL`、`JS`
- `Authority` —— `SYS_ADMIN`、`TENANT_ADMIN`、`CUSTOMER_USER`

## 依赖关系

- **依赖：** 无（不依赖任何其他公共模块）—— 它是公共模块的根
- **被谁依赖：** 项目中其余所有模块（`common/message`、`common/cache`、`common/dao-api`、
  `common/queue`、`common/actor`、`common/cluster-api`、`common/script`、`common/transport`、
  `dao`、`application`、`rule-engine`、全部 transport）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `data`
