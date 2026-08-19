/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.jetbrains.annotations.NotNull
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartFillType
 *  org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.LineSeriesStepType
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType
 */
package org.thingsboard.server.report.renderer.chart;

import java.util.Objects;
import lombok.Generated;
import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesStepType;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;

public class TbDatasetKey
implements Comparable<TbDatasetKey> {
    private String yAxisId;
    private boolean comparison;
    private TimeSeriesChartSeriesType seriesType;
    private boolean stepLine;
    private LineSeriesStepType stepType;
    private boolean smoothLine;
    private boolean fillArea;
    private int datasetIndex;

    public TbDatasetKey(TimeSeriesChartKeySettings keySettings, boolean comparison) {
        this.comparison = comparison;
        this.yAxisId = keySettings.getYAxisId();
        this.seriesType = keySettings.getSeriesType();
        if (this.seriesType == TimeSeriesChartSeriesType.line) {
            LineSeriesSettings lineSettings = keySettings.getLineSettings();
            this.stepLine = lineSettings.getStep();
            if (this.stepLine) {
                this.stepType = lineSettings.getStepType();
            }
            this.smoothLine = lineSettings.getSmooth();
            this.fillArea = lineSettings.getFillAreaSettings().getType() != ChartFillType.none;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TbDatasetKey)) {
            return false;
        }
        TbDatasetKey that = (TbDatasetKey)o;
        if (this.comparison != that.comparison) {
            return false;
        }
        if (!Objects.equals(this.yAxisId, that.yAxisId)) {
            return false;
        }
        if (this.seriesType == that.seriesType) {
            if (this.seriesType == TimeSeriesChartSeriesType.bar) {
                return true;
            }
            return this.stepLine == that.stepLine && this.stepType == that.stepType && this.smoothLine == that.smoothLine && this.fillArea == that.fillArea;
        }
        return false;
    }

    public int hashCode() {
        if (this.seriesType == TimeSeriesChartSeriesType.bar) {
            return Objects.hash(this.comparison, this.yAxisId, this.seriesType);
        }
        return Objects.hash(this.comparison, this.yAxisId, this.stepLine, this.stepType, this.smoothLine, this.fillArea);
    }

    @Override
    public int compareTo(@NotNull TbDatasetKey tbDatasetKey) {
        int result = 0;
        if (this.comparison && !tbDatasetKey.comparison) {
            result = 1;
        } else if (!this.comparison && tbDatasetKey.comparison) {
            result = -1;
        }
        if (result == 0) {
            if (this.seriesType == tbDatasetKey.seriesType) {
                if (this.seriesType != TimeSeriesChartSeriesType.bar) {
                    if (this.fillArea && !tbDatasetKey.fillArea) {
                        result = -1;
                    } else if (!this.fillArea && tbDatasetKey.fillArea) {
                        result = 1;
                    }
                }
            } else {
                result = this.seriesType == TimeSeriesChartSeriesType.bar ? -1 : 1;
            }
        }
        if (result == 0 && !Objects.equals(this.yAxisId, tbDatasetKey.yAxisId)) {
            result = this.yAxisId.equals("default") ? -1 : 1;
        }
        return result;
    }

    @Generated
    public String getYAxisId() {
        return this.yAxisId;
    }

    @Generated
    public boolean isComparison() {
        return this.comparison;
    }

    @Generated
    public TimeSeriesChartSeriesType getSeriesType() {
        return this.seriesType;
    }

    @Generated
    public boolean isStepLine() {
        return this.stepLine;
    }

    @Generated
    public LineSeriesStepType getStepType() {
        return this.stepType;
    }

    @Generated
    public boolean isSmoothLine() {
        return this.smoothLine;
    }

    @Generated
    public boolean isFillArea() {
        return this.fillArea;
    }

    @Generated
    public int getDatasetIndex() {
        return this.datasetIndex;
    }

    @Generated
    public void setYAxisId(String yAxisId) {
        this.yAxisId = yAxisId;
    }

    @Generated
    public void setComparison(boolean comparison) {
        this.comparison = comparison;
    }

    @Generated
    public void setSeriesType(TimeSeriesChartSeriesType seriesType) {
        this.seriesType = seriesType;
    }

    @Generated
    public void setStepLine(boolean stepLine) {
        this.stepLine = stepLine;
    }

    @Generated
    public void setStepType(LineSeriesStepType stepType) {
        this.stepType = stepType;
    }

    @Generated
    public void setSmoothLine(boolean smoothLine) {
        this.smoothLine = smoothLine;
    }

    @Generated
    public void setFillArea(boolean fillArea) {
        this.fillArea = fillArea;
    }

    @Generated
    public void setDatasetIndex(int datasetIndex) {
        this.datasetIndex = datasetIndex;
    }

    @Generated
    public String toString() {
        return "TbDatasetKey(yAxisId=" + this.getYAxisId() + ", comparison=" + this.isComparison() + ", seriesType=" + String.valueOf(this.getSeriesType()) + ", stepLine=" + this.isStepLine() + ", stepType=" + String.valueOf(this.getStepType()) + ", smoothLine=" + this.isSmoothLine() + ", fillArea=" + this.isFillArea() + ", datasetIndex=" + this.getDatasetIndex() + ")";
    }
}

