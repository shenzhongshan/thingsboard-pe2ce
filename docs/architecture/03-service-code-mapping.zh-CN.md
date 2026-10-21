# ThingsBoard —— 服务到代码包的映射

**本文是权威版。** 英文版 [03-service-code-mapping.md](03-service-code-mapping.md) 为对照上游 CE 的参照副本，改动请先落在本文。

本文把每个可部署的服务/进程映射到它的源码模块、主类、配置和构建产物。

---

## 1. 完整映射表

| 服务 | Docker 镜像 | Maven 源码模块（groupId:artifactId） | 打包模块（Docker） | 主类 | 语言 |
|---------|-------------|------------------------------------------|---------------------------|------------|----------|
| **Monolith / tb-node** | `tb-node` | `org.thingsboard:application` | `org.thingsboard.msa:tb-node` | `ThingsboardServerApplication` | Java 25 |
| **Monolith+PG** | `tb-postgres` | `org.thingsboard:application` | `org.thingsboard.msa:tb` | `ThingsboardServerApplication` | Java 25 |
| **Monolith+Cassandra** | `tb-cassandra` | `org.thingsboard:application` | `org.thingsboard.msa:tb` | `ThingsboardServerApplication` | Java 25 |
| **MQTT Transport** | `tb-mqtt-transport` | `org.thingsboard.transport:mqtt` | `org.thingsboard.msa.transport:mqtt` | `ThingsboardMqttTransportApplication` | Java 25 |
| **HTTP Transport** | `tb-http-transport` | `org.thingsboard.transport:http` | `org.thingsboard.msa.transport:http` | `ThingsboardHttpTransportApplication` | Java 25 |
| **CoAP Transport** | `tb-coap-transport` | `org.thingsboard.transport:coap` | `org.thingsboard.msa.transport:coap` | `ThingsboardCoapTransportApplication` | Java 25 |
| **LwM2M Transport** | `tb-lwm2m-transport` | `org.thingsboard.transport:lwm2m` | `org.thingsboard.msa.transport:lwm2m` | `ThingsboardLwm2mTransportApplication` | Java 25 |
| **SNMP Transport** | `tb-snmp-transport` | `org.thingsboard.transport:snmp` | `org.thingsboard.msa.transport:snmp` | `ThingsboardSnmpTransportApplication` | Java 25 |
| **EDQS** | `tb-edqs` | `org.thingsboard:edqs` | `org.thingsboard.msa:edqs` | `ThingsboardEdqsApplication` | Java 25 |
| **VC Executor** | `tb-vc-executor` | `org.thingsboard.msa:vc-executor` | `org.thingsboard.msa:vc-executor-docker` | `ThingsboardVersionControlExecutorApplication` | Java 25 |
| **JS Executor** | `tb-js-executor` | `org.thingsboard.msa:js-executor` | （同左） | `server.ts` → `server.js` | Node.js 22 |
| **Web UI** | `tb-web-ui` | `org.thingsboard.msa:web-ui` | （同左） | `server.ts` → `server.js` | Node.js 22 |
| **Monitoring** | `tb-monitoring` | `org.thingsboard:application` | `org.thingsboard.msa:monitoring` | `ThingsboardServerApplication` | Java 25 |

---

## 2. tb-node（Monolith / tb-core / tb-rule-engine）

同一个 Docker 镜像承担三种角色，由 `TB_SERVICE_TYPE` 环境变量区分。

**打包链路：**
```
application/                    (org.thingsboard:application)
  └─ compiles uber-jar → application/target/thingsboard.deb

msa/tb-node/                    (org.thingsboard.msa:tb-node)
  └─ Dockerfile copies .deb from application/
  └─ CMD: start-tb-node.sh
```

