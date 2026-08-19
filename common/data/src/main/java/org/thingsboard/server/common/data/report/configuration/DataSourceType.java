/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonCreator
 *  com.fasterxml.jackson.annotation.JsonValue
 */
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataSourceType {
    DEVICE("device"),
    ENTITY("entity"),
    ENTITY_COUNT("entityCount"),
    ALARM_COUNT("alarmCount");

    private final String label;

    private DataSourceType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return this.label;
    }

    @JsonCreator
    public static DataSourceType fromLabel(String value) {
        for (DataSourceType type : DataSourceType.values()) {
            if (!type.label.equalsIgnoreCase(value)) continue;
            return type;
        }
        throw new IllegalArgumentException("Unknown DataSourceType: " + value);
    }
}

