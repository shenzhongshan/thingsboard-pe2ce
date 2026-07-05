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
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;

import java.util.List;
import java.util.UUID;

public interface GroupPermissionRepository extends JpaRepository<GroupPermissionEntity, UUID> {

    List<GroupPermissionEntity> findByTenantIdAndUserGroupId(UUID tenantId, UUID userGroupId);

    List<GroupPermissionEntity> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);

    List<GroupPermissionEntity> findByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId);

}
