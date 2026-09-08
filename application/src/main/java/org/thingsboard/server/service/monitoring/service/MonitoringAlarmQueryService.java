/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

/**
 * 监测模块共用的告警查询服务。
 *
 * <p>统一封装客户用户和租户用户的查询分支，并提供跨页读取能力，
 * 避免每个监测实现类重复处理权限分支和分页。</p>
 */
public interface MonitoringAlarmQueryService {

    /** 根据当前用户权限查询一页告警。 */
    PageData<AlarmInfo> findPage(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException;

    /** 根据当前用户权限读取查询条件匹配的全部告警。 */
    List<AlarmInfo> findAll(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException;
}
