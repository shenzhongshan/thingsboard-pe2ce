/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.annotation.JsonSubTypes
 *  com.fasterxml.jackson.annotation.JsonSubTypes$Type
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartWithLabelsSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportRangeChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartBarWidthSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartGridSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartStateSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartXAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;

@JsonSubTypes(value={@JsonSubTypes.Type(value=ReportBarChartWithLabelsSettings.class, name="barChartWithLabels"), @JsonSubTypes.Type(value=ReportRangeChartSettings.class, name="rangeChart")})
public class ReportTimeSeriesChartSettings {
    private Boolean showTitle;
    private String title;
    private Font titleFont;
    private String titleColor;
    private TextAlignment titleAlignment;
    @JsonProperty(value="thresholds")
    private List<TimeSeriesChartThreshold> thresholds;
    private Boolean stack;
    @JsonProperty(value="grid")
    private TimeSeriesChartGridSettings grid;
    @JsonProperty(value="yAxes")
    private Map<String, TimeSeriesChartYAxisSettings> yAxes;
    @JsonProperty(value="xAxis")
    private TimeSeriesChartXAxisSettings xAxis;
    @JsonProperty(value="barWidthSettings")
    private TimeSeriesChartBarWidthSettings barWidthSettings;
    @JsonProperty(value="noAggregationBarWidthSettings")
    private TimeSeriesChartNoAggregationBarWidthSettings noAggregationBarWidthSettings;
    @JsonProperty(value="states")
    private List<TimeSeriesChartStateSettings> states;
    private Boolean comparisonEnabled;
    private ComparisonDuration timeForComparison;
    private Long comparisonCustomIntervalValue;
    @JsonProperty(value="comparisonXAxis")
    private TimeSeriesChartXAxisSettings comparisonXAxis;
    private Boolean showLegend;
    private Font legendColumnTitleFont;
    private String legendColumnTitleColor;
    private Font legendLabelFont;
    private String legendLabelColor;
    private Font legendValueFont;
    private String legendValueColor;
    @JsonProperty(value="legendConfig")
    private LegendConfig legendConfig;

    public ReportTimeSeriesChartSettings() {
    }

