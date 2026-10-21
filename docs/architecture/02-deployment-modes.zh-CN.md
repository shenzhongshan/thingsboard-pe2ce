# ThingsBoard —— 部署模式与服务进程视图

**本文是权威版。** 英文版 [02-deployment-modes.md](02-deployment-modes.md) 为对照上游 CE 的参照副本，改动请先落在本文。

本文梳理 ThingsBoard CE 的每一种部署拓扑，说明各模式下有哪些进程在跑、它们如何通信、以及如何扩容。

---

## 1. 服务类型

代码库定义了 7 种服务类型（`common/message/.../queue/ServiceType.java` 里的 `ServiceType` 枚举）：

| 枚举值 | `TB_SERVICE_TYPE` | 职责 |
|---|---|---|
| `TB_CORE` | `tb-core` | REST API、WebSocket、设备管理、订阅、RPC |
| `TB_RULE_ENGINE` | `tb-rule-engine` | 规则链 DAG 处理、遥测持久化、告警 |
| `TB_TRANSPORT` | `tb-transport` | 协议专属的设备连接（MQTT、HTTP、CoAP、LwM2M、SNMP） |
| `JS_EXECUTOR` | `js-executor` | 规则引擎脚本节点的远程 JavaScript 执行 |
| `TB_VC_EXECUTOR` | `tb-vc-executor` | 实体版本控制（Git 集成） |
| `EDQS` | `edqs` | 实体数据查询服务 |
| `TASK_PROCESSOR` | 无 | 内部任务处理器 |

同一个 `tb-node` Docker 镜像被 `monolith`、`tb-core` 和 `tb-rule-engine` 复用 —— 具体行为由启动时的
`TB_SERVICE_TYPE` 环境变量决定。

---

## 2. 组件激活机制

三个自定义注解控制每种服务类型加载哪些 Spring bean：

| 注解 | 何时激活 | 用途 |
|---|---|---|
| `@TbCoreComponent` | `monolith` 或 `tb-core` | 核心服务：REST API、WebSocket、设备状态、订阅 |
| `@TbRuleEngineComponent` | `monolith` 或 `tb-rule-engine` | 规则引擎服务：规则链处理、遥测持久化 |
| `@TbTransportComponent` | `monolith`（且启用 transport）或 `tb-transport` | 传输服务：MQTT、HTTP、CoAP、LwM2M、SNMP |

在 `monolith` 模式下，**三个注解全部激活** —— 一切都跑在同一个 JVM 里。

---

## 3. 部署模式 1：单体（开发 / 简单生产）

### 进程视图

```
┌─────────────────────────────────────────────────────────┐
│                   Monolith (Single JVM)                  │
│                                                         │
│  TB_SERVICE_TYPE=monolith  |  Queue: in-memory (default) │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  tb-core     │  │ tb-rule-     │  │ tb-transport  │  │
│  │  (REST API,  │  │ engine       │  │ (MQTT, HTTP,  │  │
│  │   WebSocket, │  │ (DAG proc,   │  │  CoAP, LwM2M, │  │
│  │   RPC,       │  │  telemetry,  │  │  SNMP)        │  │
│  │   device mgr)│  │  alarms)     │  │               │  │
│  └──────────────┘  └──────────────┘  └───────────────┘  │
│                                                         │
│  ┌──────────────┐  ┌──────────────────────────────────┐ │
│  │  JS Exec     │  │  EDQS (embedded, RocksDB)         │ │
│  │  (local      │  │  TB_EDQS_MODE=local               │ │
│  │   Nashorn)   │  │                                   │ │
│  └──────────────┘  └──────────────────────────────────┘ │
│                                                         │
│  Cache: Caffeine (local)  |  Queue: in-memory (no net)  │
│  Database: PostgreSQL (entities + timeseries)            │
└─────────────────────────────────────────────────────────┘
```

**Docker 镜像：** `thingsboard/tb-postgres` 或 `thingsboard/tb-cassandra`

