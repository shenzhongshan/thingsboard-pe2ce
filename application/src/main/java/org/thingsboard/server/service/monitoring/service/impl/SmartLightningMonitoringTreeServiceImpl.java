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
package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.UserEntitiesService;
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeResponse;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringTreeService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.List;

/** 智能防雷资产树的业务实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningMonitoringTreeServiceImpl implements SmartLightningMonitoringTreeService {

    private static final int TREE_ENTITY_PAGE_SIZE = 10_000;

    private final UserEntitiesService userEntitiesService;
    private final RelationService relationService;
    private final SmartLightningMonitoringTreeBuilder treeBuilder = new SmartLightningMonitoringTreeBuilder();

    @Override
    public SmartLightningMonitoringTreeResponse getTree(SecurityUser user) throws ThingsboardException {
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleAssets = findVisibleEntities(user, EntityType.ASSET);
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleDevices = findVisibleEntities(user, EntityType.DEVICE);
        List<EntityRelation> relations = new ArrayList<>();

        // 只从可见资产向外查 Contains；构建器还会过滤不可见的关系另一端。
        for (SmartLightningMonitoringTreeBuilder.VisibleEntity asset : visibleAssets) {
            relations.addAll(relationService.findByFrom(user.getTenantId(), new AssetId(asset.id()), RelationTypeGroup.COMMON));
        }
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleEntities = new ArrayList<>(visibleAssets);
        visibleEntities.addAll(visibleDevices);
        return treeBuilder.build(visibleEntities, relations);
    }

    /** 统一经平台授权服务读取实体，不能按租户直接查询，以保留共享实体的权限边界。 */
    private List<SmartLightningMonitoringTreeBuilder.VisibleEntity> findVisibleEntities(SecurityUser user, EntityType entityType)
            throws ThingsboardException {
        PageData<? extends HasName> pageData = userEntitiesService.findUserEntities(
                user, entityType, Operation.READ, new PageLink(TREE_ENTITY_PAGE_SIZE, 0));
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleEntities = new ArrayList<>();
        for (HasName entity : pageData.getData()) {
            if (entity instanceof HasId<?> identifiedEntity && identifiedEntity.getId() != null) {
                visibleEntities.add(new SmartLightningMonitoringTreeBuilder.VisibleEntity(
                        identifiedEntity.getId().getId(), entity.getName(), entityType));
            }
        }
        return visibleEntities;
    }
}
