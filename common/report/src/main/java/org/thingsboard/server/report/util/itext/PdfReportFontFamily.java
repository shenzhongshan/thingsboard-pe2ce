/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.xhtmlrenderer.css.constants.IdentValue
 *  org.xhtmlrenderer.pdf.FontDescription
 */
package org.thingsboard.server.report.util.itext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.pdf.FontDescription;

public class PdfReportFontFamily {
    private static final int SM_EXACT = 1;
    private static final int SM_LIGHTER_OR_DARKER = 2;
    private static final int SM_DARKER_OR_LIGHTER = 3;
    private final String _name;
    private final List<FontDescription> _fontDescriptions = new ArrayList<FontDescription>();

    PdfReportFontFamily(String name) {
        this._name = name;
    }

    public String getName() {
        return this._name;
    }

    public List<FontDescription> getFontDescriptions() {
        return this._fontDescriptions;
    }

    public void addFontDescription(FontDescription description) {
        this._fontDescriptions.add(description);
        this._fontDescriptions.sort(Comparator.comparingInt(FontDescription::getWeight));
    }

    public FontDescription match(int desiredWeight, IdentValue style) {
        FontDescription result;
        ArrayList<FontDescription> candidates = new ArrayList<FontDescription>();
        for (FontDescription description : this._fontDescriptions) {
            if (description.getStyle() != style) continue;
            candidates.add(description);
        }
        if (candidates.isEmpty()) {
            if (style == IdentValue.ITALIC) {
                return this.match(desiredWeight, IdentValue.OBLIQUE);
            }
            if (style == IdentValue.OBLIQUE) {
                return this.match(desiredWeight, IdentValue.NORMAL);
            }
            candidates.addAll(this._fontDescriptions);
        }
        if ((result = this.findByWeight(candidates, desiredWeight, 1)) != null) {
            return result;
        }
        if (desiredWeight <= 500) {
            return this.findByWeight(candidates, desiredWeight, 2);
        }
        return this.findByWeight(candidates, desiredWeight, 3);
    }

    private FontDescription findByWeight(List<FontDescription> matches, int desiredWeight, int searchMode) {
        if (searchMode == 1) {
            for (FontDescription description : matches) {
                if (description.getWeight() != desiredWeight) continue;
                return description;
            }
            return null;
        }
        if (searchMode == 2) {
            int offset;
            FontDescription description = null;
            for (offset = 0; offset < matches.size() && (description = matches.get(offset)).getWeight() <= desiredWeight; ++offset) {
            }
            if (offset > 0 && description.getWeight() > desiredWeight) {
                return matches.get(offset - 1);
            }
            return description;
        }
        if (searchMode == 3) {
            int offset;
            FontDescription description = null;
            for (offset = matches.size() - 1; offset >= 0 && (description = matches.get(offset)).getWeight() >= desiredWeight; --offset) {
            }
            if (offset != matches.size() - 1 && description != null && description.getWeight() < desiredWeight) {
                return matches.get(offset + 1);
            }
            return description;
        }
        return null;
    }
}

