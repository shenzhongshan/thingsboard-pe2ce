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

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * gRPC endpoint that remote integration processes connect to.
 * <p>
 * A remote integration runs outside the platform (typically inside the customer network, next to
 * equipment the platform cannot reach), authenticates with the integration's routingKey/secret, and
 * from then on streams uplink payloads in while the platform streams configuration updates and
 * downlinks back. Disabled by default; enable with {@code integrations.rpc.enabled=true}.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "integrations.rpc", value = "enabled", havingValue = "true")
public class TbRemoteIntegrationRpcService extends IntegrationTransportGrpc.IntegrationTransportImplBase
        implements IntegrationGrpcSession.RemoteIntegrationCallbacks {

    private final Map<IntegrationId, IntegrationGrpcSession> sessions = new ConcurrentHashMap<>();

    private final IntegrationService integrationService;
    private final ConverterService converterService;

    // @Lazy breaks the cycle: PlatformIntegrationService needs this bean to push config to remotes,
    // while this bean needs it to hand uplinks in. Only used at request time, never at startup.
    @Autowired
    @Lazy
    private PlatformIntegrationService platformIntegrationService;

    @Value("${integrations.rpc.port:9090}")
    private int rpcPort;
    @Value("${integrations.rpc.client_max_keep_alive_time_sec:300}")
    private int clientMaxKeepAliveTimeSec;

    private Server server;

    @PostConstruct
    public void init() {
        server = NettyServerBuilder.forPort(rpcPort)
                .permitKeepAliveTime(clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .addService(this)
                .build();
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start the remote integration RPC server on port " + rpcPort, e);
        }
        log.info("Remote integration RPC server started on port {}", rpcPort);
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> responseObserver) {
        return new IntegrationGrpcSession(this, responseObserver, this::onConnect, this::onDisconnect).getInputStream();
    }

    private void onConnect(IntegrationGrpcSession session) {
        IntegrationId id = session.getConfiguration().getId();
        IntegrationGrpcSession previous = sessions.put(id, session);
        if (previous != null && previous != session) {
            // a reconnect (or a second process claiming the same integration) supersedes the old stream
            previous.close();
        }
        log.info("[{}] Remote integration connected: {}", id, session.getConfiguration().getName());
    }

    private void onDisconnect(IntegrationGrpcSession session) {
        Integration configuration = session.getConfiguration();
        if (configuration != null) {
            sessions.remove(configuration.getId(), session);
        }
    }

    public boolean isConnected(IntegrationId integrationId) {
        IntegrationGrpcSession session = sessions.get(integrationId);
        return session != null && session.isConnected();
    }

    /** Called when an integration is edited, so the remote process picks up the change without restarting. */
    public void onIntegrationUpdated(Integration integration) {
        IntegrationGrpcSession session = sessions.get(integration.getId());
        if (session != null && session.isConnected()) {
            session.onConfigurationUpdate(integration);
        }
    }

    /** Called when a converter is edited, pushing it to every session that references it. */
    public void onConverterUpdated(Converter converter) {
        sessions.values().forEach(session -> {
            Integration cfg = session.getConfiguration();
            if (!session.isConnected() || cfg == null) {
                return;
            }
            if (converter.getId().equals(cfg.getDefaultConverterId()) || converter.getId().equals(cfg.getDownlinkConverterId())) {
                try {
                    session.onConverterUpdate(converter);
                } catch (Exception e) {
                    log.error("[{}] Failed to push converter update", cfg.getId(), e);
                }
            }
        });
    }

    // --- IntegrationGrpcSession.RemoteIntegrationCallbacks ---

    @Override
    public Integration findByRoutingKey(String routingKey) {
        return integrationService.findIntegrationByRoutingKey(routingKey).orElse(null);
    }

    @Override
    public Converter findConverter(TenantId tenantId, ConverterId converterId) {
        return converterId != null ? converterService.findConverterById(tenantId, converterId) : null;
    }

    @Override
    public void processUplink(Integration integration, UplinkMsg msg) throws Exception {
        // The remote process may either send already-decoded entity data, or the raw payload for the
        // platform to run the uplink converter on. Only the latter is wired up here - decoded uplink
        // data would bypass the converter and is handled by the remote runtime work (see report §17).
        // payloadMetadata carries the protocol-level context the remote runtime saw (MQTT topic,
        // source address, ...) - see the field's comment in integration.proto.
        Map<String, String> metadata = msg.getPayloadMetadataMap();
        for (var tbMsg : msg.getTbMsgList()) {
            platformIntegrationService.processUplink(integration, tbMsg.toByteArray(), metadata);
        }
        if (msg.getDeviceDataCount() > 0 || msg.getAssetDataCount() > 0) {
            log.warn("[{}] Remote integration sent pre-decoded entity data, which is not supported yet " +
                    "(deviceData={}, assetData={})", integration.getId(), msg.getDeviceDataCount(), msg.getAssetDataCount());
        }
        log.debug("[{}] Processed remote uplink: {}", integration.getId(), JacksonUtil.toString(msg.getUplinkMsgId()));
    }

}
