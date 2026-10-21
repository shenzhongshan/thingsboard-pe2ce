# ThingsBoard 系统架构总览

**本文是权威版。** 英文版 [00-overview.md](00-overview.md) 为对照上游 CE 的参照副本，改动请先落在本文。

## ThingsBoard 是什么？

ThingsBoard 是一个开源 IoT 平台，用于设备管理、数据采集、处理和可视化。它提供面向多租户 SaaS 的
架构，同时支持云端和本地部署。平台覆盖 IoT 数据的完整生命周期：从设备连接与协议转换，经由基于
规则的实时处理与持久化，到仪表板可视化与告警。

核心能力：
- **设备管理**：通过设备配置文件完成开通、凭证管理与配置下发
- **多协议连接**：MQTT、HTTP、CoAP、LwM2M、SNMP 传输
- **规则引擎**：基于 DAG 的消息处理，50+ 内置规则节点，覆盖过滤、转换、富化与外部集成
- **数据持久化**：时序遥测、属性、事件与告警，存入 SQL（PostgreSQL）或 NoSQL（Cassandra）后端
- **实时仪表板**：WebSocket 驱动的可视化，30+ 种 widget
- **边缘计算**：Edge 节点做分布式处理，带存储转发回云端
- **告警系统**：可配置的告警规则，含严重级别、传播与通知
- **RBAC**：跨三个层级（系统、租户、客户）的基于角色的访问控制
- **API 可扩展**：REST API、WebSocket API、自定义规则节点

## 版本与技术栈

| 组件 | 技术 |
|---|---|
| 构建工具 | Maven（Java 25、Spring Boot 3.5） |
| 后端运行时 | Java 25、Spring Boot 3.5.14 |
| 前端 | Angular 20、Angular Material 20、TypeScript |
| 构建/包管理器 | Yarn |
| 主数据库 | PostgreSQL |
| 可选 NoSQL 库 | Apache Cassandra（大规模时序） |
| 缓存 | Caffeine（进程内）、Valkey/Redis（分布式） |
| 消息队列 | Kafka（生产）或内存（开发/演示） |
| 服务发现 | ZooKeeper（微服务部署） |
| 协议编解码 | Netty 4.1（MQTT）、Californium（CoAP）、Leshan（LwM2M） |
| 序列化 | Protobuf 3.25、gRPC 1.76 |
| 容器化 | Docker、Docker Compose |
| 脚本执行 | Nashorn 沙箱 JS（JVM）、Node.js sidecar（规则引擎 JS） |

## 部署拓扑

ThingsBoard 支持两种部署模式：

### 单体部署
所有服务跑在同一个 JVM 进程里。适合开发、评估和小规模生产。内部使用内存队列或 Kafka。

### 微服务部署
各服务作为独立容器部署：

| 服务 | 容器镜像 | 用途 |
|---|---|---|
| **tb-node** | `thingsboard/tb-node` | 主应用节点：HTTP API、WebSocket、规则引擎、actor |
| **tb-http-transport** | `thingsboard/tb-http-transport` | HTTP 设备连接 |
| **tb-mqtt-transport** | `thingsboard/tb-mqtt-transport` | MQTT 设备连接 |
| **tb-coap-transport** | `thingsboard/tb-coap-transport` | CoAP 设备连接 |
| **tb-lwm2m-transport** | `thingsboard/tb-lwm2m-transport` | LwM2M 设备连接 |
| **tb-snmp-transport** | `thingsboard/tb-snmp-transport` | SNMP 设备连接 |
| **tb-edqs** | `thingsboard/tb-edqs` | 实体数据查询服务（专用查询引擎） |
| **js-executor** | Node.js sidecar | 规则引擎的远程 JavaScript 执行 |
| **tb-web-ui** | nginx 托管的 SPA | 经 nginx 提供的 Angular 前端 |
| **vc-executor** | `thingsboard/tb-vc-executor` | 版本控制操作（批量导入/导出） |

开发用的 `docker/` 下有 Docker Compose 文件，可一次拉起含 PostgreSQL、Kafka 和 Valkey 的完整栈。

## 核心数据流

