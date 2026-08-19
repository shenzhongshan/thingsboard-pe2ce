/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.block.Arrangement
 *  org.jfree.chart.block.Block
 *  org.jfree.chart.block.BlockContainer
 *  org.jfree.chart.block.LabelBlock
 *  org.jfree.chart.block.RectangleConstraint
 *  org.jfree.chart.title.LegendGraphic
 *  org.jfree.chart.title.Title
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.Size2D
 *  org.jfree.chart.ui.VerticalAlignment
 */
package org.thingsboard.server.report.renderer.chart.legend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.renderer.chart.layout.TbColumnArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbTableBlockContainer;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbRangeLegendTitle
extends Title {
    private static final Shape LEGEND_ITEM_SHAPE = new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0);
    private final List<TsChartRangeItem> rangeItems;
    private Block legendContainer;
    private Font legendLabelFont;
    private Paint legendLabelPaint;
    private double maxRelativeWidth = 0.0;
    private double maxRelativeHeight = 0.0;

    public TbRangeLegendTitle(List<TsChartRangeItem> rangeItems) {
        this.rangeItems = rangeItems;
        this.legendLabelFont = AwtFontUtils.newFont("Roboto", 0, 12.0f);
        this.legendLabelPaint = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.76)");
    }

    public void setLegendLabelFont(Font legendLabelFont) {
        this.legendLabelFont = legendLabelFont;
    }

    public void setLegendLabelPaint(Paint legendLabelPaint) {
        this.legendLabelPaint = legendLabelPaint;
    }

    public void setMaxRelativeWidth(double maxRelativeWidth) {
        this.maxRelativeWidth = maxRelativeWidth;
    }

    public void setMaxRelativeHeight(double maxRelativeHeight) {
        this.maxRelativeHeight = maxRelativeHeight;
    }

    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        this.buildLegendItems();
        RectangleEdge p = this.getPosition();
        RectangleConstraint targetConstraint = constraint;
        if (RectangleEdge.isTopOrBottom((RectangleEdge)p)) {
            if (this.maxRelativeHeight > 0.0) {
                double maxHeight = constraint.getHeight() * this.maxRelativeHeight;
                targetConstraint = this.toContentConstraint(new RectangleConstraint(constraint.getWidth(), maxHeight));
            } else {
                targetConstraint = this.toContentConstraint(constraint.toFixedWidth(constraint.getWidth()));
            }
        } else if (this.maxRelativeWidth > 0.0) {
            double maxWidth = constraint.getWidth() * this.maxRelativeWidth;
            targetConstraint = this.toContentConstraint(new RectangleConstraint(maxWidth, constraint.getHeight()));
        }
        Size2D size = this.legendContainer.arrange(g2, targetConstraint);
        Size2D result = new Size2D();
        result.height = this.calculateTotalHeight(size.height);
        result.width = this.calculateTotalWidth(size.width);
        return result;
    }

    public void draw(Graphics2D g2, Rectangle2D area) {
        this.draw(g2, area, null);
    }

    public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
        Rectangle2D target = (Rectangle2D)area.clone();
        target = this.trimMargin(target);
        target = this.trimBorder(target);
        target = this.trimPadding(target);
        return this.legendContainer.draw(g2, target, params);
    }

    private void buildLegendItems() {
        Object arrangement;
        List<TsChartRangeItem> items;
        if (RectangleEdge.isTopOrBottom((RectangleEdge)this.getPosition())) {
            items = this.rangeItems;
            arrangement = new TbFlowArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 24.0, 8.0);
        } else {
            items = new ArrayList<TsChartRangeItem>(this.rangeItems);
            Collections.reverse(items);
            arrangement = new TbColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, 24.0, 8.0);
        }
        this.legendContainer = new BlockContainer((Arrangement)arrangement);
        for (TsChartRangeItem item : items) {
            Block itemBlock = this.buildLegendItem(item);
            ((BlockContainer)this.legendContainer).add(itemBlock);
        }
    }

    private Block buildLegendItem(TsChartRangeItem item) {
        TbTableBlockContainer legendBlock = new TbTableBlockContainer(4.0, 0.0);
        int shape = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block shapeBlock = this.buildLegendShape(item);
        legendBlock.setColumnHeader(shapeBlock, shape);
        int label = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block labelBlock = this.buildLegendLabel(item);
        legendBlock.setColumnHeader(labelBlock, label);
        return legendBlock;
    }

    private Block buildLegendShape(TsChartRangeItem item) {
        Color shapeFill = ColorUtils.safeParseCssColor(item.getColor());
        LegendGraphic lg = new LegendGraphic(LEGEND_ITEM_SHAPE, (Paint)shapeFill);
        lg.setPadding(RectangleInsets.ZERO_INSETS);
        return lg;
    }

    private Block buildLegendLabel(TsChartRangeItem item) {
        String label = item.getLabel();
        Paint labelPaint = this.legendLabelPaint;
        return new LabelBlock(label, this.legendLabelFont, labelPaint);
    }
}

