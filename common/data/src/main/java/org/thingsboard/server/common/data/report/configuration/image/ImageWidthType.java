/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonCreator
 *  com.fasterxml.jackson.annotation.JsonValue
 */
package org.thingsboard.server.common.data.report.configuration.image;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ImageWidthType {
    FIT_WIDTH("fitWidth"),
    ORIGINAL("original"),
    CUSTOM("custom");

    private final String value;

    private ImageWidthType(String label) {
        this.value = label;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static ImageWidthType fromLabel(String value) {
        for (ImageWidthType type : ImageWidthType.values()) {
            if (!type.value.equalsIgnoreCase(value)) continue;
            return type;
        }
        throw new IllegalArgumentException("Unknown ImageWidthType: " + value);
    }
}

