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
import org.thingsboard.common.util.JacksonUtil;

import java.util.Base64;
import java.util.Map;

/** Port of PE's ChirpStackConverterUnwrapper. */
class ChirpStackPayloadUnwrapper extends AbstractIntegrationPayloadUnwrapper {

    private static final Map<String, String> KEYS_MAPPING = Map.ofEntries(
            Map.entry("deduplicationId", "/deduplicationId"), Map.entry("time", "/time"),
            Map.entry("tenantId", "/deviceInfo/tenantId"), Map.entry("tenantName", "/deviceInfo/tenantName"),
            Map.entry("applicationId", "/deviceInfo/applicationId"), Map.entry("applicationName", "/deviceInfo/applicationName"),
            Map.entry("deviceProfileName", "/deviceInfo/deviceProfileName"), Map.entry("deviceName", "/deviceInfo/deviceName"),
            Map.entry("eui", "/deviceInfo/devEui"), Map.entry("devAddr", "/devAddr"),
            Map.entry("fCnt", "/fCnt"), Map.entry("fPort", "/fPort"), Map.entry("confirmed", "/confirmed"),
            Map.entry("data", "/data"), Map.entry("decoded", "/object"), Map.entry("rxInfo", "/rxInfo"),
            Map.entry("frequency", "/txInfo/frequency"),
            Map.entry("spreadingFactor", "/txInfo/modulation/lora/spreadingFactor"),
            Map.entry("latitude", "/location/latitude"), Map.entry("longitude", "/location/longitude"),
            Map.entry("altitude", "/location/altitude")
    );

    @Override
    protected String getGatewayInfoPath() {
        return "/rxInfo";
    }

    @Override
    protected byte[] getPayload(JsonNode payloadJson) {
        if (payloadJson.has("object")) {
            return JacksonUtil.writeValueAsBytes(payloadJson.get("object"));
        }
        if (payloadJson.has("data")) {
            return Base64.getDecoder().decode(payloadJson.get("data").textValue());
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    protected Map<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }

}
