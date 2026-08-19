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

public class SchedulerEventFilter
implements EntityFilter {
    private AliasEntityId originator;
    private String eventType;
    private boolean originatorStateEntity;
    private AliasEntityId defaultStateEntity;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.SCHEDULER_EVENT;
    }

    @Generated
    public SchedulerEventFilter() {
    }

    @Generated
    public AliasEntityId getOriginator() {
        return this.originator;
    }

    @Generated
    public String getEventType() {
        return this.eventType;
    }

    @Generated
    public boolean isOriginatorStateEntity() {
        return this.originatorStateEntity;
    }

    @Generated
    public AliasEntityId getDefaultStateEntity() {
        return this.defaultStateEntity;
    }

    @Generated
    public void setOriginator(AliasEntityId originator) {
        this.originator = originator;
    }

    @Generated
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Generated
    public void setOriginatorStateEntity(boolean originatorStateEntity) {
        this.originatorStateEntity = originatorStateEntity;
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
        if (!(o instanceof SchedulerEventFilter)) {
            return false;
        }
        SchedulerEventFilter other = (SchedulerEventFilter)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isOriginatorStateEntity() != other.isOriginatorStateEntity()) {
            return false;
        }
        AliasEntityId this$originator = this.getOriginator();
        AliasEntityId other$originator = other.getOriginator();
        if (this$originator == null ? other$originator != null : !this$originator.equals(other$originator)) {
            return false;
        }
        String this$eventType = this.getEventType();
        String other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) {
            return false;
        }
        AliasEntityId this$defaultStateEntity = this.getDefaultStateEntity();
        AliasEntityId other$defaultStateEntity = other.getDefaultStateEntity();
        return !(this$defaultStateEntity == null ? other$defaultStateEntity != null : !this$defaultStateEntity.equals(other$defaultStateEntity));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SchedulerEventFilter;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isOriginatorStateEntity() ? 79 : 97);
        AliasEntityId $originator = this.getOriginator();
        result = result * 59 + ($originator == null ? 43 : $originator.hashCode());
        String $eventType = this.getEventType();
        result = result * 59 + ($eventType == null ? 43 : $eventType.hashCode());
        AliasEntityId $defaultStateEntity = this.getDefaultStateEntity();
        result = result * 59 + ($defaultStateEntity == null ? 43 : $defaultStateEntity.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SchedulerEventFilter(originator=" + String.valueOf(this.getOriginator()) + ", eventType=" + this.getEventType() + ", originatorStateEntity=" + this.isOriginatorStateEntity() + ", defaultStateEntity=" + String.valueOf(this.getDefaultStateEntity()) + ")";
    }
}

