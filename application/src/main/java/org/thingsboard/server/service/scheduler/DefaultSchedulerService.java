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
package org.thingsboard.server.service.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * CE port of PE's scheduler runtime. Differences vs PE: no EntityGroup / generateReport branches,
 * and event change notifications arrive via direct method calls instead of queue protos (monolith).
 */
@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultSchedulerService extends AbstractPartitionBasedService<TenantId> implements SchedulerService {

    private final TenantService tenantService;
    private final TbClusterService clusterService;
    private final SchedulerEventService schedulerEventService;
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    private final OtaPackageService otaPackageService;
    private final OtaPackageStateService firmwareStateService;
    private final TbServiceInfoProvider serviceInfoProvider;

    @Value("${server.rest.server_side_rpc.min_timeout:5000}")
    protected long minTimeout;

    private final ConcurrentMap<TenantId, List<SchedulerEventId>> tenantEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<SchedulerEventId, SchedulerEventMetaData> eventsMetaData = new ConcurrentHashMap<>();
    private volatile boolean firstRun = true;
    private String serviceId;

    @PostConstruct
    public void init() {
        super.init();
        this.serviceId = serviceInfoProvider.getServiceId();
    }

    @PreDestroy
    public void stop() {
        super.stop();
    }

    @Override
    protected String getServiceName() {
        return "Scheduler";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "scheduler-service";
    }

    @Override
    public void onSchedulerEventAdded(SchedulerEventInfo event) {
        scheduledExecutor.submit(() -> {
            if (!eventsMetaData.containsKey(event.getId())) {
                scheduleAndAddToMap(event);
            }
        });
    }

    @Override
    public void onSchedulerEventUpdated(SchedulerEventInfo event) {
        scheduledExecutor.submit(() -> {
            SchedulerEventMetaData oldMd = eventsMetaData.remove(event.getId());
            if (oldMd != null && oldMd.getNextTaskFuture() != null) {
                oldMd.getNextTaskFuture().cancel(false);
            }
            scheduleAndAddToMap(event);
        });
    }

    @Override
    public void onSchedulerEventDeleted(SchedulerEventInfo event) {
        scheduledExecutor.submit(() -> onEventDeleted(event.getId()));
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        log.info("Scheduler service {}", firstRun ? "initializing" : "updating");
        long ts = System.currentTimeMillis();
        PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, 1024);
        for (Tenant tenant : tenantIterator) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenant.getId(), tenant.getId());
            if (!addedPartitions.contains(tpi)) {
                continue;
            }
            partitionedEntities.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()).add(tenant.getId());
            addEventsForTenant(ts, tenant);
        }
        log.info("Scheduler service {}.", firstRun ? "initialized" : "updated");
        firstRun = false;
        return Collections.emptyMap();
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(TenantId tenantId) {
        tenantEvents.getOrDefault(tenantId, Collections.emptyList()).forEach(this::onEventDeleted);
        tenantEvents.remove(tenantId);
    }

    private void addEventsForTenant(long ts, Tenant tenant) {
        log.debug("[{}] Fetching scheduled events for tenant.", tenant.getId());
        List<SchedulerEventId> eventIds = new ArrayList<>();
        List<SchedulerEventInfo> events = schedulerEventService.findSchedulerEventsByTenantIdAndEnabled(tenant.getId(), true);
        long scheduled = 0;
        long passedAway = 0;
        for (SchedulerEventInfo event : events) {
            SchedulerEventMetaData md = new SchedulerEventMetaData(event.toDescriptor());
            if (!md.getDescriptor().passedAway(ts)) {
                eventsMetaData.put(event.getId(), md);
                eventIds.add(event.getId());
                scheduled++;
            } else {
                passedAway++;
            }
            scheduleNextEvent(ts, event, md);
        }
        tenantEvents.put(tenant.getId(), eventIds);
        log.debug("[{}] Scheduling {} events. Found {} passed events.", tenant.getId(), scheduled, passedAway);
    }

    private void scheduleNextEvent(long ts, SchedulerEventInfo event, SchedulerEventMetaData md) {
        if (!event.isEnabled()) {
            return;
        }
        long eventTs = md.getDescriptor().getNextEventTime(ts);
        if (eventTs != 0L) {
            long eventDelay = eventTs - ts;
            log.debug("[{}] Scheduling next fire in {} ms", event.getId(), eventDelay);
            md.setNextTaskFuture(scheduledExecutor.schedule(() -> processEvent(event.getTenantId(), event.getId()),
                    eventDelay, TimeUnit.MILLISECONDS));
        }
    }

    private void scheduleAndAddToMap(SchedulerEventInfo event) {
        long ts = System.currentTimeMillis();
        SchedulerEventMetaData eventMd = new SchedulerEventMetaData(event.toDescriptor());
        eventsMetaData.put(event.getId(), eventMd);
        tenantEvents.computeIfAbsent(event.getTenantId(), key -> new ArrayList<>()).add(event.getId());
        scheduleNextEvent(ts, event, eventMd);
    }

    void onEventDeleted(SchedulerEventId eventId) {
        SchedulerEventMetaData oldMd = eventsMetaData.remove(eventId);
        if (oldMd != null && oldMd.getNextTaskFuture() != null) {
            oldMd.getNextTaskFuture().cancel(false);
        }
    }

    private void processEvent(TenantId tenantId, SchedulerEventId eventId) {
        log.debug("[{}][{}] processEvent", tenantId, eventId);
        SchedulerEventMetaData md = eventsMetaData.get(eventId);
        if (md == null) {
            log.debug("[{}] Triggered processing of removed event.", eventId);
            return;
        }
        SchedulerEvent event;
        try {
            event = schedulerEventService.findSchedulerEventById(tenantId, eventId);
        } catch (Exception e) {
            log.error("[{}] Failed to find scheduler event. Retrying in a minute", eventId, e);
            md.setNextTaskFuture(scheduledExecutor.schedule(() -> processEvent(tenantId, eventId), 1, TimeUnit.MINUTES));
            return;
        }
        if (event == null) {
            log.debug("[{}] Triggered event is not present in the database.", eventId);
            eventsMetaData.remove(eventId);
            return;
        }
        try {
            triggerEvent(tenantId, event);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to trigger event", tenantId, eventId, e);
        }
        scheduleNextEvent(System.currentTimeMillis(), event, md);
    }

    private void triggerEvent(TenantId tenantId, SchedulerEvent event) throws Exception {
        JsonNode configuration = event.getConfiguration();
        EntityId originatorId = event.getOriginatorId();
        boolean isFirmwareUpdate = "updateFirmware".equals(event.getType());
        boolean isSoftwareUpdate = "updateSoftware".equals(event.getType());
        if (isFirmwareUpdate || isSoftwareUpdate) {
            OtaPackageId otaPackageId = JacksonUtil.convertValue(configuration.get("msgBody"), OtaPackageId.class);
            OtaPackageInfo otaPackageInfo = otaPackageService.findOtaPackageInfoById(tenantId, otaPackageId);
            if (otaPackageInfo == null) {
                throw new RuntimeException("Failed to process event: OtaPackage with id [" + otaPackageId + "] not found!");
            }
            switch (originatorId.getEntityType()) {
                case DEVICE -> {
                    Device device = deviceService.findDeviceById(tenantId, new DeviceId(originatorId.getId()));
                    if (device == null) {
                        throw new RuntimeException("Failed to process event: Device with id [" + originatorId + "] not found!");
                    }
                    if (isFirmwareUpdate) {
                        device.setFirmwareId(otaPackageId);
                    } else {
                        device.setSoftwareId(otaPackageId);
                    }
                    deviceService.saveDevice(device);
                }
                case DEVICE_PROFILE -> {
                    DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, new DeviceProfileId(originatorId.getId()));
                    if (deviceProfile == null) {
                        throw new RuntimeException("Failed to process event: Device profile with id [" + originatorId + "] not found!");
                    }
                    if (isFirmwareUpdate) {
                        deviceProfile.setFirmwareId(otaPackageId);
                    } else {
                        deviceProfile.setSoftwareId(otaPackageId);
                    }
                    firmwareStateService.update(deviceProfileService.saveDeviceProfile(deviceProfile), isFirmwareUpdate, isSoftwareUpdate);
                }
                default -> throw new RuntimeException("OTA update is not supported for entity type " + originatorId.getEntityType() + "!");
            }
        } else {
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(getMsgType(event, configuration))
                    .originator(originatorId)
                    .metaData(getTbMsgMetaData(event, configuration))
                    .dataType(TbMsgDataType.JSON)
                    .data(JacksonUtil.toString(configuration.get("msgBody")))
                    .build();
            log.debug("[{}][{}] Pushing scheduled message to the rule engine: {}", tenantId, originatorId, tbMsg);
            clusterService.pushMsgToRuleEngine(tenantId, originatorId, tbMsg, null);
        }
    }

    private String getMsgType(SchedulerEvent event, JsonNode configuration) {
        return configuration.has("msgType") && !configuration.get("msgType").isNull()
                ? configuration.get("msgType").asText() : event.getType();
    }

    private TbMsgMetaData getTbMsgMetaData(SchedulerEvent event, JsonNode configuration) {
        Map<String, String> metaData = new HashMap<>();
        if (configuration.has("metadata") && !configuration.get("metadata").isNull()) {
            Iterator<Map.Entry<String, JsonNode>> it = configuration.get("metadata").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> kv = it.next();
                metaData.put(kv.getKey(), kv.getValue().asText());
            }
        } else {
            if (event.getCustomerId() != null) {
                metaData.put("customerId", event.getCustomerId().getId().toString());
            }
            metaData.put("eventName", event.getName());
            if (event.getAdditionalInfo() != null) {
                metaData.put("additionalInfo", JacksonUtil.toString(event.getAdditionalInfo()));
            }
        }
        if ("sendRpcRequest".equals(event.getType())) {
            metaData.put("originServiceId", serviceId);
            if (metaData.get("timeout") != null) {
                metaData.put("expirationTime", String.valueOf(System.currentTimeMillis()
                        + Math.max(minTimeout, Long.parseLong(metaData.get("timeout")))));
            }
        }
        return new TbMsgMetaData(metaData);
    }

}
