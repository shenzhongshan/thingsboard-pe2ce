/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.thingsboard.server.service.deviceLog.service.impl;

import com.fasterxml.jackson.databind.JsonNode;

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelineFieldDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelineItemDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 将设备审计、告警和 active 状态记录转换为时间线展示事件，不执行权限或数据查询。 */
final class DeviceLogAuditTimelineConverter {

    /**
     * 收集当前审计页关系两端的实体 ID，供服务层批量查询名称。
     */
    static Set<EntityId> collectRelationEntityIds(List<AuditLog> auditLogs) {
        if (auditLogs == null || auditLogs.isEmpty()) {
            return Set.of();
        }
        Set<EntityId> entityIds = new HashSet<>();
        for (AuditLog auditLog : auditLogs) {
            if (auditLog == null) {
                continue;
            }
            ActionType actionType = auditLog.getActionType();
            if (!ActionType.RELATION_ADD_OR_UPDATE.equals(actionType)
                    && !ActionType.RELATION_DELETED.equals(actionType)) {
                continue;
            }
            JsonNode actionData = auditLog.getActionData();
            JsonNode relation = actionData == null || !actionData.isObject() ? null : actionData.get("relation");
            if (relation == null || !relation.isObject()) {
                continue;
            }
            addRelationEntityId(entityIds, relation.get("from"));
            addRelationEntityId(entityIds, relation.get("to"));
        }
        return entityIds;
    }

    /**
     * 把 active 历史值转换为时间线事件。
     *
     * <p>查询结果可能是乱序的，因此先按时间升序排列，再删除连续相同状态，最后按时间倒序返回。
     * 这样一个持续在线期间不会重复显示大量相同事件，同时仍保留每次状态切换的原始时间。</p>
     *
     * @param entries active 时序记录
     * @return 按时间倒序排列的设备上线/下线事件
     */
    static List<DeviceLogTimelineItemDTO> toActivityTimelineItems(List<TsKvEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        // 先按时间升序处理，以便准确识别相邻状态切换。
        List<TsKvEntry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort(Comparator.comparingLong(TsKvEntry::getTs));

        // 丢弃非法值和连续重复状态，只为状态变化生成事件。
        List<DeviceLogTimelineItemDTO> result = new ArrayList<>();
        Boolean previousState = null;
        for (TsKvEntry entry : sortedEntries) {
            Boolean state = parseActivityState(entry);
            if (state == null || state.equals(previousState)) {
                continue;
            }
            previousState = state;
            String statusText = state ? "在线" : "离线";
            result.add(new DeviceLogTimelineItemDTO(
                    entry.getTs(),
                    "设备状态",
                    state ? "设备上线" : "设备下线",
                    "设备状态变更为" + statusText,
                    List.of(new DeviceLogTimelineFieldDTO("状态", statusText))
            ));
        }
        // 时间线统一倒序展示，最新状态变化排在前面。
        result.sort(Comparator.comparingLong(DeviceLogTimelineItemDTO::getTimestamp).reversed());
        return result;
    }

    /** 解析 active 的布尔值，并兼容少数存储实现返回的 1/0 文本。 */
    private static Boolean parseActivityState(TsKvEntry entry) {
        if (entry == null || entry.getTs() <= 0 || !"active".equals(entry.getKey())) {
            return null;
        }
        Boolean booleanValue = entry.getBooleanValue().orElse(null);
        if (booleanValue != null) {
            return booleanValue;
        }
        String textValue = entry.getValueAsString();
        if ("true".equalsIgnoreCase(textValue) || "1".equals(textValue)) {
            return true;
        }
        if ("false".equalsIgnoreCase(textValue) || "0".equals(textValue)) {
            return false;
        }
        return null;
    }

