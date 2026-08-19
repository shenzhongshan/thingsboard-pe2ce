/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.context;

import lombok.Generated;

public class HeaderFooterRenderLayout {
    private String htmlContent;
    private String firstPageHtmlContent;
    private boolean enabled;
    private boolean firstPageEnabled;
    private int heightPx;
    private int firstPageHeightPx;

    @Generated
    public HeaderFooterRenderLayout() {
    }

    @Generated
    public String getHtmlContent() {
        return this.htmlContent;
    }

    @Generated
    public String getFirstPageHtmlContent() {
        return this.firstPageHtmlContent;
    }

    @Generated
    public boolean isEnabled() {
        return this.enabled;
    }

    @Generated
    public boolean isFirstPageEnabled() {
        return this.firstPageEnabled;
    }

    @Generated
    public int getHeightPx() {
        return this.heightPx;
    }

    @Generated
    public int getFirstPageHeightPx() {
        return this.firstPageHeightPx;
    }

    @Generated
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    @Generated
    public void setFirstPageHtmlContent(String firstPageHtmlContent) {
        this.firstPageHtmlContent = firstPageHtmlContent;
    }

    @Generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setFirstPageEnabled(boolean firstPageEnabled) {
        this.firstPageEnabled = firstPageEnabled;
    }

    @Generated
    public void setHeightPx(int heightPx) {
        this.heightPx = heightPx;
    }

    @Generated
    public void setFirstPageHeightPx(int firstPageHeightPx) {
        this.firstPageHeightPx = firstPageHeightPx;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HeaderFooterRenderLayout)) {
            return false;
        }
        HeaderFooterRenderLayout other = (HeaderFooterRenderLayout)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isEnabled() != other.isEnabled()) {
            return false;
        }
        if (this.isFirstPageEnabled() != other.isFirstPageEnabled()) {
            return false;
        }
        if (this.getHeightPx() != other.getHeightPx()) {
            return false;
        }
        if (this.getFirstPageHeightPx() != other.getFirstPageHeightPx()) {
            return false;
        }
        String this$htmlContent = this.getHtmlContent();
        String other$htmlContent = other.getHtmlContent();
        if (this$htmlContent == null ? other$htmlContent != null : !this$htmlContent.equals(other$htmlContent)) {
            return false;
        }
        String this$firstPageHtmlContent = this.getFirstPageHtmlContent();
        String other$firstPageHtmlContent = other.getFirstPageHtmlContent();
        return !(this$firstPageHtmlContent == null ? other$firstPageHtmlContent != null : !this$firstPageHtmlContent.equals(other$firstPageHtmlContent));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HeaderFooterRenderLayout;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isEnabled() ? 79 : 97);
        result = result * 59 + (this.isFirstPageEnabled() ? 79 : 97);
        result = result * 59 + this.getHeightPx();
        result = result * 59 + this.getFirstPageHeightPx();
        String $htmlContent = this.getHtmlContent();
        result = result * 59 + ($htmlContent == null ? 43 : $htmlContent.hashCode());
        String $firstPageHtmlContent = this.getFirstPageHtmlContent();
        result = result * 59 + ($firstPageHtmlContent == null ? 43 : $firstPageHtmlContent.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "HeaderFooterRenderLayout(htmlContent=" + this.getHtmlContent() + ", firstPageHtmlContent=" + this.getFirstPageHtmlContent() + ", enabled=" + this.isEnabled() + ", firstPageEnabled=" + this.isFirstPageEnabled() + ", heightPx=" + this.getHeightPx() + ", firstPageHeightPx=" + this.getFirstPageHeightPx() + ")";
    }
}

