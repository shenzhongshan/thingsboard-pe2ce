# Transport Layer

The Transport Layer provides device connectivity endpoints for the ThingsBoard IoT platform. It enables devices to connect and exchange data via multiple IoT protocols. Each transport is packaged as an independent Maven module with its own Spring Boot application, allowing deployments to scale transport services independently from the core application.

## Architecture Overview

The transport layer follows a layered architecture:

```
Device/Client
    |
    v
Transport Protocol Handler (MQTT/HTTP/CoAP/LwM2M/SNMP)
    |
    v
TransportAdaptor (JSON or Protobuf parsing)
    |
    v
TransportService (session management, authentication, routing)
    |
    v
Queue (Kafka / pub-sub)
    |
    v
Rule Engine / Core Application
```

Key design principles:

- **Protocol-specific handlers** decode raw bytes into ThingsBoard message formats.
- **TransportAdaptors** convert protocol payloads into the internal `SessionContext` and telemetry/attribute/RPC messages. Each transport supports both JSON and Protobuf serialization.
- **TransportService** (defined in `common/transport/transport-api`) handles device authentication, session registration, and forwarding messages to the rule engine via the queue.
- **Shared abstractions** in `common/transport/` allow each transport module to reuse authentication, SSL, rate limiting, and session management logic.

## Message Flow

### Uplink (Device to ThingsBoard)

1. Device sends data over its transport protocol (e.g., MQTT PUBLISH).
2. Protocol handler (`MqttTransportHandler`, `DeviceApiController`, etc.) receives the raw message.
3. `TransportAdaptor` (JSON or Protobuf) converts the raw payload into a protobuf `PostTelemetryMsg`, `PostAttributeMsg`, or `ToServerRpcRequestMsg`.
4. `TransportService.process()` sends the protobuf message to the queue.
5. Core application consumes the queue message and routes it through the appropriate rule chain.

### Downlink (ThingsBoard to Device)

1. Rule engine or RPC call produces a `ToDeviceRpcRequestMsg` or attribute update.
2. Queue delivers the message to the transport service.
3. `SessionMsgListener` receives the message and the transport protocol handler delivers it to the device (e.g., MQTT PUBLISH to a subscribed topic, CoAP response, HTTP long-poll).

## Shared Transport Abstractions

The `common/transport/transport-api` module provides interfaces and base implementations shared by all transports:

| File | Purpose |
|------|---------|
| `TransportService.java` | Core interface for session management, telemetry/attribute posting, RPC handling, device authentication, and queue communication |
| `DefaultTransportService.java` | Default implementation with queue integration, session registry, rate limiting, and activity reporting |
| `TransportAdaptor.java` | Generic interface for converting protocol-specific payloads into internal messages |
| `SessionMsgListener.java` | Callback interface for downlink messages (RPC requests, attribute updates, etc.) |
| `TransportContext.java` | Transport-level configuration and shared state |
| `auth/DeviceAuthService.java` | Interface for validating device credentials |
| `auth/TransportDeviceInfo.java` | Device identity information extracted during authentication |
| `auth/ValidateDeviceCredentialsResponse.java` | Authentication result with device/tenant IDs |
| `auth/SessionInfoCreator.java` | Creates protobuf `SessionInfoProto` from auth result |
| `session/DeviceAwareSessionContext.java` | Abstract base session holding device ID, tenant ID, device profile, and connection state |
| `session/SessionContext.java` | Session lifecycle interface |
| `service/DefaultTransportDeviceProfileCache.java` | Cached device profile lookup for transport nodes |
| `service/DefaultTransportResourceCache.java` | Cached resource lookup |
| `service/ToRuleEngineMsgEncoder.java` | Encodes messages destined for the rule engine |
| `service/ToTransportMsgResponseDecoder.java` | Decodes responses from the core back to transports |
| `activity/ActivityManager.java` | Tracks device connectivity activity |
| `activity/strategy/` | Activity reporting strategies (all events, first, last, first+last) |
| `limits/TransportRateLimitService.java` | Per-device and per-tenant message rate limiting |
| `config/ssl/` | SSL credential management (PEM and Keystore formats) |

## Device Authentication

Each transport authenticates devices using one of the supported credential types:

| Credential Type | Authentication Method | Supported Transports |
|----------------|---------------------|---------------------|
| Access Token | Token string passed in payload/URL | MQTT, HTTP, CoAP |
| Basic (Username/Password) | Username + password via MQTT CONNECT / HTTP Basic Auth | MQTT, HTTP |
| X.509 Certificate | TLS client certificate | MQTT, HTTP, CoAP, LwM2M |
| LwM2M Credentials | PSK, RPK, or X.509 specific to LwM2M | LwM2M |