    /**
     * 将一条平台审计记录转换为前端时间线事件。
     *
     * <p>审计事件统一显示中文标题、原始动作类型和操作人；客户分配、服务器属性变更、
     * 告警确认/清除/派发等动作会从 actionData 中补充业务字段。</p>
     *
     * @param auditLog 平台审计日志
     * @return 前端时间线事件模型
     */
    static DeviceLogTimelineItemDTO toAuditTimelineItem(AuditLog auditLog,
                                                         Map<EntityId, EntityInfo> relationEntityInfos) {
        ActionType actionType = auditLog.getActionType();
        // 公共字段先记录操作人，再根据动作类型补充客户、属性、关系或告警详情。
        List<DeviceLogTimelineFieldDTO> fields = new ArrayList<>();
        fields.add(new DeviceLogTimelineFieldDTO("操作人", resolveOperator(auditLog)));

        if (ActionType.ASSIGNED_TO_CUSTOMER.equals(actionType)) {
            addActionDataField(fields, auditLog.getActionData(), "assignedCustomerName", "分配到客户");
        } else if (ActionType.UNASSIGNED_FROM_CUSTOMER.equals(actionType)) {
            addActionDataField(fields, auditLog.getActionData(), "unassignedCustomerName", "取消分配前客户");
        }
        if (ActionType.ATTRIBUTES_UPDATED.equals(actionType)
                || ActionType.ATTRIBUTES_DELETED.equals(actionType)) {
            addAttributeChangeFields(fields, auditLog.getActionData(), actionType);
        }
        if (ActionType.RELATION_ADD_OR_UPDATE.equals(actionType)
                || ActionType.RELATION_DELETED.equals(actionType)) {
            addRelationChangeFields(fields, auditLog.getActionData(), relationEntityInfos);
        }
        if (actionType != null && actionType.name().startsWith("ALARM_")) {
            // 确认、清除和派发的审计数据通常把告警放在 entity.id 中，删除动作则使用 alarmId 字段。
            String alarmId = findAlarmId(auditLog.getActionData());
            if (!alarmId.isBlank()) {
                fields.add(new DeviceLogTimelineFieldDTO("告警 ID", alarmId));
            }
        }
        // 审计事件使用平台记录的创建时间，标题由动作枚举统一映射。
        return new DeviceLogTimelineItemDTO(
                auditLog.getCreatedTime(),
                "审计事件",
                formatActionTitle(actionType),
                "",
                fields
        );
    }

    /**
     * 将关系审计数据展开为前端可读字段。
     *
     * <p>关系审计的 actionData.relation 包含 from、to、type、typeGroup 和 additionalInfo。
     * from/to 的名称已经由当前页批量查询得到；如果实体已删除、无权限或数据格式异常，
     * 则回退为实体类型和 UUID，保证历史日志仍然可追溯。</p>
     *
     * @param fields 当前时间线事件字段集合
     * @param actionData 审计日志中的关系动作数据
     * @param relationEntityInfos 当前时间线页中关系端点的名称索引
     */
    private static void addRelationChangeFields(List<DeviceLogTimelineFieldDTO> fields, JsonNode actionData,
                                         Map<EntityId, EntityInfo> relationEntityInfos) {
        // 关系对象由审计服务写入 actionData.relation；缺失时无法解析关系详情。
        if (actionData == null || !actionData.isObject()) {
            return;
        }
        JsonNode relation = actionData.get("relation");
        if (relation == null || !relation.isObject()) {
            return;
        }

        // from 和 to 表示关系方向，格式为“实体类型/实体 ID”。
        addRelationEndpointField(fields, relation.get("from"), "关系起点", relationEntityInfos);
        addRelationEndpointField(fields, relation.get("to"), "关系终点", relationEntityInfos);

        // type 是业务关系名称，例如 Contains、Manages、Uses。
        String relationType = readObjectText(relation, "type");
        if (!relationType.isBlank()) {
            fields.add(new DeviceLogTimelineFieldDTO("关系类型", relationType));
        }

        // typeGroup 是关系分组，例如 COMMON、COMMON_RELATED 或 CUSTOMER。
        String relationTypeGroup = readObjectText(relation, "typeGroup");
        if (!relationTypeGroup.isBlank()) {
            fields.add(new DeviceLogTimelineFieldDTO("关系分组", relationTypeGroup));
        }

        // additionalInfo 是关系创建时附加的 JSON 信息，没有内容时不返回空字段。
        JsonNode additionalInfo = relation.get("additionalInfo");
        if (additionalInfo != null && !additionalInfo.isNull() && !additionalInfo.isEmpty()) {
            fields.add(new DeviceLogTimelineFieldDTO("关系附加信息", formatJsonValue(additionalInfo)));
        }
    }

