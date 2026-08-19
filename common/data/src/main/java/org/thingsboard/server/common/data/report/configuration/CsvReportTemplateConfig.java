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

public class CsvReportTemplateConfig
extends AbstractReportTemplateConfig {
    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.CSV;
    }

    @Generated
    protected CsvReportTemplateConfig(CsvReportTemplateConfigBuilder<?, ?> b) {
        super(b);
    }

    @Generated
    public static CsvReportTemplateConfigBuilder<?, ?> builder() {
        return new CsvReportTemplateConfigBuilderImpl();
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CsvReportTemplateConfig)) {
            return false;
        }
        CsvReportTemplateConfig other = (CsvReportTemplateConfig)o;
        return other.canEqual(this);
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof CsvReportTemplateConfig;
    }

    @Override
    @Generated
    public int hashCode() {
        boolean result = true;
        return 1;
    }

    @Override
    @Generated
    public String toString() {
        return "CsvReportTemplateConfig()";
    }

    @Generated
    public CsvReportTemplateConfig() {
    }

    @Generated
    private static final class CsvReportTemplateConfigBuilderImpl
    extends CsvReportTemplateConfigBuilder<CsvReportTemplateConfig, CsvReportTemplateConfigBuilderImpl> {
        @Generated
        private CsvReportTemplateConfigBuilderImpl() {
        }

        @Override
        @Generated
        protected CsvReportTemplateConfigBuilderImpl self() {
            return this;
        }

        @Override
        @Generated
        public CsvReportTemplateConfig build() {
            return new CsvReportTemplateConfig(this);
        }
    }

    @Generated
    public static abstract class CsvReportTemplateConfigBuilder<C extends CsvReportTemplateConfig, B extends CsvReportTemplateConfigBuilder<C, B>>
    extends AbstractReportTemplateConfig.AbstractReportTemplateConfigBuilder<C, B> {
        @Override
        @Generated
        protected abstract B self();

        @Override
        @Generated
        public abstract C build();

        @Override
        @Generated
        public String toString() {
            return "CsvReportTemplateConfig.CsvReportTemplateConfigBuilder(super=" + super.toString() + ")";
        }
    }
}

