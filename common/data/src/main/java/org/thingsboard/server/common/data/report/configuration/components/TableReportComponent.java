/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.common.data.report.configuration.components;

import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Heading;

public interface TableReportComponent
extends DataReportComponent {
    public boolean isShowTableHeading();

    public Heading getTableHeading();

    public TableSortOrder getTableSortOrder();
}

