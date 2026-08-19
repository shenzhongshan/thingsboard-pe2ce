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

public enum FontStyle {
    NORMAL("normal"),
    ITALIC("italic");

    private final String value;

    private FontStyle(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static FontStyle fromLabel(String value) {
        for (FontStyle type : FontStyle.values()) {
            if (!type.value.equalsIgnoreCase(value)) continue;
            return type;
        }
        throw new IllegalArgumentException("Unknown FontStyle: " + value);
    }
}

