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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseEntityGroupService implements EntityGroupService {

    private final EntityGroupRepository entityGroupRepository;
    private final RelationService relationService;
    private final DeviceService deviceService;
    private final AssetService assetService;
    private final CustomerService customerService;
    private final UserService userService;

    @Override
    public EntityGroup saveEntityGroup(TenantId tenantId, EntityGroup entityGroup) {
        if (StringUtils.isBlank(entityGroup.getName())) {
            throw new DataValidationException("Entity group name should be specified!");
        }
        if (entityGroup.getType() == null || !Arrays.asList(EntityGroup.groupTypes).contains(entityGroup.getType())) {
            throw new DataValidationException("Unsupported entity group type!");
        }
        if (entityGroup.getId() != null) {
            EntityGroup existing = findEntityGroupById(tenantId, entityGroup.getId());
            if (existing == null) {
                throw new DataValidationException("Entity group not found!");
            }
            if (existing.isGroupAll() && !EntityGroup.GROUP_ALL_NAME.equals(entityGroup.getName())) {
                throw new DataValidationException("Renaming of the 'All' entity group is prohibited!");
            }
        } else if (EntityGroup.GROUP_ALL_NAME.equals(entityGroup.getName())
                && entityGroupRepository.findByTenantIdAndTypeAndName(tenantId.getId(), entityGroup.getType(), EntityGroup.GROUP_ALL_NAME).isPresent()) {
            throw new DataValidationException("Entity group 'All' already exists!");
        }
        entityGroup.setTenantId(tenantId);
        if (entityGroup.getId() == null) {
            entityGroup.setCreatedTime(System.currentTimeMillis());
        }
        try {
            return entityGroupRepository.save(new EntityGroupEntity(entityGroup)).toData();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("group_name_per_owner_unq_key")
                    || e.getCause() != null && String.valueOf(e.getCause().getMessage()).contains("group_name_per_owner_unq_key")) {
                throw new DataValidationException("Entity group with such name already exists!");
            }
            throw e;
        }
    }

    @Override
    public EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId) {
        return entityGroupRepository.findById(entityGroupId.getId()).map(EntityGroupEntity::toData).orElse(null);
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityType groupType, PageLink pageLink) {
        ensureAllGroup(tenantId, groupType);
        return DaoUtil.toPageData(entityGroupRepository.findByTenantIdAndType(
                tenantId.getId(), groupType, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    private void ensureAllGroup(TenantId tenantId, EntityType groupType) {
        if (entityGroupRepository.findByTenantIdAndTypeAndName(tenantId.getId(), groupType, EntityGroup.GROUP_ALL_NAME).isEmpty()) {
            EntityGroup all = new EntityGroup();
            all.setTenantId(tenantId);
            all.setType(groupType);
            all.setName(EntityGroup.GROUP_ALL_NAME);
            all.setCreatedTime(System.currentTimeMillis());
            try {
                entityGroupRepository.save(new EntityGroupEntity(all));
            } catch (Exception e) {
                log.debug("[{}][{}] Concurrent 'All' group creation, ignoring", tenantId, groupType, e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            return;
        }
        if (entityGroup.isGroupAll()) {
            throw new DataValidationException("Deletion of the 'All' entity group is prohibited!");
        }
        relationService.deleteEntityRelations(tenantId, entityGroupId);
        entityGroupRepository.deleteById(entityGroupId.getId());
    }

    @Override
    public void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        EntityGroup entityGroup = getExistingNonAll(tenantId, entityGroupId, "add entities to");
        for (EntityId entityId : entityIds) {
            if (entityId.getEntityType() != entityGroup.getType()) {
                throw new DataValidationException("Entity type does not match the entity group type!");
            }
            relationService.saveRelation(tenantId,
                    new EntityRelation(entityGroupId, entityId, GROUP_RELATION_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP));
        }
    }

    @Override
    public void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        getExistingNonAll(tenantId, entityGroupId, "remove entities from");
        for (EntityId entityId : entityIds) {
            relationService.deleteRelation(tenantId, entityGroupId, entityId, GROUP_RELATION_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
        }
    }

    private EntityGroup getExistingNonAll(TenantId tenantId, EntityGroupId entityGroupId, String action) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        if (entityGroup.isGroupAll()) {
            throw new DataValidationException("Can't " + action + " the 'All' entity group — it always contains all entities!");
        }
        return entityGroup;
    }

    @Override
    public PageData<? extends HasName> findGroupEntities(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        if (entityGroup.isGroupAll()) {
            return findAllTenantEntities(tenantId, entityGroup.getType(), pageLink);
        }
        String textSearch = pageLink.getTextSearch() != null ? pageLink.getTextSearch().toLowerCase() : null;
        List<HasName> members = relationService.findByFrom(tenantId, entityGroupId, RelationTypeGroup.FROM_ENTITY_GROUP).stream()
                .map(EntityRelation::getTo)
                .map(to -> loadGroupEntity(tenantId, to))
                .filter(Objects::nonNull)  // deleted entities leave stale relations; drop them from the view
                .filter(e -> textSearch == null || e.getName().toLowerCase().contains(textSearch))
                .sorted(Comparator.comparing(HasName::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        // Group sizes are modest — paginate in memory.
        int total = members.size();
        int from = Math.min(pageLink.getPage() * pageLink.getPageSize(), total);
        int to = Math.min(from + pageLink.getPageSize(), total);
        int totalPages = (int) Math.ceil((double) total / pageLink.getPageSize());
        return new PageData<>(members.subList(from, to), totalPages, total, to < total);
    }

    private PageData<? extends HasName> findAllTenantEntities(TenantId tenantId, EntityType type, PageLink pageLink) {
        return switch (type) {
            case DEVICE -> deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
            case ASSET -> assetService.findAssetInfosByTenantId(tenantId, pageLink);
            case CUSTOMER -> customerService.findCustomersByTenantId(tenantId, pageLink);
            case USER -> userService.findUsersByTenantId(tenantId, pageLink);
            default -> throw new DataValidationException("Unsupported entity group type!");
        };
    }

    private HasName loadGroupEntity(TenantId tenantId, EntityId entityId) {
        try {
            return switch (entityId.getEntityType()) {
                case DEVICE -> deviceService.findDeviceInfoById(tenantId, new DeviceId(entityId.getId()));
                case ASSET -> assetService.findAssetInfoById(tenantId, new AssetId(entityId.getId()));
                case CUSTOMER -> customerService.findCustomerById(tenantId, new CustomerId(entityId.getId()));
                case USER -> userService.findUserById(tenantId, new UserId(entityId.getId()));
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public EntityGroup makePublic(TenantId tenantId, EntityGroupId entityGroupId, boolean isPublic) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        ObjectNode additionalInfo = entityGroup.getAdditionalInfo() != null && entityGroup.getAdditionalInfo().isObject()
                ? (ObjectNode) entityGroup.getAdditionalInfo() : JacksonUtil.newObjectNode();
        additionalInfo.put("isPublic", isPublic);
        entityGroup.setAdditionalInfo(additionalInfo);
        return saveEntityGroup(tenantId, entityGroup);
    }

}
