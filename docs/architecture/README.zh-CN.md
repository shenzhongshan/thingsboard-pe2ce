# ThingsBoard CE — 技术架构文档

ThingsBoard 是一个开源 IoT 平台，用于数据采集、处理、可视化和设备管理。本套文档覆盖社区版（CE）
的架构、组件编目与模块参考。

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

## 怎么用这套文档

- **刚接触本项目？** 从 [系统总览](00-overview.zh-CN.md) 开始，先建立全局图景。
- **要构建或部署？** 见 [构建系统](01-build-system.zh-CN.md)。
- **找某个具体的 controller/API？** 直接跳到 [REST 控制器](50-application/controllers.md)。
- **做前端？** 从 [前端总览](70-frontend/README.md) 开始。
- **排查数据库问题？** 见 [SQL Schema 与迁移](20-dao/sql-schema.md)。
- **要发版，或要加 schema 变更？** 先读 [版本管理与系统升级指南](04-upgrade-and-release.zh-CN.md)——版本号段选错会静默跳过迁移。

## 文档地图

```
docs/architecture/
├── README.md                                    ← 你在这里
├── 00-overview.md                               系统架构与数据流
├── 01-build-system.md                           Maven reactor、构建命令、打包
├── 02-deployment-modes.md                       单体、Core+RE 拆分、微服务、安装
├── 03-service-code-mapping.md                   服务→Docker 镜像→Maven 模块→主类→源码包
├── 04-upgrade-and-release.md                    版本编号、两条升级路径、发布规则
├── 04-upgrade-and-release.zh-CN.md              ^ 中文版（权威）
│
├── 10-common/                                   ── 公共共享模块 ──
│   ├── README.md                                模块总览与依赖图
│   ├── data.md                                  实体模型、DTO、ID、枚举
│   ├── message.md                               内部消息类型（TbMsg 等）
│   ├── queue.md                                 Kafka / 发布订阅队列抽象
│   ├── dao-api.md                               DAO 接口与数据库类型注解
│   ├── cache.md                                 Caffeine / Redis 双后端缓存
│   ├── cluster-api.md                           节点间集群通信
│   ├── actor.md                                 Actor 系统抽象
│   ├── proto.md                                 Protobuf 定义
│   ├── transport.md                             传输层共享类
│   ├── edge-api.md                              Edge 同步（gRPC、处理器）
│   ├── script.md                                TBEL 与 JS 脚本引擎
│   ├── coap-server.md                           CoAP 服务端工具（Californium）
│   └── util.md                                  公共工具类
│
├── 20-dao/                                      ── 数据访问层 ──
│   ├── README.md                                DAO 架构、JPA/Hibernate、双数据库
│   ├── entity-dao.md                            48 个实体 DAO 包编目
│   └── sql-schema.md                            SQL 文件、版本跟踪、升级
│
├── 30-rule-engine/                              ── 规则引擎 ──
│   ├── README.md                                DAG 处理、TbMsg 生命周期、API
│   └── components.md                            29 类规则节点（223 个文件）
│
├── 40-transport/                                ── 设备传输层 ──
│   └── README.md                                MQTT、HTTP、CoAP、LwM2M、SNMP
│
├── 50-application/                              ── 主应用服务 ──
│   ├── README.md                                Spring Boot 入口、队列消费者
│   ├── controllers.md                           63 个 REST 控制器编目
│   ├── services.md                              44 个 service 包编目
│   ├── actors.md                                Actor 层级与消息路由
│   └── config-and-security.md                   thingsboard.yml、JWT 认证、RBAC、OAuth2
│
├── 60-supporting/                               ── 支撑模块 ──
│   └── README.md                                netty-mqtt、edqs、rest-client、monitoring、msa
│
└── 70-frontend/                                 ── Angular 前端 ──
    ├── README.md                                Angular 20 总览、实体表格范式
    ├── core.md                                  44 个 HTTP service、认证、守卫、NgRx
    ├── shared.md                                20+ 组件组、9 个模型组
    ├── pages.md                                 32 个页面模块、登录模块
    └── theme-i18n.md                            28 种语言、M3 主题、Tailwind
```

## 技术栈

| 层 | 技术 |
|-------|-----------|
| 后端语言 | Java 25 |
| 框架 | Spring Boot 3.5 |
| 构建 | Maven（多模块 reactor） |
| 前端 | Angular 20 + Angular Material 20 |
| 数据库 | PostgreSQL（实体），时序数据可选（SQL / TimescaleDB / Cassandra） |
| 队列 | Kafka（主），内存（开发） |
| 缓存 | Caffeine（本地），Valkey/Redis（分布式） |
| 协议 | MQTT 3.1.1/5.0、HTTP、CoAP、LwM2M、SNMP |
| RPC | gRPC（edge）、REST（API） |
| 序列化 | Protobuf、Jackson JSON |
| 容器化 | Docker、Docker Compose、Docker-in-Docker（CI） |

