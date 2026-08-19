/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.block.Block
 *  org.jfree.chart.block.BlockContainer
 *  org.jfree.chart.block.ColumnArrangement
 *  org.jfree.chart.block.LengthConstraintType
 *  org.jfree.chart.block.RectangleConstraint
 *  org.jfree.chart.title.TextTitle
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.Size2D
 *  org.jfree.chart.ui.VerticalAlignment
 */
package org.thingsboard.server.report.renderer.chart.layout;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.block.LengthConstraintType;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;

public class TbColumnArrangement
extends ColumnArrangement {
    private double maxRelativeWidth = 0.0;

    public TbColumnArrangement(HorizontalAlignment hAlign, VerticalAlignment vAlign, double hGap, double vGap) {
        super(hAlign, vAlign, hGap, vGap);
    }

    public void setMaxRelativeWidth(double maxRelativeWidth) {
        this.maxRelativeWidth = maxRelativeWidth;
    }

    public Size2D arrange(BlockContainer container, Graphics2D g2, RectangleConstraint constraint) {
        if (this.maxRelativeWidth > 0.0) {
            double maxWidth = constraint.getWidth() * this.maxRelativeWidth;
            constraint = constraint.toFixedWidth(maxWidth);
        }
        LengthConstraintType w = constraint.getWidthConstraintType();
        LengthConstraintType h = constraint.getHeightConstraintType();
        if (w == LengthConstraintType.FIXED && h == LengthConstraintType.RANGE) {
            return this.arrangeFR(container, g2, constraint);
        }
        return super.arrange(container, g2, constraint);
    }

    protected Size2D arrangeFR(BlockContainer container, Graphics2D g2, RectangleConstraint constraint) {
        return this.arrangeFF(container, g2, constraint);
    }

    protected Size2D arrangeFF(BlockContainer container, Graphics2D g2, RectangleConstraint constraint) {
        Size2D s = super.arrangeNF(container, g2, constraint);
        if (s.width > constraint.getWidth()) {
            List<Block> blocks = container.getBlocks();
            ArrayList<Block> visibleBlocks = new ArrayList<Block>();
            double maxWidth = 0.0;
            for (Block b : blocks) {
                Rectangle2D bounds = b.getBounds();
                double left = bounds.getMinX();
                double right = bounds.getMaxX();
                if (!(left < constraint.getWidth())) continue;
                visibleBlocks.add(b);
                if (right > constraint.getWidth()) {
                    double newWidth = bounds.getWidth() - (right - constraint.getWidth());
                    bounds = new Rectangle2D.Double(bounds.getX(), bounds.getY(), newWidth, bounds.getHeight());
                    b.setBounds(bounds);
                    this.arrangeTitleWidth(g2, b, bounds.getWidth());
                }
                maxWidth = Math.max(maxWidth, bounds.getMaxX());
            }
            container.clear();
            for (Block b : visibleBlocks) {
                container.add(b);
            }
            s.width = maxWidth;
            return s;
        }
        return s;
    }

    private double arrangeTitleWidth(Graphics2D g2, Block block, double maxWidth) {
        if (block instanceof BlockContainer) {
            BlockContainer container = (BlockContainer)block;
            List<Block> blocks = container.getBlocks();
            for (Block b : blocks) {
                maxWidth = this.arrangeTitleWidth(g2, b, maxWidth);
            }
        } else if (block instanceof TextTitle) {
            TextTitle textTitle = (TextTitle)block;
            textTitle.arrange(g2, new RectangleConstraint(maxWidth -= textTitle.getPadding().getLeft() + textTitle.getPadding().getRight(), 0.0).toUnconstrainedHeight());
        } else {
            maxWidth -= block.getBounds().getWidth();
        }
        return maxWidth;
    }
}

