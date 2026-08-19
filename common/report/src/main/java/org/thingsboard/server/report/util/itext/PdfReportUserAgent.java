/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.lowagie.text.BadElementException
 *  com.lowagie.text.Rectangle
 *  com.lowagie.text.pdf.PdfReader
 *  org.apache.commons.lang3.StringUtils
 *  org.thingsboard.common.util.SsrfProtectionValidator
 *  org.xhtmlrenderer.extend.FSImage
 *  org.xhtmlrenderer.pdf.ITextFSImage
 *  org.xhtmlrenderer.pdf.ITextOutputDevice
 *  org.xhtmlrenderer.pdf.ITextUserAgent
 *  org.xhtmlrenderer.pdf.PDFAsImage
 *  org.xhtmlrenderer.resource.ImageResource
 *  org.xhtmlrenderer.util.ContentTypeDetectingInputStreamWrapper
 *  org.xhtmlrenderer.util.IOUtil
 *  org.xhtmlrenderer.util.ImageUtil
 *  org.xhtmlrenderer.util.XRLog
 */
package org.thingsboard.server.report.util.itext;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.report.util.ImageUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.thingsboard.server.report.util.itext.PdfSvgDocument;
import org.thingsboard.server.report.util.itext.PdfSvgImage;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.pdf.PDFAsImage;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.util.ContentTypeDetectingInputStreamWrapper;
import org.xhtmlrenderer.util.IOUtil;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;

