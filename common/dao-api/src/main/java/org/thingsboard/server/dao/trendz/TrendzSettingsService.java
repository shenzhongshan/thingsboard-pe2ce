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
package org.thingsboard.server.dao.trendz;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzIntegrationSettings;
import org.thingsboard.server.common.data.trendz.TrendzSettings;

/**
 * Trendz settings at two levels: per-tenant widget configuration, and the system-wide integration
 * state written by the connect/healthcheck flow.
 */
public interface TrendzSettingsService {

    TrendzSettings saveTrendzSettings(TenantId tenantId, TrendzSettings settings);

    TrendzSettings findTrendzSettings(TenantId tenantId);

    void deleteTrendzSettings(TenantId tenantId);

    void saveTrendzIntegrationSettings(TrendzIntegrationSettings settings);

    /** Null until the first connect attempt. */
    TrendzIntegrationSettings findTrendzIntegrationSettings();

    void deleteTrendzIntegrationSettings();

}
