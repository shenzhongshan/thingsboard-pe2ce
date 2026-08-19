/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.report.configuration.style.Font
 *  org.thingsboard.server.common.data.report.configuration.style.FontStyle
 *  org.thingsboard.server.common.data.report.configuration.style.FontWeight
 */
package org.thingsboard.server.report.util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.report.renderer.chart.font.TbCompositeFont;

public class AwtFontUtils {
    private static final org.thingsboard.server.common.data.report.configuration.style.Font fallbackFont = org.thingsboard.server.common.data.report.configuration.style.Font.builder().family("Roboto").size(Float.valueOf(16.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
    private static final Map<String, Font> fontMap = new ConcurrentHashMap<String, Font>();
    public static final Font ZERO_FONT;

    public static Font toAwtFont(org.thingsboard.server.common.data.report.configuration.style.Font font) {
        return AwtFontUtils.toAwtFont(font, fallbackFont);
    }

    public static Font newFont(String name, int style, float size) {
        String targetFont;
        Font awtFont;
        String family = name;
        if (StringUtils.isBlank((String)family)) {
            family = fallbackFont.getFamily();
        }
        String targetWeight = "";
        String targetStyle = "";
        if (style != 0) {
            if ((style & 1) != 0) {
                targetWeight = "Bold";
            }
            if ((style & 2) != 0) {
                targetStyle = "Italic";
            }
        }
        if ((awtFont = fontMap.get(targetFont = family + targetWeight + targetStyle)) == null) {
            awtFont = fontMap.get("Roboto");
        }
        if (size <= 0.0f) {
            size = fallbackFont.getSize().floatValue();
        }
        return awtFont.deriveFont(size);
    }

    public static Font toAwtFont(org.thingsboard.server.common.data.report.configuration.style.Font font, org.thingsboard.server.common.data.report.configuration.style.Font defaultFont) {
        float size;
        String targetFont;
        Font awtFont;
        FontStyle fontStyle;
        FontWeight weight;
        String family;
        String string = family = font != null ? font.getFamily() : defaultFont.getFamily();
        if (StringUtils.isBlank((String)family)) {
            family = defaultFont.getFamily();
        }
        FontWeight fontWeight = weight = font != null ? font.getWeight() : defaultFont.getWeight();
        if (weight == null) {
            weight = defaultFont.getWeight();
        }
        String targetWeight = "";
        if (weight != null) {
            switch (weight) {
                case NORMAL: {
                    targetWeight = "";
                    break;
                }
                case BOLD: {
                    targetWeight = "Bold";
                    break;
                }
                case WEIGHT_500: {
                    targetWeight = "Medium";
                }
            }
        }
        FontStyle fontStyle2 = fontStyle = font != null ? font.getStyle() : defaultFont.getStyle();
        if (fontStyle == null) {
            fontStyle = defaultFont.getStyle();
        }
        String targetStyle = "";
        if (fontStyle != null) {
            switch (fontStyle) {
                case NORMAL: {
                    targetStyle = "";
                    break;
                }
                case ITALIC: {
                    targetStyle = "Italic";
                }
            }
        }
        if ((awtFont = fontMap.get(targetFont = family + targetWeight + targetStyle)) == null) {
            awtFont = fontMap.get("Roboto");
        }
        if ((size = (font != null && font.getSize() != null ? font.getSize() : defaultFont.getSize()).floatValue()) <= 0.0f) {
            size = defaultFont.getSize().floatValue();
        }
        return awtFont.deriveFont(size);
    }

    private static void registerFonts() {
        LinkedHashMap<String, Font> localMap = new LinkedHashMap<String, Font>();
        localMap.put("RobotoRegular", AwtFontUtils.createFont("/fonts/roboto/Roboto-Regular.ttf"));
        localMap.put("RobotoItalic", AwtFontUtils.createFont("/fonts/roboto/Roboto-Italic.ttf"));
        localMap.put("RobotoMedium", AwtFontUtils.createFont("/fonts/roboto/Roboto-Medium.ttf"));
        localMap.put("RobotoMediumItalic", AwtFontUtils.createFont("/fonts/roboto/Roboto-MediumItalic.ttf"));
        localMap.put("RobotoBold", AwtFontUtils.createFont("/fonts/roboto/Roboto-Bold.ttf"));
        localMap.put("RobotoBoldItalic", AwtFontUtils.createFont("/fonts/roboto/Roboto-BoldItalic.ttf"));
        localMap.put("noto-sansRegular", AwtFontUtils.createFont("/fonts/cjk/NotoSansSC-Regular.ttf"));
        localMap.put("noto-sansItalic", AwtFontUtils.createFont("/fonts/cjk/NotoSans-Italic.ttf"));
        localMap.put("noto-sansMedium", AwtFontUtils.createFont("/fonts/cjk/NotoSansSC-Medium.ttf"));
        localMap.put("noto-sansMediumItalic", AwtFontUtils.createFont("/fonts/cjk/NotoSans-MediumItalic.ttf"));
        localMap.put("noto-sansBold", AwtFontUtils.createFont("/fonts/cjk/NotoSansSC-Bold.ttf"));
        localMap.put("noto-sansBoldItalic", AwtFontUtils.createFont("/fonts/cjk/NotoSans-BoldItalic.ttf"));
        Font notoSansArabic = AwtFontUtils.createFont("/fonts/notoSansArabic/NotoSansArabic-Regular.ttf");
        Font notoSansArabicMedium = AwtFontUtils.createFont("/fonts/notoSansArabic/NotoSansArabic-Medium.ttf");
        Font notoSansArabicBold = AwtFontUtils.createFont("/fonts/notoSansArabic/NotoSansArabic-Bold.ttf");
        localMap.put("noto-sans-arabicRegular", notoSansArabic);
        localMap.put("noto-sans-arabicItalic", notoSansArabic);
        localMap.put("noto-sans-arabicMedium", notoSansArabicMedium);
        localMap.put("noto-sans-arabicMediumItalic", notoSansArabicMedium);
        localMap.put("noto-sans-arabicBold", notoSansArabicBold);
        localMap.put("noto-sans-arabicBoldItalic", notoSansArabicBold);
        Font sansSerif = AwtFontUtils.createFont("/fonts/sansserif/LiberationSans-Regular.ttf");
        Font sansSerifItalic = AwtFontUtils.createFont("/fonts/sansserif/LiberationSans-Italic.ttf");
        localMap.put("sans-serifRegular", sansSerif);
        localMap.put("sans-serifItalic", sansSerifItalic);
        localMap.put("sans-serifMedium", sansSerif);
        localMap.put("sans-serifMediumItalic", sansSerifItalic);
        localMap.put("sans-serifBold", AwtFontUtils.createFont("/fonts/sansserif/LiberationSans-Bold.ttf"));
        localMap.put("sans-serifBoldItalic", AwtFontUtils.createFont("/fonts/sansserif/LiberationSans-BoldItalic.ttf"));
        Font serif = AwtFontUtils.createFont("/fonts/serif/LiberationSerif-Regular.ttf");
        Font serifItalic = AwtFontUtils.createFont("/fonts/serif/LiberationSerif-Italic.ttf");
        localMap.put("serifRegular", serif);
        localMap.put("serifItalic", serifItalic);
        localMap.put("serifMedium", serif);
        localMap.put("serifMediumItalic", serifItalic);
        localMap.put("serifBold", AwtFontUtils.createFont("/fonts/serif/LiberationSerif-Bold.ttf"));
        localMap.put("serifBoldItalic", AwtFontUtils.createFont("/fonts/serif/LiberationSerif-BoldItalic.ttf"));
        Font monospace = AwtFontUtils.createFont("/fonts/monospace/DejaVuSansMono.ttf");
        Font monospaceItalic = AwtFontUtils.createFont("/fonts/monospace/DejaVuSansMono-Oblique.ttf");
        localMap.put("monospaceRegular", monospace);
        localMap.put("monospaceItalic", monospaceItalic);
        localMap.put("monospaceMedium", monospace);
        localMap.put("monospaceMediumItalic", monospaceItalic);
        localMap.put("monospaceBold", AwtFontUtils.createFont("/fonts/monospace/DejaVuSansMono-Bold.ttf"));
        localMap.put("monospaceBoldItalic", AwtFontUtils.createFont("/fonts/monospace/DejaVuSansMono-BoldOblique.ttf"));
        List<String> fontFamilies = Arrays.asList("Roboto", "noto-sans", "noto-sans-arabic", "sans-serif", "serif", "monospace");
        List<String> fontVariants = Arrays.asList("Regular", "Italic", "Medium", "MediumItalic", "Bold", "BoldItalic");
        for (String fontFamily : fontFamilies) {
            for (String fontVariant : fontVariants) {
                AwtFontUtils.registerFont(fontFamily, fontVariant, fontFamilies, localMap);
            }
        }
    }

    private static void registerFont(String fontFamily, String fontVariant, List<String> fontFamilies, Map<String, Font> localMap) {
        String fontName = fontFamily + fontVariant;
        Font font = localMap.get(fontName);
        ArrayList<Font> fallbackFonts = new ArrayList<Font>();
        for (String family : fontFamilies) {
            if (family.equals(fontFamily)) continue;
            String fallBackFontName = family + fontVariant;
            fallbackFonts.add(localMap.get(fallBackFontName));
        }
        TbCompositeFont compositeFont = new TbCompositeFont(font, fallbackFonts);
        String registeredName = fontFamily + (!fontVariant.equals("Regular") ? fontVariant : "");
        fontMap.put(registeredName, compositeFont);
    }

    private static Font createFont(String uri) {
        URL url = AwtFontUtils.class.getResource(uri);
        if (url == null) {
            throw new RuntimeException("Could not find resource " + uri);
        }
        try (InputStream is = url.openStream();){
            Font font2 = Font.createFont(0, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font2);
            return font2;
        }
        catch (Exception e) {
            throw new RuntimeException("Could not read font from uri " + uri, e);
        }
    }

    static {
        AwtFontUtils.registerFonts();
        ZERO_FONT = fontMap.get("Roboto").deriveFont(0.0f);
    }
}