### 关键配置

```yaml
service.type: monolith          # All components active
queue.type: in-memory           # No Kafka needed
zk.enabled: false               # No ZooKeeper needed
queue.edqs.mode: local          # Embedded EDQS (RocksDB)
js.evaluator: local             # Nashorn, not remote
transport.api_enabled: true     # Embedded transport server
```

### 基础设施依赖

| 组件 | 是否必需 | 说明 |
|-----------|----------|-------|
| PostgreSQL | 是 | 实体 + 时序 |
| Kafka | 否 | 使用内存队列 |
| ZooKeeper | 否 | 单进程，无需协调 |
| Redis/Valkey | 否 | 使用进程内 Caffeine 缓存 |
| JS Executor | 否 | 使用内嵌 Nashorn |

### 扩容

- **不支持水平扩容** —— 单 JVM 进程
- 只能垂直扩容（加 CPU/内存）
- 适用于开发、评估、小规模部署

---

## 4. 部署模式 2：Core + 规则引擎拆分

### 进程视图

```
                        ZooKeeper (2181)
                             │
                    ┌────────┴────────┐
                    │  coordination,  │
                    │  partition mgmt │
                    └─────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ tb-core-1     │   │ tb-core-2     │   │ tb-core-N     │
│ (REST API,    │   │ (REST API,    │   │ (REST API,    │
│  WebSocket,   │   │  WebSocket,   │   │  WebSocket,   │
│  device mgr)  │   │  device mgr)  │   │  device mgr)  │
│ port: 8080    │   │ port: 8080    │   │ port: 8080    │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                    Kafka (9092)
                    ┌───────┴───────┐
                    │   tb_core     │
                    │   tb_rule_    │
                    │   engine      │
                    │   js_eval.*   │
                    └───────┬───────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ tb-rule-      │   │ tb-rule-      │   │ tb-rule-      │
│ engine-1      │   │ engine-2      │   │ engine-N      │
│ (DAG proc,    │   │ (DAG proc,    │   │ (DAG proc,    │
│  telemetry,   │   │  telemetry,   │   │  telemetry,   │
│  alarms)      │   │  alarms)      │   │  alarms)      │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                    Kafka (9092)
                    ┌───────┴───────┐
                    │ js_eval.req   │
                    │ js_eval.resp  │
                    └───────┬───────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ js-executor-1 │   │ js-executor-2 │...│ js-executor-10│
│ (Node.js)     │   │ (Node.js)     │   │ (Node.js)     │
│ Kafka topics  │   │ Kafka topics  │   │ Kafka topics  │
│ 30 partitions │   │ 30 partitions │   │ 30 partitions │
└───────────────┘   └───────────────┘   └───────────────┘

                    PostgreSQL (5432)
                    Cache: Valkey/Redis (6379)
```

**Docker 镜像：**
- `thingsboard/tb-node`（配 `TB_SERVICE_TYPE=tb-core` 或 `TB_SERVICE_TYPE=tb-rule-engine`）
- `thingsboard/tb-js-executor`
- `thingsboard/tb-web-ui`

### 关键配置

```yaml
# On tb-core instances:
service.type: tb-core
queue.type: kafka
zk.enabled: true                # Service discovery + partition mgmt
zk.url: zookeeper:2181
js.evaluator: remote            # Send JS to external executor
transport.type: remote          # Transport services are separate

# On tb-rule-engine instances:
service.type: tb-rule-engine
queue.type: kafka
zk.enabled: true
zk.url: zookeeper:2181
js.evaluator: remote
```

### 基础设施依赖

| 组件 | 是否必需 | 说明 |
|-----------|----------|-------|
| PostgreSQL | 是 | 共享数据库 |
| Kafka | 是 | 服务间消息总线 |
| ZooKeeper | 是 | 服务发现、分区协调 |
| Redis/Valkey | 是 | 分布式缓存（多实例共享） |
| JS Executor | 是 | 远程 Node.js 进程 |

