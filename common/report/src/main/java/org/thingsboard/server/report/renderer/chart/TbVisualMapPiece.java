/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.Color;
import java.awt.Paint;
import lombok.Generated;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbVisualMapPiece {
    private Double lt;
    private Double gte;
    private Double value;
    private String color;
    private Paint paint;
    private Paint fillPaint;

    public static TbVisualMapPiece fromRange(String color, Double from, Double to) {
        TbVisualMapPiece piece = new TbVisualMapPiece();
        piece.color = color;
        if (TbVisualMapPiece.isNumber(from) && TbVisualMapPiece.isNumber(to)) {
            if (from.compareTo(to) == 0) {
                piece.value = from;
            } else {
                piece.gte = from;
                piece.lt = to;
            }
        } else if (TbVisualMapPiece.isNumber(from)) {
            piece.gte = from;
        } else if (TbVisualMapPiece.isNumber(to)) {
            piece.lt = to;
        }
        return piece;
    }

    public void setupPaints(boolean fillArea, float fillAreaOpacity) {
        Color paint = ColorUtils.safeParseCssColor(this.color);
        this.paint = paint;
        this.fillPaint = ChartUtils.createFillPaint(fillArea, fillAreaOpacity, paint);
    }

    public boolean matchValue(double value) {
        if (TbVisualMapPiece.isNumber(this.gte) && TbVisualMapPiece.isNumber(this.lt)) {
            return value >= this.gte && value < this.lt;
        }
        if (TbVisualMapPiece.isNumber(this.gte)) {
            return value >= this.gte;
        }
        if (TbVisualMapPiece.isNumber(this.lt)) {
            return value < this.lt;
        }
        if (TbVisualMapPiece.isNumber(this.value)) {
            return this.value == value;
        }
        return false;
    }

    public boolean matchLower(double lower) {
        if (TbVisualMapPiece.isNumber(this.gte) && TbVisualMapPiece.isNumber(this.lt)) {
            return lower >= this.gte && lower < this.lt;
        }
        if (TbVisualMapPiece.isNumber(this.gte)) {
            return lower >= this.gte;
        }
        if (TbVisualMapPiece.isNumber(this.lt)) {
            return lower < this.lt;
        }
        return false;
    }

    public boolean greaterLower(double lower) {
        if (TbVisualMapPiece.isNumber(this.gte)) {
            return this.gte > lower;
        }
        if (TbVisualMapPiece.isNumber(this.lt)) {
            return lower < this.lt;
        }
        return false;
    }

    public double getUpper(double maxUpper) {
        if (TbVisualMapPiece.isNumber(this.lt)) {
            return Math.min(maxUpper, this.lt);
        }
        return maxUpper;
    }

    public double getNearestUpper(double maxUpper) {
        if (TbVisualMapPiece.isNumber(this.gte)) {
            return Math.min(maxUpper, this.gte);
        }
        if (TbVisualMapPiece.isNumber(this.lt)) {
            return Math.min(maxUpper, this.lt);
        }
        return maxUpper;
    }

    private static boolean isNumber(Double number) {
        return number != null && Double.isFinite(number);
    }

    @Generated
    public TbVisualMapPiece() {
    }

    @Generated
    public Double getLt() {
        return this.lt;
    }

    @Generated
    public Double getGte() {
        return this.gte;
    }

    @Generated
    public Double getValue() {
        return this.value;
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public Paint getPaint() {
        return this.paint;
    }

    @Generated
    public Paint getFillPaint() {
        return this.fillPaint;
    }

    @Generated
    public void setLt(Double lt) {
        this.lt = lt;
    }

    @Generated
    public void setGte(Double gte) {
        this.gte = gte;
    }

    @Generated
    public void setValue(Double value) {
        this.value = value;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    @Generated
    public void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbVisualMapPiece)) {
            return false;
        }
        TbVisualMapPiece other = (TbVisualMapPiece)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$lt = this.getLt();
        Double other$lt = other.getLt();
        if (this$lt == null ? other$lt != null : !((Object)this$lt).equals(other$lt)) {
            return false;
        }
        Double this$gte = this.getGte();
        Double other$gte = other.getGte();
        if (this$gte == null ? other$gte != null : !((Object)this$gte).equals(other$gte)) {
            return false;
        }
        Double this$value = this.getValue();
        Double other$value = other.getValue();
        if (this$value == null ? other$value != null : !((Object)this$value).equals(other$value)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
            return false;
        }
        Paint this$paint = this.getPaint();
        Paint other$paint = other.getPaint();
        if (this$paint == null ? other$paint != null : !this$paint.equals(other$paint)) {
            return false;
        }
        Paint this$fillPaint = this.getFillPaint();
        Paint other$fillPaint = other.getFillPaint();
        return !(this$fillPaint == null ? other$fillPaint != null : !this$fillPaint.equals(other$fillPaint));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbVisualMapPiece;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $lt = this.getLt();
        result = result * 59 + ($lt == null ? 43 : ((Object)$lt).hashCode());
        Double $gte = this.getGte();
        result = result * 59 + ($gte == null ? 43 : ((Object)$gte).hashCode());
        Double $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : ((Object)$value).hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        Paint $paint = this.getPaint();
        result = result * 59 + ($paint == null ? 43 : $paint.hashCode());
        Paint $fillPaint = this.getFillPaint();
        result = result * 59 + ($fillPaint == null ? 43 : $fillPaint.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbVisualMapPiece(lt=" + this.getLt() + ", gte=" + this.getGte() + ", value=" + this.getValue() + ", color=" + this.getColor() + ", paint=" + String.valueOf(this.getPaint()) + ", fillPaint=" + String.valueOf(this.getFillPaint()) + ")";
    }
}

