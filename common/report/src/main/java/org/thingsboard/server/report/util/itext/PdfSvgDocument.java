/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.github.weisj.jsvg.SVGDocument
 *  com.github.weisj.jsvg.SVGRenderingHints
 *  com.github.weisj.jsvg.attributes.ViewBox
 *  com.github.weisj.jsvg.geometry.size.FloatSize
 *  com.github.weisj.jsvg.parser.DefaultParserProvider
 *  com.github.weisj.jsvg.parser.DomProcessor
 *  com.github.weisj.jsvg.parser.LoaderContext
 *  com.github.weisj.jsvg.parser.ParserProvider
 *  com.github.weisj.jsvg.parser.SVGLoader
 */
package org.thingsboard.server.report.util.itext;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.SVGRenderingHints;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.DefaultParserProvider;
import com.github.weisj.jsvg.parser.DomProcessor;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.ParserProvider;
import com.github.weisj.jsvg.parser.SVGLoader;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class PdfSvgDocument {
    private final SVGDocument _document;
    private final ViewBox _viewBox;

    public static PdfSvgDocument fromSvgString(String svgString) {
        return PdfSvgDocument.fromSvgBytes(svgString.getBytes(StandardCharsets.UTF_8));
    }

    public static PdfSvgDocument fromSvgBytes(byte[] svgBytes) {
        SVGLoader loader = new SVGLoader();
        final AtomicReference viewBoxRef = new AtomicReference();
        DefaultParserProvider parserProvider = new DefaultParserProvider(){

            public DomProcessor createPreProcessor() {
                return root -> viewBoxRef.set(root.attributeNode().getViewBox());
            }
        };
        SVGDocument document = loader.load((InputStream)new ByteArrayInputStream(svgBytes), null, LoaderContext.builder().parserProvider((ParserProvider)parserProvider).build());
        if (document != null) {
            return new PdfSvgDocument(document, (ViewBox)viewBoxRef.get());
        }
        return null;
    }

    public PdfSvgDocument(SVGDocument document, ViewBox viewBox) {
        this._document = document;
        this._viewBox = viewBox;
    }

    public FloatSize size() {
        return this._document.size();
    }

    public byte[] render(float targetWidth, float targetHeight, int usablePageWidthPx) throws Exception {
        ViewBox targetViewBox = this._viewBox != null ? new ViewBox(this._viewBox.x, this._viewBox.y, targetWidth, targetHeight) : new ViewBox(0.0f, 0.0f, this.size().width, this.size().height);
        if (targetViewBox.width != (float)usablePageWidthPx) {
            float scale = (float)usablePageWidthPx / targetViewBox.width;
            targetViewBox.width = usablePageWidthPx;
            targetViewBox.height *= scale;
            targetViewBox.x *= scale;
            targetViewBox.y *= scale;
        }
        BufferedImage image = new BufferedImage((int)targetViewBox.width, (int)targetViewBox.height, 2);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING, SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON);
        graphics.setRenderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING, SVGRenderingHints.VALUE_SOFT_CLIPPING_ON);
        this._document.render((Component)null, graphics, targetViewBox);
        graphics.dispose();
        return PdfSvgDocument.toCompressedPngData(image);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static byte[] toCompressedPngData(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        ImageWriter writer = ImageIO.getImageWriters(type, "png").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(2);
            param.setCompressionQuality(0.0f);
        }
        ImageOutputStream output = ImageIO.createImageOutputStream(out);
        writer.setOutput(output);
        try {
            writer.write(null, new IIOImage(image, null, null), param);
        }
        finally {
            writer.dispose();
            output.flush();
        }
        return out.toByteArray();
    }
}

