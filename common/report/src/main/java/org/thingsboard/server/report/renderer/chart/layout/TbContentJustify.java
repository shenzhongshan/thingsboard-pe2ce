/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.report.renderer.chart.layout;

public class TbContentJustify {
    public static final TbContentJustify NONE = new TbContentJustify("HorizontalAlignment.NONE");
    public static final TbContentJustify SPACE_AROUND = new TbContentJustify("HorizontalAlignment.SPACE_AROUND");
    private String name;

    private TbContentJustify(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TbContentJustify)) {
            return false;
        }
        TbContentJustify that = (TbContentJustify)obj;
        return this.name.equals(that.name);
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}

