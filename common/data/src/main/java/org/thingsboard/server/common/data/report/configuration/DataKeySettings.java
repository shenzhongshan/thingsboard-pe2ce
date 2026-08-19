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
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.DefaultDataKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property="type", defaultImpl=DefaultDataKeySettings.class)
@JsonSubTypes(value={@JsonSubTypes.Type(value=ColumnSettings.class, name="COLUMN"), @JsonSubTypes.Type(value=TimeSeriesChartKeySettings.class, name="TIME_SERIES_CHART"), @JsonSubTypes.Type(value=DefaultDataKeySettings.class, name="DEFAULT")})
public interface DataKeySettings {
    public DataKeySettingsType getType();
}

