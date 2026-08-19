/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.FormatTimeUnit;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartAxisSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class TimeSeriesChartXAxisSettings
extends TimeSeriesChartAxisSettings {
    private static final Map<FormatTimeUnit, String> defaultXAxisTicksFormat = new HashMap<FormatTimeUnit, String>();
    private Map<FormatTimeUnit, String> ticksFormat;

    public TimeSeriesChartXAxisSettings() {
    }

    public TimeSeriesChartXAxisSettings(TimeSeriesChartXAxisSettings input, boolean comparison) {
        super(input != null ? input : new TimeSeriesChartXAxisSettings());
        if (input == null) {
            input = new TimeSeriesChartXAxisSettings();
        }
        this.setPosition(input.getPosition() != null ? input.getPosition() : (comparison ? AxisPosition.top : AxisPosition.bottom));
        this.setTickLabelFont(input.getTickLabelFont() != null ? input.getTickLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(10.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build());
        if (input.getTicksFormat() == null) {
            this.ticksFormat = defaultXAxisTicksFormat;
        } else {
            this.ticksFormat = input.getTicksFormat();
            defaultXAxisTicksFormat.forEach((key, val) -> {
                if (!this.ticksFormat.containsKey(key)) {
                    this.ticksFormat.put((FormatTimeUnit)((Object)key), (String)val);
                }
            });
        }
    }

    @Generated
    public Map<FormatTimeUnit, String> getTicksFormat() {
        return this.ticksFormat;
    }

    @Generated
    public void setTicksFormat(Map<FormatTimeUnit, String> ticksFormat) {
        this.ticksFormat = ticksFormat;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartXAxisSettings)) {
            return false;
        }
        TimeSeriesChartXAxisSettings other = (TimeSeriesChartXAxisSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Map<FormatTimeUnit, String> this$ticksFormat = this.getTicksFormat();
        Map<FormatTimeUnit, String> other$ticksFormat = other.getTicksFormat();
        return !(this$ticksFormat == null ? other$ticksFormat != null : !((Object)this$ticksFormat).equals(other$ticksFormat));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartXAxisSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Map<FormatTimeUnit, String> $ticksFormat = this.getTicksFormat();
        result = result * 59 + ($ticksFormat == null ? 43 : ((Object)$ticksFormat).hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "TimeSeriesChartXAxisSettings(ticksFormat=" + String.valueOf(this.getTicksFormat()) + ")";
    }

    static {
        defaultXAxisTicksFormat.put(FormatTimeUnit.millisecond, "HH:mm:ss SSS");
        defaultXAxisTicksFormat.put(FormatTimeUnit.second, "HH:mm:ss");
        defaultXAxisTicksFormat.put(FormatTimeUnit.minute, "HH:mm");
        defaultXAxisTicksFormat.put(FormatTimeUnit.hour, "HH:mm");
        defaultXAxisTicksFormat.put(FormatTimeUnit.day, "MMM dd");
        defaultXAxisTicksFormat.put(FormatTimeUnit.month, "MMM");
        defaultXAxisTicksFormat.put(FormatTimeUnit.year, "yyyy");
    }
}

