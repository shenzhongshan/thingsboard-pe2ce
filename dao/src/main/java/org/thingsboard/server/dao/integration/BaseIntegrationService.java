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
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.IntegrationEntity;
import org.thingsboard.server.dao.sql.integration.ConverterRepository;
import org.thingsboard.server.dao.sql.integration.IntegrationRepository;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseIntegrationService implements IntegrationService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RelationService relationService;

    // @Lazy：edge 侧的清理逻辑会反过来查集成，构造依赖会在启动时成环。
    @Autowired
    @Lazy
    private EdgeService edgeService;

    private final IntegrationRepository integrationRepository;
    private final ConverterRepository converterRepository;
    private final TbTenantProfileCache tbTenantProfileCache;

    @Value("${debug.settings.default_duration:15}")
    private int defaultDebugDurationMinutes;

    @Override
    public Integration saveIntegration(Integration integration) {
        log.trace("Executing saveIntegration [{}]", integration);
        if (StringUtils.isEmpty(integration.getName())) {
            throw new DataValidationException("Integration name should be specified!");
        }
        if (integration.getType() == null) {
            throw new DataValidationException("Integration type should be specified!");
        }
        if (integration.getTenantId() == null) {
            throw new DataValidationException("Integration should be assigned to tenant!");
        }
        if (integration.getDefaultConverterId() == null) {
            throw new DataValidationException("Integration uplink converter should be specified!");
        }
        var uplinkConverter = converterRepository.findById(integration.getDefaultConverterId().getId());
        if (uplinkConverter.isEmpty()) {
            throw new DataValidationException("Integration is referencing a non-existent uplink converter!");
        }
        // 模板与普通实体是两个互不相交的命名空间：模板集成下发到 edge 后要在那边找到它的
        // 转换器，而普通转换器不会被下发；反过来普通集成引用模板转换器，云端跑起来也会缺件。
        if (uplinkConverter.get().isEdgeTemplate() != integration.isEdgeTemplate()) {
            throw new DataValidationException("Edge integration can't have non-edge converter and vise versa!");
        }
        if (StringUtils.isEmpty(integration.getRoutingKey())) {
            integration.setRoutingKey(UUID.randomUUID().toString());
        }
        if (StringUtils.isEmpty(integration.getSecret())) {
            integration.setSecret(StringUtils.randomAlphanumeric(24));
        }
        IntegrationEntity existing = integrationRepository.findByRoutingKey(integration.getRoutingKey());
        if (existing != null && (integration.getId() == null || !existing.getId().equals(integration.getId().getId()))) {
            throw new DataValidationException("Integration with such routing key already exists!");
        }
        if (integration.getId() == null) {
            integration.setCreatedTime(System.currentTimeMillis());
        }
        DebugModeUtil.migrateDebugSettings(integration,
                tbTenantProfileCache.get(integration.getTenantId()).getDefaultProfileConfiguration().getMaxDebugModeDurationMinutes(),
                defaultDebugDurationMinutes, System.currentTimeMillis());
        boolean created = integration.getId() == null;
        Integration saved = integrationRepository.save(new IntegrationEntity(integration)).toData();
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId()).entityId(saved.getId())
                .entity(saved).created(created).broadcastEvent(true).build());
        return saved;
    }

    @Override
    public Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId) {
        return DaoUtil.getData(integrationRepository.findById(integrationId.getId()));
    }

    @Override
    public Optional<Integration> findIntegrationByRoutingKey(String routingKey) {
        IntegrationEntity entity = integrationRepository.findByRoutingKey(routingKey);
        return Optional.ofNullable(entity != null ? entity.toData() : null);
    }

    @Override
    public List<Integration> findAllIntegrations(TenantId tenantId) {
        return DaoUtil.convertDataList(integrationRepository.findByTenantId(tenantId.getId()));
    }

    @Override
    public List<Integration> findAllEnabledIntegrations() {
        return DaoUtil.convertDataList(integrationRepository.findAllByEnabledTrue());
    }

    @Override
    public List<Integration> findIntegrationsByConverterId(TenantId tenantId, ConverterId converterId) {
        return DaoUtil.convertDataList(
                integrationRepository.findByTenantIdAndConverterId(tenantId.getId(), converterId.getId()));
    }

    @Override
    public PageData<Integration> findTenantIntegrations(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(integrationRepository.findByTenantId(
                tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void deleteIntegration(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing deleteIntegration [{}]", integrationId);
        integrationRepository.deleteById(integrationId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(integrationId).build());
    }

    @Override
    public void deleteIntegrationsByTenantId(TenantId tenantId) {
        for (IntegrationEntity entity : integrationRepository.findByTenantId(tenantId.getId())) {
            integrationRepository.deleteById(entity.getUuid());
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findIntegrationById(tenantId, new IntegrationId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(Futures.immediateFuture(findEntity(tenantId, entityId)));
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteIntegration(tenantId, new IntegrationId(id.getId()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteIntegrationsByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }


    // ---- edge 分配 ----
    // 只有 edge 模板集成能分配给 edge：普通集成在云端有运行实例，下发给 edge 会变成
    // 两个进程同时连同一个外部端点。
    @Override
    public Integration assignIntegrationToEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId) {
        Integration integration = findIntegrationById(tenantId, integrationId);
        if (integration == null) {
            throw new DataValidationException("Can't assign non-existent integration to edge!");
        }
        if (!integration.isEdgeTemplate()) {
            // 普通集成在云端有运行实例；下发给 edge 会变成两个进程同时连同一个外部端点
            // （同一个 MQTT topic / 同一个 Kafka 消费组），消息被随机瓜分。
            throw new DataValidationException("Only edge template integration can be assigned to edge!");
        }
        if (edgeService.findEdgeById(tenantId, edgeId) == null) {
            throw new DataValidationException("Can't assign integration to non-existent edge!");
        }
        relationService.saveRelation(tenantId,
                new EntityRelation(edgeId, integrationId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId).edgeId(edgeId).entityId(integrationId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return integration;
    }

    @Override
    public Integration unassignIntegrationFromEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId) {
        Integration integration = findIntegrationById(tenantId, integrationId);
        if (integration == null) {
            throw new DataValidationException("Can't unassign non-existent integration from edge!");
        }
        if (edgeService.findEdgeById(tenantId, edgeId) == null) {
            throw new DataValidationException("Can't unassign integration from non-existent edge!");
        }
        relationService.deleteRelation(tenantId, edgeId, integrationId,
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId).edgeId(edgeId).entityId(integrationId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return integration;
    }

    @Override
    public PageData<Integration> findIntegrationsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        return DaoUtil.toPageData(integrationRepository.findByTenantIdAndEdgeId(
                tenantId.getId(), edgeId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }


    @Override
    public PageData<Integration> findTenantEdgeTemplateIntegrations(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(integrationRepository.findByTenantIdAndIsEdgeTemplate(
                tenantId.getId(), true, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

}
