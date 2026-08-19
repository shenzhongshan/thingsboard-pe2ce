/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.plot.ValueMarker
 *  org.jfree.chart.ui.LengthAdjustmentType
 *  org.jfree.chart.ui.RectangleAnchor
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.TextAnchor
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartShape
 *  org.thingsboard.server.common.data.report.configuration.chart.ThresholdLabelPosition
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.LengthAdjustmentType;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.ThresholdLabelPosition;
import org.thingsboard.server.report.renderer.chart.ChartUtils;

public class TbThresholdMarker
extends ValueMarker {
    private static final RectangleInsets DEFAULT_LABEL_OFFSET = new RectangleInsets(5.0, 5.0, 5.0, 5.0);
    private static final RectangleInsets BACKGROUND_LABEL_OFFSET = new RectangleInsets(7.0, 8.0, 7.0, 8.0);
    private ChartShape startSymbol;
    private float startSymbolSize;
    private Shape startShape;
    private ChartShape endSymbol;
    private float endSymbolSize;
    private Shape endShape;
    private Paint startShapeFillPaint;
    private Stroke startShapeOutlineStroke;
    private Paint endShapeFillPaint;
    private Stroke endShapeOutlineStroke;
    private boolean drawLabelBackground;
    private ThresholdLabelPosition labelPosition;

    public TbThresholdMarker(double value) {
        super(value);
        this.setLabelOffset(DEFAULT_LABEL_OFFSET);
        this.setAlpha(1.0f);
        this.labelPosition = ThresholdLabelPosition.end;
    }

    public Shape getStartShape() {
        return this.startShape;
    }

    public Shape getEndShape() {
        return this.endShape;
    }

    public Paint getStartShapeFillPaint() {
        return this.startShapeFillPaint;
    }

    public Stroke getStartShapeOutlineStroke() {
        return this.startShapeOutlineStroke;
    }

    public Paint getEndShapeFillPaint() {
        return this.endShapeFillPaint;
    }

    public Stroke getEndShapeOutlineStroke() {
        return this.endShapeOutlineStroke;
    }

    public boolean isDrawLabelBackground() {
        return this.drawLabelBackground;
    }

    public void setDrawLabelBackground(boolean drawLabelBackground) {
        this.drawLabelBackground = drawLabelBackground;
        if (this.drawLabelBackground) {
            this.setLabelOffset(BACKGROUND_LABEL_OFFSET);
        } else {
            this.setLabelOffset(DEFAULT_LABEL_OFFSET);
        }
    }

    public void setStartSymbol(ChartShape startSymbol, float startSymbolSize) {
        this.startSymbol = startSymbol;
        this.startSymbolSize = startSymbolSize;
        this.startShape = ChartUtils.createSeriesShape(startSymbol, startSymbolSize);
        if (this.startShape != null && ChartShape.emptyCircle.equals((Object)this.startSymbol)) {
            this.startShapeFillPaint = Color.WHITE;
            this.startShapeOutlineStroke = new BasicStroke(2.0f);
        }
    }

    public void setEndSymbol(ChartShape endSymbol, float endSymbolSize) {
        this.endSymbol = endSymbol;
        this.endSymbolSize = endSymbolSize;
        this.endShape = ChartUtils.createSeriesShape(endSymbol, endSymbolSize);
        if (this.endShape != null && ChartShape.emptyCircle.equals((Object)this.endSymbol)) {
            this.endShapeFillPaint = Color.WHITE;
            this.endShapeOutlineStroke = new BasicStroke(2.0f);
        }
    }

    public void setLabelPosition(ThresholdLabelPosition labelPosition) {
        this.labelPosition = labelPosition;
    }

    public ThresholdLabelPosition getLabelPosition() {
        return this.labelPosition;
    }

    public RectangleAnchor getLabelAnchor() {
        switch (this.labelPosition) {
            case start: 
            case insideStart: {
                return RectangleAnchor.LEFT;
            }
            case middle: 
            case insideMiddleTop: {
                return RectangleAnchor.TOP;
            }
            case end: 
            case insideEnd: {
                return RectangleAnchor.RIGHT;
            }
            case insideStartTop: {
                return RectangleAnchor.TOP_LEFT;
            }
            case insideStartBottom: {
                return RectangleAnchor.BOTTOM_LEFT;
            }
            case insideMiddle: {
                return RectangleAnchor.CENTER;
            }
            case insideMiddleBottom: {
                return RectangleAnchor.BOTTOM;
            }
            case insideEndTop: {
                return RectangleAnchor.TOP_RIGHT;
            }
            case insideEndBottom: {
                return RectangleAnchor.BOTTOM_RIGHT;
            }
        }
        return RectangleAnchor.CENTER;
    }

    public TextAnchor getLabelTextAnchor() {
        switch (this.labelPosition) {
            case start: 
            case insideEnd: {
                return TextAnchor.CENTER_RIGHT;
            }
            case middle: 
            case insideMiddleTop: {
                return TextAnchor.BOTTOM_CENTER;
            }
            case insideStart: 
            case end: {
                return TextAnchor.CENTER_LEFT;
            }
            case insideStartTop: {
                return TextAnchor.BOTTOM_LEFT;
            }
            case insideStartBottom: {
                return TextAnchor.TOP_LEFT;
            }
            case insideMiddle: {
                return TextAnchor.CENTER;
            }
            case insideMiddleBottom: {
                return TextAnchor.TOP_CENTER;
            }
            case insideEndTop: {
                return TextAnchor.BOTTOM_RIGHT;
            }
            case insideEndBottom: {
                return TextAnchor.TOP_RIGHT;
            }
        }
        return TextAnchor.CENTER;
    }

    public LengthAdjustmentType getLabelOffsetType() {
        switch (this.labelPosition) {
            case start: 
            case end: {
                return LengthAdjustmentType.EXPAND;
            }
            case insideStart: 
            case middle: 
            case insideMiddleTop: 
            case insideEnd: 
            case insideStartTop: 
            case insideStartBottom: 
            case insideMiddle: 
            case insideMiddleBottom: 
            case insideEndTop: 
            case insideEndBottom: {
                return LengthAdjustmentType.CONTRACT;
            }
        }
        return LengthAdjustmentType.CONTRACT;
    }

    public double[] measureOffset(Graphics2D g2) {
        double[] offset = new double[]{0.0, 0.0};
        if (this.startShape != null) {
            offset[0] = this.symbolOffset(this.startSymbol, this.startSymbolSize);
        }
        if (this.endShape != null) {
            offset[1] = this.symbolOffset(this.endSymbol, this.endSymbolSize);
        }
        if (this.labelPosition == ThresholdLabelPosition.start || this.labelPosition == ThresholdLabelPosition.end) {
            double width = this.measureLabelWidth(g2);
            int index = this.labelPosition == ThresholdLabelPosition.start ? 0 : 1;
            offset[index] = Math.max(width, offset[index]);
        }
        return offset;
    }

    private double measureLabelWidth(Graphics2D g2) {
        if (this.getLabel() != null) {
            FontMetrics metrics = g2.getFontMetrics(this.getLabelFont());
            double textWidth = metrics.stringWidth(this.getLabel());
            RectangleInsets labelOffset = this.getLabelOffset();
            return textWidth += labelOffset.getLeft() + labelOffset.getRight();
        }
        return 0.0;
    }

    private double symbolOffset(ChartShape symbol, float symbolSize) {
        switch (symbol) {
            case emptyCircle: {
                return symbolSize / 2.0f + 1.0f;
            }
            case circle: 
            case diamond: 
            case triangle: 
            case roundRect: 
            case rect: {
                return symbolSize / 2.0f;
            }
            case pin: {
                return symbolSize;
            }
            case arrow: 
            case none: {
                return 0.0;
            }
        }
        return symbolSize / 2.0f;
    }
}

