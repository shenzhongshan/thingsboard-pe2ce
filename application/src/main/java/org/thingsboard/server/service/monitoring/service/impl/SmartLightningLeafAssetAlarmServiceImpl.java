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
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 末级资产告警统计实现；按用户可见树和告警触发时间计算。
 *
 * <p>“末级资产”指资产树中不再包含子资产分支的资产节点（直接挂设备的层级），
 * 统计每个末级资产下属设备在周期内的告警数量和已清除（已处理）比例。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningLeafAssetAlarmServiceImpl implements SmartLightningLeafAssetAlarmService {

    /** 单次告警查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningMonitoringTreeService treeService;
    private final MonitoringAlarmQueryService alarmQueryService;

    /**
     * 统计每个末级资产下属设备在指定周期内的告警数量和处理率。
     *
     * @param user 当前登录用户，资产树和告警都按其权限过滤
     * @param period 统计周期，支持 day、month、year（自然日/月/年起点）
     * @return 末级资产统计列表，按告警数量降序、资产名升序排列
     * @throws ThingsboardException 资产树或告警查询失败时抛出
     */
    @Override
    public List<SmartLightningLeafAssetAlarmCountDTO> getCounts(SecurityUser user, String period)
            throws ThingsboardException {
        PeriodWindow window = PeriodWindow.current(period, ZoneId.systemDefault());
        SmartLightningMonitoringTreeResponse tree = treeService.getTree(user);
        Map<UUID, LeafAssetCounter> counters = new LinkedHashMap<>();
        Map<UUID, UUID> deviceToLeafAsset = new HashMap<>();
        for (SmartLightningMonitoringTreeResponse.TreeNode root : tree.roots()) {
            collectLeafAssets(root, null, counters, deviceToLeafAsset);
        }
        if (counters.isEmpty()) {
            return Collections.emptyList();
        }

        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC,
                window.start, window.end);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null);
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            if (alarm.getOriginator() == null || alarm.getOriginator().getEntityType() != EntityType.DEVICE) {
                continue;
            }
            long triggerTime = alarm.getStartTs();
            if (triggerTime < window.start || triggerTime >= window.end) {
                continue;
            }
            UUID leafAssetId = deviceToLeafAsset.get(alarm.getOriginator().getId());
            if (leafAssetId != null) {
                LeafAssetCounter counter = counters.get(leafAssetId);
                counter.alarmCount++;
                if (alarm.isCleared()) {
                    counter.handledCount++;
                }
            }
        }

        List<SmartLightningLeafAssetAlarmCountDTO> result = new ArrayList<>();
        for (LeafAssetCounter counter : counters.values()) {
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
     * 递归遍历资产树，登记末级资产并建立设备到末级资产的归属映射。
     *
     * <p>判定规则：没有资产子节点的资产即末级资产；
     * 设备归属到它所在路径上最近的末级资产，未挂在任何末级资产下的设备不参与统计。</p>
     *
     * @param node 当前遍历的树节点
     * @param currentLeafAssetId 祖先链上最近的末级资产 ID，根节点调用时为 null
     * @param counters 末级资产计数器集合（保持树的遍历顺序）
     * @param deviceToLeafAsset 设备 ID 到末级资产 ID 的归属映射
     */
    private void collectLeafAssets(
            SmartLightningMonitoringTreeResponse.TreeNode node,
            UUID currentLeafAssetId,
            Map<UUID, LeafAssetCounter> counters,
            Map<UUID, UUID> deviceToLeafAsset) {
        UUID nextLeafAssetId = currentLeafAssetId;
        if (node.entityType() == EntityType.ASSET) {
            boolean hasAssetChild = false;
            for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
                if (child.entityType() == EntityType.ASSET) {
                    hasAssetChild = true;
                    break;
                }
            }
            if (!hasAssetChild) {
                nextLeafAssetId = node.id();
                counters.put(node.id(), new LeafAssetCounter(node.id(), node.name()));
            }
        }

        if (node.entityType() == EntityType.DEVICE && nextLeafAssetId != null) {
            deviceToLeafAsset.put(node.id(), nextLeafAssetId);
        }
        for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
            collectLeafAssets(child, nextLeafAssetId, counters, deviceToLeafAsset);
        }
    }

    /** 单个末级资产的告警计数器。 */
    private static final class LeafAssetCounter {
        private final UUID assetId;
        private final String assetName;
        /** 周期内该资产下属设备的告警总数。 */
        private long alarmCount;
        /** 其中已清除（已处理）的告警数量。 */
        private long handledCount;

        private LeafAssetCounter(UUID assetId, String assetName) {
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

    /** 统计周期的时间窗口：[周期起点, 当前时刻]。 */
    private static final class PeriodWindow {
        private final long start;
        private final long end;

        private PeriodWindow(long start, long end) {
            this.start = start;
            this.end = end;
        }

        /**
         * 按周期参数计算自然周期窗口。
         *
         * <p>month 取本月 1 日 0 点，year 取今年 1 月 1 日 0 点，
         * 其他值一律按当日 0 点处理（默认日周期）。</p>
         */
        private static PeriodWindow current(String period, ZoneId zoneId) {
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            ZonedDateTime start;
            if ("month".equalsIgnoreCase(period)) {
                start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
            } else if ("year".equalsIgnoreCase(period)) {
                start = now.withDayOfYear(1).toLocalDate().atStartOfDay(zoneId);
            } else {
                start = now.toLocalDate().atStartOfDay(zoneId);
            }
            return new PeriodWindow(start.toInstant().toEpochMilli(), now.toInstant().toEpochMilli());
        }
    }
}
