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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DeduplicationUtil;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.function.Supplier;

/**
 * Platform-side rate limiting for integrations: a thin adapter over the shared
 * {@link RateLimitService}, which is where the per-tenant buckets and their tenant-profile
 * configuration already live.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultIntegrationRateLimitService implements IntegrationRateLimitService {

    private final RateLimitService rateLimitService;

    @Value("${event.debug.rate_limits.enabled:true}")
    private boolean eventRateLimitsEnabled;
    @Value("${event.debug.rate_limits.integration:}")
    private String integrationEventsRateLimitsConf;
    @Value("${event.debug.rate_limits.converter:}")
    private String converterEventsRateLimitsConf;
    @Value("#{${cache.rateLimits.timeToLiveInMinutes:120} * 60 * 1000}")
    private long deduplicationDuration;

    @Override
    public void checkLimit(TenantId tenantId, Supplier<String> msg) {
        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS_PER_TENANT, tenantId)) {
            log.trace("[{}] Tenant level rate limit detected: {}", tenantId, msg.get());
            throw new TbRateLimitsException(EntityType.TENANT);
        }
    }

    @Override
    public void checkLimitPerDevice(TenantId tenantId, String deviceName, Supplier<String> msg) {
        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS_PER_DEVICE, tenantId, Pair.of(tenantId, deviceName))) {
            log.trace("[{}][{}] Device level rate limit detected: {}", tenantId, deviceName, msg.get());
            throw new TbRateLimitsException(EntityType.DEVICE);
        }
    }

    @Override
    public void checkLimitPerAsset(TenantId tenantId, String assetName, Supplier<String> msg) {
        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS_PER_ASSET, tenantId, Pair.of(tenantId, assetName))) {
            log.trace("[{}][{}] Asset level rate limit detected: {}", tenantId, assetName, msg.get());
            throw new TbRateLimitsException(EntityType.ASSET);
        }
    }

    @Override
    public boolean checkLimit(TenantId tenantId, IntegrationId integrationId, boolean throwException) {
        if (!eventRateLimitsEnabled) {
            return true;
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_EVENTS, (Object) tenantId, integrationEventsRateLimitsConf)) {
            log.trace("[{}][{}] Integration level debug rate limit detected.", tenantId, integrationId);
            if (throwException) {
                throw new TbRateLimitsException(EntityType.INTEGRATION, "Integration debug rate limits reached!");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean checkLimit(TenantId tenantId, ConverterId converterId, boolean throwException) {
        if (!eventRateLimitsEnabled) {
            return true;
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.CONVERTER_EVENTS, (Object) tenantId, converterEventsRateLimitsConf)) {
            log.trace("[{}][{}] Converter level debug rate limit detected.", tenantId, converterId);
            if (throwException) {
                throw new TbRateLimitsException(EntityType.CONVERTER, "Converter debug rate limits reached!");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean alreadyProcessed(EntityId entityId, EntityType entityType) {
        return DeduplicationUtil.alreadyProcessed(new DeduplicationKey(entityId, entityType), deduplicationDuration);
    }

    /** Key of the deduplication window; equality is what makes the window work. */
    private record DeduplicationKey(EntityId entityId, EntityType entityType) {
    }

}
