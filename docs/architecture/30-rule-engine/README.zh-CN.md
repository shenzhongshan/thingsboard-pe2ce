# 规则引擎

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

ThingsBoard 规则引擎是一条可配置的数据处理流水线，基于规则节点构成的有向无环图（DAG）。它处理进入
系统的每一条消息 —— 遥测、属性更新、RPC 调用、设备生命周期事件 —— 并把它们路由穿过用户定义的节点
链，在链上完成过滤、转换、富化和执行动作。

## 架构

### 规则链即 DAG

每条**规则链**都是一个有向无环图：
- **节点**（顶点）是规则节点实例 —— 实现 `TbNode` 的 Java 类。
- **边**通过带类型的关系连接节点（如 `"Success"`、`"Failure"`、`"True"`、`"False"` 或自定义关系类型）。
- 节点和边都以 JSON 形式存在 SQL 数据库里（`rule_chain.configuration` 和
  `rule_node.configuration` 列）。

一条规则链的定义形如：
```json
{
  "nodes": [
    {
      "id": "uuid",
      "type": "org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode",
      "name": "Filter Telemetry",
      "configuration": { ... },
      "additionalInfo": { "layoutX": 100, "layoutY": 200 }
    }
  ],
  "connections": [
    {
      "fromIndex": 0,
      "toIndex": 1,
      "type": "True"
    }
  ]
}
```

### 消息生命周期

1. **来源**：设备经某个 transport（MQTT、HTTP、CoAP 等）发送遥测/属性/RPC，或由内部事件触发。
2. **队列**：transport 把输入转成 `TbMsg` 并推到 Kafka topic。
3. **Actor 派发**：应用从 Kafka 消费，把消息路由到相应的**规则链 actor**。
4. **根节点**：规则链 actor 从**第一个规则节点**（"input" 节点）开始处理。
5. **节点遍历**：每个节点收到 `TbMsg`，执行自身逻辑，再经
   `ctx.tellNext(msg, relationType)` 或 `ctx.tellFailure(msg)` 告知链 actor 走哪条关系。
6. **输出**：若消息抵达 "rule chain output" 节点，则被转发给下一条规则链（或根链结束）。

### `TbMsg` —— 消息对象

`TbMsg` 是流经规则链的不可变消息对象。主要字段：
- `id` —— 唯一消息 UUID
- `type` —— 消息类型（如 `POST_TELEMETRY_REQUEST`、`POST_ATTRIBUTES_REQUEST`、
  `ATTRIBUTES_UPDATED`、`RPC_CALL_FROM_SERVER_TO_DEVICE`）
- `originator` —— 触发该消息的实体 ID（设备、资产等）
- `customerId` —— 关联的客户 ID（可为 null）
- `data` —— JSON 字符串形式的载荷（遥测、属性、RPC 参数）
- `metaData` —— 键值元数据映射（设备名、设备类型、附加上下文）

### `TbMsgMetaData`

附加在每条消息上的键值映射。节点可以读取、新增或修改元数据字段。常见元数据字段：`deviceName`、
`deviceType`、`ts`（时间戳）、`scope`（属性用）、`requestId`（RPC 用）。

## 规则节点类型

`@RuleNode` 注解按 `ComponentType` 为每个节点分类：

| 类型 | 用途 | 示例 |
|------|---------|----------|
| **FILTER** | 基于数据检查有条件地路由消息 | 消息类型过滤、发起者类型过滤、JS 过滤、检查告警状态 |
| **ENRICHMENT** | 取额外数据并附加到消息上 | 取属性、取设备详情、取租户详情、取凭证 |
| **TRANSFORMATION** | 修改消息载荷、元数据或发起者 | 脚本转换、改变发起者、重命名/删除 key、JSON path |
| **ACTION** | 执行副作用 | 创建告警、清除告警、存时序、存属性、发邮件、RPC 调用、记日志 |
| **EXTERNAL** | 对接外部系统 | REST API 调用、Kafka 发布、MQTT 发布、RabbitMQ、AWS SNS/SQS/Lambda、GCP Pub/Sub、发短信 |
| **FLOW** | 控制消息在链间的流转 | input 节点（入口）、output 节点（出口到父链）、checkpoint、确认 |

## 规则引擎 API

规则引擎 API 位于 `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/`。

### `TbNode` —— 核心接口

```java
public interface TbNode {
    void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;
    void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException;
    default void destroy() {}
    default void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {}
    default TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException;
}
```

- **`init()`** —— 节点创建时调用一次。接收自己的 JSON 配置（从 `rule_node.configuration` 反序列化）。
- **`onMsg()`** —— 每条抵达该节点的消息都会调用。节点处理消息后调用 `ctx.tellNext()`、
  `ctx.tellFailure()`，或经 `ctx.newMsg()` 创建新消息。
- **`destroy()`** —— 节点被移除时做清理。
- **`onPartitionChangeMsg()`** —— 处理集群部署下的分区重分配。
- **`upgrade()`** —— 把旧版本的配置迁移上来。

### `TbContext` —— 访问各项服务

`TbContext` 让节点能访问全部平台服务：