**主类：** `org.thingsboard.server.ThingsboardServerApplication`
- 文件：`application/src/main/java/org/thingsboard/server/ThingsboardServerApplication.java`
- 注解：`@SpringBootConfiguration`、`@EnableAsync`、`@EnableScheduling`
- ComponentScan：`org.thingsboard.server`、`org.thingsboard.script`
- 配置：`--spring.config.name=thingsboard` → `thingsboard.yml`

**编译进 application .deb 的源码模块：**

| 依赖 | 源码位置 | 内容 |
|------------|----------------|---------|
| `org.thingsboard:application` | `application/src/main/java/` | 全部 service、controller、actor、config |
| `org.thingsboard:dao` | `dao/src/main/java/` | 实体 DAO、SQL/NoSQL 实现、时序 |
| `org.thingsboard.rule-engine:rule-engine-api` | `rule-engine/rule-engine-api/` | 规则引擎接口、TbNode、TbContext |
| `org.thingsboard.rule-engine:rule-engine-components` | `rule-engine/rule-engine-components/` | 全部 223 个内置规则节点实现 |
| `org.thingsboard.common:actor` | `common/actor/` | Actor 系统抽象 |
| `org.thingsboard.common:queue` | `common/queue/` | Kafka / 内存队列实现 |
| `org.thingsboard.common:cache` | `common/cache/` | Caffeine / Redis 缓存实现 |
| `org.thingsboard.common:cluster-api` | `common/cluster-api/` | 集群服务接口 |
| `org.thingsboard.common:edge-api` | `common/edge-api/` | Edge 同步 protobuf + RPC 客户端 |
| `org.thingsboard.common:edqs` | `common/edqs/` | EDQS 共享类型 |
| `org.thingsboard.common:discovery-api` | `common/discovery-api/` | 服务发现接口 |
| `org.thingsboard.common:stats` | `common/stats/` | 统计聚合 |
| `org.thingsboard.common:util` | `common/util/` | 共享工具类 |
| `org.thingsboard.common:version-control` | `common/version-control/` | 实体版本控制共享类型 |
| `org.thingsboard.common.transport:transport-api` | `common/transport/transport-api/` | 传输会话/上下文抽象 |
| `org.thingsboard.common.transport:mqtt` | `common/transport/mqtt/` | MQTT 传输共享代码（Netty 编解码等） |
| `org.thingsboard.common.transport:http` | `common/transport/http/` | HTTP 传输共享代码 |
| `org.thingsboard.common.transport:coap` | `common/transport/coap/` | CoAP 传输共享代码（Californium） |
| `org.thingsboard.common.transport:lwm2m` | `common/transport/lwm2m/` | LwM2M 传输共享代码（Leshan） |
| `org.thingsboard.common.transport:snmp` | `common/transport/snmp/` | SNMP 传输共享代码 |
| `org.thingsboard.common.script:script-api` | `common/script/script-api/` | 脚本引擎 API（TBEL） |
| `org.thingsboard.common.script:remote-js-client` | `common/script/remote-js-client/` | 远程 JS executor 客户端 |
| **仅运行时** | | |
| `org.thingsboard:ui-ngx` | `ui-ngx/` | 编译后的 Angular 前端（JAR，启动时解包） |

**application 源码包**（`application/src/main/java/org/thingsboard/server/`）：

```
org.thingsboard.server
  ├── ThingsboardServerApplication.java          ← Main class
  ├── actors/                                     ← Actor system
  │   ├── .app/ .calculatedField/ .device/
  │   ├── .ruleChain/ .service/ .shared/
  │   ├── .stats/ .tenant/
  ├── config/                                     ← Spring @Configuration classes
  ├── controller/                                 ← 63 REST controllers
  ├── exception/                                  ← Custom exceptions
  ├── install/                                    ← DB install/upgrade logic
  ├── service/                                    ← 44 business service packages
  │   ├── .action/ .ai/ .apiusage/ .asset/
  │   ├── .cf/ .component/ .device/ .edge/
  │   ├── .edqs/ .entitiy/ .executors/
  │   ├── .gateway_device/ .housekeeper/
  │   ├── .install/ .job/ .lwm2m/ .mail/
  │   ├── .mobile/ .notification/ .ota/
  │   ├── .partition/ .profile/ .query/ .queue/
  │   ├── .resource/ .rpc/ .rule/ .ruleengine/
  │   ├── .script/ .security/ .session/ .sms/
  │   ├── .state/ .stats/ .subscription/
  │   ├── .sync/ .system/ .telemetry/
  │   ├── .transport/ .ttl/ .update/ .user/ .ws/
  └── utils/
```

