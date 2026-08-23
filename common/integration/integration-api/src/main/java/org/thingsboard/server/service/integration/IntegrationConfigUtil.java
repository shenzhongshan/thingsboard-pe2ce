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
import org.thingsboard.server.common.data.StringUtils;

/**
 * Reads fields out of an integration's configuration JSON.
 * <p>
 * These two helpers are used by nearly every protocol runtime, so they belong to the shared API
 * module — PE keeps the equivalent in {@code integration/api/util/IntegrationUtil}. They used to
 * sit as package-private statics on {@code KafkaIntegrationConsumer}, which worked only while every
 * runtime lived in one module: splitting per protocol made seven modules depend on Kafka for a
 * config getter.
 */
public final class IntegrationConfigUtil {

    private IntegrationConfigUtil() {
    }

    /**
     * Most integration types nest their client settings under {@code clientConfiguration};
     * a few keep them at the top level. Returns whichever node actually holds the settings.
     */
    public static JsonNode clientConfig(JsonNode cfg) {
        if (cfg != null && cfg.has("clientConfiguration")) {
            return cfg.get("clientConfiguration");
        }
        return cfg;
    }

    /** Returns the field's text value, or throws if it is absent or blank. */
    public static String required(JsonNode cfg, String field) {
        if (cfg == null || !cfg.has(field) || StringUtils.isEmpty(cfg.get(field).asText())) {
            throw new IllegalArgumentException("Missing required configuration field: " + field);
        }
        return cfg.get(field).asText();
    }

}
