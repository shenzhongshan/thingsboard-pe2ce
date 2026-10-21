# Actor 系统

**本文是权威版。** 英文版 [actors.md](actors.md) 为对照上游 CE 的参照副本，改动请先落在本文。

Actor 系统是 ThingsBoard 应用的核心异步处理引擎。它基于一套类 Akka 的 actor 模型，实现在
`org.thingsboard.server.actors` 包以及 `common/actor` 模块的共享抽象中。

## Actor 模型概念

ThingsBoard 的 actor 系统使用以下核心抽象（定义在 `common/actor` 模块）：

| 接口/类 | 用途 |
|-----------------|---------|
| `TbActor` | Actor 实现 —— 接收消息、维护状态、处理消息 |
| `TbActorRef` | Actor 的引用/句柄 —— 用于发送消息 |
| `TbActorMsg` | 所有 actor 间消息的基接口 |
| `TbActorCtx` | Actor 上下文 —— 提供 `tell()`、`broadcastToChildren()`、自引用等 |
| `TbActorId` | Actor 的唯一标识（子类型：`TbEntityActorId`、`TbStringActorId`） |
| `TbActorCreator` | 创建 actor 实例的工厂 |
| `TbActorSystem` | 运行时容器 —— 管理 dispatcher、创建 actor、调度消息 |
| `TbActorSystemSettings` | 配置：吞吐量、调度器池大小、最大初始化尝试次数 |

## Actor 层级

Actor 系统组织成一棵树：

```
AppActor (root)
  |
  +-- StatsActor (root-level parallel actor)
  |
  +-- TenantActor (one per tenant)
        |
        +-- RuleChainManagerActor (per-tenant rule chain management)
        |     |
        |     +-- RuleChainActor (one per rule chain)
        |           |
        |           +-- RuleNodeActor (one per rule node in the chain)
        |
        +-- DeviceActor (one per device)
        |
        +-- CalculatedFieldManagerActor (per-tenant calculated field management)
              |
              +-- CalculatedFieldEntityActor (one per calculated field entity)
```

## Actor 类型

### AppActor

`AppActor.java` —— 系统的根 actor，是所有入站消息的入口。职责：

- **启动** —— 在 Spring 上下文就绪后收到 `AppInitMsg`，随后初始化全部租户 actor
- **消息路由** —— 按消息的 `TenantId` 把消息转发给正确的 `TenantActor`
- **分区变更** —— 集群分区变化时向所有子 actor 广播 `PartitionChangeMsg`
- **组件生命周期** —— 处理插件/组件启停事件的 `ComponentLifecycleMsg`
- **会话超时** —— 周期性检查过期的设备会话（仅在 TB-CORE 模式下运行时）

由 `DefaultActorService.initActorSystem()` 在 `app-dispatcher` 上创建。

### StatsActor

`StatsActor.java` —— 采集并持久化系统统计。作为 `AppActor` 的根级同层 actor 创建。

关键消息：
- `StatsPersistMsg` —— 触发把统计写入数据库
- `StatsPersistTick` —— 统计采集的周期性 tick

### TenantActor

`TenantActor.java` —— 每个租户一个实例，是所有租户级处理的中枢 actor，继承
`RuleChainManagerActor`。职责：

- **设备管理** —— 为每台设备创建并管理 `DeviceActor` 实例
- **规则链管理**（经父类 `RuleChainManagerActor`）—— 创建并管理 `RuleChainActor` 实例
- **计算字段管理** —— 管理 `CalculatedFieldManagerActor`
- **消息路由** —— 把 `DeviceAwareMsg` 路由到正确的 `DeviceActor`，`RuleChainAwareMsg` 路由到正确的
  `RuleChainActor`
- **Edge 处理** —— 为 edge 同步跟踪已删除的设备
- **API 用量状态** —— 缓存租户的 API 用量状态以供限流使用

### DeviceActor

`DeviceActor.java` —— 每台设备一个实例，管理设备会话状态与消息处理。`actors/device/` 下的关键文件：

| 文件 | 用途 |
|------|---------|
| `DeviceActor.java` | 主 actor —— 处理设备消息 |
| `DeviceActorCreator.java` | 创建 DeviceActor 实例的工厂 |
| `DeviceActorMessageProcessor.java` | 处理设备的入站消息 |
| `SessionInfo.java` / `SessionInfoMetaData.java` | 传输会话的跟踪数据 |
| `SessionTimeoutCheckMsg.java` | 周期性会话超时检查的触发器 |
| `ToDeviceRpcRequestMetadata.java` / `ToServerRpcRequestMetadata.java` | RPC 请求元数据 |
| `TransportSessionCloseReason.java` | 会话关闭原因的枚举 |

### 规则链相关 actor

位于 `actors/ruleChain/`：

