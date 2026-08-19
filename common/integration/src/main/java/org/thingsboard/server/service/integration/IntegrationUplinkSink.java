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

import org.thingsboard.server.common.data.integration.Integration;

import java.util.Map;

/**
 * Where an integration runtime hands the payloads it receives.
 * <p>
 * This is the <em>only</em> thing the protocol runtimes need from their host, which is what lets
 * the same runtime run in two very different places:
 * <ul>
 *     <li>inside the platform, where the implementation runs the uplink converter locally;</li>
 *     <li>inside a remote integration process, where the implementation forwards the payload to
 *         the platform over gRPC.</li>
 * </ul>
 * Keep this interface at one method - every extra method makes the remote process harder to build.
 */
public interface IntegrationUplinkSink {

    /**
     * @param integration the integration that received the payload
     * @param payload     the raw payload as it arrived from the device or broker
     * @param metadata    protocol-level metadata (topic, headers, source address, ...)
     */
    void processUplink(Integration integration, byte[] payload, Map<String, String> metadata) throws Exception;

}
