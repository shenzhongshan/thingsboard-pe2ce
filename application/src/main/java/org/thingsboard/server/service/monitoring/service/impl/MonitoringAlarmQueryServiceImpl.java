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

    /**
     * 根据当前用户身份查询单页告警。
     *
     * <p>客户用户只能查询本客户范围内的告警（findCustomerAlarmsV2），
     * 租户管理员可查询整个租户的告警（findAlarmsV2）；
     * 该分支逻辑与 ThingsBoard 原生告警页面保持一致。</p>
     *
     * @param user 当前登录用户，用于判断用户类型并取得租户/客户 ID
     * @param query 告警查询条件（分页、时间范围、状态、类型等）
     * @return 单页告警数据
     */
    @Override
    public PageData<AlarmInfo> findPage(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException {
        // 查询对象为空时直接返回空页，避免下游空指针。
        if (query == null) {
            return PageData.emptyPageData();
        }
        // 客户用户与租户用户走不同的原生查询入口，保证权限隔离。
        if (user.isCustomerUser()) {
            return alarmService.findCustomerAlarmsV2(
                    user.getTenantId(), user.getCustomerId(), query);
        }
        return alarmService.findAlarmsV2(user.getTenantId(), query);
    }

    /**
     * 跨页读取查询条件匹配的全部告警。
     *
     * <p>循环调用 {@link #findPage} 并通过 nextPageLink 翻页，
     * 直到没有下一页为止，避免固定上限造成统计遗漏。</p>
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @param query 告警查询条件，其分页参数决定每页大小
     * @return 所有页合并后的完整告警列表
     */
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
            // hasNext 为 true 表示还有下一页，继续用下一页链接查询。
            pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
        }
        return alarms;
    }

    /**
     * 用新的分页参数复制一份查询条件。
     *
     * <p>AlarmQueryV2 本身不可变，翻页时只能重建查询对象；
     * 其余过滤条件（类型、状态、等级、受理人）原样保留。</p>
     */
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