### 扩容

| 组件 | 扩容方式 | 机制 |
|-----------|---------|-----------|
| tb-core | 水平 | Kafka 消费者组，配 `consumer-per-partition=true` |
| tb-rule-engine | 水平 | 多实例消费同一 topic |
| js-executor | 水平 | `js_eval.requests` topic 有 30 个分区 |

### 服务间通信（Kafka topic）

```
tb_core                  ←→  tb_core (core-to-core)
tb_core.notifications    →   tb-core consumers (notifications)
tb_rule_engine           ←→  tb-rule-engine consumers
tb_rule_engine.notifications → tb-rule-engine consumers
js_eval.requests         →   js-executor consumers
js_eval.responses.{id}   →   tb-rule-engine consumers
```

---

## 5. 部署模式 3：完整微服务（生产）

### 进程视图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL CLIENTS                             │
│  Browsers (HTTPS)    |    Devices (MQTT/HTTP/CoAP/LwM2M/SNMP)       │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │  HAProxy       │
                    │  (80, 443,     │
                    │   1883, 7070,  │
                    │   9999)        │
                    └───────┬────────┘
                            │
        ┌───────┬───────────┼───────────┬───────────┬───────┐
        │       │           │           │           │       │
        ▼       ▼           ▼           ▼           ▼       ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ web-ui-1 │ │ web-ui-2 │ │ mqtt-1   │ │ mqtt-2   │ │ http-1   │
│ (Node.js)│ │ (Node.js)│ │ :1883    │ │ :1883    │ │ :8081    │
│ nginx    │ │ nginx    │ │          │ │          │ │          │
└──────────┘ └──────────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
  stateless     stateless       │            │            │
                                │            │            │
        ┌──────────┐ ┌──────────┤            │            │
        │ http-2   │ │ coap     │            │            │
        │ :8081    │ │ :5683    │            │            │
        └──────────┘ └────┬─────┘            │            │
                          │                  │            │
        ┌──────────┐ ┌────┤                  │            │
        │ lwm2m    │ │snmp│                  │            │
        │ :5685/86 │ │:1620│                 │            │
        └──────────┘ └────┘                  │            │
                                             │            │
                    ┌────────────────────────┴────────────┘
                    │
            ZooKeeper (2181)
            Service Discovery
                    │
            ┌───────┴───────┐
            ▼               ▼
    ┌───────────────┐ ┌───────────────┐
    │ tb-core-1     │ │ tb-core-2     │
    │ REST/WS/8080  │ │ REST/WS/8080  │
    │ Edge RPC:7070 │ │ Edge RPC:7070 │
    └───────┬───────┘ └───────┬───────┘
            │                 │
            └────────┬────────┘
                     │
             Kafka (9092)
                     │
    ┌────────────────┼────────────────┐
    │                │                │
    ▼                ▼                ▼
┌─────────┐   ┌───────────┐   ┌───────────┐
│ tb-re-1 │   │ tb-re-2   │   │ tb-edqs-1/2│
│ :8080   │   │ :8080     │   │ Entity Data│
│ Rule    │   │ Rule      │   │ Query Svc  │
│ Engine  │   │ Engine    │   │ (optional) │
└────┬────┘   └────┬──────┘   └───────────┘
     │             │
     └──────┬──────┘
            │
    Kafka (9092)
    js_eval.requests
            │
    ┌───────┼───────┬───...───┐
    ▼       ▼       ▼         ▼
┌───────┐ ┌───────┐      ┌───────────┐
│js-ex-1│ │js-ex-2│ ...  │js-ex-10   │
│Node.js│ │Node.js│      │Node.js    │
└───────┘ └───────┘      └───────────┘

┌───────────┐ ┌───────────┐
│vc-exec-1  │ │vc-exec-2  │  (optional)
│:8081      │ │:8081      │  Version Control
└───────────┘ └───────────┘

