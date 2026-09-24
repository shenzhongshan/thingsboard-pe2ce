/** 文件职责：告警分析周期卡片与趋势时间桶的统一返回 DTO。 */
package org.thingsboard.server.service.alarms.vo;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/** 告警分析顶部日、月、年统计。统计范围已经在 service 中按用户权限过滤。 */
@Getter
public class DeviceAlarmAnalysisSummaryVO {

    /** 今日告警次数及与昨日对比。 */
    private final DeviceAlarmPeriodSummaryVO day;
    /** 本月告警次数及与上月对比。 */
    private final DeviceAlarmPeriodSummaryVO month;
    /** 今年告警次数及与去年对比。 */
    private final DeviceAlarmPeriodSummaryVO year;
    /** 今日按小时统计的趋势数据。 */
    private final List<DeviceAlarmTrendPointVO> dayTrend;
    /** 本月按日期统计的趋势数据。 */
    private final List<DeviceAlarmTrendPointVO> monthTrend;
    /** 今年按月份统计的趋势数据。 */
    private final List<DeviceAlarmTrendPointVO> yearTrend;
    /** 当前仍未清除且严重等级为 CRITICAL 的重点设备。 */
    private final List<DeviceAlarmFocusDeviceVO> focusDevices;

    /** 组装周期卡片与日/月/年趋势数据。 */
    public DeviceAlarmAnalysisSummaryVO(DeviceAlarmPeriodSummaryVO day,
                                                 DeviceAlarmPeriodSummaryVO month,
                                                 DeviceAlarmPeriodSummaryVO year,
                                                 List<DeviceAlarmTrendPointVO> dayTrend,
                                                 List<DeviceAlarmTrendPointVO> monthTrend,
                                                 List<DeviceAlarmTrendPointVO> yearTrend,
                                                 List<DeviceAlarmFocusDeviceVO> focusDevices) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.dayTrend = dayTrend;
        this.monthTrend = monthTrend;
        this.yearTrend = yearTrend;
        this.focusDevices = focusDevices;
    }
    /**
     * 兼容只需要日、月、年三张统计卡片的旧调用方。
     * 趋势和重点设备没有参与旧接口契约时统一返回空列表，避免删除兼容构造器后旧调用失败。
     */
    public DeviceAlarmAnalysisSummaryVO(DeviceAlarmPeriodSummaryVO day,
                                        DeviceAlarmPeriodSummaryVO month,
                                        DeviceAlarmPeriodSummaryVO year) {
        this(day, month, year, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());
    }
}
