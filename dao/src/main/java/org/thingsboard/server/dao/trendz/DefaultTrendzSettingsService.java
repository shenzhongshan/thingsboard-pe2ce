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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzIntegrationSettings;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.Optional;

/**
 * Both kinds of Trendz settings live in {@code admin_settings}, under separate keys so the
 * per-tenant widget configuration and the system-wide integration state cannot overwrite each other
 * at the system tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrendzSettingsService implements TrendzSettingsService {

    private static final String SETTINGS_KEY = "trendz";
    private static final String INTEGRATION_SETTINGS_KEY = "trendzIntegration";

    private final AdminSettingsService adminSettingsService;

    @Override
    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "#tenantId")
    public TrendzSettings saveTrendzSettings(TenantId tenantId, TrendzSettings settings) {
        log.trace("Executing saveTrendzSettings [{}][{}]", tenantId, settings);
        save(tenantId, SETTINGS_KEY, settings);
        return settings;
    }

    @Override
    @Cacheable(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "#tenantId")
    public TrendzSettings findTrendzSettings(TenantId tenantId) {
        log.trace("Executing findTrendzSettings [{}]", tenantId);
        return find(tenantId, SETTINGS_KEY, TrendzSettings.class);
    }

    @Override
    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "#tenantId")
    public void deleteTrendzSettings(TenantId tenantId) {
        log.trace("Executing deleteTrendzSettings [{}]", tenantId);
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY);
    }

    @Override
    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'integration'")
    public void saveTrendzIntegrationSettings(TrendzIntegrationSettings settings) {
        log.trace("Executing saveTrendzIntegrationSettings [{}]", settings);
        save(TenantId.SYS_TENANT_ID, INTEGRATION_SETTINGS_KEY, settings);
    }

    @Override
    @Cacheable(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'integration'")
    public TrendzIntegrationSettings findTrendzIntegrationSettings() {
        log.trace("Executing findTrendzIntegrationSettings");
        return find(TenantId.SYS_TENANT_ID, INTEGRATION_SETTINGS_KEY, TrendzIntegrationSettings.class);
    }

    @Override
    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'integration'")
    public void deleteTrendzIntegrationSettings() {
        log.trace("Executing deleteTrendzIntegrationSettings");
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, INTEGRATION_SETTINGS_KEY);
    }

    private void save(TenantId tenantId, String key, Object value) {
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, key))
                .orElseGet(() -> {
                    AdminSettings created = new AdminSettings();
                    created.setTenantId(tenantId);
                    created.setKey(key);
                    return created;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(value));
        adminSettingsService.saveAdminSettings(tenantId, adminSettings);
    }

    private <T> T find(TenantId tenantId, String key, Class<T> type) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, key))
                .map(AdminSettings::getJsonValue)
                .map(json -> JacksonUtil.treeToValue(json, type))
                .orElse(null);
    }

}
