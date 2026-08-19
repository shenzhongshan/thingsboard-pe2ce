/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Generated;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceType;

public abstract class ValueSourceConfig {
    private ValueSourceType type;
    private Double value;
    private String latestKeyType;
    private String latestKey;
    private String entityKeyType;
    private String entityAlias;
    private String entityKey;

    @JsonIgnore
    public boolean isValidSource() {
        if (this.type == null) {
            return false;
        }
        switch (this.type) {
            case constant: {
                return this.value != null;
            }
            case latestKey: {
                return ("attribute".equals(this.latestKeyType) || "timeseries".equals(this.latestKeyType)) && StringUtils.isNotBlank(this.latestKey);
            }
            case entity: {
                return ("attribute".equals(this.entityKeyType) || "timeseries".equals(this.entityKeyType)) && StringUtils.isNotBlank(this.entityAlias) && StringUtils.isNotBlank(this.entityKey);
            }
        }
        return false;
    }

    @JsonIgnore
    public DataKey toEntityDataKey() {
        DataKey key = new DataKey();
        key.setName(this.entityKey);
        key.setType(this.entityKeyType);
        return key;
    }

    @Generated
    public ValueSourceConfig() {
    }

    @Generated
    public ValueSourceType getType() {
        return this.type;
    }

    @Generated
    public Double getValue() {
        return this.value;
    }

    @Generated
    public String getLatestKeyType() {
        return this.latestKeyType;
    }

    @Generated
    public String getLatestKey() {
        return this.latestKey;
    }

    @Generated
    public String getEntityKeyType() {
        return this.entityKeyType;
    }

    @Generated
    public String getEntityAlias() {
        return this.entityAlias;
    }

    @Generated
    public String getEntityKey() {
        return this.entityKey;
    }

    @Generated
    public void setType(ValueSourceType type) {
        this.type = type;
    }

    @Generated
    public void setValue(Double value) {
        this.value = value;
    }

    @Generated
    public void setLatestKeyType(String latestKeyType) {
        this.latestKeyType = latestKeyType;
    }

    @Generated
    public void setLatestKey(String latestKey) {
        this.latestKey = latestKey;
    }

    @Generated
    public void setEntityKeyType(String entityKeyType) {
        this.entityKeyType = entityKeyType;
    }

    @Generated
    public void setEntityAlias(String entityAlias) {
        this.entityAlias = entityAlias;
    }

    @Generated
    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ValueSourceConfig)) {
            return false;
        }
        ValueSourceConfig other = (ValueSourceConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$value = this.getValue();
        Double other$value = other.getValue();
        if (this$value == null ? other$value != null : !((Object)this$value).equals(other$value)) {
            return false;
        }
        ValueSourceType this$type = this.getType();
        ValueSourceType other$type = other.getType();
        if (this$type == null ? other$type != null : !((Object)((Object)this$type)).equals((Object)other$type)) {
            return false;
        }
        String this$latestKeyType = this.getLatestKeyType();
        String other$latestKeyType = other.getLatestKeyType();
        if (this$latestKeyType == null ? other$latestKeyType != null : !this$latestKeyType.equals(other$latestKeyType)) {
            return false;
        }
        String this$latestKey = this.getLatestKey();
        String other$latestKey = other.getLatestKey();
        if (this$latestKey == null ? other$latestKey != null : !this$latestKey.equals(other$latestKey)) {
            return false;
        }
        String this$entityKeyType = this.getEntityKeyType();
        String other$entityKeyType = other.getEntityKeyType();
        if (this$entityKeyType == null ? other$entityKeyType != null : !this$entityKeyType.equals(other$entityKeyType)) {
            return false;
        }
        String this$entityAlias = this.getEntityAlias();
        String other$entityAlias = other.getEntityAlias();
        if (this$entityAlias == null ? other$entityAlias != null : !this$entityAlias.equals(other$entityAlias)) {
            return false;
        }
        String this$entityKey = this.getEntityKey();
        String other$entityKey = other.getEntityKey();
        return !(this$entityKey == null ? other$entityKey != null : !this$entityKey.equals(other$entityKey));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ValueSourceConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : ((Object)$value).hashCode());
        ValueSourceType $type = this.getType();
        result = result * 59 + ($type == null ? 43 : ((Object)((Object)$type)).hashCode());
        String $latestKeyType = this.getLatestKeyType();
        result = result * 59 + ($latestKeyType == null ? 43 : $latestKeyType.hashCode());
        String $latestKey = this.getLatestKey();
        result = result * 59 + ($latestKey == null ? 43 : $latestKey.hashCode());
        String $entityKeyType = this.getEntityKeyType();
        result = result * 59 + ($entityKeyType == null ? 43 : $entityKeyType.hashCode());
        String $entityAlias = this.getEntityAlias();
        result = result * 59 + ($entityAlias == null ? 43 : $entityAlias.hashCode());
        String $entityKey = this.getEntityKey();
        result = result * 59 + ($entityKey == null ? 43 : $entityKey.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ValueSourceConfig(type=" + String.valueOf((Object)this.getType()) + ", value=" + this.getValue() + ", latestKeyType=" + this.getLatestKeyType() + ", latestKey=" + this.getLatestKey() + ", entityKeyType=" + this.getEntityKeyType() + ", entityAlias=" + this.getEntityAlias() + ", entityKey=" + this.getEntityKey() + ")";
    }
}

