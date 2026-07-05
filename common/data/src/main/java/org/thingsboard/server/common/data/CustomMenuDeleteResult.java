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
import org.thingsboard.server.common.data.menu.CMAssigneeType;

import java.util.Collections;
import java.util.List;

/**
 * Result of a custom menu delete attempt: if the menu is still assigned to customers/users,
 * deletion is blocked (success=false) unless the caller passes force=true, and the currently
 * assigned entities are returned so the UI can show what would be affected.
 */
@Schema
public class CustomMenuDeleteResult {

    private final boolean success;
    private final CMAssigneeType assigneeType;
    private final List<EntityInfo> assigneeList;

    public CustomMenuDeleteResult(boolean success, CMAssigneeType assigneeType, List<EntityInfo> assigneeList) {
        this.success = success;
        this.assigneeType = assigneeType;
        this.assigneeList = assigneeList == null ? Collections.emptyList() : assigneeList;
    }

    public static CustomMenuDeleteResult success() {
        return new CustomMenuDeleteResult(true, null, null);
    }

    public static CustomMenuDeleteResult blocked(CMAssigneeType assigneeType, List<EntityInfo> assigneeList) {
        return new CustomMenuDeleteResult(false, assigneeType, assigneeList);
    }

    public boolean isSuccess() {
        return success;
    }

    public CMAssigneeType getAssigneeType() {
        return assigneeType;
    }

    public List<EntityInfo> getAssigneeList() {
        return assigneeList;
    }
}