    /**
     * 把关系的一端实体转换为“实体类型/实体 ID”格式。
     *
     * @param fields 当前时间线事件字段集合
     * @param endpoint 关系 from 或 to 节点
     * @param label 前端展示标签
     * @param relationEntityInfos 当前时间线页中关系端点的名称索引
     */
    private static void addRelationEndpointField(List<DeviceLogTimelineFieldDTO> fields, JsonNode endpoint, String label,
                                          Map<EntityId, EntityInfo> relationEntityInfos) {
        // 端点信息不完整时不返回空的关系起点或终点字段。
        String endpointText = formatEntityReference(endpoint, relationEntityInfos);
        if (!endpointText.isBlank()) {
            fields.add(new DeviceLogTimelineFieldDTO(label, endpointText));
        }
    }

    /**
     * 格式化关系端点实体。
     *
     * <p>平台 EntityId 通常序列化为 {entityType: "DEVICE", id: "..."}；同时兼容 id
     * 被再次包装成对象的情况。</p>
     *
     * @param endpoint 实体引用 JSON
     * @param relationEntityInfos 当前时间线页中关系端点的名称索引
     * @return 实体类型、实体名称和实体 ID，无法解析时返回空字符串
     */
    private static String formatEntityReference(JsonNode endpoint, Map<EntityId, EntityInfo> relationEntityInfos) {
        // 端点必须是对象，否则没有实体类型和 ID 可供展示。
        if (endpoint == null || !endpoint.isObject()) {
            return "";
        }
        // 读取实体类型和 ID，兼容 TB 序列化出来的标量字段。
        String entityType = readObjectText(endpoint, "entityType");
        String entityId = readObjectText(endpoint, "id");
        if (entityId.isBlank()) {
            // 少数序列化结果会把 ID 包装成 {"id": {"id": "uuid"}}。
            JsonNode idNode = endpoint.get("id");
            entityId = idNode != null && idNode.isObject() ? readObjectText(idNode, "id") : "";
        }
        if (entityType.isBlank() && entityId.isBlank()) {
            return "";
        }
        // 优先展示名称，让普通用户能直接读懂；括号保留 UUID 方便定位和排查。
        EntityId parsedEntityId = parseEntityId(entityType, entityId);
        EntityInfo entityInfo = parsedEntityId == null ? null : relationEntityInfos.get(parsedEntityId);
        if (entityInfo != null && entityInfo.getName() != null && !entityInfo.getName().isBlank()) {
            return entityType + "/" + entityInfo.getName()
                    + (entityId.isBlank() ? "" : "（" + entityId + "）");
        }

        // 名称查不到时用类型和 ID，保证历史事件不会因为实体删除而丢失定位信息。
        if (entityType.isBlank()) {
            return entityId;
        }
        return entityId.isBlank() ? entityType : entityType + "/" + entityId;
    }

    /**
     * 根据审计 JSON 中的实体类型和 UUID 构造平台统一的 EntityId。
     *
     * <p>EntityService 的批量接口使用 EntityId 作为 Map 键，因此不能直接把字符串 UUID
     * 当作键。解析失败时返回 null，由调用方回退为原始字符串。</p>
     */
    private static EntityId parseEntityId(String entityType, String entityId) {
        if (entityType.isBlank() || entityId.isBlank()) {
            return null;
        }
        try {
            return EntityIdFactory.getByTypeAndUuid(entityType.toUpperCase(Locale.ROOT), entityId);
        } catch (IllegalArgumentException e) {
            // 审计历史可能包含已废弃实体类型或非 UUID 标识，不能让一条脏日志影响整页查询。
            return null;
        }
    }

