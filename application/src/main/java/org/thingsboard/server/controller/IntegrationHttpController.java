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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Inbound endpoint for HTTP-webhook-family integrations (PE path convention). The routing key
 * acts as the shared secret — same trust model as PE. Served under the device API entry point,
 * which is not JWT-protected. Covers plain HTTP plus every LPWAN network-server webhook type
 * (they're all "POST a JSON body to a routing-key URL", differing only in payload shape, which
 * IntegrationPayloadUnwrapperFactory normalizes before the uplink converter runs).
 */
@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api/v1/integrations/http")
@RequiredArgsConstructor
public class IntegrationHttpController {

    private static final Set<IntegrationType> SUPPORTED_TYPES = Set.of(
            IntegrationType.HTTP, IntegrationType.SIGFOX, IntegrationType.CHIRPSTACK,
            IntegrationType.PARTICLE, IntegrationType.KPN, IntegrationType.LORIOT,
            IntegrationType.OCEANCONNECT, IntegrationType.THINGPARK, IntegrationType.TPE,
            IntegrationType.TMOBILE_IOT_CDP);

    private final IntegrationService integrationService;
    private final PlatformIntegrationService platformIntegrationService;

    @PostMapping("/{routingKey}")
    public ResponseEntity<String> processUplink(@PathVariable("routingKey") String routingKey,
                                                @RequestBody String body,
                                                @RequestHeader Map<String, String> headers) {
        Optional<Integration> integrationOpt = integrationService.findIntegrationByRoutingKey(routingKey);
        if (integrationOpt.isEmpty() || !SUPPORTED_TYPES.contains(integrationOpt.get().getType())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Integration not found");
        }
        Integration integration = integrationOpt.get();
        if (!integration.isEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Integration is disabled");
        }
        try {
            Map<String, String> metadata = new HashMap<>();
            headers.forEach((k, v) -> metadata.put("header:" + k, v));
            platformIntegrationService.processUplink(integration, body.getBytes(StandardCharsets.UTF_8), metadata);
            return ResponseEntity.ok("");
        } catch (Exception e) {
            log.warn("[{}] Failed to process HTTP integration uplink: {}", integration.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}
