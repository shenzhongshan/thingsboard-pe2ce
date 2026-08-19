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

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.service.integration.IntegrationRuntime;
import org.thingsboard.server.service.integration.IntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.IntegrationUplinkSink;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the integration's protocol stack inside this process.
 * <p>
 * This is the second host of the runtimes in {@code common/integration} - the platform being the
 * first - and the only thing it has to provide them is {@link IntegrationUplinkSink}: where the
 * platform's implementation runs the uplink converter locally, this one puts the payload on the
 * gRPC stream and lets the platform do it.
 * <p>
 * Types with no runtime (HTTP and the other webhook-style ones) simply have no factory; for those
 * the process still works as a relay through {@link UplinkController}.
 */
@Slf4j
@Component
public class RemoteIntegrationRuntimeManager implements IntegrationUplinkSink {

    private final RemoteIntegrationClient client;
    private final Map<IntegrationType, IntegrationRuntimeFactory> factoryByType = new EnumMap<>(IntegrationType.class);

    private volatile IntegrationRuntime runtime;
    /** Last configuration we acted on, so an unrelated update does not bounce a healthy runtime. */
    private volatile String appliedFingerprint;
    @Getter
    private volatile String runtimeStatus = "NONE";

    public RemoteIntegrationRuntimeManager(RemoteIntegrationClient client, List<IntegrationRuntimeFactory> factories) {
        this.client = client;
        for (IntegrationRuntimeFactory factory : factories) {
            for (IntegrationType type : factory.getSupportedTypes()) {
                factoryByType.put(type, factory);
            }
        }
        log.info("Protocol runtimes available in this process: {}", factoryByType.keySet());
    }

    @EventListener
    public synchronized void onConfiguration(RemoteIntegrationClient.IntegrationConfigurationEvent event) {
        IntegrationConfigurationProto proto = event.configuration();
        IntegrationType type;
        try {
            type = IntegrationType.valueOf(proto.getType());
        } catch (IllegalArgumentException e) {
            log.warn("Platform sent an unknown integration type [{}]", proto.getType());
            runtimeStatus = "UNKNOWN_TYPE";
            return;
        }
        IntegrationRuntimeFactory factory = factoryByType.get(type);
        if (factory == null) {
            // Not an error: HTTP-family integrations have no protocol stack to run here.
            runtimeStatus = "NO_RUNTIME";
            return;
        }
        String fingerprint = proto.getEnabled() + "|" + proto.getConfiguration();
        if (runtime != null && fingerprint.equals(appliedFingerprint)) {
            log.debug("Configuration unchanged for the runtime, leaving it running");
            return;
        }
        stopRuntime();
        appliedFingerprint = fingerprint;
        if (!proto.getEnabled()) {
            runtimeStatus = "DISABLED";
            log.info("Integration [{}] is disabled, runtime not started", proto.getName());
            return;
        }
        try {
            IntegrationRuntime started = factory.create(toIntegration(proto, type), this);
            started.start();
            runtime = started;
            runtimeStatus = "ACTIVE";
            log.info("Started the {} runtime for integration [{}]", type, proto.getName());
        } catch (Exception e) {
            runtimeStatus = "FAILED: " + e.getMessage();
            log.error("Failed to start the {} runtime for integration [{}]", type, proto.getName(), e);
        }
    }

    /**
     * The runtimes only ever read from the {@link Integration} they are handed, so rebuilding one
     * from the wire configuration is enough - no DAO, no platform services.
     */
    private static Integration toIntegration(IntegrationConfigurationProto proto, IntegrationType type) {
        Integration integration = new Integration(new IntegrationId(
                new UUID(proto.getIntegrationIdMSB(), proto.getIntegrationIdLSB())));
        integration.setTenantId(TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())));
        integration.setName(proto.getName());
        integration.setType(type);
        integration.setRoutingKey(proto.getRoutingKey());
        integration.setEnabled(proto.getEnabled());
        integration.setDebugMode(proto.getDebugMode());
        integration.setRemote(true);
        integration.setConfiguration(JacksonUtil.toJsonNode(proto.getConfiguration()));
        return integration;
    }

    /**
     * The runtime's only way out. Note this does not buffer: if the gRPC stream is down the payload
     * is refused here and the runtime logs it. Keeping the broker connection up through a platform
     * outage and dropping those payloads beats tearing the protocol stack down and back up.
     */
    @Override
    public void processUplink(Integration integration, byte[] payload, Map<String, String> metadata) {
        client.sendUplink(payload, metadata == null ? Map.of() : metadata);
    }

    /** Lets the platform's downlink reach the protocol stack (e.g. publish to the MQTT broker). */
    @EventListener
    public void onDownlink(RemoteIntegrationClient.DownlinkEvent event) {
        IntegrationRuntime current = runtime;
        if (current == null) {
            log.debug("Downlink received but no protocol runtime is running in this process");
            return;
        }
        try {
            current.pushDownlink(event.payload(), event.metadata());
        } catch (Exception e) {
            log.warn("Failed to deliver a downlink through the {} runtime: {}", runtimeStatus, e.getMessage());
        }
    }

    @PreDestroy
    public synchronized void stop() {
        stopRuntime();
    }

    private void stopRuntime() {
        IntegrationRuntime current = runtime;
        runtime = null;
        runtimeStatus = "STOPPED";
        if (current != null) {
            try {
                current.stop();
            } catch (Exception e) {
                log.warn("Failed to stop the previous runtime cleanly", e);
            }
        }
    }

}
