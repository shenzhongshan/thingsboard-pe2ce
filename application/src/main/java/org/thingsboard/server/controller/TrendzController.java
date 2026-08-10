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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzIntegrationSettings;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.trendz.TrendzSyncService;

/**
 * Configures and monitors the Trendz integration. The Trendz content APIs live in
 * {@link TrendzApiController}; this one only deals with connecting the two systems.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/trendz")
@RequiredArgsConstructor
@Slf4j
public class TrendzController extends BaseController {

    /** The public connect endpoint is unauthenticated, so it gets a hard cap. */
    private static final String TRENDZ_PUBLIC_SYNC_ENDPOINT_RATE_LIMIT = "1:60";

    private final TrendzSyncService trendzSyncService;
    private final TrendzSettingsService trendzSettingsService;
    private final RateLimitService rateLimitService;

    @ApiOperation(value = "Get Trendz settings (getTrendzSettings)",
            notes = "Per-tenant Trendz settings used by the Trendz dashboard widgets. " +
                    "Available for users with TENANT_ADMIN authority.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/settings")
    public TrendzSettings getTrendzSettings() throws ThingsboardException {
        TrendzSettings settings = trendzSettingsService.findTrendzSettings(getTenantId());
        return settings != null ? settings : new TrendzSettings();
    }

    @ApiOperation(value = "Save Trendz settings (saveTrendzSettings)",
            notes = "Saves the per-tenant Trendz settings used by the Trendz dashboard widgets. " +
                    "Available for users with TENANT_ADMIN authority.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/settings")
    public TrendzSettings saveTrendzSettings(@RequestBody TrendzSettings settings) throws ThingsboardException {
        return trendzSettingsService.saveTrendzSettings(getTenantId(), settings);
    }

    @ApiOperation(value = "Get Trendz configuration (getTrendzConfig)",
            notes = "Returns the configured Trendz and ThingsBoard URLs.\n\nAvailable for users with 'SYS_ADMIN' authority.")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/config")
    public TrendzConfiguration getTrendzConfig(@AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        TrendzIntegrationSettings settings = trendzSettingsService.findTrendzIntegrationSettings();
        return settings != null ? settings.configuration() : new TrendzConfiguration(null, null);
    }

    @ApiOperation(value = "Save Trendz configuration (saveTrendzConfig)",
            notes = "Saves the Trendz and ThingsBoard URLs without connecting. Use the connect endpoint to " +
                    "actually synchronize.\n\nAvailable for users with 'SYS_ADMIN' authority.")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/config")
    public TrendzConfiguration saveTrendzConfig(@RequestBody TrendzConfiguration config,
                                                @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        TrendzIntegrationSettings current = trendzSettingsService.findTrendzIntegrationSettings();
        if (current != null && config.equals(current.configuration())) {
            // Unchanged: keep the existing sync result rather than resetting it to "not initialized".
            return current.configuration();
        }
        trendzSettingsService.saveTrendzIntegrationSettings(new TrendzIntegrationSettings(config, notInitialized()));
        return config;
    }

    @ApiOperation(value = "Get Trendz synchronization result (getTrendzSyncResult)",
            notes = "Returns the outcome of the last connect attempt.\n\nAvailable for any authorized user.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/sync")
    public TrendzSynchronizationResult getTrendzSyncResult() {
        TrendzIntegrationSettings settings = trendzSettingsService.findTrendzIntegrationSettings();
        if (settings != null && settings.synchronizationResult() != null) {
            return settings.synchronizationResult();
        }
        return notInitialized();
    }

    @ApiOperation(value = "Perform Trendz healthcheck (performTrendzHealthcheck)",
            notes = "Checks that a previously synchronized Trendz is still reachable and accepts our API key." +
                    "\n\nAvailable for any authorized user.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/healthcheck")
    public TrendzHealthcheckResult performTrendzHealthcheck() {
        return trendzSyncService.performHealthcheck();
    }

    @ApiOperation(value = "Connect to Trendz (connectToTrendz)",
            notes = "Runs the handshake: generates the internal API key if needed, checks the Trendz version, and " +
                    "hands Trendz the URLs and key it needs to call back.\n\nAvailable for users with 'SYS_ADMIN' authority.")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/connect")
    public TrendzSynchronizationResult connectToTrendz(@AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        return trendzSyncService.performSync().synchronizationResult();
    }

    @ApiOperation(value = "Public connect to Trendz (publicConnectToTrendz)",
            notes = "Lets a freshly started Trendz ask ThingsBoard to connect, for deployments where the two come " +
                    "up together. Only acts if Trendz is not already synchronized, and is rate limited.")
    @PostMapping("/public/connect")
    public void publicConnectToTrendz() {
        try {
            if (rateLimitService.checkRateLimit(LimitedApi.TRENDZ_PUBLIC_SYNC, (Object) TenantId.SYS_TENANT_ID, TRENDZ_PUBLIC_SYNC_ENDPOINT_RATE_LIMIT)) {
                trendzSyncService.performSyncIfNeeded();
            } else {
                log.warn("Failed to perform Trendz public synchronization: rate limit");
            }
        } catch (Exception e) {
            // Unauthenticated endpoint: never leak the reason back to the caller.
            log.error("Failed to perform Trendz public synchronization.", e);
        }
    }

    private static TrendzSynchronizationResult notInitialized() {
        return new TrendzSynchronizationResult(null, 0L,
                TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE);
    }

}
