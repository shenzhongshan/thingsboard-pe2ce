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
package org.thingsboard.server.dao.group;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the PE-parity "All" group persistence added in this port: every entity of the 6
 * non-edge types gets a real Contains/FROM_ENTITY_GROUP relation to its tenant's (and, if
 * customer-owned, its customer's) "All" group — see EntityGroupService#ensureAllGroupMembership
 * / #moveAllGroupMembership and the entity services wired to call them.
 */
@DaoSqlTest
public class BaseEntityGroupServiceTest extends AbstractServiceTest {

    @Autowired
    private EntityGroupService entityGroupService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private UserService userService;

    private boolean isInAllGroup(EntityType groupType, CustomerId ownerId, EntityId entityId) {
        var allGroupId = entityGroupService.findOrCreateAllGroupId(tenantId, ownerId, groupType);
        return relationService.checkRelation(tenantId, allGroupId, entityId,
                EntityGroupService.GROUP_RELATION_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
    }

    private Customer createCustomer(String title, CustomerId parentCustomerId) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle(title);
        customer.setParentCustomerId(parentCustomerId);
        return customerService.saveCustomer(customer);
    }

    private Device createDevice(String name, CustomerId customerId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(name);
        device.setType("default");
        if (customerId != null) {
            device.setCustomerId(customerId);
        }
        return deviceService.saveDeviceWithAccessToken(device, "TEST_TOKEN_" + name);
    }

    @Test
    public void testDeviceCreateAddsTenantAllGroupRelationOnly() {
        Device device = createDevice("d1", null);
        assertThat(isInAllGroup(EntityType.DEVICE, null, device.getId())).isTrue();
    }

    @Test
    public void testCustomerOwnedDeviceCreateAddsBothTenantAndCustomerAllGroupRelations() {
        Customer customer = createCustomer("c1", null);
        Device device = createDevice("d2", customer.getId());

        assertThat(isInAllGroup(EntityType.DEVICE, null, device.getId())).isTrue();
        assertThat(isInAllGroup(EntityType.DEVICE, customer.getId(), device.getId())).isTrue();
    }

    @Test
    public void testAssignAndUnassignDeviceMovesOnlyCustomerLevelRelation() {
        Customer customerA = createCustomer("custA", null);
        Customer customerB = createCustomer("custB", null);
        Device device = createDevice("d3", null);

        deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerA.getId());
        assertThat(isInAllGroup(EntityType.DEVICE, customerA.getId(), device.getId())).isTrue();
        assertThat(isInAllGroup(EntityType.DEVICE, null, device.getId())).isTrue();

        deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerB.getId());
        assertThat(isInAllGroup(EntityType.DEVICE, customerA.getId(), device.getId())).isFalse();
        assertThat(isInAllGroup(EntityType.DEVICE, customerB.getId(), device.getId())).isTrue();
        assertThat(isInAllGroup(EntityType.DEVICE, null, device.getId())).isTrue();

        deviceService.unassignDeviceFromCustomer(tenantId, device.getId());
        assertThat(isInAllGroup(EntityType.DEVICE, customerB.getId(), device.getId())).isFalse();
        assertThat(isInAllGroup(EntityType.DEVICE, null, device.getId())).isTrue();
    }

    @Test
    public void testDeleteDeviceRemovesAllGroupRelation() {
        Device device = createDevice("d4", null);
        assertThat(isInAllGroup(EntityType.DEVICE, null, device.getId())).isTrue();

        deviceService.deleteDevice(tenantId, device.getId());

        assertThat(relationService.findByTo(tenantId, device.getId(), RelationTypeGroup.FROM_ENTITY_GROUP)).isEmpty();
    }

    @Test
    public void testReparentingCustomerMovesCustomerGroupRelation() {
        Customer parentA = createCustomer("parentA", null);
        Customer parentB = createCustomer("parentB", null);
        Customer child = createCustomer("child", parentA.getId());

        assertThat(isInAllGroup(EntityType.CUSTOMER, parentA.getId(), child.getId())).isTrue();
        assertThat(isInAllGroup(EntityType.CUSTOMER, null, child.getId())).isTrue();

        child.setParentCustomerId(parentB.getId());
        customerService.saveCustomer(child);

        assertThat(isInAllGroup(EntityType.CUSTOMER, parentA.getId(), child.getId())).isFalse();
        assertThat(isInAllGroup(EntityType.CUSTOMER, parentB.getId(), child.getId())).isTrue();
        assertThat(isInAllGroup(EntityType.CUSTOMER, null, child.getId())).isTrue();
    }

    @Test
    public void testUserCreateUnderCustomerAddsBothLevelRelations() {
        Customer customer = createCustomer("custU", null);
        User user = new User();
        user.setTenantId(tenantId);
        user.setCustomerId(customer.getId());
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setEmail("entitygrouptest_" + System.nanoTime() + "@thingsboard.org");
        User savedUser = userService.saveUser(tenantId, user);

        assertThat(isInAllGroup(EntityType.USER, null, savedUser.getId())).isTrue();
        assertThat(isInAllGroup(EntityType.USER, customer.getId(), savedUser.getId())).isTrue();
    }

    @Test
    public void testOwnerIdRoundTripPreservesTenantVsCustomer() {
        Customer customer = createCustomer("ownerRoundTrip", null);

        EntityGroup tenantGroup = new EntityGroup();
        tenantGroup.setTenantId(tenantId);
        tenantGroup.setType(EntityType.DEVICE);
        tenantGroup.setName("tenant-owned-group");
        EntityGroup savedTenantGroup = entityGroupService.saveEntityGroup(tenantId, tenantGroup);
        EntityGroup reloadedTenantGroup = entityGroupService.findEntityGroupById(tenantId, savedTenantGroup.getId());
        assertThat(reloadedTenantGroup.getOwnerId()).isEqualTo(tenantId);

        EntityGroup customerGroup = new EntityGroup();
        customerGroup.setTenantId(tenantId);
        customerGroup.setOwnerId(customer.getId());
        customerGroup.setType(EntityType.DEVICE);
        customerGroup.setName("customer-owned-group");
        EntityGroup savedCustomerGroup = entityGroupService.saveEntityGroup(tenantId, customerGroup);
        EntityGroup reloadedCustomerGroup = entityGroupService.findEntityGroupById(tenantId, savedCustomerGroup.getId());
        assertThat(reloadedCustomerGroup.getOwnerId()).isEqualTo(customer.getId());
    }

    @Test
    public void testStaleVersionSaveThrowsOptimisticLockConflict() {
        EntityGroup group = new EntityGroup();
        group.setTenantId(tenantId);
        group.setType(EntityType.DEVICE);
        group.setName("version-conflict-group");
        EntityGroup saved = entityGroupService.saveEntityGroup(tenantId, group);

        EntityGroup update1 = entityGroupService.findEntityGroupById(tenantId, saved.getId());
        update1.setName("renamed-once");
        entityGroupService.saveEntityGroup(tenantId, update1);

        EntityGroup staleUpdate = entityGroupService.findEntityGroupById(tenantId, saved.getId());
        staleUpdate.setVersion(saved.getVersion());
        staleUpdate.setName("renamed-with-stale-version");

        assertThatThrownBy(() -> entityGroupService.saveEntityGroup(tenantId, staleUpdate))
                .isInstanceOfAny(ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class);
    }

}
