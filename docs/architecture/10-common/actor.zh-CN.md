# Common Actor（`common/actor`）

**本文是权威版。** 英文版 [actor.md](actor.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:actor`

`common/actor` 模块提供受 Akka actor 模型启发的 actor 系统抽象。ThingsBoard 中每个设备、租户、
规则链和计算字段都作为一个隔离的 actor 运行，在自己的线程上下文中异步处理消息。

## 架构

### Actor 模型

actor 是一种轻量的并发原语：每个 actor 有一个邮箱（入站消息队列），并一次一条、顺序处理消息。
这免去了加锁的必要，并天然提供隔离。actor 系统负责 actor 生命周期、消息路由和故障恢复。

关键概念：
- **Actor** —— 拥有私有状态和一个邮箱的计算单元
- **ActorRef** —— actor 的句柄（用于发送消息）
- **ActorSystem** —— 管理 actor、dispatcher 和邮箱的运行时
- **Dispatcher** —— 执行 actor 的线程池
- **Mailbox** —— 每个 actor 的消息队列

## 核心接口与类

| 类/接口 | 用途 |
|----------------|---------|
| `TbActor` | 核心 actor 接口：`process(TbActorMsg)` 返回 true 表示消息已被处理 |
| `TbActorRef` | 用于发送消息的 actor 引用/句柄 |
| `TbActorCtx` | actor 上下文 —— 提供自身引用、父引用、创建子 actor、访问系统 |
| `TbActorId` | actor 标识（有类型、可比较） |
| `TbActorSystem` | actor 系统生命周期管理（创建、停止、查找 actor） |
| `TbActorCreator` | 创建 actor 实例的工厂 |
| `TbActorSystemSettings` | actor 系统配置（线程池、dispatcher） |
| `TbActorMailbox` | 每个 actor 的消息队列 |
| `Dispatcher` | 为 actor 分配线程 dispatcher |
| `InitFailureStrategy` | actor 初始化失败如何处理（延迟重试、停止） |
| `ProcessFailureStrategy` | 消息处理失败如何处理（继续、停止） |
| `TbActorNotRegisteredException` | 引用不存在的 actor 时抛出的异常 |
| `TbActorException` | actor 系统异常基类 |
| `TbRuleNodeUpdateException` | 规则节点更新失败的异常 |

### Actor 类型

actor 由其 `TbActorId` 标识：

| Actor ID 类 | ID 类型 | 用于 |
|---------------|---------|---------|
| `TbEntityActorId` | 基于 EntityId | 设备、租户、规则链、资产 actor |
| `TbStringActorId` | 基于 String | 具名 actor（系统服务、统计） |
| `TbCalculatedFieldEntityActorId` | 基于 CalculatedField | 计算字段求值 actor |

### Actor 生命周期

```
Create -> Init -> Process (loop) -> Destroy
                    |
            (failure handling)
                    |
          InitFailureStrategy / ProcessFailureStrategy
```

1. **Create** —— `TbActorCreator.createActor()` 实例化 actor
2. **Init** —— 调用 `TbActor.init(TbActorCtx)`；actor 建立自身状态
3. **Process** —— 消息经 `TbActor.process(TbActorMsg)` 抵达；顺序处理
4. **Destroy** —— `TbActor.destroy(TbActorStopReason, Throwable)` 清理资源
5. **Failure** —— `InitFailureStrategy` 与 `ProcessFailureStrategy` 定义重试/停止行为

### DefaultTbActorSystem

`DefaultTbActorSystem` 是主要实现（位于 `application/`）：

```java
public class DefaultTbActorSystem implements TbActorSystem {
    // Manages actor registry, dispatchers, mailboxes, schedulers
    // Routes messages to actors based on TbActorId
}
```

### 失败策略

```java
// Initialization failure: retry with exponential backoff
InitFailureStrategy.retryWithDelay(5000L * attempt);

// Processing failure: skip the message and continue
ProcessFailureStrategy.resume();

// Processing failure: stop the actor entirely
ProcessFailureStrategy.stop();
```

### 邮箱与派发

每个 actor 有专属的 `TbActorMailbox`。`Dispatcher` 把 actor 分配到某个线程池（如固定 N 线程的池）。
同一 dispatcher 上的 actor 共享线程，但每个 actor 内部仍保持顺序处理。系统支持多个 dispatcher 以
隔离负载（例如设备 actor 与规则链 actor 各用独立 dispatcher）。

### Actor 层级

actor 构成一棵树：
- **系统根** —— `TbActorSystem`
  - **App actor** —— 每个服务组件一个
    - **租户 actor** —— 每租户一个
      - **规则链 actor** —— 每租户每规则链一个
      - **设备 actor** —— 每租户每设备一个
      - **资产 actor** —— 每资产一个
    - **统计 actor** —— 系统级统计

消息沿层级路由。`TbActorCtx` 提供按 ID 查找子 actor、以及向父/子 actor 发送（tell）消息的方法。

## AbstractTbActor

`AbstractTbActor` 提供了带通用模式的基础实现：

```java
public abstract class AbstractTbActor implements TbActor {
    protected TbActorCtx ctx;

    @Override
    public void init(TbActorCtx ctx) {
        this.ctx = ctx;
    }

    // Subclasses override process()
}
```

## 依赖关系

- **依赖：** `common/data`（以及来自 `common/message` 的 TbActorMsg 类型）
- **被谁依赖：** `application`（actor 实现）、`common/message`（TbActorMsg 接口）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `actor`
