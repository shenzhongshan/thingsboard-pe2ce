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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the per-group-type resource split. These are pure enum/map invariants — no Spring context —
 * so they run without Docker, unlike the controller tests.
 */
class GroupResourcePermissionTest {

    @Test
    void groupResourceFromGroupType_mapsEveryGroupableType() {
        assertThat(Resource.groupResourceFromGroupType(EntityType.CUSTOMER)).isEqualTo(Resource.CUSTOMER_GROUP);
        assertThat(Resource.groupResourceFromGroupType(EntityType.DEVICE)).isEqualTo(Resource.DEVICE_GROUP);
        assertThat(Resource.groupResourceFromGroupType(EntityType.ASSET)).isEqualTo(Resource.ASSET_GROUP);
        assertThat(Resource.groupResourceFromGroupType(EntityType.USER)).isEqualTo(Resource.USER_GROUP);
        assertThat(Resource.groupResourceFromGroupType(EntityType.ENTITY_VIEW)).isEqualTo(Resource.ENTITY_VIEW_GROUP);
        assertThat(Resource.groupResourceFromGroupType(EntityType.EDGE)).isEqualTo(Resource.EDGE_GROUP);
        assertThat(Resource.groupResourceFromGroupType(EntityType.DASHBOARD)).isEqualTo(Resource.DASHBOARD_GROUP);
    }

    @Test
    void groupResourceFromGroupType_fallsBackToUmbrella() {
        assertThat(Resource.groupResourceFromGroupType(null)).isEqualTo(Resource.ENTITY_GROUP);
        assertThat(Resource.groupResourceFromGroupType(EntityType.RULE_CHAIN)).isEqualTo(Resource.ENTITY_GROUP);
    }

    /**
     * The seven group resources deliberately carry no entityTypes, so that of(ENTITY_GROUP) keeps
     * resolving to the umbrella. If someone gives them EntityType.ENTITY_GROUP (as PE does), of()
     * starts returning whichever happens to be declared first and entity-level checks silently
     * change meaning.
     */
    @Test
    void of_entityGroup_resolvesToUmbrellaNotAPerTypeResource() {
        assertThat(Resource.of(EntityType.ENTITY_GROUP)).isEqualTo(Resource.ENTITY_GROUP);
        assertThat(Resource.groupResources()).allSatisfy(r -> assertThat(r.getEntityTypes()).isEmpty());
    }

    @Test
    void umbrellaGrantImpliesEveryPerTypeGroupResource() {
        var perms = new MergedUserPermissions(
                Map.of(Resource.ENTITY_GROUP, Set.of(Operation.READ)), Collections.emptyMap());
        for (Resource groupResource : Resource.groupResources()) {
            assertThat(perms.hasGenericPermission(groupResource, Operation.READ))
                    .as("ENTITY_GROUP grant should cover %s", groupResource)
                    .isTrue();
        }
    }

    /** The actual new capability: a grant on one group type must not leak to the others. */
    @Test
    void perTypeGrantDoesNotLeakToOtherGroupTypes() {
        var perms = new MergedUserPermissions(
                Map.of(Resource.DEVICE_GROUP, Set.of(Operation.READ)), Collections.emptyMap());
        assertThat(perms.hasGenericPermission(Resource.DEVICE_GROUP, Operation.READ)).isTrue();
        assertThat(perms.hasGenericPermission(Resource.USER_GROUP, Operation.READ)).isFalse();
        assertThat(perms.hasGenericPermission(Resource.CUSTOMER_GROUP, Operation.READ)).isFalse();
        assertThat(perms.hasGenericPermission(Resource.ENTITY_GROUP, Operation.READ)).isFalse();
    }

    @Test
    void perTypeGrantDoesNotWidenTheOperation() {
        var perms = new MergedUserPermissions(
                Map.of(Resource.DEVICE_GROUP, Set.of(Operation.READ)), Collections.emptyMap());
        assertThat(perms.hasGenericPermission(Resource.DEVICE_GROUP, Operation.DELETE)).isFalse();
    }

