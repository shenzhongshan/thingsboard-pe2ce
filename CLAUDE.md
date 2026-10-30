# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System

Maven (Java 25, Spring Boot 3.5) for backend; Yarn + Angular 20 for frontend.

```bash
# Full build (skip packaging artifacts not needed for tests)
mvn clean install -T6 -DskipTests -Dpkg.skip=true

# Full build with all packaging (bootjar, deb, rpm, zip)
mvn clean install -T6 -DskipTests

# Windows monolith installer only (boot jar + ZIP, no deb/rpm)
# -> application/target/thingsboard-windows.zip
mvn clean install -T6 -DskipTests -Dpkg.skip.deb=true -Dpkg.skip.rpm=true -Dpkg.skip.zip=false

# Frontend
cd ui-ngx && yarn start          # dev server at http://localhost:4200
cd ui-ngx && yarn build:prod     # production build
cd ui-ngx && yarn lint           # ESLint
```

Use `-Dpkg.skip=true` to skip all packaging (bootjar, deb, rpm, zip) — always safe for tests. Individual skip flags: `-Dpkg.skip.bootjar=true`, `-Dpkg.skip.deb=true`, `-Dpkg.skip.rpm=true`, `-Dpkg.skip.zip=true`.

## Running Tests

```bash
# Set memory options for parallel tests
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max_old_space_size=4096"
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"

# Compile without tests first
mvn clean install -T6 -DskipTests -Dpkg.skip=true

# Test all non-application modules in parallel
mvn test -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui' -T4

# Test dao module (parallel by packages)
mvn test -pl dao -Dparallel=packages -DforkCount=4

# Test application module — controller tests
mvn test -pl application \
  -Dtest='!**/nosql/**,org.thingsboard.server.controller.**' \
  -DforkCount=6 -Dparallel=classes \
  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5

# Run a single test class
mvn test -pl application -Dtest=DeviceControllerTest -DfailIfNoTests=false
```

Application tests depend on Testcontainers (PostgreSQL, Kafka, etc.). If Docker API version is incompatible, add `"min-api-version": "1.32"` to `/etc/docker/daemon.json`. If testcontainers can't find Docker, remove `~/.testcontainers.properties`.

## Module Architecture

The Maven reactor builds modules in dependency order (see `<modules>` in `pom.xml`):

| Module | Purpose |
|--------|---------|
| `common/data` | Entity models (Device, Asset, Alarm, Dashboard, etc.), DTOs, IDs, enums |
| `common/message` | Internal message types for rule engine, queue, and clustering |
| `common/queue` | Kafka/pub-sub queue layer |
| `common/dao-api` | DAO interfaces (repositories), service-level contracts |
| `common/cache` | Caffeine-based caching |
| `common/cluster-api` | Cluster communication interfaces |
| `common/actor` | Actor system abstractions |
| `common/proto` | Protobuf definitions |
| `common/edge-api` | Edge sync protobuf definitions, RPC client, exceptions |
| `common/transport` | Transport-layer shared classes |
| `dao` | Data access implementations (SQL via `sql/` + JDBC, NoSQL via `nosql/`). Persists all entities: device, asset, alarm, dashboard, rule chain, edge, audit log, attributes, timeseries, events, relations, widgets, OTA packages, notifications, etc. |
| `rule-engine/rule-engine-api` | Rule engine interfaces |
| `rule-engine/rule-engine-components` | Built-in rule node implementations (filters, transformers, actions, external integrations) |
| `transport/mqtt` | MQTT device transport (Netty-based) |
| `transport/http` | HTTP device transport |
| `transport/coap` | CoAP device transport |
| `transport/lwm2m` | LwM2M device transport |
| `transport/snmp` | SNMP device transport |
| `netty-mqtt` | Custom Netty MQTT codec |
| `edqs` | Entity Data Query Service |
| `application` | Spring Boot application (controllers, services, actors, config). Entry point: `ThingsboardApplication` |
| `ui-ngx` | Angular 20 frontend |
| `msa/tb-node` | Docker image for the main node |
| `msa/web-ui` | Docker image for frontend (nginx-based) |
| `msa/js-executor` | Node.js sidecar for rule engine JS execution |
| `msa/transport` | Docker image for transport services |
| `msa/edqs` | Docker image for EDQS |
| `msa/black-box-tests` | Integration/E2E tests |
| `rest-client` | Java REST client SDK for ThingsBoard API |
| `monitoring` | Micrometer-based monitoring/metrics |
| `tools` | Utility tools (migration, data export, etc.) |

## Database Schema & Migrations

No Liquibase or Flyway. The project uses a custom schema management approach:

