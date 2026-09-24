
package org.thingsboard.server.service.monitoring.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.DeviceStatusSummaryDTO;
import org.thingsboard.server.service.monitoring.service.DeviceStatusSummaryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.util.DeviceStatusSummaryCalculator;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.*;
import java.util.concurrent.ExecutionException;

/** 顶部导航栏设备状态统计的业务实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceStatusSummaryServiceImpl implements DeviceStatusSummaryService {

    private static final String ACTIVE_TELEMETRY_KEY = "active";
    private static final int TELEMETRY_BATCH_SIZE = 50;

    private final SmartLightningDeviceService smartLightningDeviceService;
    private final TimeseriesService timeseriesService;
    private final AlarmStatisticsService alarmStatisticsService;

    /**
     * 统计当前用户可见设备中“正常”与“异常”的数量。
     *
     * <p>正常 = 在线（active 为 true/1）且没有未清除告警；
     * 异常 = 存在未清除告警的设备（按设备去重，无论在线离线）；
     * 离线且无告警的设备不计入这两项。</p>
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 正常与异常设备数量
     */
    @Override
    public DeviceStatusSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        // 先取得当前用户有权读取的设备范围，后续属性和告警查询都限制在这批设备内。
        Set<UUID> visibleDeviceIds = smartLightningDeviceService.getVisibleDeviceIds(user);
        // 从最新时序数据读取在线状态，再独立查询存在未清除告警的设备集合。
        Map<UUID, String> activeValuesByDeviceId = findActiveValues(user, visibleDeviceIds);
        Set<UUID> activeAlarmDeviceIds = alarmStatisticsService.findActiveAlarmDeviceIds(user, visibleDeviceIds);
        // 由纯计算器按统一口径生成正常数和异常数，不在服务中重复计算规则。
        return DeviceStatusSummaryCalculator.calculate(visibleDeviceIds, activeValuesByDeviceId, activeAlarmDeviceIds);
    }

    /** 分批读取每台设备的最新 active 遥测，限制并发量并避免读取无关属性。 */
    private Map<UUID, String> findActiveValues(SecurityUser user, Set<UUID> deviceIds)
            throws ThingsboardException {
        List<UUID> orderedDeviceIds = new ArrayList<>(deviceIds);
        Map<UUID, String> activeValuesByDeviceId = new HashMap<>();
        for (int batchStart = 0; batchStart < orderedDeviceIds.size(); batchStart += TELEMETRY_BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + TELEMETRY_BATCH_SIZE, orderedDeviceIds.size());
            List<ListenableFuture<Optional<TsKvEntry>>> futures = new ArrayList<>();
            for (int index = batchStart; index < batchEnd; index++) {
                UUID deviceId = orderedDeviceIds.get(index);
                futures.add(timeseriesService.findLatest(
                        user.getTenantId(), new DeviceId(deviceId), ACTIVE_TELEMETRY_KEY));
            }
            List<Optional<TsKvEntry>> entriesByDevice;
            try {
                // 单台设备查询失败时返回 null 占位，不影响同批其他设备的状态统计。
                entriesByDevice = Futures.successfulAsList(futures).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ThingsboardException(
                        "Failed to read latest device active telemetry", e, ThingsboardErrorCode.DATABASE);
            } catch (ExecutionException e) {
                throw new ThingsboardException(
                        "Failed to read latest device active telemetry", e, ThingsboardErrorCode.DATABASE);
            }
            for (int index = 0; index < entriesByDevice.size(); index++) {
                Optional<TsKvEntry> latestEntry = entriesByDevice.get(index);
                if (latestEntry != null && latestEntry.isPresent()
                        && MonitoringValueParser.hasStoredValue(latestEntry.get())) {
                    activeValuesByDeviceId.put(
                            orderedDeviceIds.get(batchStart + index), latestEntry.get().getValueAsString());
                }
            }
        }
        return activeValuesByDeviceId;
    }

}
