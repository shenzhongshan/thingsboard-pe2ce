/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.remote;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.DownlinkMsg;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains the bidirectional gRPC stream to the platform: authenticates once with the
 * integration's routing key and secret, then pushes uplinks up and applies whatever the platform
 * sends down (configuration updates, converter updates, downlinks). Reconnects on failure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteIntegrationClient {

    private final ApplicationEventPublisher eventPublisher;

    @Value("${integration.routingKey}")
    private String routingKey;
    @Value("${integration.secret}")
    private String secret;
    @Value("${integration.rpc.host:localhost}")
    private String host;
    @Value("${integration.rpc.port:9090}")
    private int port;
    @Value("${integration.rpc.reconnect_delay_sec:10}")
    private int reconnectDelaySec;
    @Value("${integration.serviceId:remote-integration}")
    private String serviceId;

    private final AtomicInteger uplinkMsgIdSeq = new AtomicInteger();
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "remote-integration-reconnect"));

    /**
     * Identifies the current connection attempt. One failed attempt reports itself through several
     * callbacks - the refusal response, the stream's onError and its onCompleted - and each of them
     * used to schedule its own reconnect, so attempts multiplied every cycle instead of happening
     * once per {@link #reconnectDelaySec}. Every callback carries the epoch it belongs to and only
     * the first one to move the epoch forward gets to schedule; the rest are stale and do nothing.
     */
    private final AtomicInteger connectionEpoch = new AtomicInteger();

    private volatile ManagedChannel channel;
    private volatile StreamObserver<RequestMsg> outputStream;
    private volatile boolean connected;
    private volatile boolean stopped;

    /** Configuration pushed by the platform on connect and whenever the integration is edited. */
    @Getter
    private volatile IntegrationConfigurationProto configuration;

    /**
     * Deliberately not {@code @PostConstruct}: the connect response arrives within milliseconds and
     * carries the configuration that {@link RemoteIntegrationRuntimeManager} needs. Connecting
     * during bean construction would publish that event before the listener bean exists.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        connect();
    }

    @PreDestroy
    public void destroy() {
        stopped = true;
        reconnectExecutor.shutdownNow();
        closeChannel();
    }

    public boolean isConnected() {
        return connected;
    }

    private synchronized void connect() {
        if (stopped) {
            return;
        }
        // Bump first: it retires every callback still in flight from the previous attempt, including
        // the onCompleted that closeChannel() is about to trigger on the old stream.
        int epoch = connectionEpoch.incrementAndGet();
        closeChannel();
        log.info("Connecting to ThingsBoard at {}:{} with routing key [{}]", host, port, routingKey);
        try {
            channel = NettyChannelBuilder.forAddress(host, port).usePlaintext().build();
            IntegrationTransportGrpc.IntegrationTransportStub stub = IntegrationTransportGrpc.newStub(channel);
            CountDownLatch connectLatch = new CountDownLatch(1);
            outputStream = stub.handleMsgs(new StreamObserver<>() {

                @Override
                public void onNext(ResponseMsg responseMsg) {
                    try {
                        onResponse(responseMsg, connectLatch, epoch);
                    } catch (Exception e) {
                        log.error("Failed to process a message from the platform", e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("Stream to the platform failed: {}", t.getMessage());
                    connectLatch.countDown();
                    onDisconnected(epoch);
                }

                @Override
                public void onCompleted() {
                    log.info("Platform closed the stream");
                    connectLatch.countDown();
                    onDisconnected(epoch);
                }
            });

            outputStream.onNext(RequestMsg.newBuilder()
                    .setConnectRequestMsg(ConnectRequestMsg.newBuilder()
                            .setIntegrationRoutingKey(routingKey)
                            .setIntegrationSecret(secret)
                            .setServiceId(serviceId)
                            .build())
                    .build());

            if (!connectLatch.await(20, TimeUnit.SECONDS)) {
                log.warn("Timed out waiting for the platform connect response");
                onDisconnected(epoch);
            }
        } catch (Exception e) {
            log.error("Failed to connect to the platform", e);
            onDisconnected(epoch);
        }
    }

    private void onResponse(ResponseMsg msg, CountDownLatch connectLatch, int epoch) {
        if (msg.hasConnectResponseMsg()) {
            var response = msg.getConnectResponseMsg();
            connectLatch.countDown();
            if (response.getResponseCode() == ConnectResponseCode.ACCEPTED) {
                configuration = response.getConfiguration();
                connected = true;
                log.info("Connected. Integration [{}] type [{}], uplink converter [{}]",
                        configuration.getName(), configuration.getType(),
                        configuration.getUplinkConverter().getName());
                eventPublisher.publishEvent(new IntegrationConfigurationEvent(configuration));
            } else {
                // credentials or the remote flag are wrong - retrying will not help until the
                // platform side is fixed, but keep retrying slowly so a fix is picked up
                log.error("Platform refused the connection: {} - {}",
                        response.getResponseCode(), response.getErrorMsg());
                onDisconnected(epoch);
            }
            return;
        }
        if (msg.hasIntegrationUpdateMsg()) {
            configuration = msg.getIntegrationUpdateMsg().getConfiguration();
            log.info("Integration configuration updated: enabled={}", configuration.getEnabled());
            eventPublisher.publishEvent(new IntegrationConfigurationEvent(configuration));
            return;
        }
        if (msg.hasConverterUpdateMsg()) {
            log.info("Converter updated: {}", msg.getConverterUpdateMsg().getConfiguration().getName());
            return;
        }
        if (msg.hasDownlinkMsg()) {
            onDownlink(msg.getDownlinkMsg());
            return;
        }
        if (msg.hasUplinkResponseMsg()) {
            var response = msg.getUplinkResponseMsg();
            if (!response.getSuccess()) {
                log.warn("Platform rejected an uplink: {}", response.getErrorMsg());
            }
        }
    }

    private void onDownlink(DownlinkMsg downlink) {
        if (!downlink.hasDeviceData()) {
            log.info("Downlink received without device data, nothing to deliver: {}", downlink);
            return;
        }
        var data = downlink.getDeviceData();
        log.info("Downlink for device [{}] type [{}]: {}",
                data.getDeviceName(), data.getDeviceType(), data.getTbMsgProto().getData());
        // If a protocol runtime is up in this process it can actually deliver this (e.g. publish to
        // the MQTT broker); with no runtime the log line above is all we can do.
        eventPublisher.publishEvent(new DownlinkEvent(
                data.getTbMsgProto().getData().getBytes(StandardCharsets.UTF_8),
                Map.of("deviceName", data.getDeviceName(), "deviceType", data.getDeviceType())));
    }

    private void onDisconnected(int epoch) {
        if (connectionEpoch.get() != epoch) {
            return; // a callback from an attempt we have already replaced
        }
        connected = false;
        if (stopped) {
            return;
        }
        if (!connectionEpoch.compareAndSet(epoch, epoch + 1)) {
            return; // another callback for this same attempt already scheduled the reconnect
        }
        log.info("Reconnecting in {}s", reconnectDelaySec);
        reconnectExecutor.schedule(this::connect, reconnectDelaySec, TimeUnit.SECONDS);
    }

    private void closeChannel() {
        connected = false;
        StreamObserver<RequestMsg> stream = outputStream;
        outputStream = null;
        if (stream != null) {
            try {
                stream.onCompleted();
            } catch (Exception ignored) {
                // stream already broken
            }
        }
        ManagedChannel ch = channel;
        channel = null;
        if (ch != null) {
            ch.shutdownNow();
        }
    }

    public void sendUplink(byte[] payload) {
        sendUplink(payload, Map.of());
    }

    /**
     * Sends a raw device payload to the platform. The platform runs the integration's uplink
     * converter on it, exactly as it would for a payload received by an in-process integration.
     *
     * @param metadata protocol-level context the runtime saw (MQTT topic, source address, ...);
     *                 the platform merges it into the metadata the converter script receives
     */
    public void sendUplink(byte[] payload, Map<String, String> metadata) {
        StreamObserver<RequestMsg> stream = outputStream;
        if (!connected || stream == null) {
            throw new IllegalStateException("Not connected to the platform");
        }
        stream.onNext(RequestMsg.newBuilder()
                .setUplinkMsg(UplinkMsg.newBuilder()
                        .setUplinkMsgId(uplinkMsgIdSeq.incrementAndGet())
                        .addTbMsg(ByteString.copyFrom(payload))
                        .putAllPayloadMetadata(metadata)
                        .build())
                .build());
    }

    /** Published whenever the platform hands down a (new) integration configuration. */
    public record IntegrationConfigurationEvent(IntegrationConfigurationProto configuration) {}

    /** Published for each downlink, so a running protocol runtime can deliver it to the device. */
    public record DownlinkEvent(byte[] payload, Map<String, String> metadata) {}

}
