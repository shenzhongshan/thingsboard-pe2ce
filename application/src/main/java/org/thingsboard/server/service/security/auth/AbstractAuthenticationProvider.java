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
package org.thingsboard.server.service.security.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.service.security.permission.MergedUserPermissions;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAuthenticationProvider implements AuthenticationProvider {

    private final CustomerService customerService;
    private final UserAuthDetailsCache userAuthDetailsCache;

    protected SecurityUser authenticateByPublicId(String publicId, String authContextName, UserPrincipal userPrincipal) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException(authContextName + " is not valid");
        }
        Customer publicCustomer = customerService.findCustomerById(systemId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found");
        }

        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException(authContextName + " is not valid");
        }

        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        UserPrincipal principal = userPrincipal == null ? new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId) : userPrincipal;

        return new SecurityUser(user, true, principal);
    }

    protected SecurityUser authenticateByUserId(TenantId tenantId, UserId userId) {
        return authenticateByUserId(tenantId, userId, null);
    }

    /**
     * When {@code permissionsInfo} is present it <em>replaces</em> the user's own permissions rather
     * than adding to them: an internal API key is a ceiling on what may be done, while the user
     * identity still decides whose data is in scope.
     */
    protected SecurityUser authenticateByUserId(TenantId tenantId, UserId userId, AuthorityPermissionsInfo permissionsInfo) {
        UserAuthDetails userAuthDetails = userAuthDetailsCache.getUserAuthDetails(tenantId, userId);
        if (userAuthDetails == null) {
            throw new UsernameNotFoundException("User with credentials not found");
        }
        if (!userAuthDetails.credentialsEnabled()) {
            throw new DisabledException("User is not active");
        }

        User user = userAuthDetails.user();
        if (user.getAuthority() == null) {
            throw new InsufficientAuthenticationException("User has no authority assigned");
        }

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        if (permissionsInfo != null) {
            Map<Resource, Set<Operation>> permissions = permissionsInfo.getPermissionsForAuthority(user.getAuthority());
            if (permissions != null && !permissions.isEmpty()) {
                SecurityUser securityUser = new SecurityUser(user, true, userPrincipal,
                        new MergedUserPermissions(permissions, Collections.emptyMap()));
                securityUser.setPermissionsRestricted(true);
                return securityUser;
            }
        }
        return new SecurityUser(user, true, userPrincipal);
    }

}
