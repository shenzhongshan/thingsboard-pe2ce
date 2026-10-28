/** 文件职责：将电气安全告警统计服务 DTO 显式映射为 HTTP VO。 */
package org.thingsboard.server.controller.electrical_safety.converter;

import org.thingsboard.server.controller.electrical_safety.vo.ElectricalSafetyDashboardAlarmStatisticsVO;
import org.thingsboard.server.controller.electrical_safety.vo.ElectricalSafetyFunctionAlarmCountVO;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyDashboardAlarmStatisticsDTO;
import org.thingsboard.server.service.electrical_safety.dto.ElectricalSafetyFunctionAlarmCountDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 转换器只做字段映射，不执行数据库访问、权限判断或统计。 */
public final class ElectricalSafetyDashboardAlarmStatisticsVOConverter {

    /** 静态转换器不持有请求状态或外部依赖。 */
    private ElectricalSafetyDashboardAlarmStatisticsVOConverter() {
    }

    /** 将告警统计 DTO 转换为稳定的模块接口 VO。 */
    public static ElectricalSafetyDashboardAlarmStatisticsVO toVO(
            ElectricalSafetyDashboardAlarmStatisticsDTO source) {
        if (source == null) {
            return ElectricalSafetyDashboardAlarmStatisticsVO.builder()
                    .activeAlarmDeviceIds(Collections.emptyList())
                    .functionCategoryCounts(Collections.emptyList())
                    .build();
        }
        List<ElectricalSafetyFunctionAlarmCountVO> counts = new ArrayList<>();
        if (source.getFunctionCategoryCounts() != null) {
            for (ElectricalSafetyFunctionAlarmCountDTO count : source.getFunctionCategoryCounts()) {
                if (count != null) {
                    counts.add(ElectricalSafetyFunctionAlarmCountVO.builder()
                            .functionCategory(count.getFunctionCategory())
                            .alarmCount(count.getAlarmCount())
                            .pendingCount(count.getPendingCount())
                            .unassignedCount(count.getUnassignedCount())
                            .processedCount(count.getProcessedCount())
                            .build());
                }
            }
        }
        return ElectricalSafetyDashboardAlarmStatisticsVO.builder()
                .activeAlarmDeviceIds(source.getActiveAlarmDeviceIds() == null
                        ? Collections.emptyList() : new ArrayList<>(source.getActiveAlarmDeviceIds()))
                .functionCategoryCounts(counts)
                .build();
    }
}
