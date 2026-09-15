package org.thingsboard.server.service.alarms.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSummaryDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 当前用户全部可见设备的未清除告警摘要接口。 */
public interface DeviceAlarmSummaryService {

    /** 返回当前用户可见全部设备的未清除告警总数及按类型统计。 */
    DeviceAlarmSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;
}
