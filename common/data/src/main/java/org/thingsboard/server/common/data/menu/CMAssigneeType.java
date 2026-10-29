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
package org.thingsboard.server.common.data.menu;

/**
 * Who a custom menu is assigned to. CUSTOMERS/USERS assignment is implemented via a direct
 * customMenuId reference on Customer/User (see BaseCustomMenuService), not a join table.
 * USER_GROUPS matches by {@link CustomMenuInfo#getUserGroupNames()} against the entity-group
 * names the resolving user directly belongs to (see EntityGroupService) — a name match, not an
 * id reference, because the same group name ("Customer Users", "Customer Administrators") is
 * reused by every customer, matching PE.
 */
public enum CMAssigneeType {
    NO_ASSIGN,
    ALL,
    CUSTOMERS,
    USERS,
    USER_GROUPS
}
