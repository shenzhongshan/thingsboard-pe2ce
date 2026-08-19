/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.report.configuration.CellSettings
 *  org.thingsboard.server.common.data.report.configuration.ColumnSettings
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataKeySettings
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.TableSortOrder
 *  org.thingsboard.server.common.data.report.configuration.components.TableWithLayoutReportComponent
 *  org.thingsboard.server.common.data.report.configuration.style.Font
 *  org.thingsboard.server.common.data.report.configuration.style.FontStyle
 *  org.thingsboard.server.common.data.report.configuration.style.FontWeight
 *  org.thingsboard.server.common.data.report.configuration.style.Heading
 *  org.thingsboard.server.common.data.report.configuration.style.TextAlignment
 *  org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment
 */
package org.thingsboard.server.report.renderer;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.CellSettings;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.components.TableWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.ReportComponentWithLayoutRenderer;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ReportUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

public abstract class TableWithLayoutComponentRenderer<C extends TableWithLayoutReportComponent>
extends ReportComponentWithLayoutRenderer<C> {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(TableWithLayoutComponentRenderer.class);

    protected String dataSourceName() {
        return "data source";
    }

    protected String noDataMessage() {
        return "Table content is empty";
    }

    @Override
    protected String renderContent(C component, ComponentData componentData) {
        Optional<DataSource> dataSource = ReportUtils.getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return this.renderError("No " + this.dataSourceName() + " is configured for " + String.valueOf(this.getType()) + " component. Please check the " + this.dataSourceName() + " configuration.");
        }
        List<DataKey> columns = this.getColumns(component, dataSource.get());
        if (columns.isEmpty()) {
            return this.renderError("No columns are configured for " + String.valueOf(this.getType()) + " component. Please check the " + this.dataSourceName() + " configuration.");
        }
        HashMap<String, CellVariables> headers = this.buildCellVariables(columns, true);
        List<LinkedHashMap<String, CellVariables>> rows = this.buildDataRows(columns, component.getTableSortOrder(), componentData);
        HashMap<String, Object> componentVars = new HashMap<String, Object>();
        componentVars.put("columns", headers);
        componentVars.put("rows", rows);
        componentVars.put("noDataMessage", this.noDataMessage());
        if (component.isShowTableHeading() && component.getTableHeading() != null) {
            componentVars.put("showTableHeading", true);
            this.populateHeadingVariables(component, componentData, componentVars);
        } else {
            componentVars.put("showTableHeading", false);
        }
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/table-template", componentVars);
    }

    private String renderError(String errorMessage) {
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", errorMessage));
    }

    protected List<DataKey> getColumns(C component, DataSource dataSource) {
        LinkedList<DataKey> dataKeys = new LinkedList<DataKey>();
        Optional.ofNullable(dataSource.getDataKeys()).ifPresent(dataKeys::addAll);
        Optional.ofNullable(dataSource.getLatestDataKeys()).ifPresent(dataKeys::addAll);
        return dataKeys;
    }

    private List<LinkedHashMap<String, CellVariables>> buildDataRows(List<DataKey> columns, TableSortOrder tableSortOrder, ComponentData reportDataSource) {
        ArrayList<LinkedHashMap<String, CellVariables>> rows = new ArrayList<LinkedHashMap<String, CellVariables>>();
        HashMap<String, CellVariables> cellDefaults = this.buildCellVariables(columns, false);
        List<Map<String, String>> entityDatas = reportDataSource.getEntityDatas();
        ReportUtils.sortRowsByTableSortOrder(entityDatas, tableSortOrder);
        for (Map<String, String> entityData : entityDatas) {
            LinkedHashMap<String, CellVariables> row = new LinkedHashMap<String, CellVariables>();
            for (DataKey dataKey : columns) {
                String label = dataKey.getLabel();
                String key = dataKey.getName();
                CellVariables base = cellDefaults.get(label);
                String value = entityData.get(label);
                CellVariables variables = base.toBuilder().fontSize(this.formatFontSize(key, base.getFontSize())).fontWeight(this.formatFontWeight(key, base.getFontWeight())).color(this.formatColor(key, value, base.getColor())).value(this.formatValue(key, value, dataKey)).build();
                row.put(label, variables);
            }
            rows.add(row);
        }
        return rows;
    }

    private void populateHeadingVariables(C component, ComponentData componentData, Map<String, Object> vars) {
        Heading heading = component.getTableHeading();
        String headingText = ThymeleafUtil.renderFromTextString(heading.getText(), componentData.getVariables());
        Font font = this.getHeadingFont(heading);
        vars.put("headingText", headingText);
        vars.put("headingColor", ColorUtils.normalizeCssColorOrDefault(heading.getColor(), "#000"));
        vars.put("headingFontSize", Optional.ofNullable(font.getSize()).filter(s -> s.floatValue() > 0.0f).orElse(Float.valueOf(10.0f)));
        vars.put("headingFontWeight", Optional.ofNullable(font.getWeight()).orElse(FontWeight.NORMAL).getValue());
        vars.put("headingFontStyle", Optional.ofNullable(font.getStyle()).orElse(FontStyle.NORMAL).getValue());
        vars.put("headingFontFamily", StringUtils.defaultString((String)font.getFamily(), (String)"Roboto"));
        vars.put("headingTextAlignment", Optional.ofNullable(heading.getTextAlignment()).orElse(TextAlignment.CENTER).getValue());
        vars.put("headingVerticalAlignment", Optional.ofNullable(heading.getVerticalAlignment()).orElse(VerticalAlignment.MIDDLE).getValue());
        vars.put("headingHeight", heading.getHeight() != null && heading.getHeight() > 0 ? heading.getHeight() + "pt" : "100%");
    }

    private Float formatFontSize(String key, Float fontSize) {
        if (fontSize != null) {
            return fontSize;
        }
        return this.defaultFontSize(key);
    }

    private String formatFontWeight(String key, String fontWeight) {
        if (fontWeight != null) {
            return fontWeight;
        }
        return this.defaultFontWeight(key);
    }

    private String formatColor(String key, String value, String defaultColor) {
        if (defaultColor != null) {
            return defaultColor;
        }
        return this.defaultColor(key, value);
    }

    private String formatValue(String key, String value, DataKey dataKey) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return this.defaultValue(key, value);
    }

    protected Float defaultFontSize(String key) {
        if (ReportUtils.ENTITY_TIME_FIELDS.contains(key)) {
            return Float.valueOf(9.0f);
        }
        return null;
    }

    protected String defaultFontWeight(String key) {
        return null;
    }

    protected String defaultColor(String key, String value) {
        return null;
    }

    protected String defaultValue(String key, String value) {
        return value;
    }

    protected HashMap<String, CellVariables> buildCellVariables(List<DataKey> columns, boolean isHeader) {
        LinkedHashMap<String, CellVariables> result = new LinkedHashMap<String, CellVariables>();
        for (DataKey dataKey : columns) {
            result.put(dataKey.getLabel(), this.toCellVariables(dataKey, isHeader));
        }
        return result;
    }

    protected CellVariables toCellVariables(DataKey dataKey, boolean isHeader) {
        DataKeySettings settings = dataKey.getSettings();
        ColumnSettings columnSettings = null;
        if (settings instanceof ColumnSettings) {
            columnSettings = (ColumnSettings)settings;
        }
        return this.toCellVariables(dataKey.getName(), columnSettings, isHeader);
    }

    protected CellVariables toCellVariables(String key, ColumnSettings columnSettings, boolean isHeader) {
        if (columnSettings != null) {
            CellVariables cellVariables;
            CellSettings cellSettings;
            CellSettings cellSettings2 = cellSettings = isHeader ? columnSettings.getHeader() : columnSettings.getCell();
            if (cellSettings != null) {
                Font font = cellSettings.getFont();
                cellVariables = CellVariables.builder().key(key).color(cellSettings.getColor() != null ? ColorUtils.normalizeCssColor(cellSettings.getColor()) : null).backgroundColor(cellSettings.getBackgroundColor() != null ? ColorUtils.normalizeCssColor(cellSettings.getBackgroundColor()) : null).fontSize(font != null && font.getSize() != null && font.getSize().floatValue() > 0.0f ? font.getSize() : null).fontWeight(font != null && font.getWeight() != null ? font.getWeight().name() : null).fontStyle(font != null && font.getStyle() != null ? font.getStyle().name() : null).fontFamily(font != null && font.getFamily() != null && !font.getFamily().isEmpty() ? font.getFamily() : null).textAlignment(cellSettings.getTextAlignment() != null ? cellSettings.getTextAlignment().name() : null).verticalAlignment(cellSettings.getVerticalAlignment() != null ? cellSettings.getVerticalAlignment().name() : null).build();
            } else {
                cellVariables = new CellVariables(key);
            }
            if (isHeader && !StringUtils.isBlank((String)columnSettings.getColumnWidth())) {
                cellVariables.setWidth(columnSettings.getColumnWidth());
            }
            return cellVariables;
        }
        return new CellVariables(key);
    }

    private Font getHeadingFont(Heading tableHeading) {
        Font headingFont = tableHeading.getFont();
        if (headingFont == null) {
            headingFont = new Font();
            headingFont.setSize(Float.valueOf(20.0f));
            headingFont.setFamily("Roboto");
            headingFont.setStyle(FontStyle.NORMAL);
            headingFont.setWeight(FontWeight.NORMAL);
        }
        return headingFont;
    }

    static class CellVariables {
        private String key;
        private String value;
        private String width;
        private String color;
        private String backgroundColor;
        private Float fontSize;
        private String fontWeight;
        private String fontStyle;
        private String fontFamily;
        private String textAlignment;
        private String verticalAlignment;

        public CellVariables(String key) {
            this.key = key;
        }

        @Generated
        public static CellVariablesBuilder builder() {
            return new CellVariablesBuilder();
        }

        @Generated
        public CellVariablesBuilder toBuilder() {
            return new CellVariablesBuilder().key(this.key).value(this.value).width(this.width).color(this.color).backgroundColor(this.backgroundColor).fontSize(this.fontSize).fontWeight(this.fontWeight).fontStyle(this.fontStyle).fontFamily(this.fontFamily).textAlignment(this.textAlignment).verticalAlignment(this.verticalAlignment);
        }

        @Generated
        public String getKey() {
            return this.key;
        }

        @Generated
        public String getValue() {
            return this.value;
        }

        @Generated
        public String getWidth() {
            return this.width;
        }

        @Generated
        public String getColor() {
            return this.color;
        }

        @Generated
        public String getBackgroundColor() {
            return this.backgroundColor;
        }

        @Generated
        public Float getFontSize() {
            return this.fontSize;
        }

        @Generated
        public String getFontWeight() {
            return this.fontWeight;
        }

        @Generated
        public String getFontStyle() {
            return this.fontStyle;
        }

        @Generated
        public String getFontFamily() {
            return this.fontFamily;
        }

        @Generated
        public String getTextAlignment() {
            return this.textAlignment;
        }

        @Generated
        public String getVerticalAlignment() {
            return this.verticalAlignment;
        }

        @Generated
        public void setKey(String key) {
            this.key = key;
        }

        @Generated
        public void setValue(String value) {
            this.value = value;
        }

        @Generated
        public void setWidth(String width) {
            this.width = width;
        }

        @Generated
        public void setColor(String color) {
            this.color = color;
        }

        @Generated
        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        @Generated
        public void setFontSize(Float fontSize) {
            this.fontSize = fontSize;
        }

        @Generated
        public void setFontWeight(String fontWeight) {
            this.fontWeight = fontWeight;
        }

        @Generated
        public void setFontStyle(String fontStyle) {
            this.fontStyle = fontStyle;
        }

        @Generated
        public void setFontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
        }

        @Generated
        public void setTextAlignment(String textAlignment) {
            this.textAlignment = textAlignment;
        }

        @Generated
        public void setVerticalAlignment(String verticalAlignment) {
            this.verticalAlignment = verticalAlignment;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof CellVariables)) {
                return false;
            }
            CellVariables other = (CellVariables)o;
            if (!other.canEqual(this)) {
                return false;
            }
            Float this$fontSize = this.getFontSize();
            Float other$fontSize = other.getFontSize();
            if (this$fontSize == null ? other$fontSize != null : !((Object)this$fontSize).equals(other$fontSize)) {
                return false;
            }
            String this$key = this.getKey();
            String other$key = other.getKey();
            if (this$key == null ? other$key != null : !this$key.equals(other$key)) {
                return false;
            }
            String this$value = this.getValue();
            String other$value = other.getValue();
            if (this$value == null ? other$value != null : !this$value.equals(other$value)) {
                return false;
            }
            String this$width = this.getWidth();
            String other$width = other.getWidth();
            if (this$width == null ? other$width != null : !this$width.equals(other$width)) {
                return false;
            }
            String this$color = this.getColor();
            String other$color = other.getColor();
            if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
                return false;
            }
            String this$backgroundColor = this.getBackgroundColor();
            String other$backgroundColor = other.getBackgroundColor();
            if (this$backgroundColor == null ? other$backgroundColor != null : !this$backgroundColor.equals(other$backgroundColor)) {
                return false;
            }
            String this$fontWeight = this.getFontWeight();
            String other$fontWeight = other.getFontWeight();
            if (this$fontWeight == null ? other$fontWeight != null : !this$fontWeight.equals(other$fontWeight)) {
                return false;
            }
            String this$fontStyle = this.getFontStyle();
            String other$fontStyle = other.getFontStyle();
            if (this$fontStyle == null ? other$fontStyle != null : !this$fontStyle.equals(other$fontStyle)) {
                return false;
            }
            String this$fontFamily = this.getFontFamily();
            String other$fontFamily = other.getFontFamily();
            if (this$fontFamily == null ? other$fontFamily != null : !this$fontFamily.equals(other$fontFamily)) {
                return false;
            }
            String this$textAlignment = this.getTextAlignment();
            String other$textAlignment = other.getTextAlignment();
            if (this$textAlignment == null ? other$textAlignment != null : !this$textAlignment.equals(other$textAlignment)) {
                return false;
            }
            String this$verticalAlignment = this.getVerticalAlignment();
            String other$verticalAlignment = other.getVerticalAlignment();
            return !(this$verticalAlignment == null ? other$verticalAlignment != null : !this$verticalAlignment.equals(other$verticalAlignment));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof CellVariables;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            Float $fontSize = this.getFontSize();
            result = result * 59 + ($fontSize == null ? 43 : ((Object)$fontSize).hashCode());
            String $key = this.getKey();
            result = result * 59 + ($key == null ? 43 : $key.hashCode());
            String $value = this.getValue();
            result = result * 59 + ($value == null ? 43 : $value.hashCode());
            String $width = this.getWidth();
            result = result * 59 + ($width == null ? 43 : $width.hashCode());
            String $color = this.getColor();
            result = result * 59 + ($color == null ? 43 : $color.hashCode());
            String $backgroundColor = this.getBackgroundColor();
            result = result * 59 + ($backgroundColor == null ? 43 : $backgroundColor.hashCode());
            String $fontWeight = this.getFontWeight();
            result = result * 59 + ($fontWeight == null ? 43 : $fontWeight.hashCode());
            String $fontStyle = this.getFontStyle();
            result = result * 59 + ($fontStyle == null ? 43 : $fontStyle.hashCode());
            String $fontFamily = this.getFontFamily();
            result = result * 59 + ($fontFamily == null ? 43 : $fontFamily.hashCode());
            String $textAlignment = this.getTextAlignment();
            result = result * 59 + ($textAlignment == null ? 43 : $textAlignment.hashCode());
            String $verticalAlignment = this.getVerticalAlignment();
            result = result * 59 + ($verticalAlignment == null ? 43 : $verticalAlignment.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "TableWithLayoutComponentRenderer.CellVariables(key=" + this.getKey() + ", value=" + this.getValue() + ", width=" + this.getWidth() + ", color=" + this.getColor() + ", backgroundColor=" + this.getBackgroundColor() + ", fontSize=" + this.getFontSize() + ", fontWeight=" + this.getFontWeight() + ", fontStyle=" + this.getFontStyle() + ", fontFamily=" + this.getFontFamily() + ", textAlignment=" + this.getTextAlignment() + ", verticalAlignment=" + this.getVerticalAlignment() + ")";
        }

        @ConstructorProperties(value={"key", "value", "width", "color", "backgroundColor", "fontSize", "fontWeight", "fontStyle", "fontFamily", "textAlignment", "verticalAlignment"})
        @Generated
        public CellVariables(String key, String value, String width, String color, String backgroundColor, Float fontSize, String fontWeight, String fontStyle, String fontFamily, String textAlignment, String verticalAlignment) {
            this.key = key;
            this.value = value;
            this.width = width;
            this.color = color;
            this.backgroundColor = backgroundColor;
            this.fontSize = fontSize;
            this.fontWeight = fontWeight;
            this.fontStyle = fontStyle;
            this.fontFamily = fontFamily;
            this.textAlignment = textAlignment;
            this.verticalAlignment = verticalAlignment;
        }

        @Generated
        public CellVariables() {
        }

        @Generated
        public static class CellVariablesBuilder {
            @Generated
            private String key;
            @Generated
            private String value;
            @Generated
            private String width;
            @Generated
            private String color;
            @Generated
            private String backgroundColor;
            @Generated
            private Float fontSize;
            @Generated
            private String fontWeight;
            @Generated
            private String fontStyle;
            @Generated
            private String fontFamily;
            @Generated
            private String textAlignment;
            @Generated
            private String verticalAlignment;

            @Generated
            CellVariablesBuilder() {
            }

            @Generated
            public CellVariablesBuilder key(String key) {
                this.key = key;
                return this;
            }

            @Generated
            public CellVariablesBuilder value(String value) {
                this.value = value;
                return this;
            }

            @Generated
            public CellVariablesBuilder width(String width) {
                this.width = width;
                return this;
            }

            @Generated
            public CellVariablesBuilder color(String color) {
                this.color = color;
                return this;
            }

            @Generated
            public CellVariablesBuilder backgroundColor(String backgroundColor) {
                this.backgroundColor = backgroundColor;
                return this;
            }

            @Generated
            public CellVariablesBuilder fontSize(Float fontSize) {
                this.fontSize = fontSize;
                return this;
            }

            @Generated
            public CellVariablesBuilder fontWeight(String fontWeight) {
                this.fontWeight = fontWeight;
                return this;
            }

            @Generated
            public CellVariablesBuilder fontStyle(String fontStyle) {
                this.fontStyle = fontStyle;
                return this;
            }

            @Generated
            public CellVariablesBuilder fontFamily(String fontFamily) {
                this.fontFamily = fontFamily;
                return this;
            }

            @Generated
            public CellVariablesBuilder textAlignment(String textAlignment) {
                this.textAlignment = textAlignment;
                return this;
            }

            @Generated
            public CellVariablesBuilder verticalAlignment(String verticalAlignment) {
                this.verticalAlignment = verticalAlignment;
                return this;
            }

            @Generated
            public CellVariables build() {
                return new CellVariables(this.key, this.value, this.width, this.color, this.backgroundColor, this.fontSize, this.fontWeight, this.fontStyle, this.fontFamily, this.textAlignment, this.verticalAlignment);
            }

            @Generated
            public String toString() {
                return "TableWithLayoutComponentRenderer.CellVariables.CellVariablesBuilder(key=" + this.key + ", value=" + this.value + ", width=" + this.width + ", color=" + this.color + ", backgroundColor=" + this.backgroundColor + ", fontSize=" + this.fontSize + ", fontWeight=" + this.fontWeight + ", fontStyle=" + this.fontStyle + ", fontFamily=" + this.fontFamily + ", textAlignment=" + this.textAlignment + ", verticalAlignment=" + this.verticalAlignment + ")";
            }
        }
    }
}

