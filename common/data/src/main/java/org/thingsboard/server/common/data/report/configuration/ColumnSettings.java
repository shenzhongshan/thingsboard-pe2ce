/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.CellSettings;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

public class ColumnSettings
implements DataKeySettings {
    private String columnWidth;
    private CellSettings header;
    private CellSettings cell;

    @Override
    public DataKeySettingsType getType() {
        return DataKeySettingsType.COLUMN;
    }

    @Generated
    public ColumnSettings() {
    }

    @Generated
    public String getColumnWidth() {
        return this.columnWidth;
    }

    @Generated
    public CellSettings getHeader() {
        return this.header;
    }

    @Generated
    public CellSettings getCell() {
        return this.cell;
    }

    @Generated
    public void setColumnWidth(String columnWidth) {
        this.columnWidth = columnWidth;
    }

    @Generated
    public void setHeader(CellSettings header) {
        this.header = header;
    }

    @Generated
    public void setCell(CellSettings cell) {
        this.cell = cell;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ColumnSettings)) {
            return false;
        }
        ColumnSettings other = (ColumnSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$columnWidth = this.getColumnWidth();
        String other$columnWidth = other.getColumnWidth();
        if (this$columnWidth == null ? other$columnWidth != null : !this$columnWidth.equals(other$columnWidth)) {
            return false;
        }
        CellSettings this$header = this.getHeader();
        CellSettings other$header = other.getHeader();
        if (this$header == null ? other$header != null : !((Object)this$header).equals(other$header)) {
            return false;
        }
        CellSettings this$cell = this.getCell();
        CellSettings other$cell = other.getCell();
        return !(this$cell == null ? other$cell != null : !((Object)this$cell).equals(other$cell));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ColumnSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $columnWidth = this.getColumnWidth();
        result = result * 59 + ($columnWidth == null ? 43 : $columnWidth.hashCode());
        CellSettings $header = this.getHeader();
        result = result * 59 + ($header == null ? 43 : ((Object)$header).hashCode());
        CellSettings $cell = this.getCell();
        result = result * 59 + ($cell == null ? 43 : ((Object)$cell).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ColumnSettings(columnWidth=" + this.getColumnWidth() + ", header=" + String.valueOf(this.getHeader()) + ", cell=" + String.valueOf(this.getCell()) + ")";
    }
}

