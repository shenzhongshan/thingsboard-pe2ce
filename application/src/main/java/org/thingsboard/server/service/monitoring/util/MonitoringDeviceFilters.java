/** 文件职责：监测模块设备分类筛选的公共规则。 */
package org.thingsboard.server.service.monitoring.util;

import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 监测设备分类工具。
 *
 * <p>看板、数据监测和数据查询都使用设备服务器属性 functionCategory 作为分类依据，
 * 统一在这里完成空值处理和大小写不敏感匹配，避免各页面产生不同设备数量。</p>
 */
public final class MonitoringDeviceFilters {

    private MonitoringDeviceFilters() {
    }

    /** 按 functionCategory 筛选设备；输入为空时返回不可变空列表。 */
    public static List<MonitoringDeviceContextBO> byFunctionCategory(
            List<MonitoringDeviceContextBO> devices, String functionCategory) {
        if (devices == null || devices.isEmpty() || functionCategory == null) {
            return Collections.emptyList();
        }
        List<MonitoringDeviceContextBO> result = new ArrayList<>();
        for (MonitoringDeviceContextBO device : devices) {
            if (device == null) {
                continue;
            }
            String category = MonitoringValueParser.trimToEmpty(device.getFunctionCategory());
            if (functionCategory.equalsIgnoreCase(category)) {
                result.add(device);
            }
        }
        return result;
    }
}
