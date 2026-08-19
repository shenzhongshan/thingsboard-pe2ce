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

public enum TextAlignment {
    CENTER("center"),
    RIGHT("right"),
    LEFT("left"),
    JUSTIFY("justify");

    private final String value;

    private TextAlignment(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static TextAlignment fromLabel(String value) {
        for (TextAlignment type : TextAlignment.values()) {
            if (!type.value.equalsIgnoreCase(value)) continue;
            return type;
        }
        throw new IllegalArgumentException("Unknown TextAlignment: " + value);
    }
}

