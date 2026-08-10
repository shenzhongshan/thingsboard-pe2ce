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
package org.thingsboard.server.service.trendz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzIntegrationSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drives the Trendz handshake and records why it failed when it does.
 * <p>
 * Every exit path writes a {@link TrendzSynchronizationResult}, because the administrator's only
 * window into this is the settings page — a silent failure would look identical to never trying.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTrendzSyncService implements TrendzSyncService {

    private static final String MIN_SUPPORTED_VERSION = "1.15.0";

    private final ApiKeyService apiKeyService;
    private final UserService userService;
    private final TrendzSettingsService trendzSettingsService;
    private final SystemSecurityService systemSecurityService;
    private final TrendzClient trendzClient;

    @Value("${trendz.enabled:true}")
    private boolean trendzEnabled;
    @Value("${trendz.default_tb_url:}")
    private String defaultTbUrl;
    @Value("${trendz.default_trendz_url:}")
    private String defaultTrendzUrl;

    @Override
    public synchronized TrendzIntegrationSettings performSync() {
        log.trace("Executing performSync");
        if (!trendzEnabled) {
            return saveSettings(null, null, null, 0L, TrendzSynchronizationResultType.SYNC_DISABLED, TrendzSynchronizationStatus.NOT_AVAILABLE);
        }
        TrendzIntegrationSettings settings = trendzSettingsService.findTrendzIntegrationSettings();
        if (!isConfigured(settings)) {
            settings = createDefaultSettings(settings);
        }
        String tbUrl = settings.configuration().tbUrl();
        String trendzUrl = settings.configuration().trendzUrl();
        if (tbUrl == null || trendzUrl == null) {
            return saveSettings(trendzUrl, tbUrl, null, 0L, TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE);
        }
        log.trace("Starting Trendz synchronization. Trendz URL: {}, TB URL: {}", trendzUrl, tbUrl);

        long updatedTs = System.currentTimeMillis();
        ApiKey trendzApiKey = findOrCreateTrendzApiKey();
        TrendzInfo trendzInfo = validateTrendzConnection(trendzUrl, tbUrl, updatedTs);
        if (trendzInfo == null) {
            // validateTrendzConnection already recorded the specific reason.
            return trendzSettingsService.findTrendzIntegrationSettings();
        }
        // Trendz needs both URLs: the internal one to call us, the external one to link back to us.
        String externalTbUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, null);
        TrendzHealthcheckResult syncResult = trendzClient.processTrendzInitRequest(
                trendzUrl, tbUrl, externalTbUrl != null ? externalTbUrl : tbUrl, trendzApiKey.getValue(), null);
        if (syncResult == null) {
            return saveSettings(trendzUrl, tbUrl, trendzInfo.version(), updatedTs, TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR, TrendzSynchronizationStatus.AVAILABLE);
        }
        TrendzIntegrationSettings saved = saveSettings(trendzUrl, tbUrl, syncResult.version(), updatedTs, syncResult.type(), syncResult.status());
        if (syncResult.type() != TrendzSynchronizationResultType.SYNC_COMPLETED) {
            log.debug("Trendz sync failed. Status: {}, Message: {}", syncResult.type(), syncResult.message());
        }
        log.info("Trendz synchronization completed. Status: {}, Result: {}",
                saved.synchronizationResult().status(), saved.synchronizationResult().type());
        return saved;
    }

    @Override
    public void performSyncIfNeeded() {
        TrendzIntegrationSettings settings = trendzSettingsService.findTrendzIntegrationSettings();
        if (isSyncedUp(settings)) {
            log.trace("Trendz is already synced up. Status: {}, Result: {}",
                    settings.synchronizationResult().status(), settings.synchronizationResult().type());
            return;
        }
        performSync();
    }

    @Override
    public TrendzHealthcheckResult performHealthcheck() {
        log.trace("Executing performHealthcheck");
        if (!trendzEnabled) {
            return healthcheck(null, TrendzSynchronizationResultType.SYNC_DISABLED, TrendzSynchronizationStatus.NOT_AVAILABLE);
        }
        TrendzIntegrationSettings settings = trendzSettingsService.findTrendzIntegrationSettings();
        if (!isSyncedUp(settings)) {
            return healthcheck(null, TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE);
        }
        String knownVersion = settings.synchronizationResult().version();
        String trendzUrl = settings.configuration().trendzUrl();

        JsonNode rawResponse = trendzClient.checkTrendzReachability(trendzUrl);
        if (rawResponse == null) {
            return healthcheck(knownVersion, TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE, TrendzSynchronizationStatus.NOT_AVAILABLE);
        }
        TrendzInfo trendzInfo;
        try {
            trendzInfo = JacksonUtil.convertValue(rawResponse, TrendzInfo.class);
        } catch (Exception e) {
            return healthcheck(knownVersion, TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR, TrendzSynchronizationStatus.NOT_AVAILABLE);
        }
        if (trendzInfo != null && !isVersionSupported(trendzInfo.version())) {
            return healthcheck(knownVersion, TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION, TrendzSynchronizationStatus.AVAILABLE);
        }
        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey == null || !trendzApiKey.isInternal()) {
            return healthcheck(knownVersion, TrendzSynchronizationResultType.TRENDZ_AUTH_INVALID, TrendzSynchronizationStatus.AVAILABLE);
        }
        return trendzClient.sendHealthcheckRequest(trendzUrl, trendzApiKey.getValue());
    }

    @Override
    public void performApiKeyRotationSync(ApiKey newApiKey, ApiKey oldApiKey) {
        log.trace("Executing performApiKeyRotationSync");
        if (!trendzEnabled) {
            return;
        }
        try {
            if (!TRENDZ_API_KEY_DESCRIPTION.equals(newApiKey.getDescription()) || !newApiKey.isInternal()) {
                return;
            }
            TrendzIntegrationSettings settings = trendzSettingsService.findTrendzIntegrationSettings();
            if (settings == null || settings.configuration() == null) {
                log.debug("Trendz settings not found, cannot notify about key rotation");
                return;
            }
            String trendzUrl = settings.configuration().trendzUrl();
            String tbUrl = settings.configuration().tbUrl();
            if (StringUtils.isEmpty(trendzUrl) || StringUtils.isEmpty(tbUrl)) {
                log.debug("Trendz URL or TB URL not configured, cannot notify about key rotation");
                return;
            }
            String externalTbUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, null);
            // The previous key goes along so Trendz can accept both until it has switched over.
            trendzClient.processTrendzInitRequest(trendzUrl, tbUrl, externalTbUrl != null ? externalTbUrl : tbUrl,
                    newApiKey.getValue(), oldApiKey != null ? oldApiKey.getValue() : null);
        } catch (Exception e) {
            log.debug("Error notifying Trendz about API key rotation", e);
        }
    }

    private TrendzHealthcheckResult healthcheck(String version, TrendzSynchronizationResultType type, TrendzSynchronizationStatus status) {
        return new TrendzHealthcheckResult(version, type, status, type.getMessage());
    }

    private boolean isSyncedUp(TrendzIntegrationSettings settings) {
        return settings != null && settings.synchronizationResult() != null
                && settings.synchronizationResult().status() != null
                && settings.synchronizationResult().status() != TrendzSynchronizationStatus.NOT_AVAILABLE;
    }

    private boolean isConfigured(TrendzIntegrationSettings settings) {
        return settings != null && settings.configuration() != null
                && settings.configuration().tbUrl() != null && settings.configuration().trendzUrl() != null;
    }

    /** Seeds settings from the deployment's environment defaults, keeping anything already set. */
    private TrendzIntegrationSettings createDefaultSettings(TrendzIntegrationSettings previous) {
        TrendzConfiguration previousConfig = previous != null ? previous.configuration() : null;
        String trendzUrl = firstNonBlank(previousConfig != null ? previousConfig.trendzUrl() : null, defaultTrendzUrl);
        String tbUrl = firstNonBlank(previousConfig != null ? previousConfig.tbUrl() : null, defaultTbUrl);
        TrendzIntegrationSettings settings = new TrendzIntegrationSettings(new TrendzConfiguration(trendzUrl, tbUrl),
                new TrendzSynchronizationResult(null, 0L, TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE));
        trendzSettingsService.saveTrendzIntegrationSettings(settings);
        return settings;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (StringUtils.isNotBlank(preferred)) {
            return preferred;
        }
        return StringUtils.isNotBlank(fallback) ? fallback : null;
    }

    private TrendzInfo validateTrendzConnection(String trendzUrl, String tbUrl, long updatedTs) {
        JsonNode rawResponse = trendzClient.checkTrendzReachability(trendzUrl);
        if (rawResponse == null) {
            saveSettings(trendzUrl, tbUrl, null, updatedTs, TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE, TrendzSynchronizationStatus.NOT_AVAILABLE);
            log.debug("Trendz is not reachable at URL: {}", trendzUrl);
            return null;
        }
        TrendzInfo trendzInfo;
        try {
            trendzInfo = JacksonUtil.convertValue(rawResponse, TrendzInfo.class);
        } catch (Exception e) {
            saveSettings(trendzUrl, tbUrl, null, updatedTs, TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR, TrendzSynchronizationStatus.NOT_AVAILABLE);
            log.debug("Trendz version info is not recognized from URL: {}", trendzUrl, e);
            return null;
        }
        if (trendzInfo != null && !isVersionSupported(trendzInfo.version())) {
            saveSettings(trendzUrl, tbUrl, trendzInfo.version(), updatedTs, TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION, TrendzSynchronizationStatus.AVAILABLE);
            log.debug("Trendz version {} is not supported. Minimum required version: {}", trendzInfo.version(), MIN_SUPPORTED_VERSION);
            return null;
        }
        return trendzInfo;
    }

    /** Compares dotted version components, ignoring any {@code -SNAPSHOT}-style suffix. */
    private boolean isVersionSupported(String version) {
        if (StringUtils.isBlank(version)) {
            return false;
        }
        try {
            String[] parts = version.split("-")[0].split("\\.");
            String[] minParts = MIN_SUPPORTED_VERSION.split("\\.");
            for (int i = 0; i < Math.min(parts.length, minParts.length); i++) {
                int current = Integer.parseInt(parts[i]);
                int required = Integer.parseInt(minParts[i]);
                if (current != required) {
                    return current > required;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to parse Trendz version '{}': {}", version, e.getMessage());
            return false;
        }
    }

    private ApiKey findOrCreateTrendzApiKey() {
        ApiKey existing = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (existing != null && existing.isInternal()) {
            log.trace("Found existing Trendz API key: {}", existing.getId());
            return existing;
        }
        log.trace("Creating new Trendz internal API key with configured permissions");
        User sysAdmin = userService.findSysAdmins(new PageLink(1, 0, null, new SortOrder("createdTime", SortOrder.Direction.ASC)))
                .getData().get(0);
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setUserId(sysAdmin.getId());
        apiKeyInfo.setDescription(TRENDZ_API_KEY_DESCRIPTION);
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setExpirationTime(0L);
        apiKeyInfo.setInternal(true);
        apiKeyInfo.setPermissions(buildTrendzPermissions().toJson());
        return apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
    }

    /**
     * What Trendz is allowed to do when it calls back, whichever user it acts as. Deliberately
     * narrow: read entities and their data, manage alarms it raises, and read the metadata it needs
     * to render. Anything outside this list is refused even if the impersonated user could do it.
     */
    private AuthorityPermissionsInfo buildTrendzPermissions() {
        Set<Operation> readAndData = Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES,
                Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY);
        Map<Resource, Set<Operation>> tenantAndCustomer = new HashMap<>();
        tenantAndCustomer.put(Resource.DEVICE, readAndData);
        tenantAndCustomer.put(Resource.ASSET, readAndData);
        tenantAndCustomer.put(Resource.CUSTOMER, readAndData);
        tenantAndCustomer.put(Resource.ALARM, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        tenantAndCustomer.put(Resource.DASHBOARD, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE));
        tenantAndCustomer.put(Resource.TENANT, Set.of(Operation.READ));
        tenantAndCustomer.put(Resource.USER, Set.of(Operation.READ));
        tenantAndCustomer.put(Resource.WHITE_LABELING, Set.of(Operation.READ));
        tenantAndCustomer.put(Resource.DEVICE_PROFILE, Set.of(Operation.READ));
        tenantAndCustomer.put(Resource.ASSET_PROFILE, Set.of(Operation.READ));

        Map<Resource, Set<Operation>> sysAdmin = new HashMap<>();
        Set<Operation> manage = Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE);
        sysAdmin.put(Resource.ADMIN_SETTINGS, Set.of(Operation.READ, Operation.WRITE));
        sysAdmin.put(Resource.WIDGETS_BUNDLE, manage);
        sysAdmin.put(Resource.WIDGET_TYPE, manage);
        sysAdmin.put(Resource.TB_RESOURCE, manage);

        Map<Authority, Map<Resource, Set<Operation>>> permissions = new HashMap<>();
        permissions.put(Authority.TENANT_ADMIN, tenantAndCustomer);
        permissions.put(Authority.CUSTOMER_USER, tenantAndCustomer);
        permissions.put(Authority.SYS_ADMIN, sysAdmin);

        AuthorityPermissionsInfo info = new AuthorityPermissionsInfo();
        info.setOperationsByResource(permissions);
        return info;
    }

    private TrendzIntegrationSettings saveSettings(String trendzUrl, String tbUrl, String version, long updatedTs,
                                        TrendzSynchronizationResultType type, TrendzSynchronizationStatus status) {
        TrendzIntegrationSettings settings = new TrendzIntegrationSettings(new TrendzConfiguration(trendzUrl, tbUrl),
                new TrendzSynchronizationResult(version, updatedTs, type, status));
        trendzSettingsService.saveTrendzIntegrationSettings(settings);
        return settings;
    }

    /** Only the version matters to us; Trendz reports more that we deliberately ignore. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TrendzInfo(String version, List<String> features) {
    }

}
