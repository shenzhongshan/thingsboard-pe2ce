# 支撑模块

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

支撑模块提供基础设施、工具和客户端库，作为 ThingsBoard 核心平台的补充。包括自研协议编解码、
edge 服务、Java 客户端 SDK、监控工具、数据迁移工具，以及全部服务的 Docker 打包。

## netty-mqtt

**位置：** `netty-mqtt/`  
**用途：** 基于 Netty 的自研 MQTT 客户端，支持 MQTT 3.1.1 与 MQTT 5.0。

该模块实现了一个建立在 Netty 之上的全异步 MQTT 客户端。ThingsBoard 内部把它用于基于 MQTT 的规则
引擎节点、集成测试，以及 MQTT transport 的出站消息。

| 文件 | 用途 |
|------|---------|
| `MqttClient.java` | 客户端接口：connect、publish、subscribe、unsubscribe、disconnect |
| `MqttClientImpl.java` | 完整的异步实现，含连接、订阅与发布 |
| `MqttClientConfig.java` | 客户端配置（host、端口、client ID、凭证、last will、SSL、重连） |
| `MqttChannelHandler.java` | Netty channel handler：处理 CONNACK、PUBLISH、SUBACK、UNSUBACK、PUBACK、PUBREC、PUBREL、PUBCOMP、DISCONNECT |
| `MqttHandler.java` | 入站 PUBLISH 消息的处理器接口 |
| `MqttClientCallback.java` | 连接级事件回调（连接确认、发布确认、订阅确认、断开） |
| `MqttConnectResult.java` | 连接结果，含成功/失败与返回码 |
| `MqttSubscription.java` | 订阅表示，含 topic filter、handler 与 QoS |
| `MqttPendingSubscription.java` | 待完成订阅，带 future |
| `MqttPendingPublish.java` | 待完成发布，带重传与确认跟踪 |
| `MqttPendingUnsubscription.java` | 待完成取消订阅 |
| `MqttIncomingQos2Publish.java` | 跟踪入站 QoS 2 发布（PUBREC/PUBREL/PUBCOMP 流程） |
| `MqttLastWill.java` | Last Will and Testament 配置 |
| `MqttPingHandler.java` | 保活 PINGREQ/PINGRESP 处理器 |
| `RetransmissionHandler.java` | QoS 1/2 消息重传，带退避 |
| `ReconnectStrategy.java` | 重连逻辑接口 |
| `ReconnectStrategyExponential.java` | 指数退避重连策略 |
| `ChannelClosedException.java` | channel 关闭的异常 |
| `MaxRetransmissionsReachedException.java` | 超过最大重传次数的异常 |

**MQTT 3.1.1 与 5.0：** 客户端经 `MqttVersion` 枚举同时支持两个版本。5.0 增加了 reason code、
properties、会话过期等增强。

**QoS 支持：** 完整支持 QoS 0（至多一次）、QoS 1（至少一次）和 QoS 2（恰好一次），包含完整的
PUBREC/PUBREL/PUBCOMP 握手。

**重连：** 带指数退避的自动重连。`ReconnectStrategyExponential` 实现负责递增重试间隔。

**用途：** 该库是 MQTT transport（`common/transport/mqtt`）、monitoring 模块、集成测试和 MQTT
规则引擎节点的依赖。

## edqs

**位置：** `edqs/`  
**用途：** 面向 edge 部署的实体数据查询服务。

EDQS 是一个轻量 Spring Boot 服务，为 edge 侧提供 ThingsBoard 实体数据查询能力的一个子集。它与
edge 实例并行运行，在无需连接中心 ThingsBoard 服务端的情况下缓存并提供实体、关系与属性数据。

| 文件 | 用途 |
|------|---------|
| `ThingsboardEdqsApplication.java` | Spring Boot 应用入口 |
| `EdqsController.java` | 暴露 `/api/edqs/ready` 健康端点的 REST 控制器 |

**EDQS 与主队列的区别：** 不同于基于 Kafka 的主队列系统，EDQS 作为独立的 HTTP 服务运行。它维护一份
内存数据仓库（由 `common/edqs` 模块支撑），镜像该 edge 所需的实体数据子集。核心应用把数据更新推给
EDQS，edge 规则链则查询 EDQS 做实体查找与关系遍历。

**关键依赖：** EDQS 依赖 `common/edqs` 提供数据仓库逻辑，依赖 `common/data` 提供实体模型。它与
edge 管理体系集成以同步数据子集。

## rest-client

**位置：** `rest-client/`  
**用途：** ThingsBoard API 的 Java REST 客户端 SDK。

一个覆盖完整 ThingsBoard REST API 的 Java 客户端库。它负责认证（JWT）、HTTP 请求构造、响应反序列化，
并为所有实体操作提供类型化方法。

| 文件 | 用途 |
|------|---------|
| `RestClient.java` | 主客户端类，覆盖所有实体类型的方法：登录，设备/资产/客户/用户/仪表板/规则链/edge/租户的 CRUD，遥测、属性、告警、审计日志、OTA 包等 |
| `utils/RestJsonConverter.java` | 基于 Jackson 的 JSON 序列化/反序列化工具 |

