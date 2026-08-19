/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.LegendItem
 *  org.jfree.chart.LegendItemSource
 *  org.jfree.chart.block.Arrangement
 *  org.jfree.chart.block.Block
 *  org.jfree.chart.block.BlockContainer
 *  org.jfree.chart.block.BorderArrangement
 *  org.jfree.chart.block.CenterArrangement
 *  org.jfree.chart.title.LegendGraphic
 *  org.jfree.chart.title.LegendItemBlockContainer
 *  org.jfree.chart.title.LegendTitle
 *  org.jfree.chart.title.TextTitle
 *  org.jfree.chart.title.Title
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.VerticalAlignment
 */
package org.thingsboard.server.report.renderer.chart.legend;

import java.awt.Font;
import java.awt.Paint;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.CenterArrangement;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.LegendItemBlockContainer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.VerticalAlignment;

public class TbLegendTitle
extends LegendTitle {
    public TbLegendTitle(LegendItemSource source, Arrangement hLayout, Arrangement vLayout) {
        super(source, hLayout, vLayout);
    }

    protected Block createLegendItemBlock(LegendItem item) {
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
        lg.setPadding(this.getLegendItemGraphicPadding());
        LegendItemBlockContainer legendItem = new LegendItemBlockContainer((Arrangement)new BorderArrangement(), item.getDataset(), item.getSeriesKey());
        lg.setShapeAnchor(this.getLegendItemGraphicAnchor());
        lg.setShapeLocation(this.getLegendItemGraphicLocation());
        legendItem.add((Block)lg, (Object)this.getLegendItemGraphicEdge());
        Font textFont = item.getLabelFont();
        if (textFont == null) {
            textFont = this.getItemFont();
        }
        if ((textPaint = item.getLabelPaint()) == null) {
            textPaint = this.getItemPaint();
        }
        TextTitle labelBlock = new TextTitle(item.getLabel(), textFont, textPaint, Title.DEFAULT_POSITION, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, this.getItemLabelPadding());
        labelBlock.setMaximumLinesToDisplay(1);
        legendItem.add((Block)labelBlock);
        legendItem.setToolTipText(item.getToolTipText());
        legendItem.setURLText(item.getURLText());
        BlockContainer result = new BlockContainer((Arrangement)new CenterArrangement());
        result.add((Block)legendItem);
        return result;
    }
}