```
Device                   Transport          Queue              Rule Engine
  │                         │                  │                    │
  ├─ MQTT/HTTP/CoAP ──────►├─ Parse ──────────►├─ Topic ───────────►├─ Filter nodes
  │   telemetry upload      │   protocol        │   tb_rule_engine   │   "Originator Type"
  │                         │   Convert to      │                    │   "Message Type Switch"
  │                         │   internal msg     │                    │
  │                         │                  │                    ├─ Transform nodes
  │                         │                  │                    │   "Script"
  │                         │                  │                    │   "Change Originator"
  │                         │                  │                    │
  │                         │                  │                    ├─ Action nodes
  │                         │                  │                    │   "Save Timeseries"
  │                         │                  │                    │   "Create Alarm"
  │                         │                  │                    │   "RPC Call"
  │                         │                  │                    │   "Send Email"
  │                         │                  │                    │   "MQTT/HTTP out"
  │                         │                  │                    │
  │                         │                  │                    ▼
  │                         │                  │              Persistence
  │                         │                  │              ┌─────────────┐
  │                         │                  │              │ PostgreSQL   │
  │                         │                  │              │ (entities,   │
  │                         │                  │              │  attributes, │
  │                         │                  │              │  events)     │
  │                         │                  │              ├─────────────┤
  │                         │                  │              │ Cassandra    │
  │                         │                  │              │ (time-series,│
  │                         │                  │              │  latest TS)  │
  │                         │                  │              └─────────────┘
  │                         │                  │                    │
  │                         │                  │              WebSocket Push
  │                         │                  │              ┌─────────────┐
  │                         │                  │              │ Dashboard   │
  │                         │                  │              │ Widgets     │
  │                         │                  │              │ (real-time) │
  │                         │                  │              └─────────────┘
```

**逐步流程**：

1. **设备上报遥测**，经 MQTT、HTTP 或 CoAP 发往某个 transport 端点。
2. **Transport** 对设备做认证，解码协议专有报文，并转换成规范的内部消息
   （`TelemetryUploadRequest` 之类）。
3. **Transport 推送**该消息到**队列**（Kafka topic `tb_rule_engine` 或内存等价物）。
4. **规则引擎**从队列消费该消息，并按设备所分配的规则链进行路由。
5. **规则节点**处理消息：按发起者类型过滤、用脚本转换数据、把遥测存库、产生告警、
   调外部 API、发邮件等等。
6. **WebSocket 通知**把更新后的遥测和告警状态实时推给已连接的仪表板会话。

**下行流程**（服务端到设备）：

1. 用户或规则引擎向设备发起一次 **RPC 请求**（远程过程调用）。
2. 请求入队到 `tb_transport.api.requests`。
3. 对应的 **transport** 消费该 RPC 请求，并通过设备当前活跃的会话（MQTT、HTTP、CoAP）投递给设备。
4. 设备回送 RPC 响应，沿上行路径返回给调用方。

## 架构分层

### 1. 传输层

- **位置**：`transport/` Maven 模块；子模块 `mqtt`、`http`、`coap`、`lwm2m`、`snmp`
- **职责**：面向设备的网络端点
- **细节**：每个 transport 都是独立的、基于 Netty 的服务端（CoAP/LwM2M 基于 Californium）。
  transport 用访问令牌、X.509 证书或 LwM2M 凭证对设备认证，把协议专有报文（JSON、CBOR、Protobuf）
  解码成 `common/transport/transport-api` 里定义的内部 `TransportProtos` 消息格式。transport 是
  无状态的——会话状态保存在内存或共享缓存（Valkey/Redis）里，以便水平扩展。

### 2. 队列层

- **位置**：`common/queue/` Maven 模块
- **职责**：transport、规则引擎与核心服务之间的异步消息总线
- **细节**：对 Kafka（生产）或内存实现（开发）做抽象。topic 按实体 ID 分区以保证有序处理。
  每类微服务有专属 topic：
  - `tb_rule_engine` —— 遥测、属性更新、设备上下线
  - `tb_transport.api.requests` —— 发往设备的 RPC 与属性请求
  - `tb_core` —— 核心服务通知（告警、事件）
  - `tb_ota_package` —— OTA 固件升级
  - `tb_version_control` —— 批量导入/导出操作
  - `tb_housekeeper` —— 系统维护任务