**认证：** `RestClient` 支持用户名/密码登录，返回 JWT 令牌。该令牌经 Spring `RestTemplate`
拦截器自动加入后续请求头。

**主要方法分类：**
- **登录/认证：** `login()`、`getUser()`、`refreshToken()`
- **设备：** `saveDevice()`、`getDeviceById()`、`getTenantDevices()`、`saveDeviceCredentials()`
- **资产：** `saveAsset()`、`getAssetById()`、`getTenantAssets()`
- **遥测：** `saveDeviceTelemetry()`、`getLatestTimeseries()`
- **告警：** `createAlarm()`、`getAlarms()`、`clearAlarm()`、`ackAlarm()`
- **仪表板：** `saveDashboard()`、`getDashboard()`、`publishDashboardToCustomer()`
- **规则链：** `saveRuleChain()`、`getRuleChains()`、`setRootRuleChain()`
- **RPC：** `handleOneWayDeviceRPCRequest()`、`handleTwoWayDeviceRPCRequest()`
- **Edge：** `saveEdge()`、`getTenantEdges()`、`assignEdgeToCustomer()`
- **管理：** `getAdminSettings()`、`saveAdminSettings()`、`sendTestMail()`

**用途：** rest-client 被以下方使用：
- `msa/black-box-tests/` 做集成与 E2E 测试
- 对接 ThingsBoard 的外部 Java 应用
- monitoring 模块做健康检查

## monitoring

**位置：** `monitoring/`  
**用途：** 基于 Micrometer 的服务监控与健康检查。

一个独立的 Spring Boot 应用，用于监控 ThingsBoard 服务健康。它可以检查全部 transport 端点
（MQTT、HTTP、CoAP、LwM2M）、Web UI、核心应用和 EDQS，并经可配置的通知渠道（如 Slack）上报
故障与高延迟。

| 文件 | 用途 |
|------|---------|
| `ThingsboardMonitoringApplication.java` | 监控应用入口 |
| `config/MonitoringConfig.java` | 被监控目标与通知渠道的配置 |
| `config/MonitoringTarget.java` | 监控目标配置的基类 |
| `config/transport/` | 各 transport 专属的监控配置（Mqtt、Http、Coap、Lwm2m） |
| `client/TbClient.java` | 供监控使用的 ThingsBoard REST API 客户端 |
| `client/WsClient.java` | 用于校验遥测订阅的 WebSocket 客户端 |
| `client/Lwm2mClient.java` | 用于 LwM2M transport 健康检查的 LwM2M 客户端 |
| `service/BaseMonitoringService.java` | 带调度的监控服务基类 |
| `service/MonitoringReporter.java` | 指标上报与评估 |
| `service/MonitoringEntityService.java` | 为测试设备做实体准备 |
| `service/transport/TransportsMonitoringService.java` | 编排各 transport 的健康检查 |
| `service/transport/impl/MqttTransportHealthChecker.java` | MQTT transport：连接、发布遥测、订阅属性 |
| `service/transport/impl/HttpTransportHealthChecker.java` | HTTP transport：上报遥测、拉取属性 |
| `service/transport/impl/CoapTransportHealthChecker.java` | CoAP transport：上报遥测、observe |
| `service/transport/impl/Lwm2mTransportHealthChecker.java` | LwM2M transport：注册、发数据、observe |
| `notification/NotificationService.java` | 通知路由服务 |
| `notification/channels/NotificationChannel.java` | 通知渠道接口 |
| `notification/channels/impl/SlackNotificationChannel.java` | Slack 通知实现 |
| `notification/incident/IncidentManager.java` | 管理事件单生命周期（开启、解决） |
| `data/notification/` | 通知 DTO：服务故障、恢复、高延迟、信息 |
| `data/Latencies.java` / `data/Latency.java` | 延迟测量模型 |

**监控流程：**
1. 启动时，monitoring 为每个 transport 创建测试实体（设备、设备配置文件）。
2. 按可配置的间隔，各 `TransportHealthChecker` 经对应 transport 连接、发送测试遥测并校验投递。
3. 按 transport 类型采集延迟测量值。
4. 若检查失败或延迟超阈值，`IncidentManager` 开启一个事件单。
5. `NotificationService` 经已配置渠道（Slack 等）发出告警。
6. 恢复后，事件单被关闭，并发出恢复通知。

## tools

**位置：** `tools/`  
**用途：** 数据迁移、导出与诊断的实用工具。

| 文件 | 用途 |
|------|---------|
| `MqttSslClient.java` | 测试 MQTT TLS 连接的 CLI 工具 |
| `migrator/MigratorTool.java` | ThingsBoard 版本升级的迁移工具主入口 |
| `migrator/PgCaMigrator.java` | PostgreSQL 到 Cassandra 的数据迁移工具 |
| `migrator/RelatedEntitiesParser.java` | 为迁移解析实体关系 |
| `migrator/DictionaryParser.java` | 为迁移解析数据字典映射 |
| `migrator/WriterBuilder.java` | 构造迁移数据的输出 writer |
| `i18n/TranslationPruner.java` | 从语言文件里剪除未使用的翻译 key |

