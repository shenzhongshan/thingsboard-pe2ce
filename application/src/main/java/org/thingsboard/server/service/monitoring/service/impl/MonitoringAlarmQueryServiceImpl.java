/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 监测模块共用的告警查询实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class MonitoringAlarmQueryServiceImpl implements MonitoringAlarmQueryService {

    private final AlarmService alarmService;

    @Override
    public PageData<AlarmInfo> findPage(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException {
        if (query == null) {
            return PageData.emptyPageData();
        }
        if (user.isCustomerUser()) {
            return alarmService.findCustomerAlarmsV2(
                    user.getTenantId(), user.getCustomerId(), query);
        }
        return alarmService.findAlarmsV2(user.getTenantId(), query);
    }

    @Override
    public List<AlarmInfo> findAll(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException {
        if (query == null || query.getPageLink() == null) {
            return Collections.emptyList();
        }

        List<AlarmInfo> alarms = new ArrayList<>();
        TimePageLink pageLink = query.getPageLink();
        while (pageLink != null) {
            PageData<AlarmInfo> pageData = findPage(user, copyQueryWithPageLink(query, pageLink));
            alarms.addAll(pageData.getData());
            pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
        }
        return alarms;
    }

    private AlarmQueryV2 copyQueryWithPageLink(AlarmQueryV2 query, TimePageLink pageLink) {
        return new AlarmQueryV2(
                query.getAffectedEntityId(),
                pageLink,
                query.getTypeList(),
                query.getStatusList(),
                query.getSeverityList(),
                query.getAssigneeId()
        );
    }
}
