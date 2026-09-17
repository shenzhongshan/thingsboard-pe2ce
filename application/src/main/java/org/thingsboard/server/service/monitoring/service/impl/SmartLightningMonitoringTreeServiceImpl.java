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
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringTreeService;
import org.thingsboard.server.service.query.VisibleAssetTree;
import org.thingsboard.server.service.query.VisibleAssetTreeService;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.List;

/** 智能防雷资产树的业务实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningMonitoringTreeServiceImpl
        implements SmartLightningMonitoringTreeService, VisibleAssetTreeService {

    private static final int TREE_ENTITY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;

    private final UserEntitiesService userEntitiesService;
    private final RelationService relationService;
    private final SmartLightningMonitoringTreeBuilder treeBuilder = new SmartLightningMonitoringTreeBuilder();

    @Override
    public SmartLightningMonitoringTreeDTO getTree(SecurityUser user) throws ThingsboardException {
        return toDTO(buildVisibleAssetTree(user));
    }

    /** 返回中性的内部树模型，告警与监测业务均可复用且不依赖 HTTP Response。 */
    @Override
    public VisibleAssetTree getVisibleAssetTree(SecurityUser user) throws ThingsboardException {
        return buildVisibleAssetTree(user);
    }

    private VisibleAssetTree buildVisibleAssetTree(SecurityUser user) throws ThingsboardException {
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleAssets = findVisibleEntities(user, EntityType.ASSET);
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleDevices = findVisibleEntities(user, EntityType.DEVICE);
        List<EntityRelation> relations = new ArrayList<>();

        // 只从可见资产向外批量查 Contains；构建器还会过滤不可见的关系另一端。
        List<AssetId> assetIds = new ArrayList<>();
        for (SmartLightningMonitoringTreeBuilder.VisibleEntity asset : visibleAssets) {
            assetIds.add(new AssetId(asset.id()));
        }
        relations.addAll(relationService.findByFrom(user.getTenantId(), assetIds, RelationTypeGroup.COMMON));
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleEntities = new ArrayList<>(visibleAssets);
        visibleEntities.addAll(visibleDevices);
        return treeBuilder.build(visibleEntities, relations);
    }

    /** 在服务边界把内部树模型转换为 DTO。 */
    private SmartLightningMonitoringTreeDTO toDTO(VisibleAssetTree source) {
        List<SmartLightningMonitoringTreeDTO.TreeNode> roots = new ArrayList<>();
        if (source.roots() != null) {
            for (VisibleAssetTree.Node root : source.roots()) {
                roots.add(toDTONode(root));
            }
        }
        return new SmartLightningMonitoringTreeDTO(roots);
    }

    private SmartLightningMonitoringTreeDTO.TreeNode toDTONode(VisibleAssetTree.Node source) {
        List<SmartLightningMonitoringTreeDTO.TreeNode> children = new ArrayList<>();
        if (source.children() != null) {
            for (VisibleAssetTree.Node child : source.children()) {
                children.add(toDTONode(child));
            }
        }
        return new SmartLightningMonitoringTreeDTO.TreeNode(
                source.id(), source.name(), source.entityType(), children);
    }

    /** 统一经平台授权服务读取实体，不能按租户直接查询，以保留共享实体的权限边界。 */
    private List<SmartLightningMonitoringTreeBuilder.VisibleEntity> findVisibleEntities(SecurityUser user, EntityType entityType)
            throws ThingsboardException {
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleEntities = new ArrayList<>();
        PageLink pageLink = new PageLink(TREE_ENTITY_PAGE_SIZE, 0);
        while (pageLink != null) {
            PageData<? extends HasName> pageData = userEntitiesService.findUserEntities(
                    user, entityType, Operation.READ, pageLink);
            for (HasName entity : pageData.getData()) {
                if (entity instanceof HasId<?> identifiedEntity && identifiedEntity.getId() != null) {
                    visibleEntities.add(new SmartLightningMonitoringTreeBuilder.VisibleEntity(
                            identifiedEntity.getId().getId(), entity.getName(), entityType));
                }
            }
            // 授权查询可能受平台最大分页限制，必须逐页读取，不能假设一次返回全部实体。
            pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
        }
        return visibleEntities;
    }
}