┌──────────────────────────────────────┐
│ PostgreSQL (5432) + Cassandra (9042) │
│ Valkey/Redis Cache (6379)            │
└──────────────────────────────────────┘
```

### 完整服务清单

| 服务 | Docker 镜像 | 端口 | 副本数 | 扩容方式 | 类型 |
|---------|-------------|------|----------|---------|------|
| `tb-core-1/2` | `tb-node` | 8080, 7070 | 2（可配） | 按分区 | Java 25 |
| `tb-rule-engine-1/2` | `tb-node` | 8080 | 2（可配） | 按队列 | Java 25 |
| `tb-mqtt-transport-1/2` | `tb-mqtt-transport` | 1883 | 2（可配） | 无状态 | Java 25 |
| `tb-http-transport-1/2` | `tb-http-transport` | 8081 | 2（可配） | 无状态 | Java 25 |
| `tb-coap-transport` | `tb-coap-transport` | 5683/udp | 1 | 无状态 | Java 25 |
| `tb-lwm2m-transport` | `tb-lwm2m-transport` | 5685,5686/udp | 1 | 无状态 | Java 25 |
| `tb-snmp-transport` | `tb-snmp-transport` | 1620/udp | 1 | 无状态 | Java 25 |
| `tb-js-executor` | `tb-js-executor` | —— | 10 | 按分区 | Node.js 22 |
| `tb-web-ui-1/2` | `tb-web-ui` | 8080（内部） | 2 | 无状态 | Node.js 22 |
| `tb-vc-executor-1/2` | `tb-vc-executor` | 8081 | 2（可选） | 按分区 | Java 25 |
| `tb-edqs-1/2` | `tb-edqs` | 8080 | 2（可选） | 按分区 | Java 25 |
| `haproxy` | `haproxy-certbot` | 80, 443, 1883, 7070 | 1 | 不适用 | HAProxy |
| `zookeeper` | `zookeeper:3.8.1` | 2181 | 1 | 不适用 | Java |
| `kafka` | `bitnamilegacy/kafka:4.0` | 9092 | 1（KRaft） | 不适用 | Java |
| `postgres` | `postgres:16` | 5432 | 1 | 不适用 | PostgreSQL |
| `cassandra` | `cassandra:5.0` | 9042 | 1（仅 hybrid） | 不适用 | Cassandra |
| `valkey` | `bitnamilegacy/valkey:8.0` | 6379 | 1/3/6 | 不适用 | Valkey |

### Kafka Topic 映射（完整微服务）

```
┌─────────────────────────────────────────────────────────────────┐
│                        KAFKA TOPIC MAP                           │
├─────────────────┬────────────────┬──────────────────────────────┤
│ Topic            │ Partitions     │ Direction / Consumers         │
├─────────────────┼────────────────┼──────────────────────────────┤
│ tb_core          │ 10             │ Core ↔ Core                   │
│ tb_core.notify   │ —              │ → Core consumers              │
│ tb_rule_engine   │ 1              │ RE ↔ RE                      │
│ tb_re.notify     │ —              │ → RE consumers               │
│ tb_transport.api │                │                               │
│  .requests       │ 10             │ Transport → Core              │
│ tb_transport.api │                │                               │
│  .responses      │ 10             │ Core → Transport              │
│ js_eval.requests │ 30             │ RE → JS Executor              │
│ js_eval.resp.{id}│ —              │ JS Executor → RE (per node)   │
│ edqs.events      │ 1              │ Core → EDQS                   │
│ edqs.state       │ 1 (compacted)  │ Core → EDQS                   │
│ edqs.requests    │ 1              │ Core → EDQS                   │
│ edqs.responses   │ 1              │ EDQS → Core                   │
│ tb_edge          │ 10             │ Core ↔ Core (edge sync)       │
│ tb_housekeeper   │ 10             │ Maintenance tasks             │
│ tb_cf_event      │ 1              │ Calculated Field events       │
│ tb_cf_state      │ 1              │ Calculated Field state        │
│ tb_version_      │                │                               │
│   control        │ 10             │ Core ↔ VC Executor            │
│ tb_notifications │ —              │ Notification delivery         │
└─────────────────┴────────────────┴──────────────────────────────┘
```

### HAProxy 路由

| 前端 | 端口 | 路由 | 后端 |
|----------|------|-------|---------|
| `http-in` | 80 | `/api/v1/` | HTTP transport (8081) |
| `http-in` | 80 | `/api/`、`/static/` 等 | tb-core (8080) |
| `http-in` | 80 | 默认 | tb-web-ui (8080) |
| `https-in` | 443 | 同 http-in | （SSL 在此终结） |
| `mqtt-in` | 1883 | TCP（SNI） | MQTT transport (1883) |
| `edges-rpc-in` | 7070 | TCP | tb-core (7070) |
| `stats` | 9999 | HTTP | HAProxy Stats |

---

## 6. 部署模式 4：安装 / 升级模式

### 进程视图

```
┌─────────────────────────────────────────────────────────┐
│              tb-core-1 (ephemeral, INSTALL_TB=true)      │
│                                                         │
│  spring.profiles.active=install                         │
│  INSTALL_TB=true       (triggers schema creation)       │
│  LOAD_DEMO=optional    (loads demo data)                │
│                                                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │ SqlAbstractDatabaseSchemaService                    │ │
│  │   ├── Execute schema-entities.sql                  │ │
│  │   ├── Execute schema-ts-psql.sql                   │ │
│  │   ├── Execute schema-functions.sql                 │ │
│  │   ├── Execute schema-views.sql                     │ │
│  │   └── Insert tb_schema_settings version row        │ │
│  │                                                    │ │
│  │ SqlDatabaseUpgradeService (UPGRADE_TB=true)         │ │
│  │   ├── Load basic/schema_update.sql                 │ │
│  │   ├── Load lts/schema_update.sql                   │ │
│  │   └── Apply sequential upgrades                    │ │
│  │                                                    │ │
│  │ DefaultSystemDataLoaderService                      │ │
│  │   └── Insert system-level data (admin user,        │ │
│  │       default rule chains, widget bundles, etc.)    │ │
│  └────────────────────────────────────────────────────┘ │
│                                                         │
│  Starts → Creates/Upgrades schema → Exits               │
│  Kafka, ZooKeeper, Cache: NOT required                  │
│  PostgreSQL: REQUIRED                                   │
└─────────────────────────────────────────────────────────┘
```

这是一种引导模式，不是运行时部署。容器启动后创建或升级数据库 schema、加载系统数据，然后退出。
被 `docker-install-tb.sh` 和 `docker-upgrade-tb.sh` 使用。

---

## 7. 部署矩阵小结

### 按模式对比

| 维度 | 单体 | Core+RE 拆分 | 完整微服务 |
|--------|----------|---------------|--------------------|
| **Java 进程数** | 1 | 2-4（1-2 core + 1-2 RE） | 10+（core、RE、transport×5、edqs、vc） |
| **Node.js 进程数** | 0（内嵌 Nashorn） | 1-10（js-executor）+ 1-2（web-ui） | 10（js-executor）+ 2（web-ui） |
| **队列** | 内存（无 Kafka） | 必需 Kafka | 必需 Kafka |
| **服务发现** | 无 | ZooKeeper | ZooKeeper |
| **缓存** | Caffeine（本地） | Valkey/Redis（共享） | Valkey/Redis（共享） |
| **实体数据库** | PostgreSQL | PostgreSQL | PostgreSQL |
| **时序数据库** | PostgreSQL、Cassandra 或 TimescaleDB | PostgreSQL、Cassandra 或 TimescaleDB | PostgreSQL、Cassandra 或 TimescaleDB |
| **JS 执行** | 本地（JVM Nashorn） | 远程（经 Kafka 的 Node.js） | 远程（经 Kafka 的 Node.js） |
| **Transport** | 内嵌在 JVM 中 | 经 Kafka 的独立进程 | 经 Kafka 的独立进程 |
| **EDQS** | 内嵌（RocksDB） | 可选远程 | 可选远程（2 实例） |
| **水平扩容** | 否 | 是（core、RE、js-exec） | 是（全部组件） |
| **容错** | 否 | 是（多实例） | 是（多实例） |
| **复杂度** | 低 | 中 | 高 |

### 各模式的基础设施依赖

```
                         Monolith    Core+RE      Full Micro
