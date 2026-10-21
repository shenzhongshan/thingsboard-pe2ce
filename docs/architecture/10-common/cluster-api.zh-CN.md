# Common Cluster API（`common/cluster-api`）

**本文是权威版。** 英文版 [cluster-api.md](cluster-api.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:cluster-api`

`common/cluster-api` 模块定义节点间集群通信的接口。它提供服务发现、集群节点间基于队列的消息传递，
以及实体状态变更广播的 API 契约。具体实现位于 `application/` 模块。

## 架构

ThingsBoard 以节点集群的形式运行，每个节点跑一个或多个服务（Core、规则引擎、Transport、EDQS、
JS Executor、版本控制）。节点之间**只**通过队列系统（通常是 Kafka）通信 —— 节点间没有直接的
HTTP/RPC。

### 核心接口

| 接口 | 用途 |
|-----------|---------|
| `TbClusterService` | 主要的集群通信 API —— 向其他服务推消息、广播状态变更 |
| `TbQueueClusterService` | 队列层面的集群操作（TbClusterService 的父接口） |
| `TbQueueProducer<T>` | 向指定 topic 生产消息 |
| `TbQueueConsumer<T>` | 从指定 topic 消费消息 |
| `TbQueueRequestTemplate<REQ,RES>` | 请求-应答消息模式 |
| `TbQueueResponseTemplate<REQ,RES>` | 请求-应答的应答侧 |
| `TbQueueMsg` | 队列消息信封接口 |
| `TbQueueMsgHeaders` | 消息头接口 |
| `TbQueueMsgDecoder<T>` | 把原始字节解码成类型化消息 |
| `TbQueueMsgMetadata` | 消息元数据（时间戳、来源） |
| `TbQueueCallback` | 队列操作的异步回调接口 |
| `TbQueueHandler` | 消息处理回调 |
| `TbQueueAdmin` | topic/分区管理 |
| `TbEdgeQueueAdmin` | Edge 专属的队列管理 |

### TbClusterService

主集群通信接口，继承 `TbQueueClusterService`。其方法按目标服务归类：

#### 发往 Core
```java
void pushMsgToCore(TopicPartitionInfo tpi, UUID msgKey, ToCoreMsg msg, TbQueueCallback callback);
void broadcastToCore(ToCoreNotificationMsg msg);
void pushNotificationToCore(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);
```

#### 发往规则引擎
```java
void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg msg, TbQueueCallback callback);
void pushNotificationToRuleEngine(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);
```

#### 发往 Transport
```java
void pushNotificationToTransport(String targetServiceId, ToTransportMsg response, TbQueueCallback callback);
```

#### 发往计算字段
```java
void pushMsgToCalculatedFields(TenantId tenantId, EntityId entityId, ToCalculatedFieldMsg msg, TbQueueCallback callback);
```

#### 发往 Edge
```java
void pushMsgToEdge(TenantId tenantId, EntityId entityId, ToEdgeMsg msg, TbQueueCallback callback);
void pushEdgeSyncRequestToEdge(ToEdgeSyncRequest request);
```

#### 发往版本控制
```java
void pushMsgToVersionControl(TenantId tenantId, ToVersionControlServiceMsg msg, TbQueueCallback callback);
```

#### 实体状态变更广播
```java
void broadcastEntityStateChangeEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state);
void onDeviceProfileChange(DeviceProfile deviceProfile, DeviceProfile old, TbQueueCallback callback);
void onTenantProfileChange(TenantProfile tenantProfile, TbQueueCallback callback);
void onTenantChange(Tenant tenant, TbQueueCallback callback);
void onApiStateChange(ApiUsageState apiUsageState, TbQueueCallback callback);
void onDeviceUpdated(Device device, Device old);
void onAssetUpdated(Asset asset, Asset old);
void onCustomerUpdated(Customer customer, Customer old);
void onCalculatedFieldUpdated(CalculatedField cf, CalculatedField old, TbQueueCallback callback);
void onRelationUpdated(TenantId tenantId, EntityRelation relation, TbQueueCallback callback);
void onResourceChange(TbResourceInfo resource, TbQueueCallback callback);
```

### 路由策略

消息按以下方式路由到特定节点：
1. **TopicPartitionInfo**（直接指定分区）
2. **TenantId + EntityId**（基于分区的一致性哈希）
3. **Service ID**（按唯一服务 ID 定向到某个节点）
4. **广播**（发给某服务类型的全部节点）

### 队列消息信封

```java
public interface TbQueueMsg {
    UUID getKey();              // Partition key
    byte[] getData();           // Serialized payload (protobuf)
    TbQueueMsgHeaders getHeaders();  // Metadata
}
```

消息头携带路由元数据（来源服务 ID、回调 topic、消息类型）。

### 请求-应答模式

`TbQueueRequestTemplate` 和 `TbQueueResponseTemplate` 让异步队列之上能做同步风格的通信。请求消息
带有关联 ID 和 reply-to topic。调用方阻塞（或异步处理）直到响应抵达。

## 依赖关系

- **依赖：** `common/data`（实体模型、ID）、`common/message`（消息类型）
- **被谁依赖：** `common/queue`、`application`、`rule-engine`、`transport/*`、`edge-api`、`version-control`
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `cluster-api`
