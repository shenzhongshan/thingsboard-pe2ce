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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

/**
 * CE port of PE's custom translation API. Level (SYSTEM/TENANT/CUSTOMER) is resolved from the
 * caller's authority, same as {@link WhiteLabelingController} — no explicit level path segments.
 * A TENANT_ADMIN may act on behalf of one of their customers via the optional customerId param.
 * Reuses the existing Resource.WHITE_LABELING permission (matches PE, no dedicated resource).
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomTranslationController extends BaseController {

    private final CustomTranslationService customTranslationService;

    @ApiOperation(value = "Get merged custom translation (getMergedCustomTranslation)",
            notes = "Returns the effective translation-key overrides for the given locale, deep-merged from SYSTEM down through TENANT and (if applicable) CUSTOMER level.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/custom/merged/{localeCode}")
    public JsonNode getMergedCustomTranslation(
            @PathVariable String localeCode,
            @Parameter(description = "A string value representing the customer id, for a tenant admin previewing a customer's effective translation")
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        Owner owner = resolveOwner(customerId, Operation.READ);
        return customTranslationService.getMergedCustomTranslation(owner.tenantId, owner.customerId, localeCode);
    }

    @ApiOperation(value = "Get own-level custom translation (getCustomTranslation)",
            notes = "Returns the raw (unmerged) translation-key overrides stored at the caller's own level, for editing.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/custom/{localeCode}")
    public JsonNode getCustomTranslation(
            @PathVariable String localeCode,
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        Owner owner = resolveOwner(customerId, Operation.READ);
        CustomTranslation customTranslation = customTranslationService.getCustomTranslation(owner.tenantId, owner.customerId, localeCode);
        return customTranslation != null ? customTranslation.getValue() : null;
    }

    @ApiOperation(value = "List locale codes customized at the caller's own level (getCustomizedLocales)")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/custom/locales")
    public List<String> getCustomizedLocales(
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        Owner owner = resolveOwner(customerId, Operation.READ);
        return customTranslationService.getCustomizedLocales(owner.tenantId, owner.customerId);
    }

    @ApiOperation(value = "Save custom translation (saveCustomTranslation)",
            notes = "Full replace of the translation-key overrides at the caller's own level.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/translation/custom/{localeCode}")
    public JsonNode saveCustomTranslation(
            @PathVariable String localeCode,
            @RequestBody JsonNode value,
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        Owner owner = resolveOwner(customerId, Operation.WRITE);
        return customTranslationService.saveCustomTranslation(owner.tenantId, owner.customerId, localeCode, value).getValue();
    }

    @ApiOperation(value = "Patch custom translation (patchCustomTranslation)",
            notes = "Deep-merges the given keys into the existing overrides at the caller's own level.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PatchMapping(value = "/translation/custom/{localeCode}")
    public JsonNode patchCustomTranslation(
            @PathVariable String localeCode,
            @RequestBody JsonNode patch,
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        Owner owner = resolveOwner(customerId, Operation.WRITE);
        return customTranslationService.patchCustomTranslation(owner.tenantId, owner.customerId, localeCode, patch).getValue();
    }

    @ApiOperation(value = "Delete one custom translation key (deleteCustomTranslationKey)")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/translation/custom/{localeCode}/{keyPath}")
    public void deleteCustomTranslationKey(
            @PathVariable String localeCode,
            @PathVariable String keyPath,
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        Owner owner = resolveOwner(customerId, Operation.WRITE);
        customTranslationService.deleteCustomTranslationKey(owner.tenantId, owner.customerId, localeCode, keyPath);
    }

    @ApiOperation(value = "Delete custom translation (deleteCustomTranslation)",
            notes = "Deletes the whole override record at the caller's own level for the given locale.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/translation/custom/{localeCode}")
    public void deleteCustomTranslation(
            @PathVariable String localeCode,
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        Owner owner = resolveOwner(customerId, Operation.WRITE);
        customTranslationService.deleteCustomTranslation(owner.tenantId, owner.customerId, localeCode);
    }

    private record Owner(TenantId tenantId, CustomerId customerId) {}

    private Owner resolveOwner(String strCustomerId, Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
        SecurityUser currentUser = getCurrentUser();
        Authority authority = currentUser.getAuthority();
        if (Authority.SYS_ADMIN.equals(authority)) {
            return new Owner(TenantId.SYS_TENANT_ID, null);
        }
        if (Authority.TENANT_ADMIN.equals(authority)) {
            if (!StringUtils.isEmpty(strCustomerId)) {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, operation);
                return new Owner(getTenantId(), customerId);
            }
            return new Owner(getTenantId(), null);
        }
        return new Owner(getTenantId(), currentUser.getCustomerId());
    }

}