PostgreSQL                 ●           ●             ●
Cassandra (optional)       ○           ○             ○
Kafka                      ✗           ●             ●
ZooKeeper                  ✗           ●             ●
Valkey/Redis               ✗           ●             ●
JS Executor (Node.js)      ✗           ●             ●
HAProxy                    ✗           ✗             ●
```

---

## 8. 缓存部署变体

缓存后端可独立选择，与任何部署模式都能配合：

| 缓存模式 | Compose 文件 | 架构 |
|------------|-------------|--------------|
| **valkey**（单机） | `docker-compose.valkey.yml` | 单个 Valkey 8.0 节点（端口 6379） |
| **valkey-cluster** | `docker-compose.valkey-cluster.yml` | 6 节点 Valkey 集群（3 主 + 3 从） |
| **valkey-sentinel** | `docker-compose.valkey-sentinel.yml` | 1 主 + 1 从 + 1 sentinel |
| **caffeine**（内嵌） | 无 | 进程内 Caffeine 缓存（仅单体） |

相关配置：
```yaml
cache:
  type: "${CACHE_TYPE:caffeine}"  # caffeine or redis
redis:
  connection_type: "${REDIS_CONNECTION_TYPE:standalone}"  # standalone, cluster, sentinel
```

---

## 9. 数据库部署变体

| 模式 | Compose 文件 | 实体 | 时序 | 额外容器 |
|------|-------------|----------|------------|---------------------|
| **postgres** | `docker-compose.postgres.yml` | PostgreSQL | PostgreSQL（sql） | 无 |
| **hybrid** | `docker-compose.hybrid.yml` | PostgreSQL | Cassandra | `cassandra:5.0` |

相关配置：
```yaml
database:
  ts:
    type: "${DATABASE_TS_TYPE:sql}"  # sql, cassandra, timescale
  ts_latest:
    type: "${DATABASE_TS_LATEST_TYPE:sql}"  # sql, cassandra, timescale
