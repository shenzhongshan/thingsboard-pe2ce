/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.common.data.report.configuration;

import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

public class DefaultDataKeySettings
implements DataKeySettings {
    @Override
    public DataKeySettingsType getType() {
        return DataKeySettingsType.DEFAULT;
    }
}