The `DeviceAuthService` interface validates credentials by communicating with the core application through the transport API queue. Authenticated sessions are tracked in the `TransportService` session registry.

## Gateway Device Support

ThingsBoard supports gateway devices that proxy telemetry for multiple constrained devices through a single connection. Gateway support is built into the MQTT transport and can be configured for other transports:

- A gateway device connects and authenticates like any other device.
- When a gateway publishes data on behalf of a child device, the transport calls `TransportService.process()` with `GetOrCreateDeviceFromGatewayRequestMsg`.
- The transport core auto-creates or looks up the child device by name, assigns it to the gateway, and uses the child's device profile for further processing.
- Gateway-specific session classes: `GatewayDeviceSessionContext`, `GatewaySessionHandler`, `AbstractGatewaySessionHandler`.

## Transport Modules

Each transport is structured as two modules:
- **Top-level module** (`transport/<name>/`): Thin Spring Boot application entry point.
- **Common module** (`common/transport/<name>/`): Protocol-specific logic shared with the core.

### MQTT Transport

**Protocol:** MQTT 3.1.1 and MQTT 5.0  
**Default port:** 1883 (plain), 8883 (TLS)  
**Netty-based:** Uses Netty's `io.netty.handler.codec.mqtt` codec  
**Module entry point:** `transport/mqtt/` -> `ThingsboardMqttTransportApplication.java`

| File | Purpose |
|------|---------|
| `MqttTransportContext.java` | Transport-level config (SSL, rate limits, adaptors) |
| `MqttTransportServerInitializer.java` | Netty channel pipeline setup with MQTT encoder/decoder and SSL |
| `MqttTransportHandler.java` | Main Netty handler: processes CONNECT, PUBLISH, SUBSCRIBE, DISCONNECT |
| `MqttTransportService.java` | Registers/deregisters sessions, manages gateway subscriptions |
| `MqttSslHandlerProvider.java` | SSL context creation for MQTT TLS connections |
| `adaptors/JsonMqttAdaptor.java` | JSON payload conversion for telemetry/attributes/RPC |
| `adaptors/ProtoMqttAdaptor.java` | Protobuf payload conversion |
| `adaptors/MqttTransportAdaptor.java` | Adaptor interface for MQTT |
| `adaptors/BackwardCompatibilityAdaptor.java` | Supports legacy v1 JSON payload format |
| `session/MqttDeviceAwareSessionContext.java` | Per-device MQTT session state |
| `session/DeviceSessionCtx.java` | Device session context with Netty channel reference |
| `session/GatewayDeviceSessionContext.java` | Gateway-attached device session |
| `session/GatewaySessionHandler.java` | Handles gateway connect/disconnect and child device lifecycle |
| `session/SparkplugDeviceSessionContext.java` | Sparkplug B-aware device session |
| `session/SparkplugNodeSessionHandler.java` | Sparkplug B edge node session |
| `gateway/GatewayMetricsService.java` | Tracks gateway session counts and metrics |
| `limits/IpFilter.java` / `SessionLimits.java` | Connection/IP-based rate limiting |
| `util/MqttTopicFilter.java` | Topic filtering for subscriptions (exact, regex, all) |
| `util/sparkplug/SparkplugTopicService.java` | Sparkplug B topic parsing (v2.2 and v3.0) |
| `util/sparkplug/SparkplugMetricUtil.java` | Sparkplug B metric conversion utilities |

**Session management:** Sessions are tracked by client ID in a concurrent map. On CONNECT, the transport authenticates via `TransportService`, creates a `DeviceSessionCtx`, and registers subscriptions. On DISCONNECT or channel close, the session is deregistered.

**Gateway mode:** Gateway devices subscribe to `v1/gateway/connect` and `v1/gateway/disconnect` topics and publish device data to `v1/gateway/telemetry`, `v1/gateway/attributes`, etc. The `GatewaySessionHandler` manages child device session lifecycle.

**Sparkplug B support:** Native Sparkplug B v2.2/v3.0 topic structure parsing. `SparkplugNodeSessionHandler` manages NBIRTH/DBIRTH/NDEATH messages. Metric values are converted from Sparkplug data types to ThingsBoard telemetry.

### HTTP Transport

**Protocol:** HTTP/HTTPS (REST API)  
**Default port:** 8081 (plain), 8443 (TLS)  
**Spring MVC-based:** Uses Spring Boot embedded Tomcat  
**Module entry point:** `transport/http/` -> `ThingsboardHttpTransportApplication.java`

