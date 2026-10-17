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
package org.thingsboard.server.service.cf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.user.UserService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the EDGE branch added to OwnerService#getOwner / #changeOwner so that
 * TbChangeOwnerNode becomes a true superset of the now-deprecated
 * TbAssignToCustomerNode/TbUnassignFromCustomerNode (which already supported Edge as
 * originator via EdgeService#assignEdgeToCustomer/#unassignEdgeFromCustomer).
 */
@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private AssetService assetService;
    @Mock
    private CustomerService customerService;
    @Mock
    private EntityViewService entityViewService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private EdgeService edgeService;

    private OwnerService ownerService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());
    private final CustomerId customerId = new CustomerId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        ownerService = new OwnerService(deviceService, assetService, customerService,
                entityViewService, dashboardService, userService, roleService, edgeService);
    }

    @Test
    void getOwner_edgeAssignedToCustomer_returnsCustomer() {
        when(edgeService.findEdgeById(tenantId, edgeId)).thenReturn(edge(customerId));

        assertThat(ownerService.getOwner(tenantId, edgeId)).isEqualTo(customerId);
    }

    /**
     * 阳性对照：customerId 用 NULL_UUID 哨兵表示"未分配给客户"，不是 null。
     * 真对象而非 mock(Edge.class) —— getOwnerId() 是带哨兵判断的派生逻辑，
     * stub 掉它这两个用例就都成了空转。
     */
    @Test
    void getOwner_edgeNotAssignedToCustomer_returnsTenant() {
        when(edgeService.findEdgeById(tenantId, edgeId)).thenReturn(edge(new CustomerId(CustomerId.NULL_UUID)));

        assertThat(ownerService.getOwner(tenantId, edgeId)).isEqualTo(tenantId);
    }

    private Edge edge(CustomerId customerId) {
        Edge edge = new Edge(edgeId);
        edge.setTenantId(tenantId);
        edge.setCustomerId(customerId);
        return edge;
    }

    @Test
    void changeOwner_edgeToCustomer_assignsEdgeToCustomer() {
        ownerService.changeOwner(tenantId, customerId, edgeId);

        verify(edgeService).assignEdgeToCustomer(eq(tenantId), eq(edgeId), eq(customerId));
        verifyNoMoreInteractions(edgeService);
    }

    @Test
    void changeOwner_edgeToTenant_unassignsEdgeFromCustomer() {
        ownerService.changeOwner(tenantId, tenantId, edgeId);

        verify(edgeService).unassignEdgeFromCustomer(eq(tenantId), eq(edgeId));
        verifyNoMoreInteractions(edgeService);
    }

}
