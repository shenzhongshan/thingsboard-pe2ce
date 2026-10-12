/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.alarms.service;

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
        /**
         * 处理一页告警明细。
         *
         * @param page 当前页的告警明细
         * @throws ThingsboardException 调用方处理当前页失败时向上抛出
         */
        void visit(List<AlarmInfo> page) throws ThingsboardException;
    }

    /**
     * 使用平台租户或客户告警查询入口，按当前用户权限查询一页告警。
     *
     * @param user 当前登录用户
     * @param query 原生 v2 告警过滤条件和分页参数
     * @return 当前用户权限范围内的一页告警
     * @throws ThingsboardException 原生告警查询失败时抛出
     */
    PageData<AlarmInfo> findPage(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException;

    /**
     * 在调用方已按当前用户权限筛出的设备 ID 范围内查询告警。
     *
     * @param user 当前登录用户，用于限定租户数据范围
     * @param query ThingsBoard v2 告警分页与过滤条件
     * @param visibleDeviceIds 调用方按自身模块类别筛选出的当前用户可见设备 ID
     * @return 仅包含给定设备直接产生的告警分页
     * @throws ThingsboardException 底层平台告警查询失败时抛出
     */
    PageData<AlarmInfo> findPageForDevices(SecurityUser user, AlarmQueryV2 query,
                                          Collection<UUID> visibleDeviceIds) throws ThingsboardException;

    /**
     * 根据当前用户权限逐页处理告警。
     *
     * <p>实现只在内存中保留当前页，适合统计、去重和趋势聚合等场景；
     * 不允许在该方法内部合并出全量告警列表。</p>
     *
     * @param user 当前登录用户
     * @param query 原生 v2 告警过滤条件和首页面参数
     * @param visitor 对每个查询结果页执行的处理逻辑
     * @throws ThingsboardException 查询任一页面或访问器处理失败时抛出
     */
    void forEachPage(SecurityUser user, AlarmQueryV2 query, AlarmPageVisitor visitor)
            throws ThingsboardException;

}
