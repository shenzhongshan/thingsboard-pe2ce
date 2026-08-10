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
package org.thingsboard.server.service.security.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.security.permission.MergedUserPermissions;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecurityUser extends User {

    private static final long serialVersionUID = -797397440703066079L;

    private Collection<GrantedAuthority> authorities;
    @Getter @Setter
    private boolean enabled;
    @Getter @Setter
    private UserPrincipal userPrincipal;
    @Getter @Setter
    private String sessionId = UUID.randomUUID().toString();
    @Getter @Setter
    private MergedUserPermissions userPermissions;
    /**
     * Set when the caller authenticated with an internal API key that declares a permission
     * ceiling. Those permissions then cap the request on top of the usual authority checks, which
     * on their own would grant a tenant admin everything within the tenant.
     */
    @Getter @Setter
    private boolean permissionsRestricted;

    public SecurityUser() {
        super();
    }

    public SecurityUser(UserId id) {
        super(id);
    }

    public SecurityUser(User user, boolean enabled, UserPrincipal userPrincipal) {
        super(user);
        this.enabled = enabled;
        this.userPrincipal = userPrincipal;
    }

    public SecurityUser(User user, boolean enabled, UserPrincipal userPrincipal, MergedUserPermissions userPermissions) {
        super(user);
        this.enabled = enabled;
        this.userPrincipal = userPrincipal;
        this.userPermissions = userPermissions;
    }

    /**
     * The entity that owns this user: its customer when customer-scoped, otherwise its tenant.
     * Mirrors the {@code getOwnerId()} convention used by Device/Asset/etc.
     */
    @JsonIgnore
    public EntityId getOwnerId() {
        return getCustomerId() != null && !getCustomerId().isNullUid() ? getCustomerId() : getTenantId();
    }

    public Collection<GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            authorities = Stream.of(SecurityUser.this.getAuthority())
                    .map(authority -> new SimpleGrantedAuthority(authority.name()))
                    .collect(Collectors.toList());
        }
        return authorities;
    }

}
