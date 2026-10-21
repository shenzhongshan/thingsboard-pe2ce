# Common Queue（`common/queue`）

**本文是权威版。** 英文版 [queue.md](queue.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:queue`

`common/queue` 模块为 ThingsBoard 各服务之间的消息传递提供队列抽象层。它支持多种后端
（Kafka、内存、pub-sub），并负责分区发现、消费者管理和消息序列化。

## 架构

队列系统在服务类型之间路由消息：
- **Transport -> Core** —— 设备遥测、属性更新、认证请求
- **Core -> 规则引擎** —— 供规则链处理的 TbMsg 流
- **Core -> Transport** —— RPC 命令、属性更新、会话通知
- **Core -> EDQS** —— 实体数据查询事件与请求
- **Core -> 版本控制** —— Git 提交请求
- **Core -> Core** —— 节点间集群通信（广播、RPC）

### 关键抽象

| 接口 | 用途 |
|-----------|---------|
| `TbQueueProducer<T>` | 向某个 topic 生产消息 |
| `TbQueueConsumer<T>` | 从某个 topic 消费消息 |
| `TbQueueRequestTemplate<REQ,RES>` | 基于队列的请求-应答模式 |
| `TbQueueResponseTemplate<REQ,RES>` | 请求-应答的应答侧 |
| `TbQueueMsg` | 包装原始字节 + key + headers 的信封 |
| `TbQueueCallback` | 生产/消费完成的异步回调 |
| `TbQueueClusterService` | 集群感知的队列操作 |
| `TbQueueAdmin` | topic/分区管理 |

### 消息生命周期

1. **编码** —— 生产者把消息体序列化成 `byte[]`（通常是 protobuf）
2. **分区** —— 对消息 key（实体 UUID）做哈希以决定目标分区
3. **发布** —— 经 `TbQueueProducer.send()` 异步发布
4. **消费** —— `QueueConsumerManager` 在一个线程池上轮询各分区
5. **解码** —— 消费者把字节反序列化回类型化消息
6. **处理** —— 消息被派发给相应的处理方（actor、service 等）

### 分区策略

消息按实体 ID（基于 UUID 的 key）分区。这保证某个设备、租户或规则链的所有消息会有序地落在同一个
分区上。`PartitionService` 管理把租户/实体映射到分区的一致性哈希环。集群拓扑变化时，
`PartitionChangeEvent` 触发分区再均衡。

## 子包结构

| 子包 | 用途 | 主要文件 |
|-----------|---------|-----------|
| `common/` | 队列模板的抽象实现 | `AbstractTbQueueConsumerTemplate.java`、`AbstractParallelTbQueueConsumerTemplate.java`、`DefaultTbQueueMsg.java`、`DefaultTbQueueRequestTemplate.java` |
| `common/consumer/` | 消费者管理 | `QueueConsumerManager.java`、`PartitionedQueueConsumerManager.java`、`MainQueueConsumerManager.java`、`QueueTaskType.java` |
| `common/state/` | 队列状态跟踪 | `QueueStateService.java`、`DefaultQueueStateService.java`、`KafkaQueueStateService.java` |
| `discovery/` | 服务发现与分区分配 | `DiscoveryService.java`、`PartitionService.java`、`HashPartitionService.java`、`ConsistentHashCircle.java` |
| `discovery/event/` | 集群拓扑事件 | `ClusterTopologyChangeEvent.java`、`PartitionChangeEvent.java`、`ServiceListChangedEvent.java` |
| `kafka/` | Kafka 专属实现 | Kafka 的 producer、consumer、admin 实现 |
| `pubsub/` | Google Cloud Pub/Sub 后端 | Pub/Sub 的 producer 与 consumer 实现 |
| `memory/` | 内存队列后端 | 供测试与单节点部署使用的内存 producer/consumer |
| `rabbitmq/` | RabbitMQ 后端 | RabbitMQ 的 producer 与 consumer 实现 |
| `aws-sqs/` | AWS SQS 后端 | SQS 的 producer 与 consumer 实现 |
| `azure-service-bus/` | Azure Service Bus 后端 | Azure 的 producer 与 consumer 实现 |

### 队列后端

| 后端 | 激活 profile | 适用场景 |
|---------|---------------|----------|
| Kafka | 默认（生产） | 多节点集群、持久化消息 |
| 内存 | `queue-type=in-memory` | 单节点测试、开发 |
| Pub/Sub | `queue-type=pubsub` | GCP 部署 |
| RabbitMQ | `queue-type=rabbitmq` | 基于 RabbitMQ 的部署 |
| AWS SQS | `queue-type=aws-sqs` | AWS 部署 |
| Azure Service Bus | `queue-type=azure-service-bus` | Azure 部署 |

### DefaultTbQueueMsg

标准的队列消息信封：

```java
@Data
public class DefaultTbQueueMsg implements TbQueueMsg {
    private final UUID key;       // Partition key (entity UUID)
    private final byte[] data;    // Serialized payload
    private final DefaultTbQueueMsgHeaders headers;  // Metadata headers
}
```

### 消费者管理

`QueueConsumerManager` 是所有消费者生命周期管理的基类。它负责：
- 创建和管理消费者线程（池大小可配）
- 处理分区分配/再均衡事件
- 提供带排空的优雅关闭
- 上报消费延迟与健康指标

特化的管理器：
- `MainQueueConsumerManager` —— 标准的每 topic 单消费者
- `PartitionedQueueConsumerManager` —— 多消费者做分区化处理

### 服务发现

`discovery/` 子包与服务发现机制（ZooKeeper 或 Kubernetes）集成。关键组件：
- `HashPartitionService` —— 用一致性哈希计算哪个节点拥有哪些分区
- `ConsistentHashCircle` —— 一致性哈希环实现
- `DiscoveryService` —— 服务注册表查询接口
- `DummyDiscoveryService` —— 单节点模式下的空实现
- `DefaultTbServiceInfoProvider` —— 提供本地服务元数据用于注册

## Protobuf 集成

队列消息用 protobuf 序列化。主要包装类：
- `TbProtoQueueMsg<T>` —— 通用 protobuf 消息包装
- `TbProtoJsQueueMsg` —— JS executor 消息包装（JS invoke protobuf）

## Kafka 专属架构

Kafka 后端使用：
- **按服务类型分 topic** —— `tb_core`、`tb_rule_engine`、`tb_transport`、`tb_edqs`、
  `tb_version_control` 等
- **按租户分区** —— 实现租户级隔离与并行
- **消费者组** —— 每个服务实例都是自己消费者组里的成员
- **手动提交 offset** —— 在处理成功之后提交，而非之前
- **状态跟踪** —— `KafkaQueueStateService` 按节点跟踪分区 offset

## 依赖关系

- **依赖：** `common/data`、`common/message`、`common/cluster-api`、`common/discovery-api`、`common/stats`
- **被谁依赖：** `application`、`transport/*`、`msa/*`
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `queue`
