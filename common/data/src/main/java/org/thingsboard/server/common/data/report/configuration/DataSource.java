/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.beans.ConstructorProperties;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;

@Schema
@JsonIgnoreProperties(ignoreUnknown=true)
public class DataSource {
    private DataSourceType type;
    private String deviceId;
    private String entityAliasId;
    private String filterId;
    private List<DataKey> dataKeys;
    private List<DataKey> latestDataKeys;
    private AlarmFilterConfig alarmFilterConfig;

    @Generated
    public static DataSourceBuilder builder() {
        return new DataSourceBuilder();
    }

    @Generated
    public DataSourceType getType() {
        return this.type;
    }

    @Generated
    public String getDeviceId() {
        return this.deviceId;
    }

    @Generated
    public String getEntityAliasId() {
        return this.entityAliasId;
    }

    @Generated
    public String getFilterId() {
        return this.filterId;
    }

    @Generated
    public List<DataKey> getDataKeys() {
        return this.dataKeys;
    }

    @Generated
    public List<DataKey> getLatestDataKeys() {
        return this.latestDataKeys;
    }

    @Generated
    public AlarmFilterConfig getAlarmFilterConfig() {
        return this.alarmFilterConfig;
    }

    @Generated
    public void setType(DataSourceType type) {
        this.type = type;
    }

    @Generated
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Generated
    public void setEntityAliasId(String entityAliasId) {
        this.entityAliasId = entityAliasId;
    }

    @Generated
    public void setFilterId(String filterId) {
        this.filterId = filterId;
    }

    @Generated
    public void setDataKeys(List<DataKey> dataKeys) {
        this.dataKeys = dataKeys;
    }

    @Generated
    public void setLatestDataKeys(List<DataKey> latestDataKeys) {
        this.latestDataKeys = latestDataKeys;
    }

    @Generated
    public void setAlarmFilterConfig(AlarmFilterConfig alarmFilterConfig) {
        this.alarmFilterConfig = alarmFilterConfig;
    }