---

## 3. 传输服务

每个 transport 都是依赖极少的轻量 Spring Boot 应用。它们共享同一套架构：
- 各自包内有自己的主类
- 扫描自己的包 + 共享传输代码 + queue + cache
- 使用各自的配置 YAML（如 `tb-mqtt-transport.yml`）
- 打包成 .deb，安装进 Docker

### 3a. MQTT Transport

**打包链路：**
```
transport/mqtt/                  (org.thingsboard.transport:mqtt)
  └─ compiles → transport/mqtt/target/tb-mqtt-transport.deb

msa/transport/mqtt/              (org.thingsboard.msa.transport:mqtt)
  └─ Dockerfile copies .deb
  └─ CMD: start-tb-mqtt-transport.sh
```

| 属性 | 值 |
|----------|-------|
| 主类 | `org.thingsboard.server.mqtt.ThingsboardMqttTransportApplication` |
| 配置 | `tb-mqtt-transport.yml` |
| ComponentScan | `org.thingsboard.server.mqtt`、`org.thingsboard.server.common`、`org.thingsboard.server.transport.mqtt`、`org.thingsboard.server.queue`、`org.thingsboard.server.cache` |
| Maven 依赖 | `common/transport/mqtt`、`common/queue`、`spring-boot-starter-web` |
| 源码包 | `org.thingsboard.server.mqtt`（单个包，传输处理器） |

### 3b. HTTP Transport

**打包链路：**
```
transport/http/                  (org.thingsboard.transport:http)
  └─ → transport/http/target/tb-http-transport.deb

msa/transport/http/              (org.thingsboard.msa.transport:http)
  └─ Dockerfile copies .deb
```

| 属性 | 值 |
|----------|-------|
| 主类 | `org.thingsboard.server.http.ThingsboardHttpTransportApplication` |
| 配置 | `tb-http-transport.yml` |
| ComponentScan | `org.thingsboard.server.http`、`org.thingsboard.server.common`、`org.thingsboard.server.transport.http`、`org.thingsboard.server.queue`、`org.thingsboard.server.cache` |
| Maven 依赖 | `common/transport/http`、`common/queue`、`spring-boot-starter-web` |
| 源码包 | `org.thingsboard.server.http`（单个包，HTTP controller 处理器） |

### 3c. CoAP Transport

**打包链路：**
```
transport/coap/                  (org.thingsboard.transport:coap)
  └─ → transport/coap/target/tb-coap-transport.deb

msa/transport/coap/              (org.thingsboard.msa.transport:coap)
  └─ Dockerfile copies .deb
```

| 属性 | 值 |
|----------|-------|
| 主类 | `org.thingsboard.server.coap.ThingsboardCoapTransportApplication` |
| 配置 | `tb-coap-transport.yml` |
| ComponentScan | `org.thingsboard.server.coap`、`org.thingsboard.server.common`、`org.thingsboard.server.coapserver`、`org.thingsboard.server.transport.coap`、`org.thingsboard.server.queue`、`org.thingsboard.server.cache` |
| Maven 依赖 | `common/transport/coap`、`common/queue`、`common/coap-server`、`spring-boot-starter-web` |
| 源码包 | `org.thingsboard.server.coap`（CoAP 设备端点） |

### 3d. LwM2M Transport