```
ctx.getDeviceService()       -> DeviceService
ctx.getAssetService()        -> AssetService
ctx.getAlarmService()        -> AlarmService
ctx.getTelemetryService()    -> TelemetryService
ctx.getRpcService()          -> RPC service
ctx.getMailService()         -> Email sending
ctx.getSmsService()          -> SMS sending
ctx.getNotificationCenter()  -> Notifications
// ... 40+ services available
```

消息路由方法：
- `ctx.tellNext(msg, relationType)` —— 沿指定关系转发给下一个节点
- `ctx.tellFailure(msg, error)` —— 沿 "Failure" 关系转发
- `ctx.newMsg(msg)` —— 创建新消息（用于产生新的 TbMsg）
- `ctx.ack(msg)` —— 确认消息已处理
- `ctx.getCallback(msg)` —— 取异步操作用的回调

### `@RuleNode` 注解

```java
@RuleNode(
    type = ComponentType.FILTER,
    name = "message type switch",
    configClazz = EmptyNodeConfiguration.class,
    relationTypes = {"Success"},
    nodeDescription = "Route incoming messages by Message Type",
    nodeDetails = "...",
    uiResources = {"static/rulenode/rulenode-core-config.js"},
    configDirective = "tbEnrichedNodeConfig"
)
```

该注解把节点注册到组件扫描器。主要属性：
- `type` —— `ComponentType` 枚举值（FILTER、ENRICHMENT、TRANSFORMATION、ACTION、EXTERNAL、FLOW）
- `configClazz` —— 用于反序列化该节点 JSON 配置的配置类
- `relationTypes` —— 该节点可能产生的输出关系类型集合
- `customRelations` —— 为 true 时允许用户自定义关系名
- `ruleChainNode` —— 为 true 时表示该节点代表一个子链链接
- `version` —— 配置版本，用于支持升级迁移

### `NodeDefinition`

运行时由 `@RuleNode` 注解数据构造。UI 用它渲染规则节点面板和配置表单。包含：
- `details`、`description` —— 人类可读文本
- `inEnabled`、`outEnabled` —— 该节点是否有输入/输出端点
- `defaultConfiguration` —— 默认 JSON 配置
- `uiResources`、`configDirective` —— UI 渲染提示

### 主要 API 接口

| 接口 | 用途 |
|-----------|---------|
| `TbNode` | 规则节点核心生命周期：init、process、destroy |
| `TbContext` | 节点的服务访问 + 消息路由 |
| `NodeConfiguration` | 节点配置对象的标记基类 |
| `TbNodeConfiguration` | 包装 JSON 配置并提供类型化访问 |
| `TbNodeState` | 有状态节点的持久化状态（序列化为 JSON） |
| `NodeDefinition` | 供 UI 组件面板使用的节点元数据 |
| `RuleEngineDeviceProfileCache` | 供规则引擎使用的设备配置文件缓存 |
| `RuleEngineAssetProfileCache` | 供规则引擎使用的资产配置文件缓存 |
| `RuleEngineTelemetryService` | 规则节点发起的遥测操作 |
| `RuleEngineAlarmService` | 规则节点发起的告警操作 |
| `RuleEngineRpcService` | 规则节点发起的 RPC 操作 |
| `MailService` | 邮件发送接口 |
| `SmsService` / `SmsSender` / `SmsSenderFactory` | 短信发送 |
| `ScriptEngine` | JS/Python 脚本执行接口 |
| `NotificationCenter` / `SlackService` / `FirebaseService` | 通知渠道 |

## 与 Actor 系统的集成

每条规则链背后都有一个 actor（`RuleChainActor`）。actor 系统负责：

1. **按链顺序处理消息** —— 单条规则链内同一时刻只处理一条消息，从而保证该链内行为确定。
2. **隔离租户的链** —— 每个租户有自己的根规则链 actor，加上各子链 actor。
3. **通过队列系统支撑并发** —— 多个规则链 actor 在集群中并行运行。
4. **持久化节点状态** —— 有状态节点（去重、checkpoint、延迟）经数据库里的 `RuleNodeState`
   记录持久化状态。

### 处理流程

```
Transport -> Kafka Queue -> RuleChainActorMessageProcessor
                                |
                                v
                        RuleChainActor
                                |
              +-----------------+------------------+
              |                                    |
         TbNode.onMsg()                       TbNode.onMsg()
              |                                    |
         ctx.tellNext()                       ctx.tellNext()
              |                                    |
              v                                    v
         Next Node                            Next Node
              |                                    |
         ... (continues through DAG)        ...
              |
         ctx.tellNext() with "Output" relation type
              |
              v
         Forward to parent chain (or end)
```

## 组件体系

规则节点组件位于 `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/`，
含 **29 个分类目录**、**223 个 Java 文件**。

组件在启动时经 `@RuleNode` 注解自动发现，并注册到 `component_descriptor` 表。UI 读该表来构建规则
节点面板。

每个组件通常由两部分构成：
- 一个 `Tb...Node` 类（实现 `TbNode`）
- 一个 `Tb...NodeConfiguration` 类（实现 `NodeConfiguration`）

## 相关文档

- [规则节点编目](components.zh-CN.md) —— 全部规则节点分类与实现的详细编目
- [DAO 层总览](../20-dao/README.zh-CN.md) —— 规则节点如何访问持久化数据
- [实体 DAO 编目](../20-dao/entity-dao.zh-CN.md) —— 规则引擎服务使用的实体 DAO