    /**
     * 从关系端点提取 EntityId，并忽略无法解析的历史数据。
     */
    private static void addRelationEntityId(Set<EntityId> entityIds, JsonNode endpoint) {
        if (endpoint == null || !endpoint.isObject()) {
            return;
        }
        String entityType = readObjectText(endpoint, "entityType");
        String entityId = readObjectText(endpoint, "id");
        if (entityId.isBlank()) {
            JsonNode idNode = endpoint.get("id");
            entityId = idNode != null && idNode.isObject() ? readObjectText(idNode, "id") : "";
        }
        EntityId parsedEntityId = parseEntityId(entityType, entityId);
        if (parsedEntityId != null) {
            entityIds.add(parsedEntityId);
        }
    }

    /**
     * 从 JSON 对象中读取普通文本字段。
     *
     * @param object JSON 对象
     * @param key 字段名
     * @return 文本值，字段不存在或不是标量时返回空字符串
     */
    private static String readObjectText(JsonNode object, String key) {
        // 仅读取标量值，避免把嵌套对象直接错误转换成字符串。
        if (object == null || !object.isObject()) {
            return "";
        }
        JsonNode value = object.get(key);
        return value != null && value.isValueNode() ? value.asText("").trim() : "";
    }

    /**
     * 将服务器属性审计数据展开成页面可读的键值对。
     *
     * <p>ATTRIBUTES_UPDATED 的 actionData.attributes 是 JSON 对象，方法会逐个返回属性键和值；
     * ATTRIBUTES_DELETED 的 attributes 是 JSON 数组，方法会把被删除的键合并展示。
     * 同时保留 SERVER_SCOPE、SHARED_SCOPE 等属性范围，便于用户判断变更来源。</p>
     *
     * @param fields 当前时间线事件的字段集合
     * @param actionData 审计日志中的动作数据
     * @param actionType 属性更新或删除动作类型
     */
    private static void addAttributeChangeFields(List<DeviceLogTimelineFieldDTO> fields, JsonNode actionData,
                                          ActionType actionType) {
        // 审计日志中的 scope 表示属性作用域，例如 SERVER_SCOPE、SHARED_SCOPE 或 CLIENT_SCOPE。
        String scope = findText(actionData, "scope");
        if (!scope.isBlank()) {
            // 作用域不是具体键值，但它能帮助用户判断这次变更来自哪一类属性。
            fields.add(new DeviceLogTimelineFieldDTO("属性范围", scope));
        }

        // actionData 可能为空，或者不是 JSON 对象；这两种情况都没有可解析的属性数据。
        if (actionData == null || !actionData.isObject()) {
            return;
        }

        // ATTRIBUTES_UPDATED 和 ATTRIBUTES_DELETED 都把具体属性放在 actionData.attributes 中。
        JsonNode attributes = actionData.get("attributes");
        if (attributes == null || attributes.isNull()) {
            // 没有 attributes 时保留已经加入的作用域信息，但不继续处理。
            return;
        }

        // 属性更新时，attributes 是 {"属性名": "属性值"} 形式的 JSON 对象。
        if (ActionType.ATTRIBUTES_UPDATED.equals(actionType) && attributes.isObject()) {
            // fields() 返回属性对象的键值迭代器，逐项转换为前端时间线字段。
            Iterator<Map.Entry<String, JsonNode>> iterator = attributes.fields();
            while (iterator.hasNext()) {
                // 读取当前属性的名称和 JSON 值。
                Map.Entry<String, JsonNode> attribute = iterator.next();
                // 分开展示属性键名和本次写入值，并保留属性值的原始 JSON 类型。
                fields.add(new DeviceLogTimelineFieldDTO(
                        "属性键名", attribute.getKey()));
                fields.add(new DeviceLogTimelineFieldDTO(
                        "修改后值", formatJsonValue(attribute.getValue())));
            }
        // 属性删除时，attributes 是 ["属性名1", "属性名2"] 形式的 JSON 数组。
        } else if (ActionType.ATTRIBUTES_DELETED.equals(actionType) && attributes.isArray()) {
            // 先收集有效的属性名，避免把 null、空字符串或复杂 JSON 节点展示给用户。
            List<String> deletedKeys = new ArrayList<>();
            for (JsonNode key : attributes) {
                // 删除属性数组正常情况下只包含字符串节点，因此只接受值节点。
                if (key.isValueNode() && !key.asText().isBlank()) {
                    // 保存属性名，稍后用顿号合并成一条紧凑的展示字段。
                    deletedKeys.add(key.asText());
                }
            }
            if (!deletedKeys.isEmpty()) {
                // 删除动作没有新值，只展示本次被删除的属性名列表。
                fields.add(new DeviceLogTimelineFieldDTO("删除属性", String.join("、", deletedKeys)));
            }
        }
    }