## 项目数字一览

| 类别 | 数量 |
|----------|-------|
| 源文件总数 | ~9,764 |
| Java 文件 | 4,649 |
| 前端文件（.ts/.html/.scss） | 3,150 |
| 配置文件（.xml/.yml/.properties） | 529 |
| SQL schema 文件 | 14 |
| Protobuf 定义 | 12 |
| Maven 模块 | 30+ |
| REST 控制器 | 63 |
| service 包 | 44 |
| DAO 包 | 48 |
| 规则节点类别 | 29 |
| 传输协议 | 5 |
| 前端页面模块 | 32 |
| i18n 语言 | 28 |

## 核心架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                      FRONTEND (Angular 20)                   │
│                   ui-ngx/ — SPA with Material UI             │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST API + WebSocket
┌──────────────────────────▼──────────────────────────────────┐
│                APPLICATION SERVER (Spring Boot)              │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │Controllers│  │   Services   │  │     Actor System        │ │
│  │ (63 REST) │  │ (44 packages)│  │ (Tenant→Device→RuleChain)│ │
│  └──────────┘  └──────────────┘  └────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                   RULE ENGINE (DAG Pipeline)                  │
│      29 node categories: filters, transforms, actions        │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     QUEUE (Kafka / In-Memory)                 │
│   Telemetry, attributes, RPC, notifications, edge events     │
└──┬──────────┬──────────┬──────────┬──────────┬──────────────┘
   │          │          │          │          │
┌──▼──┐  ┌───▼──┐  ┌───▼──┐  ┌───▼───┐  ┌──▼───┐
│MQTT │  │ HTTP │  │ CoAP │  │LwM2M  │  │ SNMP │
└─────┘  └──────┘  └──────┘  └───────┘  └──────┘
               TRANSPORT LAYER (device connectivity)
```

## 关键架构概念

| 概念 | 说明 | 文档 |
|---------|-------------|----------|
| **租户隔离** | 多租户：每个实体都有 `tenant_id`；TenantId 是所有 DAO 方法的第一个参数 | [DAO 总览](20-dao/README.md) |
| **实体表格范式** | 前端：`TableConfigResolver` → `EntitiesTableComponent` → `EntityComponent<T>` | [前端总览](70-frontend/README.md) |
| **数据库类型注解** | `@SqlDao`、`@NoSqlAnyDao`、`@TimescaleDBTsDao` —— 按数据库后端条件装配 bean | [DAO API](10-common/dao-api.md) |
| **规则链 DAG** | 用 JSON 定义的规则节点图；消息依次穿过 filter → transform → action | [规则引擎](30-rule-engine/README.md) |
| **每实体一个 Actor** | 每个租户、设备、规则链各有一个 actor；按实体串行处理 | [Actors](50-application/actors.md) |
| **Edge 同步** | 云端与 edge 实例之间基于 gRPC 的双向同步 | [Edge API](10-common/edge-api.md) |
| **Schema 版本管理** | 通过 `tb_schema_settings` 自建版本跟踪；不用 Liquibase/Flyway | [SQL Schema](20-dao/sql-schema.md) |

## 速查：常用路径

| 是什么 | 在哪 |
|------|-------|
| 应用入口 | `application/.../ThingsboardApplication.java` |
| 主配置 | `application/src/main/resources/thingsboard.yml` |
| SQL schema 文件 | `dao/src/main/resources/sql/` |
| 升级 SQL 脚本 | `application/src/main/data/upgrade/` |
| REST 控制器 | `application/.../controller/` |
| 业务 service | `application/.../service/` |
| DAO 接口 | `dao/.../dao/`（接口）+ `dao/.../dao/sql/`（JPA 实现） |
| 规则节点实现 | `rule-engine/rule-engine-components/.../rule/engine/` |
| 传输协议处理器 | `transport/<protocol>/src/main/java/` |
| Protobuf 定义 | `common/proto/src/main/proto/` |
| 前端页面 | `ui-ngx/src/app/modules/home/pages/` |
| HTTP service（前端） | `ui-ngx/src/app/core/http/` |
| i18n 语言文件 | `ui-ngx/src/assets/locale/` |
| Docker compose 文件 | `docker/` |