| File | Purpose |
|------|---------|
| `DeviceApiController.java` | REST controller: `/api/v1/{accessToken}/telemetry`, `/attributes`, `/rpc`, etc. |
| `HttpTransportContext.java` | HTTP transport configuration and shared state |
| `config/TransportSecurityConfiguration.java` | Spring Security config for device API endpoints |
| `config/PayloadSizeFilter.java` | Request payload size limiting filter |

**Endpoints:**
- `POST /api/v1/{accessToken}/telemetry` -- Post telemetry data (JSON or Protobuf)
- `POST /api/v1/{accessToken}/attributes` -- Post attribute updates
- `GET /api/v1/{accessToken}/attributes` -- Get client/server/shared attributes
- `POST /api/v1/{accessToken}/rpc` -- Send RPC response
- `GET /api/v1/{accessToken}/rpc` -- Long-poll for RPC requests (optional timeout)
- `POST /api/v1/{accessToken}/claim` -- Claim device
- `POST /api/v1/provision` -- Device provisioning
- `GET /api/v1/{accessToken}/firmware` -- OTA firmware download

**Session management:** HTTP is stateless; sessions are created per-request or via long-polling for RPC. The `TransportService.registerSyncSession()` is used for RPC long-poll with a configurable timeout.

### CoAP Transport

**Protocol:** CoAP (Constrained Application Protocol) over UDP, RFC 7252  
**Default port:** 5683 (plain), 5684 (DTLS)  
**Library:** Eclipse Californium  
**Module entry point:** `transport/coap/` -> `ThingsboardCoapTransportApplication.java`

| File | Purpose |
|------|---------|
| `CoapTransportResource.java` | Main CoAP resource handling telemetry, attributes, RPC |
| `AbstractCoapTransportResource.java` | Base class for CoAP resources with shared request handling |
| `CoapTransportService.java` | Transport service with CoAP-specific session management |
| `CoapTransportContext.java` | CoAP transport configuration |
| `OtaPackageTransportResource.java` | CoAP resource for OTA firmware delivery |
| `adaptors/JsonCoapAdaptor.java` | JSON payload conversion for CoAP |
| `adaptors/ProtoCoapAdaptor.java` | Protobuf payload conversion |
| `adaptors/CoapTransportAdaptor.java` | Shared adaptor interface |
| `callback/CoapDeviceAuthCallback.java` | Async authentication callback |
| `callback/CoapResponseCallback.java` | Async response callback |
| `callback/ToServerRpcSyncSessionCallback.java` | Sync RPC session callback |
| `callback/GetAttributesSyncSessionCallback.java` | Sync attribute request callback |
| `callback/CoapNoOpCallback.java` | No-op callback placeholder |
| `client/CoapClientContext.java` | Client-side context for CoAP observe and outbound requests |
| `client/NoSecClient.java` | Unsecured CoAP client |
| `client/SecureClientNoAuth.java` | DTLS client without cert auth |
| `client/SecureClientX509.java` | DTLS client with X.509 certificate |
| `client/TbCoapClientState.java` | Tracks per-client state |
| `client/TbCoapObservationState.java` | Tracks CoAP observe relationships |
| `efento/CoapEfentoTransportResource.java` | Efento sensor CoAP resource |
| `efento/adaptor/EfentoCoapAdaptor.java` | Efento proprietary protocol adaptor |

**Session management:** CoAP sessions are tracked by client address and token. Sync sessions support blocking requests (get attributes, RPC responses) via `registerSyncSession()` with a timeout. Observe relationships are managed through `TbCoapObservationState`.

**Efento support:** Native support for Efento wireless sensors using their proprietary CoAP protocol. The `CoapEfentoTransportResource` handles Efento-specific resource paths and the `EfentoCoapAdaptor` decodes the custom binary payload format.

### LwM2M Transport

**Protocol:** Lightweight M2M (LwM2M) 1.0 / 1.1 over CoAP  
**Default port:** 5685 (plain), 5686 (DTLS)  
**Library:** Eclipse Leshan  
**Module entry point:** `transport/lwm2m/` -> `ThingsboardLwm2mTransportApplication.java`

