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
package org.thingsboard.server.service.edge.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.ReportTemplateEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SecretEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SysAdminRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantRolesEdgeEventFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers the full-sync fetcher registration. This is the only place these fetchers can be observed
 * without a live edge: {@link EdgeSyncCursor} is constructed exclusively from
 * {@code EdgeGrpcSession.doSync}, so nothing runs them until a real edge connects over gRPC -
 * creating and activating an edge through the REST API is not enough.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EdgeSyncCursorTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EdgeContextComponent ctx;
    @Mock
    private RoleService roleService;
    @Mock
    private SecretService secretService;
    @Mock
    private ReportTemplateService reportTemplateService;

    private Edge edge(CustomerId customerId) {
        Edge edge = new Edge(new EdgeId(UUID.randomUUID()));
        edge.setTenantId(tenantId);
        edge.setName("test-edge");
        edge.setCustomerId(customerId);
        return edge;
    }

    private List<EdgeEventFetcher> fetchersOf(EdgeSyncCursor cursor) {
        List<EdgeEventFetcher> all = new ArrayList<>();
        while (cursor.hasNext()) {
            all.add(cursor.getNext());
        }
        return all;
    }

    private EdgeSyncCursor newCursor(Edge edge, boolean fullSync) {
        when(ctx.getRoleService()).thenReturn(roleService);
        when(ctx.getSecretService()).thenReturn(secretService);
        when(ctx.getReportTemplateService()).thenReturn(reportTemplateService);
        when(ctx.getCustomerService().findPublicCustomer(any())).thenReturn((Customer) null);
        return new EdgeSyncCursor(ctx, edge, fullSync);
    }

    @Test
    public void fullSyncRegistersRoleSecretAndReportTemplateFetchers() {
        List<EdgeEventFetcher> fetchers = fetchersOf(newCursor(edge(null), true));

        assertThat(fetchers).as("sys-admin roles are synced so tenants inherit system roles")
                .hasAtLeastOneElementOfType(SysAdminRolesEdgeEventFetcher.class);
        assertThat(fetchers).hasAtLeastOneElementOfType(TenantRolesEdgeEventFetcher.class);
        assertThat(fetchers).hasAtLeastOneElementOfType(SecretEdgeEventFetcher.class);
        assertThat(fetchers).hasAtLeastOneElementOfType(ReportTemplateEdgeEventFetcher.class);
    }

    @Test
    public void rolesAreSyncedBeforeTheEntitiesThatReferenceThem() {
        List<Class<?>> types = fetchersOf(newCursor(edge(null), true)).stream()
                .<Class<?>>map(Object::getClass).toList();

        assertThat(types.indexOf(TenantRolesEdgeEventFetcher.class))
                .as("roles must precede report templates, matching PE's ordering")
                .isLessThan(types.indexOf(ReportTemplateEdgeEventFetcher.class));
    }

    @Test
    public void partialSyncSkipsTheFullSyncOnlyFetchers() {
        List<EdgeEventFetcher> fetchers = fetchersOf(newCursor(edge(null), false));

        assertThat(fetchers).doesNotHaveAnyElementsOfTypes(
                SysAdminRolesEdgeEventFetcher.class, TenantRolesEdgeEventFetcher.class,
                SecretEdgeEventFetcher.class, ReportTemplateEdgeEventFetcher.class);
    }

    @Test
    public void customerOwnedEdgeAlsoGetsItsOwnersReportTemplates() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        long tenantLevel = fetchersOf(newCursor(edge(null), true)).stream()
                .filter(ReportTemplateEdgeEventFetcher.class::isInstance).count();
        long customerLevel = fetchersOf(newCursor(edge(customerId), true)).stream()
                .filter(ReportTemplateEdgeEventFetcher.class::isInstance).count();

        assertThat(tenantLevel).isEqualTo(1);
        assertThat(customerLevel).as("a customer-owned edge gets the tenant's templates plus its own")
                .isEqualTo(2);
    }

    @Test
    public void roleFetcherEmitsRoleAddedEventsForTenantRoles() {
        Edge edge = edge(null);
        Role role = new Role(new RoleId(UUID.randomUUID()));
        role.setTenantId(tenantId);
        when(roleService.findRolesByTenantId(eq(tenantId), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(role), 1, 1, false));

        PageData<EdgeEvent> events = new TenantRolesEdgeEventFetcher(roleService)
                .fetchEdgeEvents(tenantId, edge, new PageLink(100));

        assertThat(events.getData()).singleElement().satisfies(event -> {
            assertThat(event.getType()).isEqualTo(EdgeEventType.ROLE);
            assertThat(event.getAction()).isEqualTo(EdgeEventActionType.ADDED);
            assertThat(event.getEntityId()).isEqualTo(role.getId().getId());
        });
    }

    @Test
    public void sysAdminRoleFetcherReadsSystemTenantNotTheEdgeTenant() {
        Edge edge = edge(null);
        when(roleService.findRolesByTenantId(eq(TenantId.SYS_TENANT_ID), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        new SysAdminRolesEdgeEventFetcher(roleService).fetchEdgeEvents(tenantId, edge, new PageLink(100));

        // If it had used the edge's tenantId the stub above would not have matched and the fetcher
        // would have NPE'd on a null PageData - reaching here is the assertion.
        assertThat(true).isTrue();
    }

    @Test
    public void secretFetcherEmitsSecretAddedEvents() {
        Edge edge = edge(null);
        SecretInfo secret = new SecretInfo(new SecretId(UUID.randomUUID()));
        secret.setTenantId(tenantId);
        when(secretService.findSecretInfosByTenantId(eq(tenantId), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(secret), 1, 1, false));

        PageData<EdgeEvent> events = new SecretEdgeEventFetcher(secretService)
                .fetchEdgeEvents(tenantId, edge, new PageLink(100));

        assertThat(events.getData()).singleElement().satisfies(event -> {
            assertThat(event.getType()).isEqualTo(EdgeEventType.SECRET);
            assertThat(event.getEntityId()).isEqualTo(secret.getId().getId());
        });
    }

    @Test
    public void reportTemplateFetcherPassesItsCustomerIdThrough() {
        Edge edge = edge(null);
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        ReportTemplateInfo template = new ReportTemplateInfo();
        template.setId(new ReportTemplateId(UUID.randomUUID()));
        when(reportTemplateService.findReportTemplateInfos(eq(tenantId), eq(customerId), any(PageLink.class)))
                .thenReturn(new PageData<>(List.of(template), 1, 1, false));

        PageData<EdgeEvent> events = new ReportTemplateEdgeEventFetcher(reportTemplateService, customerId)
                .fetchEdgeEvents(tenantId, edge, new PageLink(100));

        assertThat(events.getData()).singleElement().satisfies(event -> {
            assertThat(event.getType()).isEqualTo(EdgeEventType.REPORT_TEMPLATE);
            assertThat(event.getEntityId()).isEqualTo(template.getId().getId());
        });
    }

}