**打包链路：**
```
transport/lwm2m/                 (org.thingsboard.transport:lwm2m)
  └─ → transport/lwm2m/target/tb-lwm2m-transport.deb

msa/transport/lwm2m/             (org.thingsboard.msa.transport:lwm2m)
  └─ Dockerfile copies .deb
```

| 属性 | 值 |
|----------|-------|
| 主类 | `org.thingsboard.server.lwm2m.ThingsboardLwm2mTransportApplication` |
| 配置 | `tb-lwm2m-transport.yml` |
| ComponentScan | `org.thingsboard.server.lwm2m`、`org.thingsboard.server.common`、`org.thingsboard.server.transport.lwm2m`、`org.thingsboard.server.queue`、`org.thingsboard.server.cache` |
| Maven 依赖 | `common/transport/lwm2m`、`common/queue`、`common/cache`、`leshan-server-cf`、`leshan-client-cf`、`leshan-server-redis` |
| 源码包 | `org.thingsboard.server.lwm2m`（LwM2M bootstrap、注册、资源处理） |

### 3e. SNMP Transport

**打包链路：**
```
transport/snmp/                  (org.thingsboard.transport:snmp)
  └─ → transport/snmp/target/tb-snmp-transport.deb

msa/transport/snmp/              (org.thingsboard.msa.transport:snmp)
  └─ Dockerfile copies .deb
```

| 属性 | 值 |
|----------|-------|
| 主类 | `org.thingsboard.server.snmp.ThingsboardSnmpTransportApplication` |
| 配置 | `tb-snmp-transport.yml` |
| ComponentScan | `org.thingsboard.server.snmp`、`org.thingsboard.server.common`、`org.thingsboard.server.transport.snmp`、`org.thingsboard.server.queue`、`org.thingsboard.server.cache` |
| Maven 依赖 | `common/transport/snmp`、`common/queue`、`spring-boot-starter-web` |
| 源码包 | `org.thingsboard.server.snmp`（SNMP trap/listener 处理） |

---

## 4. EDQS（实体数据查询服务）

**打包链路：**
```
edqs/                            (org.thingsboard:edqs)
  └─ compiles → edqs/target/tb-edqs.deb

msa/edqs/                        (org.thingsboard.msa:edqs)
  └─ Dockerfile copies .deb from edqs/
```

| 属性 | 值 |
|----------|-------|
| 主类 | `org.thingsboard.server.edqs.ThingsboardEdqsApplication` |
| 配置 | `edqs.yml` |
| 注解 | `@SpringBootConfiguration`、`@EnableAutoConfiguration`、`@EnableAsync`、`@EnableScheduling` |
| ComponentScan | `org.thingsboard.server.edqs`、`org.thingsboard.server.queue.edqs`、`org.thingsboard.server.queue.discovery`、`org.thingsboard.server.queue.kafka`、`org.thingsboard.server.queue.settings`、`org.thingsboard.server.queue.environment`、`org.thingsboard.server.common.stats` |
| Maven 依赖 | `common/edqs`、`curator-recipes`（ZooKeeper）、`grpc-protobuf` |
| 源码包 | `org.thingsboard.server.edqs`（单个包，1 个 controller 类） |

---

## 5. VC Executor（版本控制执行器）

**打包链路：**
```
msa/vc-executor/                 (org.thingsboard.msa:vc-executor, packaging: jar)
  └─ compiles → msa/vc-executor/target/tb-vc-executor.deb

msa/vc-executor-docker/          (org.thingsboard.msa:vc-executor-docker, packaging: pom)
  └─ Dockerfile copies .deb from vc-executor/
```

