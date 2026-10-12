package org.thingsboard.server.service.alarms.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisOverviewDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmLeafAssetCountDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSeverityTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/** 面向当前用户可见防雷设备的告警分析页面业务接口。 */
public interface DeviceAlarmAnalysisService {

    /**
     * 按告警触发时间（startTs）统计今日/本月/今年的告警次数、环比和对应趋势时间桶。
     *
     * <p>统计范围按当前用户权限与 productCategory=lightning 双重过滤。</p>
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 日、月、年三个周期各自的当前次数、上一周期次数、变化百分比及趋势数据
     * @throws ThingsboardException 设备或告警查询失败时抛出
     */
    DeviceAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;

    /** 按周期返回当前用户可见防雷设备所属末级父资产的告警数量和处理率。 */
    List<DeviceAlarmLeafAssetCountDTO> getLeafAssetCounts(SecurityUser user, String period)
            throws ThingsboardException;

    /** 按周期返回防雷设备告警类型排行。 */
    List<DeviceAlarmTypeTotalDTO> getTypeRanking(SecurityUser user, String period)
            throws ThingsboardException;

    /** 按周期返回防雷设备告警严重程度分布。 */
    List<DeviceAlarmSeverityTotalDTO> getSeverityRanking(SecurityUser user, String period)
            throws ThingsboardException;

    /** 按周期返回防雷设备确认/清除状态分布和告警覆盖率。 */
    DeviceAlarmAnalysisOverviewDTO getOverview(SecurityUser user, String period)
            throws ThingsboardException;
}