- **Schema creation**: Raw SQL files in `dao/src/main/resources/sql/` (e.g., `schema-entities.sql`, `schema-functions.sql`, `schema-ts-psql.sql`). Executed by `SqlAbstractDatabaseSchemaService` at install time via plain JDBC.
- **Version tracking**: The `tb_schema_settings` table stores a `schema_version` (bigint) in a packed format like `4002001001` (= v4.2.1.1). `DatabaseSchemaSettingsService` validates the version on startup — only supported upgrade paths are allowed (set `SKIP_SCHEMA_VERSION_CHECK=true` to bypass).
- **Upgrade scripts**: `SqlDatabaseUpgradeService` loads `schema_update.sql` from `application/src/main/data/upgrade/` (both `basic/` and `lts/` paths).
- The `@Profile("install")` annotation triggers schema creation/upgrade at app startup.

## Dual Database Support (SQL / NoSQL / TimescaleDB)

The application supports three backends for timeseries data. Entity CRUD is always SQL (JPA). Selection is via annotation-based conditional beans defined in `common/dao-api/.../dao/util/`:

| Annotation | Activates when |
|---|---|
| `@SqlDao` | JPA DAO implementations (hardcoded marker) |
| `@NoSqlAnyDao` | `database.ts.type == 'cassandra'` or `database.ts_latest.type == 'cassandra'` |
| `@SqlTsDao` / `@SqlTsLatestDao` | timeseries = SQL |
| `@TimescaleDBTsDao` / `@TimescaleDBTsLatestDao` | timeseries = TimescaleDB |
| `@NoSqlTsDao` / `@NoSqlTsLatestDao` | timeseries = Cassandra |

Controlled by `database.ts.type` and `database.ts_latest.type` in `thingsboard.yml`. Cassandra DAOs live under `dao/.../nosql/`; SQL/TimescaleDB DAOs under `dao/.../sqlts/`. Cassandra is legacy — only timeseries/latest-timeseries DAOs have Cassandra implementations.

## Key Backend Concepts

**Actor system** (`application/.../actors/`): Each tenant, device, and rule chain has its own actor. The actor system processes messages asynchronously using Akka-like patterns. Key actor types: `tenant`, `device`, `ruleChain`, `app`, `calculatedField`, `stats`.

**Rule engine** (`rule-engine/`): Data processing pipelines. Each rule chain is a DAG of rule nodes. Messages flow through filter nodes ("Originator Type", "Message Type Switch"), transformation nodes ("Script", "Change Originator"), and action nodes ("Save Timeseries", "Create Alarm", "RPC Call", "Send Email", "MQTT", "HTTP", etc.).

**Transport layer** (`transport/`): Device connectivity endpoints. Each transport receives telemetry, attribute updates, and RPC responses from devices, converts them to internal messages, and pushes to the queue for processing by the rule engine.

**Queue system** (`common/queue/`): Kafka-based message bus. All transport messages and rule engine outputs flow through the queue. The application consumes queue messages and routes them to appropriate actors.

**Data model** (`common/data/`): Every major entity has its own subpackage with Java records/POJOs, DTOs, and IDs. Entity types: Device, Asset, DeviceProfile, AssetProfile, Customer, User, Tenant, TenantProfile, Dashboard, RuleChain, Alarm, Edge, EntityView, Widget, Resource, OtaPackage, Notification, AiModel, etc.

**Services** (`application/.../service/`): 40+ service packages. Each entity type has a dedicated service. System-level services handle telemetry, queues, subscriptions, state, sync, edge, housekeeping, install, mail, SMS, notifications, security, OTA, etc.

**Controllers** (`application/.../controller/`): REST controllers. Map 1:1 to entity types. BaseController provides common auth/tenant helpers. Authentication is JWT-based (jjwt).

**Edge** (`common/edge-api/`, `application/.../service/edge/`): Synchronization framework for edge deployments. Rule-engine-like processors sync entities (device, asset, alarm, dashboard, rule chain, customer, user, etc.) to remote edge instances via gRPC. EDQS (Entity Data Query Service) is a dedicated edge message queue with its own Docker image and compose files.

**Application configuration** (`application/src/main/resources/thingsboard.yml`): ~2200-line Spring Boot config. Controls databases, caching (Caffeine/Redis), actor system, rate limits, audit logging, version control, OAuth2, mail, EDQS, and more. Most settings are overridable via env vars (e.g., `DATABASE_TS_TYPE`, `TB_QUEUE_TYPE`, `CACHE`).

## Frontend Architecture

Angular 20 with Angular Material 20. Key path aliases: `@app/*`, `@core/*`, `@modules/*`, `@home/*`, `@shared/*`.

