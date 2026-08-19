/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.errorprone.annotations.CheckReturnValue
 *  org.xhtmlrenderer.extend.FSImage
 *  org.xhtmlrenderer.extend.Size
 *  org.xhtmlrenderer.pdf.ITextFSImage
 */
package org.thingsboard.server.report.util.itext;

import com.google.errorprone.annotations.CheckReturnValue;
import org.thingsboard.server.report.util.itext.PdfSvgDocument;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.Size;
import org.xhtmlrenderer.pdf.ITextFSImage;

public class PdfSvgImage
extends ITextFSImage {
    private final PdfSvgDocument _svgDocument;
    private byte[] _image;
    private final float dotsPerPixel;
    private final int usablePageWidthPx;

    public PdfSvgImage(PdfSvgDocument svgDocument, float dotsPerPixel, int usablePageWidthPx) {
        this(svgDocument, dotsPerPixel, usablePageWidthPx, new Size((int)(svgDocument.size().width * dotsPerPixel), (int)(svgDocument.size().height * dotsPerPixel)));
    }

    public PdfSvgImage(PdfSvgDocument svgDocument, float dotsPerPixel, int usablePageWidthPx, Size size) {
        super(null, size, null);
        this._svgDocument = svgDocument;
        this.dotsPerPixel = dotsPerPixel;
        this.usablePageWidthPx = usablePageWidthPx;
    }

    @CheckReturnValue
    public FSImage scale(int width, int height) {
        Size newSize = this.size.scale(width, height);
        if (this.size != newSize) {
            return new PdfSvgImage(this._svgDocument, this.dotsPerPixel, this.usablePageWidthPx, newSize);
        }
        return this;
    }

    public byte[] getImage() {
        if (this._image == null) {
            try {
                this._image = this._svgDocument.render((float)this.getWidth() / this.dotsPerPixel, (float)this.getHeight() / this.dotsPerPixel, this.usablePageWidthPx);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return this._image;
    }
}

