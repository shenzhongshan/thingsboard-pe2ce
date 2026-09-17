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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.EnumMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public final class EdgeUtils {

    private static final EnumMap<EntityType, EdgeEventType> entityTypeEdgeEventTypeEnumMap;
    private static final EnumMap<ActionType, EdgeEventActionType> actionTypeEdgeEventActionTypeEnumMap;

    static {
        entityTypeEdgeEventTypeEnumMap = new EnumMap<>(EntityType.class);
        for (EdgeEventType edgeEventType : EdgeEventType.values()) {
            if (edgeEventType.getEntityType() != null) {
                entityTypeEdgeEventTypeEnumMap.put(edgeEventType.getEntityType(), edgeEventType);
            }
        }

        actionTypeEdgeEventActionTypeEnumMap = new EnumMap<>(ActionType.class);
        for (EdgeEventActionType edgeEventActionType : EdgeEventActionType.values()) {
            if (edgeEventActionType.getActionType() != null) {
                actionTypeEdgeEventActionTypeEnumMap.put(edgeEventActionType.getActionType(), edgeEventActionType);
            }
        }
    }

    private static final int STACK_TRACE_LIMIT = 10;

    private EdgeUtils() {}

    public static int nextPositiveInt() {
        return ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    }

    public static EdgeEventType getEdgeEventTypeByEntityType(EntityType entityType) {
        return entityTypeEdgeEventTypeEnumMap.get(entityType);
    }

    public static EdgeEventActionType getEdgeEventActionTypeByActionType(ActionType actionType) {
        return actionTypeEdgeEventActionTypeEnumMap.get(actionType);
    }

    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               EntityId entityId,
                                               JsonNode body) {
        return constructEdgeEvent(tenantId, edgeId, type, action, entityId, body, null);
    }

    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               EntityId entityId,
                                               JsonNode body,
                                               EntityId entityGroupId) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        if (entityGroupId != null) {
            edgeEvent.setEntityGroupId(entityGroupId.getId());
        }
        edgeEvent.setBody(body);
        return edgeEvent;
    }

    /**
     * PE 为每台 edge 在其 owner 下自动维护一组名为 "[Edge] &lt;edgeName&gt; All"（客户所有时为
     * "[Edge][&lt;customerName&gt;] &lt;edgeName&gt; All"）的实体分组，代表"这台 edge 上有哪些实体"。
     * 这些分组本身不能再下发给 edge（会循环），所以事件溯源链路要按名字把它们跳过。
     */
    public static boolean isEdgeGroupAll(String groupName) {
        if (groupName == null) {
            return false;
        }
        return groupName.startsWith(EntityGroup.GROUP_EDGE_ALL_STARTS_WITH)
                && groupName.endsWith(EntityGroup.GROUP_EDGE_ALL_ENDS_WITH);
    }

    public static String getEdgeGroupAllName(String edgeName) {
        return getEdgeGroupAllName(null, edgeName);
    }

    public static String getEdgeGroupAllName(String customerName, String edgeName) {
        if (customerName != null) {
            return String.format(EntityGroup.GROUP_EDGE_CUSTOMER_ALL_NAME_PATTERN, customerName, edgeName);
        }
        return String.format(EntityGroup.GROUP_EDGE_ALL_NAME_PATTERN, edgeName);
    }

    // ---- edge 模板的参数化占位符 ----
    // 一份集成模板要下发到 N 台 edge，每台的 broker 地址/凭据都不同。PE 的做法是配置里写
    // ${{attrKey}}，下发时用该 edge 自己的 SERVER_SCOPE 属性替换。注意是**双大括号**，
    // 与规则引擎的 ${} 单括号不是一回事。
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\$\\{\\{)(.*?)(}})");
    private static final String ATTRIBUTE_PLACEHOLDER_PATTERN = "${{%s}}";
    private static final String ATTRIBUTE_REGEXP_PLACEHOLDER_PATTERN = "\\$\\{\\{%s}}";

    /** 从配置 JSON 里挑出所有 ${{key}} 的 key。 */
    public static Set<String> getAttributeKeysFromConfiguration(String configuration) {
        Set<String> keys = new HashSet<>();
        if (configuration == null) {
            return keys;
        }
        Matcher m = ATTRIBUTE_PATTERN.matcher(configuration);
        while (m.find()) {
            keys.add(m.group(2));
        }
        return keys;
    }

    public static String formatAttributeKeyToPlaceholderFormat(String attributeKey) {
        return String.format(ATTRIBUTE_PLACEHOLDER_PATTERN, attributeKey);
    }

    /** 用于 String.replaceAll 的正则形式（大括号和 $ 都要转义）。 */
    public static String formatAttributeKeyToRegexpPlaceholderFormat(String attributeKey) {
        return String.format(ATTRIBUTE_REGEXP_PLACEHOLDER_PATTERN, attributeKey);
    }

    public static String createErrorMsgFromRootCauseAndStackTrace(Throwable t) {
        Throwable rootCause = Throwables.getRootCause(t);
        StringBuilder errorMsg = new StringBuilder(rootCause.getMessage() != null ? rootCause.getMessage() : "");
        if (rootCause.getStackTrace().length > 0) {
            int idx = 0;
            for (StackTraceElement stackTraceElement : rootCause.getStackTrace()) {
                errorMsg.append("\n").append(stackTraceElement.toString());
                idx++;
                if (idx > STACK_TRACE_LIMIT) {
                    break;
                }
            }
        }
        return errorMsg.toString();
    }
}
