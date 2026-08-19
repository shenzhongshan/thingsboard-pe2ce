/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.data.Range
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Generated;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.renderer.chart.TbVisualMapArea;
import org.thingsboard.server.report.renderer.chart.TbVisualMapPiece;
import org.thingsboard.server.report.util.ColorUtils;

public class TbVisualMap {
    private final Paint outOfRangePaint;
    private final Paint outOfRangeFillPaint;
    private final List<TbVisualMapPiece> pieces;

    public TbVisualMap(String outOfRangeColor, List<TsChartRangeItem> rangeItems, boolean fillArea, float fillAreaOpacity) {
        Color outOfRangePaint = ColorUtils.safeParseCssColor(outOfRangeColor);
        this.outOfRangePaint = outOfRangePaint;
        this.outOfRangeFillPaint = ChartUtils.createFillPaint(fillArea, fillAreaOpacity, outOfRangePaint);
        this.pieces = rangeItems.stream().map(TsChartRangeItem::getPiece).toList();
        this.pieces.forEach(p -> p.setupPaints(fillArea, fillAreaOpacity));
    }

    public boolean isEmpty() {
        return this.pieces.isEmpty();
    }

    public List<TbVisualMapArea> calculateAreas(ValueAxis rangeAxis, Rectangle2D dataArea) {
        ArrayList<TbVisualMapArea> areas = new ArrayList<TbVisualMapArea>();
        Range range = rangeAxis.getRange();
        double lower = range.getLowerBound();
        double upper = range.getUpperBound();
        double currentLower = lower;
        while (currentLower < upper) {
            Paint fillPaint;
            Paint paint;
            double currentUpper;
            double finalCurrentLower = currentLower;
            Optional<TbVisualMapPiece> piece = this.pieces.stream().filter(p -> p.matchLower(finalCurrentLower)).findFirst();
            if (piece.isPresent()) {
                TbVisualMapPiece pieceData = piece.get();
                currentUpper = pieceData.getUpper(upper);
                paint = pieceData.getPaint();
                fillPaint = pieceData.getFillPaint();
            } else {
                paint = this.outOfRangePaint;
                fillPaint = this.outOfRangeFillPaint;
                Optional<TbVisualMapPiece> nextPiece = this.pieces.stream().filter(p -> p.greaterLower(finalCurrentLower)).findFirst();
                if (nextPiece.isPresent()) {
                    TbVisualMapPiece pieceData = nextPiece.get();
                    currentUpper = pieceData.getNearestUpper(upper);
                } else {
                    currentUpper = upper;
                }
            }
            double bottomY = rangeAxis.valueToJava2D(currentLower, dataArea, RectangleEdge.LEFT);
            double topY = rangeAxis.valueToJava2D(currentUpper, dataArea, RectangleEdge.LEFT);
            Rectangle2D.Double area = new Rectangle2D.Double(dataArea.getX(), topY, dataArea.getWidth(), bottomY - topY);
            TbVisualMapArea mapArea = new TbVisualMapArea(paint, fillPaint, area);
            areas.add(mapArea);
            currentLower = currentUpper;
        }
        return areas;
    }

    public Paint lookupPaint(double value) {
        Optional<TbVisualMapPiece> piece = this.pieces.stream().filter(p -> p.matchValue(value)).findFirst();
        if (piece.isPresent()) {
            TbVisualMapPiece pieceData = piece.get();
            return pieceData.getPaint();
        }
        return this.outOfRangePaint;
    }

    @Generated
    public Paint getOutOfRangePaint() {
        return this.outOfRangePaint;
    }

    @Generated
    public Paint getOutOfRangeFillPaint() {
        return this.outOfRangeFillPaint;
    }

    @Generated
    public List<TbVisualMapPiece> getPieces() {
        return this.pieces;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbVisualMap)) {
            return false;
        }
        TbVisualMap other = (TbVisualMap)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Paint this$outOfRangePaint = this.getOutOfRangePaint();
        Paint other$outOfRangePaint = other.getOutOfRangePaint();
        if (this$outOfRangePaint == null ? other$outOfRangePaint != null : !this$outOfRangePaint.equals(other$outOfRangePaint)) {
            return false;
        }
        Paint this$outOfRangeFillPaint = this.getOutOfRangeFillPaint();
        Paint other$outOfRangeFillPaint = other.getOutOfRangeFillPaint();
        if (this$outOfRangeFillPaint == null ? other$outOfRangeFillPaint != null : !this$outOfRangeFillPaint.equals(other$outOfRangeFillPaint)) {
            return false;
        }
        List<TbVisualMapPiece> this$pieces = this.getPieces();
        List<TbVisualMapPiece> other$pieces = other.getPieces();
        return !(this$pieces == null ? other$pieces != null : !((Object)this$pieces).equals(other$pieces));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbVisualMap;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Paint $outOfRangePaint = this.getOutOfRangePaint();
        result = result * 59 + ($outOfRangePaint == null ? 43 : $outOfRangePaint.hashCode());
        Paint $outOfRangeFillPaint = this.getOutOfRangeFillPaint();
        result = result * 59 + ($outOfRangeFillPaint == null ? 43 : $outOfRangeFillPaint.hashCode());
        List<TbVisualMapPiece> $pieces = this.getPieces();
        result = result * 59 + ($pieces == null ? 43 : ((Object)$pieces).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbVisualMap(outOfRangePaint=" + String.valueOf(this.getOutOfRangePaint()) + ", outOfRangeFillPaint=" + String.valueOf(this.getOutOfRangeFillPaint()) + ", pieces=" + String.valueOf(this.getPieces()) + ")";
    }
}

