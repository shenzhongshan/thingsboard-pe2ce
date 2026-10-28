/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.alarms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 设备告警模块共用的告警查询实现，封装 ThingsBoard AlarmService 的权限查询和设备集合查询。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceAlarmQueryServiceImpl implements DeviceAlarmQueryService {

    /** ThingsBoard 原生告警数据访问服务；本类通过它复用平台过滤和分页能力。 */
    private final AlarmService alarmService;

    /** 原生告警搜索支持的字段，保持与 ThingsBoard v2 告警查询语义一致。 */
    private static final List<EntityKey> ALARM_SEARCH_FIELDS = List.of(
            new EntityKey(EntityKeyType.ALARM_FIELD, "type"),
            new EntityKey(EntityKeyType.ALARM_FIELD, "severity"),
            new EntityKey(EntityKeyType.ALARM_FIELD, "status"));

    /** 数据库告警查询允许排序的字段，阻止未经校验的字段进入 SQL 排序表达式。 */
    private static final Set<String> ALARM_SORT_PROPERTIES = Set.of(
            "createdTime", "ackTs", "ackTime", "clearTs", "clearTime", "assignTime", "details",
            "endTs", "endTime", "startTs", "startTime", "acknowledged", "cleared", "type", "severity",
            "originatorId", "originatorType", "originator", "originatorLabel", "originatorDisplayName",
            "assigneeId", "assignee", "assigneeFirstName", "assigneeLastName", "assigneeEmail", "status");

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
    public PageData<AlarmInfo> findPage(SecurityUser user, AlarmQueryV2 query) {
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
     * 只在调用方提供的已授权设备集合中查询告警，避免先拉取整个租户的告警再做内存过滤。
     * 权限范围由模块服务通过 VisibleDeviceContextService 获取；数据库查询仍复用平台 AlarmService。
     *
     * @param user 当前登录用户，用于限定租户数据库查询
     * @param query 原生告警类型、状态、严重程度、受理人、时间、文本、排序和分页条件
     * @param visibleDeviceIds 已由模块服务按用户 READ 权限和 productCategory 筛选的设备 ID
     * @return 仅由给定设备直接产生的告警分页；非法分页或空设备集合返回空页
     */
    @Override
    public PageData<AlarmInfo> findPageForDevices(
            SecurityUser user, AlarmQueryV2 query, Collection<UUID> visibleDeviceIds) {
        // 查询条件或已授权设备集合缺失时，不退化为全租户查询，直接返回空页以保持模块隔离。
        if (query == null || query.getPageLink() == null || visibleDeviceIds == null || visibleDeviceIds.isEmpty()) {
            return PageData.emptyPageData();
        }
        TimePageLink pageLink = query.getPageLink();
        // 拒绝非法页号和每页数量，避免异常分页参数产生无效或不可控的数据库查询。
        if (pageLink.getPageSize() <= 0 || pageLink.getPage() < 0) {
            return PageData.emptyPageData();
        }

        // 保留设备 ID 的首次出现顺序并去重；调用方已按当前用户权限和模块类别预先筛选该集合。
        Set<UUID> allowedDeviceIds = new LinkedHashSet<>(visibleDeviceIds);
        List<EntityId> originatorIds = new ArrayList<>(allowedDeviceIds.size());
        for (UUID deviceId : allowedDeviceIds) {
            if (deviceId != null) {
                originatorIds.add(new DeviceId(deviceId));
            }
        }
        if (originatorIds.isEmpty()) {
            // 原集合可能只包含 null；清理后没有合法设备时不执行底层查询。
            return PageData.emptyPageData();
        }

        // 单实体过滤只允许命中本模块已授权设备，其他实体类型或越界设备均返回空页。
        if (query.getAffectedEntityId() != null) {
            EntityId affectedEntityId = query.getAffectedEntityId();
            if (affectedEntityId.getEntityType() != EntityType.DEVICE
                    || !allowedDeviceIds.contains(affectedEntityId.getId())) {
                return PageData.emptyPageData();
            }
            originatorIds = List.of(new DeviceId(affectedEntityId.getId()));
        }

        // 将 v2 告警查询排序转换为数据查询排序；字段白名单及默认创建时间倒序在辅助方法中统一处理。
        EntityDataSortOrder sortOrder = createAlarmSortOrder(pageLink);
        // 逐项映射分页、时间范围、类型、状态、严重程度和受理人条件，保持 v2 查询的筛选口径。
        AlarmDataPageLink alarmPageLink = new AlarmDataPageLink(
                pageLink.getPageSize(), pageLink.getPage(), pageLink.getTextSearch(), sortOrder,
                false, false,
                pageLink.getStartTime() == null ? 0 : pageLink.getStartTime(),
                pageLink.getEndTime() == null ? 0 : pageLink.getEndTime(),
                0,
                query.getTypeList(), query.getStatusList(), query.getSeverityList(), query.getAssigneeId());
        // ALARM_FIELD 搜索键与原生 v2 接口一致：告警类型、严重程度和状态。
        AlarmDataQuery alarmDataQuery = new AlarmDataQuery(
                null, alarmPageLink, null, null, null, ALARM_SEARCH_FIELDS);
        // 授权设备以 originator 条件传入平台查询，在分页之前限制范围，避免先查全租户再过滤导致页数失真。
        PageData<AlarmData> alarmDataPage = alarmService.findAlarmDataByQueryForEntities(
                user.getTenantId(), alarmDataQuery, originatorIds);

        // AlarmInfo 是模块接口沿用的告警响应类型；逐条显式转换并保留平台结果顺序。
        List<AlarmInfo> alarms = new ArrayList<>(alarmDataPage.getData().size());
        for (AlarmData alarmData : alarmDataPage.getData()) {
            alarms.add(new AlarmInfo(alarmData));
        }
        // 复制底层分页元信息；总数反映授权设备范围，供前端继续使用原生分页交互。
        return new PageData<>(alarms, alarmDataPage.getTotalPages(),
                alarmDataPage.getTotalElements(), alarmDataPage.hasNext());
    }

    /**
     * 创建校验过字段名的告警排序，避免未支持的用户输入字段进入底层查询排序表达式。
     * 未提供或不在白名单中的属性统一回退到创建时间倒序。
     *
     * @param pageLink ThingsBoard v2 时间分页链接，可能包含排序字段和方向
     * @return AlarmData 查询使用的告警字段排序
     */
    private EntityDataSortOrder createAlarmSortOrder(TimePageLink pageLink) {
        // 先设置稳定的默认排序；请求缺少排序信息或字段不受支持时继续使用此默认值。
        String property = "createdTime";
        org.thingsboard.server.common.data.page.SortOrder.Direction direction =
                org.thingsboard.server.common.data.page.SortOrder.Direction.DESC;
        // 只采纳字段名在白名单内且方向非空的排序；方向为平台枚举，不接受任意 SQL 片段。
        if (pageLink.getSortOrder() != null
                && pageLink.getSortOrder().getDirection() != null
                && ALARM_SORT_PROPERTIES.contains(pageLink.getSortOrder().getProperty())) {
            property = pageLink.getSortOrder().getProperty();
            direction = pageLink.getSortOrder().getDirection();
        }
        // AlarmData 以 ALARM_FIELD 类型表示告警字段，并直接复用平台排序方向定义。
        return new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ALARM_FIELD, property),
                EntityDataSortOrder.Direction.valueOf(direction.name()));
    }

    /**
     * 跨页读取查询条件匹配的告警，并把每一页交给调用方即时处理。
     *
     * <p>这里故意不创建全量 {@code List<AlarmInfo>}。调用方完成当前页的计数、
     * 去重或分桶后，当前页即可被释放，避免历史告警量增长时持续占用堆内存。</p>
     */
    @Override
    public void forEachPage(SecurityUser user, AlarmQueryV2 query, AlarmPageVisitor visitor)
            throws ThingsboardException {
        Objects.requireNonNull(visitor, "告警分页访问器不能为空");
        if (query == null || query.getPageLink() == null) {
            return;
        }

        TimePageLink pageLink = query.getPageLink();
        while (pageLink != null) {
            PageData<AlarmInfo> pageData = findPage(user, copyQueryWithPageLink(query, pageLink));
            visitor.visit(pageData.getData());
            // hasNext 为 true 表示还有下一页，继续用下一页链接查询。
            pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
        }
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
