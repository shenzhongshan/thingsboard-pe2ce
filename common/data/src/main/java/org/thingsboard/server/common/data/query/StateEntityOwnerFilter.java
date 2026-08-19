/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.query;

import lombok.Generated;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;

public class StateEntityOwnerFilter
implements EntityFilter {
    private AliasEntityId singleEntity;
    private AliasEntityId defaultStateEntity;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.STATE_ENTITY_OWNER;
    }

    @Generated
    public StateEntityOwnerFilter() {
    }

    @Generated
    public AliasEntityId getSingleEntity() {
        return this.singleEntity;
    }

    @Generated
    public AliasEntityId getDefaultStateEntity() {
        return this.defaultStateEntity;
    }

    @Generated
    public void setSingleEntity(AliasEntityId singleEntity) {
        this.singleEntity = singleEntity;
    }

    @Generated
    public void setDefaultStateEntity(AliasEntityId defaultStateEntity) {
        this.defaultStateEntity = defaultStateEntity;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StateEntityOwnerFilter)) {
            return false;
        }
        StateEntityOwnerFilter other = (StateEntityOwnerFilter)o;
        if (!other.canEqual(this)) {
            return false;
        }
        AliasEntityId this$singleEntity = this.getSingleEntity();
        AliasEntityId other$singleEntity = other.getSingleEntity();
        if (this$singleEntity == null ? other$singleEntity != null : !this$singleEntity.equals(other$singleEntity)) {
            return false;
        }
        AliasEntityId this$defaultStateEntity = this.getDefaultStateEntity();
        AliasEntityId other$defaultStateEntity = other.getDefaultStateEntity();
        return !(this$defaultStateEntity == null ? other$defaultStateEntity != null : !this$defaultStateEntity.equals(other$defaultStateEntity));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof StateEntityOwnerFilter;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        AliasEntityId $singleEntity = this.getSingleEntity();
        result = result * 59 + ($singleEntity == null ? 43 : $singleEntity.hashCode());
        AliasEntityId $defaultStateEntity = this.getDefaultStateEntity();
        result = result * 59 + ($defaultStateEntity == null ? 43 : $defaultStateEntity.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "StateEntityOwnerFilter(singleEntity=" + String.valueOf(this.getSingleEntity()) + ", defaultStateEntity=" + String.valueOf(this.getDefaultStateEntity()) + ")";
    }
}

