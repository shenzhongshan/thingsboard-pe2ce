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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.service.query.VisibleAssetTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 将当前用户可见的资产、设备和关系转换为前端可直接渲染的监测树。
 *
 * <p>这里只接受控制器已经按权限筛选过的实体，并再次以该可见集合为边界过滤关系。
 * 因此即使关系另一端属于其他客户，也不会被放入返回结果。</p>
 */
public class SmartLightningMonitoringTreeBuilder {

    private static final String CONTAINS_RELATION = "Contains";

    /** 监测树所需的最小实体信息，避免把资产配置等无关字段返回给浏览器。 */
    public record VisibleEntity(UUID id, String name, EntityType entityType) {
    }

    /**
     * 构建资产树。只允许 {@code ASSET -> ASSET/DEVICE} 的 Contains 关系；
     * 没有挂载到资产的设备不会作为根节点显示。
     */
    public VisibleAssetTree build(Collection<VisibleEntity> visibleEntities,
                                  Collection<EntityRelation> relations) {
        // 先只创建可见资产和设备节点，后续关系无法把不可见实体引入结果。
        Map<UUID, NodeDraft> nodesById = new HashMap<>();
        for (VisibleEntity entity : visibleEntities) {
            if (entity.id() != null && (entity.entityType() == EntityType.ASSET || entity.entityType() == EntityType.DEVICE)) {
                nodesById.put(entity.id(), new NodeDraft(entity));
            }
        }

        Map<UUID, UUID> parentIdsByChildId = new HashMap<>();
        List<EntityRelation> sortedRelations = new ArrayList<>();
        for (EntityRelation relation : relations) {
            // 先过滤空关系端点，避免排序阶段解引用脏数据导致空指针。
            if (isSortableRelation(relation)) {
                sortedRelations.add(relation);
            }
        }
        // 多父级关系并不适用于树；排序保证始终选择同一条可用关系。
        sortedRelations.sort(Comparator.comparing((EntityRelation relation) -> relation.getFrom().getId().toString())
                .thenComparing(relation -> relation.getTo().getId().toString()));
        // 按稳定顺序逐条关联父子节点，过滤非法类型、不可见节点和会形成环的关系。
        for (EntityRelation relation : sortedRelations) {
            addTreeRelation(relation, nodesById, parentIdsByChildId);
        }

        // 只把没有父资产的资产作为根节点，并稳定排序每层子节点和根节点。
        List<NodeDraft> rootDrafts = new ArrayList<>();
        for (NodeDraft node : nodesById.values()) {
            node.children.sort(Comparator.comparing(child -> child.entity.name()));
            if (node.entity.entityType() == EntityType.ASSET && !parentIdsByChildId.containsKey(node.entity.id())) {
                rootDrafts.add(node);
            }
        }
        rootDrafts.sort(Comparator.comparing(node -> node.entity.name()));
        List<VisibleAssetTree.Node> roots = new ArrayList<>();
        // 将内部可变构建节点转换为对外不可变树节点。
        for (NodeDraft rootDraft : rootDrafts) {
            roots.add(toTreeNode(rootDraft, new HashSet<>()));
        }
        return new VisibleAssetTree(roots);
    }

    /** 判断关系是否具备排序所需的完整端点和 ID。 */
    private boolean isSortableRelation(EntityRelation relation) {
        return relation != null
                && relation.getFrom() != null
                && relation.getTo() != null
                && relation.getFrom().getId() != null
                && relation.getTo().getId() != null;
    }

    /** 校验 Contains 关系两端及父子约束，通过后才写入树的父子索引。 */
    private void addTreeRelation(EntityRelation relation, Map<UUID, NodeDraft> nodesById,
                                 Map<UUID, UUID> parentIdsByChildId) {
        if (!CONTAINS_RELATION.equals(relation.getType()) || relation.getFrom() == null || relation.getTo() == null ||
                relation.getFrom().getEntityType() != EntityType.ASSET ||
                (relation.getTo().getEntityType() != EntityType.ASSET && relation.getTo().getEntityType() != EntityType.DEVICE)) {
            return;
        }
        UUID parentId = relation.getFrom().getId();
        UUID childId = relation.getTo().getId();
        NodeDraft parent = nodesById.get(parentId);
        NodeDraft child = nodesById.get(childId);
        // 关系两端都必须是当前用户可见实体，且一个树节点只能有一个父级。
        if (parent == null || child == null || parentIdsByChildId.containsKey(childId) || parentId.equals(childId) ||
                wouldCreateCycle(parentId, childId, parentIdsByChildId)) {
            return;
        }
        parent.children.add(child);
        parentIdsByChildId.put(childId, parentId);
    }

    /** 沿父节点链回溯，判断新增关系是否会让子节点成为自身祖先。 */
    private boolean wouldCreateCycle(UUID parentId, UUID childId, Map<UUID, UUID> parentIdsByChildId) {
        UUID ancestorId = parentId;
        while (ancestorId != null) {
            if (ancestorId.equals(childId)) {
                return true;
            }
            ancestorId = parentIdsByChildId.get(ancestorId);
        }
        return false;
    }

    private VisibleAssetTree.Node toTreeNode(NodeDraft draft, Set<UUID> visitedIds) {
        // 数据库若存在历史环，仍以空子集降级，避免序列化时无限递归。
        if (!visitedIds.add(draft.entity.id())) {
            return new VisibleAssetTree.Node(
                    draft.entity.id(), draft.entity.name(), draft.entity.entityType(), List.of());
        }
        List<VisibleAssetTree.Node> children = new ArrayList<>();
        for (NodeDraft child : draft.children) {
            children.add(toTreeNode(child, new HashSet<>(visitedIds)));
        }
        return new VisibleAssetTree.Node(
                draft.entity.id(), draft.entity.name(), draft.entity.entityType(), children);
    }

    private static class NodeDraft {
        private final VisibleEntity entity;
        private final List<NodeDraft> children = new ArrayList<>();

        private NodeDraft(VisibleEntity entity) {
            this.entity = entity;
        }
    }
}
