# Supporting Modules

Supporting modules provide infrastructure, tooling, and client libraries that complement the core ThingsBoard platform. These include custom protocol codecs, edge services, Java client SDKs, monitoring tools, data migration utilities, and Docker packaging for all services.

## netty-mqtt

**Location:** `netty-mqtt/`  
**Purpose:** Custom Netty-based MQTT client with support for MQTT 3.1.1 and MQTT 5.0.

This module implements a fully asynchronous MQTT client built on top of Netty. It is used internally by ThingsBoard for MQTT-based rule engine nodes, integration tests, and the MQTT transport's outbound messaging.

| File | Purpose |
|------|---------|
| `MqttClient.java` | Client interface: connect, publish, subscribe, unsubscribe, disconnect |
| `MqttClientImpl.java` | Full async implementation with connection, subscription, and publishing |
| `MqttClientConfig.java` | Client configuration (host, port, client ID, credentials, last will, SSL, reconnect) |
| `MqttChannelHandler.java` | Netty channel handler: processes CONNACK, PUBLISH, SUBACK, UNSUBACK, PUBACK, PUBREC, PUBREL, PUBCOMP, DISCONNECT |
| `MqttHandler.java` | Message handler interface for incoming PUBLISH messages |
| `MqttClientCallback.java` | Connection-level event callback (connection ack, publish ack, subscription ack, disconnect) |
| `MqttConnectResult.java` | Connection result with success/failure and return code |
| `MqttSubscription.java` | Subscription representation with topic filter, handler, and QoS |
| `MqttPendingSubscription.java` | Pending subscription with future completion |
| `MqttPendingPublish.java` | Pending publish with retransmission and acknowledgment tracking |
| `MqttPendingUnsubscription.java` | Pending unsubscription |
| `MqttIncomingQos2Publish.java` | Tracks incoming QoS 2 publish (PUBREC/PUBREL/PUBCOMP flow) |
| `MqttLastWill.java` | Last Will and Testament configuration |
| `MqttPingHandler.java` | Keep-alive PINGREQ/PINGRESP handler |
| `RetransmissionHandler.java` | QoS 1/2 message retransmission with backoff |
| `ReconnectStrategy.java` | Interface for reconnection logic |
| `ReconnectStrategyExponential.java` | Exponential backoff reconnect strategy |
| `ChannelClosedException.java` | Exception for channel closure |
| `MaxRetransmissionsReachedException.java` | Exception when max retransmissions exceeded |

**MQTT 3.1.1 vs 5.0:** The client supports both versions via `MqttVersion` enum. Version 5.0 adds reason codes, properties, session expiry, and other enhancements.

**QoS Support:** Full support for QoS 0 (at most once), QoS 1 (at least once), and QoS 2 (exactly once), including the complete PUBREC/PUBREL/PUBCOMP handshake.

**Reconnection:** Automatic reconnection with exponential backoff. The `ReconnectStrategyExponential` implementation handles increasing delays between retry attempts.

**Usage:** This library is a dependency of the MQTT transport (`common/transport/mqtt`), the monitoring module, integration tests, and MQTT rule engine nodes.

## edqs

**Location:** `edqs/`  
**Purpose:** Entity Data Query Service for edge deployments.

EDQS is a lightweight Spring Boot service that provides a subset of ThingsBoard entity data query functionality for use at the edge. It runs alongside edge instances to cache and serve entity, relation, and attribute data without requiring a connection to the central ThingsBoard server.

| File | Purpose |
|------|---------|
| `ThingsboardEdqsApplication.java` | Spring Boot application entry point |
| `EdqsController.java` | REST controller exposing `/api/edqs/ready` health endpoint |

**How EDQS differs from the main queue:** Unlike the main Kafka-based queue system, EDQS runs as a dedicated HTTP service. It maintains an in-memory data repository (backed by the `common/edqs` module) that mirrors the edge's subset of entity data. The core application pushes data updates to EDQS, and edge rule chains query EDQS for entity lookups and relation traversals.

**Key dependencies:** EDQS relies on `common/edqs` for data repository logic and `common/data` for entity models. It integrates with the edge management system to sync data subsets.

## rest-client

**Location:** `rest-client/`  
**Purpose:** Java REST client SDK for the ThingsBoard API.

A comprehensive Java client library that wraps the entire ThingsBoard REST API. It handles authentication (JWT), HTTP request construction, response deserialization, and provides typed methods for all entity operations.

| File | Purpose |
|------|---------|
| `RestClient.java` | Main client class with methods for all entity types: login, CRUD for devices/assets/customers/users/dashboards/rule-chains/edges/tenants, telemetry, attributes, alarms, audit logs, OTA packages, etc. |
| `utils/RestJsonConverter.java` | Jackson-based JSON serialization/deserialization utilities |

