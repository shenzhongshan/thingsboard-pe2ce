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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.Set;

/**
 * Response of GET /api/permissions/allowedPermissions. Combines the static permission
 * vocabulary (what could ever be granted for this authority) with the concrete permissions
 * merged for the current user, so the UI can hide elements the user cannot act on.
 * Server-side checks still run on every API call regardless of this response.
 */
@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllowedPermissionsInfo {

    @Schema(description = "Static map (vocabulary) of allowed operations by resource type")
    private Map<Resource, Set<Operation>> operationsByResource;

    @Schema(description = "Static set (vocabulary) of allowed operations for group roles")
    private Set<Operation> allowedForGroupRoleOperations;

    @Schema(description = "Static set (vocabulary) of allowed operations for group owner")
    private Set<Operation> allowedForGroupOwnerOnlyOperations;

    @Schema(description = "Static set (vocabulary) of allowed group operations for group owner")
    private Set<Operation> allowedForGroupOwnerOnlyGroupOperations;

    @Schema(description = "Static set (vocabulary) of all possibly allowed resources. Depends only on the authority of the user")
    private Set<Resource> allowedResources;

    @Schema(description = "Merged permissions for all generic and group roles assigned to the user")
    private MergedUserPermissions userPermissions;

    @Schema(description = "Owner Id of the user (Tenant or Customer)")
    private EntityId userOwnerId;

}
