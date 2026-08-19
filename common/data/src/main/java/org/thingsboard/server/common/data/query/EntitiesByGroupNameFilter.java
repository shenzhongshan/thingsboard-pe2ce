/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.query;

import lombok.Generated;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;

public class EntitiesByGroupNameFilter
implements EntityFilter {
    private EntityType groupType;
    private EntityId ownerId;
    private String entityGroupNameFilter;
    private boolean groupStateEntity;
    private String stateEntityParamName;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITIES_BY_GROUP_NAME;
    }

    @Generated
    public EntitiesByGroupNameFilter() {
    }

    @Generated
    public EntityType getGroupType() {
        return this.groupType;
    }

    @Generated
    public EntityId getOwnerId() {
        return this.ownerId;
    }

    @Generated
    public String getEntityGroupNameFilter() {
        return this.entityGroupNameFilter;
    }

    @Generated
    public boolean isGroupStateEntity() {
        return this.groupStateEntity;
    }

    @Generated
    public String getStateEntityParamName() {
        return this.stateEntityParamName;
    }

    @Generated
    public void setGroupType(EntityType groupType) {
        this.groupType = groupType;
    }

    @Generated
    public void setOwnerId(EntityId ownerId) {
        this.ownerId = ownerId;
    }

    @Generated
    public void setEntityGroupNameFilter(String entityGroupNameFilter) {
        this.entityGroupNameFilter = entityGroupNameFilter;
    }

    @Generated
    public void setGroupStateEntity(boolean groupStateEntity) {
        this.groupStateEntity = groupStateEntity;
    }

    @Generated
    public void setStateEntityParamName(String stateEntityParamName) {
        this.stateEntityParamName = stateEntityParamName;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EntitiesByGroupNameFilter)) {
            return false;
        }
        EntitiesByGroupNameFilter other = (EntitiesByGroupNameFilter)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isGroupStateEntity() != other.isGroupStateEntity()) {
            return false;
        }
        EntityType this$groupType = this.getGroupType();
        EntityType other$groupType = other.getGroupType();
        if (this$groupType == null ? other$groupType != null : !((Object)((Object)this$groupType)).equals((Object)other$groupType)) {
            return false;
        }
        EntityId this$ownerId = this.getOwnerId();
        EntityId other$ownerId = other.getOwnerId();
        if (this$ownerId == null ? other$ownerId != null : !this$ownerId.equals(other$ownerId)) {
            return false;
        }
        String this$entityGroupNameFilter = this.getEntityGroupNameFilter();
        String other$entityGroupNameFilter = other.getEntityGroupNameFilter();
        if (this$entityGroupNameFilter == null ? other$entityGroupNameFilter != null : !this$entityGroupNameFilter.equals(other$entityGroupNameFilter)) {
            return false;
        }
        String this$stateEntityParamName = this.getStateEntityParamName();
        String other$stateEntityParamName = other.getStateEntityParamName();
        return !(this$stateEntityParamName == null ? other$stateEntityParamName != null : !this$stateEntityParamName.equals(other$stateEntityParamName));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof EntitiesByGroupNameFilter;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isGroupStateEntity() ? 79 : 97);
        EntityType $groupType = this.getGroupType();
        result = result * 59 + ($groupType == null ? 43 : ((Object)((Object)$groupType)).hashCode());
        EntityId $ownerId = this.getOwnerId();
        result = result * 59 + ($ownerId == null ? 43 : $ownerId.hashCode());
        String $entityGroupNameFilter = this.getEntityGroupNameFilter();
        result = result * 59 + ($entityGroupNameFilter == null ? 43 : $entityGroupNameFilter.hashCode());
        String $stateEntityParamName = this.getStateEntityParamName();
        result = result * 59 + ($stateEntityParamName == null ? 43 : $stateEntityParamName.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "EntitiesByGroupNameFilter(groupType=" + String.valueOf((Object)this.getGroupType()) + ", ownerId=" + String.valueOf(this.getOwnerId()) + ", entityGroupNameFilter=" + this.getEntityGroupNameFilter() + ", groupStateEntity=" + this.isGroupStateEntity() + ", stateEntityParamName=" + this.getStateEntityParamName() + ")";
    }
}