**Authentication:** The `RestClient` supports username/password login which returns a JWT token. The token is automatically included in subsequent request headers via a Spring `RestTemplate` interceptor.

**Key method categories:**
- **Login/Auth:** `login()`, `getUser()`, `refreshToken()`
- **Device:** `saveDevice()`, `getDeviceById()`, `getTenantDevices()`, `saveDeviceCredentials()`
- **Asset:** `saveAsset()`, `getAssetById()`, `getTenantAssets()`
- **Telemetry:** `saveDeviceTelemetry()`, `getLatestTimeseries()`
- **Alarm:** `createAlarm()`, `getAlarms()`, `clearAlarm()`, `ackAlarm()`
- **Dashboard:** `saveDashboard()`, `getDashboard()`, `publishDashboardToCustomer()`
- **Rule Chain:** `saveRuleChain()`, `getRuleChains()`, `setRootRuleChain()`
- **RPC:** `handleOneWayDeviceRPCRequest()`, `handleTwoWayDeviceRPCRequest()`
- **Edge:** `saveEdge()`, `getTenantEdges()`, `assignEdgeToCustomer()`
- **Admin:** `getAdminSettings()`, `saveAdminSettings()`, `sendTestMail()`

**Usage:** The rest-client is used by:
- `msa/black-box-tests/` for integration and E2E testing
- External Java applications integrating with ThingsBoard
- The monitoring module for health checks

## monitoring

**Location:** `monitoring/`  
**Purpose:** Micrometer-based service monitoring and health checking.

A standalone Spring Boot application that monitors ThingsBoard service health. It can check all transport endpoints (MQTT, HTTP, CoAP, LwM2M), the web UI, the core application, and EDQS. It reports failures and high latency via configurable notification channels (e.g., Slack).

| File | Purpose |
|------|---------|
| `ThingsboardMonitoringApplication.java` | Monitoring application entry point |
| `config/MonitoringConfig.java` | Configuration for monitored targets and notification channels |
| `config/MonitoringTarget.java` | Base class for monitoring target configuration |
| `config/transport/` | Transport-specific monitoring configs (Mqtt, Http, Coap, Lwm2m) |
| `client/TbClient.java` | ThingsBoard REST API client for monitoring |
| `client/WsClient.java` | WebSocket client for telemetry subscription verification |
| `client/Lwm2mClient.java` | LwM2M client for LwM2M transport health checks |
| `service/BaseMonitoringService.java` | Base monitoring service with scheduling |
| `service/MonitoringReporter.java` | Metric reporting and evaluation |
| `service/MonitoringEntityService.java` | Entity setup for test devices |
| `service/transport/TransportsMonitoringService.java` | Orchestrates transport health checks |
| `service/transport/impl/MqttTransportHealthChecker.java` | MQTT transport: connect, publish telemetry, subscribe to attributes |
| `service/transport/impl/HttpTransportHealthChecker.java` | HTTP transport: post telemetry, get attributes |
| `service/transport/impl/CoapTransportHealthChecker.java` | CoAP transport: post telemetry, observe |
| `service/transport/impl/Lwm2mTransportHealthChecker.java` | LwM2M transport: register, send data, observe |
| `notification/NotificationService.java` | Notification routing service |
| `notification/channels/NotificationChannel.java` | Notification channel interface |
| `notification/channels/impl/SlackNotificationChannel.java` | Slack notification implementation |
| `notification/incident/IncidentManager.java` | Manages incident lifecycle (open, resolve) |
| `data/notification/` | Notification DTOs: service failure, recovery, high latency, info |
| `data/Latencies.java` / `data/Latency.java` | Latency measurement models |

**Monitoring workflow:**
1. On startup, monitoring creates test entities (devices, device profiles) for each transport.
2. At configurable intervals, each `TransportHealthChecker` connects via the transport, sends test telemetry, and verifies delivery.
3. Latency measurements are collected per transport type.
4. If a check fails or latency exceeds thresholds, `IncidentManager` opens an incident.
5. `NotificationService` sends alerts via configured channels (Slack, etc.).
6. On recovery, the incident is resolved with a recovery notification.

## tools

**Location:** `tools/`  
**Purpose:** Utility tools for data migration, export, and diagnostics.

