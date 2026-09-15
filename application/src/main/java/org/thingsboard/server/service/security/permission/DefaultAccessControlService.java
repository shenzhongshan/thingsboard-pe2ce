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
package org.thingsboard.server.service.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.EnumSet;
import java.util.Set;

@Service
@Slf4j
public class DefaultAccessControlService implements AccessControlService {

    private static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";

    private final Map<Authority, Permissions> authorityPermissions = new HashMap<>();

    public DefaultAccessControlService(SysAdminPermissions sysAdminPermissions,
                                       TenantAdminPermissions tenantAdminPermissions,
                                       CustomerUserPermissions customerUserPermissions,
                                       MfaConfigurationPermissions mfaConfigurationPermissions) {
        authorityPermissions.put(Authority.SYS_ADMIN, sysAdminPermissions);
        authorityPermissions.put(Authority.TENANT_ADMIN, tenantAdminPermissions);
        authorityPermissions.put(Authority.CUSTOMER_USER, customerUserPermissions);
        authorityPermissions.put(Authority.MFA_CONFIGURATION_TOKEN, mfaConfigurationPermissions);
    }

    @Override
    public Set<Resource> getAllowedResources(Authority authority) {
        Permissions permissions = authorityPermissions.get(authority);
        if (permissions instanceof AbstractPermissions abstractPermissions) {
            // The keySet is the map of permission *checkers*, which is not quite the same thing as
            // the vocabulary of resources a role may be written against. Resource.ALL is the
            // difference: it never needs a checker of its own (MergedUserPermissions#hasGenericPermission
            // consults the ALL entry before the per-resource one), but it must be grantable - the
            // stock "Tenant Administrator" and "Customer Administrator" roles are literally
            // {"ALL":["ALL"]}, and without this the role editor cannot express them.
            // PE keeps the two apart as Resource.resourcesByAuthority vs its permission checkers.
            Set<Resource> resources = EnumSet.copyOf(abstractPermissions.keySet());
            resources.add(Resource.ALL);
            return Set.copyOf(resources);
        }
        return Set.of();
    }

    @Override
    public void checkPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException {
        checkRestriction(user, resource, operation);
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        if (!permissionChecker.hasPermission(user, operation)) {
            permissionDenied();
        }
    }

    /**
     * An internal API key declares what it may do; that list caps the request regardless of how
     * powerful the impersonated user is. Applied before the normal authority check, never instead
     * of it, so a restricted caller can only ever do less than the user it acts as.
     */
    private void checkRestriction(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException {
        if (user.isPermissionsRestricted()
                && (user.getUserPermissions() == null || !user.getUserPermissions().hasGenericPermission(resource, operation))) {
            permissionDenied();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException {
        checkRestriction(user, resource, operation);
        var permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        return permissionChecker.hasPermission(user, operation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends EntityId, T extends HasTenantId> void checkPermission(SecurityUser user, Resource resource,
                                                                            Operation operation, I entityId, T entity) throws ThingsboardException {
        checkRestriction(user, resource, operation);
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        if (!permissionChecker.hasPermission(user, operation, entityId, entity)) {
            permissionDenied();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends EntityId, T extends HasTenantId> boolean hasPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException {
        checkRestriction(user, resource, operation);
        var permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        return permissionChecker.hasPermission(user, operation, entityId, entity);
    }

    private PermissionChecker getPermissionChecker(Authority authority, Resource resource) throws ThingsboardException {
        Permissions permissions = authorityPermissions.get(authority);
        if (permissions == null) {
            permissionDenied();
        }
        Optional<PermissionChecker> permissionChecker = permissions.getPermissionChecker(resource);
        if (permissionChecker.isEmpty()) {
            permissionDenied();
        }
        return permissionChecker.get();
    }

    private void permissionDenied() throws ThingsboardException {
        throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

}
