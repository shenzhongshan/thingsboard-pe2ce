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

public enum DoughnutLayout {
    DEFAULT("default"),
    WITH_TOTAL("with_total");

    private final String label;

    private DoughnutLayout(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return this.label;
    }

    @JsonCreator
    public static DoughnutLayout fromLabel(String value) {
        for (DoughnutLayout layout : DoughnutLayout.values()) {
            if (!layout.label.equalsIgnoreCase(value)) continue;
            return layout;
        }
        throw new IllegalArgumentException("Unknown DoughnutLayout: " + value);
    }
}

