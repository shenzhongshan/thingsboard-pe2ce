/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.server.common.data.report.configuration.chart.ColorRange
 */
package org.thingsboard.server.report.context.chart;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ColorRange;
import org.thingsboard.server.report.renderer.chart.TbVisualMapPiece;

public class TsChartRangeItem {
    private int index;
    private Double from;
    private Double to;
    private String color;
    private String label;
    private boolean visible;
    private TbVisualMapPiece piece;

    public static List<TsChartRangeItem> toRangeItems(List<ColorRange> colorRanges, NumberFormat valueFormat) {
        ArrayList<TsChartRangeItem> rangeItems = new ArrayList<TsChartRangeItem>();
        int counter = 0;
        List<ColorRange> ranges = ColorRange.filterIncludingColorRanges(colorRanges).stream().sorted(ColorRange.COLOR_RANGE_COMPARATOR).filter(r -> TsChartRangeItem.isNumber(r.getFrom()) || TsChartRangeItem.isNumber(r.getTo())).toList();
        for (int i = 0; i < ranges.size(); ++i) {
            ColorRange prevRange;
            ColorRange range = ranges.get(i);
            Double from = range.getFrom();
            Double to = range.getTo();
            if (i > 0 && TsChartRangeItem.isNumber((prevRange = ranges.get(i - 1)).getTo()) && TsChartRangeItem.isNumber(from) && from < prevRange.getTo()) {
                from = prevRange.getTo();
            }
            Double formatToValue = to != null ? Double.valueOf(valueFormat.format(to)) : null;
            Double formatFromValue = from != null ? Double.valueOf(valueFormat.format(from)) : null;
            TsChartRangeItem rangeItem = new TsChartRangeItem();
            rangeItem.setIndex(counter++);
            rangeItem.setColor(range.getColor());
            rangeItem.setVisible(true);
            rangeItem.setFrom(from);
            rangeItem.setTo(to);
            rangeItem.setLabel(TsChartRangeItem.rangeItemLabel(formatFromValue, formatToValue, valueFormat));
            rangeItem.setPiece(TbVisualMapPiece.fromRange(range.getColor(), formatFromValue, formatToValue));
            rangeItems.add(rangeItem);
        }
        return rangeItems;
    }

    public static List<Double> toMarkPoints(List<TsChartRangeItem> ranges) {
        HashSet<Double> points = new HashSet<Double>();
        for (TsChartRangeItem range : ranges) {
            if (!range.isVisible()) continue;
            if (TsChartRangeItem.isNumber(range.getFrom())) {
                points.add(range.getFrom());
            }
            if (!TsChartRangeItem.isNumber(range.getTo())) continue;
            points.add(range.getTo());
        }
        return points.stream().sorted(Double::compareTo).toList();
    }

    private static boolean isNumber(Double number) {
        return number != null && Double.isFinite(number);
    }

    private static String rangeItemLabel(Double from, Double to, NumberFormat valueFormat) {
        if (TsChartRangeItem.isNumber(from) && TsChartRangeItem.isNumber(to)) {
            if (from.compareTo(to) == 0) {
                return valueFormat.format(from);
            }
            return valueFormat.format(from) + " - " + valueFormat.format(to);
        }
        if (TsChartRangeItem.isNumber(from)) {
            return "\u2265 " + valueFormat.format(from);
        }
        if (TsChartRangeItem.isNumber(to)) {
            return "< " + valueFormat.format(to);
        }
        return null;
    }

    @Generated
    public TsChartRangeItem() {
    }

    @Generated
    public int getIndex() {
        return this.index;
    }

    @Generated
    public Double getFrom() {
        return this.from;
    }

    @Generated
    public Double getTo() {
        return this.to;
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public boolean isVisible() {
        return this.visible;
    }

    @Generated
    public TbVisualMapPiece getPiece() {
        return this.piece;
    }

    @Generated
    public void setIndex(int index) {
        this.index = index;
    }

    @Generated
    public void setFrom(Double from) {
        this.from = from;
    }

    @Generated
    public void setTo(Double to) {
        this.to = to;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Generated
    public void setPiece(TbVisualMapPiece piece) {
        this.piece = piece;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TsChartRangeItem)) {
            return false;
        }
        TsChartRangeItem other = (TsChartRangeItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIndex() != other.getIndex()) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        Double this$from = this.getFrom();
        Double other$from = other.getFrom();
        if (this$from == null ? other$from != null : !((Object)this$from).equals(other$from)) {
            return false;
        }
        Double this$to = this.getTo();
        Double other$to = other.getTo();
        if (this$to == null ? other$to != null : !((Object)this$to).equals(other$to)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) {
            return false;
        }
        TbVisualMapPiece this$piece = this.getPiece();
        TbVisualMapPiece other$piece = other.getPiece();
        return !(this$piece == null ? other$piece != null : !((Object)this$piece).equals(other$piece));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TsChartRangeItem;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIndex();
        result = result * 59 + (this.isVisible() ? 79 : 97);
        Double $from = this.getFrom();
        result = result * 59 + ($from == null ? 43 : ((Object)$from).hashCode());
        Double $to = this.getTo();
        result = result * 59 + ($to == null ? 43 : ((Object)$to).hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        TbVisualMapPiece $piece = this.getPiece();
        result = result * 59 + ($piece == null ? 43 : ((Object)$piece).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TsChartRangeItem(index=" + this.getIndex() + ", from=" + this.getFrom() + ", to=" + this.getTo() + ", color=" + this.getColor() + ", label=" + this.getLabel() + ", visible=" + this.isVisible() + ", piece=" + String.valueOf(this.getPiece()) + ")";
    }
}