| 属性 | 值 |
|----------|-------|
| 主类 | `org.thingsboard.server.vc.ThingsboardVersionControlExecutorApplication` |
| 配置 | `tb-vc-executor.yml` |
| 注解 | `@SpringBootApplication`、`@EnableAsync`、`@EnableScheduling` |
| ComponentScan | `org.thingsboard.server`、`org.thingsboard.server.common`、`org.thingsboard.server.service.sync.vc` |
| Maven 依赖 | `common/queue`、`common/version-control`、`grpc-netty-shaded`、`spring-boot-starter-web` |
| 源码包 | `org.thingsboard.server.vc`、`org.thingsboard.server.vc.service`（版本控制的 Git 操作） |

---

## 6. JS Executor（Node.js / TypeScript）

**打包链路：**
```
msa/js-executor/                 (org.thingsboard.msa:js-executor, packaging: pom)
  └─ yarn compiles TypeScript
  └─ Dockerfile copies server.js + api/ + queue/ + config/
```

| 属性 | 值 |
|----------|-------|
| 入口 | `msa/js-executor/server.ts`（async IIFE） |
| 语言 | TypeScript → JavaScript（Node.js 22） |
| 基础镜像 | `thingsboard/node:22.22.2-bookworm-slim` |
| 通信 | Kafka（`kafkajs` 库） |
| 源码结构 | |
| `server.ts` | 主入口 —— 初始化 HTTP 服务、Kafka 消费者，加载处理器 |
| `api/httpServer.ts` | 用于健康检查的 Express HTTP 服务 |
| `api/jsExecutor.ts` | 核心 JS 执行引擎（沙箱化的 `vm` 上下文） |
| `api/jsInvokeMessageProcessor.ts` | JS eval 请求的 Kafka 消息处理器 |
| `api/jsExecutor.models.ts` | eval 请求/响应的 TypeScript 接口 |
| `api/utils.ts` | 工具函数 |
| `config/logger.ts` | Winston logger 配置 |
| `queue/kafkaTemplate.ts` | Kafka 生产者/消费者模板 |
| `queue/queue.models.ts` | 队列消息类型定义 |

---

## 7. Web UI（Node.js / TypeScript）

**打包链路：**
```
msa/web-ui/                      (org.thingsboard.msa:web-ui, packaging: pom)
  └─ Extracts ui-ngx JAR into web/
  └─ yarn compiles TypeScript
  └─ Dockerfile copies server.js + web/ + config/
```

| 属性 | 值 |
|----------|-------|
| 入口 | `msa/web-ui/server.ts`（async IIFE） |
| 语言 | TypeScript → JavaScript（Node.js 22） |
| 基础镜像 | `thingsboard/node:22.22.2-bookworm-slim` |
| 上游产物 | `org.thingsboard:ui-ngx`（预构建的 Angular SPA，解包到 `web/`） |
| 源码结构 | |
| `server.ts` | Express HTTP 服务 —— 从 `web/` 提供静态文件、健康检查 |
| `config/default.yml` | 默认配置（端口、绑定地址、代理设置） |
| `config/custom-environment-variables.yml` | 环境变量覆盖映射 |
| `config/logger.ts` | Winston logger 配置 |
| `install.js` | 安装/初始化脚本 |

---

## 8. 源码模块依赖图

