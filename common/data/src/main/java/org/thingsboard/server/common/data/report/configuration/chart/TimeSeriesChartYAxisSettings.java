/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartAxisSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class TimeSeriesChartYAxisSettings
extends TimeSeriesChartAxisSettings {
    private String id;
    private Integer order;
    private String units;
    private Integer decimals;
    private Double interval;
    private Integer splitNumber;
    private Double min;
    private Double max;

    public TimeSeriesChartYAxisSettings() {
    }

    public TimeSeriesChartYAxisSettings(TimeSeriesChartYAxisSettings input) {
        super(input != null ? input : new TimeSeriesChartYAxisSettings());
        if (input == null) {
            input = new TimeSeriesChartYAxisSettings();
        }
        this.setPosition(input.getPosition() != null ? input.getPosition() : AxisPosition.left);
        this.setTickLabelFont(input.getTickLabelFont() != null ? input.getTickLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build());
        this.id = input.getId() != null ? input.getId() : "default";
        this.order = input.getOrder() != null ? input.getOrder() : 0;
        this.units = input.getUnits();
        this.decimals = input.getDecimals() != null ? input.getDecimals() : 0;
        this.interval = input.getInterval();
        this.splitNumber = input.getSplitNumber();
        this.min = input.getMin();
        this.max = input.getMax();
    }

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public Integer getOrder() {
        return this.order;
    }

    @Generated
    public String getUnits() {
        return this.units;
    }

    @Generated
    public Integer getDecimals() {
        return this.decimals;
    }

    @Generated
    public Double getInterval() {
        return this.interval;
    }

    @Generated
    public Integer getSplitNumber() {
        return this.splitNumber;
    }

    @Generated
    public Double getMin() {
        return this.min;
    }

    @Generated
    public Double getMax() {
        return this.max;
    }

    @Generated
    public void setId(String id) {
        this.id = id;
    }

    @Generated
    public void setOrder(Integer order) {
        this.order = order;
    }

    @Generated
    public void setUnits(String units) {
        this.units = units;
    }

    @Generated
    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    @Generated
    public void setInterval(Double interval) {
        this.interval = interval;
    }

    @Generated
    public void setSplitNumber(Integer splitNumber) {
        this.splitNumber = splitNumber;
    }

    @Generated
    public void setMin(Double min) {
        this.min = min;
    }

    @Generated
    public void setMax(Double max) {
        this.max = max;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartYAxisSettings)) {
            return false;
        }
        TimeSeriesChartYAxisSettings other = (TimeSeriesChartYAxisSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$order = this.getOrder();
        Integer other$order = other.getOrder();
        if (this$order == null ? other$order != null : !((Object)this$order).equals(other$order)) {
            return false;
        }
        Integer this$decimals = this.getDecimals();
        Integer other$decimals = other.getDecimals();
        if (this$decimals == null ? other$decimals != null : !((Object)this$decimals).equals(other$decimals)) {
            return false;
        }
        Double this$interval = this.getInterval();
        Double other$interval = other.getInterval();
        if (this$interval == null ? other$interval != null : !((Object)this$interval).equals(other$interval)) {
            return false;
        }
        Integer this$splitNumber = this.getSplitNumber();
        Integer other$splitNumber = other.getSplitNumber();
        if (this$splitNumber == null ? other$splitNumber != null : !((Object)this$splitNumber).equals(other$splitNumber)) {
            return false;
        }
        Double this$min = this.getMin();
        Double other$min = other.getMin();
        if (this$min == null ? other$min != null : !((Object)this$min).equals(other$min)) {
            return false;
        }
        Double this$max = this.getMax();
        Double other$max = other.getMax();
        if (this$max == null ? other$max != null : !((Object)this$max).equals(other$max)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$units = this.getUnits();
        String other$units = other.getUnits();
        return !(this$units == null ? other$units != null : !this$units.equals(other$units));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartYAxisSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $order = this.getOrder();
        result = result * 59 + ($order == null ? 43 : ((Object)$order).hashCode());
        Integer $decimals = this.getDecimals();
        result = result * 59 + ($decimals == null ? 43 : ((Object)$decimals).hashCode());
        Double $interval = this.getInterval();
        result = result * 59 + ($interval == null ? 43 : ((Object)$interval).hashCode());
        Integer $splitNumber = this.getSplitNumber();
        result = result * 59 + ($splitNumber == null ? 43 : ((Object)$splitNumber).hashCode());
        Double $min = this.getMin();
        result = result * 59 + ($min == null ? 43 : ((Object)$min).hashCode());
        Double $max = this.getMax();
        result = result * 59 + ($max == null ? 43 : ((Object)$max).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $units = this.getUnits();
        result = result * 59 + ($units == null ? 43 : $units.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "TimeSeriesChartYAxisSettings(id=" + this.getId() + ", order=" + this.getOrder() + ", units=" + this.getUnits() + ", decimals=" + this.getDecimals() + ", interval=" + this.getInterval() + ", splitNumber=" + this.getSplitNumber() + ", min=" + this.getMin() + ", max=" + this.getMax() + ")";
    }
}

