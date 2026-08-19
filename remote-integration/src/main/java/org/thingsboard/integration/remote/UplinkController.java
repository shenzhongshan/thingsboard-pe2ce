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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Local ingress of the remote integration: devices on the customer network POST their payloads
 * here and the process relays them to the platform over the gRPC stream. This is what makes the
 * process useful even though the platform itself is unreachable from the devices.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UplinkController {

    private final RemoteIntegrationClient client;
    private final RemoteIntegrationRuntimeManager runtimeManager;

    @PostMapping(value = "/uplink", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Map<String, Object>> uplink(@RequestBody byte[] payload) {
        if (!client.isConnected()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Not connected to the platform"));
        }
        try {
            client.sendUplink(payload);
            return ResponseEntity.ok(Map.of("status", "accepted", "bytes", payload.length));
        } catch (Exception e) {
            log.warn("Failed to forward uplink", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        var cfg = client.getConfiguration();
        return Map.of(
                "connected", client.isConnected(),
                "integration", cfg != null ? cfg.getName() : "",
                "type", cfg != null ? cfg.getType() : "",
                "enabled", cfg != null && cfg.getEnabled(),
                "uplinkConverter", cfg != null ? cfg.getUplinkConverter().getName() : "",
                // state of the protocol stack running in THIS process ("NO_RUNTIME" for the
                // webhook-style types, which have no stack and only use /uplink above)
                "runtime", runtimeManager.getRuntimeStatus());
    }

}
