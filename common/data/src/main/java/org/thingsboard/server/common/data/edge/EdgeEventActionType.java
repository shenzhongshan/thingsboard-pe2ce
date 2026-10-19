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
package org.thingsboard.server.common.data.edge;

import lombok.Getter;
import org.thingsboard.server.common.data.audit.ActionType;

@Getter
public enum EdgeEventActionType {
    ADDED(ActionType.ADDED),
    UPDATED(ActionType.UPDATED),
    DELETED(ActionType.DELETED),
    POST_ATTRIBUTES(null),
    ATTRIBUTES_UPDATED(ActionType.ATTRIBUTES_UPDATED),
    ATTRIBUTES_DELETED(ActionType.ATTRIBUTES_DELETED),
    TIMESERIES_UPDATED(ActionType.TIMESERIES_UPDATED),
    CREDENTIALS_UPDATED(ActionType.CREDENTIALS_UPDATED),
    ASSIGNED_TO_CUSTOMER(ActionType.ASSIGNED_TO_CUSTOMER),
    UNASSIGNED_FROM_CUSTOMER(ActionType.UNASSIGNED_FROM_CUSTOMER),
    RELATION_ADD_OR_UPDATE(ActionType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(ActionType.RELATION_DELETED),
    RPC_CALL(ActionType.RPC_CALL),
    ALARM_ACK(ActionType.ALARM_ACK),
    ALARM_CLEAR(ActionType.ALARM_CLEAR),
    ALARM_DELETE(ActionType.ALARM_DELETE),
    ALARM_ASSIGNED(ActionType.ALARM_ASSIGNED),
    ALARM_UNASSIGNED(ActionType.ALARM_UNASSIGNED),
    ADDED_COMMENT(ActionType.ADDED_COMMENT),
    UPDATED_COMMENT(ActionType.UPDATED_COMMENT),
    DELETED_COMMENT(ActionType.DELETED_COMMENT),
    ASSIGNED_TO_EDGE(ActionType.ASSIGNED_TO_EDGE),
    UNASSIGNED_FROM_EDGE(ActionType.UNASSIGNED_FROM_EDGE),
    // 分组成员变化。实测过（probe_group_membership_events.py）：没有这两个 action 时，
    // 往"已分配给 edge 的分组"里加/删实体，edge 一条事件都收不到，只能等下一次全量同步。
    // 与 PE 同名；PE 把它们分别并入 ENTITY_CREATED / ENTITY_DELETED 下行消息。
    ADDED_TO_ENTITY_GROUP(ActionType.ADDED_TO_ENTITY_GROUP),
    REMOVED_FROM_ENTITY_GROUP(ActionType.REMOVED_FROM_ENTITY_GROUP),
    CREDENTIALS_REQUEST(null), // deprecated
    ENTITY_MERGE_REQUEST(null); // deprecated

    private final ActionType actionType;

    EdgeEventActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}
