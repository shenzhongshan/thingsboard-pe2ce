/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.query;

import lombok.Generated;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;

public class EntityGroupFilter
implements EntityFilter {
    private EntityType groupType;
    private String entityGroup;
    private boolean groupStateEntity;
    private EntityType defaultStateGroupType;
    private String defaultStateEntityGroup;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_GROUP;
    }

    @Generated
    public EntityGroupFilter() {
    }

    @Generated
    public EntityType getGroupType() {
        return this.groupType;
    }

    @Generated
    public String getEntityGroup() {
        return this.entityGroup;
    }

    @Generated
    public boolean isGroupStateEntity() {
        return this.groupStateEntity;
    }

    @Generated
    public EntityType getDefaultStateGroupType() {
        return this.defaultStateGroupType;
    }

    @Generated
    public String getDefaultStateEntityGroup() {
        return this.defaultStateEntityGroup;
    }

    @Generated
    public void setGroupType(EntityType groupType) {
        this.groupType = groupType;
    }

    @Generated
    public void setEntityGroup(String entityGroup) {
        this.entityGroup = entityGroup;
    }

    @Generated
    public void setGroupStateEntity(boolean groupStateEntity) {
        this.groupStateEntity = groupStateEntity;
    }

    @Generated
    public void setDefaultStateGroupType(EntityType defaultStateGroupType) {
        this.defaultStateGroupType = defaultStateGroupType;
    }

    @Generated
    public void setDefaultStateEntityGroup(String defaultStateEntityGroup) {
        this.defaultStateEntityGroup = defaultStateEntityGroup;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EntityGroupFilter)) {
            return false;
        }
        EntityGroupFilter other = (EntityGroupFilter)o;
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
        String this$entityGroup = this.getEntityGroup();
        String other$entityGroup = other.getEntityGroup();
        if (this$entityGroup == null ? other$entityGroup != null : !this$entityGroup.equals(other$entityGroup)) {
            return false;
        }
        EntityType this$defaultStateGroupType = this.getDefaultStateGroupType();
        EntityType other$defaultStateGroupType = other.getDefaultStateGroupType();
        if (this$defaultStateGroupType == null ? other$defaultStateGroupType != null : !((Object)((Object)this$defaultStateGroupType)).equals((Object)other$defaultStateGroupType)) {
            return false;
        }
        String this$defaultStateEntityGroup = this.getDefaultStateEntityGroup();
        String other$defaultStateEntityGroup = other.getDefaultStateEntityGroup();
        return !(this$defaultStateEntityGroup == null ? other$defaultStateEntityGroup != null : !this$defaultStateEntityGroup.equals(other$defaultStateEntityGroup));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof EntityGroupFilter;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isGroupStateEntity() ? 79 : 97);
        EntityType $groupType = this.getGroupType();
        result = result * 59 + ($groupType == null ? 43 : ((Object)((Object)$groupType)).hashCode());
        String $entityGroup = this.getEntityGroup();
        result = result * 59 + ($entityGroup == null ? 43 : $entityGroup.hashCode());
        EntityType $defaultStateGroupType = this.getDefaultStateGroupType();
        result = result * 59 + ($defaultStateGroupType == null ? 43 : ((Object)((Object)$defaultStateGroupType)).hashCode());
        String $defaultStateEntityGroup = this.getDefaultStateEntityGroup();
        result = result * 59 + ($defaultStateEntityGroup == null ? 43 : $defaultStateEntityGroup.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "EntityGroupFilter(groupType=" + String.valueOf((Object)this.getGroupType()) + ", entityGroup=" + this.getEntityGroup() + ", groupStateEntity=" + this.isGroupStateEntity() + ", defaultStateGroupType=" + String.valueOf((Object)this.getDefaultStateGroupType()) + ", defaultStateEntityGroup=" + this.getDefaultStateEntityGroup() + ")";
    }
}

