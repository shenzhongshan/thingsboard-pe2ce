/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.alarms.service;

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/**
 * 设备告警模块共用的告警查询服务。
 *
 * <p>统一封装客户用户和租户用户的查询分支，并提供跨页读取能力，
 * 避免各个告警统计和监测实现类重复处理权限分支和分页。</p>
 */
public interface DeviceAlarmQueryService {

    /**
     * 告警分页访问器：调用方只处理当前页，不能把所有页合并到内存。
     */
    @FunctionalInterface
    interface AlarmPageVisitor {
        void visit(List<AlarmInfo> page) throws ThingsboardException;
    }

    /** 根据当前用户权限查询一页告警。 */
    PageData<AlarmInfo> findPage(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException;

    /**
     * 根据当前用户权限逐页处理告警。
     *
     * <p>实现只在内存中保留当前页，适合统计、去重和趋势聚合等场景；
     * 不允许在该方法内部合并出全量告警列表。</p>
     */
    void forEachPage(SecurityUser user, AlarmQueryV2 query, AlarmPageVisitor visitor)
            throws ThingsboardException;

}
