/** 文件职责：复用平台告警查询和聚合服务实现电气安全告警范围过滤。 */
package org.thingsboard.server.service.electrical_safety.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.bo.AlarmDeviceCountBO;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyDashboardAlarmStatisticsDTO;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyFunctionAlarmCountDTO;
import org.thingsboard.server.service.electrical_safety.service.ElectricalSafetyAlarmService;
import org.thingsboard.server.service.query.VisibleDeviceContextService;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 电气安全告警服务：先用共享设备服务确定用户权限和产品分类，再复用原生告警查询及数据库聚合。
 * 所有统计都限定在 productCategory=electrical_safety 的设备 ID 集合中，不修改平台全局告警接口。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ElectricalSafetyAlarmServiceImpl implements ElectricalSafetyAlarmService {

    /** 当前模块允许查询的服务器属性产品分类。 */
    private static final String ELECTRICAL_SAFETY_PRODUCT_CATEGORY = "electrical_safety";
    /** 看板周期边界沿用告警分析服务使用的 JVM 系统时区。 */
    private static final ZoneId REPORTING_ZONE_ID = ZoneId.systemDefault();

    /** 查询当前用户可见设备，并按服务器属性 productCategory 过滤。 */
    private final VisibleDeviceContextService visibleDeviceContextService;
    /** 复用平台告警分页查询，避免改变 ThingsBoard 原生告警路由。 */
    private final DeviceAlarmQueryService deviceAlarmQueryService;
    /** 复用数据库侧告警聚合，避免加载全量告警明细计算趋势。 */
    private final AlarmStatisticsService alarmStatisticsService;

    /**
     * 只查询调用方声明的电气安全分类内、当前用户有权访问设备的告警。
     *
     * @param user 当前登录用户
     * @param query ThingsBoard 原生 v2 告警过滤条件和分页参数
     * @param productCategory 调用方传入的产品大类
     * @return 当前权限范围内的电气安全设备告警分页
     * @throws ThingsboardException 可见设备或平台告警读取失败时抛出
     */
    @Override
    public PageData<AlarmInfo> findAlarms(SecurityUser user, AlarmQueryV2 query, String productCategory)
            throws ThingsboardException {
        // 产品分类不匹配时直接返回空页，不允许此模块成为跨分类告警查询入口。
        if (!ELECTRICAL_SAFETY_PRODUCT_CATEGORY.equals(productCategory)) {
            return PageData.emptyPageData();
        }
        // 当前用户设备 READ 权限和 electrical_safety 服务器属性过滤由共享查询服务统一完成。
        List<VisibleDeviceContextBO> contexts = findVisibleElectricalSafetyDevices(user);
        // 原生告警查询只接收已授权设备 ID，不能先查全租户告警后在内存过滤。
        List<UUID> deviceIds = collectDeviceIds(contexts);
        return deviceAlarmQueryService.findPageForDevices(user, query, deviceIds);
    }

    /**
     * 按自然日、自然周或自然月统计电气安全告警事件，并查询当前活动告警设备。
     * 周从周一零点开始；告警事件以平台 start_ts 触发时间统计，周期采用系统时区。
     *
     * @param user 当前登录用户
     * @param productCategory 调用方传入的产品大类
     * @param period 统计周期：day、week 或 month
     * @return 活动告警设备及按功能类型合并的周期告警事件数量
     * @throws ThingsboardException 分类或周期参数无效、可见设备或告警统计读取失败时抛出
     */
    @Override
    public ElectricalSafetyDashboardAlarmStatisticsDTO getDashboardStatistics(
            SecurityUser user, String productCategory, String period) throws ThingsboardException {
        // 产品分类固定在模块边界校验，避免篡改请求参数跨查其他设备类别。
        if (!ELECTRICAL_SAFETY_PRODUCT_CATEGORY.equals(productCategory)) {
            throw new ThingsboardException("Unsupported product category", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        // 从当前时区计算所选自然周期起点，结束边界取请求执行时刻。
        ZonedDateTime now = ZonedDateTime.now(REPORTING_ZONE_ID);
        long startTs = calculatePeriodStart(period, now.toLocalDate(), now.getZone());
        long endTs = now.toInstant().toEpochMilli();

        // 设备集合由当前用户权限和模块产品分类共同限定，后续所有统计只使用此集合。
        List<VisibleDeviceContextBO> contexts = findVisibleElectricalSafetyDevices(user);
        Set<UUID> deviceIds = new LinkedHashSet<>();
        Map<UUID, String> functionCategoryByDeviceId = new LinkedHashMap<>();
        for (VisibleDeviceContextBO context : contexts) {
            if (context == null || context.getDeviceId() == null) {
                continue;
            }
            deviceIds.add(context.getDeviceId());
            // 告警聚合结果按设备 ID 反查规范 functionCategory，不凭设备名称推断类别。
            String functionCategory = context.getFunctionCategory();
            functionCategoryByDeviceId.put(context.getDeviceId(),
                    functionCategory == null || functionCategory.isBlank() ? "other" : functionCategory.trim());
        }

        // 空设备集合直接返回空统计，避免把空集合传入 SQL IN 条件或退化为全租户查询。
        if (deviceIds.isEmpty()) {
            return new ElectricalSafetyDashboardAlarmStatisticsDTO(
                    Collections.emptyList(), Collections.emptyList());
        }

        // 活动设备数以 cleared=false 去重设备 ID 统计；事件趋势以 start_ts 落入所选周期统计。
        Set<UUID> activeAlarmDeviceIds = alarmStatisticsService.findActiveAlarmDeviceIds(user, deviceIds);
        List<AlarmDeviceCountBO> deviceCounts = alarmStatisticsService.countByDevice(
                user, deviceIds, startTs, endTs);
        Map<String, ElectricalSafetyFunctionAlarmCountDTO> countsByFunctionCategory = new LinkedHashMap<>();
        for (AlarmDeviceCountBO deviceCount : deviceCounts) {
            if (deviceCount == null || deviceCount.getDeviceId() == null) {
                continue;
            }
            String functionCategory = functionCategoryByDeviceId.get(deviceCount.getDeviceId());
            if (functionCategory != null) {
                ElectricalSafetyFunctionAlarmCountDTO previous = countsByFunctionCategory.get(functionCategory);
                long alarmCount = deviceCount.getAlarmCount();
                long processedCount = deviceCount.getHandledCount();
                long pendingCount = Math.max(0L, alarmCount - processedCount);
                long unassignedCount = deviceCount.getUnassignedCount();
                if (previous != null) {
                    alarmCount += previous.getAlarmCount();
                    pendingCount += previous.getPendingCount();
                    unassignedCount += previous.getUnassignedCount();
                    processedCount += previous.getProcessedCount();
                }
                countsByFunctionCategory.put(functionCategory,
                        new ElectricalSafetyFunctionAlarmCountDTO(
                                functionCategory, alarmCount, pendingCount, unassignedCount, processedCount));
            }
        }

        // 转换为模块 DTO 列表，保持当前设备上下文首次出现的功能分类顺序。
        List<ElectricalSafetyFunctionAlarmCountDTO> categoryCounts =
                new ArrayList<>(countsByFunctionCategory.values());
        List<UUID> activeDeviceIds = activeAlarmDeviceIds == null
                ? Collections.emptyList() : new ArrayList<>(activeAlarmDeviceIds);
        return new ElectricalSafetyDashboardAlarmStatisticsDTO(activeDeviceIds, categoryCounts);
    }

    /** 取得当前用户可见的电气安全设备上下文，并统一将 null 返回值收敛为空列表。 */
    private List<VisibleDeviceContextBO> findVisibleElectricalSafetyDevices(SecurityUser user)
            throws ThingsboardException {
        // productCategory 使用项目配置中的规范属性值，绝不基于名称、型号或 telemetry 推断。
        List<VisibleDeviceContextBO> contexts = visibleDeviceContextService.findVisibleDeviceContexts(
                user, ELECTRICAL_SAFETY_PRODUCT_CATEGORY);
        return contexts == null ? Collections.emptyList() : contexts;
    }

    /** 从已授权设备上下文中提取非空 ID，供平台告警查询作 originator 范围限制。 */
    private List<UUID> collectDeviceIds(List<VisibleDeviceContextBO> contexts) {
        List<UUID> deviceIds = new ArrayList<>();
        for (VisibleDeviceContextBO context : contexts) {
            if (context != null && context.getDeviceId() != null) {
                deviceIds.add(context.getDeviceId());
            }
        }
        return deviceIds;
    }

    /** 根据当前日期和页面周期计算包含自然周期起点的 Unix 毫秒时间戳。 */
    private long calculatePeriodStart(String period, LocalDate today, ZoneId zoneId)
            throws ThingsboardException {
        LocalDate periodStart;
        if ("day".equals(period)) {
            periodStart = today;
        } else if ("week".equals(period)) {
            periodStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if ("month".equals(period)) {
            periodStart = today.withDayOfMonth(1);
        } else {
            throw new ThingsboardException("Unsupported period", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return periodStart.atStartOfDay(zoneId).toInstant().toEpochMilli();
    }
}
