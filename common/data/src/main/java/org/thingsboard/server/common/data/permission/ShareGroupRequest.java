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
package org.thingsboard.server.common.data.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;

import java.util.List;

/**
 * Request body for sharing an entity group with a user group. Sharing ultimately creates
 * {@link GroupPermission}s; this request adds two conveniences over the raw permission API:
 * targeting an owner's "All" user group by owner id, and auto-provisioning a canonical
 * read-only / read-write role when no explicit roles are given.
 */
@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareGroupRequest {

    @Schema(description = "When 'allUserGroup' is true, the owner (Tenant or Customer) whose 'All' user group to share with.")
    private EntityId ownerId;

    @Schema(description = "Share with the 'All' user group of 'ownerId' instead of a specific user group.")
    private boolean allUserGroup;

    @Schema(description = "When 'allUserGroup' is false, the specific user group to share with.")
    private EntityGroupId userGroupId;

    @Schema(description = "Used only when 'roleIds' is absent: true creates/uses a read-only role, false a write role.")
    private boolean readElseWrite;

    @Schema(description = "Explicit group role ids to share with. When absent, a canonical role is used (see 'readElseWrite').")
    private List<RoleId> roleIds;

}
