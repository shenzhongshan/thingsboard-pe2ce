# 公共模块

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

`common/` 目录包含 17 个 Maven 模块，提供共享抽象、数据模型、消息类型和基础设施接口，供
ThingsBoard 其余各层（DAO、transport、规则引擎、application、微服务）使用。

## 模块总览

| 模块 | Maven artifact | 用途 | 主要包 |
|--------|---------------|---------|-------------|
| [data](data.zh-CN.md) | `org.thingsboard.common:data` | 实体模型、DTO、枚举、ID、常量 | `common.data`、`common.data.id`、`common.data.*` |
| [message](message.zh-CN.md) | `org.thingsboard.common:message` | 内部消息类型（TbMsg、actor 消息、队列消息） | `common.msg`、`common.msg.queue`、`common.msg.cluster` |
| [queue](queue.zh-CN.md) | `org.thingsboard.common:queue` | 队列抽象层（Kafka、内存、pub-sub） | `queue.common`、`queue.discovery`、`queue.kafka` |
| [dao-api](dao-api.zh-CN.md) | `org.thingsboard.common:dao-api` | DAO 接口、数据库类型注解、service 契约 | `dao.*`、`dao.util` |
| [cache](cache.zh-CN.md) | `org.thingsboard.common:cache` | Caffeine/Redis 缓存、按实体的缓存实现 | `cache`、`cache.device`、`cache.customer` |
| [cluster-api](cluster-api.zh-CN.md) | `org.thingsboard.common:cluster-api` | 集群通信接口（RPC、事件、路由） | `cluster`、`queue` |
| [actor](actor.zh-CN.md) | `org.thingsboard.common:actor` | Actor 系统抽象（类 Akka 模型） | `actors` |
| [proto](proto.zh-CN.md) | `org.thingsboard.common:proto` | 供 transport、JS invoke、队列使用的 Protobuf/gRPC 定义 | `gen.transport`、`gen.js` |
| [transport](transport.zh-CN.md) | `org.thingsboard.common:transport` | 共享传输抽象（每种协议一个子模块） | `transport.coap`、`transport.http`、`transport.mqtt` |
| [edge-api](edge-api.zh-CN.md) | `org.thingsboard.common:edge-api` | Edge 同步的 gRPC 服务定义与客户端 | `edge.rpc`、`edge.exception` |
| [script](script.zh-CN.md) | `org.thingsboard.common:script` | 脚本引擎 API（TBEL、JS、Nashorn） | `script.api`、`script.api.js`、`script.api.tbel` |
| [coap-server](coap-server.zh-CN.md) | `org.thingsboard.common:coap-server` | CoAP 服务端基础设施（DTLS、会话管理） | `coapserver` |
| [util](util.zh-CN.md) | `org.thingsboard.common:util` | 公共工具（JSON、地理、executor、SSL、校验） | `common.util`、`common.util.geo` |
| **discovery-api** | `org.thingsboard.common:discovery-api` | 服务发现 provider 接口 | `queue.discovery` |
| **edqs** | `org.thingsboard.common:edqs` | 实体数据查询服务抽象（基于 RocksDB） | `edqs.data`、`edqs.query`、`edqs.repo` |
| **stats** | `org.thingsboard.common:stats` | 统计、计数器、API 用量上报 | `common.stats` |
| **version-control** | `org.thingsboard.common:version-control` | 基于 Git 的版本控制服务（实体同步） | `service.sync`、`service.sync.vc` |

最后四个模块（discovery-api、edqs、stats、version-control）在上表中有收录，但没有单独的文档页。
它们提供的是职责单一、用途聚焦的抽象。

## 架构

所有公共模块都隶属于父 POM `org.thingsboard:common`，版本 `4.4.0-SNAPSHOT`。各模块的 group ID 均为
`org.thingsboard.common`。

### 设计原则

1. **被所有层共享** —— `common/` 下的每个模块都是 application、DAO、transport 和规则引擎层的依赖。
   公共模块从不反向依赖更高层的模块。

2. **依赖最小化** —— 公共模块避开重型框架。多数只依赖 `org.thingsboard:data` 和少量库
   （Jackson、Protobuf、Caffeine、Redis 客户端）。

3. **只有 API，没有具体实现** —— `dao-api`、`cluster-api`、`actor` 这类模块只定义接口和抽象类。
   具体实现位于 `dao/`、`application/` 和 `transport/`。

4. **后端可插拔** —— `cache` 和 `queue` 这类模块支持可插拔后端（Caffeine 对 Redis，Kafka 对内存
   对 pub-sub），由 Spring profile 和注解控制。

### 依赖图

```
                          +-------------------+
                          |      [data]       |  <-- Foundation: entities, DTOs, IDs
                          +--------+----------+
                                   |
          +-------------+----------+---------+------------+
          |             |                    |            |
     [message]     [util]              [cache]      [proto]
          |             |                    |            |
    +-----+-----+  +----+----+         +----+----+  +----+----+
    |           |  |         |         |         |  |         |
[actor]  [cluster-api][dao-api]  [discovery-api][queue][edge-api]
    |           |         |         |         |        |
    |           |    [script]  [version-control]  [edqs]
    |           |
    +-----+-----+
          |
    [coap-server]
          |
     [transport]
          |
       [stats]
```

关键依赖流向：
- `data` 是基石 —— 一切都依赖它
- `message` 依赖 `data`、`proto`；被 `actor`、`cluster-api`、`queue` 使用
- `queue` 依赖 `data`、`message`、`cluster-api`、`discovery-api`
- `dao-api` 依赖 `data`；被 `dao` 模块使用（该模块不在 common/ 下）
- `cache` 依赖 `data`；同时提供 Caffeine 和 Redis 两种实现
- `script` 依赖 `data`、`message`、`proto`
- `transport/*` 子模块依赖 `data`、`message`、`proto`、`transport-api`
- `edge-api` 依赖 `data`、`proto`
- `version-control` 依赖 `data`、`proto`、`cluster-api`

## 在构建中的位置

```bash
# All common modules are compiled as part of the full build:
mvn clean install -T6 -DskipTests -Dpkg.skip=true
```

公共模块在 reactor 构建顺序中靠前编译，早于 `dao`、`application`、`rule-engine`、`transport/*`
和 `msa/*` 等模块。
