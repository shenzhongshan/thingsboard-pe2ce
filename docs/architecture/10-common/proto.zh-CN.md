# Common Proto（`common/proto`）

**本文是权威版。** 英文版 [proto.md](proto.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:proto`

`common/proto` 模块定义服务间通信的 Protobuf 消息 schema。这些 schema 被 Kafka 队列序列化、gRPC
服务定义和传输层使用。

## Proto 文件

| 文件 | 包 | 生成的 Java 类 | 用途 |
|------|---------|---------------------|---------|
| `queue.proto` | `msgqueue` | `MsgProtos` | 队列消息信封、TbMsg 的 protobuf 序列化 |
| `transport.proto` | `transport` | `TransportProtos` | 全部传输层消息、服务发现、core 通信 |
| `jsinvoke.proto` | `js` | `JsInvokeProtos` | 远程 JS executor 通信 |

此外，`common/edge-api/` 下的 `edge.proto` 定义 Edge 的 gRPC 服务与消息类型
（见 [edge-api.zh-CN.md](edge-api.zh-CN.md)）。

### queue.proto

定义 `TbMsg` 用于 Kafka 序列化的 protobuf 表示：

- `TbMsgProto` —— 完整的 TbMsg 序列化，含 UUID、type、实体 ID、元数据映射、customer ID、
  规则链/节点 ID 和 data 载荷
- `TbMsgMetaDataProto` —— 键值映射序列化
- `TbMsgProcessingStackItemProto` —— 用于环路检测的规则节点处理栈

### transport.proto

最庞大的 proto 文件（1934 行）。定义服务之间交换的全部消息。主要部分：

#### 实体类型 Proto
`EntityTypeProto` 枚举含 35+ 个值，映射到 `EntityType`（TENANT、CUSTOMER、USER、DASHBOARD、ASSET、
DEVICE、ALARM、RULE_CHAIN、RULE_NODE、ENTITY_VIEW、WIDGETS_BUNDLE、WIDGET_TYPE、TENANT_PROFILE、
DEVICE_PROFILE、ASSET_PROFILE、API_USAGE_STATE、TB_RESOURCE、OTA_PACKAGE、EDGE、RPC、QUEUE、
NOTIFICATION*、OAUTH2_CLIENT、DOMAIN、MOBILE_APP、MOBILE_APP_BUNDLE、CALCULATED_FIELD、JOB、
ADMIN_SETTINGS、AI_MODEL、API_KEY）。

#### 服务发现
- `ServiceInfo` —— 节点元数据（服务 ID、类型、transport、系统信息）
- `SystemInfoProto` —— CPU、内存、磁盘占用

#### Transport-Service 消息
- `SessionInfoProto` —— 设备会话状态
- `PostTelemetryMsg` / `PostAttributeMsg` —— 设备数据上传
- `GetAttributeRequestMsg` / `GetAttributeResponseMsg` —— 属性拉取
- `ToDeviceRpcRequestMsg` / `ToDeviceRpcResponseMsg` —— RPC 命令
- `ValidateDeviceTokenRequestMsg` / `ValidateDeviceX509CertRequestMsg` —— 认证
- `ProvisionDeviceRequestMsg` / `ProvisionDeviceResponseMsg` —— 设备开通
- `ClaimDeviceMsg` —— 设备认领
- `GetOtaPackageRequestMsg` / `GetOtaPackageResponseMsg` —— OTA 固件
- `SubscribeToAttributeUpdatesMsg` / `SubscribeToRPCMsg` —— 订阅
- `SessionCloseNotificationProto` —— 会话终止

#### Core 到 Core 的消息
- `ToCoreMsg` —— 从 transport 发往 core 的消息（设备会话、遥测、RPC、实体订阅）
- `ToCoreNotificationMsg` —— 高优先级 core 通知（RPC 响应、队列更新、生命周期事件）
- `ToRuleEngineMsg` —— 转发给规则引擎的消息
- `ToTransportMsg` —— 从 core 发往 transport 的消息（RPC、属性更新、实体变更）
- `SubscriptionMgrMsgProto` —— 时序/属性/告警订阅管理
- `ComponentLifecycleMsgProto` —— 实体生命周期事件广播（CREATED、UPDATED、DELETED 等）

#### Edge 消息
- `EdgeNotificationMsgProto` —— Edge 实体同步通知
- `ToEdgeSyncRequestMsgProto` / `FromEdgeSyncResponseMsgProto` —— Edge 同步协议
- `EdgeEventUpdateMsgProto` / `EdgeHighPriorityMsgProto` —— Edge 状态更新
- `ToEdgeMsg` / `ToEdgeNotificationMsg` —— 发往 edge 队列的消息

#### 版本控制消息
- `CommitRequestMsg` / `CommitResponseMsg` —— Git 提交操作
- `ListVersionsRequestMsg` / `ListVersionsResponseMsg` —— 版本历史列举
- `EntityContentRequestMsg` / `EntityContentResponseMsg` —— 实体内容获取
- `VersionsDiffRequestMsg` / `VersionsDiffResponseMsg` —— 版本间差异
- `ToVersionControlServiceMsg` —— 版本控制服务请求信封

#### 计算字段
- `CalculatedFieldTelemetryMsgProto` —— 计算字段遥测
- `CalculatedFieldStateProto` —— 计算字段求值状态（单值参数、滚动、地理围栏、聚合）
- `ToCalculatedFieldMsg` / `ToCalculatedFieldNotificationMsg` —— 计算字段消息

#### EDQS 消息
- `ToEdqsMsg` / `FromEdqsMsg` —— EDQS 事件与请求消息
- `EdqsEventMsg` / `EdqsRequestMsg` / `EdqsResponseMsg` —— EDQS 协议

#### 其他
- `ToUsageStatsServiceMsg` —— API 用量统计
- `ToOtaPackageStateServiceMsg` —— OTA 状态更新
- `NotificationSchedulerServiceMsg` —— 通知调度
- `HousekeeperTaskProto` —— 数据清理任务
- `JobStatsMsg` / `TaskResultProto` —— Job 执行跟踪
- `AlarmStateProto` —— 计算字段告警规则状态

#### 顶层消息路由
```protobuf
message TransportApiRequestMsg { ... }   // Transport -> Core
message TransportApiResponseMsg { ... }   // Core -> Transport
message ToCoreMsg { ... }                // Transport/Core -> Core
message ToCoreNotificationMsg { ... }     // High-priority core notifications
message ToRuleEngineMsg { ... }           // Core -> Rule Engine
message ToTransportMsg { ... }            // Core/RE -> Transport
message ToEdgeMsg { ... }                 // Core -> Edge
message ToVersionControlServiceMsg { ... } // Core -> Version Control
```

### jsinvoke.proto

定义与远程 JS executor（Node.js sidecar）通信的协议：

- `RemoteJsRequest` —— 包含 compile/invoke/release 子请求
- `RemoteJsResponse` —— 包含 compile/invoke/release 子响应
- `JsCompileRequest` / `JsCompileResponse` —— 脚本编译
- `JsInvokeRequest` / `JsInvokeResponse` —— 脚本执行（函数名、参数、超时）
- `JsReleaseRequest` / `JsReleaseResponse` —— 脚本释放/清理
- `JsInvokeErrorCode` —— COMPILATION_ERROR、RUNTIME_ERROR、TIMEOUT_ERROR、NOT_FOUND_ERROR

## 生成的代码

Protobuf 编译产出的 Java 类位于：
- `org.thingsboard.server.gen.transport.TransportProtos`
- `org.thingsboard.server.gen.transport.TransportApiProtos`
- `org.thingsboard.server.gen.transport.MsgProtos`
- `org.thingsboard.server.gen.edge.v1.EdgeProtos`
- `org.thingsboard.server.gen.js.JsInvokeProtos`

## 依赖关系

- **依赖：** common 内部无依赖（独立的 proto 定义）
- **被谁依赖：** `common/message`、`common/queue`、`common/script`、`common/edge-api`、
  `application`、`transport/*`、`rule-engine`
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `proto`
