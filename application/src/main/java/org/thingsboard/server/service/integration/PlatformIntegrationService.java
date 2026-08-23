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
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.Map;

/**
 * Runtime lifecycle manager for integrations. CE port: in-process only (no remote integrations,
 * no integration executor microservice); change notifications are direct in-JVM calls (monolith),
 * same pattern as the scheduler port.
 */
public interface PlatformIntegrationService extends IntegrationUplinkSink {

    void onIntegrationUpdated(Integration integration);

    void onIntegrationDeleted(Integration integration);

    JsonNode getIntegrationStatus(IntegrationId integrationId);

    @Override
    void processUplink(Integration integration, byte[] payload, Map<String, String> metadata) throws Exception;

    /**
     * Pushes a downlink payload to the integration's external endpoint. CE simplification vs PE:
     * only MQTT integrations (publishes to the configured downlinkTopic) support downlink in this
     * build; other integration types throw UnsupportedOperationException.
     */
    /**
     * Runs the integration's downlink converter (if one is configured) over the message and hands
     * the encoded payload to the runtime. Without a converter the message data is sent as-is.
     */
    void pushDownlink(Integration integration, JsonNode msg, String msgType, Map<String, String> metadata) throws Exception;

}
