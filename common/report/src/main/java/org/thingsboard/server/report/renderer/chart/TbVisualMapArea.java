/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.beans.ConstructorProperties;
import lombok.Generated;

public class TbVisualMapArea {
    private final Paint paint;
    private final Paint fillPaint;
    private final Rectangle2D area;

    @Generated
    public Paint getPaint() {
        return this.paint;
    }

    @Generated
    public Paint getFillPaint() {
        return this.fillPaint;
    }

    @Generated
    public Rectangle2D getArea() {
        return this.area;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbVisualMapArea)) {
            return false;
        }
        TbVisualMapArea other = (TbVisualMapArea)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Paint this$paint = this.getPaint();
        Paint other$paint = other.getPaint();
        if (this$paint == null ? other$paint != null : !this$paint.equals(other$paint)) {
            return false;
        }
        Paint this$fillPaint = this.getFillPaint();
        Paint other$fillPaint = other.getFillPaint();
        if (this$fillPaint == null ? other$fillPaint != null : !this$fillPaint.equals(other$fillPaint)) {
            return false;
        }
        Rectangle2D this$area = this.getArea();
        Rectangle2D other$area = other.getArea();
        return !(this$area == null ? other$area != null : !((Object)this$area).equals(other$area));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbVisualMapArea;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Paint $paint = this.getPaint();
        result = result * 59 + ($paint == null ? 43 : $paint.hashCode());
        Paint $fillPaint = this.getFillPaint();
        result = result * 59 + ($fillPaint == null ? 43 : $fillPaint.hashCode());
        Rectangle2D $area = this.getArea();
        result = result * 59 + ($area == null ? 43 : ((Object)$area).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbVisualMapArea(paint=" + String.valueOf(this.getPaint()) + ", fillPaint=" + String.valueOf(this.getFillPaint()) + ", area=" + String.valueOf(this.getArea()) + ")";
    }

    @ConstructorProperties(value={"paint", "fillPaint", "area"})
    @Generated
    public TbVisualMapArea(Paint paint, Paint fillPaint, Rectangle2D area) {
        this.paint = paint;
        this.fillPaint = fillPaint;
        this.area = area;
    }
}