```
                           ┌─────────────────────────────┐
                           │  ui-ngx (Angular frontend)   │── runtime dep of application
                           └─────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                    application (tb-node / monolith)                  │
│  ThingsboardServerApplication                                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │actors/   │ │controllers│ │service/  │ │config/   │ │install/  │ │
│  │          │ │(63 REST) │ │(44 pkgs) │ │          │ │          │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
└────────────────────────────┬───────────────────────────────────────┘
                             │ depends on
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐
│    dao       │  │  rule-engine │  │  common/*            │
│  (entities,  │  │  (api +      │  │  actor, queue,       │
│   timeseries)│  │   components)│  │  cache, cluster-api, │
│              │  │              │  │  edge-api, edqs,     │
│              │  │              │  │  discovery-api,      │
│              │  │              │  │  stats, util,        │
│              │  │              │  │  version-control     │
└──────────────┘  └──────────────┘  └──────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                transport/* (lightweight microservices)        │
│                                                              │
│  mqtt/  → ThingsboardMqttTransportApplication                │
│  http/  → ThingsboardHttpTransportApplication                │
│  coap/  → ThingsboardCoapTransportApplication                │
│  lwm2m/ → ThingsboardLwm2mTransportApplication               │
│  snmp/  → ThingsboardSnmpTransportApplication                │
│                                                              │
│  Each depends on: common/transport/<proto>, common/queue,    │
│                    common/cache, spring-boot-starter-web     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    edqs/ (standalone service)                 │
│  ThingsboardEdqsApplication                                  │
│  depends on: common/edqs, common/queue (Kafka),              │
│              curator-recipes (ZooKeeper)                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              msa/vc-executor/ (standalone service)            │
│  ThingsboardVersionControlExecutorApplication                │
│  depends on: common/queue, common/version-control, gRPC      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│     msa/js-executor/  │   msa/web-ui/   (Node.js / TS)      │
│  server.ts → server.js│ server.ts → server.js               │
│  Kafka consumer       │ Express HTTP server                  │
│  Sandboxed JS exec    │ Serves Angular SPA                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. 配置文件映射

| 服务 | 配置文件 | 配置位置 |
|---------|------------|-----------------|
| tb-node（monolith/tb-core/tb-rule-engine） | `thingsboard.yml` | `application/src/main/resources/` |
| EDQS | `edqs.yml` | `edqs/src/main/resources/` |
| MQTT Transport | `tb-mqtt-transport.yml` | `transport/mqtt/src/main/resources/` |
| HTTP Transport | `tb-http-transport.yml` | `transport/http/src/main/resources/` |
| CoAP Transport | `tb-coap-transport.yml` | `transport/coap/src/main/resources/` |
| LwM2M Transport | `tb-lwm2m-transport.yml` | `transport/lwm2m/src/main/resources/` |
| SNMP Transport | `tb-snmp-transport.yml` | `transport/snmp/src/main/resources/` |
| VC Executor | `tb-vc-executor.yml` | `msa/vc-executor/src/main/resources/` |
| JS Executor | `config/logger.ts` | `msa/js-executor/config/` |
| Web UI | `config/default.yml` | `msa/web-ui/config/` |

---

## 10. Dockerfile 到源码的映射

每个 Docker 镜像都由一个包装了源码模块的 Maven 打包模块构建而来：

```
Source (Java JAR)                     Packaging (Docker)                Docker Image
─────────────────                     ───────────────────               ─────────────
application/                   →      msa/tb-node/              →      tb-node
application/                   →      msa/tb/                   →      tb-postgres
application/                   →      msa/tb/                   →      tb-cassandra
application/                   →      msa/monitoring/           →      tb-monitoring
transport/mqtt/                →      msa/transport/mqtt/       →      tb-mqtt-transport
transport/http/                →      msa/transport/http/       →      tb-http-transport
transport/coap/                →      msa/transport/coap/       →      tb-coap-transport
transport/lwm2m/              →      msa/transport/lwm2m/      →      tb-lwm2m-transport
transport/snmp/               →      msa/transport/snmp/       →      tb-snmp-transport
edqs/                          →      msa/edqs/                 →      tb-edqs
msa/vc-executor/              →      msa/vc-executor-docker/    →      tb-vc-executor
msa/js-executor/              →      (self)                     →      tb-js-executor
msa/web-ui/                    →      (self)                    →      tb-web-ui
```

所有 Java 服务使用基础镜像 `thingsboard/openjdk25:trixie-slim`。  
所有 Node.js 服务使用基础镜像 `thingsboard/node:22.22.2-bookworm-slim`。

每个 Java 服务都会安装一个 `.deb` 包、把 JAR 设为可执行（`chmod 555`），然后运行一个 `start-*.sh`
脚本，以 `--spring.config.name=<service>` 启动该 JAR。
