/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonCreator
 *  com.fasterxml.jackson.annotation.JsonValue
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportComponentSubType {
    DOUGHNUT_CHART("doughnutChart"),
    HORIZONTAL_DOUGHNUT_CHART("horizontalDoughnutChart"),
    POINT_CHART("pointChart"),
    BAR_CHART("barChart"),
    PIE_CHART("pieChart"),
    LINE_CHART("lineChart"),
    LATEST_BAR_CHART("latestBarChart"),
    RANGE_CHART("rangeChart"),
    BAR_CHART_WITH_LABELS("barChartWithLabels"),
    STATE_CHART("stateChart"),
    DEFAULT("default");

    private final String name;

    private ReportComponentSubType(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    @JsonCreator
    public static ReportComponentSubType fromLabel(String value) {
        for (ReportComponentSubType layout : ReportComponentSubType.values()) {
            if (!layout.name.equalsIgnoreCase(value)) continue;
            return layout;
        }
        throw new IllegalArgumentException("Unknown ReportComponentSubType: " + value);
    }
}

