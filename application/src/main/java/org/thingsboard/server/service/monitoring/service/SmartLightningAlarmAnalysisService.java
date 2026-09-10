package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 告警分析页面的周期统计接口。 */
public interface SmartLightningAlarmAnalysisService {

    /**
     * 按告警触发时间（startTs）统计今日/本月/今年的告警次数及与上一对比周期的环比。
     *
     * <p>统计范围是当前用户可见的全部设备，不按产品分类过滤。</p>
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 日、月、年三个周期各自的当前次数、上一周期次数和变化百分比
     * @throws ThingsboardException 设备或告警查询失败时抛出
     */
    SmartLightningAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;
}
