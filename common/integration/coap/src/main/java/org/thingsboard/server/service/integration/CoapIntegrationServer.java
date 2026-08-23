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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.HashMap;
import java.util.Map;

/**
 * In-process CoAP server for a COAP integration: one dedicated (non-DTLS) Californium server per
 * integration, listening on the configured port; POSTing to the configured resource path pushes
 * an uplink message. Configuration JSON: {port, resourceName?}.
 * CE simplification vs PE: PE registers integration resources on a single server-wide shared CoAP
 * server (same one used by device transport) and supports DTLS/PSK security modes; this port uses
 * one plain-CoAP server per integration instead (mirrors this port's TCP/UDP integrations), and
 * does not implement DTLS.
 */
@Slf4j
public class CoapIntegrationServer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile CoapServer server;

    public CoapIntegrationServer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() {
        JsonNode cfg = integration.getConfiguration();
        if (cfg == null || !cfg.has("port")) {
            throw new IllegalArgumentException("CoAP port is not configured!");
        }
        int port = cfg.get("port").asInt();
        String resourceName = cfg.has("resourceName") && !cfg.get("resourceName").asText().isEmpty()
                ? cfg.get("resourceName").asText() : "telemetry";

        CoapServer coapServer = new CoapServer(port);
        coapServer.add(new CoapResource(resourceName) {
            @Override
            public void handlePOST(CoapExchange exchange) {
                byte[] payload = exchange.getRequestPayload();
                Map<String, String> metadata = new HashMap<>();
                metadata.put("remoteAddress", String.valueOf(exchange.getSourceAddress()));
                try {
                    uplinkSink.processUplink(integration, payload != null ? payload : new byte[0], metadata);
                    exchange.respond(CoAP.ResponseCode.CHANGED);
                } catch (Exception e) {
                    log.warn("[{}][{}] Failed to process CoAP uplink: {}", tenantId, integration.getName(), e.getMessage());
                    exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                }
            }
        });
        coapServer.start();
        this.server = coapServer;
        log.info("[{}][{}] CoAP integration listening on port {}, resource [{}]", tenantId, integration.getName(), port, resourceName);
    }

    @Override
    public void stop() {
        CoapServer s = this.server;
        if (s != null) {
            s.destroy();
            this.server = null;
        }
    }

}