    public ReportTimeSeriesChartSettings(ReportTimeSeriesChartSettings input) {
        if (input == null) {
            input = new ReportTimeSeriesChartSettings();
        }
        this.showTitle = input.getShowTitle() != null ? input.getShowTitle() : Boolean.TRUE;
        this.title = input.getTitle() != null ? input.getTitle() : "Time series chart";
        this.titleFont = input.getTitleFont() != null ? input.getTitleFont() : Font.builder().family("Roboto").size(Float.valueOf(18.0f)).weight(FontWeight.WEIGHT_500).style(FontStyle.NORMAL).build();
        this.titleColor = input.getTitleColor() != null ? input.getTitleColor() : "rgba(0, 0, 0, 0.87)";
        this.titleAlignment = input.getTitleAlignment() != null ? input.getTitleAlignment() : TextAlignment.CENTER;
        this.thresholds = input.getThresholds() != null ? input.getThresholds() : new ArrayList<TimeSeriesChartThreshold>();
        this.stack = input.getStack() != null ? input.getStack() : Boolean.FALSE;
        this.grid = new TimeSeriesChartGridSettings(input.getGrid());
        this.yAxes = new HashMap<String, TimeSeriesChartYAxisSettings>();
        if (input.getYAxes() == null) {
            this.yAxes.put("default", new TimeSeriesChartYAxisSettings(null));
        } else {
            input.getYAxes().forEach((key, value) -> {
                TimeSeriesChartYAxisSettings yAxisSettings = new TimeSeriesChartYAxisSettings((TimeSeriesChartYAxisSettings)value);
                this.yAxes.put((String)key, yAxisSettings);
            });
        }
        this.xAxis = new TimeSeriesChartXAxisSettings(input.getXAxis(), false);
        this.barWidthSettings = new TimeSeriesChartBarWidthSettings(input.getBarWidthSettings());
        this.noAggregationBarWidthSettings = new TimeSeriesChartNoAggregationBarWidthSettings(input.getNoAggregationBarWidthSettings());
        this.states = input.getStates() != null ? input.getStates() : new ArrayList<TimeSeriesChartStateSettings>();
        this.comparisonEnabled = input.getComparisonEnabled() != null ? input.getComparisonEnabled() : Boolean.FALSE;
        this.timeForComparison = input.getTimeForComparison() != null ? input.getTimeForComparison() : ComparisonDuration.previousInterval;
        this.comparisonCustomIntervalValue = input.getComparisonCustomIntervalValue() != null ? input.getComparisonCustomIntervalValue() : 0x6DDD00L;
        this.comparisonXAxis = new TimeSeriesChartXAxisSettings(input.getComparisonXAxis(), true);
        this.showLegend = input.getShowLegend() != null ? input.getShowLegend() : Boolean.TRUE;
        this.legendColumnTitleFont = input.getLegendColumnTitleFont() != null ? input.getLegendColumnTitleFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.legendColumnTitleColor = input.getLegendColumnTitleColor() != null ? input.getLegendColumnTitleColor() : "rgba(0, 0, 0, 0.38)";
        this.legendLabelFont = input.getLegendLabelFont() != null ? input.getLegendLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.legendLabelColor = input.getLegendLabelColor() != null ? input.getLegendLabelColor() : "rgba(0, 0, 0, 0.87)";
        this.legendValueFont = input.getLegendValueFont() != null ? input.getLegendValueFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.WEIGHT_500).style(FontStyle.NORMAL).build();
        this.legendValueColor = input.getLegendValueColor() != null ? input.getLegendValueColor() : "rgba(0, 0, 0, 0.87)";
        this.legendConfig = new LegendConfig(input.getLegendConfig());
    }

    @Generated
    public Boolean getShowTitle() {
        return this.showTitle;
    }

    @Generated
    public String getTitle() {
        return this.title;
    }

    @Generated
    public Font getTitleFont() {
        return this.titleFont;
    }

    @Generated
    public String getTitleColor() {
        return this.titleColor;
    }

    @Generated
    public TextAlignment getTitleAlignment() {
        return this.titleAlignment;
    }

    @Generated
    public List<TimeSeriesChartThreshold> getThresholds() {
        return this.thresholds;
    }

    @Generated
    public Boolean getStack() {
        return this.stack;
    }

    @Generated
    public TimeSeriesChartGridSettings getGrid() {
        return this.grid;
    }

    @Generated
    public Map<String, TimeSeriesChartYAxisSettings> getYAxes() {
        return this.yAxes;
    }

    @Generated
    public TimeSeriesChartXAxisSettings getXAxis() {
        return this.xAxis;
    }

    @Generated
    public TimeSeriesChartBarWidthSettings getBarWidthSettings() {
        return this.barWidthSettings;
    }

    @Generated
    public TimeSeriesChartNoAggregationBarWidthSettings getNoAggregationBarWidthSettings() {
        return this.noAggregationBarWidthSettings;
    }

    @Generated
    public List<TimeSeriesChartStateSettings> getStates() {
        return this.states;
    }

    @Generated
    public Boolean getComparisonEnabled() {
        return this.comparisonEnabled;
    }

    @Generated
    public ComparisonDuration getTimeForComparison() {
        return this.timeForComparison;
    }

    @Generated
    public Long getComparisonCustomIntervalValue() {
        return this.comparisonCustomIntervalValue;
    }

    @Generated
    public TimeSeriesChartXAxisSettings getComparisonXAxis() {
        return this.comparisonXAxis;
    }

    @Generated
    public Boolean getShowLegend() {
        return this.showLegend;
    }

    @Generated
    public Font getLegendColumnTitleFont() {
        return this.legendColumnTitleFont;
    }

    @Generated
    public String getLegendColumnTitleColor() {
        return this.legendColumnTitleColor;
    }

    @Generated
    public Font getLegendLabelFont() {
        return this.legendLabelFont;
    }

    @Generated
    public String getLegendLabelColor() {
        return this.legendLabelColor;
    }

    @Generated
    public Font getLegendValueFont() {
        return this.legendValueFont;
    }

    @Generated
    public String getLegendValueColor() {
        return this.legendValueColor;
    }

    @Generated
    public LegendConfig getLegendConfig() {
        return this.legendConfig;
    }

    @Generated
    public void setShowTitle(Boolean showTitle) {
        this.showTitle = showTitle;
    }

    @Generated
    public void setTitle(String title) {
        this.title = title;
    }

    @Generated
    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
    }

    @Generated
    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    @Generated
    public void setTitleAlignment(TextAlignment titleAlignment) {
        this.titleAlignment = titleAlignment;
    }

    @Generated
    public void setThresholds(List<TimeSeriesChartThreshold> thresholds) {
        this.thresholds = thresholds;
    }

    @Generated
    public void setStack(Boolean stack) {
        this.stack = stack;
    }

    @Generated
    public void setGrid(TimeSeriesChartGridSettings grid) {
        this.grid = grid;
    }

    @Generated
    public void setYAxes(Map<String, TimeSeriesChartYAxisSettings> yAxes) {
        this.yAxes = yAxes;
    }

    @Generated
    public void setXAxis(TimeSeriesChartXAxisSettings xAxis) {
        this.xAxis = xAxis;
    }

    @Generated
    public void setBarWidthSettings(TimeSeriesChartBarWidthSettings barWidthSettings) {
        this.barWidthSettings = barWidthSettings;
    }

    @Generated
    public void setNoAggregationBarWidthSettings(TimeSeriesChartNoAggregationBarWidthSettings noAggregationBarWidthSettings) {
        this.noAggregationBarWidthSettings = noAggregationBarWidthSettings;
    }

    @Generated
    public void setStates(List<TimeSeriesChartStateSettings> states) {
        this.states = states;
    }

    @Generated
    public void setComparisonEnabled(Boolean comparisonEnabled) {
        this.comparisonEnabled = comparisonEnabled;
    }

    @Generated
    public void setTimeForComparison(ComparisonDuration timeForComparison) {
        this.timeForComparison = timeForComparison;
    }

    @Generated
    public void setComparisonCustomIntervalValue(Long comparisonCustomIntervalValue) {
        this.comparisonCustomIntervalValue = comparisonCustomIntervalValue;
    }

    @Generated
    public void setComparisonXAxis(TimeSeriesChartXAxisSettings comparisonXAxis) {
        this.comparisonXAxis = comparisonXAxis;
    }

    @Generated
    public void setShowLegend(Boolean showLegend) {
        this.showLegend = showLegend;
    }

    @Generated
    public void setLegendColumnTitleFont(Font legendColumnTitleFont) {
        this.legendColumnTitleFont = legendColumnTitleFont;
    }

    @Generated
    public void setLegendColumnTitleColor(String legendColumnTitleColor) {
        this.legendColumnTitleColor = legendColumnTitleColor;
    }

    @Generated
    public void setLegendLabelFont(Font legendLabelFont) {
        this.legendLabelFont = legendLabelFont;
    }

    @Generated
    public void setLegendLabelColor(String legendLabelColor) {
        this.legendLabelColor = legendLabelColor;
    }

    @Generated
    public void setLegendValueFont(Font legendValueFont) {
        this.legendValueFont = legendValueFont;
    }

    @Generated
    public void setLegendValueColor(String legendValueColor) {
        this.legendValueColor = legendValueColor;
    }

    @Generated
    public void setLegendConfig(LegendConfig legendConfig) {
        this.legendConfig = legendConfig;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportTimeSeriesChartSettings)) {
            return false;
        }
        ReportTimeSeriesChartSettings other = (ReportTimeSeriesChartSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showTitle = this.getShowTitle();
        Boolean other$showTitle = other.getShowTitle();
        if (this$showTitle == null ? other$showTitle != null : !((Object)this$showTitle).equals(other$showTitle)) {
            return false;
        }
        Boolean this$stack = this.getStack();
        Boolean other$stack = other.getStack();
        if (this$stack == null ? other$stack != null : !((Object)this$stack).equals(other$stack)) {
            return false;
        }
        Boolean this$comparisonEnabled = this.getComparisonEnabled();
        Boolean other$comparisonEnabled = other.getComparisonEnabled();
        if (this$comparisonEnabled == null ? other$comparisonEnabled != null : !((Object)this$comparisonEnabled).equals(other$comparisonEnabled)) {
            return false;
        }
        Long this$comparisonCustomIntervalValue = this.getComparisonCustomIntervalValue();
        Long other$comparisonCustomIntervalValue = other.getComparisonCustomIntervalValue();
        if (this$comparisonCustomIntervalValue == null ? other$comparisonCustomIntervalValue != null : !((Object)this$comparisonCustomIntervalValue).equals(other$comparisonCustomIntervalValue)) {
            return false;
        }
        Boolean this$showLegend = this.getShowLegend();
        Boolean other$showLegend = other.getShowLegend();
        if (this$showLegend == null ? other$showLegend != null : !((Object)this$showLegend).equals(other$showLegend)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        Font this$titleFont = this.getTitleFont();
        Font other$titleFont = other.getTitleFont();
        if (this$titleFont == null ? other$titleFont != null : !((Object)this$titleFont).equals(other$titleFont)) {
            return false;
        }
        String this$titleColor = this.getTitleColor();
        String other$titleColor = other.getTitleColor();
        if (this$titleColor == null ? other$titleColor != null : !this$titleColor.equals(other$titleColor)) {
            return false;
        }
        TextAlignment this$titleAlignment = this.getTitleAlignment();
        TextAlignment other$titleAlignment = other.getTitleAlignment();
        if (this$titleAlignment == null ? other$titleAlignment != null : !((Object)((Object)this$titleAlignment)).equals((Object)other$titleAlignment)) {
            return false;
        }
        List<TimeSeriesChartThreshold> this$thresholds = this.getThresholds();
        List<TimeSeriesChartThreshold> other$thresholds = other.getThresholds();
        if (this$thresholds == null ? other$thresholds != null : !((Object)this$thresholds).equals(other$thresholds)) {
            return false;
        }
        TimeSeriesChartGridSettings this$grid = this.getGrid();
        TimeSeriesChartGridSettings other$grid = other.getGrid();
        if (this$grid == null ? other$grid != null : !((Object)this$grid).equals(other$grid)) {
            return false;
        }
        Map<String, TimeSeriesChartYAxisSettings> this$yAxes = this.getYAxes();
        Map<String, TimeSeriesChartYAxisSettings> other$yAxes = other.getYAxes();
        if (this$yAxes == null ? other$yAxes != null : !((Object)this$yAxes).equals(other$yAxes)) {
            return false;
        }
        TimeSeriesChartXAxisSettings this$xAxis = this.getXAxis();
        TimeSeriesChartXAxisSettings other$xAxis = other.getXAxis();
        if (this$xAxis == null ? other$xAxis != null : !((Object)this$xAxis).equals(other$xAxis)) {
            return false;
        }
        TimeSeriesChartBarWidthSettings this$barWidthSettings = this.getBarWidthSettings();
        TimeSeriesChartBarWidthSettings other$barWidthSettings = other.getBarWidthSettings();
        if (this$barWidthSettings == null ? other$barWidthSettings != null : !((Object)this$barWidthSettings).equals(other$barWidthSettings)) {
            return false;
        }
        TimeSeriesChartNoAggregationBarWidthSettings this$noAggregationBarWidthSettings = this.getNoAggregationBarWidthSettings();
        TimeSeriesChartNoAggregationBarWidthSettings other$noAggregationBarWidthSettings = other.getNoAggregationBarWidthSettings();
        if (this$noAggregationBarWidthSettings == null ? other$noAggregationBarWidthSettings != null : !((Object)this$noAggregationBarWidthSettings).equals(other$noAggregationBarWidthSettings)) {
            return false;
        }
        List<TimeSeriesChartStateSettings> this$states = this.getStates();
        List<TimeSeriesChartStateSettings> other$states = other.getStates();
        if (this$states == null ? other$states != null : !((Object)this$states).equals(other$states)) {
            return false;
        }
        ComparisonDuration this$timeForComparison = this.getTimeForComparison();
        ComparisonDuration other$timeForComparison = other.getTimeForComparison();
        if (this$timeForComparison == null ? other$timeForComparison != null : !((Object)((Object)this$timeForComparison)).equals((Object)other$timeForComparison)) {
            return false;
        }
        TimeSeriesChartXAxisSettings this$comparisonXAxis = this.getComparisonXAxis();
        TimeSeriesChartXAxisSettings other$comparisonXAxis = other.getComparisonXAxis();
        if (this$comparisonXAxis == null ? other$comparisonXAxis != null : !((Object)this$comparisonXAxis).equals(other$comparisonXAxis)) {
            return false;
        }
        Font this$legendColumnTitleFont = this.getLegendColumnTitleFont();
        Font other$legendColumnTitleFont = other.getLegendColumnTitleFont();
        if (this$legendColumnTitleFont == null ? other$legendColumnTitleFont != null : !((Object)this$legendColumnTitleFont).equals(other$legendColumnTitleFont)) {
            return false;
        }
        String this$legendColumnTitleColor = this.getLegendColumnTitleColor();
        String other$legendColumnTitleColor = other.getLegendColumnTitleColor();
        if (this$legendColumnTitleColor == null ? other$legendColumnTitleColor != null : !this$legendColumnTitleColor.equals(other$legendColumnTitleColor)) {
            return false;
        }
        Font this$legendLabelFont = this.getLegendLabelFont();
        Font other$legendLabelFont = other.getLegendLabelFont();
        if (this$legendLabelFont == null ? other$legendLabelFont != null : !((Object)this$legendLabelFont).equals(other$legendLabelFont)) {
            return false;
        }
        String this$legendLabelColor = this.getLegendLabelColor();
        String other$legendLabelColor = other.getLegendLabelColor();
        if (this$legendLabelColor == null ? other$legendLabelColor != null : !this$legendLabelColor.equals(other$legendLabelColor)) {
            return false;
        }
        Font this$legendValueFont = this.getLegendValueFont();
        Font other$legendValueFont = other.getLegendValueFont();
        if (this$legendValueFont == null ? other$legendValueFont != null : !((Object)this$legendValueFont).equals(other$legendValueFont)) {
            return false;
        }
        String this$legendValueColor = this.getLegendValueColor();
        String other$legendValueColor = other.getLegendValueColor();
        if (this$legendValueColor == null ? other$legendValueColor != null : !this$legendValueColor.equals(other$legendValueColor)) {
            return false;
        }
        LegendConfig this$legendConfig = this.getLegendConfig();
        LegendConfig other$legendConfig = other.getLegendConfig();
        return !(this$legendConfig == null ? other$legendConfig != null : !((Object)this$legendConfig).equals(other$legendConfig));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportTimeSeriesChartSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showTitle = this.getShowTitle();
        result = result * 59 + ($showTitle == null ? 43 : ((Object)$showTitle).hashCode());
        Boolean $stack = this.getStack();
        result = result * 59 + ($stack == null ? 43 : ((Object)$stack).hashCode());
        Boolean $comparisonEnabled = this.getComparisonEnabled();
        result = result * 59 + ($comparisonEnabled == null ? 43 : ((Object)$comparisonEnabled).hashCode());
        Long $comparisonCustomIntervalValue = this.getComparisonCustomIntervalValue();
        result = result * 59 + ($comparisonCustomIntervalValue == null ? 43 : ((Object)$comparisonCustomIntervalValue).hashCode());
        Boolean $showLegend = this.getShowLegend();
        result = result * 59 + ($showLegend == null ? 43 : ((Object)$showLegend).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        Font $titleFont = this.getTitleFont();
        result = result * 59 + ($titleFont == null ? 43 : ((Object)$titleFont).hashCode());
        String $titleColor = this.getTitleColor();
        result = result * 59 + ($titleColor == null ? 43 : $titleColor.hashCode());
        TextAlignment $titleAlignment = this.getTitleAlignment();
        result = result * 59 + ($titleAlignment == null ? 43 : ((Object)((Object)$titleAlignment)).hashCode());
        List<TimeSeriesChartThreshold> $thresholds = this.getThresholds();
        result = result * 59 + ($thresholds == null ? 43 : ((Object)$thresholds).hashCode());
        TimeSeriesChartGridSettings $grid = this.getGrid();
        result = result * 59 + ($grid == null ? 43 : ((Object)$grid).hashCode());
        Map<String, TimeSeriesChartYAxisSettings> $yAxes = this.getYAxes();
        result = result * 59 + ($yAxes == null ? 43 : ((Object)$yAxes).hashCode());
        TimeSeriesChartXAxisSettings $xAxis = this.getXAxis();
        result = result * 59 + ($xAxis == null ? 43 : ((Object)$xAxis).hashCode());
        TimeSeriesChartBarWidthSettings $barWidthSettings = this.getBarWidthSettings();
        result = result * 59 + ($barWidthSettings == null ? 43 : ((Object)$barWidthSettings).hashCode());
        TimeSeriesChartNoAggregationBarWidthSettings $noAggregationBarWidthSettings = this.getNoAggregationBarWidthSettings();
        result = result * 59 + ($noAggregationBarWidthSettings == null ? 43 : ((Object)$noAggregationBarWidthSettings).hashCode());
        List<TimeSeriesChartStateSettings> $states = this.getStates();
        result = result * 59 + ($states == null ? 43 : ((Object)$states).hashCode());
        ComparisonDuration $timeForComparison = this.getTimeForComparison();
        result = result * 59 + ($timeForComparison == null ? 43 : ((Object)((Object)$timeForComparison)).hashCode());
        TimeSeriesChartXAxisSettings $comparisonXAxis = this.getComparisonXAxis();
        result = result * 59 + ($comparisonXAxis == null ? 43 : ((Object)$comparisonXAxis).hashCode());
        Font $legendColumnTitleFont = this.getLegendColumnTitleFont();
        result = result * 59 + ($legendColumnTitleFont == null ? 43 : ((Object)$legendColumnTitleFont).hashCode());
        String $legendColumnTitleColor = this.getLegendColumnTitleColor();
        result = result * 59 + ($legendColumnTitleColor == null ? 43 : $legendColumnTitleColor.hashCode());
        Font $legendLabelFont = this.getLegendLabelFont();
        result = result * 59 + ($legendLabelFont == null ? 43 : ((Object)$legendLabelFont).hashCode());
        String $legendLabelColor = this.getLegendLabelColor();
        result = result * 59 + ($legendLabelColor == null ? 43 : $legendLabelColor.hashCode());
        Font $legendValueFont = this.getLegendValueFont();
        result = result * 59 + ($legendValueFont == null ? 43 : ((Object)$legendValueFont).hashCode());
        String $legendValueColor = this.getLegendValueColor();
        result = result * 59 + ($legendValueColor == null ? 43 : $legendValueColor.hashCode());
        LegendConfig $legendConfig = this.getLegendConfig();
        result = result * 59 + ($legendConfig == null ? 43 : ((Object)$legendConfig).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ReportTimeSeriesChartSettings(showTitle=" + this.getShowTitle() + ", title=" + this.getTitle() + ", titleFont=" + String.valueOf(this.getTitleFont()) + ", titleColor=" + this.getTitleColor() + ", titleAlignment=" + String.valueOf((Object)this.getTitleAlignment()) + ", thresholds=" + String.valueOf(this.getThresholds()) + ", stack=" + this.getStack() + ", grid=" + String.valueOf(this.getGrid()) + ", yAxes=" + String.valueOf(this.getYAxes()) + ", xAxis=" + String.valueOf(this.getXAxis()) + ", barWidthSettings=" + String.valueOf(this.getBarWidthSettings()) + ", noAggregationBarWidthSettings=" + String.valueOf(this.getNoAggregationBarWidthSettings()) + ", states=" + String.valueOf(this.getStates()) + ", comparisonEnabled=" + this.getComparisonEnabled() + ", timeForComparison=" + String.valueOf((Object)this.getTimeForComparison()) + ", comparisonCustomIntervalValue=" + this.getComparisonCustomIntervalValue() + ", comparisonXAxis=" + String.valueOf(this.getComparisonXAxis()) + ", showLegend=" + this.getShowLegend() + ", legendColumnTitleFont=" + String.valueOf(this.getLegendColumnTitleFont()) + ", legendColumnTitleColor=" + this.getLegendColumnTitleColor() + ", legendLabelFont=" + String.valueOf(this.getLegendLabelFont()) + ", legendLabelColor=" + this.getLegendLabelColor() + ", legendValueFont=" + String.valueOf(this.getLegendValueFont()) + ", legendValueColor=" + this.getLegendValueColor() + ", legendConfig=" + String.valueOf(this.getLegendConfig()) + ")";
    }
}