| 文件 | 用途 |
|------|---------|
| `RuleChainActor.java` | 单条规则链实例的 actor |
| `RuleChainActorMessageProcessor.java` | 处理流经一条规则链的消息 |
| `RuleChainManagerActor.java` | 管理某租户的全部规则链 —— 创建与销毁 `RuleChainActor` 实例 |
| `RuleNodeActor.java` | 链中单个规则节点的 actor |
| `RuleNodeActorMessageProcessor.java` | 在规则节点处处理消息 |
| `RuleNodeCtx.java` | 规则节点的执行上下文 |
| `RuleNodeRelation.java` | 定义规则节点之间的关系（连线） |
| `DefaultTbContext.java` | 规则引擎上下文的默认实现 |
| `RuleEngineComponentActor.java` | 规则引擎组件的基 actor |
| `RuleChainInputMsg.java` / `RuleChainOutputMsg.java` | 输入/输出消息类型 |
| `RuleChainToRuleChainMsg.java` | 规则链之间路由的消息 |
| `RuleChainToRuleNodeMsg.java` | 从链路由到某个具体节点的消息 |
| `RuleNodeToRuleChainTellNextMsg.java` | 从节点发给链的"tell next"逻辑的消息 |
| `RuleNodeToSelfMsg.java` | 规则节点内部的自寻址消息 |
| `TbToRuleChainActorMsg.java` / `TbToRuleNodeActorMsg.java` | 发给链/节点 actor 的外部消息 |

### 计算字段相关 actor

位于 `actors/calculatedField/`：

| 文件 | 用途 |
|------|---------|
| `CalculatedFieldManagerActor.java` | 计算字段 actor 的租户级管理器 |
| `CalculatedFieldEntityActor.java` | 在某个具体实体上求值计算字段的 actor |
| `AbstractCalculatedFieldActor.java` | 含通用计算字段逻辑的基类 |
| `CalculatedFieldEntityMessageProcessor.java` | 处理计算字段求值消息 |
| `CalculatedFieldTelemetryMsg.java` | 计算字段的遥测输入触发器 |
| `CalculatedFieldLinkedTelemetryMsg.java` | 来自被关联/被引用实体的遥测 |
| `CalculatedFieldAlarmActionMsg.java` | 计算字段的告警类触发器 |
| `CalculatedFieldRelationActionMsg.java` | 关系变更触发器 |
| `CalculatedFieldReevaluateMsg.java` | 周期性重算触发器 |
| `CalculatedFieldStateRestoreMsg.java` | 分区变更时的状态恢复 |

## Actor 生命周期与监管

### 创建

Actor 经 `Creator` 内部类模式创建。每个 actor 类都定义一个继承 `ContextBasedCreator` 的静态
`ActorCreator`：

```java
public static class ActorCreator extends ContextBasedCreator<DeviceActor> {
    private final DeviceId deviceId;
    public ActorCreator(ActorSystemContext context, DeviceId deviceId) {
        super(context);
        this.deviceId = deviceId;
    }
    @Override
    public DeviceActor createActor() {
        return new DeviceActor(context, deviceId);
    }
}
```

`TbActorSystem.createRootActor()` 方法创建根 actor（AppActor、StatsActor）。子 actor 由其父 actor
通过 `TbActorCtx.createChild()` 方法创建。

### 初始化

Actor 创建后会调用 `init(TbActorCtx)`。actor 在这里：
- 调度周期性消息
- 初始化状态
- 调用 `super.init(ctx)` 以保存上下文引用

### 消息处理

Actor 实现 `TbActor` 接口的 `doProcess(TbActorMsg)` 方法。该方法：
1. 经 `msg.getMsgType()` 判断消息类型（一个枚举，含 `APP_INIT_MSG`、`PARTITION_CHANGE_MSG`、
   `COMPONENT_LIFE_CYCLE_MSG`、`QUEUE_TO_RULE_ENGINE_MSG`、`TRANSPORT_TO_DEVICE_ACTOR_MSG` 等）
2. 路由到相应的处理器
3. 返回 `true`（消息已处理）或 `false`（消息未被处理）

> **注意：给 `MsgType` 加枚举值时，必须同时检查所有以它做分派键的 switch。** 带
> `default -> throw` 的 switch 会在编译全绿的情况下于运行时炸掉——编译通过不能证明分派完备。

### 终止

Actor 经 `destroy(TbActorStopReason)` 停止。`TbActorStopReason` 是枚举，取值包括 `STOPPED`
（优雅停止）、`INIT_FAILED`（初始化失败）和 `DELETED`（实体已删除）。

### 错误处理

Actor 抛出 `TbActorException` 时，故障按如下方式处理：
1. 经 `ActorSystemContext.persistError()` 记录错误
2. 由父 actor 决定是重启还是停止该子 actor

## 消息路由

### Dispatcher

Actor 系统使用具名 dispatcher，每个背后都是一个 `ExecutorService`：

