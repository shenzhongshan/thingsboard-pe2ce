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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.function.Supplier;

/**
 * Gate in front of everything an integration ingests, so one runaway device or a misconfigured
 * broker cannot saturate the platform.
 * <p>
 * Two distinct kinds of limit live here, exactly as in PE:
 * <ul>
 *     <li><b>message limits</b> ({@code checkLimit*}, per tenant / device / asset) come from the
 *         tenant profile and <b>throw</b> {@code TbRateLimitsException} — the message is rejected;</li>
 *     <li><b>debug event limits</b> (the {@code IntegrationId}/{@code ConverterId} overloads) come
 *         from {@code event.debug.rate_limits.*} and normally just return {@code false} — the
 *         event is dropped but the message itself still flows.</li>
 * </ul>
 * The interface lives in this module because the runtimes call it; the implementation does not,
 * since it needs the platform's rate-limit cache. A remote integration process supplies its own
 * (see {@code remote-integration}).
 *
 * <p>The {@code Supplier<String>} arguments are only evaluated when trace logging is on — the
 * payload is not serialised on the hot path.
 */
public interface IntegrationRateLimitService {

    /** @throws org.thingsboard.server.common.msg.tools.TbRateLimitsException if the tenant is over its limit */
    void checkLimit(TenantId tenantId, Supplier<String> msg);

    /** @throws org.thingsboard.server.common.msg.tools.TbRateLimitsException if this device is over its limit */
    void checkLimitPerDevice(TenantId tenantId, String deviceName, Supplier<String> msg);

    /** @throws org.thingsboard.server.common.msg.tools.TbRateLimitsException if this asset is over its limit */
    void checkLimitPerAsset(TenantId tenantId, String assetName, Supplier<String> msg);

    /** @return false when the integration's debug events are being rate limited */
    boolean checkLimit(TenantId tenantId, IntegrationId integrationId, boolean throwException);

    /** @return false when the converter's debug events are being rate limited */
    boolean checkLimit(TenantId tenantId, ConverterId converterId, boolean throwException);

    /** True if this entity was already handled within the deduplication window. */
    boolean alreadyProcessed(EntityId entityId, EntityType entityType);

}
