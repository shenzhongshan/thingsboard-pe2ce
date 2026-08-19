/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.ibm.icu.text.ArabicShaping
 *  com.ibm.icu.text.Bidi
 *  com.lowagie.text.pdf.BaseFont
 *  org.xhtmlrenderer.css.constants.IdentValue
 *  org.xhtmlrenderer.extend.FontContext
 *  org.xhtmlrenderer.extend.OutputDevice
 *  org.xhtmlrenderer.pdf.FontDescription
 *  org.xhtmlrenderer.pdf.ITextFSFont
 *  org.xhtmlrenderer.pdf.ITextFSFontMetrics
 *  org.xhtmlrenderer.pdf.ITextOutputDevice
 *  org.xhtmlrenderer.pdf.ITextTextRenderer
 *  org.xhtmlrenderer.render.FSFont
 *  org.xhtmlrenderer.render.FSFontMetrics
 *  org.xhtmlrenderer.render.JustificationInfo
 */
package org.thingsboard.server.report.util.itext;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.lowagie.text.pdf.BaseFont;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.extend.FontContext;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.pdf.FontDescription;
import org.xhtmlrenderer.pdf.ITextFSFont;
import org.xhtmlrenderer.pdf.ITextFSFontMetrics;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextTextRenderer;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.JustificationInfo;

public class PdfReportTextRenderer
extends ITextTextRenderer {
    private static final float TEXT_MEASURING_DELTA = 0.01f;
    private final Map<Integer, Map<IdentValue, List<FontDescription>>> fallbacksMap = new HashMap<Integer, Map<IdentValue, List<FontDescription>>>();

    public PdfReportTextRenderer(List<FontDescription> fallbackFonts) {
        if (fallbackFonts != null) {
            for (FontDescription fontDescription : fallbackFonts) {
                Map<IdentValue, List<FontDescription>> fdByStyle = this.fallbacksMap.computeIfAbsent(fontDescription.getWeight(), k -> new HashMap());
                fdByStyle.computeIfAbsent(fontDescription.getStyle(), k -> new ArrayList()).add(fontDescription);
            }
        }
    }

    public FSFontMetrics getFSFontMetrics(FontContext context, FSFont font, String string) {
        FontDescription description = ((ITextFSFont)font).getFontDescription();
        BaseFont bf = description.getFont();
        float size = font.getSize2D();
        float strikethroughThickness = description.getYStrikeoutSize() != 0.0f ? description.getYStrikeoutSize() / 1000.0f * size : size / 12.0f;
        return new ITextFSFontMetrics(bf.getFontDescriptor(9, size) + bf.getFontDescriptor(11, size), -bf.getFontDescriptor(10, size), -description.getYStrikeoutPosition() / 1000.0f * size, strikethroughThickness, -description.getUnderlinePosition() / 1000.0f * size, description.getUnderlineThickness() / 1000.0f * size);
    }

    public void drawString(OutputDevice outputDevice, String s, float x, float y) {
        this.drawString(outputDevice, s, x, y, null);
    }

    public void drawString(OutputDevice outputDevice, String s, float x, float y, JustificationInfo info) {
        ITextOutputDevice iod = (ITextOutputDevice)outputDevice;
        FSFont fSFont = iod.getSharedContext().getFont(iod.getFontSpecification());
        if (!(fSFont instanceof ITextFSFont)) {
            iod.drawString(s, x, y, info);
            return;
        }
        ITextFSFont curFont = (ITextFSFont)fSFont;
        String vis = PdfReportTextRenderer.shapeAndReorderLTRParagraph(s);
        FontDescription primary = curFont.getFontDescription();
        float size = curFont.getSize2D();
        List<FontDescription> candidates = this.prepareFontCandidates(primary);
        List<Run> runs = PdfReportTextRenderer.shapeRunsByBaseFont(vis, candidates, size);
        float cursor = x;
        for (Run r : runs) {
            ITextFSFont runFsFont = new ITextFSFont(r.fd, size);
            iod.setFont((FSFont)runFsFont);
            iod.drawString(r.text, cursor, y, info);
            cursor += r.widthPt;
        }
        iod.setFont((FSFont)curFont);
    }

    public int getWidth(FontContext context, FSFont font, String string) {
        if (font instanceof ITextFSFont) {
            ITextFSFont curFont = (ITextFSFont)font;
            FontDescription primary = curFont.getFontDescription();
            float size = curFont.getSize2D();
            List<FontDescription> candidates = this.prepareFontCandidates(primary);
            String vis = PdfReportTextRenderer.shapeAndReorderLTRParagraph(string);
            List<Run> runs = PdfReportTextRenderer.shapeRunsByBaseFont(vis, candidates, size);
            float result = 0.0f;
            for (Run r : runs) {
                result += r.widthPt;
            }
            if ((double)result - Math.floor(result) < (double)0.01f) {
                return (int)result;
            }
            return (int)Math.ceil(result);
        }
        return super.getWidth(context, font, string);
    }

    private List<FontDescription> prepareFontCandidates(FontDescription primary) {
        ArrayList<FontDescription> candidates = new ArrayList<FontDescription>();
        candidates.add(primary);
        candidates.addAll((Collection)this.fallbacksMap.get(primary.getWeight()).get(primary.getStyle()));
        return candidates;
    }

    private static String shapeAndReorderLTRParagraph(String logical) {
        try {
            int shapeFlags = 524296;
            String shaped = new ArabicShaping(shapeFlags).shape(logical);
            Bidi bidi = new Bidi(shaped, 0);
            int opts = 6;
            return bidi.writeReordered(opts);
        }
        catch (Exception e) {
            return logical;
        }
    }

    private static List<Run> shapeRunsByBaseFont(String s, List<FontDescription> candidates, float size) {
        ArrayList<Run> out = new ArrayList<Run>();
        if (s.isEmpty()) {
            return out;
        }
        int i = 0;
        int len = s.length();
        int cp = s.codePointAt(0);
        FontDescription cur = PdfReportTextRenderer.pick(candidates, cp);
        int start = 0;
        for (i = Character.charCount(cp); i < len; i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            FontDescription fd = PdfReportTextRenderer.pick(candidates, cp);
            if (fd == cur) continue;
            String slice = s.substring(start, i);
            out.add(PdfReportTextRenderer.run(slice, cur, size));
            cur = fd;
            start = i;
        }
        out.add(PdfReportTextRenderer.run(s.substring(start), cur, size));
        return out;
    }

    private static FontDescription pick(List<FontDescription> candidates, int codePoint) {
        for (FontDescription fd : candidates) {
            if (!fd.getFont().charExists(codePoint)) continue;
            return fd;
        }
        return candidates.get(0);
    }

    private static Run run(String text, FontDescription fd, float size) {
        float w = fd.getFont().getWidthPointKerned(text, size);
        return new Run(text, fd, w);
    }

    private record Run(String text, FontDescription fd, float widthPt) {
    }
}

