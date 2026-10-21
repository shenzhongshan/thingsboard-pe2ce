# Common Edge API (`common/edge-api`)

**Maven artifact:** `org.thingsboard.common:edge-api`

The `common/edge-api` module provides the gRPC-based communication protocol for ThingsBoard Edge instances. Edge is a lightweight ThingsBoard distribution that runs at remote locations (factories, stores, vehicles) and synchronizes data with a central ThingsBoard server.

## Architecture

Edge instances communicate with the central ThingsBoard platform via a bidirectional gRPC streaming connection. The Edge acts as a gRPC client, connecting to the server's `EdgeRpcService`. Messages flow in both directions:

- **Uplink** (Edge -> Cloud) -- Telemetry, attributes, events, device connectivity
- **Downlink** (Cloud -> Edge) -- Entity configurations, rule chains, dashboards, device profiles

### Proto Definition

The gRPC service and messages are defined in `edge.proto` (located at `common/edge-api/src/main/proto/edge.proto`):

```protobuf
service EdgeRpcService {
    rpc handleMsgs(stream RequestMsg) returns (stream ResponseMsg) {}
}
```

Generated Java classes: `org.thingsboard.server.gen.edge.v1.EdgeProtos`

### EdgeVersion

The `EdgeVersion` enum tracks protocol compatibility across versions:

**This fork replaced the wire protocol wholesale with PE 4.3.1.1's**, so the values differ from
upstream CE and the numbering is mixed:

| Enum value | proto number |
|---------|-------------------|
| `V_3_3_0` | 0 |
| `V_3_3_3` | 1 |
| `V_3_4_0` | 2 |
| `V_3_6_0` | 3 |
| `V_3_6_1` | 4 |
| `V_3_6_2` | 5 |
| `V_3_6_4` | 6 |
| `V_3_7_0` | 7 |
| `V_3_8_0` | 8 |
| `V_3_9_0` | 9 |
| `V_4_0_0` | 10 |
| `V_4_1_0` | 11 |
| `V_4_2_0` | 12 |
| `V_4_3_0` | 13 |
| `V_4_2_1_2` | 14 |
| `V_4_2_2` | 4220 |
| `V_4_2_2_1` | 4221 |
| `V_4_3_0_1` | 15 |
| `V_4_3_1` | 4310 |
| `V_4_3_1_1` | 4311 |
| `V_LATEST` | 99999 |

**The proto numbers can no longer be compared as versions** — `V_4_2_1_2(14)` sorts after
`V_4_3_0(13)`. `EdgeVersionComparator` therefore **parses the version out of the enum name** and
compares segment by segment; do not compare the proto numbers directly. `V_LATEST` (= 99999) is
special-cased by that comparator to mean "the newest version".

### Message Types

#### RequestMsg (Edge -> Cloud)

| MsgType | Purpose |
|---------|---------|
| `CONNECT_RPC_MESSAGE` | Initial connection handshake with Edge metadata |
| `UPLINK_RPC_MESSAGE` | Device telemetry, attributes, events pushed to cloud |
| `DOWNLINK_RPC_RESPONSE_MESSAGE` | Edge acknowledges cloud downlink messages |
| `SYNC_REQUEST_RPC_MESSAGE` | Edge requests entity sync from cloud |

#### ResponseMsg (Cloud -> Edge)

| Type | Purpose |
|------|---------|
| `ConnectResponseMsg` | Connection accepted/denied with sync configuration |
| `UplinkResponseMsg` | Acknowledgment of uplink messages |
| `DownlinkMsg` | Entity configurations pushed to Edge (rule chains, dashboards, devices, assets, etc.) |
| `EdgeUpdateMsg` | Edge configuration/software updates |

### Sync Protocol

After connecting, the Edge sends a `SyncRequestMsg`. The cloud responds with a stream of `DownlinkMsg` items that represent the full state an Edge needs:

1. **Tenant** configuration
2. **Device profiles** assigned to the Edge
3. **Asset profiles**
4. **Rule chains** (edge-specific and root)
5. **Dashboards**
6. **Device/Asset** definitions
7. **Relations** between entities
8. **Widgets bundles**
9. **Notification rules**

Each downlink message includes:
- `downlinkMsgId` (int64) -- Monotonically increasing ID for ordering
- `syncCompleted` (bool) -- Whether this is the last sync message
- `entityType` + `entityDataProto` -- Entity type and serialized entity data
- `entityIdProto` + `customerIdProto` -- Ownership information

## Java Side

### Core Classes

| Class | Purpose |
|-------|---------|
| `EdgeRpcClient` | Interface for the Edge gRPC client |
| `EdgeGrpcClient` | gRPC implementation of EdgeRpcClient using the protobuf service stub |
| `EdgeVersionComparator` | Compares Edge version strings for compatibility checks |
| `EdgeConnectionException` | Exception for Edge connectivity issues |

### EdgeRpcClient Interface

```java
public interface EdgeRpcClient {
    CompletableFuture<Void> connect(...);
    void disconnect(String reason);
    void sendSyncRequest(SyncRequestMsg request);
    void sendUplinkMsg(UplinkMsg msg);
    CompletableFuture<Void> onDownlinkResponse(DownlinkResponseMsg response);
}
```

### EdgeGrpcClient

The gRPC implementation manages:
- Channel lifecycle (connecting, reconnecting, disconnecting)
- Bidirectional streaming via `handleMsgs`
- Response correlation (mapping responses to requests)
- Reconnection with exponential backoff
- Stream observer management

## Relationships

- **Depends on:** `common/data` (entity types, IDs), `common/proto` (transport protobuf via import), gRPC/protobuf libraries
- **Depended on by:** Edge microservice, `application` (server-side Edge handler)
- **Maven groupId:** `org.thingsboard.common`
- **Maven artifactId:** `edge-api`
