/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.CharEncoding
 *  org.w3c.tidy.Tidy
 *  org.xhtmlrenderer.extend.FontResolver
 *  org.xhtmlrenderer.extend.ReplacedElementFactory
 *  org.xhtmlrenderer.extend.TextRenderer
 *  org.xhtmlrenderer.pdf.ITextOutputDevice
 *  org.xhtmlrenderer.pdf.ITextRenderer
 *  org.xhtmlrenderer.pdf.ITextUserAgent
 */
package org.thingsboard.server.report.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import org.apache.commons.codec.CharEncoding;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.thingsboard.server.report.util.itext.PdfReplacedElementFactory;
import org.thingsboard.server.report.util.itext.PdfReportFontResolver;
import org.thingsboard.server.report.util.itext.PdfReportTextRenderer;
import org.thingsboard.server.report.util.itext.PdfReportUserAgent;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.extend.FontResolver;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;

public class HtmlRenderUtils {
    private static final PdfReportFontResolver fontResolver = new PdfReportFontResolver();
    private static final TextRenderer textRenderer = new PdfReportTextRenderer(fontResolver.getFallBackFonts());

    public static ITextRenderer createRenderer(ReportDataService dataService, TbReportCtx ctx, int usablePageWidthPx) {
        ITextOutputDevice outputDevice = new ITextOutputDevice(26.666666f);
        PdfReportUserAgent userAgent = new PdfReportUserAgent(dataService, ctx, outputDevice, 20, usablePageWidthPx);
        PdfReplacedElementFactory replacedElementFactory = new PdfReplacedElementFactory();
        return new ITextRenderer(26.666666f, 20, outputDevice, (ITextUserAgent)userAgent, (FontResolver)fontResolver, (ReplacedElementFactory)replacedElementFactory, textRenderer);
    }

    public static Document parseDom(String html) throws UnsupportedEncodingException {
        return HtmlRenderUtils.parseDom(html, null);
    }

    public static Document parseDom(String html, OutputStream out) throws UnsupportedEncodingException {
        Tidy tidy = new Tidy();
        tidy.setInputEncoding(CharEncoding.UTF_8);
        tidy.setOutputEncoding(CharEncoding.UTF_8);
        tidy.setXHTML(true);
        tidy.setTrimEmptyElements(false);
        tidy.setShowWarnings(false);
        tidy.setErrout(new PrintWriter(new ByteArrayOutputStream()));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes(CharEncoding.UTF_8));
        return tidy.parseDOM((InputStream)inputStream, out);
    }

    public static int measureHtmlHeight(ITextRenderer renderer, String htmlContent, int width) throws Exception {
        HashMap<String, Object> variables = new HashMap<String, Object>();
        variables.put("htmlContent", htmlContent);
        variables.put("pageWidth", width + "px");
        variables.put("pageHeight", "1000px");
        String renderedHtmlContent = ThymeleafUtil.renderFromHtmlTemplate("html/measure-template", variables);
        Document document = HtmlRenderUtils.parseDom(renderedHtmlContent);
        renderer.setDocument(document);
        renderer.layout();
        return (int)Math.ceil((double)renderer.getRootBox().getHeight() / 20.0);
    }
}