### 3. 规则引擎

- **位置**：`rule-engine/` Maven 模块；子模块 `rule-engine-api`、`rule-engine-components`
- **职责**：以有向无环图（DAG）形式组织的数据处理流水线
- **细节**：每个租户配置一条或多条**规则链**——由**规则节点**组成的可视化 DAG。消息从 "Input"
  节点进入并穿行整张图。每个节点可以过滤（按条件路由）、转换（跑 JavaScript、从库里富化）或
  执行动作（存数据、发 HTTP、产生告警）。规则引擎在**actor 系统**内处理消息，每个规则链实例映射
  到一个 actor。内置节点类型 50+，包括：
  - **过滤器**：Originator Type、Message Type Switch、Script、Device Profile、Entity Type
  - **转换器**：Script（JavaScript）、Change Originator、Originator Attributes、Originator Fields
  - **动作**：Save Timeseries、Save Attributes、Create Alarm、Clear Alarm、RPC Call、Send Email、
    REST API Call、MQTT、HTTP、Kafka、AWS SQS/SNS、Azure Service Bus、Google Pub/Sub
  - **AI**：AI Agent、AI Insight、AI Guard、AI Summarize
  - **Edge**：Push to Edge、Push to Cloud

### 4. Actor 系统

- **位置**：`common/actor/`（抽象）、`application/.../actors/`（实现）
- **职责**：有状态实体处理的并发模型
- **细节**：每个**设备**、**租户**、**规则链**、**edge**、**计算字段**和**告警**都由各自的 actor
  表示。actor 在专属的 dispatcher 线程池上串行处理消息，从而不需要显式加锁。该 actor 系统受 Akka
  启发，但用自研的轻量框架实现。dispatcher 线程池大小可按 actor 类型配置（见 `thingsboard.yml` 里的
  `actors.system.*_dispatcher_pool_size`）。主要 actor 类型：
  - `tenant` —— 租户级状态与配置
  - `device` —— 设备会话管理、RPC 派发
  - `ruleChain` —— 规则引擎消息处理
  - `app` —— 应用级协调
  - `calculatedField` —— 计算指标
  - `stats` —— 用量统计聚合

### 5. DAO 层（数据访问）

- **位置**：`dao/` Maven 模块、`common/dao-api/`（接口）
- **职责**：所有实体类型的持久化抽象
- **细节**：DAO 接口定义在 `common/dao-api/`。实现在 `dao/` 下，分 `sql/` 子目录（经 JPA 走
  JDBC/PostgreSQL）和 `nosql/` 子目录（Cassandra，用于时序与最新遥测）。持久化的实体类型包括：
  Device、Asset、DeviceProfile、AssetProfile、Customer、User、Tenant、TenantProfile、Dashboard、
  RuleChain、Alarm、Edge、EntityView、Widget、Resource、OtaPackage、Notification、AiModel 等。
  DAO 层对 SQL 用 Spring Data JPA repository，对 NoSQL 用 Cassandra driver。

### 6. 控制器与服务（应用层）

- **位置**：`application/.../controller/` 和 `application/.../service/`
- **职责**：REST API 与业务逻辑
- **细节**：REST 控制器与实体类型一一对应组织（如 `DeviceController`、`AlarmController`、
  `DashboardController`）。基类控制器（`BaseController`）提供通用的基于 JWT 的认证、租户隔离和
  分页辅助。控制器委派给专门的 service 类。每个实体类型有自己的 service（如 `DeviceService`、
  `AlarmService`、`DashboardService`）。系统级 service 处理横切关注点：遥测、队列、订阅、状态、
  同步、edge 同步、housekeeping、安装、邮件、短信、通知、安全和 OTA。

### 7. 前端（Angular 应用）

- **位置**：`ui-ngx/` Maven 模块
- **职责**：用于设备管理、仪表板和后台管理的 Web UI
- **细节**：基于 Angular Material 20 组件库构建的 Angular 20 单页应用。主要结构区域：
  - `src/app/core/http/` —— HTTP service 层，每个实体类型一个 service
  - `src/app/modules/home/pages/` —— 遵循实体表格范式的功能页面
  - `src/app/shared/components/` —— 可复用组件（实体选择器、时间窗选择器、仪表板）
  - 实体页面用 `TableConfigResolver` 定义列与操作，并继承通用基类 `EntityComponent<T>`
  - 仪表板实时更新经由 WebSocket（可回落 SockJS）

