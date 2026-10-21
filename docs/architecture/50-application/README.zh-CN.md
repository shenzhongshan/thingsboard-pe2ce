# Application 模块

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

`application` 模块是 ThingsBoard 的主 Spring Boot 3.5 应用服务端。它提供 REST API、WebSocket
端点、actor 系统、service 层、安全，以及全部服务端业务逻辑。它含 949 个 Java 源文件、分布在 7 个
顶层包中，是 ThingsBoard 单仓里最大也最复杂的模块。

## 入口

应用入口是 `ThingsboardServerApplication`，位于：

```
application/src/main/java/org/thingsboard/server/ThingsboardServerApplication.java
```

它用的是 `@SpringBootConfiguration`（**不是** `@SpringBootApplication`），并显式加了以下注解：
- `@EnableAsync` —— 启用经 `@Async` 的异步方法执行
- `@EnableScheduling` —— 启用 `@Scheduled` 周期任务
- `@ComponentScan({"org.thingsboard.server", "org.thingsboard.script"})` —— 扫描主应用包与脚本
  引擎包

`main()` 方法经 `SpringApplication.run()` 启动，并在未显式指定时自动追加
`--spring.config.name=thingsboard`，以确保加载 `thingsboard.yml` 配置文件。

应用启动耗时由 `@AfterStartUp` 钩子以 `Ordered.LOWEST_PRECEDENCE` 记录。

## 包结构

| 包 | 用途 | 主要文件 |
|---------|---------|-----------|
| `actors/` | Actor 系统 —— 租户、设备、规则链、计算字段和统计 actor | `ActorSystemContext.java`、`TenantActor.java`、`DeviceActor.java`、`AppActor.java`、`StatsActor.java` |
| `config/` | Spring `@Configuration` 类 —— 安全、web、CORS、调度、Swagger、WebSocket、加密、限流 | `ThingsboardSecurityConfiguration.java`、`WebConfig.java`、`SchedulingConfiguration.java` |
| `controller/` | REST API 控制器（63 个控制器类） | `BaseController.java`、`AuthController.java`、`DeviceController.java`、`TenantController.java` |
| `exception/` | 自定义异常类型与错误响应处理 | `DataValidationException.java`、`ThingsboardErrorResponseHandler.java` |
| `install/` | 数据库 schema 创建、升级、迁移和初始数据加载 | `DefaultSystemDataLoaderService.java`、`SqlEntityDatabaseSchemaService.java` |
| `service/` | service 层（44 个 service 包）—— 实体 service、系统 service、集成 service | 见 [services.zh-CN.md](services.zh-CN.md) |
| `utils/` | 应用级工具类 | 各类辅助工具 |

## 应用如何消费队列消息

应用与 ThingsBoard 队列系统（基于 Kafka 或 pub-sub）集成以异步处理消息。关键的消费者 service 在
`service/queue/` 下：

- **`TbCoreConsumerService`** —— 消费 TB-CORE 服务类型的消息（REST API、WebSocket、设备状态管理）
- **`TbRuleEngineConsumerService`** —— 消费 TB-RULE-ENGINE 的消息（规则链处理）
- **`TbEdgeConsumerService`** —— 消费 edge 同步的消息
- **`TbCalculatedFieldConsumerService`** —— 消费计算字段求值的消息

消息从队列抵达，被反序列化成 `TbActorMsg` 的子类型，再经 `ActorSystemContext.tell()` 或
`tellWithHighPriority()` 路由到相应的 actor。路由层级如下：

```
Queue Message -> Consumer Service -> ActorSystemContext.tell() -> AppActor -> TenantActor -> DeviceActor / RuleChainActor
```

`PartitionService` 决定由哪个分区/节点处理某条消息，`PartitionChangeEvent` 触发重新分区。

## Spring Profile 与条件装配

应用大量使用条件装配以支持不同部署模式：

- **`@TbCoreComponent`** —— 标记在 TB-CORE 模式（REST API + actor）下才激活的 bean
- 经 `TbServiceInfoProvider.isService(ServiceType.TB_CORE)` 或 `TB_RULE_ENGINE` 做服务类型判断
- 可选依赖用 `@Autowired(required = false)` 配 `@Lazy`（如 Edge service、TbelInvokeService、
  Cassandra 集群、Redis template）
- 可被覆盖的 bean 用 `@ConditionalOnMissingBean`（如 CorsFilter）
- 数据库类型经 `thingsboard.yml` 属性选择：`DATABASE_TS_TYPE`、`DATABASE_TS_LATEST_TYPE`
- 队列类型经 `TB_QUEUE_TYPE` 选择（kafka、pub-sub 等）
- 缓存类型选择：`caffeine`（本地）对 Redis

## 对其他模块的主要依赖

| 模块 | 依赖内容 |
|--------|------------|
| `common/data` | 实体模型（Device、Asset、Alarm、Tenant、User 等）、DTO、ID、枚举 |
| `common/message` | 内部消息类型（`TbMsg`、`TbActorMsg`、队列消息） |
| `common/queue` | 队列基础设施（producer、consumer、发现、分区） |
| `common/dao-api` | DAO 接口（数据访问的 service 契约） |
| `common/cache` | 基于 Caffeine 的缓存工具 |
| `common/cluster-api` | 集群通信接口 |
| `common/actor` | Actor 系统抽象（`TbActor`、`TbActorRef`、`TbActorSystem`） |
| `common/transport` | 传输层共享类（会话、凭证） |
| `dao` | 数据访问实现（SQL 与 NoSQL） |
| `rule-engine/rule-engine-api` | 规则引擎接口（`MailService`、`SmsService` 等） |
| `rule-engine/rule-engine-components` | 内置规则节点实现 |
| `netty-mqtt` | 供内部 MQTT 客户端操作使用的 MQTT 编解码 |

## 线程模型与异步处理

应用使用多种线程策略：

1. **Actor 系统 dispatcher** —— actor 系统为每个 dispatcher 创建专属的 `ExecutorService`
   （work-stealing 池）：
   - `app-dispatcher`（默认 1 线程）
   - `tenant-dispatcher`（默认 2 线程）
   - `device-dispatcher`（默认 4 线程）
   - `rule-dispatcher`（默认 8 线程）
   - `cf-manager-dispatcher`（默认 2 线程）
   - `cf-entity-dispatcher`（默认 8 线程）

2. **专属 executor service** —— 位于 `service/executors/`：
   - `DbCallbackExecutorService` —— 数据库回调执行
   - `ExternalCallExecutorService` —— 外部 HTTP/调用执行
   - `NotificationExecutorService` —— 通知处理
   - `SharedEventLoopGroupService` —— Netty event loop group
   - `PubSubRuleNodeExecutorProvider` —— pub-sub 规则节点执行
   - `VersionControlExecutor` —— 版本控制操作

3. **Spring `@Async`** —— 经 `@EnableAsync` 启用，用于异步方法执行

4. **DeferredResult** —— 控制器用 `DeferredResult<T>` 做长轮询与异步 HTTP 响应，经
   `DonAsynchron.withCallback()` 包装

5. **定时任务** —— 经 `@EnableScheduling` 做周期操作（统计上报、会话超时检查、housekeeping）

## 相关文档

- [控制器](controllers.zh-CN.md) —— REST API 控制器编目
- [服务](services.zh-CN.md) —— service 层编目
- [Actors](actors.zh-CN.md) —— actor 系统文档
- [配置与安全](config-and-security.zh-CN.md) —— 配置与安全架构
