/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  com.fasterxml.jackson.annotation.JsonSubTypes
 *  com.fasterxml.jackson.annotation.JsonSubTypes$Type
 *  com.fasterxml.jackson.annotation.JsonTypeInfo
 *  com.fasterxml.jackson.annotation.JsonTypeInfo$As
 *  com.fasterxml.jackson.annotation.JsonTypeInfo$Id
 */
package org.thingsboard.server.common.data.report.configuration.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.DividerComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ErrorComponent;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.PageBreakComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property="type")
@JsonSubTypes(value={@JsonSubTypes.Type(value=HeadingComponent.class, name="HEADING"), @JsonSubTypes.Type(value=RichTextComponent.class, name="RICH_TEXT"), @JsonSubTypes.Type(value=EntityTableComponent.class, name="ENTITY_TABLE"), @JsonSubTypes.Type(value=PageBreakComponent.class, name="PAGE_BREAK"), @JsonSubTypes.Type(value=TimeseriesTableComponent.class, name="TIME_SERIES_TABLE"), @JsonSubTypes.Type(value=AlarmTableComponent.class, name="ALARM_TABLE"), @JsonSubTypes.Type(value=TimeseriesChartComponent.class, name="TIME_SERIES_CHART"), @JsonSubTypes.Type(value=LatestChartComponent.class, name="LATEST_CHART"), @JsonSubTypes.Type(value=DashboardComponent.class, name="DASHBOARD"), @JsonSubTypes.Type(value=ImageComponent.class, name="IMAGE"), @JsonSubTypes.Type(value=SubReportComponent.class, name="SUB_REPORT"), @JsonSubTypes.Type(value=ErrorComponent.class, name="ERROR"), @JsonSubTypes.Type(value=DividerComponent.class, name="DIVIDER"), @JsonSubTypes.Type(value=SplitViewComponent.class, name="SPLIT_VIEW")})
public interface ReportComponent
extends Serializable {
    public ReportComponentType getType();

    public ReportComponentSubType getSubType();
}

