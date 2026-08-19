/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.common.data.report.configuration.components;

import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Insets;

public interface LayoutReportComponent
extends ReportComponent {
    public Insets getMargins();

    public Insets getPaddings();

    public String getBackground();

    public Integer getBorderWidth();

    public Integer getBorderRadius();

    public String getBorderColor();
}

