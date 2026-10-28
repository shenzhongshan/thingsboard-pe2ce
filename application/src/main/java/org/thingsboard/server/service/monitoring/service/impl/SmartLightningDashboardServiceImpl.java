package org.thingsboard.server.service.monitoring.service.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDashboardService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 智能防雷看板页面级服务实现。
 *
 * <p>对外提供一个页面级实现；内部按地网、SPD 和雷电流三个板块隔离原有逻辑。
 * SPD 实时状态使用接地、空开、温度和寿命规范遥测键，周期统计仍沿用现有时间窗口。</p>
 */
@Service
@TbCoreComponent
public class SmartLightningDashboardServiceImpl implements SmartLightningDashboardService {

    private final GroundSummaryHandler groundSummaryHandler;
    private final SpdSummaryHandler spdSummaryHandler;
    private final LightningCurrentSummaryHandler lightningCurrentSummaryHandler;

    /**
     * 初始化三个看板板块处理器，并向它们注入相同的设备、告警和遥测服务。
     *
     * <p>本服务保留 Spring 对外入口，具体指标查询和汇总由对应处理器负责。</p>
     */
    public SmartLightningDashboardServiceImpl(
            SmartLightningDeviceService smartLightningDeviceService,
            AlarmStatisticsService alarmStatisticsService,
            TimeseriesService timeseriesService
    ) {
        this.groundSummaryHandler = new GroundSummaryHandler(
                smartLightningDeviceService, alarmStatisticsService, timeseriesService);
        this.spdSummaryHandler = new SpdSummaryHandler(
                smartLightningDeviceService, alarmStatisticsService, timeseriesService);
        this.lightningCurrentSummaryHandler = new LightningCurrentSummaryHandler(
                smartLightningDeviceService, alarmStatisticsService, timeseriesService);
    }

    @Override
    public GroundMonitoringSummaryDTO getGroundSummary(SecurityUser user, String period)
            throws ThingsboardException {
        // 将地网摘要请求交给专属处理器，保留原有服务接口。
        return groundSummaryHandler.getSummary(user, period);
    }

    @Override
    public SpdMonitoringSummaryDTO getSpdSummary(SecurityUser user, String period)
            throws ThingsboardException {
        // SPD 设备筛选、遥测读取及指标聚合由 SPD 处理器完成。
        return spdSummaryHandler.getSummary(user, period);
    }

    @Override
    public LightningCurrentMonitoringSummaryDTO getLightningCurrentSummary(SecurityUser user, String period)
            throws ThingsboardException {
        // 雷电流周期统计由雷电流处理器完成。
        return lightningCurrentSummaryHandler.getSummary(user, period);
    }



}
