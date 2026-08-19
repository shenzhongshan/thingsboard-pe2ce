/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.components.AbstractDataWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TableWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Heading;

@Schema
public abstract class AbstractTableWithLayoutReportComponent
extends AbstractDataWithLayoutReportComponent
implements TableWithLayoutReportComponent {
    private boolean showTableHeading;
    private Heading tableHeading;
    private TableSortOrder tableSortOrder;

    @Override
    @Generated
    public boolean isShowTableHeading() {
        return this.showTableHeading;
    }

    @Override
    @Generated
    public Heading getTableHeading() {
        return this.tableHeading;
    }

    @Override
    @Generated
    public TableSortOrder getTableSortOrder() {
        return this.tableSortOrder;
    }

    @Generated
    public void setShowTableHeading(boolean showTableHeading) {
        this.showTableHeading = showTableHeading;
    }

    @Generated
    public void setTableHeading(Heading tableHeading) {
        this.tableHeading = tableHeading;
    }

    @Generated
    public void setTableSortOrder(TableSortOrder tableSortOrder) {
        this.tableSortOrder = tableSortOrder;
    }

    @Override
    @Generated
    public String toString() {
        return "AbstractTableWithLayoutReportComponent(showTableHeading=" + this.isShowTableHeading() + ", tableHeading=" + String.valueOf(this.getTableHeading()) + ", tableSortOrder=" + String.valueOf(this.getTableSortOrder()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractTableWithLayoutReportComponent)) {
            return false;
        }
        AbstractTableWithLayoutReportComponent other = (AbstractTableWithLayoutReportComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (this.isShowTableHeading() != other.isShowTableHeading()) {
            return false;
        }
        Heading this$tableHeading = this.getTableHeading();
        Heading other$tableHeading = other.getTableHeading();
        if (this$tableHeading == null ? other$tableHeading != null : !((Object)this$tableHeading).equals(other$tableHeading)) {
            return false;
        }
        TableSortOrder this$tableSortOrder = this.getTableSortOrder();
        TableSortOrder other$tableSortOrder = other.getTableSortOrder();
        return !(this$tableSortOrder == null ? other$tableSortOrder != null : !((Object)this$tableSortOrder).equals(other$tableSortOrder));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractTableWithLayoutReportComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        result = result * 59 + (this.isShowTableHeading() ? 79 : 97);
        Heading $tableHeading = this.getTableHeading();
        result = result * 59 + ($tableHeading == null ? 43 : ((Object)$tableHeading).hashCode());
        TableSortOrder $tableSortOrder = this.getTableSortOrder();
        result = result * 59 + ($tableSortOrder == null ? 43 : ((Object)$tableSortOrder).hashCode());
        return result;
    }

    @Generated
    public AbstractTableWithLayoutReportComponent() {
    }

    @ConstructorProperties(value={"showTableHeading", "tableHeading", "tableSortOrder"})
    @Generated
    public AbstractTableWithLayoutReportComponent(boolean showTableHeading, Heading tableHeading, TableSortOrder tableSortOrder) {
        this.showTableHeading = showTableHeading;
        this.tableHeading = tableHeading;
        this.tableSortOrder = tableSortOrder;
    }
}

