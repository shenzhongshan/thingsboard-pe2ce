/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  com.fasterxml.jackson.annotation.JsonSubTypes
 *  com.fasterxml.jackson.annotation.JsonSubTypes$Type
 *  com.fasterxml.jackson.annotation.JsonTypeInfo
 *  com.fasterxml.jackson.annotation.JsonTypeInfo$Id
 */
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.Filter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

@JsonIgnoreProperties(ignoreUnknown=true)
// defaultImpl is added on top of PE's annotation. The authoritative format lives in the
// report_template column, so a configuration posted without the redundant "format" key has to bind
// rather than fail the whole request with a 500 - notably `{}`, which several callers send.
// ReportTemplateEntity stamps the column value into the JSON on write, so what gets persisted always
// carries a correct and self-consistent type id.
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="format", defaultImpl=PdfReportTemplateConfig.class)
@JsonSubTypes(value={@JsonSubTypes.Type(value=PdfReportTemplateConfig.class, name="PDF"), @JsonSubTypes.Type(value=CsvReportTemplateConfig.class, name="CSV")})
public interface ReportTemplateConfig {
    public String getNamePattern();

    public String getTimeDataPattern();

    public TbReportFormat getFormat();

    public List<EntityAlias> getEntityAliases();

    public List<Filter> getFilters();

    public List<ReportComponent> getComponents();
}