```

---

## 10. 队列部署变体

| 模式 | Compose 文件 | 本地 broker | 适用场景 |
|------|-------------|-------------|----------|
| **kafka** | `docker-compose.kafka.yml` | 有（KRaft，端口 9092） | 默认生产方案 |
| **confluent** | `docker-compose.confluent.yml` | 无（Confluent Cloud） | 云原生、托管 Kafka |

---

## 11. 启动顺序（完整微服务）

```
1. PostgreSQL    ──→  healthy
2. ZooKeeper     ──→  healthy
3. Kafka         ──→  healthy  
4. Valkey        ──→  healthy
5. JS Executor   ──→  healthy (10 replicas)
6. tb-rule-engine-1/2  ──→  healthy
7. tb-core-1/2         ──→  healthy
8. Transport services  ──→  healthy (MQTT, HTTP, CoAP, LwM2M, SNMP)
9. EDQS (optional)     ──→  healthy
10. VC Executor (optional) ──→  healthy
11. Web UI              ──→  healthy
12. HAProxy             ──→  healthy (exposes all ports)
```

安装脚本（`docker-install-tb.sh`）走的是另一套顺序：
1. 只启动基础设施（postgres、zookeeper、kafka、valkey）
2. 以 `INSTALL_TB=true` 启动 tb-core-1（创建 schema、加载系统数据，然后退出）
3. 正常启动所有服务
