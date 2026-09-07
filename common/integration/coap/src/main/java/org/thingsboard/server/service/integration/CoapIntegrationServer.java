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
import org.eclipse.californium.core.server.resources.Resource;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CoAP integration runtime, with two modes.
 * <p>
 * <b>Shared-server mode (PE parity, preferred).</b> Configuration
 * {@code clientConfiguration: {securityMode, coapEndpoint, dtlsCoapEndpoint?}} — the integration
 * publishes itself as a resource on the platform's own CoAP server at the path taken from
 * {@code coapEndpoint}, with the routing key as the leaf node, exactly like PE's CoapIntegration.
 * Devices then post to {@code coap://host:5683/api/v1/integrations/coap/{routingKey}} and no extra
 * port is opened.
 * <p>
 * <b>Legacy standalone mode.</b> Configuration {@code {port, resourceName?}} — the original CE
 * behaviour, one dedicated plain-CoAP server per integration on its own port. Kept because
 * integrations created before the shared-server mode existed are persisted in this shape, and
 * because the shared CoAP server bean only exists in `monolith`/`tb-transport` processes
 * (see TbCoapServerComponent), so this is also the fallback when the platform has no CoAP server.
 */
@Slf4j
public class CoapIntegrationServer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    /** Null when the platform has no CoAP server in this process — forces legacy mode. */
    private final CoapServerService coapServerService;
    @Getter
    private final TenantId tenantId;

    /** Legacy mode only. */
    private volatile CoapServer standaloneServer;
    /** Shared-server mode only; detached on stop so the resource disappears with the integration. */
    private volatile CoapResource sharedResource;
    private volatile CoapResource sharedDtlsResource;

    public CoapIntegrationServer(Integration integration, IntegrationUplinkSink uplinkSink,
                                 CoapServerService coapServerService) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.coapServerService = coapServerService;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = integration.getConfiguration();
        JsonNode client = IntegrationConfigUtil.clientConfig(cfg);
        String coapEndpoint = text(client, "coapEndpoint");
        String dtlsCoapEndpoint = text(client, "dtlsCoapEndpoint");

        if (coapEndpoint.isEmpty() && dtlsCoapEndpoint.isEmpty()) {
            startStandalone(cfg);
            return;
        }
        if (coapServerService == null) {
            throw new IllegalStateException(
                    "CoAP integration endpoints require the platform CoAP server, which is not running in this process!");
        }
        String securityMode = text(client, "securityMode");
        if (securityMode.isEmpty()) {
            securityMode = "NO_SECURE";
        }
        boolean wantsNoSec = "NO_SECURE".equals(securityMode) || "MIXED".equals(securityMode);
        boolean wantsDtls = "DTLS".equals(securityMode) || "MIXED".equals(securityMode);
        if (wantsDtls && !coapServerService.isDtlsEnabled()) {
            throw new IllegalStateException("CoAP server doesn't have DTLS endpoint enabled!");
        }
        if (wantsNoSec) {
            sharedResource = publish(coapEndpoint);
        }
        if (wantsDtls) {
            String dtlsPath = resourcePath(dtlsCoapEndpoint);
            // MIXED with both endpoints on the same path would otherwise add the leaf twice.
            if (!(wantsNoSec && dtlsPath.equals(resourcePath(coapEndpoint)))) {
                sharedDtlsResource = publish(dtlsCoapEndpoint);
            }
        }
        log.info("[{}][{}] CoAP integration published on the shared server, security mode [{}]",
                tenantId, integration.getName(), securityMode);
    }

    /**
     * Attaches a leaf resource named after the routing key under the endpoint's path, creating the
     * intermediate nodes as needed — PE's addIntegrationResource.
     */
    private CoapResource publish(String endpoint) throws Exception {
        List<String> hierarchy = resourceHierarchy(resourcePath(endpoint));
        Resource parent = hierarchy.isEmpty()
                ? coapServerService.getCoapServer().getRoot()
                : coapServerService.addResourceHierarchicallyAndReturnLast(hierarchy);
        CoapResource leaf = new CoapResource(integration.getRoutingKey()) {
            @Override
            public Resource getChild(String name) {
                // PE does the same: everything below the routing key is still this integration, so a
                // device may append arbitrary path segments without 4.04-ing.
                return this;
            }

            @Override
            public void handlePOST(CoapExchange exchange) {
                handleUplink(exchange);
            }

            @Override
            public void handlePUT(CoapExchange exchange) {
                handleUplink(exchange);
            }
        };
        parent.add(leaf);
        return leaf;
    }

    /** PE's getEndpointUri: the URI path with the routing key segment removed. */
    private String resourcePath(String endpoint) {
        String path = URI.create(endpoint).getPath();
        if (path == null) {
            return "";
        }
        String routingKey = integration.getRoutingKey();
        return routingKey != null ? path.replace(routingKey, "") : path;
    }

    private static List<String> resourceHierarchy(String path) {
        List<String> parts = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                parts.add(segment);
            }
        }
        return parts;
    }

    private void startStandalone(JsonNode cfg) {
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
                handleUplink(exchange);
            }
        });
        coapServer.start();
        this.standaloneServer = coapServer;
        log.info("[{}][{}] CoAP integration listening on port {}, resource [{}]",
                tenantId, integration.getName(), port, resourceName);
    }

    private void handleUplink(CoapExchange exchange) {
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

    private static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText("") : "";
    }

    @Override
    public void stop() {
        CoapServer s = this.standaloneServer;
        if (s != null) {
            s.destroy();
            this.standaloneServer = null;
        }
        // delete() detaches the leaf from the shared server; without it a stopped/deleted
        // integration would keep answering on its old path until the process restarts.
        if (sharedResource != null) {
            sharedResource.delete();
            sharedResource = null;
        }
        if (sharedDtlsResource != null) {
            sharedDtlsResource.delete();
            sharedDtlsResource = null;
        }
    }

}
