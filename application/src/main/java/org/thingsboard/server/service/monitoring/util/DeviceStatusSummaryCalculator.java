package org.thingsboard.server.service.monitoring.util;

import org.thingsboard.server.service.monitoring.dto.DeviceStatusSummary;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 顶部设备状态统计的纯计算逻辑。
 *
 * <p>正常设备必须同时满足服务器属性 active 为 true/1，且没有未清除告警；
 * 异常设备为“存在未清除告警”与“已经离线”两组设备的并集，按设备 ID 去重。</p>
 */
public final class DeviceStatusSummaryCalculator {

    private DeviceStatusSummaryCalculator() {
    }

    public static DeviceStatusSummary calculate(
            List<UUID> visibleDeviceIds,
            Map<UUID, String> activeValuesByDeviceId,
            List<UUID> activeAlarmDeviceIds
    ) {
        Set<UUID> visibleDeviceIdSet = new HashSet<>(visibleDeviceIds);
        Set<UUID> abnormalDeviceIds = new HashSet<>();
        for (UUID deviceId : activeAlarmDeviceIds) {
            if (visibleDeviceIdSet.contains(deviceId)) {
                abnormalDeviceIds.add(deviceId);
            }
        }

        int normal = 0;
        for (UUID deviceId : visibleDeviceIds) {
            boolean active = isActive(activeValuesByDeviceId.get(deviceId));
            if (!active) {
                abnormalDeviceIds.add(deviceId);
            }
            if (active && !abnormalDeviceIds.contains(deviceId)) {
                normal++;
            }
        }
        return new DeviceStatusSummary(normal, abnormalDeviceIds.size());
    }

    private static boolean isActive(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

}
