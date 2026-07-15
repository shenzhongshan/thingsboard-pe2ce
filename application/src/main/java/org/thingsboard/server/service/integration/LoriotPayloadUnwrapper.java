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

import java.util.HexFormat;
import java.util.Map;

/** Port of PE's LoriotConverterUnwrapper. */
class LoriotPayloadUnwrapper extends AbstractIntegrationPayloadUnwrapper {

    private static final Map<String, String> KEYS_MAPPING = Map.ofEntries(
            Map.entry("cmd", "/cmd"), Map.entry("seqno", "/seqno"), Map.entry("eui", "/EUI"),
            Map.entry("ts", "/ts"), Map.entry("ack", "/ack"), Map.entry("battery", "/bat"),
            Map.entry("fCnt", "/fcnt"), Map.entry("fPort", "/port"), Map.entry("offline", "/offline"),
            Map.entry("frequency", "/freq"), Map.entry("dr", "/dr"), Map.entry("rssi", "/rssi"),
            Map.entry("snr", "/snr"), Map.entry("toa", "/toa"), Map.entry("data", "/data"),
            Map.entry("decoded", "/decoded"), Map.entry("gws", "/gws")
    );

    @Override
    protected String getGatewayInfoPath() {
        return "/gws";
    }

    @Override
    protected byte[] getPayload(JsonNode payloadJson) {
        if (payloadJson.has("decoded")) {
            return JacksonUtil.writeValueAsBytes(payloadJson.get("decoded"));
        }
        if (payloadJson.has("data")) {
            return HexFormat.of().parseHex(payloadJson.get("data").textValue());
        }
        if (payloadJson.has("encdata")) {
            return HexFormat.of().parseHex(payloadJson.get("encdata").textValue());
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    protected void postMapping(Map<String, String> kvMap) {
        kvMap.putIfAbsent("ts", String.valueOf(System.currentTimeMillis()));
    }

    @Override
    protected Map<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }

}
