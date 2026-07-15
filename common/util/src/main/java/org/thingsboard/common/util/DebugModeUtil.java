/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.common.util;

import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class DebugModeUtil {

    public static final int DEBUG_MODE_DEFAULT_DURATION_MINUTES = 15;

    private DebugModeUtil() {
    }

    public static int getMaxDebugAllDuration(int tenantProfileDuration, int systemDefaultDuration) {
        if (tenantProfileDuration > 0) {
            return tenantProfileDuration;
        } else {
            return systemDefaultDuration > 0 ? systemDefaultDuration : DEBUG_MODE_DEFAULT_DURATION_MINUTES;
        }
    }

    /**
     * Migrates a legacy debugMode boolean into a DebugSettings object and clamps allEnabledUntil
     * to the tenant's max debug-all duration, mirroring AbstractEntityService.updateDebugSettings
     * for entities (e.g. Integration/Converter) whose DAO service doesn't extend that base class.
     */
    public static void migrateDebugSettings(HasDebugSettings entity, int tenantProfileMaxMinutes, int systemDefaultMinutes, long now) {
        long maxDebugAllUntil = now + TimeUnit.MINUTES.toMillis(getMaxDebugAllDuration(tenantProfileMaxMinutes, systemDefaultMinutes));
        if (entity.getDebugSettings() != null) {
            entity.setDebugSettings(entity.getDebugSettings().copy(maxDebugAllUntil));
        } else if (entity.isDebugMode()) {
            entity.setDebugSettings(DebugSettings.failuresOrUntil(maxDebugAllUntil));
            entity.setDebugMode(false);
        }
    }

    public static boolean isDebugAllAvailable(HasDebugSettings debugSettingsAware) {
        var debugSettings = debugSettingsAware.getDebugSettings();
        return debugSettings != null && debugSettings.getAllEnabledUntil() > System.currentTimeMillis();
    }

    public static boolean isDebugAvailable(HasDebugSettings debugSettingsAware, String nodeConnection) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && debugSettings.isFailuresEnabled() && TbNodeConnectionType.FAILURE.equals(nodeConnection);
        }
    }

    public static boolean isDebugFailuresAvailable(HasDebugSettings debugSettingsAware, Set<String> nodeConnections) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && nodeConnections != null && debugSettings.isFailuresEnabled() && nodeConnections.contains(TbNodeConnectionType.FAILURE);
        }
    }

    public static boolean isDebugFailuresAvailable(HasDebugSettings debugSettingsAware) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && debugSettings.isFailuresEnabled();
        }
    }

}
