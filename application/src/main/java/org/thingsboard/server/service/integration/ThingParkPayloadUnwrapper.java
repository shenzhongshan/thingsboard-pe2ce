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

/** Port of PE's ThingParkConverterUnwrapper, shared by both THINGPARK and TPE (ThingPark Enterprise). */
class ThingParkPayloadUnwrapper extends AbstractIntegrationPayloadUnwrapper {

    private static final Map<String, String> KEYS_MAPPING = Map.ofEntries(
            Map.entry("time", "/DevEUI_uplink/Time"), Map.entry("eui", "/DevEUI_uplink/DevEUI"),
            Map.entry("fPort", "/DevEUI_uplink/FPort"), Map.entry("fCnt", "/DevEUI_uplink/FCntUp"),
            Map.entry("adr", "/DevEUI_uplink/ADRbit"), Map.entry("mType", "/DevEUI_uplink/MType"),
            Map.entry("data", "/DevEUI_uplink/payload_hex"), Map.entry("rssi", "/DevEUI_uplink/LrrRSSI"),
            Map.entry("snr", "/DevEUI_uplink/LrrSNR"), Map.entry("spreadingFactor", "/DevEUI_uplink/SpFact"),
            Map.entry("channel", "/DevEUI_uplink/Channel"), Map.entry("latitude", "/DevEUI_uplink/LrrLAT"),
            Map.entry("longitude", "/DevEUI_uplink/LrrLON"), Map.entry("devAddr", "/DevEUI_uplink/DevAddr"),
            Map.entry("frequency", "/DevEUI_uplink/Frequency"), Map.entry("decoded", "/DevEUI_uplink/payload")
    );

    @Override
    protected byte[] getPayload(JsonNode payloadJson) {
        JsonNode uplink = payloadJson.get("DevEUI_uplink");
        if (uplink == null) {
            return EMPTY_BYTE_ARRAY;
        }
        if (uplink.has("payload")) {
            return JacksonUtil.writeValueAsBytes(uplink.get("payload"));
        }
        if (uplink.has("payload_hex")) {
            return HexFormat.of().parseHex(uplink.get("payload_hex").textValue());
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    protected Map<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }

}
