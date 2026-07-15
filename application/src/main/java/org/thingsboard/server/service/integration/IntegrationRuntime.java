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

import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

/**
 * A running, in-process integration client/server (MQTT client, TCP/UDP server, cloud MQ consumer,
 * etc). One instance per active non-passive Integration entity. Registered/created via an
 * IntegrationRuntimeFactory keyed by IntegrationType.
 */
public interface IntegrationRuntime {

    void start() throws Exception;

    void stop();

    TenantId getTenantId();

    /**
     * Pushes a downlink payload to the integration's external endpoint. Default: unsupported.
     * Override in runtimes whose protocol supports server-initiated messages (e.g. MQTT publish).
     */
    default void pushDownlink(byte[] payload, Map<String, String> metadata) throws Exception {
        throw new UnsupportedOperationException("Downlink is not supported for this integration type!");
    }

}
