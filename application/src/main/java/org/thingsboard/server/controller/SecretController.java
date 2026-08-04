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

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.secret.TbSecretService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * Tenant-scoped credential vault. A secret's value is write-only: it is accepted on create/update
 * but never returned by any endpoint here. Consumers (AI Model config, rule node config, mail/SMS
 * settings, etc.) reference a secret by embedding {@code ${secret:<name>;type:<type>}} directly in
 * the same text field that would otherwise hold the raw plaintext credential; the platform resolves
 * it server-side at the point of use.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SecretController extends BaseController {

    private final TbSecretService tbSecretService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping("/secret")
    public SecretInfo saveSecret(@Parameter(description = "A JSON value representing the Secret.", required = true)
                                  @RequestBody @Valid Secret secret) throws Exception {
        secret.setTenantId(getTenantId());
        checkEntity(secret.getId(), secret, Resource.SECRET);
        return tbSecretService.save(secret, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping("/secret/{id}/description")
    public SecretInfo updateSecretDescription(@PathVariable UUID id,
                                               @RequestBody Optional<String> description) throws Exception {
        SecretId secretId = new SecretId(id);
        Secret secret = new Secret(checkSecretId(secretId, Operation.WRITE));
        secret.setDescription(description.orElse(null));
        return tbSecretService.save(secret, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping("/secret/{id}/value")
    public SecretInfo updateSecretValue(@PathVariable UUID id, @RequestBody String value) throws Exception {
        SecretId secretId = new SecretId(id);
        Secret secret = new Secret(checkSecretId(secretId, Operation.WRITE));
        secret.setValue(value);
        return tbSecretService.save(secret, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping("/secret/{id}")
    public ResponseEntity<TbSecretDeleteResult> deleteSecret(@PathVariable UUID id) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        SecretInfo secretInfo = checkSecretId(secretId, Operation.DELETE);
        TbSecretDeleteResult result = tbSecretService.delete(secretInfo, getCurrentUser());
        return (result.isSuccess() ? ResponseEntity.ok() : ResponseEntity.badRequest()).body(result);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/secrets", params = {"pageSize", "page"})
    public PageData<SecretInfo> getSecretInfos(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                @RequestParam int pageSize,
                                                @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                @RequestParam int page,
                                                @Parameter(description = "The case insensitive 'substring' filter based on the secret name and description.")
                                                @RequestParam(required = false) String textSearch,
                                                @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                @RequestParam(required = false) String sortProperty,
                                                @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.SECRET, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(secretService.findSecretInfosByTenantId(getTenantId(), pageLink));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/secret/names")
    public List<String> getSecretNames() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.SECRET, Operation.READ);
        return checkNotNull(secretService.findSecretNamesByTenantId(getTenantId()));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/secret/{id}/info")
    public SecretInfo getSecretInfoById(@PathVariable UUID id) throws ThingsboardException {
        SecretId secretId = new SecretId(id);
        return checkSecretId(secretId, Operation.READ);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/secret")
    public SecretInfo getSecretInfoByName(@RequestParam String name) throws ThingsboardException {
        checkParameter("name", name);
        accessControlService.checkPermission(getCurrentUser(), Resource.SECRET, Operation.READ);
        return checkNotNull(secretService.findSecretInfoByName(getTenantId(), name));
    }

}
