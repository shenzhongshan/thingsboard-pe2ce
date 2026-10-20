# ThingsBoard CE — Technical Architecture Documentation

ThingsBoard is an open-source IoT platform for data collection, processing, visualization, and device management. This documentation covers the Community Edition (CE) architecture, component catalog, and module reference.

## How to Use This Documentation

- **New to the project?** Start with [System Overview](00-overview.md) for the big picture.
- **Building or deploying?** See [Build System](01-build-system.md).
- **Looking for a specific controller/API?** Jump to [REST Controllers](50-application/controllers.md).
- **Working on the frontend?** Start with [Frontend Overview](70-frontend/README.md).
- **Debugging database issues?** See [SQL Schema & Migrations](20-dao/sql-schema.md).
- **Cutting a release, or adding a schema change?** Read [Versioning & Upgrade Guide](04-upgrade-and-release.md) ([中文](04-upgrade-and-release.zh-CN.md)) first — picking the wrong version segment silently skips migrations.

## Document Map

```
docs/architecture/
├── README.md                                    ← You are here
├── 00-overview.md                               System architecture & data flow
├── 01-build-system.md                           Maven reactor, build commands, packaging
├── 02-deployment-modes.md                       Monolith, Core+RE split, Microservices, Install
├── 03-service-code-mapping.md                   Service→Docker image→Maven module→Main class→Source pkgs
├── 04-upgrade-and-release.md                    Version numbering, the two upgrade paths, release rules
├── 04-upgrade-and-release.zh-CN.md              ^ 中文版
│
├── 10-common/                                   ── Common Shared Modules ──
│   ├── README.md                                Module overview & dependency graph
│   ├── data.md                                  Entity models, DTOs, IDs, enums
│   ├── message.md                               Internal message types (TbMsg, etc.)
│   ├── queue.md                                 Kafka/pub-sub queue abstraction
│   ├── dao-api.md                               DAO interfaces & DB-type annotations
│   ├── cache.md                                 Caffeine/Redis dual-backend caching
│   ├── cluster-api.md                           Inter-node cluster communication
│   ├── actor.md                                 Actor system abstractions
│   ├── proto.md                                 Protobuf definitions
│   ├── transport.md                             Transport-layer shared classes
│   ├── edge-api.md                              Edge sync (gRPC, processors)
│   ├── script.md                                TBEL & JS script engine
│   ├── coap-server.md                           CoAP server utilities (Californium)
│   └── util.md                                  Common utility classes
│
├── 20-dao/                                      ── Data Access Layer ──
│   ├── README.md                                DAO architecture, JPA/Hibernate, dual DB
│   ├── entity-dao.md                            48 entity DAO packages catalog
│   └── sql-schema.md                            SQL files, version tracking, upgrades
│
├── 30-rule-engine/                              ── Rule Engine ──
│   ├── README.md                                DAG processing, TbMsg lifecycle, API
│   └── components.md                            29 rule node categories (223 files)
│
├── 40-transport/                                ── Device Transport Layer ──
│   └── README.md                                MQTT, HTTP, CoAP, LwM2M, SNMP
│
├── 50-application/                              ── Main Application Server ──
│   ├── README.md                                Spring Boot entry, queue consumers
│   ├── controllers.md                           63 REST controllers catalog
│   ├── services.md                              44 service packages catalog
│   ├── actors.md                                Actor hierarchy & message routing
│   └── config-and-security.md                   thingsboard.yml, JWT auth, RBAC, OAuth2
│
├── 60-supporting/                               ── Supporting Modules ──
│   └── README.md                                netty-mqtt, edqs, rest-client, monitoring, msa
│
└── 70-frontend/                                 ── Angular Frontend ──
    ├── README.md                                Angular 20 overview, entity table pattern
    ├── core.md                                  44 HTTP services, auth, guards, NgRx
    ├── shared.md                                20+ component groups, 9 model groups
    ├── pages.md                                 32 page modules, login module
    └── theme-i18n.md                            28 locales, M3 theming, Tailwind
```

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend Language | Java 25 |
| Framework | Spring Boot 3.5 |
| Build | Maven (multi-module reactor) |
| Frontend | Angular 20 + Angular Material 20 |
| Database | PostgreSQL (entities), configurable for timeseries (SQL / TimescaleDB / Cassandra) |
| Queue | Kafka (primary), in-memory (dev) |
| Cache | Caffeine (local), Valkey/Redis (distributed) |
| Protocols | MQTT 3.1.1/5.0, HTTP, CoAP, LwM2M, SNMP |
| RPC | gRPC (edge), REST (API) |
| Serialization | Protobuf, Jackson JSON |
| Containerization | Docker, Docker Compose, Docker-in-Docker (CI) |

## Project by the Numbers

| Category | Count |
|----------|-------|
| Total source files | ~9,764 |
| Java files | 4,649 |
| Frontend files (.ts/.html/.scss) | 3,150 |
| Configuration files (.xml/.yml/.properties) | 529 |
| SQL schema files | 14 |
| Protobuf definitions | 12 |
| Maven modules | 30+ |
| REST controllers | 63 |
| Service packages | 44 |
| DAO packages | 48 |
| Rule node categories | 29 |
| Transport protocols | 5 |
| Frontend page modules | 32 |
| i18n locales | 28 |

## Core Architecture Layers

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

## Key Architectural Concepts

| Concept | Description | Document |
|---------|-------------|----------|
| **Tenant Isolation** | Multi-tenant: every entity has `tenant_id`; TenantId is first param on all DAO methods | [DAO Overview](20-dao/README.md) |
| **Entity Table Pattern** | Frontend: `TableConfigResolver` → `EntitiesTableComponent` → `EntityComponent<T>` | [Frontend Overview](70-frontend/README.md) |
| **DB-Type Annotations** | `@SqlDao`, `@NoSqlAnyDao`, `@TimescaleDBTsDao` — conditional bean routing per database backend | [DAO API](10-common/dao-api.md) |
| **Rule Chain DAG** | JSON-defined graph of rule nodes; messages traverse filter → transform → action | [Rule Engine](30-rule-engine/README.md) |
| **Actor per Entity** | Each tenant, device, and rule chain has an actor; sequential per-entity processing | [Actors](50-application/actors.md) |
| **Edge Sync** | gRPC-based bidirectional sync between cloud and edge instances | [Edge API](10-common/edge-api.md) |
| **Schema Versioning** | Custom version tracking via `tb_schema_settings`; no Liquibase/Flyway | [SQL Schema](20-dao/sql-schema.md) |

## Quick Reference: Common Paths

| What | Where |
|------|-------|
| Application entry point | `application/.../ThingsboardApplication.java` |
| Main configuration | `application/src/main/resources/thingsboard.yml` |
| SQL schema files | `dao/src/main/resources/sql/` |
| Upgrade SQL scripts | `application/src/main/data/upgrade/` |
| REST controllers | `application/.../controller/` |
| Business services | `application/.../service/` |
| DAO interfaces | `dao/.../dao/` (interface) + `dao/.../dao/sql/` (JPA impl) |
| Rule node implementations | `rule-engine/rule-engine-components/.../rule/engine/` |
| Transport protocol handlers | `transport/<protocol>/src/main/java/` |
| Protobuf definitions | `common/proto/src/main/proto/` |
| Frontend pages | `ui-ngx/src/app/modules/home/pages/` |
| HTTP services (frontend) | `ui-ngx/src/app/core/http/` |
| i18n locale files | `ui-ngx/src/assets/locale/` |
| Docker compose files | `docker/` |