    /** Group-scoped roles are keyed by group id and are unaffected by the resource split. */
    @Test
    void groupRoleGrantStillMatchesByGroupId() {
        var groupId = new EntityGroupId(UUID.randomUUID());
        var perms = new MergedUserPermissions(Collections.emptyMap(),
                Map.of(groupId, new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ))));
        assertThat(perms.hasGroupPermissions(groupId, Operation.READ)).isTrue();
        assertThat(perms.hasGroupPermissions(new EntityGroupId(UUID.randomUUID()), Operation.READ)).isFalse();
    }

    /**
     * A resource with no operations vocabulary is invisible to the role editor and to
     * /api/permissions/allowedPermissions, so it can never actually be granted.
     */
    @ParameterizedTest
    @EnumSource(Resource.class)
    void everyResourceDeclaresItsOperations(Resource resource) {
        assertThat(Resource.operationsForResource(resource))
                .as("%s has no entry in operationsByResource", resource)
                .isNotEmpty();
    }

    // --- resolution feeding the /api/user/{devices,assets,...} listings ---

    @Test
    void resolution_genericGrantYieldsOwnerScope() {
        var perms = new MergedUserPermissions(
                Map.of(Resource.DEVICE, Set.of(Operation.READ)), Collections.emptyMap());
        var resolved = perms.getGroupPermissionsByEntityTypeAndOperation(EntityType.DEVICE, Operation.READ);
        assertThat(resolved.hasGenericRead()).isTrue();
        assertThat(resolved.entityGroupIds()).isEmpty();
        assertThat(resolved.isEmpty()).isFalse();
    }

    @Test
    void resolution_groupGrantYieldsOnlyMatchingTypeGroups() {
        var deviceGroup = new EntityGroupId(UUID.randomUUID());
        var userGroup = new EntityGroupId(UUID.randomUUID());
        var perms = new MergedUserPermissions(Collections.emptyMap(), Map.of(
                deviceGroup, new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)),
                userGroup, new MergedGroupPermissionInfo(EntityType.USER, Set.of(Operation.READ))));

        var forDevices = perms.getGroupPermissionsByEntityTypeAndOperation(EntityType.DEVICE, Operation.READ);
        assertThat(forDevices.hasGenericRead()).isFalse();
        assertThat(forDevices.entityGroupIds()).containsExactly(deviceGroup);

        var forUsers = perms.getGroupPermissionsByEntityTypeAndOperation(EntityType.USER, Operation.READ);
        assertThat(forUsers.entityGroupIds()).containsExactly(userGroup);

        var forAssets = perms.getGroupPermissionsByEntityTypeAndOperation(EntityType.ASSET, Operation.READ);
        assertThat(forAssets.isEmpty()).isTrue();
    }

    @Test
    void resolution_groupGrantIgnoredWhenOperationNotCovered() {
        var deviceGroup = new EntityGroupId(UUID.randomUUID());
        var perms = new MergedUserPermissions(Collections.emptyMap(),
                Map.of(deviceGroup, new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ))));
        assertThat(perms.getGroupPermissionsByEntityTypeAndOperation(EntityType.DEVICE, Operation.DELETE).isEmpty())
                .isTrue();
    }

    /** Operations that group roles may not carry must never pull in group grants. */
    @Test
    void resolution_operationNotAllowedForGroupRoleSkipsGroups() {
        var deviceGroup = new EntityGroupId(UUID.randomUUID());
        var perms = new MergedUserPermissions(Collections.emptyMap(),
                Map.of(deviceGroup, new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        assertThat(Operation.CLAIM_DEVICES.isAllowedForGroupRole()).isFalse();
        assertThat(perms.getGroupPermissionsByEntityTypeAndOperation(EntityType.DEVICE, Operation.CLAIM_DEVICES)
                .entityGroupIds()).isEmpty();
    }

    /** No grant at all means "nothing visible", which the endpoints render as an empty page. */
    @Test
    void resolution_noGrantIsEmpty() {
        var perms = new MergedUserPermissions(Collections.emptyMap(), Collections.emptyMap());
        assertThat(perms.getGroupPermissionsByEntityTypeAndOperation(EntityType.DEVICE, Operation.READ).isEmpty())
                .isTrue();
    }

    @Test
    void resolution_allPermissionsGrantsGenericReadForEveryType() {
        for (EntityType type : new EntityType[]{EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER,
                EntityType.USER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD}) {
            assertThat(MergedUserPermissions.ALL.getGroupPermissionsByEntityTypeAndOperation(type, Operation.READ)
                    .hasGenericRead()).as("ALL should cover %s", type).isTrue();
        }
    }
}
