/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.lowagie.text.DocumentException
 *  com.lowagie.text.pdf.BaseFont
 *  lombok.Generated
 *  org.jetbrains.annotations.NotNull
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.xhtmlrenderer.css.constants.IdentValue
 *  org.xhtmlrenderer.css.sheet.FontFaceRule
 *  org.xhtmlrenderer.css.value.FontSpecification
 *  org.xhtmlrenderer.extend.UserAgentCallback
 *  org.xhtmlrenderer.layout.SharedContext
 *  org.xhtmlrenderer.pdf.FontDescription
 *  org.xhtmlrenderer.pdf.ITextFSFont
 *  org.xhtmlrenderer.pdf.ITextFontResolver
 *  org.xhtmlrenderer.pdf.TrueTypeUtil
 *  org.xhtmlrenderer.render.FSFont
 *  org.xhtmlrenderer.util.IOUtil
 */
package org.thingsboard.server.report.util.itext;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Generated;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.report.util.itext.PdfReportFontFamily;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.sheet.FontFaceRule;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.FontDescription;
import org.xhtmlrenderer.pdf.ITextFSFont;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.TrueTypeUtil;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.util.IOUtil;

public class PdfReportFontResolver
extends ITextFontResolver {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(PdfReportFontResolver.class);
    private static final Map<String, PdfReportFontFamily> families = new HashMap<String, PdfReportFontFamily>();
    private final Map<String, PdfReportFontFamily> _fontFamilies = new HashMap<String, PdfReportFontFamily>();
    private final Map<String, FontDescription> _fontCache = new ConcurrentHashMap<String, FontDescription>();

    public FSFont resolveFont(@NotNull SharedContext renderingContext, FontSpecification spec) {
        return this.resolveFont(spec.families, spec.size, spec.fontWeight, spec.fontStyle);
    }

    public void flushCache() {
    }

    public void flushFontFaceFonts() {
    }

    public void importFontFaces(List<FontFaceRule> fontFaces, UserAgentCallback userAgentCallback) {
    }

    private FSFont resolveFont(String[] families, float size, IdentValue weight, IdentValue style) {
        if (style != IdentValue.NORMAL && style != IdentValue.OBLIQUE && style != IdentValue.ITALIC) {
            style = IdentValue.NORMAL;
        }
        if (families != null) {
            for (String family : families) {
                FSFont font = this.resolveFont(family, size, weight, style);
                if (font == null) continue;
                log.debug("Resolved font {}:{}:{} -> {}", new Object[]{family, weight, style, font});
                return font;
            }
        }
        log.debug("Could not resolve font {}:{}:{} - fallback to Roboto", new Object[]{Arrays.toString(families), weight, style});
        return this.resolveFont("Roboto", size, weight, style);
    }

    private FSFont resolveFont(String fontFamily, float size, IdentValue weight, IdentValue style) {
        String normalizedFontFamily = this.stripQuotes(fontFamily);
        String cacheKey = String.format("%s-%s-%s", normalizedFontFamily, weight, style);
        FontDescription result = this._fontCache.get(cacheKey);
        if (result != null) {
            log.debug("Resolved font {}:{}:{} -> {}", new Object[]{fontFamily, weight, style, result});
            return new ITextFSFont(result, size);
        }
        PdfReportFontFamily family = this.getPdfReportFonts().get(normalizedFontFamily);
        if (family != null && (result = family.match(ITextFontResolver.convertWeightToInt((IdentValue)weight), style)) != null) {
            this._fontCache.put(cacheKey, result);
            return new ITextFSFont(result, size);
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Map<String, PdfReportFontFamily> getPdfReportFonts() {
        if (this._fontFamilies.isEmpty()) {
            Map<String, PdfReportFontFamily> map = this._fontFamilies;
            synchronized (map) {
                if (this._fontFamilies.isEmpty()) {
                    this._fontFamilies.putAll(this.loadPdfReportFonts());
                }
            }
        }
        return this._fontFamilies;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Map<String, PdfReportFontFamily> loadPdfReportFonts() {
        if (families.isEmpty()) {
            Map<String, PdfReportFontFamily> map = families;
            synchronized (map) {
                this.addRoboto();
                this.addNotoSans();
                this.addMonospace();
                this.addSansSerif();
                this.addSerif();
                this.addNotoSansArabic();
            }
        }
        return families;
    }

    private void addRoboto() {
        PdfReportFontFamily roboto = new PdfReportFontFamily("Roboto");
        this.loadFont(roboto, "/fonts/roboto/Roboto-Regular.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.NORMAL);
        this.loadFont(roboto, "/fonts/roboto/Roboto-Italic.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.ITALIC);
        this.loadFont(roboto, "/fonts/roboto/Roboto-Medium.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        this.loadFont(roboto, "/fonts/roboto/Roboto-MediumItalic.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        this.loadFont(roboto, "/fonts/roboto/Roboto-Bold.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.NORMAL);
        this.loadFont(roboto, "/fonts/roboto/Roboto-BoldItalic.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("Roboto", roboto);
    }

    private void addNotoSans() {
        PdfReportFontFamily notoSans = new PdfReportFontFamily("noto-sans");
        this.loadFont(notoSans, "/fonts/cjk/NotoSansSC-Regular.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.NORMAL);
        this.loadFont(notoSans, "/fonts/cjk/NotoSans-Italic.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.ITALIC);
        this.loadFont(notoSans, "/fonts/cjk/NotoSansSC-Medium.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        this.loadFont(notoSans, "/fonts/cjk/NotoSans-MediumItalic.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        this.loadFont(notoSans, "/fonts/cjk/NotoSansSC-Bold.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.NORMAL);
        this.loadFont(notoSans, "/fonts/cjk/NotoSans-BoldItalic.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("noto-sans", notoSans);
    }

    private void addMonospace() {
        PdfReportFontFamily monospace = new PdfReportFontFamily("monospace");
        this.loadFont(monospace, "/fonts/monospace/DejaVuSansMono.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.NORMAL);
        this.loadFont(monospace, "/fonts/monospace/DejaVuSansMono-Oblique.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.ITALIC);
        this.loadFont(monospace, "/fonts/monospace/DejaVuSansMono.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        this.loadFont(monospace, "/fonts/monospace/DejaVuSansMono-Oblique.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        this.loadFont(monospace, "/fonts/monospace/DejaVuSansMono-Bold.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.NORMAL);
        this.loadFont(monospace, "/fonts/monospace/DejaVuSansMono-BoldOblique.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("monospace", monospace);
    }

    private void addSansSerif() {
        PdfReportFontFamily sansSerif = new PdfReportFontFamily("sans-serif");
        this.loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Regular.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.NORMAL);
        this.loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Italic.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.ITALIC);
        this.loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Regular.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        this.loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Italic.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        this.loadFont(sansSerif, "/fonts/sansserif/LiberationSans-Bold.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.NORMAL);
        this.loadFont(sansSerif, "/fonts/sansserif/LiberationSans-BoldItalic.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("sans-serif", sansSerif);
    }

    private void addSerif() {
        PdfReportFontFamily serif = new PdfReportFontFamily("serif");
        this.loadFont(serif, "/fonts/serif/LiberationSerif-Regular.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.NORMAL);
        this.loadFont(serif, "/fonts/serif/LiberationSerif-Italic.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.ITALIC);
        this.loadFont(serif, "/fonts/serif/LiberationSerif-Regular.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        this.loadFont(serif, "/fonts/serif/LiberationSerif-Italic.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.ITALIC);
        this.loadFont(serif, "/fonts/serif/LiberationSerif-Bold.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.NORMAL);
        this.loadFont(serif, "/fonts/serif/LiberationSerif-BoldItalic.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.ITALIC);
        families.put("serif", serif);
    }

    private void addNotoSansArabic() {
        PdfReportFontFamily arabic = new PdfReportFontFamily("noto-sans-arabic");
        this.loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-Regular.ttf", "Identity-H", true, IdentValue.NORMAL, IdentValue.NORMAL);
        this.loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-Medium.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        this.loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-Bold.ttf", "Identity-H", true, IdentValue.BOLD, IdentValue.NORMAL);
        this.loadFont(arabic, "/fonts/notoSansArabic/NotoSansArabic-SemiBold.ttf", "Identity-H", true, IdentValue.FONT_WEIGHT_500, IdentValue.NORMAL);
        families.put("noto-sans-arabic", arabic);
    }

    private void loadFont(PdfReportFontFamily family, String uri, String encoding, boolean embedded, IdentValue fontWeightOverride, IdentValue fontStyleOverride) {
        try {
            byte[] ttfAfm = this.getBinaryResource(uri);
            BaseFont font = BaseFont.createFont((String)uri, (String)encoding, (boolean)embedded, (boolean)false, (byte[])ttfAfm, null);
            family.addFontDescription(TrueTypeUtil.extractDescription((String)uri, (byte[])ttfAfm, (BaseFont)font, (boolean)true, (IdentValue)fontWeightOverride, (IdentValue)fontStyleOverride));
        }
        catch (DocumentException | IOException e) {
            log.warn("Could not load font " + uri, e);
        }
    }

    private String stripQuotes(String text) {
        String result = text;
        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private byte[] getBinaryResource(String uri) throws IOException {
        byte[] byArray;
        block9: {
            URL url = PdfReportFontResolver.class.getResource(uri);
            if (url == null) {
                throw new IOException("Could not find resource " + uri);
            }
            InputStream is = url.openStream();
            try {
                byte[] byArray2 = byArray = is == null ? null : IOUtil.readBytes((InputStream)is);
                if (is == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    throw new IOException("Could not read resource " + uri, e);
                }
            }
            is.close();
        }
        return byArray;
    }

    public List<FontDescription> getFallBackFonts() {
        List<String> fallbackFamilies = Arrays.asList("Roboto", "noto-sans", "noto-sans-arabic", "sans-serif", "serif", "monospace");
        ArrayList<FontDescription> fallbacks = new ArrayList<FontDescription>(fallbackFamilies.size());
        for (String family : fallbackFamilies) {
            PdfReportFontFamily f = this.getPdfReportFonts().get(family);
            if (f == null) continue;
            for (FontDescription fd : f.getFontDescriptions()) {
                if (fallbacks.contains(fd)) continue;
                fallbacks.add(fd);
            }
        }
        return fallbacks;
    }
}

