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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Inbound endpoint for HTTP-webhook-family integrations. The routing key acts as the shared secret —
 * same trust model as PE. Served under the device API entry point, which is not JWT-protected.
 * <p>
 * PE gives every webhook type its own URL segment (one controller each: HttpIntegrationController,
 * SigFoxIntegrationController, ...), and the segment is baked into the endpoint URL the UI shows the
 * user. Those URLs get configured into third-party network servers, so the segments are part of the
 * contract, not cosmetic — hence the explicit path list below rather than a single {@code /http/}
 * catch-all. The payload shapes differ per type, which IntegrationPayloadUnwrapperFactory normalizes
 * before the uplink converter runs.
 */
@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationHttpController {

    /**
     * URL segment -> type. Segments are PE's, verified against its controllers' @RequestMapping
     * values; {@code tmobile_iot_cdp} and {@code tpe} in particular do not match their enum names.
     */
    private static final Map<String, IntegrationType> TYPE_BY_PATH = Map.ofEntries(
            Map.entry("http", IntegrationType.HTTP),
            Map.entry("sigfox", IntegrationType.SIGFOX),
            Map.entry("chirpstack", IntegrationType.CHIRPSTACK),
            Map.entry("particle", IntegrationType.PARTICLE),
            Map.entry("kpn", IntegrationType.KPN),
            Map.entry("loriot", IntegrationType.LORIOT),
            Map.entry("oceanconnect", IntegrationType.OCEANCONNECT),
            Map.entry("thingpark", IntegrationType.THINGPARK),
            Map.entry("tpe", IntegrationType.TPE),
            Map.entry("tmobile_iot_cdp", IntegrationType.TMOBILE_IOT_CDP));

    private final IntegrationService integrationService;
    private final PlatformIntegrationService platformIntegrationService;

    @RequestMapping(
            value = {
                    "/http/{routingKey}", "/http/{routingKey}/{suffix}",
                    "/sigfox/{routingKey}", "/sigfox/{routingKey}/{suffix}",
                    "/chirpstack/{routingKey}", "/chirpstack/{routingKey}/{suffix}",
                    "/particle/{routingKey}", "/particle/{routingKey}/{suffix}",
                    "/kpn/{routingKey}", "/kpn/{routingKey}/{suffix}",
                    "/loriot/{routingKey}", "/loriot/{routingKey}/{suffix}",
                    "/oceanconnect/{routingKey}", "/oceanconnect/{routingKey}/{suffix}",
                    "/thingpark/{routingKey}", "/thingpark/{routingKey}/{suffix}",
                    "/tpe/{routingKey}", "/tpe/{routingKey}/{suffix}",
                    "/tmobile_iot_cdp/{routingKey}", "/tmobile_iot_cdp/{routingKey}/{suffix}"
            },
            method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<String> processUplink(@PathVariable("routingKey") String routingKey,
                                                @PathVariable(value = "suffix", required = false) String suffix,
                                                @RequestBody(required = false) String body,
                                                @RequestHeader Map<String, String> headers,
                                                HttpServletRequest request) {
        IntegrationType expectedType = TYPE_BY_PATH.get(pathSegment(request));
        Optional<Integration> integrationOpt = integrationService.findIntegrationByRoutingKey(routingKey);
        // The type check is what stops an MQTT integration's routing key from being replayed against
        // a webhook URL, and a SigFox key from being posted to the /loriot/ path.
        if (expectedType == null || integrationOpt.isEmpty() || integrationOpt.get().getType() != expectedType) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Integration not found");
        }
        Integration integration = integrationOpt.get();
        if (!integration.isEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Integration is disabled");
        }
        if (!checkSecurity(integration, headers)) {
            // PE returns a bare 403 here with no body — do not leak which header failed.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            Map<String, String> metadata = new HashMap<>();
            headers.forEach((k, v) -> metadata.put("header:" + k, v));
            if (suffix != null) {
                metadata.put("suffix", suffix);
            }
            byte[] payload = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
            platformIntegrationService.processUplink(integration, payload, metadata);
            // PE's replaceNoContentToOk: some network servers treat 204 as a delivery failure and
            // retry forever, so the integration can be told to answer 200 instead.
            return replaceNoContentToOk(integration)
                    ? ResponseEntity.ok("")
                    : ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (TbRateLimitsException e) {
            // distinct from the 500 below on purpose: a device that is merely too fast must be able
            // to back off, which it cannot do if throttling looks like a server fault
            log.debug("[{}] Uplink rejected by rate limits: {}", integration.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        } catch (Exception e) {
            log.warn("[{}] Failed to process HTTP integration uplink: {}", integration.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /** The segment between {@code /api/v1/integrations/} and the routing key. */
    private static String pathSegment(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int base = uri.indexOf("/api/v1/integrations/");
        if (base < 0) {
            return null;
        }
        String rest = uri.substring(base + "/api/v1/integrations/".length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    /**
     * PE's BasicHttpIntegration#checkSecurity: when {@code enableSecurity} is set, every entry of
     * {@code headersFilter} must be present on the request with exactly that value. Header lookup is
     * case-insensitive on the name (PE lowercases the configured key before matching) and exact on
     * the value. An empty filter map with security on therefore passes — same as PE.
     */
    private static boolean checkSecurity(Integration integration, Map<String, String> headers) {
        JsonNode cfg = integration.getConfiguration();
        if (cfg == null || !cfg.path("enableSecurity").asBoolean(false)) {
            return true;
        }
        JsonNode filter = cfg.get("headersFilter");
        if (filter == null || !filter.isObject()) {
            return true;
        }
        Map<String, String> lowerCased = new HashMap<>();
        headers.forEach((k, v) -> lowerCased.put(k.toLowerCase(), v));
        for (Iterator<Map.Entry<String, JsonNode>> it = filter.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> expected = it.next();
            String actual = lowerCased.get(expected.getKey().toLowerCase());
            if (actual == null || !actual.equals(expected.getValue().asText())) {
                return false;
            }
        }
        return true;
    }

    private static boolean replaceNoContentToOk(Integration integration) {
        JsonNode cfg = integration.getConfiguration();
        return cfg != null && cfg.path("replaceNoContentToOk").asBoolean(false);
    }

}
