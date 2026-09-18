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
package org.thingsboard.server.service.integration;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;

/**
 * Identifies one device whose activity is being tracked on behalf of an integration. Used as the
 * key of {@link IntegrationActivityManager}'s state map, so it must have value equality.
 */
public record IntegrationActivityKey(TenantId tenantId, DeviceId deviceId) {

    public IntegrationActivityKey {
        Objects.requireNonNull(tenantId, "tenantId is marked non-null but is null");
        Objects.requireNonNull(deviceId, "deviceId is marked non-null but is null");
    }

}