**迁移工具：** `migrator/` 包提供在 ThingsBoard 版本之间升级、以及在数据库后端之间迁移
（PostgreSQL 到 Cassandra）的工具。`MigratorTool` 读取源数据、解析实体关系，并把转换后的数据写入
目标数据库。

## msa/ —— 微服务架构 Docker 镜像

**位置：** `msa/`  
**用途：** 全部 ThingsBoard 服务的 Docker 镜像。每个子目录把某个特定服务打包成一个 Docker 容器。

### 模块结构

| 模块 | Dockerfile 位置 | 用途 |
|--------|--------------------|---------|
| `msa/tb-node/` | `msa/tb-node/docker/Dockerfile` | ThingsBoard 主服务节点（核心应用、全部 transport、规则引擎） |
| `msa/tb/` | `msa/tb/docker-postgres/Dockerfile`、`msa/tb/docker-cassandra/Dockerfile` | 主节点的遗留/替代打包（PostgreSQL 与 Cassandra 两个变体） |
| `msa/web-ui/` | `msa/web-ui/docker/Dockerfile` | 经 nginx 提供的前端 Web UI |
| `msa/js-executor/` | `msa/js-executor/docker/Dockerfile` | 执行规则引擎 JavaScript 函数的 Node.js sidecar |
| `msa/transport/` | 各 transport 专属子目录 | 按协议的 transport Docker 镜像：`transport/mqtt/`、`transport/http/`、`transport/coap/`、`transport/lwm2m/`、`transport/snmp/` |
| `msa/edqs/` | `msa/edqs/docker/Dockerfile` | 面向 edge 部署的 EDQS Docker 镜像 |
| `msa/monitoring/` | `msa/monitoring/docker/Dockerfile` | monitoring 服务的 Docker 镜像 |
| `msa/vc-executor/` | —— | 版本控制执行器（仅构建，被 tb-node 使用） |
| `msa/vc-executor-docker/` | `msa/vc-executor-docker/docker/Dockerfile` | 独立的版本控制执行器 Docker 镜像 |
| `msa/black-box-tests/` | —— | 集成与 E2E 测试（不是 Docker 镜像，而是用 docker-compose 的测试套件） |

### 主要 Docker 镜像细节

**tb-node**（`msa/tb-node/`）：一体化的 ThingsBoard 节点。包含核心应用、内嵌 transport、规则引擎和
Web UI。适用于单体部署。构建为多架构镜像（amd64/arm64）。

**web-ui**（`msa/web-ui/`）：提供 Angular 应用的轻量 nginx 容器。用于前端与后端分离部署的场景。

**js-executor**（`msa/js-executor/`）：基于 Node.js 的 gRPC 服务，为规则引擎执行 JavaScript 函数
（Script 节点、filter 节点）。作为主应用的 sidecar 运行，或在 Kubernetes 中作为独立 pod。使用 VM2
沙箱做隔离。

**transport 容器**（`msa/transport/`）：五个独立 Docker 镜像 —— MQTT、HTTP、CoAP、LwM2M、SNMP。
每个只包含该 transport 专属的 Spring Boot 应用。用于 transport 需要独立扩容的微服务部署。

**black-box-tests**（`msa/black-box-tests/`）：经 docker-compose 启动全部服务并运行集成测试的套件，
覆盖设备连接（MQTT、HTTP、CoAP、LwM2M）、REST API 用法、WebSocket 遥测、EDQS 查询、规则引擎节点
和 Selenium UI 冒烟测试。主要测试包：

- `connectivity/` —— 协议级测试（MqttClientTest、HttpClientTest、CoapClientTest、Lwm2mClientTest）
- `connectivity/lwm2m/` —— LwM2M 专项测试（安全模式、observation、组合操作）
- `edqs/` —— EDQS 实体数据查询测试
- `rule/` —— 规则引擎节点测试（MqttNodeTest）
- `security/` —— JS executor 的沙箱隔离测试
- `ui/` —— Selenium WebDriver UI 测试（登录，资产/设备/客户 CRUD，告警分派，配置文件）
- `cf/` —— 计算字段测试

## 关联关系

- [传输层](../40-transport/README.zh-CN.md) —— transport 模块依赖 netty-mqtt 提供 MQTT 客户端能力；
  EDQS 把传输能力延伸到 edge
- [主应用](../50-application/README.zh-CN.md) —— 核心依赖 netty-mqtt 以支撑规则引擎的 MQTT 节点；
  monitoring 检查核心健康
- [前端](../70-frontend/README.zh-CN.md) —— web-ui Docker 镜像打包 Angular 前端
- [公共模块](../10-common/README.zh-CN.md) —— 完整的 Maven reactor 依赖图
