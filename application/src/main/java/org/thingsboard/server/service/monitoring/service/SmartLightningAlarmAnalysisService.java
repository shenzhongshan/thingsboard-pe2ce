package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 告警分析页面的周期统计接口。 */
public interface SmartLightningAlarmAnalysisService {

    SmartLightningAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;
}
