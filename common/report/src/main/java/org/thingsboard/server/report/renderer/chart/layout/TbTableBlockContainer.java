/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.block.AbstractBlock
 *  org.jfree.chart.block.Arrangement
 *  org.jfree.chart.block.Block
 *  org.jfree.chart.block.BlockContainer
 *  org.jfree.chart.block.RectangleConstraint
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.Size2D
 *  org.jfree.chart.ui.VerticalAlignment
 */
package org.thingsboard.server.report.renderer.chart.layout;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.block.AbstractBlock;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;

public class TbTableBlockContainer
extends AbstractBlock
implements Block {
    private final double horizontalGap;
    private final double verticalGap;
    private final List<TbTableBlockColumn> columns;
    private final Map<Integer, Double> columnWidths;
    private double headerHeight = 0.0;
    private double rowHeight = 0.0;

    public TbTableBlockContainer(double horizontalGap, double verticalGap) {
        this.horizontalGap = horizontalGap;
        this.verticalGap = verticalGap;
        this.columns = new ArrayList<TbTableBlockColumn>();
        this.columnWidths = new HashMap<Integer, Double>();
    }

    public int addColumn(HorizontalAlignment headerAlign, HorizontalAlignment cellAlign, boolean fitWidth) {
        TbTableBlockColumn column = new TbTableBlockColumn(headerAlign, cellAlign, fitWidth);
        this.columns.add(column);
        int index = this.columns.size() - 1;
        this.columnWidths.put(index, 0.0);
        return index;
    }

    public void setColumnHeader(Block header, int column) {
        this.columns.get(column).setHeader(header);
    }

    public void addColumnCell(Block cell, int column) {
        this.columns.get(column).addCell(cell);
    }

    public void clear() {
        this.columns.clear();
        this.columnWidths.clear();
        this.headerHeight = 0.0;
        this.rowHeight = 0.0;
    }

    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        if (this.columns.isEmpty()) {
            return new Size2D(0.0, 0.0);
        }
        double maxWidth = constraint.getWidth();
        double maxHeight = constraint.getHeight();
        if (maxWidth <= 0.0 && maxHeight <= 0.0) {
            maxWidth = Double.MAX_VALUE;
            maxHeight = Double.MAX_VALUE;
        }
        ArrayList<Size2D> headerSizes = new ArrayList<Size2D>();
        ArrayList<Size2D> maxCellSizes = new ArrayList<Size2D>();
        for (int i = 0; i < this.columns.size(); ++i) {
            TbTableBlockColumn column2 = this.columns.get(i);
            Size2D[] sizes = column2.calculateMaxCellDimensions(g2);
            headerSizes.add(sizes[0]);
            maxCellSizes.add(sizes[1]);
            if (!column2.fixedWidth()) continue;
            this.columnWidths.put(i, sizes[1].width);
        }
        for (Size2D header : headerSizes) {
            this.headerHeight = Math.max(this.headerHeight, header.getHeight());
        }
        for (Size2D maxCell : maxCellSizes) {
            this.rowHeight = Math.max(this.rowHeight, maxCell.getHeight());
        }
        int fitWidthColumnsCount = (int)this.columns.stream().filter(c -> !c.fixedWidth()).count();
        if (fitWidthColumnsCount > 0) {
            double availableColumnsWidth;
            int fixedColumnsCount = this.columns.size() - fitWidthColumnsCount;
            double fixedColumnsWidth = 0.0;
            if (fixedColumnsCount > 0) {
                fixedColumnsWidth = this.columnWidths.values().stream().mapToDouble(Double::doubleValue).sum() + (double)(fixedColumnsCount - 1) * this.horizontalGap;
            }
            if ((availableColumnsWidth = Math.max(0.0, maxWidth - fixedColumnsWidth)) > 0.0) {
                double fitWidthColumnWidth = (availableColumnsWidth - (double)fitWidthColumnsCount * this.horizontalGap) / (double)fitWidthColumnsCount;
                this.columns.stream().filter(c -> !c.fixedWidth()).forEach(column -> {
                    int index = this.columns.indexOf(column);
                    this.columnWidths.put(index, fitWidthColumnWidth);
                });
            }
        }
        boolean heightOverflow = false;
        boolean widthOverflow = false;
        double x = 0.0;
        double y = 0.0;
        double cellHeight = this.headerHeight;
        if (maxHeight < this.headerHeight) {
            heightOverflow = true;
            cellHeight = 0.0;
        }
        for (int i = 0; i < this.columns.size(); ++i) {
            double leftWidth = widthOverflow ? 0.0 : Math.max(maxWidth - x, 0.0);
            double horizontalGap = 0.0;
            if (i > 0) {
                horizontalGap = this.horizontalGap;
                if (leftWidth < this.horizontalGap) {
                    widthOverflow = true;
                    horizontalGap = 0.0;
                }
            }
            leftWidth = widthOverflow ? 0.0 : Math.max(maxWidth - (x += horizontalGap), 0.0);
            TbTableBlockColumn column3 = this.columns.get(i);
            double cellWidth = this.columnWidths.get(i);
            if (leftWidth < cellWidth) {
                widthOverflow = true;
                cellWidth = 0.0;
            }
            column3.setHeaderBounds(g2, new Rectangle2D.Double(x, y, cellWidth, cellHeight));
            x += cellWidth;
        }
        y += cellHeight;
        int rows = this.columns.get(0).getCellsCount();
        for (int row = 0; row < rows; ++row) {
            double leftHeight = heightOverflow ? 0.0 : Math.max(maxHeight - y, 0.0);
            double verticalGap = this.verticalGap;
            if (leftHeight < this.verticalGap) {
                heightOverflow = true;
                verticalGap = 0.0;
            }
            leftHeight = heightOverflow ? 0.0 : Math.max(maxHeight - (y += verticalGap), 0.0);
            cellHeight = this.rowHeight;
            if (leftHeight < this.rowHeight) {
                heightOverflow = true;
                cellHeight = 0.0;
            }
            x = 0.0;
            widthOverflow = false;
            for (int c2 = 0; c2 < this.columns.size(); ++c2) {
                double leftWidth = widthOverflow ? 0.0 : Math.max(maxWidth - x, 0.0);
                double horizontalGap = 0.0;
                if (c2 > 0) {
                    horizontalGap = this.horizontalGap;
                    if (leftWidth < this.horizontalGap) {
                        widthOverflow = true;
                        horizontalGap = 0.0;
                    }
                }
                leftWidth = widthOverflow ? 0.0 : Math.max(maxWidth - (x += horizontalGap), 0.0);
                TbTableBlockColumn column4 = this.columns.get(c2);
                double cellWidth = this.columnWidths.get(c2);
                if (leftWidth < cellWidth) {
                    widthOverflow = true;
                    cellWidth = 0.0;
                }
                column4.setCellBounds(g2, new Rectangle2D.Double(x, y, cellWidth, cellHeight), row);
                x += cellWidth;
            }
            y += cellHeight;
        }
        return new Size2D(x, y);
    }

    public void draw(Graphics2D g2, Rectangle2D area) {
        this.draw(g2, area, null);
    }

    public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
        Rectangle2D target = (Rectangle2D)area.clone();
        for (TbTableBlockColumn column : this.columns) {
            column.draw(g2, target);
        }
        return null;
    }

    public static class TbTableBlockColumn {
        private final BlockContainer header;
        private final List<BlockContainer> cells;
        private final HorizontalAlignment cellAlign;
        private final boolean fitWidth;

        public TbTableBlockColumn(HorizontalAlignment headerAlign, HorizontalAlignment cellAlign, boolean fitWidth) {
            this.cellAlign = cellAlign;
            this.fitWidth = fitWidth;
            this.header = new BlockContainer((Arrangement)new TbFlowArrangement(headerAlign, VerticalAlignment.CENTER, 0.0, 0.0));
            this.cells = new ArrayList<BlockContainer>();
        }

        public void setHeader(Block header) {
            this.header.add(header);
        }

        public void addCell(Block cell) {
            BlockContainer cellContainer = new BlockContainer((Arrangement)new TbFlowArrangement(this.cellAlign, VerticalAlignment.CENTER, 0.0, 0.0));
            cellContainer.add(cell);
            this.cells.add(cellContainer);
        }

        public Size2D[] calculateMaxCellDimensions(Graphics2D g2) {
            Size2D size;
            Size2D[] sizes = new Size2D[2];
            RectangleConstraint constraint = RectangleConstraint.NONE;
            double maxWidth = 0.0;
            double maxHeight = 0.0;
            sizes[0] = size = this.header.arrange(g2, constraint);
            maxWidth = Math.max(maxWidth, size.getWidth());
            maxHeight = Math.max(maxHeight, size.getHeight());
            for (BlockContainer cell : this.cells) {
                size = cell.arrange(g2, constraint);
                maxWidth = Math.max(maxWidth, size.getWidth());
                maxHeight = Math.max(maxHeight, size.getHeight());
            }
            sizes[1] = new Size2D(maxWidth, maxHeight);
            return sizes;
        }

        public int getCellsCount() {
            return this.cells.size();
        }

        public void setHeaderBounds(Graphics2D g2, Rectangle2D bounds) {
            this.header.arrange(g2, new RectangleConstraint(bounds.getWidth(), bounds.getHeight()));
            this.header.setBounds(bounds);
        }

        public void setCellBounds(Graphics2D g2, Rectangle2D bounds, int row) {
            BlockContainer cell = this.cells.get(row);
            cell.arrange(g2, new RectangleConstraint(bounds.getWidth(), bounds.getHeight()));
            cell.setBounds(bounds);
        }

        public boolean fixedWidth() {
            return !this.fitWidth;
        }

        public void draw(Graphics2D g2, Rectangle2D area) {
            double x = area.getX();
            double y = area.getY();
            Rectangle2D headerBounds = this.header.getBounds();
            if (!headerBounds.isEmpty()) {
                Rectangle2D.Double headerArea = new Rectangle2D.Double(x + headerBounds.getX(), y + headerBounds.getY(), headerBounds.getWidth(), headerBounds.getHeight());
                this.header.draw(g2, (Rectangle2D)headerArea);
            }
            for (BlockContainer cell : this.cells) {
                Rectangle2D cellBounds = cell.getBounds();
                if (cellBounds.isEmpty()) continue;
                Rectangle2D.Double cellArea = new Rectangle2D.Double(x + cellBounds.getX(), y + cellBounds.getY(), cellBounds.getWidth(), cellBounds.getHeight());
                cell.draw(g2, (Rectangle2D)cellArea);
            }
        }
    }
}

