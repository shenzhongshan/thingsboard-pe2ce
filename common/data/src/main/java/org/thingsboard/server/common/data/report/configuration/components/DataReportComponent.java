/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.common.data.report.configuration.components;

import java.util.List;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

public interface DataReportComponent
extends ReportComponent {
    public List<DataSource> getDataSources();
}

