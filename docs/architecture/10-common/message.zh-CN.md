# Common Message（`common/message`）

**本文是权威版。** 英文版 [message.md](message.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:message`

`common/message` 模块定义了 actor 之间、规则引擎节点之间、队列 topic、集群节点和传输服务之间通信
所用的全部内部消息类型。它是 ThingsBoard 的通信骨干。

## 架构

消息在系统中分若干层流转：

1. **Transport 到 Core** —— 设备遥测、属性、RPC 响应从 transport 作为 `TransportProtos` 消息抵达，
   发布到 Kafka topic
2. **Core 到规则引擎** —— `TbMsg` 实例被路由穿过规则链进行处理
3. **Core 到 Transport** —— RPC 请求与属性更新回流到设备
4. **集群广播** —— 实体状态变化、配置文件更新、缓存失效在集群范围内广播
5. **Edge 同步** —— Edge 实例接收实体更新并上送遥测

## 主要类型

### TbMsg —— 核心消息

`TbMsg` 是流经规则引擎的基础消息类型。它携带：

| 字段 | 类型 | 用途 |
|-------|------|---------|
| `id` | `UUID` | 唯一消息标识 |
| `ts` | `long` | 时间戳（epoch 毫秒） |
| `type` | `String` | 可读类型（如 `POST_TELEMETRY`、`POST_ATTRIBUTES_REQUEST`） |
| `internalType` | `TbMsgType` | 基于枚举的内部类型 |
| `originator` | `EntityId` | 来源实体（设备、资产等） |
| `customerId` | `CustomerId` | 关联的客户 |
| `metaData` | `TbMsgMetaData` | 键值元数据映射 |
| `dataType` | `TbMsgDataType` | 载荷类型（JSON、proto、text） |
| `data` | `String` | JSON 载荷 |
| `queueName` | `String` | 路由目标队列 |

`TbMsg` 是不可变的（final，经 Lombok `@Data`）。元数据装在 `TbMsgMetaData` 里 —— 一个简单的键值
容器，带有若干取类型化值的辅助方法。

### TbMsgType

`TbMsgType` 枚举为规则链路由对消息做分类。例如：
- `POST_TELEMETRY_REQUEST` —— 设备上报遥测
- `POST_ATTRIBUTES_REQUEST` —— 设备上报属性
- `TO_SERVER_RPC_REQUEST` —— 设备到服务端的 RPC 调用
- `RPC_CALL_FROM_SERVER_TO_DEVICE` —— 服务端到设备的 RPC
- `ACTIVITY_EVENT` —— 设备上线/下线/活动
- `INACTIVITY_EVENT` —— 设备不活动超时
- `LC_EVENT` —— 生命周期事件
- `ERROR_EVENT` —— 错误事件
- `ENTITY_CREATED`、`ENTITY_UPDATED`、`ENTITY_DELETED` —— 实体生命周期事件
- `ALARM_*` —— 告警生命周期事件

### Actor 消息

`TbActorMsg` 是 actor 间所有交换消息的标记接口。主要消息类：

| 类 | 用途 |
|-------|---------|
| `TbActorMsg` | 所有 actor 消息的基接口 |
| `TbRuleEngineActorMsg` | 发往规则引擎 actor 的消息（包装 TbMsg） |
| `ToDeviceActorNotificationMsg` | 发往设备 actor 的通知（edge 更新、改名、RPC） |
| `CalculatedFieldStatePartitionRestoreMsg` | 计算字段状态恢复 |
| `ToCalculatedFieldSystemMsg` | 发往计算字段 actor 系统的消息 |
| `TbActorStopReason` | actor 停止原因的枚举 |

### 子包结构

| 子包 | 用途 | 主要文件 |
|-----------|---------|-----------|
| （根） | 核心消息类型 | `TbMsg.java`、`TbMsgMetaData.java`、`TbMsgDataType.java`、`TbMsgProcessingCtx.java` |
| `queue/` | 队列专属消息 | `QueueToRuleEngineMsg.java`、`TbMsgCallback.java`、`TopicPartitionInfo.java` |
| `cluster/` | 集群通信 | `ClusterEventMsg.java`、`ServerAddress.java` |
| `rule/` | 规则引擎消息 | 规则节点输入/输出消息类型 |
| `session/` | 传输会话消息 | `AdaptorToSessionActorMsg.java`、`BasicTransportToDeviceSessionActorMsg.java` |
| `rpc/` | RPC 消息 | `FromDeviceRpcResponse.java`、`ToDeviceRpcRequest.java` |
| `edge/` | Edge 同步消息 | `EdgeEventUpdateMsg.java`、`EdgeHighPriorityMsg.java`、`FromEdgeSyncResponse.java`、`ToEdgeSyncRequest.java` |
| `plugin/` | 插件生命周期 | `ComponentLifecycleMsg.java` |
| `aware/` | 上下文感知消息 | `TenantAwareMsg.java`、`CustomerAwareMsg.java` |
| `cf/` | 计算字段消息 | `CalculatedFieldLinkedTelemetryMsg.java` |
| `gateway/` | 网关消息 | 网关相关消息类型 |
| `housekeeper/` | Housekeeper 任务消息 | `HousekeeperTask.java` |
| `notification/` | 通知消息 | 通知触发类型 |
| `edqs/` | EDQS 消息 | EDQS 专属消息类型 |
| `timeout/` | 超时消息 | 空闲/超时 actor 消息 |
| `tools/` | 消息工具 | 辅助类 |

## 消息生命周期

1. **创建** —— transport 从设备原始数据创建 `TbMsg`（protobuf -> TbMsg）
2. **入队** —— `TbMsg` 经 protobuf 序列化后发布到某个 Kafka 分区（以实体 ID 为 key）
3. **消费** —— 规则引擎消费者从 Kafka 读取并反序列化
4. **处理** —— 链上每个规则节点变换 `TbMsg`（元数据、data、type、originator）
5. **输出** —— 处理后的 `TbMsg` 被重新发布到输出队列或被提交（存时序、创建告警等）

## 依赖关系

- **依赖：** `common/data`（实体 ID、类型）、`common/proto`（protobuf 序列化）
- **被谁依赖：** `common/actor`、`common/queue`、`common/cluster-api`、`common/script`、
  `rule-engine`、`application`、`transport/*`
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `message`