    /**
     * 把审计 JSON 值转换为页面展示字符串。
     *
     * <p>字符串值直接返回文本，数字、布尔值和对象数组保留 JSON 表达，null 统一显示为 null，
     * 从而避免后端把属性类型错误转换成布尔或数字。</p>
     *
     * @param value Jackson JSON 节点
     * @return 可展示的属性值
     */
    private static String formatJsonValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        return value.isTextual() ? value.asText() : value.toString();
    }

    /**
     * 从不同版本的审计数据结构中提取告警 ID。
     *
     * <p>删除告警通常使用 actionData.alarmId；确认、清除、派发等动作通常把完整告警放在
     * actionData.entity.id 中。这里兼容字符串 ID、{id: "..."} 和 {uuid: "..."} 三种结构。</p>
     *
     * @param actionData 审计动作数据
     * @return 告警 ID，无法解析时返回空字符串
     */
    private static String findAlarmId(JsonNode actionData) {
        String alarmId = findText(actionData, "alarmId");
        if (!alarmId.isBlank()) {
            return alarmId;
        }
        if (actionData == null || !actionData.isObject()) {
            return "";
        }
        JsonNode entity = actionData.get("entity");
        JsonNode id = entity == null ? null : entity.get("id");
        if (id == null || id.isNull()) {
            return "";
        }
        if (id.isTextual()) {
            return id.asText().trim();
        }
        if (id.isObject()) {
            String nestedId = findText(id, "id");
            return nestedId.isBlank() ? findText(id, "uuid") : nestedId;
        }
        return "";
    }

    /**
     * 将告警记录转换成时间线事件。
     *
     * <p>告警发生时间优先使用 startTs，缺失时回退到 createdTime；只展示发生时的告警类型、
     * 严重程度、告警 ID 和非空详情，不把当前 acknowledged/cleared 状态冒充成历史状态。</p>
     *
     * @param alarm 平台告警信息
     * @return 告警发生时间线事件
     */
    static DeviceLogTimelineItemDTO toAlarmTimelineItem(AlarmInfo alarm) {
        // 先组装告警 ID、严重程度及非空详情字段，再确定事件发生时间。
        List<DeviceLogTimelineFieldDTO> fields = new ArrayList<>();
        fields.add(new DeviceLogTimelineFieldDTO("告警 ID", alarm.getId() == null ? "未知" : alarm.getId().toString()));
        fields.add(new DeviceLogTimelineFieldDTO("严重程度", formatSeverity(alarm.getSeverity())));
        String details = formatDetails(alarm.getDetails());
        if (!details.isBlank()) {
            fields.add(new DeviceLogTimelineFieldDTO("告警详情", details));
        }
        long timestamp = alarm.getStartTs() > 0 ? alarm.getStartTs() : alarm.getCreatedTime();
        return new DeviceLogTimelineItemDTO(timestamp, "告警事件", "发生告警：" + alarm.getType(), "", fields);
    }

    /**
     * 从审计快照中读取客户分配名称。
     *
     * <p>名称必须使用事件发生时 actionData 中保存的快照，而不是重新查询客户表，
     * 这样客户后续改名不会改变历史时间线的含义。</p>
     *
     * @param fields 当前事件字段集合
     * @param actionData 审计动作数据
     * @param key 客户名称字段名
     * @param label 前端展示标签
     */
    private static void addActionDataField(List<DeviceLogTimelineFieldDTO> fields, JsonNode actionData,
                                    String key, String label) {
        String value = findText(actionData, key);
        if (!value.isBlank()) {
            fields.add(new DeviceLogTimelineFieldDTO(label, value));
        }
    }

    /**
     * 从 JSON 对象中读取文本字段，并兼容 alarm、alarmInfo、entity 等嵌套对象。
     *
     * @param node 要搜索的 JSON 节点
     * @param key 目标字段名
     * @return 字段文本，字段不存在或不是标量值时返回空字符串
     */
    private static String findText(JsonNode node, String key) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (!node.isObject()) {
            return "";
        }
        JsonNode value = node.get(key);
        if (value != null && !value.isNull() && !value.isContainerNode()) {
            return value.asText("").trim();
        }
        for (String nestedKey : List.of("alarm", "alarmInfo", "entity")) {
            String nestedValue = findText(node.get(nestedKey), key);
            if (!nestedValue.isBlank()) {
                return nestedValue;
            }
        }
        return "";
    }

    /**
     * 解析审计操作人。
     *
     * <p>优先使用平台保存的用户名；用户名缺失时返回用户 ID；系统触发且没有用户 ID 时显示“系统”。</p>
     *
     * @param auditLog 审计日志
     * @return 操作人名称
     */
    private static String resolveOperator(AuditLog auditLog) {
        if (auditLog.getUserName() != null && !auditLog.getUserName().isBlank()) {
            return auditLog.getUserName();
        }
        return auditLog.getUserId() == null ? "系统" : auditLog.getUserId().toString();
    }

    /**
     * 将平台告警严重程度枚举转换成页面使用的中文名称。
     *
     * @param severity 平台严重程度
     * @return 严重、重要、次要、警告或不确定
     */
    private static String formatSeverity(AlarmSeverity severity) {
        if (severity == null) {
            return "不确定";
        }
        return switch (severity) {
            case CRITICAL -> "严重";
            case MAJOR -> "重要";
            case MINOR -> "次要";
            case WARNING -> "警告";
            case INDETERMINATE -> "不确定";
        };
    }

    /**
     * 格式化告警详情。
     *
     * <p>空对象、null 和空节点不返回详情字段；普通文本直接返回，复杂 JSON 保留完整 JSON 字符串，
     * 让前端可以在悬浮或抽屉中展示原始业务内容。</p>
     *
     * @param details 告警详情 JSON
     * @return 格式化后的详情文本，空详情返回空字符串
     */
    private static String formatDetails(JsonNode details) {
        if (details == null || details.isNull() || details.isEmpty()) {
            return "";
        }
        return details.isTextual() ? details.asText() : details.toString();
    }

    /**
     * 把平台审计动作转换为中文标题，并保留原始动作类型。
     *
     * <p>中文标题方便普通用户理解，括号中的枚举值方便开发排查、数据库对照和后续扩展。
     * 未配置映射的动作统一回退为“设备操作”。</p>
     *
     * @param actionType 平台审计动作类型
     * @return 中文动作标题和原始动作枚举
     */
    private static String formatActionTitle(ActionType actionType) {
        // 属性作用范围会在详情字段中单独展示，因此标题直接使用简洁名称。
        if (ActionType.ATTRIBUTES_UPDATED.equals(actionType)) {
            return "更新属性";
        }
        String raw = actionType == null ? "UNKNOWN" : actionType.name();
        String title = switch (raw) {
            case "ASSIGNED_TO_CUSTOMER" -> "分配到客户";
            case "UNASSIGNED_FROM_CUSTOMER" -> "取消客户分配";
            case "ALARM_ACK" -> "确认告警";
            case "ALARM_CLEAR" -> "清除告警";
            case "ALARM_ASSIGNED" -> "派发告警";
            case "ALARM_UNASSIGNED" -> "取消派发告警";
            case "ALARM_DELETE" -> "删除告警";
            case "ATTRIBUTES_DELETED" -> "删除服务器属性";
            case "RELATION_ADD_OR_UPDATE" -> "新增或更新关系";
            case "RELATION_DELETED" -> "删除关系";
            case "ADDED" -> "创建实体";
            case "UPDATED" -> "更新实体";
            case "DELETED" -> "删除实体";
            default -> "设备操作";
        };
        return title + "（" + raw + "）";
    }

}