## 租户隔离模型

ThingsBoard 是一个**多租户 SaaS 平台**。数据模型在每一层都强制隔离：

- **系统管理员**：最高权限层级。管理系统配置、租户创建和平台级设置。
- **租户管理员**：管理本租户的设备、资产、客户、仪表板和规则链。每个租户是一个隔离的工作区。
- **客户**：某个租户的终端用户。客户拥有设备的子分组，并有自己的仪表板。
- **用户**：隶属于某个客户或直接隶属于租户。拥有基于角色的权限（RBAC）。

隔离在数据库层面强制执行：每个实体（Device、Asset、Dashboard 等）都带 `tenant_id` 字段。所有查询
都按从已认证用户 JWT 中取出的租户 ID 限定作用域。actor 系统确保租户专属的 actor 只处理属于本租户的
消息。

## 边缘计算架构

ThingsBoard Edge 把平台延伸到连接不稳定的远端站点：

- **Edge 节点**：部署在远端现场（厂房、门店、车辆）的轻量 ThingsBoard 实例。它运行平台的一个
  子集——规则引擎、本地存储和设备连接——不要求与云端保持长连接。
- **云到边同步**：租户管理员把规则链、仪表板、设备配置文件和资产分配给某个 edge，这些通过队列
  （`tb_edge` 系列 topic）下推到 edge 节点。
- **边到云同步**：edge 节点在本地采集遥测、应用规则链，并在网络可用时向上游发布数据。
  edge 侧产生的数据走专属的 `tb_edge_event.notifications` topic。

这套架构支撑如下场景：
- 走卫星链路的海上平台
- 走蜂窝网络的车队
- 必须在断网期间继续营业的门店
- 网络物理隔离的工业现场

## 关键配置

主配置文件是 `application/src/main/resources/thingsboard.yml`。它用环境变量参数化
（如 `${HTTP_BIND_PORT:8080}`），因此可以完全通过 Docker 环境变量或 Kubernetes ConfigMap 来配置。
主要配置段：

| 配置段 | 用途 |
|---|---|
| `server` | HTTP/HTTPS 绑定地址、端口、SSL、WebSocket 设置 |
| `queue` | 队列类型（内存或 kafka）、Kafka bootstrap 服务器、topic 配置 |
| `database.ts` | 时序存储后端（sql、cassandra、timescale） |
| `cassandra` | Cassandra 集群连接设置 |
| `actors` | actor 系统线程池、dispatcher 大小、超时 |
| `security.jwt` | JWT 令牌签名与过期 |
| `zk` | ZooKeeper 服务发现（微服务模式） |
| `cache` | Caffeine 缓存规格与 Valkey/Redis 配置 |
| `edge` | 边缘计算队列与同步设置 |

## 模块地图

详细的模块说明见 [构建系统](01-build-system.zh-CN.md)。

| 模块 | 子模块 | 用途 |
|---|---|---|
| `common/` | 16 个子模块 | 共享数据模型、消息、队列、缓存、actor 抽象 |
| `netty-mqtt/` | —— | 自研 Netty MQTT 编解码 |
| `dao/` | —— | 数据访问实现（SQL + NoSQL） |
| `edqs/` | —— | 实体数据查询服务 |
| `rule-engine/` | `api`、`components` | 规则引擎框架 + 内置规则节点 |
| `transport/` | `mqtt`、`http`、`coap`、`lwm2m`、`snmp` | 设备连接传输 |
| `application/` | —— | Spring Boot 应用（控制器、服务、actor） |
| `ui-ngx/` | —— | Angular 20 前端 |
| `msa/` | 10 个子模块 | 所有服务的 Docker 镜像 + 黑盒测试 |
| `rest-client/` | —— | Java REST 客户端 SDK |
| `monitoring/` | —— | 基于 Micrometer 的指标 |
| `tools/` | —— | 迁移与实用工具 |
