package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.Arrays;
import lombok.Generated;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;

public class WhiteLabeling
implements Serializable {
    private static final long serialVersionUID = 2628323657987010348L;
    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName="type", max=50)
    private WhiteLabelingType type;
    @NoXss
    @Length(fieldName="settings", max=10000000)
    private transient JsonNode settings;
    private DomainId domainId;
    @JsonIgnore
    private byte[] settingsBytes;

    public JsonNode getSettings() {
        return BaseDataWithAdditionalInfo.getJson(() -> this.settings, () -> this.settingsBytes);
    }

    public void setSettings(JsonNode settings) {
        BaseDataWithAdditionalInfo.setJson(settings, json -> {
            this.settings = json;
        }, bytes -> {
            this.settingsBytes = bytes;
        });
    }

    @Generated
    public static WhiteLabelingBuilder builder() {
        return new WhiteLabelingBuilder();
    }

    @Generated
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @Generated
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    @Generated
    public WhiteLabelingType getType() {
        return this.type;
    }

    @Generated
    public DomainId getDomainId() {
        return this.domainId;
    }

    @Generated
    public byte[] getSettingsBytes() {
        return this.settingsBytes;
    }

    @Generated
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @Generated
    public void setType(WhiteLabelingType type) {
        this.type = type;
    }

    @Generated
    public void setDomainId(DomainId domainId) {
        this.domainId = domainId;
    }

    @Generated
    public void setSettingsBytes(byte[] settingsBytes) {
        this.settingsBytes = settingsBytes;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WhiteLabeling)) {
            return false;
        }
        WhiteLabeling other = (WhiteLabeling)o;
        if (!other.canEqual(this)) {
            return false;
        }
        TenantId this$tenantId = this.getTenantId();
        TenantId other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        CustomerId this$customerId = this.getCustomerId();
        CustomerId other$customerId = other.getCustomerId();
        if (this$customerId == null ? other$customerId != null : !((Object)this$customerId).equals(other$customerId)) {
            return false;
        }
        WhiteLabelingType this$type = this.getType();
        WhiteLabelingType other$type = other.getType();
        if (this$type == null ? other$type != null : !((Object)((Object)this$type)).equals((Object)other$type)) {
            return false;
        }
        DomainId this$domainId = this.getDomainId();
        DomainId other$domainId = other.getDomainId();
        if (this$domainId == null ? other$domainId != null : !((Object)this$domainId).equals(other$domainId)) {
            return false;
        }
        return Arrays.equals(this.getSettingsBytes(), other.getSettingsBytes());
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof WhiteLabeling;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        TenantId $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        CustomerId $customerId = this.getCustomerId();
        result = result * 59 + ($customerId == null ? 43 : ((Object)$customerId).hashCode());
        WhiteLabelingType $type = this.getType();
        result = result * 59 + ($type == null ? 43 : ((Object)((Object)$type)).hashCode());
        DomainId $domainId = this.getDomainId();
        result = result * 59 + ($domainId == null ? 43 : ((Object)$domainId).hashCode());
        result = result * 59 + Arrays.hashCode(this.getSettingsBytes());
        return result;
    }

    @Generated
    public String toString() {
        return "WhiteLabeling(tenantId=" + String.valueOf(this.getTenantId()) + ", customerId=" + String.valueOf(this.getCustomerId()) + ", type=" + String.valueOf((Object)this.getType()) + ", settings=" + String.valueOf(this.getSettings()) + ", domainId=" + String.valueOf(this.getDomainId()) + ", settingsBytes=" + Arrays.toString(this.getSettingsBytes()) + ")";
    }

    @ConstructorProperties(value={"tenantId", "customerId", "type", "settings", "domainId", "settingsBytes"})
    @Generated
    public WhiteLabeling(TenantId tenantId, CustomerId customerId, WhiteLabelingType type, JsonNode settings, DomainId domainId, byte[] settingsBytes) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.type = type;
        this.settings = settings;
        this.domainId = domainId;
        this.settingsBytes = settingsBytes;
    }

    @Generated
    public WhiteLabeling() {
    }

    @Generated
    public static class WhiteLabelingBuilder {
        @Generated
        private TenantId tenantId;
        @Generated
        private CustomerId customerId;
        @Generated
        private WhiteLabelingType type;
        @Generated
        private JsonNode settings;
        @Generated
        private DomainId domainId;
        @Generated
        private byte[] settingsBytes;

        @Generated
        WhiteLabelingBuilder() {
        }

        @Generated
        public WhiteLabelingBuilder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        @Generated
        public WhiteLabelingBuilder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        @Generated
        public WhiteLabelingBuilder type(WhiteLabelingType type) {
            this.type = type;
            return this;
        }

        @Generated
        public WhiteLabelingBuilder settings(JsonNode settings) {
            this.settings = settings;
            return this;
        }

        @Generated
        public WhiteLabelingBuilder domainId(DomainId domainId) {
            this.domainId = domainId;
            return this;
        }

        @JsonIgnore
        @Generated
        public WhiteLabelingBuilder settingsBytes(byte[] settingsBytes) {
            this.settingsBytes = settingsBytes;
            return this;
        }

        @Generated
        public WhiteLabeling build() {
            return new WhiteLabeling(this.tenantId, this.customerId, this.type, this.settings, this.domainId, this.settingsBytes);
        }

        @Generated
        public String toString() {
            return "WhiteLabeling.WhiteLabelingBuilder(tenantId=" + String.valueOf(this.tenantId) + ", customerId=" + String.valueOf(this.customerId) + ", type=" + String.valueOf((Object)this.type) + ", settings=" + String.valueOf(this.settings) + ", domainId=" + String.valueOf(this.domainId) + ", settingsBytes=" + Arrays.toString(this.settingsBytes) + ")";
        }
    }
}