| File | Purpose |
|------|---------|
| `server/DefaultLwM2mTransportService.java` | Main transport service integrating Leshan server |
| `server/AbstractLwM2mTransportResource.java` | Base LwM2M resource handler |
| `server/adaptors/LwM2MTransportAdaptor.java` | Converts LwM2M objects/resources to telemetry |
| `server/adaptors/LwM2MJsonAdaptor.java` | JSON payload conversion |
| `server/client/LwM2mClient.java` | LwM2M client representation |
| `server/client/LwM2mClientContext.java` | Client context with registration, observation state |
| `server/client/LwM2mClientContextImpl.java` | Client context implementation |
| `server/client/ModelObject.java` | LwM2M object model definition |
| `server/client/ResourceValue.java` / `ResourceUpdateResult.java` | Resource data tracking |
| `server/attributes/DefaultLwM2MAttributesService.java` | Attribute service for LwM2M |
| `secure/LwM2mCredentialsSecurityInfoValidator.java` | PSK, RPK, X.509 credential validation |
| `secure/TbLwM2MAuthorizer.java` | LwM2M authorization provider for Leshan |
| `secure/TbLwM2MDtlsCertificateVerifier.java` | DTLS certificate verification |
| `secure/TbX509DtlsSessionInfo.java` | X.509 DTLS session metadata |
| `secure/credentials/LwM2MClientCredentials.java` | Client credential model |
| `bootstrap/LwM2MTransportBootstrapService.java` | Bootstrap server implementation |
| `bootstrap/store/LwM2MInMemoryBootstrapConfigStore.java` | In-memory bootstrap configuration store |
| `bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java` | Bootstrap session management |
| `config/TbLwM2mVersion.java` | LwM2M version enum (1.0, 1.1) |
| `config/LwM2MSecureServerConfig.java` | Secure server configuration |
| `config/LwM2MTransportServerConfig.java` | Transport server configuration |

**Session management:** LwM2M sessions map to Leshan client registrations. When a device registers, the transport creates a `LwM2mClientContext`, authenticates the device, and maps LwM2M object instances to ThingsBoard telemetry keys. Observed resources automatically push updates as telemetry.

**Bootstrap server:** The LwM2M transport includes a bootstrap server (`LwM2MTransportBootstrapService`) for provisioning device security and server configurations. Bootstrap configurations are stored in memory and managed per device profile.

**Security:** Supports PSK (Pre-Shared Key), RPK (Raw Public Key), and X.509 certificate-based authentication. The `TbLwM2MAuthorizer` validates credentials against the device credentials stored in ThingsBoard.

### SNMP Transport

**Protocol:** SNMP v1, v2c, v3  
**Default port:** 161 (standard SNMP)  
**Library:** SNMP4J  
**Module entry point:** `transport/snmp/` -> `ThingsboardSnmpTransportApplication.java`

| File | Purpose |
|------|---------|
| `SnmpTransportContext.java` | SNMP transport configuration |
| `SnmpTransportService.java` | Main SNMP transport service |
| `service/SnmpAuthService.java` | SNMP credential validation (community strings, USM) |
| `service/PduService.java` | SNMP PDU handling (GET, GETNEXT, GETBULK, SET) |
| `service/ProtoTransportEntityService.java` | Entity data retrieval for SNMP configuration |
| `service/SnmpTransportBalancingService.java` | Load balancing across multiple SNMP transport instances |
| `session/DeviceSessionContext.java` | Per-device SNMP session |
| `session/ScheduledTask.java` | Scheduled SNMP polling tasks |
| `event/SnmpTransportListChangedEvent.java` | Event for transport list changes |
| `event/SnmpTransportListChangedEventListener.java` | Listener for transport list changes |

**Session management:** Unlike other transports, SNMP is primarily a polling protocol. The SNMP transport manages scheduled tasks that periodically poll SNMP-enabled devices. Sessions are created per poll cycle. The transport also handles incoming SNMP traps (v1/v2c/v3) as telemetry events.

**Load balancing:** The `SnmpTransportBalancingService` distributes SNMP device polling across multiple transport instances in a cluster, ensuring each device is polled by exactly one transport node.

## Transport Configuration

Each transport reads its configuration from the Spring environment (typically `thingsboard.yml` or environment variables). Common configuration properties include:

- `transport.<protocol>.bind_address` -- Network interface to bind to
- `transport.<protocol>.bind_port` -- Port to listen on
- `transport.<protocol>.timeout` -- Connection/session timeout in milliseconds
- `transport.<protocol>.ssl.enabled` -- Whether TLS/DTLS is enabled
- `transport.<protocol>.ssl.credentials` -- SSL certificate configuration
- `transport.<protocol>.rate_limits` -- Message rate limiting configuration

## Relationships

- [Rule Engine](../30-rule-engine/README.md) -- Transports send data to the rule engine via the queue
- [Queue System](../10-common/queue.md) -- Kafka-based message bus between transports and core
- [Supporting Modules](../60-supporting/README.md) -- Netty MQTT codec, EDQS, monitoring tools
- [Core Application](../50-application/README.md) -- Consumes transport messages, hosts rule engine actors
