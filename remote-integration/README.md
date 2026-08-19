# ThingsBoard Remote Integration

Runs one ThingsBoard integration **outside** the platform — typically inside the customer network,
next to equipment the platform cannot reach — and connects back to the platform over gRPC using the
integration's routing key and secret.

```
   devices ──HTTP──▶ [ this process ] ──gRPC──▶ [ ThingsBoard platform ]
   (customer LAN)      :8081 /uplink              :9090
                                       ◀── config / converter / downlink ──
```

## 1. Enable the gRPC endpoint on the platform

Off by default. Start the platform with:

```
INTEGRATIONS_RPC_ENABLED=true      # required
INTEGRATIONS_RPC_PORT=9090         # optional, this is the default
```

## 2. Create a remote integration in ThingsBoard

Create an integration as usual, then set `remote: true` on it. The platform **refuses** a gRPC
connection for an integration that is not flagged remote, so this step is not optional.

Note its `routingKey` and `secret` — they are the credentials this process authenticates with.

## 3. Start this process

```bash
INTEGRATION_ROUTING_KEY=<routingKey> \
INTEGRATION_SECRET=<secret> \
RPC_HOST=<platform host> \
RPC_PORT=9090 \
HTTP_BIND_PORT=8081 \
java -jar remote-integration-4.4.0-SNAPSHOT-boot.jar
```

The runnable artifact is the **`-boot`** one — the plain `remote-integration-4.4.0-SNAPSHOT.jar` next to it is the
thin jar and has no main manifest attribute. Building with `-Dpkg.skip=true` skips the `-boot` jar entirely.

On Windows PowerShell:

```powershell
$env:INTEGRATION_ROUTING_KEY="<routingKey>"
$env:INTEGRATION_SECRET="<secret>"
$env:RPC_HOST="localhost"
$env:HTTP_BIND_PORT="8081"
java -jar remote-integration-4.4.0-SNAPSHOT-boot.jar
```

A successful start logs:

```
Connected. Integration [<name>] type [HTTP], uplink converter [<converter>]
```

## 4. Send an uplink

Devices POST their raw payload to this process; it relays it to the platform, which runs the
integration's **uplink converter** on it exactly as for an in-process integration.

```bash
curl -X POST http://localhost:8081/uplink -H "Content-Type: application/json" \
     -d '{"dev":"sensor-1","t":23.5}'
```

`GET http://localhost:8081/status` reports the connection state, the configuration the platform
pushed down, and the state of the protocol runtime (see below).

## Protocol runtimes

For an integration whose type has a protocol stack — MQTT, OPC-UA, Kafka, RabbitMQ, Pulsar, TCP,
UDP, CoAP, SQS, Kinesis, Event Hubs, Service Bus, Pub/Sub, Tuya — this process runs that stack
itself, right next to the equipment. Nothing has to reach `/uplink`:

```
   devices ──MQTT──▶ [ this process: MqttIntegrationClient ] ──gRPC──▶ platform
```

The runtimes are the very same classes the platform runs, shared through the `common/integration`
module; the only thing this process supplies is `IntegrationUplinkSink`, whose one method puts the
payload on the gRPC stream instead of running the converter locally. Protocol metadata (MQTT topic
and friends) travels with it in `UplinkMsg.payloadMetadata` and reaches the converter script.

`status.runtime` tells you which state the stack is in:

| value | meaning |
|---|---|
| `ACTIVE` | the protocol stack is up |
| `NO_RUNTIME` | this type has no stack (HTTP and the other webhook-style ones) — use `/uplink` |
| `DISABLED` | the integration is disabled on the platform |
| `FAILED: …` | the stack could not start; the message is the cause |
| `STOPPED` / `NONE` | stopped, or no configuration received yet |

Editing the integration on the platform restarts the stack here without restarting the process.

## What this shell does and does not do

**Does**: authenticate, keep the stream alive with automatic reconnect, receive configuration and
converter updates without restarting, run the protocol stacks listed above, relay raw uplinks
through `/uplink`, deliver downlinks through the running stack.

**Does not**: run the converter locally. It only sends raw payloads (`UplinkMsg.tbMsg`) and lets the
platform decode them; PE additionally supports the remote side running the converter and sending
pre-decoded `DeviceUplinkDataProto`, which the platform side explicitly rejects for now. It also
hosts exactly one integration per process — the one its routing key identifies.
