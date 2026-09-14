/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.SmartLightningLeafAssetAlarmCountDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeResponse;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningLeafAssetAlarmService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringTreeService;
import org.thingsboard.server.service.monitoring.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 末级设备父资产告警统计实现；按用户可见树和告警触发时间计算。
 *
 * <p>统计节点必须同时满足两个条件：没有资产子节点，并且直接挂载至少一台设备。
 * 这样只有资产树的末级设备父节点会作为站点统计，祖先资产不会被重复计入。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class  SmartLightningLeafAssetAlarmServiceImpl implements SmartLightningLeafAssetAlarmService {

    /** 单次告警查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningMonitoringTreeService treeService;
    private final MonitoringAlarmQueryService alarmQueryService;

    /**
     * 统计每个末级设备父资产直接挂载设备在指定周期内的告警数量和处理率。
     *
     * @param user 当前登录用户，资产树和告警都按其权限过滤
     * @param period 统计周期，支持 day、week、month（按当前时刻向前回溯）
     * @return 末级设备父资产统计列表，按告警数量降序、资产名升序排列
     * @throws ThingsboardException 资产树或告警查询失败时抛出
     */
    @Override
    public List<SmartLightningLeafAssetAlarmCountDTO> getCounts(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        SmartLightningMonitoringTreeResponse tree = treeService.getTree(user);
        Map<UUID, ParentAssetCounter> counters = new LinkedHashMap<>();
        Map<UUID, UUID> deviceToParentAsset = new HashMap<>();
        for (SmartLightningMonitoringTreeResponse.TreeNode root : tree.roots()) {
            collectDeviceParentAssets(root, counters, deviceToParentAsset);
        }
        if (counters.isEmpty()) {
            return Collections.emptyList();
        }

        // AlarmQueryV2 的 TimePageLink 按告警创建时间分页，不等同于告警触发时间 startTs。
        // 本统计要求按 startTs 计算，因此不能把周期边界放进分页条件，否则创建时间
        // 与触发时间跨界的告警会被接口提前过滤掉，后面的 startTs 判断也就无法补回。
        // 这里沿用告警分析公共查询的无时间分页方式，再在下方按 startTs 过滤。
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null);
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            if (alarm.getOriginator() == null || alarm.getOriginator().getEntityType() != EntityType.DEVICE) {
                continue;
            }
            long triggerTime = alarm.getStartTs();
            if (triggerTime < window.getStartTs() || triggerTime >= window.getEndTs()) {
                continue;
            }
            UUID parentAssetId = deviceToParentAsset.get(alarm.getOriginator().getId());
            if (parentAssetId != null) {
                ParentAssetCounter counter = counters.get(parentAssetId);
                counter.alarmCount++;
                if (alarm.isCleared()) {
                    counter.handledCount++;
                }
            }
        }

        List<SmartLightningLeafAssetAlarmCountDTO> result = new ArrayList<>();
        for (ParentAssetCounter counter : counters.values()) {
            result.add(new SmartLightningLeafAssetAlarmCountDTO(
                    counter.assetId,
                    counter.assetName,
                    counter.alarmCount,
                    counter.handledCount,
                    counter.handledRate()));
        }
        result.sort(Comparator.comparingLong(SmartLightningLeafAssetAlarmCountDTO::getAlarmCount)
                .reversed()
                .thenComparing(SmartLightningLeafAssetAlarmCountDTO::getAssetName,
                        Comparator.nullsLast(String::compareTo)));
        return result;
    }

    /**
     * 递归遍历资产树，登记末级设备父资产并建立设备到父资产的归属映射。
     *
     * <p>判定规则：当前资产必须存在子节点，且所有直接子节点都必须是设备。
     * 只要子节点中出现资产，当前节点就不是末级父节点；设备只映射到当前节点，
     * 不向祖先资产传递。</p>
     *
     * @param node 当前遍历的树节点
     * @param counters 设备直属父资产计数器集合（保持树的遍历顺序）
     * @param deviceToParentAsset 设备 ID 到直属父资产 ID 的归属映射
     */
    private void collectDeviceParentAssets(
            SmartLightningMonitoringTreeResponse.TreeNode node,
            Map<UUID, ParentAssetCounter> counters,
        Map<UUID, UUID> deviceToParentAsset) {
        if (node.entityType() == EntityType.ASSET) {
            boolean hasChildren = !node.children().isEmpty();
            boolean allChildrenAreDevices = hasChildren;
            for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
                if (child.entityType() != EntityType.DEVICE) {
                    allChildrenAreDevices = false;
                }
            }
            if (allChildrenAreDevices) {
                counters.putIfAbsent(node.id(), new ParentAssetCounter(node.id(), node.name()));
                for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
                    if (child.entityType() == EntityType.DEVICE) {
                        deviceToParentAsset.put(child.id(), node.id());
                    }
                }
            }
        }
        for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
            if (child.entityType() == EntityType.ASSET) {
                collectDeviceParentAssets(child, counters, deviceToParentAsset);
            }
        }
    }

    /** 单个设备直属父资产的告警计数器。 */
    private static final class ParentAssetCounter {
        private final UUID assetId;
        private final String assetName;
        /** 周期内该资产下属设备的告警总数。 */
        private long alarmCount;
        /** 其中已清除（已处理）的告警数量。 */
        private long handledCount;

        private ParentAssetCounter(UUID assetId, String assetName) {
            this.assetId = assetId;
            this.assetName = assetName;
        }

        /** 处理率百分比（已清除数 / 总数 × 100），保留两位小数；无告警时为 0。 */
        private double handledRate() {
            if (alarmCount == 0) {
                return 0D;
            }
            double rate = handledCount * 100D / alarmCount;
            return Math.round(rate * 100D) / 100D;
        }
    }

}
