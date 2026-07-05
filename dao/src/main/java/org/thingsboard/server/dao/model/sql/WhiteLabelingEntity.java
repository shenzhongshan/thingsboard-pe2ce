package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.Generated;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

@Entity
@Table(name="white_labeling")
@IdClass(value=WhiteLabelingCompositeKey.class)
public class WhiteLabelingEntity
implements ToData<WhiteLabeling>,
Serializable {
    @Id
    @Column(name="tenant_id", columnDefinition="uuid")
    private UUID tenantId;
    @Id
    @Column(name="customer_id", columnDefinition="uuid")
    private UUID customerId;
    @Id
    @Enumerated(value=EnumType.STRING)
    @Column(name="type")
    private WhiteLabelingType type;
    @Convert(converter=JsonConverter.class)
    @Column(name="settings")
    private JsonNode settings;
    @Column(name="domain_id")
    private UUID domainId;

    public WhiteLabelingEntity(WhiteLabeling whiteLabeling) {
        this.tenantId = whiteLabeling.getTenantId().getId();
        this.customerId = whiteLabeling.getCustomerId() != null ? whiteLabeling.getCustomerId().getId() : EntityId.NULL_UUID;
        this.type = whiteLabeling.getType();
        if (whiteLabeling.getSettings() != null) {
            this.settings = whiteLabeling.getSettings();
        }
        if (whiteLabeling.getDomainId() != null) {
            this.domainId = whiteLabeling.getDomainId().getId();
        }
    }

    @Override
    public WhiteLabeling toData() {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(TenantId.fromUUID((UUID)this.tenantId));
        if (!EntityId.NULL_UUID.equals(this.customerId)) {
            whiteLabeling.setCustomerId(new CustomerId(this.customerId));
        }
        whiteLabeling.setType(this.type);
        if (this.settings != null) {
            whiteLabeling.setSettings(this.settings);
        }
        if (this.domainId != null) {
            whiteLabeling.setDomainId(new DomainId(this.domainId));
        }
        return whiteLabeling;
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
    public JsonNode getSettings() {
        return this.settings;
    }

    @Generated
    public UUID getDomainId() {
        return this.domainId;
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
    public void setSettings(JsonNode settings) {
        this.settings = settings;
    }

    @Generated
    public void setDomainId(UUID domainId) {
        this.domainId = domainId;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WhiteLabelingEntity)) {
            return false;
        }
        WhiteLabelingEntity other = (WhiteLabelingEntity)o;
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
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        JsonNode this$settings = this.getSettings();
        JsonNode other$settings = other.getSettings();
        if (this$settings == null ? other$settings != null : !this$settings.equals(other$settings)) {
            return false;
        }
        UUID this$domainId = this.getDomainId();
        UUID other$domainId = other.getDomainId();
        return !(this$domainId == null ? other$domainId != null : !((Object)this$domainId).equals(other$domainId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof WhiteLabelingEntity;
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
        JsonNode $settings = this.getSettings();
        result = result * 59 + ($settings == null ? 43 : $settings.hashCode());
        UUID $domainId = this.getDomainId();
        result = result * 59 + ($domainId == null ? 43 : ((Object)$domainId).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "WhiteLabelingEntity(tenantId=" + String.valueOf(this.getTenantId()) + ", customerId=" + String.valueOf(this.getCustomerId()) + ", type=" + String.valueOf(this.getType()) + ", settings=" + String.valueOf(this.getSettings()) + ", domainId=" + String.valueOf(this.getDomainId()) + ")";
    }

    @Generated
    public WhiteLabelingEntity() {
    }
}

