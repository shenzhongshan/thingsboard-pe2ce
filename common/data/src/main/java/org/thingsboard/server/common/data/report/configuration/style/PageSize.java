/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.style;

import lombok.Generated;

public enum PageSize {
    A4(595, 842),
    LETTER(612, 792),
    LEGAL(612, 1008),
    A5(420, 595),
    A3(842, 1191),
    TABLOID(792, 1224);

    private final int width;
    private final int height;

    private PageSize(int pageWidth, int pageHeight) {
        this.width = pageWidth;
        this.height = pageHeight;
    }

    @Generated
    public int getWidth() {
        return this.width;
    }

    @Generated
    public int getHeight() {
        return this.height;
    }
}

