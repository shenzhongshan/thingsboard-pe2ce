package org.thingsboard.server.service.alarms.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 当前用户可见防雷设备的未清除告警统计。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmSummaryVO {

    /** 当前未清除（ACTIVE）告警总数。 */
    private final int activeCount;
    /** 各告警类型的数量列表，数量降序排列。 */
    private final List<DeviceAlarmTypeTotalVO> typeTotals;
}