    @Generated
    public String toString() {
        return "DataSource(type=" + String.valueOf((Object)this.getType()) + ", deviceId=" + this.getDeviceId() + ", entityAliasId=" + this.getEntityAliasId() + ", filterId=" + this.getFilterId() + ", dataKeys=" + String.valueOf(this.getDataKeys()) + ", latestDataKeys=" + String.valueOf(this.getLatestDataKeys()) + ", alarmFilterConfig=" + String.valueOf(this.getAlarmFilterConfig()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DataSource)) {
            return false;
        }
        DataSource other = (DataSource)o;
        if (!other.canEqual(this)) {
            return false;
        }
        DataSourceType this$type = this.getType();
        DataSourceType other$type = other.getType();
        if (this$type == null ? other$type != null : !((Object)((Object)this$type)).equals((Object)other$type)) {
            return false;
        }
        String this$deviceId = this.getDeviceId();
        String other$deviceId = other.getDeviceId();
        if (this$deviceId == null ? other$deviceId != null : !this$deviceId.equals(other$deviceId)) {
            return false;
        }
        String this$entityAliasId = this.getEntityAliasId();
        String other$entityAliasId = other.getEntityAliasId();
        if (this$entityAliasId == null ? other$entityAliasId != null : !this$entityAliasId.equals(other$entityAliasId)) {
            return false;
        }
        String this$filterId = this.getFilterId();
        String other$filterId = other.getFilterId();
        if (this$filterId == null ? other$filterId != null : !this$filterId.equals(other$filterId)) {
            return false;
        }
        List<DataKey> this$dataKeys = this.getDataKeys();
        List<DataKey> other$dataKeys = other.getDataKeys();
        if (this$dataKeys == null ? other$dataKeys != null : !((Object)this$dataKeys).equals(other$dataKeys)) {
            return false;
        }
        List<DataKey> this$latestDataKeys = this.getLatestDataKeys();
        List<DataKey> other$latestDataKeys = other.getLatestDataKeys();
        if (this$latestDataKeys == null ? other$latestDataKeys != null : !((Object)this$latestDataKeys).equals(other$latestDataKeys)) {
            return false;
        }
        AlarmFilterConfig this$alarmFilterConfig = this.getAlarmFilterConfig();
        AlarmFilterConfig other$alarmFilterConfig = other.getAlarmFilterConfig();
        return !(this$alarmFilterConfig == null ? other$alarmFilterConfig != null : !((Object)this$alarmFilterConfig).equals(other$alarmFilterConfig));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DataSource;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        DataSourceType $type = this.getType();
        result = result * 59 + ($type == null ? 43 : ((Object)((Object)$type)).hashCode());
        String $deviceId = this.getDeviceId();
        result = result * 59 + ($deviceId == null ? 43 : $deviceId.hashCode());
        String $entityAliasId = this.getEntityAliasId();
        result = result * 59 + ($entityAliasId == null ? 43 : $entityAliasId.hashCode());
        String $filterId = this.getFilterId();
        result = result * 59 + ($filterId == null ? 43 : $filterId.hashCode());
        List<DataKey> $dataKeys = this.getDataKeys();
        result = result * 59 + ($dataKeys == null ? 43 : ((Object)$dataKeys).hashCode());
        List<DataKey> $latestDataKeys = this.getLatestDataKeys();
        result = result * 59 + ($latestDataKeys == null ? 43 : ((Object)$latestDataKeys).hashCode());
        AlarmFilterConfig $alarmFilterConfig = this.getAlarmFilterConfig();
        result = result * 59 + ($alarmFilterConfig == null ? 43 : ((Object)$alarmFilterConfig).hashCode());
        return result;
    }

    @Generated
    public DataSource() {
    }

    @ConstructorProperties(value={"type", "deviceId", "entityAliasId", "filterId", "dataKeys", "latestDataKeys", "alarmFilterConfig"})
    @Generated
    public DataSource(DataSourceType type, String deviceId, String entityAliasId, String filterId, List<DataKey> dataKeys, List<DataKey> latestDataKeys, AlarmFilterConfig alarmFilterConfig) {
        this.type = type;
        this.deviceId = deviceId;
        this.entityAliasId = entityAliasId;
        this.filterId = filterId;
        this.dataKeys = dataKeys;
        this.latestDataKeys = latestDataKeys;
        this.alarmFilterConfig = alarmFilterConfig;
    }

    @Generated
    public static class DataSourceBuilder {
        @Generated
        private DataSourceType type;
        @Generated
        private String deviceId;
        @Generated
        private String entityAliasId;
        @Generated
        private String filterId;
        @Generated
        private List<DataKey> dataKeys;
        @Generated
        private List<DataKey> latestDataKeys;
        @Generated
        private AlarmFilterConfig alarmFilterConfig;

        @Generated
        DataSourceBuilder() {
        }

        @Generated
        public DataSourceBuilder type(DataSourceType type) {
            this.type = type;
            return this;
        }

        @Generated
        public DataSourceBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Generated
        public DataSourceBuilder entityAliasId(String entityAliasId) {
            this.entityAliasId = entityAliasId;
            return this;
        }

        @Generated
        public DataSourceBuilder filterId(String filterId) {
            this.filterId = filterId;
            return this;
        }

        @Generated
        public DataSourceBuilder dataKeys(List<DataKey> dataKeys) {
            this.dataKeys = dataKeys;
            return this;
        }

        @Generated
        public DataSourceBuilder latestDataKeys(List<DataKey> latestDataKeys) {
            this.latestDataKeys = latestDataKeys;
            return this;
        }

        @Generated
        public DataSourceBuilder alarmFilterConfig(AlarmFilterConfig alarmFilterConfig) {
            this.alarmFilterConfig = alarmFilterConfig;
            return this;
        }

        @Generated
        public DataSource build() {
            return new DataSource(this.type, this.deviceId, this.entityAliasId, this.filterId, this.dataKeys, this.latestDataKeys, this.alarmFilterConfig);
        }

        @Generated
        public String toString() {
            return "DataSource.DataSourceBuilder(type=" + String.valueOf((Object)this.type) + ", deviceId=" + this.deviceId + ", entityAliasId=" + this.entityAliasId + ", filterId=" + this.filterId + ", dataKeys=" + String.valueOf(this.dataKeys) + ", latestDataKeys=" + String.valueOf(this.latestDataKeys) + ", alarmFilterConfig=" + String.valueOf(this.alarmFilterConfig) + ")";
        }
    }
}

