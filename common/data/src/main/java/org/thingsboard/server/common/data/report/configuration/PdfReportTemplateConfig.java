/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import lombok.Generated;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.AbstractReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.style.Insets;
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;

public class PdfReportTemplateConfig
extends AbstractReportTemplateConfig {
    private PageSize pageSize;
    private PageOrientation pageOrientation;
    private Insets pageMargins;
    private String pageBackground;
    private HeaderFooter header;
    private HeaderFooter footer;

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.PDF;
    }

    @Generated
    protected PdfReportTemplateConfig(PdfReportTemplateConfigBuilder<?, ?> b) {
        super(b);
        this.pageSize = b.pageSize;
        this.pageOrientation = b.pageOrientation;
        this.pageMargins = b.pageMargins;
        this.pageBackground = b.pageBackground;
        this.header = b.header;
        this.footer = b.footer;
    }

    @Generated
    public static PdfReportTemplateConfigBuilder<?, ?> builder() {
        return new PdfReportTemplateConfigBuilderImpl();
    }

    @Generated
    public PageSize getPageSize() {
        return this.pageSize;
    }

    @Generated
    public PageOrientation getPageOrientation() {
        return this.pageOrientation;
    }

    @Generated
    public Insets getPageMargins() {
        return this.pageMargins;
    }

    @Generated
    public String getPageBackground() {
        return this.pageBackground;
    }

    @Generated
    public HeaderFooter getHeader() {
        return this.header;
    }

    @Generated
    public HeaderFooter getFooter() {
        return this.footer;
    }

    @Generated
    public void setPageSize(PageSize pageSize) {
        this.pageSize = pageSize;
    }

    @Generated
    public void setPageOrientation(PageOrientation pageOrientation) {
        this.pageOrientation = pageOrientation;
    }

    @Generated
    public void setPageMargins(Insets pageMargins) {
        this.pageMargins = pageMargins;
    }

    @Generated
    public void setPageBackground(String pageBackground) {
        this.pageBackground = pageBackground;
    }

    @Generated
    public void setHeader(HeaderFooter header) {
        this.header = header;
    }

    @Generated
    public void setFooter(HeaderFooter footer) {
        this.footer = footer;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PdfReportTemplateConfig)) {
            return false;
        }
        PdfReportTemplateConfig other = (PdfReportTemplateConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        PageSize this$pageSize = this.getPageSize();
        PageSize other$pageSize = other.getPageSize();
        if (this$pageSize == null ? other$pageSize != null : !((Object)((Object)this$pageSize)).equals((Object)other$pageSize)) {
            return false;
        }
        PageOrientation this$pageOrientation = this.getPageOrientation();
        PageOrientation other$pageOrientation = other.getPageOrientation();
        if (this$pageOrientation == null ? other$pageOrientation != null : !((Object)((Object)this$pageOrientation)).equals((Object)other$pageOrientation)) {
            return false;
        }
        Insets this$pageMargins = this.getPageMargins();
        Insets other$pageMargins = other.getPageMargins();
        if (this$pageMargins == null ? other$pageMargins != null : !((Object)this$pageMargins).equals(other$pageMargins)) {
            return false;
        }
        String this$pageBackground = this.getPageBackground();
        String other$pageBackground = other.getPageBackground();
        if (this$pageBackground == null ? other$pageBackground != null : !this$pageBackground.equals(other$pageBackground)) {
            return false;
        }
        HeaderFooter this$header = this.getHeader();
        HeaderFooter other$header = other.getHeader();
        if (this$header == null ? other$header != null : !((Object)this$header).equals(other$header)) {
            return false;
        }
        HeaderFooter this$footer = this.getFooter();
        HeaderFooter other$footer = other.getFooter();
        return !(this$footer == null ? other$footer != null : !((Object)this$footer).equals(other$footer));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PdfReportTemplateConfig;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        PageSize $pageSize = this.getPageSize();
        result = result * 59 + ($pageSize == null ? 43 : ((Object)((Object)$pageSize)).hashCode());
        PageOrientation $pageOrientation = this.getPageOrientation();
        result = result * 59 + ($pageOrientation == null ? 43 : ((Object)((Object)$pageOrientation)).hashCode());
        Insets $pageMargins = this.getPageMargins();
        result = result * 59 + ($pageMargins == null ? 43 : ((Object)$pageMargins).hashCode());
        String $pageBackground = this.getPageBackground();
        result = result * 59 + ($pageBackground == null ? 43 : $pageBackground.hashCode());
        HeaderFooter $header = this.getHeader();
        result = result * 59 + ($header == null ? 43 : ((Object)$header).hashCode());
        HeaderFooter $footer = this.getFooter();
        result = result * 59 + ($footer == null ? 43 : ((Object)$footer).hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "PdfReportTemplateConfig(pageSize=" + String.valueOf((Object)this.getPageSize()) + ", pageOrientation=" + String.valueOf((Object)this.getPageOrientation()) + ", pageMargins=" + String.valueOf(this.getPageMargins()) + ", pageBackground=" + this.getPageBackground() + ", header=" + String.valueOf(this.getHeader()) + ", footer=" + String.valueOf(this.getFooter()) + ")";
    }

    @Generated
    public PdfReportTemplateConfig() {
    }

    @Generated
    public static abstract class PdfReportTemplateConfigBuilder<C extends PdfReportTemplateConfig, B extends PdfReportTemplateConfigBuilder<C, B>>
    extends AbstractReportTemplateConfig.AbstractReportTemplateConfigBuilder<C, B> {
        @Generated
        private PageSize pageSize;
        @Generated
        private PageOrientation pageOrientation;
        @Generated
        private Insets pageMargins;
        @Generated
        private String pageBackground;
        @Generated
        private HeaderFooter header;
        @Generated
        private HeaderFooter footer;

        @Generated
        public B pageSize(PageSize pageSize) {
            this.pageSize = pageSize;
            return (B)this.self();
        }

        @Generated
        public B pageOrientation(PageOrientation pageOrientation) {
            this.pageOrientation = pageOrientation;
            return (B)this.self();
        }

        @Generated
        public B pageMargins(Insets pageMargins) {
            this.pageMargins = pageMargins;
            return (B)this.self();
        }

        @Generated
        public B pageBackground(String pageBackground) {
            this.pageBackground = pageBackground;
            return (B)this.self();
        }

        @Generated
        public B header(HeaderFooter header) {
            this.header = header;
            return (B)this.self();
        }

        @Generated
        public B footer(HeaderFooter footer) {
            this.footer = footer;
            return (B)this.self();
        }

        @Override
        @Generated
        protected abstract B self();

        @Override
        @Generated
        public abstract C build();

        @Override
        @Generated
        public String toString() {
            return "PdfReportTemplateConfig.PdfReportTemplateConfigBuilder(super=" + super.toString() + ", pageSize=" + String.valueOf((Object)this.pageSize) + ", pageOrientation=" + String.valueOf((Object)this.pageOrientation) + ", pageMargins=" + String.valueOf(this.pageMargins) + ", pageBackground=" + this.pageBackground + ", header=" + String.valueOf(this.header) + ", footer=" + String.valueOf(this.footer) + ")";
        }
    }

    @Generated
    private static final class PdfReportTemplateConfigBuilderImpl
    extends PdfReportTemplateConfigBuilder<PdfReportTemplateConfig, PdfReportTemplateConfigBuilderImpl> {
        @Generated
        private PdfReportTemplateConfigBuilderImpl() {
        }

        @Override
        @Generated
        protected PdfReportTemplateConfigBuilderImpl self() {
            return this;
        }

        @Override
        @Generated
        public PdfReportTemplateConfig build() {
            return new PdfReportTemplateConfig(this);
        }
    }
}

