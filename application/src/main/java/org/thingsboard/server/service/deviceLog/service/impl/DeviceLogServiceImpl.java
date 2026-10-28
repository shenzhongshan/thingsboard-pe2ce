/**
 * 文件职责：设备日志列表的业务实现。
 *
 * <p>这里复用 UserEntitiesService 做权限过滤，不直接按租户全量查询，确保租户管理员、客户用户
 * 以及共享实体的可见范围与平台一致。分页逐页读取，避免一次把大量设备合并到内存。</p>
 */
package org.thingsboard.server.service.deviceLog.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.deviceLog.DeviceLogService;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogDeviceDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelineItemDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelinePageDTO;
import org.thingsboard.server.service.entitiy.UserEntitiesService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 组合平台审计、告警和设备状态数据，提供带权限校验的设备日志时间线。 */
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
    private final TimeseriesService timeseriesService;

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
     * 查询结果包含审计事件、告警发生事件和设备 active 状态变更；普通高频遥测不逐条写入时间线。</p>
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
     * 统一读取审计、告警和设备状态三类事件，并合并成一条时间线。
     *
     * <p>方法先验证设备存在和当前用户的 DEVICE_READ 权限，再分别向平台审计服务、告警服务和
     * TimeseriesService 读取数据。三个来源各读取有限数量的候选数据，在内存中按实际事件时间倒序合并，
     * 最后截取页面大小；不会把设备的全部历史记录加载到内存。</p>
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
        // 先确认设备存在，再执行 DEVICE_READ 权限检查，未授权时不读取时间线数据。
        DeviceInfo deviceInfo = deviceService.findDeviceInfoById(user.getTenantId(), new DeviceId(deviceId));
        if (deviceInfo == null) {
            throw new ThingsboardException("Device not found: " + deviceId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        accessControlService.checkPermission(user, Resource.of(EntityType.DEVICE), Operation.READ,
                deviceInfo.getId(), deviceInfo);

        int safePageSize = normalizePageSize(pageSize);
        // 各来源读取有限候选，再在服务层合并排序，避免一次加载完整历史。
        int fetchSize = Math.min(safePageSize * 2, MAX_TIMELINE_PAGE_SIZE);
        TimePageLink pageLink = new TimePageLink(fetchSize, 0, null,
                SortOrder.BY_CREATED_TIME_DESC, startTime, endTime);

        // 分别查询平台审计、设备告警和 active 历史，候选数量受 fetchSize 限制。
        DeviceId entityId = new DeviceId(deviceId);
        PageData<AuditLog> auditPage = auditLogService.findAuditLogsByTenantIdAndEntityId(
                user.getTenantId(), entityId, null, pageLink);
        TimelineAlarmCandidates alarmCandidates = findTimelineAlarmCandidates(user, entityId, pageLink, fetchSize);

        // 集成设备的上下线状态由平台状态服务写入 active 时序；读取它才能补齐设备状态时间线。
        List<TsKvEntry> activityEntries = readActivityEntries(
                user, entityId, startTime == null ? 0L : startTime, endTime, fetchSize);
        List<DeviceLogTimelineItemDTO> activityItems = toActivityTimelineItems(activityEntries);

        // 关系事件的 from/to 只保存实体类型和 UUID。先收集当前页的全部端点，
        // 再通过 EntityService 一次性批量查名称，避免每条关系分别访问数据库。
        // 批量补齐关系端点名称后，再把三个来源统一转换为时间线事件。
        Map<EntityId, EntityInfo> relationEntityInfos = resolveRelationEntityInfos(user, auditPage.getData());
        List<DeviceLogTimelineItemDTO> items = new ArrayList<>();
        for (AuditLog auditLog : auditPage.getData()) {
            items.add(DeviceLogAuditTimelineConverter.toAuditTimelineItem(auditLog, relationEntityInfos));
        }
        for (AlarmInfo alarm : alarmCandidates.alarms()) {
            items.add(DeviceLogAuditTimelineConverter.toAlarmTimelineItem(alarm));
        }
        items.addAll(activityItems);
        // 按实际事件时间再次过滤，避免来源分页时间字段与事件时间不一致。
        List<DeviceLogTimelineItemDTO> validItems = new ArrayList<>();
        for (DeviceLogTimelineItemDTO item : items) {
            if (item.getTimestamp() > 0
                    && (startTime == null || item.getTimestamp() >= startTime)
                    && item.getTimestamp() <= endTime) {
                validItems.add(item);
            }
        }
        items = validItems;
        // 合并后按事件时间倒序，只保留本页条数并计算下一页游标。
        items.sort(Comparator.comparingLong(DeviceLogTimelineItemDTO::getTimestamp).reversed());

        boolean pageOverflow = items.size() > safePageSize;
        if (pageOverflow) {
            items = new ArrayList<>(items.subList(0, safePageSize));
        }
        boolean hasNext = pageOverflow || auditPage.hasNext() || alarmCandidates.hasNext()
                || activityEntries.size() >= fetchSize;
        // 正常情况下游标取本页最旧事件；如果来源仍有下一页但本页被过滤为空，
        // 使用当前窗口终点继续向前推进，避免前端拿不到游标而无法点击“查看更多”。
        String nextCursor = hasNext
                ? Long.toString(items.isEmpty() ? endTime : items.get(items.size() - 1).getTimestamp())
                : null;
        return new DeviceLogTimelinePageDTO(items, hasNext, nextCursor);
    }

    /**
     * 按目标设备查询告警，并在客户用户场景下继续扫描源页以补足候选数量。
     */
    /** 按设备查告警，并继续扫描客户告警源页直到凑满候选数或读完数据。 */
    private TimelineAlarmCandidates findTimelineAlarmCandidates(
            SecurityUser user, DeviceId deviceId, TimePageLink initialPageLink, int fetchSize) {
        // 客户用户的告警查询还需校验客户 ID；租户管理员保留租户范围。
        CustomerId customerId = resolveCustomerId(user);
        List<AlarmInfo> candidates = new ArrayList<>(fetchSize);
        TimePageLink pageLink = initialPageLink;
        boolean hasExtraCandidate = false;
        while (pageLink != null) {
            AlarmQueryV2 query = new AlarmQueryV2(
                    deviceId, pageLink, null, List.of(AlarmSearchStatus.ANY), null, null);
            PageData<AlarmInfo> page = alarmService.findAlarmsV2(user.getTenantId(), query);
            for (AlarmInfo alarm : page.getData()) {
                if (customerId != null && !customerId.equals(alarm.getCustomerId())) {
                    continue;
                }
                if (candidates.size() < fetchSize) {
                    candidates.add(alarm);
                } else {
                    hasExtraCandidate = true;
                }
            }
            // 候选已满时结束扫描，并结合源分页状态说明是否仍有下一页。
            if (hasExtraCandidate || candidates.size() >= fetchSize) {
                return new TimelineAlarmCandidates(candidates, hasExtraCandidate || page.hasNext());
            }
            if (!page.hasNext()) {
                break;
            }
            pageLink = pageLink.nextPageLink();
        }
        return new TimelineAlarmCandidates(candidates, false);
    }

    /** 保存本页告警候选和告警源是否还有后续数据。 */
    private record TimelineAlarmCandidates(List<AlarmInfo> alarms, boolean hasNext) {
    }

    /**
     * 查询设备 active 历史值。
     *
     * <p>平台状态服务在启用 {@code persistToTelemetry} 时会把 true/false 写入 ts_kv；
     * 这里通过平台统一的时序服务读取，兼容 SQL、TimescaleDB 和 Cassandra 存储实现。</p>
     *
     * @param user 当前用户，用于传入租户范围
     * @param entityId 目标设备
     * @param startTime 查询起始时间
     * @param endTime 查询结束时间
     * @param limit 最多读取条数
     * @return active 时序记录；没有记录时返回空列表
     * @throws ThingsboardException 时序查询失败时抛出
     */
    private List<TsKvEntry> readActivityEntries(SecurityUser user, DeviceId entityId,
                                                long startTime, long endTime, int limit)
            throws ThingsboardException {
        try {
            return timeseriesService.findAll(
                    user.getTenantId(), entityId,
                    List.of(new BaseReadTsKvQuery("active", startTime, endTime, limit, "DESC"))
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThingsboardException("Interrupted while reading device activity history",
                    e, ThingsboardErrorCode.DATABASE);
        } catch (ExecutionException e) {
            throw new ThingsboardException("Failed to read device activity history",
                    e, ThingsboardErrorCode.DATABASE);
        }
    }

    /** 将 active 历史值转换委托给纯转换器，保留既有包级静态入口。 */
    static List<DeviceLogTimelineItemDTO> toActivityTimelineItems(List<TsKvEntry> entries) {
        // 兼容已有包级调用入口，实际状态去重和 DTO 转换委托给纯转换器。
        return DeviceLogAuditTimelineConverter.toActivityTimelineItems(entries);
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
     * 批量解析当前审计页中关系两端的实体名称，并由服务层执行权限范围查询。
     */
    private Map<EntityId, EntityInfo> resolveRelationEntityInfos(SecurityUser user, List<AuditLog> auditLogs) {
        Set<EntityId> entityIds = DeviceLogAuditTimelineConverter.collectRelationEntityIds(auditLogs);
        if (entityIds.isEmpty()) {
            return Map.of();
        }
        Map<EntityId, EntityInfo> entityInfos = entityService.fetchEntityInfos(
                user.getTenantId(), resolveCustomerId(user), entityIds);
        return entityInfos == null ? Map.of() : entityInfos;
    }

    /**
     * 获取当前用户对应的客户范围；租户管理员没有客户 ID，使用 null 表示租户范围。
     */
    private CustomerId resolveCustomerId(SecurityUser user) {
        return user.getCustomerId() == null || user.getCustomerId().isNullUid()
                ? null : user.getCustomerId();
    }

}
