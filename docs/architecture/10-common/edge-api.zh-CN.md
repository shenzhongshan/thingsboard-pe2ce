# Common Edge API（`common/edge-api`）

**本文是权威版。** 英文版 [edge-api.md](edge-api.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:edge-api`

`common/edge-api` 模块提供 ThingsBoard Edge 实例所用的、基于 gRPC 的通信协议。Edge 是一个轻量的
ThingsBoard 发行版，运行在远端现场（厂房、门店、车辆），并与中心 ThingsBoard 服务端同步数据。

## 架构

Edge 实例通过双向 gRPC 流式连接与中心 ThingsBoard 平台通信。Edge 作为 gRPC 客户端，连接到服务端的
`EdgeRpcService`。消息双向流动：

- **上行**（Edge -> 云） —— 遥测、属性、事件、设备连接状态
- **下行**（云 -> Edge） —— 实体配置、规则链、仪表板、设备配置文件

### Proto 定义

gRPC 服务与消息定义在 `edge.proto`（位于 `common/edge-api/src/main/proto/edge.proto`）：

```protobuf
service EdgeRpcService {
    rpc handleMsgs(stream RequestMsg) returns (stream ResponseMsg) {}
}
```

生成的 Java 类：`org.thingsboard.server.gen.edge.v1.EdgeProtos`

### EdgeVersion

`EdgeVersion` 枚举跟踪各版本间的协议兼容性。**本 fork 的线协议已整栈替换为 PE 4.3.1.1 的那套**，
所以枚举值与上游 CE 不同，且编号是混编的：

| 枚举值 | proto 编号 |
|---------|-----------|
| `V_3_3_0` | 0 |
| `V_3_3_3` | 1 |
| `V_3_4_0` | 2 |
| `V_3_6_0` | 3 |
| `V_3_6_1` | 4 |
| `V_3_6_2` | 5 |
| `V_3_6_4` | 6 |
| `V_3_7_0` | 7 |
| `V_3_8_0` | 8 |
| `V_3_9_0` | 9 |
| `V_4_0_0` | 10 |
| `V_4_1_0` | 11 |
| `V_4_2_0` | 12 |
| `V_4_3_0` | 13 |
| `V_4_2_1_2` | 14 |
| `V_4_2_2` | 4220 |
| `V_4_2_2_1` | 4221 |
| `V_4_3_0_1` | 15 |
| `V_4_3_1` | 4310 |
| `V_4_3_1_1` | 4311 |
| `V_LATEST` | 99999 |

**注意 proto 编号已经不能用来比较版本大小了** —— `V_4_2_1_2(14)` 排在 `V_4_3_0(13)` 之后。所以
`EdgeVersionComparator` 是**从枚举名字反解版本号**再逐段比较的，不要直接比 proto 数值。
`V_LATEST`（= 99999）由该比较器特判成"当前最新"。

### 消息类型

#### RequestMsg（Edge -> 云）

| MsgType | 用途 |
|---------|---------|
| `CONNECT_RPC_MESSAGE` | 初始连接握手，携带 Edge 元数据 |
| `UPLINK_RPC_MESSAGE` | 推往云端的设备遥测、属性、事件 |
| `DOWNLINK_RPC_RESPONSE_MESSAGE` | Edge 对云端下行消息的确认 |
| `SYNC_REQUEST_RPC_MESSAGE` | Edge 向云端请求实体同步 |

#### ResponseMsg（云 -> Edge）

| 类型 | 用途 |
|------|---------|
| `ConnectResponseMsg` | 连接接受/拒绝，附带同步配置 |
| `UplinkResponseMsg` | 对上行消息的确认 |
| `DownlinkMsg` | 推给 Edge 的实体配置（规则链、仪表板、设备、资产等） |
| `EdgeUpdateMsg` | Edge 配置/软件更新 |

### 同步协议

连接建立后，Edge 发出 `SyncRequestMsg`。云端以一串 `DownlinkMsg` 回应，代表 Edge 所需的完整状态：

1. **租户**配置
2. 分配给该 Edge 的**设备配置文件**
3. **资产配置文件**
4. **规则链**（edge 专属的与 root 的）
5. **仪表板**
6. **设备/资产**定义
7. 实体之间的**关系**
8. **Widget bundle**
9. **通知规则**

每条下行消息包含：
- `downlinkMsgId`（int64） —— 单调递增的 ID，用于排序
- `syncCompleted`（bool） —— 是否为本次同步的最后一条
- `entityType` + `entityDataProto` —— 实体类型与序列化后的实体数据
- `entityIdProto` + `customerIdProto` —— 归属信息

## Java 侧

### 核心类

| 类 | 用途 |
|-------|---------|
| `EdgeRpcClient` | Edge gRPC 客户端接口 |
| `EdgeGrpcClient` | 用 protobuf 服务 stub 实现的 EdgeRpcClient |
| `EdgeVersionComparator` | 比较 Edge 版本以做兼容性检查（按枚举名反解，不按 proto 数值） |
| `EdgeConnectionException` | Edge 连接问题的异常 |

### EdgeRpcClient 接口

```java
public interface EdgeRpcClient {
    CompletableFuture<Void> connect(...);
    void disconnect(String reason);
    void sendSyncRequest(SyncRequestMsg request);
    void sendUplinkMsg(UplinkMsg msg);
    CompletableFuture<Void> onDownlinkResponse(DownlinkResponseMsg response);
}
```

### EdgeGrpcClient

该 gRPC 实现负责：
- channel 生命周期（连接、重连、断开）
- 经 `handleMsgs` 的双向流
- 响应关联（把响应映射回请求）
- 带指数退避的重连
- stream observer 管理

## 依赖关系

- **依赖：** `common/data`（实体类型、ID）、`common/proto`（通过 import 引入 transport protobuf）、
  gRPC/protobuf 库
- **被谁依赖：** Edge 微服务、`application`（服务端的 Edge 处理器）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `edge-api`
