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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Builds the MQTT username/password pair Azure IoT Hub expects for SAS-token device auth. The
 * password (SAS token) is time-limited and computed fresh per connection attempt, matching PE's
 * AzureIotHubUtil.buildUsername/buildSasToken behavior.
 */
final class AzureIotHubUtil {

    static final long DEFAULT_TTL_SEC = TimeUnit.HOURS.toSeconds(1);

    private AzureIotHubUtil() {
    }

    static String buildUsername(String host, String deviceId) {
        return host + "/" + deviceId + "/?api-version=2021-04-12";
    }

    static String buildSasToken(String host, String deviceId, String sasKey, long ttlSeconds) {
        try {
            String resourceUri = urlEncode(host + "/devices/" + deviceId);
            long expiry = System.currentTimeMillis() / 1000L + ttlSeconds;
            String stringToSign = resourceUri + "\n" + expiry;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(sasKey), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
            return "SharedAccessSignature sr=" + resourceUri + "&sig=" + urlEncode(signature) + "&se=" + expiry;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Azure IoT Hub SAS token: " + e.getMessage(), e);
        }
    }

    private static String urlEncode(String value) throws java.io.UnsupportedEncodingException {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

}
