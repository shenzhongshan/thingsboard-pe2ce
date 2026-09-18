/**
 * 文件职责：设备日志列表的业务实现。
 *
 * <p>这里复用 UserEntitiesService 做权限过滤，不直接按租户全量查询，确保租户管理员、客户用户
 * 以及共享实体的可见范围与平台一致。分页逐页读取，避免一次把大量设备合并到内存。</p>
 */
package org.thingsboard.server.service.deviceLog.service.impl;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.deviceLog.DeviceLogService;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogDeviceDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelineFieldDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelineItemDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelinePageDTO;
import org.thingsboard.server.service.entitiy.UserEntitiesService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceLogServiceImpl implements DeviceLogService {

    private static final int DEFAULT_TIMELINE_PAGE_SIZE = 50;
    private static final int MAX_TIMELINE_PAGE_SIZE = 100;
    private static final int DEFAULT_RECENT_DAYS = 30;

    private final UserEntitiesService userEntitiesService;
    private final DeviceService deviceService;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;
    private final AlarmService alarmService;
    private final EntityService entityService;

    /**
     * 查询当前用户能够读取的设备，并转换为日志列表需要的最小字段集合。
     *
     * <p>设备范围由平台的 {@link UserEntitiesService} 统一计算，包含租户权限、客户分配和共享设备权限。
     * 本方法不自行拼接租户或客户条件，避免自定义页面与平台权限规则不一致。</p>
     *
     * @param user 当前登录用户
     * @param pageLink 设备列表分页参数和搜索条件
     * @return 当前用户可见的设备分页结果
     * @throws ThingsboardException 权限或设备查询失败时抛出
     */
    @Override
    public PageData<DeviceLogDeviceDTO> findVisibleDevices(SecurityUser user, PageLink pageLink)
            throws ThingsboardException {
        PageData<? extends HasName> pageData = userEntitiesService.findUserEntities(
                user, EntityType.DEVICE, Operation.READ, pageLink);
        List<DeviceLogDeviceDTO> result = new ArrayList<>();
        for (HasName entity : pageData.getData()) {
            if (!(entity instanceof DeviceInfo deviceInfo) || deviceInfo.getId() == null) {
                continue;
            }
            result.add(new DeviceLogDeviceDTO(
                    deviceInfo.getId().getId(),
                    deviceInfo.getName(),
                    resolveDeviceProfileName(deviceInfo)
            ));
        }
        return new PageData<>(result, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    /**
     * 解析设备列表中展示的配置名称。
     *
     * <p>优先使用设备配置的显示名称；旧数据或配置缺失时回退到设备类型，保证列表不会出现空白列。</p>
     *
     * @param deviceInfo 平台设备信息
     * @return 配置名称或设备类型
     */
    private String resolveDeviceProfileName(DeviceInfo deviceInfo) {
        String profileName = deviceInfo.getDeviceProfileName();
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        return deviceInfo.getType();
    }

    /**
     * 查询设备最近时间线。
     *
     * <p>最近时间线默认覆盖最近 30 天，调用方可以通过 days 调整到 1 至 90 天。
     * 查询结果只包含审计事件和告警发生事件，不把高频遥测逐条写入时间线。</p>
     *
     * @param user 当前登录用户
     * @param deviceId 目标设备 ID
     * @param days 查询最近多少天，非正数使用默认 30 天
     * @param pageSize 单页数量，内部会限制最大值
     * @return 按时间倒序排列的最近时间线分页结果
     * @throws ThingsboardException 设备不存在、无读取权限或查询参数非法时抛出
     */
    @Override
    public DeviceLogTimelinePageDTO findRecentTimeline(SecurityUser user, UUID deviceId, int days, int pageSize)
            throws ThingsboardException {
        int safeDays = days <= 0 ? DEFAULT_RECENT_DAYS : Math.min(days, 90);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - safeDays * 24L * 60 * 60 * 1000;
        return findTimeline(user, deviceId, startTime, endTime, pageSize);
    }

    /**
     * 查询最近时间线之外的历史事件。
     *
     * <p>历史查询通过上一页返回的时间游标向更早时间推进。首次调用可以不传 beforeTs，
     * 后续调用必须使用 nextCursor，避免一次性读取全部审计和告警记录。</p>
     *
     * @param user 当前登录用户
     * @param deviceId 目标设备 ID
     * @param pageSize 单页数量，内部会限制最大值
     * @param beforeTs 游标时间戳（毫秒），只查询早于该时间的事件
     * @return 按时间倒序排列的历史时间线分页结果
     * @throws ThingsboardException 设备不存在、无读取权限或游标格式错误时抛出
     */
    @Override
    public DeviceLogTimelinePageDTO findHistoryTimeline(SecurityUser user, UUID deviceId, int pageSize,
                                                        String beforeTs) throws ThingsboardException {
        return findTimeline(user, deviceId, null, resolveEndTime(beforeTs), pageSize);
    }

    /**
     * 统一读取审计和告警两类事件，并合并成一条时间线。
     *
     * <p>方法先验证设备存在和当前用户的 DEVICE_READ 权限，再分别向平台审计服务和告警服务读取数据。
     * 两个来源各读取不超过两页的候选数据，在内存中按实际事件时间倒序合并，最后截取页面大小。
     * 这种方式只保留当前页附近的数据，避免把设备的全部历史记录加载到内存。</p>
     *
     * @param user 当前登录用户
     * @param deviceId 目标设备 ID
     * @param startTime 起始时间（毫秒），为 null 时不限制起始时间
     * @param endTime 结束时间（毫秒）
     * @param pageSize 返回页面大小
     * @return 合并后的时间线分页结果和下一页游标
     * @throws ThingsboardException 设备不存在或当前用户没有设备读取权限时抛出
     */
    private DeviceLogTimelinePageDTO findTimeline(SecurityUser user, UUID deviceId, Long startTime,
                                                  long endTime, int pageSize) throws ThingsboardException {
        DeviceInfo deviceInfo = deviceService.findDeviceInfoById(user.getTenantId(), new DeviceId(deviceId));
        if (deviceInfo == null) {
            throw new ThingsboardException("Device not found: " + deviceId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        accessControlService.checkPermission(user, Resource.of(EntityType.DEVICE), Operation.READ,
                deviceInfo.getId(), deviceInfo);

        int safePageSize = normalizePageSize(pageSize);
        // 两个来源各多取一页，避免审计和告警交错时第一页数量不足。
        int fetchSize = Math.min(safePageSize * 2, MAX_TIMELINE_PAGE_SIZE);
        TimePageLink pageLink = new TimePageLink(fetchSize, 0, null,
                SortOrder.BY_CREATED_TIME_DESC, startTime, endTime);

        DeviceId entityId = new DeviceId(deviceId);
        PageData<AuditLog> auditPage = auditLogService.findAuditLogsByTenantIdAndEntityId(
                user.getTenantId(), entityId, null, pageLink);
        // 参数顺序：实体、分页、查询范围、告警状态、受理人、是否读取来源实体。
        AlarmQuery alarmQuery = new AlarmQuery(entityId, pageLink, AlarmSearchStatus.ANY, null, null, false);
        PageData<AlarmInfo> alarmPage;
        if (user.getCustomerId() != null && !user.getCustomerId().isNullUid()) {
            alarmPage = alarmService.findCustomerAlarms(user.getTenantId(), user.getCustomerId(), alarmQuery);
        } else {
            alarmPage = alarmService.findAlarms(user.getTenantId(), alarmQuery);
        }

        // 关系事件的 from/to 只保存实体类型和 UUID。先收集当前页的全部端点，
        // 再通过 EntityService 一次性批量查名称，避免每条关系分别访问数据库。
        Map<EntityId, EntityInfo> relationEntityInfos = resolveRelationEntityInfos(user, auditPage.getData());
        List<DeviceLogTimelineItemDTO> items = new ArrayList<>();
        for (AuditLog auditLog : auditPage.getData()) {
            items.add(toAuditTimelineItem(auditLog, relationEntityInfos));
        }
        for (AlarmInfo alarm : alarmPage.getData()) {
            DeviceLogTimelineItemDTO item = toAlarmTimelineItem(alarm);
            if (item.getTimestamp() >= (startTime == null ? Long.MIN_VALUE : startTime)
                    && item.getTimestamp() <= endTime) {
                items.add(item);
            }
        }
        List<DeviceLogTimelineItemDTO> validItems = new ArrayList<>();
        for (DeviceLogTimelineItemDTO item : items) {
            if (item.getTimestamp() > 0
                    && (startTime == null || item.getTimestamp() >= startTime)
                    && item.getTimestamp() <= endTime) {
                validItems.add(item);
            }
        }
        items = validItems;
        items.sort(Comparator.comparingLong(DeviceLogTimelineItemDTO::getTimestamp).reversed());

        boolean pageOverflow = items.size() > safePageSize;
        if (pageOverflow) {
            items = new ArrayList<>(items.subList(0, safePageSize));
        }
        boolean hasNext = pageOverflow || auditPage.hasNext() || alarmPage.hasNext();
        // 正常情况下游标取本页最旧事件；如果来源仍有下一页但本页被过滤为空，
        // 使用当前窗口终点继续向前推进，避免前端拿不到游标而无法点击“查看更多”。
        String nextCursor = hasNext
                ? Long.toString(items.isEmpty() ? endTime : items.get(items.size() - 1).getTimestamp())
                : null;
        return new DeviceLogTimelinePageDTO(items, hasNext, nextCursor);
    }

    /**
     * 规范化时间线分页大小。
     *
     * <p>未传或传入非正数时使用 50；即使前端传入更大的值，也最多读取 100 条，
     * 防止异常请求导致审计和告警数据在服务端被大量加载。</p>
     *
     * @param pageSize 请求的页面大小
     * @return 可安全使用的页面大小
     */
    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_TIMELINE_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_TIMELINE_PAGE_SIZE);
    }

    /**
     * 解析历史时间线的结束游标。
     *
     * <p>时间线按照时间倒序展示，因此下一页应查询早于当前页最旧事件的记录。
     * 减去 1 毫秒可以避免同一时间点的记录在相邻页面重复出现；空游标表示从当前时间开始。</p>
     *
     * @param beforeTs 前端传入的毫秒时间戳字符串
     * @return 查询使用的结束时间
     * @throws ThingsboardException 游标不是合法数字时抛出参数错误
     */
    private long resolveEndTime(String beforeTs) throws ThingsboardException {
        if (beforeTs == null || beforeTs.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return Long.parseLong(beforeTs) - 1;
        } catch (NumberFormatException e) {
            throw new ThingsboardException("Invalid timeline cursor", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
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
    private DeviceLogTimelineItemDTO toAuditTimelineItem(AuditLog auditLog,
                                                         Map<EntityId, EntityInfo> relationEntityInfos) {
        ActionType actionType = auditLog.getActionType();
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
    private void addRelationChangeFields(List<DeviceLogTimelineFieldDTO> fields, JsonNode actionData,
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
    private void addRelationEndpointField(List<DeviceLogTimelineFieldDTO> fields, JsonNode endpoint, String label,
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
    private String formatEntityReference(JsonNode endpoint, Map<EntityId, EntityInfo> relationEntityInfos) {
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
    private EntityId parseEntityId(String entityType, String entityId) {
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
     * 批量解析当前审计页中关系两端的实体名称。
     *
     * <p>关系日志通常同时包含 from 和 to。先去重收集 EntityId，再调用一次
     * EntityService.fetchEntityInfos，避免按照关系条数产生 N+1 次查询；返回的 Map
     * 只覆盖当前时间线页实际出现的实体。</p>
     */
    private Map<EntityId, EntityInfo> resolveRelationEntityInfos(SecurityUser user,
                                                                  List<AuditLog> auditLogs) {
        Set<EntityId> entityIds = new HashSet<>();
        for (AuditLog auditLog : auditLogs) {
            ActionType actionType = auditLog.getActionType();
            if (!ActionType.RELATION_ADD_OR_UPDATE.equals(actionType)
                    && !ActionType.RELATION_DELETED.equals(actionType)) {
                continue;
            }
            JsonNode actionData = auditLog.getActionData();
            JsonNode relation = actionData == null ? null : actionData.get("relation");
            if (relation == null || !relation.isObject()) {
                continue;
            }
            addRelationEntityId(entityIds, relation.get("from"));
            addRelationEntityId(entityIds, relation.get("to"));
        }
        if (entityIds.isEmpty()) {
            return Map.of();
        }
        Map<EntityId, EntityInfo> entityInfos = entityService.fetchEntityInfos(
                user.getTenantId(), resolveCustomerId(user), entityIds);
        return entityInfos == null ? Map.of() : entityInfos;
    }

    /**
     * 从关系端点提取 EntityId，并忽略无法解析的历史数据。
     */
    private void addRelationEntityId(Set<EntityId> entityIds, JsonNode endpoint) {
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
     * 获取当前用户对应的客户范围；租户管理员没有客户 ID，使用 null 表示租户范围。
     */
    private CustomerId resolveCustomerId(SecurityUser user) {
        return user.getCustomerId() == null || user.getCustomerId().isNullUid()
                ? null : user.getCustomerId();
    }

    /**
     * 从 JSON 对象中读取普通文本字段。
     *
     * @param object JSON 对象
     * @param key 字段名
     * @return 文本值，字段不存在或不是标量时返回空字符串
     */
    private String readObjectText(JsonNode object, String key) {
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
    private void addAttributeChangeFields(List<DeviceLogTimelineFieldDTO> fields, JsonNode actionData,
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
                // 属性名作为字段标签，属性值统一交给 formatJsonValue 保留原始类型表现。
                fields.add(new DeviceLogTimelineFieldDTO(
                        attribute.getKey(), formatJsonValue(attribute.getValue())));
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
    private String formatJsonValue(JsonNode value) {
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
    private String findAlarmId(JsonNode actionData) {
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
    private DeviceLogTimelineItemDTO toAlarmTimelineItem(AlarmInfo alarm) {
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
    private void addActionDataField(List<DeviceLogTimelineFieldDTO> fields, JsonNode actionData,
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
    private String findText(JsonNode node, String key) {
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
    private String resolveOperator(AuditLog auditLog) {
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
    private String formatSeverity(AlarmSeverity severity) {
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
    private String formatDetails(JsonNode details) {
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
    private String formatActionTitle(ActionType actionType) {
        String raw = actionType == null ? "UNKNOWN" : actionType.name();
        String title = switch (raw) {
            case "ASSIGNED_TO_CUSTOMER" -> "分配到客户";
            case "UNASSIGNED_FROM_CUSTOMER" -> "取消客户分配";
            case "ALARM_ACK" -> "确认告警";
            case "ALARM_CLEAR" -> "清除告警";
            case "ALARM_ASSIGNED" -> "派发告警";
            case "ALARM_UNASSIGNED" -> "取消派发告警";
            case "ALARM_DELETE" -> "删除告警";
            case "ATTRIBUTES_UPDATED" -> "更新服务器属性";
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
