package org.thingsboard.server.dao.wl;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import lombok.Generated;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;

public class WhiteLabelingCacheKey
implements Serializable {
    private WhiteLabelingCompositeKey key;
    private WhiteLabelingType type;
    private String domainName;

    public static WhiteLabelingCacheKey forKey(WhiteLabelingCompositeKey key) {
        return new WhiteLabelingCacheKey(key, null, null);
    }

    public static WhiteLabelingCacheKey forTypeAndDomain(WhiteLabelingType type, String domainName) {
        return new WhiteLabelingCacheKey(null, type, domainName);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WhiteLabelingCacheKey{");
        if (this.domainName != null && this.type != null) {
            builder.append("domainName=").append(this.domainName);
            builder.append(",type=").append(this.type);
        } else {
            if (this.key.getTenantId() != null) {
                builder.append("tenantId=").append(this.key.getTenantId());
            }
            if (this.key.getCustomerId() != null) {
                builder.append(",customerId=").append(this.key.getCustomerId());
            }
            if (this.key.getType() != null) {
                builder.append(",type=").append(this.key.getType());
            }
        }
        builder.append("}");
        return builder.toString();
    }

    @Generated
    public WhiteLabelingCompositeKey getKey() {
        return this.key;
    }

    @Generated
    public WhiteLabelingType getType() {
        return this.type;
    }

    @Generated
    public String getDomainName() {
        return this.domainName;
    }

    @Generated
    public void setKey(WhiteLabelingCompositeKey key) {
        this.key = key;
    }

    @Generated
    public void setType(WhiteLabelingType type) {
        this.type = type;
    }

    @Generated
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WhiteLabelingCacheKey)) {
            return false;
        }
        WhiteLabelingCacheKey other = (WhiteLabelingCacheKey)o;
        if (!other.canEqual(this)) {
            return false;
        }
        WhiteLabelingCompositeKey this$key = this.getKey();
        WhiteLabelingCompositeKey other$key = other.getKey();
        if (this$key == null ? other$key != null : !((Object)this$key).equals(other$key)) {
            return false;
        }
        WhiteLabelingType this$type = this.getType();
        WhiteLabelingType other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$domainName = this.getDomainName();
        String other$domainName = other.getDomainName();
        return !(this$domainName == null ? other$domainName != null : !this$domainName.equals(other$domainName));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof WhiteLabelingCacheKey;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        WhiteLabelingCompositeKey $key = this.getKey();
        result = result * 59 + ($key == null ? 43 : ((Object)$key).hashCode());
        WhiteLabelingType $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $domainName = this.getDomainName();
        result = result * 59 + ($domainName == null ? 43 : $domainName.hashCode());
        return result;
    }

    @ConstructorProperties(value={"key", "type", "domainName"})
    @Generated
    public WhiteLabelingCacheKey(WhiteLabelingCompositeKey key, WhiteLabelingType type, String domainName) {
        this.key = key;
        this.type = type;
        this.domainName = domainName;
    }

    @Generated
    public WhiteLabelingCacheKey() {
    }
}