public class PdfReportUserAgent
extends ITextUserAgent {
    private final TbReportCtx _ctx;
    private final ReportDataService _dataService;
    private final ITextOutputDevice _outputDevice;
    private final int dotsPerPixel;
    private final int usablePageWidthPx;

    public PdfReportUserAgent(ReportDataService dataService, TbReportCtx ctx, ITextOutputDevice outputDevice, int dotsPerPixel, int usablePageWidthPx) {
        super(outputDevice, dotsPerPixel);
        this._dataService = dataService;
        this._ctx = ctx;
        this._outputDevice = outputDevice;
        this.dotsPerPixel = dotsPerPixel;
        this.usablePageWidthPx = usablePageWidthPx;
    }

    public String resolveURI(String uri) {
        return uri;
    }

    public ImageResource getImageResource(String uriStr) {
        ImageResource resource;
        String unresolvedUri = uriStr = StringUtils.removeStart((String)uriStr, (String)"tb-image;");
        if (!(ImageUtil.isEmbeddedBase64Image((String)uriStr) || ImageUtils.isTbImage(uriStr) || ImageUtils.isEmptyImage(uriStr))) {
            uriStr = this.resolveURI(uriStr);
        }
        if ((resource = (ImageResource)this._imageCache.get(unresolvedUri)) == null) {
            resource = this.loadImageResource(uriStr);
            this._imageCache.put(unresolvedUri, resource);
        }
        if (resource != null) {
            FSImage image = resource.getImage();
            return new ImageResource(resource.getImageUri(), image);
        }
        return new ImageResource(uriStr, null);
    }

    protected InputStream resolveAndOpenStream(String uri) {
        InputStream is = null;
        URL url = PdfReportUserAgent.class.getResource(uri);
        if (url == null) {
            if (uri.startsWith("/assets/")) {
                url = PdfReportUserAgent.class.getResource("/public" + uri);
            }
            if (url == null) {
                try {
                    URI parsedUri = new URI(uri);
                    String scheme = parsedUri.getScheme();
                    if (scheme != null && !scheme.equalsIgnoreCase("jar")) {
                        SsrfProtectionValidator.validateUri((URI)parsedUri);
                    }
                }
                catch (URISyntaxException e) {
                    XRLog.exception((String)("Invalid URI: " + uri), (Throwable)e);
                    throw new PdfReportImageException(uri, "Invalid URI syntax", null, e);
                }
                catch (RuntimeException e) {
                    XRLog.exception((String)e.getMessage());
                    throw new PdfReportImageException(uri, "URI is invalid", null, e);
                }
                return super.resolveAndOpenStream(uri);
            }
        }
        try {
            is = url.openStream();
        }
        catch (MalformedURLException e) {
            XRLog.exception((String)("bad URL given: " + uri), (Throwable)e);
        }
        catch (FileNotFoundException e) {
            XRLog.exception((String)("item at URI " + uri + " not found"));
        }
        catch (IOException e) {
            XRLog.exception((String)("IO problem for " + uri), (Throwable)e);
        }
        return is;
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private ImageResource loadImageResource(String uriStr) {
        block19: {
            String originalUri = uriStr;
            uriStr = ImageUtil.isEmbeddedBase64Image((String)uriStr) ? "Base64 Data" : uriStr;
            try (InputStream is = this.resolveImageStream(originalUri);){
                ImageResource imageResource;
                block24: {
                    ContentTypeDetectingInputStreamWrapper cis;
                    block22: {
                        ImageResource reader2;
                        block23: {
                            block20: {
                                ImageResource imageResource2;
                                block21: {
                                    if (is == null) break block19;
                                    cis = ContentTypeDetectingInputStreamWrapper.detectContentType((InputStream)is);
                                    try {
                                        if (!cis.isPdf()) break block20;
                                        URI uri = new URI(uriStr);
                                        PdfReader pdfReader = this._outputDevice.getReader(uri);
                                        Rectangle rect = pdfReader.getPageSizeWithRotation(1);
                                        float initialWidth = rect.getWidth() * this._outputDevice.getDotsPerPoint();
                                        float initialHeight = rect.getHeight() * this._outputDevice.getDotsPerPoint();
                                        PDFAsImage image = new PDFAsImage(uri, initialWidth, initialHeight);
                                        imageResource2 = new ImageResource(uriStr, (FSImage)image);
                                        if (cis == null) break block21;
                                    }
                                    catch (Throwable throwable) {
                                        if (cis != null) {
                                            try {
                                                cis.close();
                                            }
                                            catch (Throwable throwable2) {
                                                throwable.addSuppressed(throwable2);
                                            }
                                        }
                                        throw throwable;
                                    }
                                    cis.close();
                                }
                                return imageResource2;
                            }
                            if (!cis.isSvg()) break block22;
                            PdfSvgImage image = this.readSvg(uriStr, (InputStream)cis);
                            reader2 = new ImageResource(uriStr, (FSImage)image);
                            if (cis == null) break block23;
                            cis.close();
                        }
                        return reader2;
                    }
                    byte[] image = IOUtil.readBytes((InputStream)cis);
                    ITextFSImage itextImage = new ITextFSImage(image, ImageUtils.getOriginalImageSize(image, this.dotsPerPixel), uriStr);
                    imageResource = new ImageResource(uriStr, (FSImage)itextImage);
                    if (cis == null) break block24;
                    cis.close();
                }
                return imageResource;
            }
            catch (BadElementException | IOException | URISyntaxException e) {
                XRLog.exception((String)("Can't read image file; unexpected problem for URI '" + uriStr + "'"), (Throwable)e);
                return this.errorImageResource(uriStr, "Can't read image file", null, e);
            }
            catch (PdfReportImageException e) {
                return this.errorImageResource(e);
            }
        }
        return this.errorLoadImageFromUriResource(uriStr, null);
    }

    private InputStream resolveImageStream(String uriStr) throws PdfReportImageException {
        if (ImageUtil.isEmbeddedBase64Image((String)uriStr)) {
            try {
                byte[] buffer = ImageUtil.getEmbeddedBase64Image((String)uriStr);
                return new ByteArrayInputStream(buffer);
            }
            catch (BadElementException e) {
                XRLog.exception((String)"Can't read XHTML embedded image.", (Throwable)e);
                throw this.errorLoadImageFromBase64Exception((Exception)((Object)e));
            }
        }
        if (ImageUtils.isTbImage(uriStr)) {
            try {
                byte[] imageData = null;
                if (ImageUtils.isInternalTbImage(uriStr)) {
                    imageData = this.loadInternalTbImage(uriStr);
                } else if (ImageUtils.isPublicTbImage(uriStr)) {
                    imageData = this.loadPublicTbImage(uriStr);
                }
                if (imageData != null) {
                    return new ByteArrayInputStream(imageData);
                }
            }
            catch (Exception e) {
                XRLog.exception((String)"Can't read TB image.", (Throwable)e);
                throw this.errorLoadTbImageException(uriStr, e);
            }
            throw this.errorLoadTbImageException(uriStr, null);
        }
        if (ImageUtils.isEmptyImage(uriStr)) {
            throw this.errorEmptyImageException(uriStr);
        }
        return this.resolveAndOpenStream(uriStr);
    }

    private PdfSvgImage readSvg(String uri, InputStream in) throws IOException, PdfReportImageException {
        byte[] svgBytes = IOUtil.readBytes((InputStream)in);
        PdfSvgDocument svgDocument = PdfSvgDocument.fromSvgBytes(svgBytes);
        if (svgDocument == null) {
            throw new PdfReportImageException(uri, "Could not load image from SVG.", null, null);
        }
        return new PdfSvgImage(svgDocument, this.dotsPerPixel, this.usablePageWidthPx);
    }

    private PdfReportImageException errorLoadImageFromBase64Exception(Exception exception) {
        return new PdfReportImageException(null, "Failed to load image from base64 data", "Base64 Data", exception);
    }

    private PdfReportImageException errorLoadTbImageException(String uri, Exception exception) {
        return new PdfReportImageException(uri, "Can't read TB image.", null, exception);
    }

    private PdfReportImageException errorEmptyImageException(String uri) {
        return new PdfReportImageException(uri, "Image uri is empty.", "URI is empty", null);
    }

    private ImageResource errorLoadImageFromUriResource(String uri, Exception exception) {
        return this.errorImageResource(uri, "Failed to load image.", null, exception);
    }

    private ImageResource errorImageResource(PdfReportImageException exception) {
        return this.errorImageResource(exception.uri, exception.getMessage(), exception.uriString, exception.getCause());
    }

    private ImageResource errorImageResource(String uri, String errorMessage, String uriString, Throwable exception) {
        HashMap<String, Object> errorImageVariables = new HashMap<String, Object>();
        errorImageVariables.put("errorMessage", errorMessage);
        if (uriString == null) {
            uriString = uri;
        }
        errorImageVariables.put("uri", uriString);
        if (exception != null) {
            errorImageVariables.put("exception", exception.getMessage());
            errorImageVariables.put("uriPos", 11);
        } else {
            errorImageVariables.put("uriPos", 16);
        }
        String errorImageSvg = ThymeleafUtil.renderFromSvgTemplate("svg/error-image", errorImageVariables);
        PdfSvgDocument svgDocument = PdfSvgDocument.fromSvgString(errorImageSvg);
        PdfSvgImage image = new PdfSvgImage(svgDocument, this.dotsPerPixel, this.usablePageWidthPx);
        return new ImageResource(uri, (FSImage)image);
    }

    private byte[] loadInternalTbImage(String uri) throws Exception {
        String[] parts;
        String imageType = null;
        if (uri.startsWith("/api/images/tenant/")) {
            imageType = "tenant";
        } else if (uri.startsWith("/api/images/system/")) {
            imageType = "system";
        }
        if (imageType != null && (parts = uri.split("/")).length >= 5) {
            String key = parts[4];
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            return this._dataService.downloadImage(imageType, key, this._ctx);
        }
        return null;
    }

    private byte[] loadPublicTbImage(String uri) throws Exception {
        String[] parts = uri.split("/");
        if (parts.length >= 5) {
            String publicKey = parts[4];
            return this._dataService.downloadPublicImage(publicKey, this._ctx);
        }
        return null;
    }

    static class PdfReportImageException
    extends RuntimeException {
        private final String uri;
        private final String uriString;

        PdfReportImageException(String uri, String errorMessage, String uriString, Exception exception) {
            super(errorMessage, exception);
            this.uri = uri;
            this.uriString = uriString;
        }
    }
}

