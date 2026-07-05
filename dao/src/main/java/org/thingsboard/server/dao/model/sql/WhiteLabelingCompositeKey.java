package org.thingsboard.server.dao.model.sql;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.UUID;
import lombok.Generated;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;

public class WhiteLabelingCompositeKey
implements Serializable {
    private UUID tenantId;
    private UUID customerId;
    private WhiteLabelingType type;

    public WhiteLabelingCompositeKey(TenantId tenantId, WhiteLabelingType type) {
        this(tenantId, null, type);
    }

    public WhiteLabelingCompositeKey(TenantId tenantId, CustomerId customerId, WhiteLabelingType type) {
        this.tenantId = tenantId.getId();
        this.customerId = customerId != null ? customerId.getId() : EntityId.NULL_UUID;
        this.type = type;
    }

    @Generated
    public UUID getTenantId() {
        return this.tenantId;
    }

    @Generated
    public UUID getCustomerId() {
        return this.customerId;
    }

    @Generated
    public WhiteLabelingType getType() {
        return this.type;
    }

    @Generated
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    @Generated
    public void setType(WhiteLabelingType type) {
        this.type = type;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WhiteLabelingCompositeKey)) {
            return false;
        }
        WhiteLabelingCompositeKey other = (WhiteLabelingCompositeKey)o;
        if (!other.canEqual(this)) {
            return false;
        }
        UUID this$tenantId = this.getTenantId();
        UUID other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        UUID this$customerId = this.getCustomerId();
        UUID other$customerId = other.getCustomerId();
        if (this$customerId == null ? other$customerId != null : !((Object)this$customerId).equals(other$customerId)) {
            return false;
        }
        WhiteLabelingType this$type = this.getType();
        WhiteLabelingType other$type = other.getType();
        return !(this$type == null ? other$type != null : !this$type.equals(other$type));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof WhiteLabelingCompositeKey;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        UUID $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        UUID $customerId = this.getCustomerId();
        result = result * 59 + ($customerId == null ? 43 : ((Object)$customerId).hashCode());
        WhiteLabelingType $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "WhiteLabelingCompositeKey(tenantId=" + String.valueOf(this.getTenantId()) + ", customerId=" + String.valueOf(this.getCustomerId()) + ", type=" + String.valueOf(this.getType()) + ")";
    }

    @ConstructorProperties(value={"tenantId", "customerId", "type"})
    @Generated
    public WhiteLabelingCompositeKey(UUID tenantId, UUID customerId, WhiteLabelingType type) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.type = type;
    }

    @Generated
    public WhiteLabelingCompositeKey() {
    }
}