| Dispatcher 名 | 默认池大小 | 用途 |
|-----------------|-------------------|---------|
| `app-dispatcher` | 1 | 根 AppActor |
| `tenant-dispatcher` | 2 | TenantActor 实例 |
| `device-dispatcher` | 4 | DeviceActor 实例 |
| `rule-dispatcher` | 8 | RuleChainActor 与 RuleNodeActor 实例 |
| `cf-manager-dispatcher` | 2 | CalculatedFieldManagerActor 实例 |
| `cf-entity-dispatcher` | 8 | CalculatedFieldEntityActor 实例 |

池大小经 `thingsboard.yml` 中 `actors.system.*_dispatcher_pool_size` 属性配置。

### 消息流

1. 队列消费者（`DefaultTbCoreConsumerService`、`DefaultTbRuleEngineConsumerService`）从
   Kafka/pub-sub 接收消息
2. 消费者把消息反序列化成 `TbActorMsg` 的子类型
3. 消息经 `ActorSystemContext.tell(msg)` 或 `tellWithHighPriority(msg)` 送入 actor 系统 ——
   两者都路由到 `AppActor`
4. `AppActor` 判定目标 `TenantId` 并转发到正确的 `TenantActor`
5. `TenantActor` 按消息类型路由到 `DeviceActor`、`RuleChainActor` 或
   `CalculatedFieldManagerActor`
6. `RuleChainActor` 沿规则链 DAG 把 `TbMsg` 在 `RuleNodeActor` 实例间路由

## Actor 上下文与状态

### ActorSystemContext

`ActorSystemContext.java` 是一个 Spring `@Component`，充当所有 actor 的共享上下文。它提供：

- **DAO 访问** —— 注入全部 DAO service（DeviceService、AssetService、TenantService 等）
- **Service 访问** —— 全部 service 层 bean（邮件、短信、遥测、RPC、edge 等）
- **配置值** —— 来自 `thingsboard.yml` 的 actor 系统设置
- **工具方法** —— `persistError()`、`persistLifecycleEvent()`、`persistDebugInput()`、
  `persistDebugOutput()`
- **调度** —— `schedulePeriodicMsgWithDelay()` 与 `scheduleMsgWithDelay()`，用于定时消息
- **分区解析** —— `resolve()` 系列方法，判定由哪个分区处理某条消息

### 共享 actor 组件

位于 `actors/shared/`：

| 文件 | 用途 |
|------|---------|
| `AbstractContextAwareMsgProcessor.java` | 可访问 ActorSystemContext 的消息处理器基类 |
| `ActorTerminationMsg.java` | Actor 正在终止时发出的消息 |
| `ComponentMsgProcessor.java` | 组件级消息处理器的接口 |
| `RuleChainErrorActor.java` | 处理规则链处理过程中的错误 |

### Service 类

位于 `actors/service/`：

| 文件 | 用途 |
|------|---------|
| `ActorService.java` | Actor 系统生命周期的接口 |
| `DefaultActorService.java` | 实现 —— 初始化 actor 系统、创建根 actor、处理分区变更 |
| `ContextAwareActor.java` | 需要 ActorSystemContext 的 actor 的基类 |
| `ContextBasedCreator.java` | 传递上下文的 actor creator 基类 |
| `ComponentActor.java` | 组件级 actor 的基类 |

## 关键配置属性

均可在 `thingsboard.yml` 的 `actors` 前缀下配置：

```yaml
actors:
  session:
    max_concurrent_sessions_per_device: 1
    sync.timeout: 10000
  rule:
    chain.error_persist_frequency: 3000
    node.error_persist_frequency: 3000
    external.force_ack: false
    allow_system_mail_service: true
    allow_system_sms_service: true
  statistics:
    enabled: true
    persist_frequency: 3600000
  rpc:
    submit_strategy: BURST
    close_session_on_rpc_delivery_timeout: false
    response_timeout_ms: 30000
    max_retries: 5
  tenant:
    create_components_on_init: true
  system:
    throughput: 5
    max_actor_init_attempts: 10
    scheduler_pool_size: 1
    app_dispatcher_pool_size: 1
    tenant_dispatcher_pool_size: 2
    device_dispatcher_pool_size: 4
    rule_dispatcher_pool_size: 8
    cfm_dispatcher_pool_size: 2
    cfe_dispatcher_pool_size: 8
```

注意 dispatcher 名与配置键不同形：`cf-manager-dispatcher` 对应 `cfm_dispatcher_pool_size`，
`cf-entity-dispatcher` 对应 `cfe_dispatcher_pool_size`。按 dispatcher 名去 yml 里搜是搜不到的。

## 相关文档

- [README](README.zh-CN.md) —— Application 模块总览
- [服务](services.zh-CN.md) —— 与 actor 系统交互的 service
- [配置与安全](config-and-security.zh-CN.md) —— 安全配置