- `src/app/core/http/` — HTTP services, one per entity type (e.g., `device.service.ts`)
- `src/app/modules/home/pages/` — Feature pages. Most follow the **entity table pattern**: a `TableConfigResolver` defines columns/actions, `<entity>.component.ts` extends `EntityComponent<T>` for the detail form, `<entity>-tabs.component.ts` for additional tabs
- `src/app/shared/components/` — Shared components (entity selectors, timewindow pickers, image gallery, dialogs, JSON editors)
- `src/app/modules/home/components/entity/` — Framework components: `entities-table.component.ts` (generic table), `entity-details-page.component.ts`, `entity-details-panel.component.ts`
- `src/environments/` — Environment configs

See `ui-ngx/structure.md` for full route-to-source mapping and component catalog.

**i18n**: Uses `@ngx-translate/core` with 28 locale files in `ui-ngx/src/assets/locale/` (pattern: `locale.constant-{lang}_{REGION}.json`). ICU message format support via `@messageformat/core`. Locale is managed through `@ngrx/store`.

**Frontend testing**: No test infrastructure is configured. There is no test runner (no Jest, Karma, or Cypress). The `package.json` has no `"test"` script. A single trivial `auth.service.spec.ts` exists but cannot be executed.

## Docker & Local Development

The `docker/` directory contains docker-compose files for local development with all infrastructure dependencies (PostgreSQL, Kafka/Confluent, Cassandra, Valkey/Redis, Prometheus/Grafana). Configure via `docker/.env` (`DATABASE`, `TB_QUEUE_TYPE`, `CACHE`).

CI uses Docker-in-Docker. Packaging produces `.deb` (via Gradle Maven plugin), `.rpm`, `.zip`, and Docker images. Docker images are multi-arch (amd64/arm64) using Docker buildx. Set `DOCKER_BUILDKIT=0` for local Docker builds.

## Related Repositories

- `shenzhongshan/thingsboard`: unmodified ThingsBoard CE. Use it only as a reference for upstream behaviour.
- `shenzhongshan/thingsboard-pe2ce` (this repo): CE 4.4 with most PE features ported (RBAC / `AccessControlService`, `/api/permissions/allowedPermissions`, entity groups, white labeling, integrations, etc.). `/api/system/info` still reports `"type":"CE"`. Day-to-day development happens on the `mydev-pe` branch.
- `shenzhongshan/flutter_thingsboard_pe_app`: the Flutter mobile app plus the WeChat mini program in `taro_thingsboard_app/` (Taro + React), which talks to this server.

## Mobile Center: WeChat Mini Programs

The mobile center supports `PlatformType.WECHAT_MINIPROGRAM` in addition to Android/iOS. WeChat login is not implemented.

- **App**: the package name is the mini program AppID, validated as `^wx[0-9a-f]{16}$` (`MobileAppDataValidator`). A published mini program needs no store info. Version info (min/latest) drives the mini program's update check.
- **Bundle**: `mobile_app_bundle.wechat_app_id` (unique, FK to `mobile_app`, `ON DELETE SET NULL`). It is defined in `schema-entities.sql` and added by idempotent `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` blocks in both `upgrade/basic` and `upgrade/lts` `schema_update.sql`. Bundle queries join `androidAppId OR iosAppID OR wechatAppId`.
- **Tenant isolation**: each tenant registers and publishes its own mini program. In `MobileAppController#getUserMobileInfo`, a mini program owned by a tenant returns 403 "Mobile application belongs to another tenant" to users of other tenants; a mini program owned by the system administrator serves every tenant. Android/iOS apps keep upstream behaviour. `MobileAppBundleDataValidator` only accepts a mini program from the bundle's own tenant.
- **OAuth2**: `PlatformType.isOAuth2Supported()` is false for mini programs. `Oauth2ClientDataValidator` rejects clients that target them, and the UI offers only `oauth2PlatformTypes` (WEB/ANDROID/IOS).
- **UI**: `ui-ngx/src/app/modules/home/pages/mobile/` (app form, bundle dialog/table, configuration dialog with the mini program build steps). Translations exist in `en_US` and `zh_CN`.
- **Tests**: `MobileAppControllerTest` and `MobileAppBundleControllerTest`.

## Known Build Issues (outside the mobile center)

- `bouncycastle.version` is 1.84, but `org.bouncycastle:bcprov-ext-jdk18on` stops at 1.78.1 on Maven Central, so `integration-tuya` only resolves from a repository or local cache that has it.
- `application/src/test/.../client/NotificationApiClientTest.java` (lines 171-172) fails to compile: calls to `markAllNotificationsAsRead` and `getUnreadNotificationsCount` are ambiguous between overloads of the generated `ThingsboardApi`.
- `ui-ngx` depends on `echarts`/`zrender` GitHub archive tarballs; environments that block GitHub archive downloads need them fetched another way (`git clone` of the `5.5.2-TB` / `5.5.0-TB` tags works).
