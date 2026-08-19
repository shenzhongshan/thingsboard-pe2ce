/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotNull
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.Filter;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

public abstract class AbstractReportTemplateConfig
implements ReportTemplateConfig {
    protected String namePattern;
    protected String timeDataPattern;
    protected List<EntityAlias> entityAliases;
    protected List<Filter> filters;
    @NotNull
    protected List<ReportComponent> components;

    @Generated
    protected AbstractReportTemplateConfig(AbstractReportTemplateConfigBuilder<?, ?> b) {
        this.namePattern = b.namePattern;
        this.timeDataPattern = b.timeDataPattern;
        this.entityAliases = b.entityAliases;
        this.filters = b.filters;
        this.components = b.components;
    }

    @Override
    @Generated
    public String getNamePattern() {
        return this.namePattern;
    }

    @Override
    @Generated
    public String getTimeDataPattern() {
        return this.timeDataPattern;
    }

    @Override
    @Generated
    public List<EntityAlias> getEntityAliases() {
        return this.entityAliases;
    }

    @Override
    @Generated
    public List<Filter> getFilters() {
        return this.filters;
    }

    @Override
    @Generated
    public List<ReportComponent> getComponents() {
        return this.components;
    }

    @Generated
    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    @Generated
    public void setTimeDataPattern(String timeDataPattern) {
        this.timeDataPattern = timeDataPattern;
    }

    @Generated
    public void setEntityAliases(List<EntityAlias> entityAliases) {
        this.entityAliases = entityAliases;
    }

    @Generated
    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    @Generated
    public void setComponents(List<ReportComponent> components) {
        this.components = components;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractReportTemplateConfig)) {
            return false;
        }
        AbstractReportTemplateConfig other = (AbstractReportTemplateConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$namePattern = this.getNamePattern();
        String other$namePattern = other.getNamePattern();
        if (this$namePattern == null ? other$namePattern != null : !this$namePattern.equals(other$namePattern)) {
            return false;
        }
        String this$timeDataPattern = this.getTimeDataPattern();
        String other$timeDataPattern = other.getTimeDataPattern();
        if (this$timeDataPattern == null ? other$timeDataPattern != null : !this$timeDataPattern.equals(other$timeDataPattern)) {
            return false;
        }
        List<EntityAlias> this$entityAliases = this.getEntityAliases();
        List<EntityAlias> other$entityAliases = other.getEntityAliases();
        if (this$entityAliases == null ? other$entityAliases != null : !((Object)this$entityAliases).equals(other$entityAliases)) {
            return false;
        }
        List<Filter> this$filters = this.getFilters();
        List<Filter> other$filters = other.getFilters();
        if (this$filters == null ? other$filters != null : !((Object)this$filters).equals(other$filters)) {
            return false;
        }
        List<ReportComponent> this$components = this.getComponents();
        List<ReportComponent> other$components = other.getComponents();
        return !(this$components == null ? other$components != null : !((Object)this$components).equals(other$components));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractReportTemplateConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $namePattern = this.getNamePattern();
        result = result * 59 + ($namePattern == null ? 43 : $namePattern.hashCode());
        String $timeDataPattern = this.getTimeDataPattern();
        result = result * 59 + ($timeDataPattern == null ? 43 : $timeDataPattern.hashCode());
        List<EntityAlias> $entityAliases = this.getEntityAliases();
        result = result * 59 + ($entityAliases == null ? 43 : ((Object)$entityAliases).hashCode());
        List<Filter> $filters = this.getFilters();
        result = result * 59 + ($filters == null ? 43 : ((Object)$filters).hashCode());
        List<ReportComponent> $components = this.getComponents();
        result = result * 59 + ($components == null ? 43 : ((Object)$components).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "AbstractReportTemplateConfig(namePattern=" + this.getNamePattern() + ", timeDataPattern=" + this.getTimeDataPattern() + ", entityAliases=" + String.valueOf(this.getEntityAliases()) + ", filters=" + String.valueOf(this.getFilters()) + ", components=" + String.valueOf(this.getComponents()) + ")";
    }

    @Generated
    public AbstractReportTemplateConfig() {
    }

    @Generated
    public static abstract class AbstractReportTemplateConfigBuilder<C extends AbstractReportTemplateConfig, B extends AbstractReportTemplateConfigBuilder<C, B>> {
        @Generated
        private String namePattern;
        @Generated
        private String timeDataPattern;
        @Generated
        private List<EntityAlias> entityAliases;
        @Generated
        private List<Filter> filters;
        @Generated
        private List<ReportComponent> components;

        @Generated
        public B namePattern(String namePattern) {
            this.namePattern = namePattern;
            return this.self();
        }

        @Generated
        public B timeDataPattern(String timeDataPattern) {
            this.timeDataPattern = timeDataPattern;
            return this.self();
        }

        @Generated
        public B entityAliases(List<EntityAlias> entityAliases) {
            this.entityAliases = entityAliases;
            return this.self();
        }

        @Generated
        public B filters(List<Filter> filters) {
            this.filters = filters;
            return this.self();
        }

        @Generated
        public B components(List<ReportComponent> components) {
            this.components = components;
            return this.self();
        }

        @Generated
        protected abstract B self();

        @Generated
        public abstract C build();

        @Generated
        public String toString() {
            return "AbstractReportTemplateConfig.AbstractReportTemplateConfigBuilder(namePattern=" + this.namePattern + ", timeDataPattern=" + this.timeDataPattern + ", entityAliases=" + String.valueOf(this.entityAliases) + ", filters=" + String.valueOf(this.filters) + ", components=" + String.valueOf(this.components) + ")";
        }
    }
}

