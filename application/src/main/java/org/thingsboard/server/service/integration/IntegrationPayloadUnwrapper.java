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

import java.util.Map;

/**
 * Pre-converter transform for LPWAN network-server webhook payloads (LoRaWAN, etc): extracts the
 * vendor envelope's metadata fields (EUI, fPort, rssi/snr, ...) into the uplink metadata map and
 * unwraps the actual device payload so the user's uplink converter script sees just the device
 * data, not the network-server envelope. Mirrors PE's ConverterUnwrapper.
 */
public interface IntegrationPayloadUnwrapper {

    UnwrapResult unwrap(byte[] payload, Map<String, String> metadata) throws Exception;

    record UnwrapResult(byte[] payload, Map<String, String> metadata) {
    }

}
