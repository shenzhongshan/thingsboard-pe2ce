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
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.UserId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class UserInfo extends User {

    private static final long serialVersionUID = 8221690143714368789L;

    @Schema(description = "Comma-separated names of the Entity Groups this user belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
    private String groups;

    @Schema(description = "Title of the owning Customer; absent for tenant-level users.", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    public UserInfo() {
        super();
    }

    public UserInfo(UserId userId) {
        super(userId);
    }

    public UserInfo(User user) {
        super(user);
    }

    public UserInfo(User user, String groups) {
        super(user);
        this.groups = groups;
    }
}
