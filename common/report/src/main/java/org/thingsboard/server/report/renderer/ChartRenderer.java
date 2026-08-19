/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.JFreeChart
 *  org.thingsboard.server.common.data.report.configuration.components.AbstractChartComponent
 *  org.thingsboard.server.common.data.report.configuration.image.ImageWidthType
 */
package org.thingsboard.server.report.renderer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.jfree.chart.JFreeChart;
import org.thingsboard.server.common.data.report.configuration.components.AbstractChartComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.AbstractImageRenderer;
import org.thingsboard.server.report.renderer.chart.graphics.TbGraphics2D;

public abstract class ChartRenderer<C extends AbstractChartComponent>
extends AbstractImageRenderer<C> {
    private int width;
    private int height;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected String getImageUrl(C component, ComponentData reportDataSource) {
        this.width = ImageWidthType.CUSTOM == component.getWidthType() ? (component.getCustomWidth() >= 1 ? component.getCustomWidth() : 100) : this.layoutWidthPx;
        this.height = component.getHeight() >= 1 ? component.getHeight() : 400;
        int pixelDensity = 4;
        int imageWidth = this.width * pixelDensity;
        int imageHeight = this.height * pixelDensity;
        BufferedImage highResImage = new BufferedImage(imageWidth, imageHeight, 2);
        TbGraphics2D g2 = new TbGraphics2D(highResImage.createGraphics());
        ((Graphics2D)g2).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ((Graphics2D)g2).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        ((Graphics2D)g2).scale(pixelDensity, pixelDensity);
        try {
            JFreeChart chart = this.createChart(g2, component, reportDataSource);
            chart.draw((Graphics2D)g2, (Rectangle2D)new Rectangle(0, 0, this.width, this.height));
        }
        finally {
            ((Graphics)g2).dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write((RenderedImage)highResImage, "png", baos);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] imageData = baos.toByteArray();
        return this.encodeImage(imageData);
    }

    protected int getWidth() {
        return this.width;
    }

    protected int getHeight() {
        return this.height;
    }

    protected abstract JFreeChart createChart(Graphics2D var1, C var2, ComponentData var3);

    private String encodeImage(byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }
}