| File | Purpose |
|------|---------|
| `MqttSslClient.java` | CLI tool for testing MQTT TLS connections |
| `migrator/MigratorTool.java` | Main migration tool entry point for ThingsBoard version upgrades |
| `migrator/PgCaMigrator.java` | PostgreSQL to Cassandra data migration tool |
| `migrator/RelatedEntitiesParser.java` | Parses entity relationships for migration |
| `migrator/DictionaryParser.java` | Parses data dictionary mappings for migration |
| `migrator/WriterBuilder.java` | Build output writers for migration data |
| `i18n/TranslationPruner.java` | Prunes unused translation keys from locale files |

**Migration tools:** The `migrator/` package provides tools for upgrading ThingsBoard between versions and migrating between database backends (PostgreSQL to Cassandra). The `MigratorTool` reads source data, parses entity relationships, and writes transformed data to the target database.

## msa/ -- Microservice Architecture Docker Images

**Location:** `msa/`  
**Purpose:** Docker images for all ThingsBoard services. Each subdirectory packages a specific service into a Docker container.

### Module Structure

| Module | Dockerfile Location | Purpose |
|--------|--------------------|---------|
| `msa/tb-node/` | `msa/tb-node/docker/Dockerfile` | Main ThingsBoard server node (core application, all transports, rule engine) |
| `msa/tb/` | `msa/tb/docker-postgres/Dockerfile`, `msa/tb/docker-cassandra/Dockerfile` | Legacy/alternative main node packaging (PostgreSQL and Cassandra variants) |
| `msa/web-ui/` | `msa/web-ui/docker/Dockerfile` | Frontend web UI served via nginx |
| `msa/js-executor/` | `msa/js-executor/docker/Dockerfile` | Node.js sidecar for executing rule engine JavaScript functions |
| `msa/transport/` | Transport-specific subdirectories | Per-protocol transport Docker images: `transport/mqtt/`, `transport/http/`, `transport/coap/`, `transport/lwm2m/`, `transport/snmp/` |
| `msa/edqs/` | `msa/edqs/docker/Dockerfile` | EDQS Docker image for edge deployments |
| `msa/monitoring/` | `msa/monitoring/docker/Dockerfile` | Monitoring service Docker image |
| `msa/vc-executor/` | — | Version control executor (build only, used by tb-node) |
| `msa/vc-executor-docker/` | `msa/vc-executor-docker/docker/Dockerfile` | Standalone version control executor Docker image |
| `msa/black-box-tests/` | — | Integration and E2E tests (not a Docker image, but a test suite using docker-compose) |

### Key Docker Image Details

**tb-node** (`msa/tb-node/`): The all-in-one ThingsBoard node. Includes the core application with embedded transports, rule engine, and Web UI. Suitable for monolithic deployments. Built as a multi-arch image (amd64/arm64).

**web-ui** (`msa/web-ui/`): Lightweight nginx container serving the Angular application. Used when the frontend is deployed separately from the backend.

**js-executor** (`msa/js-executor/`): Node.js-based gRPC service that executes JavaScript functions for the rule engine (Script nodes, filter nodes). Runs as a sidecar to the main application or as a separate pod in Kubernetes. Uses the VM2 sandbox for isolation.

**transport containers** (`msa/transport/`): Five separate Docker images -- MQTT, HTTP, CoAP, LwM2M, and SNMP. Each contains just the transport-specific Spring Boot application. Used in microservice deployments where transports scale independently.

**black-box-tests** (`msa/black-box-tests/`): Test suite that starts all services via docker-compose and runs integration tests covering device connectivity (MQTT, HTTP, CoAP, LwM2M), REST API usage, WebSocket telemetry, EDQS queries, rule engine nodes, and Selenium UI smoke tests. Key test packages:

- `connectivity/` -- Protocol-level tests (MqttClientTest, HttpClientTest, CoapClientTest, Lwm2mClientTest)
- `connectivity/lwm2m/` -- LwM2M-specific tests (security modes, observation, composite operations)
- `edqs/` -- EDQS entity data query tests
- `rule/` -- Rule engine node tests (MqttNodeTest)
- `security/` -- Sandbox isolation tests for JS executor
- `ui/` -- Selenium WebDriver UI tests (login, asset/device/customer CRUD, alarm assignment, profiles)
- `cf/` -- Calculated field tests

## Relationships

- [Transport Layer](../40-transport/README.md) -- Transport modules depend on netty-mqtt for MQTT client functionality; EDQS extends transport capabilities to the edge
- [Core Application](../50-application/README.md) -- Core depends on netty-mqtt for rule engine MQTT nodes; monitoring checks core health
- [Frontend](../70-frontend/README.md) -- The web-ui Docker image packages the Angular frontend
- [Common Modules](../10-common/README.md) -- Full Maven reactor dependency graph
