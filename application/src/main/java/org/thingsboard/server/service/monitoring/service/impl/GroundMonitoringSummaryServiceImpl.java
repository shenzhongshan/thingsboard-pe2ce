/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.monitoring.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.service.GroundMonitoringSummaryService;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummary;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * 地网监测看板的聚合实现。
 *
 * <p>设备查询复用 SmartLightningDeviceService，随后仅保留
 * functionCategory=ground_resistance 的设备。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class GroundMonitoringSummaryServiceImpl implements GroundMonitoringSummaryService {

    private static final int QUERY_PAGE_SIZE = 10_000;
    private static final String GROUND_RESISTANCE_FUNCTION_CATEGORY = "ground_resistance";
    private static final String GROUND_RESISTANCE_TELEMETRY_KEY = "groundResistance";

    private final SmartLightningDeviceService smartLightningDeviceService;
    private final MonitoringAlarmQueryService alarmQueryService;
    private final TimeseriesService timeseriesService;

    /**
     * 查询当前用户可见地网设备（functionCategory=ground_resistance）的完整摘要。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @param period 告警统计窗口，仅接受 day、week、month
     * @return 设备总数、在线/离线数量、周期内告警数、最新阻值的最大和最小值
     * @throws ThingsboardException 周期参数非法或设备、告警、遥测查询失败时抛出
     */
    @Override
    public GroundMonitoringSummary getSummary(SecurityUser user, String period) throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        List<SmartLightningDeviceContextDTO> groundDevices = findVisibleGroundDevices(user);
        List<EntityId> groundDeviceIds = new ArrayList<>();
        int online = 0;
        for (SmartLightningDeviceContextDTO device : groundDevices) {
            groundDeviceIds.add(new DeviceId(device.getDeviceId()));
            if (Boolean.TRUE.equals(device.getActive())) {
                online++;
            }
        }

        int alarmCount = countGroundAlarmsSince(user, groundDeviceIds, window);
        List<Double> resistanceValues = findLatestGroundResistanceValues(user, groundDeviceIds);
        Double maxResistance = null;
        Double minResistance = null;
        for (Double resistanceValue : resistanceValues) {
            if (maxResistance == null || resistanceValue > maxResistance) {
                maxResistance = resistanceValue;
            }
            if (minResistance == null || resistanceValue < minResistance) {
                minResistance = resistanceValue;
            }
        }

        int total = groundDevices.size();
        return new GroundMonitoringSummary(total, online, total - online, alarmCount, maxResistance, minResistance);
    }

    /** 复用智能防雷设备服务的权限、批量属性和 productCategory 过滤。 */
    private List<SmartLightningDeviceContextDTO> findVisibleGroundDevices(SecurityUser user)
            throws ThingsboardException {
        PageData<SmartLightningDeviceContextDTO> pageData = smartLightningDeviceService.getLightningDeviceContexts(
                user, new PageLink(QUERY_PAGE_SIZE, 0));
        List<SmartLightningDeviceContextDTO> groundDevices = new ArrayList<>();
        for (SmartLightningDeviceContextDTO device : pageData.getData()) {
            if (GROUND_RESISTANCE_FUNCTION_CATEGORY.equalsIgnoreCase(
                    MonitoringValueParser.trimToEmpty(device.getFunctionCategory()))) {
                groundDevices.add(device);
            }
        }
        return groundDevices;
    }

    /** 告警按 createdTime（触发时间）筛选，包含在窗口中已清除的历史告警。 */
    private int countGroundAlarmsSince(
            SecurityUser user, List<EntityId> groundDeviceIds, MonitoringPeriodWindow window)
            throws ThingsboardException {
        if (groundDeviceIds.isEmpty()) {
            return 0;
        }
        Set<UUID> groundDeviceIdValues = new HashSet<>();
        for (EntityId groundDeviceId : groundDeviceIds) {
            groundDeviceIdValues.add(groundDeviceId.getId());
        }

        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC,
                window.getStartTs(), window.getEndTs());
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), List.of(AlarmSearchStatus.ANY), Collections.emptyList(), null);
        int count = 0;
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            EntityId originator = alarm.getOriginator();
            if (originator != null && originator.getEntityType() == EntityType.DEVICE
                    && groundDeviceIdValues.contains(originator.getId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 同时读取每台地网设备的最新阻值。TimeseriesService 没有按实体保留结果的单次批量 API，
     * 故由服务层并行发起查询并统一等待，前端始终只请求一个汇总接口。
     */
    private List<Double> findLatestGroundResistanceValues(SecurityUser user, List<EntityId> groundDeviceIds)
            throws ThingsboardException {
        List<ListenableFuture<Optional<TsKvEntry>>> futures = new ArrayList<>();
        for (EntityId groundDeviceId : groundDeviceIds) {
            futures.add(timeseriesService.findLatest(
                    user.getTenantId(), new DeviceId(groundDeviceId.getId()), GROUND_RESISTANCE_TELEMETRY_KEY));
        }

        List<Optional<TsKvEntry>> latestEntries;
        try {
            latestEntries = Futures.allAsList(futures).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThingsboardException("Failed to read latest ground resistance", e, ThingsboardErrorCode.DATABASE);
        } catch (ExecutionException e) {
            throw new ThingsboardException("Failed to read latest ground resistance", e, ThingsboardErrorCode.DATABASE);
        }

        List<Double> resistanceValues = new ArrayList<>();
        for (Optional<TsKvEntry> latestEntry : latestEntries) {
            if (latestEntry.isEmpty()) {
                continue;
            }
            Double resistanceValue = MonitoringValueParser.parseFiniteDouble(latestEntry.get().getValueAsString());
            if (resistanceValue != null) {
                resistanceValues.add(resistanceValue);
            }
        }
        return resistanceValues;
    }

}
