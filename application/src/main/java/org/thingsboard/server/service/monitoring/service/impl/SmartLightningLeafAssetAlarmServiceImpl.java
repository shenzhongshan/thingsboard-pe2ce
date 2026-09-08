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

/** 末级资产告警统计实现；按用户可见树和告警触发时间计算。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningLeafAssetAlarmServiceImpl implements SmartLightningLeafAssetAlarmService {

    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningMonitoringTreeService treeService;
    private final MonitoringAlarmQueryService alarmQueryService;

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

    private static final class LeafAssetCounter {
        private final UUID assetId;
        private final String assetName;
        private long alarmCount;
        private long handledCount;

        private LeafAssetCounter(UUID assetId, String assetName) {
            this.assetId = assetId;
            this.assetName = assetName;
        }

        private double handledRate() {
            if (alarmCount == 0) {
                return 0D;
            }
            double rate = handledCount * 100D / alarmCount;
            return Math.round(rate * 100D) / 100D;
        }
    }

    private static final class PeriodWindow {
        private final long start;
        private final long end;

        private PeriodWindow(long start, long end) {
            this.start = start;
            this.end = end;
        }

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
