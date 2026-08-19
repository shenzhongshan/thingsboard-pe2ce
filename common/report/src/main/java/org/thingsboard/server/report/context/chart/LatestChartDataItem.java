/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 */
package org.thingsboard.server.report.context.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.report.context.chart.LatestChartDataSource;

public class LatestChartDataItem {
    private LatestChartDataSource dataSource;
    private DataKey dataKey;
    private String label;
    private long ts;
    private double value;
    private boolean hasValue;
    private int index;
    private int keyIndex;
    private int seriesIndex;

    public String toString() {
        return "LatestChartDataItem{dataKey=" + String.valueOf(this.dataKey) + ", index=" + this.index + ", label=" + this.label + ", ts=" + this.ts + ", value=" + this.value + ", hasValue=" + this.hasValue + ", keyIndex=" + this.keyIndex + ", seriesIndex=" + this.seriesIndex + "}";
    }

    @Generated
    public LatestChartDataItem() {
    }

    @Generated
    public LatestChartDataSource getDataSource() {
        return this.dataSource;
    }

    @Generated
    public DataKey getDataKey() {
        return this.dataKey;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public long getTs() {
        return this.ts;
    }

    @Generated
    public double getValue() {
        return this.value;
    }

    @Generated
    public boolean isHasValue() {
        return this.hasValue;
    }

    @Generated
    public int getIndex() {
        return this.index;
    }

    @Generated
    public int getKeyIndex() {
        return this.keyIndex;
    }

    @Generated
    public int getSeriesIndex() {
        return this.seriesIndex;
    }

    @Generated
    public void setDataSource(LatestChartDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Generated
    public void setDataKey(DataKey dataKey) {
        this.dataKey = dataKey;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setTs(long ts) {
        this.ts = ts;
    }

    @Generated
    public void setValue(double value) {
        this.value = value;
    }

    @Generated
    public void setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
    }

    @Generated
    public void setIndex(int index) {
        this.index = index;
    }

    @Generated
    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }

    @Generated
    public void setSeriesIndex(int seriesIndex) {
        this.seriesIndex = seriesIndex;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LatestChartDataItem)) {
            return false;
        }
        LatestChartDataItem other = (LatestChartDataItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getTs() != other.getTs()) {
            return false;
        }
        if (Double.compare(this.getValue(), other.getValue()) != 0) {
            return false;
        }
        if (this.isHasValue() != other.isHasValue()) {
            return false;
        }
        if (this.getIndex() != other.getIndex()) {
            return false;
        }
        if (this.getKeyIndex() != other.getKeyIndex()) {
            return false;
        }
        if (this.getSeriesIndex() != other.getSeriesIndex()) {
            return false;
        }
        LatestChartDataSource this$dataSource = this.getDataSource();
        LatestChartDataSource other$dataSource = other.getDataSource();
        if (this$dataSource == null ? other$dataSource != null : !((Object)this$dataSource).equals(other$dataSource)) {
            return false;
        }
        DataKey this$dataKey = this.getDataKey();
        DataKey other$dataKey = other.getDataKey();
        if (this$dataKey == null ? other$dataKey != null : !this$dataKey.equals(other$dataKey)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        return !(this$label == null ? other$label != null : !this$label.equals(other$label));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LatestChartDataItem;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $ts = this.getTs();
        result = result * 59 + (int)($ts >>> 32 ^ $ts);
        long $value = Double.doubleToLongBits(this.getValue());
        result = result * 59 + (int)($value >>> 32 ^ $value);
        result = result * 59 + (this.isHasValue() ? 79 : 97);
        result = result * 59 + this.getIndex();
        result = result * 59 + this.getKeyIndex();
        result = result * 59 + this.getSeriesIndex();
        LatestChartDataSource $dataSource = this.getDataSource();
        result = result * 59 + ($dataSource == null ? 43 : ((Object)$dataSource).hashCode());
        DataKey $dataKey = this.getDataKey();
        result = result * 59 + ($dataKey == null ? 43 : $dataKey.hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        return result;
    }
}

