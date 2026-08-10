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
package org.thingsboard.server.service.security.permission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.security.Authority;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * A ceiling on what an internal API key may do, declared per authority.
 * <p>
 * Stored on the key as raw JSON and materialised here, because {@link Resource} and
 * {@link Operation} live in this module rather than in the shared data model.
 */
@Schema
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityPermissionsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Permissions per authority, e.g. " +
            "{\"TENANT_ADMIN\": {\"DEVICE\": [\"READ\", \"WRITE\"]}, \"CUSTOMER_USER\": {\"DEVICE\": [\"READ\"]}}")
    private Map<Authority, Map<Resource, Set<Operation>>> operationsByResource;

    public Map<Resource, Set<Operation>> getPermissionsForAuthority(Authority authority) {
        if (operationsByResource == null || authority == null) {
            return null;
        }
        return operationsByResource.get(authority);
    }

    /** Null-safe: a key with no declared ceiling simply has none. */
    public static AuthorityPermissionsInfo fromJson(JsonNode json) {
        return json != null && !json.isNull() ? JacksonUtil.convertValue(json, AuthorityPermissionsInfo.class) : null;
    }

    public JsonNode toJson() {
        return JacksonUtil.valueToTree(this);
    }

}
