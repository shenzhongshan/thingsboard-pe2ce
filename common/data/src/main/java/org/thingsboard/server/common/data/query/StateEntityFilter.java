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

public class StateEntityFilter
implements EntityFilter {
    private AliasEntityId defaultStateEntity;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.STATE_ENTITY;
    }

    @Generated
    public StateEntityFilter() {
    }

    @Generated
    public AliasEntityId getDefaultStateEntity() {
        return this.defaultStateEntity;
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
        if (!(o instanceof StateEntityFilter)) {
            return false;
        }
        StateEntityFilter other = (StateEntityFilter)o;
        if (!other.canEqual(this)) {
            return false;
        }
        AliasEntityId this$defaultStateEntity = this.getDefaultStateEntity();
        AliasEntityId other$defaultStateEntity = other.getDefaultStateEntity();
        return !(this$defaultStateEntity == null ? other$defaultStateEntity != null : !this$defaultStateEntity.equals(other$defaultStateEntity));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof StateEntityFilter;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        AliasEntityId $defaultStateEntity = this.getDefaultStateEntity();
        result = result * 59 + ($defaultStateEntity == null ? 43 : $defaultStateEntity.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "StateEntityFilter(defaultStateEntity=" + String.valueOf(this.getDefaultStateEntity()) + ")";
    }
}

