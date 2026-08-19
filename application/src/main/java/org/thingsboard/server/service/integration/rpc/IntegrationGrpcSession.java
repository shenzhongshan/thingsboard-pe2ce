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
package org.thingsboard.server.service.integration.rpc;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.ConverterUpdateMsg;
import org.thingsboard.server.gen.integration.DeviceDownlinkDataProto;
import org.thingsboard.server.gen.integration.DownlinkMsg;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationUpdateMsg;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;
import org.thingsboard.server.common.msg.gen.MsgProtos.TbMsgProto;

import java.io.Closeable;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * One connected remote integration process. The remote side opens the bidirectional
 * {@code handleMsgs} stream, authenticates with the integration's routingKey/secret, then streams
 * uplink payloads up while the platform streams configuration updates and downlinks back.
 */
@Slf4j
public class IntegrationGrpcSession implements Closeable {

    private final RemoteIntegrationCallbacks callbacks;
    private final StreamObserver<ResponseMsg> outputStream;
    @Getter
    private final StreamObserver<RequestMsg> inputStream;

    @Getter
    private volatile Integration configuration;
    @Getter
    private volatile boolean connected;
    @Getter
    private volatile String serviceId;

    public IntegrationGrpcSession(RemoteIntegrationCallbacks callbacks,
                                  StreamObserver<ResponseMsg> outputStream,
                                  Consumer<IntegrationGrpcSession> onConnect,
                                  Consumer<IntegrationGrpcSession> onDisconnect) {
        this.callbacks = callbacks;
        this.outputStream = outputStream;
        this.inputStream = new StreamObserver<>() {

            @Override
            public void onNext(RequestMsg requestMsg) {
                if (requestMsg.hasConnectRequestMsg()) {
                    ConnectResponseMsg response = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder().setConnectResponseMsg(response).build());
                    if (ConnectResponseCode.ACCEPTED != response.getResponseCode()) {
                        outputStream.onCompleted();
                        return;
                    }
                    serviceId = requestMsg.getConnectRequestMsg().getServiceId();
                    connected = true;
                    onConnect.accept(IntegrationGrpcSession.this);
                    return;
                }
                if (!connected) {
                    log.warn("Received a message on a session that has not connected yet, ignoring");
                    return;
                }
                if (requestMsg.hasUplinkMsg()) {
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setUplinkResponseMsg(processUplink(requestMsg.getUplinkMsg())).build());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[{}] Remote integration stream failed", integrationName(), t);
                disconnect(onDisconnect);
            }

            @Override
            public void onCompleted() {
                log.info("[{}] Remote integration disconnected", integrationName());
                disconnect(onDisconnect);
            }
        };
    }

    private void disconnect(Consumer<IntegrationGrpcSession> onDisconnect) {
        connected = false;
        onDisconnect.accept(this);
    }

    private String integrationName() {
        return configuration != null ? configuration.getName() : "unknown";
    }

    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        String routingKey = request.getIntegrationRoutingKey();
        log.info("Remote integration connect request, routing key [{}], serviceId [{}]", routingKey, request.getServiceId());
        Integration integration = callbacks.findByRoutingKey(routingKey);
        if (integration == null) {
            return refuse(ConnectResponseCode.BAD_CREDENTIALS,
                    "Failed to find the integration! Routing key: " + routingKey);
        }
        if (!integration.getSecret().equals(request.getIntegrationSecret())) {
            return refuse(ConnectResponseCode.BAD_CREDENTIALS, "Failed to validate the integration!");
        }
        if (!integration.isRemote()) {
            // guard against a remote process hijacking an integration the platform runs itself
            return refuse(ConnectResponseCode.BAD_CREDENTIALS,
                    "Integration [" + integration.getName() + "] is not configured as remote!");
        }
        try {
            this.configuration = integration;
            return ConnectResponseMsg.newBuilder()
                    .setResponseCode(ConnectResponseCode.ACCEPTED)
                    .setErrorMsg("")
                    .setConfiguration(constructIntegrationConfigProto(integration))
                    .build();
        } catch (Exception e) {
            log.error("[{}] Failed to process integration connection", routingKey, e);
            return refuse(ConnectResponseCode.SERVER_UNAVAILABLE, "Failed to process integration connection!");
        }
    }

    private static ConnectResponseMsg refuse(ConnectResponseCode code, String msg) {
        return ConnectResponseMsg.newBuilder()
                .setResponseCode(code)
                .setErrorMsg(msg)
                .setConfiguration(IntegrationConfigurationProto.getDefaultInstance())
                .build();
    }

    private UplinkResponseMsg processUplink(UplinkMsg msg) {
        try {
            callbacks.processUplink(configuration, msg);
            return UplinkResponseMsg.newBuilder().setSuccess(true).build();
        } catch (Exception e) {
            log.warn("[{}] Failed to process uplink from remote integration", integrationName(), e);
            return UplinkResponseMsg.newBuilder().setSuccess(false)
                    .setErrorMsg(String.valueOf(e.getMessage())).build();
        }
    }

    /** Pushes a changed integration configuration to the remote process without requiring a reconnect. */
    public void onConfigurationUpdate(Integration integration) {
        this.configuration = integration;
        outputStream.onNext(ResponseMsg.newBuilder()
                .setIntegrationUpdateMsg(IntegrationUpdateMsg.newBuilder()
                        .setConfiguration(constructIntegrationConfigProto(integration)).build())
                .build());
    }

    /** Pushes a changed converter (uplink or downlink script) to the remote process. */
    public void onConverterUpdate(Converter converter) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setConverterUpdateMsg(ConverterUpdateMsg.newBuilder()
                        .setConfiguration(constructConverterProto(converter)).build())
                .build());
    }

    /** Forwards a downlink to the remote process as a serialized TbMsg, matching PE's wire shape. */
    public void onDownlink(String deviceName, String deviceType, TbMsgProto tbMsg) {
        outputStream.onNext(ResponseMsg.newBuilder()
                .setDownlinkMsg(DownlinkMsg.newBuilder()
                        .setDeviceData(DeviceDownlinkDataProto.newBuilder()
                                .setDeviceName(deviceName)
                                .setDeviceType(deviceType)
                                .setTbMsgProto(tbMsg)
                                .build())
                        .build())
                .build());
    }

    private IntegrationConfigurationProto constructIntegrationConfigProto(Integration integration) {
        UUID tenantId = integration.getTenantId().getId();
        UUID id = integration.getId().getId();
        IntegrationConfigurationProto.Builder builder = IntegrationConfigurationProto.newBuilder()
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .setIntegrationIdMSB(id.getMostSignificantBits())
                .setIntegrationIdLSB(id.getLeastSignificantBits())
                .setName(integration.getName())
                .setRoutingKey(integration.getRoutingKey())
                .setType(integration.getType().name())
                .setDebugMode(integration.isDebugMode())
                .setEnabled(integration.isEnabled())
                .setConfiguration(JacksonUtil.toString(integration.getConfiguration()));
        Converter uplink = callbacks.findConverter(integration.getTenantId(), integration.getDefaultConverterId());
        if (uplink != null) {
            builder.setUplinkConverter(constructConverterProto(uplink));
        }
        Converter downlink = callbacks.findConverter(integration.getTenantId(), integration.getDownlinkConverterId());
        if (downlink != null) {
            builder.setDownlinkConverter(constructConverterProto(downlink));
        }
        return builder.build();
    }

    private static ConverterConfigurationProto constructConverterProto(Converter converter) {
        UUID tenantId = converter.getTenantId().getId();
        UUID id = converter.getId().getId();
        return ConverterConfigurationProto.newBuilder()
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .setConverterIdMSB(id.getMostSignificantBits())
                .setConverterIdLSB(id.getLeastSignificantBits())
                .setName(converter.getName())
                .setDebugMode(converter.isDebugMode())
                .setConfiguration(JacksonUtil.toString(converter.getConfiguration()))
                .build();
    }

    @Override
    public void close() {
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}] Failed to close the remote integration stream", integrationName(), e);
        }
    }

    /** Platform-side operations a session needs; keeps the session free of Spring wiring. */
    public interface RemoteIntegrationCallbacks {

        Integration findByRoutingKey(String routingKey);

        Converter findConverter(org.thingsboard.server.common.data.id.TenantId tenantId,
                                org.thingsboard.server.common.data.id.ConverterId converterId);

        void processUplink(Integration integration, UplinkMsg msg) throws Exception;

    }

    /** Convenience for tests/registries that want to react to both lifecycle edges with one lambda. */
    public interface SessionLifecycleListener extends BiConsumer<IntegrationGrpcSession, Boolean> {
    }

}
