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

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Port of PE's AbstractConverterUnwrapper: flattens a subset of the vendor JSON envelope (by
 * JSON-pointer path) into the metadata map, picks the strongest-RSSI gateway's rssi/snr if the
 * envelope carries an array of receiving gateways, then extracts the actual device payload.
 */
abstract class AbstractIntegrationPayloadUnwrapper implements IntegrationPayloadUnwrapper {

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Override
    public UnwrapResult unwrap(byte[] payload, Map<String, String> metadata) throws Exception {
        JsonNode payloadJson = JacksonUtil.fromBytes(payload);
        Map<String, String> kvMap = new TreeMap<>(metadata);
        getKeysMapping().forEach((name, path) -> {
            if (path.isEmpty()) {
                return;
            }
            JsonNode value = payloadJson.at(path);
            if (!value.isMissingNode() && !value.isNull()) {
                kvMap.put(name, value.isTextual() ? value.asText() : value.toString());
            }
        });
        addGatewayInfo(kvMap, payloadJson);
        postMapping(kvMap);
        // caller-supplied metadata (e.g. HTTP headers) takes precedence over extracted keys, matching PE.
        kvMap.putAll(metadata);
        byte[] devicePayload = getPayload(payloadJson);
        return new UnwrapResult(devicePayload, kvMap);
    }

    protected void addGatewayInfo(Map<String, String> kvMap, JsonNode payloadJson) {
        JsonNode gwArray = payloadJson.at(getGatewayInfoPath());
        if (gwArray.isMissingNode() || !gwArray.isArray() || gwArray.isEmpty()) {
            return;
        }
        JsonNode best = null;
        int maxRssi = Integer.MIN_VALUE;
        for (JsonNode node : gwArray) {
            JsonNode rssiNode = node.get("rssi");
            if (rssiNode == null || !rssiNode.isNumber() || rssiNode.asInt() <= maxRssi) {
                continue;
            }
            maxRssi = rssiNode.asInt();
            best = node;
        }
        if (best == null) {
            return;
        }
        if (best.has("rssi")) {
            kvMap.put("rssi", best.get("rssi").asText());
        }
        if (best.has("snr")) {
            kvMap.put("snr", best.get("snr").asText());
        }
    }

    protected String getGatewayInfoPath() {
        return "";
    }

    protected void postMapping(Map<String, String> kvMap) {
        long ts = 0L;
        if (kvMap.containsKey("time")) {
            ts = parseDateToTimestamp(kvMap.get("time"));
        }
        if (ts == 0L) {
            ts = System.currentTimeMillis();
        }
        kvMap.put("ts", String.valueOf(ts));
    }

    protected static long parseDateToTimestamp(String dateString) {
        try {
            return OffsetDateTime.parse(dateString).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0L;
        }
    }

    protected abstract byte[] getPayload(JsonNode payloadJson) throws Exception;

    protected abstract Map<String, String> getKeysMapping();

}
