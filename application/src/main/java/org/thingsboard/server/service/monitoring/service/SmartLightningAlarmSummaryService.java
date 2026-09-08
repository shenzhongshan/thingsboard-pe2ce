package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmSummaryDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 智能防雷看板告警统计业务接口。 */
public interface SmartLightningAlarmSummaryService {

    /** 返回当前用户可见防雷设备的未清除告警总数及按类型统计。 */
    SmartLightningAlarmSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;
}
