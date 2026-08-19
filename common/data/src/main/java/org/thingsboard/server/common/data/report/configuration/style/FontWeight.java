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
import java.util.List;

public enum FontWeight {
    NORMAL("normal"),
    BOLD("bold"),
    WEIGHT_500("500");

    private final String value;

    private FontWeight(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static FontWeight fromLabel(String value) {
        for (FontWeight type : FontWeight.values()) {
            if (!type.value.equalsIgnoreCase(value)) continue;
            return type;
        }
        if (List.of("lighter", "100", "200", "300", "400").contains(value)) {
            return NORMAL;
        }
        if (List.of("bolder", "600", "700", "800", "900").contains(value)) {
            return BOLD;
        }
        throw new IllegalArgumentException("Unknown FontWeight: " + value);
    }
}

