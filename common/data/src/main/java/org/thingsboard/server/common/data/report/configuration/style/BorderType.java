/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonCreator
 *  com.fasterxml.jackson.annotation.JsonValue
 */
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BorderType {
    SOLID("solid"),
    DASHED("dashed"),
    DOTTED("dotted");

    private final String value;

    private BorderType(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static BorderType fromLabel(String value) {
        for (BorderType type : BorderType.values()) {
            if (!type.value.equalsIgnoreCase(value)) continue;
            return type;
        }
        throw new IllegalArgumentException("Unknown BorderType: " + value);
    }
}

