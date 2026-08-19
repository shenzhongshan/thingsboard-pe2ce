/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.LegendItem
 *  org.jfree.chart.block.Arrangement
 *  org.jfree.chart.block.Block
 *  org.jfree.chart.block.BlockContainer
 *  org.jfree.chart.block.BorderArrangement
 *  org.jfree.chart.block.CenterArrangement
 *  org.jfree.chart.block.EmptyBlock
 *  org.jfree.chart.block.LabelBlock
 *  org.jfree.chart.block.RectangleConstraint
 *  org.jfree.chart.title.LegendGraphic
 *  org.jfree.chart.title.LegendItemBlockContainer
 *  org.jfree.chart.title.LegendTitle
 *  org.jfree.chart.title.TextTitle
 *  org.jfree.chart.title.Title
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.RectangleAnchor
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.Size2D
 *  org.jfree.chart.ui.VerticalAlignment
 *  org.thingsboard.server.common.data.report.configuration.chart.LegendConfig
 */
package org.thingsboard.server.report.renderer.chart.legend;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.LegendItem;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.CenterArrangement;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.LegendItemBlockContainer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.report.renderer.chart.layout.TbTableBlockContainer;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItemSource;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbTableLegendTitle
extends Title {
    private final TbLegendItemSource source;
    private final LegendConfig legendConfig;
    private final TbTableBlockContainer legendTable;
    private Comparator<TbLegendItem> legendItemComparator;
    private RectangleInsets legendItemGraphicPadding;
    private RectangleAnchor legendItemGraphicAnchor;
    private RectangleAnchor legendItemGraphicLocation;
    private RectangleEdge legendItemGraphicEdge;
    private Font itemFont;
    private Paint itemPaint;
    private RectangleInsets itemLabelPadding;
    private Font legendColumnTitleFont;
    private Paint legendColumnTitlePaint;
    private Font legendValueFont;
    private Paint legendValuePaint;
    private double maxRelativeWidth = 0.0;
    private double maxRelativeHeight = 0.0;

    public TbTableLegendTitle(TbLegendItemSource source, LegendConfig config) {
        this.source = source;
        this.legendConfig = config;
        this.legendTable = new TbTableBlockContainer(16.0, 8.0);
        this.legendItemGraphicPadding = new RectangleInsets(2.0, 2.0, 2.0, 2.0);
        this.legendItemGraphicAnchor = RectangleAnchor.CENTER;
        this.legendItemGraphicLocation = RectangleAnchor.CENTER;
        this.legendItemGraphicEdge = RectangleEdge.LEFT;
        this.itemFont = LegendTitle.DEFAULT_ITEM_FONT;
        this.itemPaint = LegendTitle.DEFAULT_ITEM_PAINT;
        this.itemLabelPadding = new RectangleInsets(2.0, 2.0, 2.0, 2.0);
        this.legendColumnTitleFont = AwtFontUtils.newFont("Roboto", 0, 12.0f);
        this.legendColumnTitlePaint = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.38)");
        this.legendValueFont = AwtFontUtils.newFont("RobotoMedium", 0, 12.0f);
        this.legendValuePaint = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.87)");
    }

    public void setLegendItemComparator(Comparator<TbLegendItem> legendItemComparator) {
        this.legendItemComparator = legendItemComparator;
    }

    public void setLegendItemGraphicPadding(RectangleInsets legendItemGraphicPadding) {
        this.legendItemGraphicPadding = legendItemGraphicPadding;
    }

    public void setLegendItemGraphicAnchor(RectangleAnchor legendItemGraphicAnchor) {
        this.legendItemGraphicAnchor = legendItemGraphicAnchor;
    }

    public void setLegendItemGraphicLocation(RectangleAnchor legendItemGraphicLocation) {
        this.legendItemGraphicLocation = legendItemGraphicLocation;
    }

    public void setLegendItemGraphicEdge(RectangleEdge legendItemGraphicEdge) {
        this.legendItemGraphicEdge = legendItemGraphicEdge;
    }

    public void setItemFont(Font itemFont) {
        this.itemFont = itemFont;
    }

    public void setItemPaint(Paint itemPaint) {
        this.itemPaint = itemPaint;
    }

    public void setItemLabelPadding(RectangleInsets itemLabelPadding) {
        this.itemLabelPadding = itemLabelPadding;
    }

    public void setLegendColumnTitleFont(Font legendColumnTitleFont) {
        this.legendColumnTitleFont = legendColumnTitleFont;
    }

    public void setLegendColumnTitlePaint(Paint legendColumnTitlePaint) {
        this.legendColumnTitlePaint = legendColumnTitlePaint;
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
        this.fetchLegendItems();
        RectangleEdge p = this.getPosition();
        RectangleConstraint targetConstraint = constraint;
        if (RectangleEdge.isTopOrBottom((RectangleEdge)p)) {
            if (this.maxRelativeHeight > 0.0) {
                double maxHeight = constraint.getHeight() * this.maxRelativeHeight;
                targetConstraint = this.toContentConstraint(new RectangleConstraint(constraint.getWidth(), maxHeight));
            }
        } else if (this.maxRelativeWidth > 0.0) {
            double maxWidth = constraint.getWidth() * this.maxRelativeWidth;
            targetConstraint = this.toContentConstraint(new RectangleConstraint(maxWidth, constraint.getHeight()));
        }
        Size2D size = this.legendTable.arrange(g2, targetConstraint);
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
        return this.legendTable.draw(g2, target, params);
    }

    private void fetchLegendItems() {
        this.legendTable.clear();
        TbLegendValuesRequest request = this.buildLegendValuesRequest();
        List<TbLegendItem> legendItems = this.source.getTbLegendItems(request);
        if (legendItems != null && !legendItems.isEmpty()) {
            RectangleEdge p;
            if (this.legendItemComparator != null) {
                legendItems.sort(this.legendItemComparator);
            }
            if (RectangleEdge.isTopOrBottom((RectangleEdge)(p = this.getPosition()))) {
                this.buildVerticalLegendItems(legendItems);
            } else {
                this.buildHorizontalLegendItems(legendItems);
            }
        }
    }

    private TbLegendValuesRequest buildLegendValuesRequest() {
        TbLegendValuesRequest request = new TbLegendValuesRequest();
        if (this.legendConfig.getShowMin().booleanValue()) {
            request.setMin(true);
        }
        if (this.legendConfig.getShowMax().booleanValue()) {
            request.setMax(true);
        }
        if (this.legendConfig.getShowAvg().booleanValue()) {
            request.setAvg(true);
        }
        if (this.legendConfig.getShowTotal().booleanValue()) {
            request.setTotal(true);
        }
        if (this.legendConfig.getShowLatest().booleanValue()) {
            request.setLatest(true);
        }
        return request;
    }

    private void buildVerticalLegendItems(List<TbLegendItem> legendItems) {
        HashMap<String, Integer> legendTableColumns = new HashMap<String, Integer>();
        int legend = this.legendTable.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, true);
        this.legendTable.setColumnHeader((Block)new EmptyBlock(0.0, 0.0), legend);
        legendTableColumns.put("legend", legend);
        if (this.legendConfig.getShowMin().booleanValue()) {
            int min = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader((Block)new LabelBlock("Min", this.legendColumnTitleFont, this.legendColumnTitlePaint), min);
            legendTableColumns.put("min", min);
        }
        if (this.legendConfig.getShowMax().booleanValue()) {
            int max = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader((Block)new LabelBlock("Max", this.legendColumnTitleFont, this.legendColumnTitlePaint), max);
            legendTableColumns.put("max", max);
        }
        if (this.legendConfig.getShowAvg().booleanValue()) {
            int avg = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader((Block)new LabelBlock("Avg", this.legendColumnTitleFont, this.legendColumnTitlePaint), avg);
            legendTableColumns.put("avg", avg);
        }
        if (this.legendConfig.getShowTotal().booleanValue()) {
            int total = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader((Block)new LabelBlock("Total", this.legendColumnTitleFont, this.legendColumnTitlePaint), total);
            legendTableColumns.put("total", total);
        }
        if (this.legendConfig.getShowLatest().booleanValue()) {
            int latest = this.legendTable.addColumn(HorizontalAlignment.RIGHT, HorizontalAlignment.RIGHT, false);
            this.legendTable.setColumnHeader((Block)new LabelBlock("Latest", this.legendColumnTitleFont, this.legendColumnTitlePaint), latest);
            legendTableColumns.put("latest", latest);
        }
        for (TbLegendItem legendItem : legendItems) {
            this.addVerticalItemBlock(legendItem, legendTableColumns);
        }
    }

    private void buildHorizontalLegendItems(List<TbLegendItem> legendItems) {
        int legendValuesTitles = this.legendTable.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        this.legendTable.setColumnHeader((Block)new EmptyBlock(0.0, 0.0), legendValuesTitles);
        if (this.legendConfig.getShowMin().booleanValue()) {
            this.legendTable.addColumnCell((Block)new LabelBlock("Min", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (this.legendConfig.getShowMax().booleanValue()) {
            this.legendTable.addColumnCell((Block)new LabelBlock("Max", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (this.legendConfig.getShowAvg().booleanValue()) {
            this.legendTable.addColumnCell((Block)new LabelBlock("Avg", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (this.legendConfig.getShowTotal().booleanValue()) {
            this.legendTable.addColumnCell((Block)new LabelBlock("Total", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        if (this.legendConfig.getShowLatest().booleanValue()) {
            this.legendTable.addColumnCell((Block)new LabelBlock("Latest", this.legendColumnTitleFont, this.legendColumnTitlePaint), legendValuesTitles);
        }
        for (TbLegendItem legendItem : legendItems) {
            this.addHorizontalItemBlock(legendItem);
        }
    }

    private void addVerticalItemBlock(TbLegendItem item, Map<String, Integer> legendTableColumns) {
        LabelBlock valueBlock;
        this.legendTable.addColumnCell(this.createLegendItemBlock(item.getLegendItem()), legendTableColumns.get("legend"));
        TbLegendValues values = item.getLegendValues();
        if (this.legendConfig.getShowMin().booleanValue()) {
            valueBlock = new LabelBlock(values.getMin(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, legendTableColumns.get("min"));
        }
        if (this.legendConfig.getShowMax().booleanValue()) {
            valueBlock = new LabelBlock(values.getMax(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, legendTableColumns.get("max"));
        }
        if (this.legendConfig.getShowAvg().booleanValue()) {
            valueBlock = new LabelBlock(values.getAvg(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, legendTableColumns.get("avg"));
        }
        if (this.legendConfig.getShowTotal().booleanValue()) {
            valueBlock = new LabelBlock(values.getTotal(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, legendTableColumns.get("total"));
        }
        if (this.legendConfig.getShowLatest().booleanValue()) {
            valueBlock = new LabelBlock(values.getLatest(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, legendTableColumns.get("latest"));
        }
    }

    private void addHorizontalItemBlock(TbLegendItem item) {
        LabelBlock valueBlock;
        int column = this.legendTable.addColumn(HorizontalAlignment.CENTER, HorizontalAlignment.RIGHT, false);
        this.legendTable.setColumnHeader(this.createLegendItemBlock(item.getLegendItem()), column);
        TbLegendValues values = item.getLegendValues();
        if (this.legendConfig.getShowMin().booleanValue()) {
            valueBlock = new LabelBlock(values.getMin(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, column);
        }
        if (this.legendConfig.getShowMax().booleanValue()) {
            valueBlock = new LabelBlock(values.getMax(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, column);
        }
        if (this.legendConfig.getShowAvg().booleanValue()) {
            valueBlock = new LabelBlock(values.getAvg(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, column);
        }
        if (this.legendConfig.getShowTotal().booleanValue()) {
            valueBlock = new LabelBlock(values.getTotal(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, column);
        }
        if (this.legendConfig.getShowLatest().booleanValue()) {
            valueBlock = new LabelBlock(values.getLatest(), this.legendValueFont, this.legendValuePaint);
            this.legendTable.addColumnCell((Block)valueBlock, column);
        }
    }

    private Block createLegendItemBlock(LegendItem item) {
        Paint textPaint;
        LegendGraphic lg = new LegendGraphic(item.getShape(), item.getFillPaint());
        lg.setFillPaintTransformer(item.getFillPaintTransformer());
        lg.setShapeFilled(item.isShapeFilled());
        lg.setLine(item.getLine());
        lg.setLineStroke(item.getLineStroke());
        lg.setLinePaint(item.getLinePaint());
        lg.setLineVisible(item.isLineVisible());
        lg.setShapeVisible(item.isShapeVisible());
        lg.setShapeOutlineVisible(item.isShapeOutlineVisible());
        lg.setOutlinePaint(item.getOutlinePaint());
        lg.setOutlineStroke(item.getOutlineStroke());
        lg.setPadding(this.legendItemGraphicPadding);
        LegendItemBlockContainer legendItem = new LegendItemBlockContainer((Arrangement)new BorderArrangement(), item.getDataset(), item.getSeriesKey());
        lg.setShapeAnchor(this.legendItemGraphicAnchor);
        lg.setShapeLocation(this.legendItemGraphicLocation);
        legendItem.add((Block)lg, (Object)this.legendItemGraphicEdge);
        Font textFont = item.getLabelFont();
        if (textFont == null) {
            textFont = this.itemFont;
        }
        if ((textPaint = item.getLabelPaint()) == null) {
            textPaint = this.itemPaint;
        }
        TextTitle labelBlock = new TextTitle(item.getLabel(), textFont, textPaint, Title.DEFAULT_POSITION, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, this.itemLabelPadding);
        labelBlock.setMaximumLinesToDisplay(1);
        legendItem.add((Block)labelBlock);
        legendItem.setToolTipText(item.getToolTipText());
        legendItem.setURLText(item.getURLText());
        BlockContainer result = new BlockContainer((Arrangement)new CenterArrangement());
        result.add((Block)legendItem);
        return result;
    }
}

