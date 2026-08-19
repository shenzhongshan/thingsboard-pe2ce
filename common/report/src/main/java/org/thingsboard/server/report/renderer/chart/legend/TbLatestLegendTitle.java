/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.block.Arrangement
 *  org.jfree.chart.block.Block
 *  org.jfree.chart.block.BlockContainer
 *  org.jfree.chart.block.EmptyBlock
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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.report.renderer.chart.layout.TbContentJustify;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbTableBlockContainer;
import org.thingsboard.server.report.renderer.chart.legend.TbLatestChartLegendItem;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbLatestLegendTitle
extends Title {
    private static final Shape LEGEND_ITEM_SHAPE = new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0);
    private static final Paint TOTAL_SHAPE_FILL = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.06)");
    private static final Paint DISABLED_ITEM_PAINT = ColorUtils.safeParseCssColor("#ccc");
    private final List<TbLatestChartLegendItem> legendItems;
    private Block legendContainer;
    private Font legendLabelFont;
    private Paint legendLabelPaint;
    private Font legendValueFont;
    private Paint legendValuePaint;
    private double maxRelativeWidth = 0.0;
    private double maxRelativeHeight = 0.0;

    public TbLatestLegendTitle(List<TbLatestChartLegendItem> legendItems) {
        this.legendItems = legendItems;
        this.legendLabelFont = AwtFontUtils.newFont("Roboto", 0, 12.0f);
        this.legendLabelPaint = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.38)");
        this.legendValueFont = AwtFontUtils.newFont("Roboto", 1, 14.0f);
        this.legendValuePaint = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.87)");
    }

    public void setLegendLabelFont(Font legendLabelFont) {
        this.legendLabelFont = legendLabelFont;
    }

    public void setLegendLabelPaint(Paint legendLabelPaint) {
        this.legendLabelPaint = legendLabelPaint;
    }

    public void setLegendValueFont(Font legendValueFont) {
        this.legendValueFont = legendValueFont;
    }

    public void setLegendValuePaint(Paint legendValuePaint) {
        this.legendValuePaint = legendValuePaint;
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
        if (RectangleEdge.isTopOrBottom((RectangleEdge)this.getPosition())) {
            TbFlowArrangement legendArrangement = new TbFlowArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, TbContentJustify.SPACE_AROUND, 8.0, 8.0);
            this.legendContainer = new BlockContainer((Arrangement)legendArrangement);
        } else {
            this.legendContainer = new TbTableBlockContainer(4.0, 8.0);
        }
        RectangleEdge p = this.getPosition();
        if (RectangleEdge.isTopOrBottom((RectangleEdge)p)) {
            this.buildVerticalLegendItems();
        } else {
            this.buildHorizontalLegendItems();
        }
    }

    private void buildVerticalLegendItems() {
        BlockContainer container = (BlockContainer)this.legendContainer;
        for (TbLatestChartLegendItem item : this.legendItems) {
            Block itemBlock = this.buildVerticalLegendItem(item);
            container.add(itemBlock);
        }
    }

    private void buildHorizontalLegendItems() {
        TbTableBlockContainer tableContainer = (TbTableBlockContainer)this.legendContainer;
        int legendShape = tableContainer.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        tableContainer.setColumnHeader((Block)new EmptyBlock(0.0, 0.0), legendShape);
        int legendLabel = tableContainer.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        tableContainer.setColumnHeader((Block)new EmptyBlock(0.0, 0.0), legendLabel);
        int legendValue = tableContainer.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, false);
        tableContainer.setColumnHeader((Block)new EmptyBlock(0.0, 0.0), legendValue);
        for (TbLatestChartLegendItem item : this.legendItems) {
            Block shapeBlock = this.buildLegendShape(item);
            tableContainer.addColumnCell(shapeBlock, legendShape);
            Block labelBlock = this.buildLegendLabel(item);
            tableContainer.addColumnCell(labelBlock, legendLabel);
            Block valueBlock = this.buildLegendValue(item);
            tableContainer.addColumnCell(valueBlock, legendValue);
        }
    }

    private Block buildVerticalLegendItem(TbLatestChartLegendItem item) {
        TbTableBlockContainer legendBlock = new TbTableBlockContainer(4.0, 0.0);
        int shape = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block shapeBlock = this.buildLegendShape(item);
        legendBlock.setColumnHeader(shapeBlock, shape);
        legendBlock.addColumnCell((Block)new EmptyBlock(0.0, 0.0), shape);
        int label = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block labelBlock = this.buildLegendLabel(item);
        legendBlock.setColumnHeader(labelBlock, label);
        Block valueBlock = this.buildLegendValue(item);
        legendBlock.addColumnCell(valueBlock, label);
        return legendBlock;
    }

    private Block buildLegendShape(TbLatestChartLegendItem item) {
        Paint shapeFill = !item.isHasValue() ? DISABLED_ITEM_PAINT : (!item.isTotal() ? ColorUtils.safeParseCssColor(item.getColor()) : TOTAL_SHAPE_FILL);
        LegendGraphic lg = new LegendGraphic(LEGEND_ITEM_SHAPE, shapeFill);
        lg.setPadding(RectangleInsets.ZERO_INSETS);
        return lg;
    }

    private Block buildLegendLabel(TbLatestChartLegendItem item) {
        String label = item.getLabel();
        Paint labelPaint = this.legendLabelPaint;
        if (!item.isHasValue()) {
            labelPaint = DISABLED_ITEM_PAINT;
        }
        return new LabelBlock(label, this.legendLabelFont, labelPaint);
    }

    private Block buildLegendValue(TbLatestChartLegendItem item) {
        String value = item.getValue();
        Paint labelPaint = this.legendValuePaint;
        if (!item.isHasValue()) {
            labelPaint = DISABLED_ITEM_PAINT;
        }
        return new LabelBlock(value, this.legendValueFont, labelPaint);
    }
}

