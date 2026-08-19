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
package org.thingsboard.server.common.data.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EntityKeyType {
    ATTRIBUTE("attribute"),
    CLIENT_ATTRIBUTE("clientAttribute"),
    SHARED_ATTRIBUTE("sharedAttribute"),
    SERVER_ATTRIBUTE("serverAttribute"),
    TIME_SERIES("timeseries"),
    ENTITY_FIELD("entityField"),
    ALARM_FIELD("alarm");

    /**
     * The camelCase name the report engine's data-key configuration uses. Deliberately not a
     * {@code @JsonValue}: the wire format of this enum stays {@link #name()} as before.
     */
    private final String name;

    public static EntityKeyType fromName(String name) {
        for (EntityKeyType entityKeyType : values()) {
            if (entityKeyType.getName().equals(name)) {
                return entityKeyType;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported entity key type : %s", name));
    }

    public boolean isAttribute() {
        return this == ATTRIBUTE || this == CLIENT_ATTRIBUTE || this == SHARED_ATTRIBUTE || this == SERVER_ATTRIBUTE;
    }
}
