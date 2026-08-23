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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * One message on its way out to a device, as produced by an integration's downlink converter.
 * <p>
 * The converter script returns JSON; this is the parsed, protocol-ready form of it. Shape and
 * validation match PE's {@code AbstractDownlinkDataConverter.parseDownlinkData} so existing
 * encoder scripts port over unchanged.
 *
 * @param contentType JSON, TEXT or BINARY — tells the runtime how the bytes were encoded
 * @param data        the payload bytes
 * @param metadata    protocol hints for the runtime (MQTT topic, HTTP headers, ...)
 */
public record DownlinkData(String contentType, byte[] data, Map<String, String> metadata) {

    public boolean isEmpty() {
        return data == null || data.length == 0;
    }

    /**
     * Parses one downlink object emitted by an encoder script.
     *
     * @throws IllegalArgumentException if the script returned something unusable — better a clear
     *                                  error than silently sending an empty or wrong payload
     */
    public static DownlinkData parse(JsonNode src) {
        if (src == null || !src.isObject()) {
            throw new IllegalArgumentException("Invalid downlink json type: "
                    + (src == null ? "null" : src.getNodeType().toString()));
        }
        if (!src.has("contentType")) {
            throw new IllegalArgumentException("Downlink content type is not set!");
        }
        if (!src.has("data")) {
            throw new IllegalArgumentException("Downlink data is not set!");
        }
        String contentType = src.get("contentType").asText();
        String strData = src.get("data").asText();
        byte[] data = switch (contentType) {
            case "JSON", "TEXT" -> strData.getBytes(StandardCharsets.UTF_8);
            case "BINARY" -> Base64.getDecoder().decode(strData);
            default -> throw new IllegalArgumentException("Unknown downlink content type " + contentType);
        };

        Map<String, String> metadata = new HashMap<>();
        if (src.has("metadata")) {
            JsonNode metadataElement = src.get("metadata");
            if (!metadataElement.isObject()) {
                throw new IllegalArgumentException("Invalid downlink metadata format!");
            }
            metadataElement.fields().forEachRemaining(entry -> {
                if (!entry.getValue().isValueNode()) {
                    throw new IllegalArgumentException("Invalid downlink metadata value format!");
                }
                metadata.put(entry.getKey(), entry.getValue().asText());
            });
        }
        return new DownlinkData(contentType, data, metadata);
    }

}
