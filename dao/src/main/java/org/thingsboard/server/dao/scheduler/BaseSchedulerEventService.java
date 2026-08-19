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
package org.thingsboard.server.dao.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventDescriptor;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventTimeFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerRepeat;
import org.thingsboard.server.common.data.scheduler.TimerRepeat;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseSchedulerEventService implements SchedulerEventService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final SchedulerEventDao schedulerEventDao;
    private final DataValidator<SchedulerEvent> schedulerEventValidator;

    @Override
    public SchedulerEvent findSchedulerEventById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventById [{}]", schedulerEventId);
        return schedulerEventDao.findById(tenantId, schedulerEventId);
    }

    @Override
    public SchedulerEventInfo findSchedulerEventInfoById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventInfoById [{}]", schedulerEventId);
        return schedulerEventDao.findInfoById(tenantId, schedulerEventId);
    }

    @Override
    public SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventWithCustomerInfoById [{}]", schedulerEventId);
        return schedulerEventDao.findWithCustomerInfoById(tenantId, schedulerEventId);
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(TenantId tenantId) {
        log.trace("Executing findSchedulerEventsByTenantId [{}]", tenantId);
        return schedulerEventDao.findInfosByTenantId(tenantId);
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndEnabled(TenantId tenantId, boolean enabled) {
        log.trace("Executing findSchedulerEventsByTenantIdAndEnabled [{}]", tenantId);
        return schedulerEventDao.findInfosByTenantIdAndEnabled(tenantId, enabled);
    }

    @Override
    public PageData<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndFilter(TenantId tenantId, SchedulerEventFilter filter, PageLink pageLink) {
        log.trace("Executing findSchedulerEventsByTenantIdAndFilter [{}][{}]", tenantId, filter);
        return schedulerEventDao.findByTenantIdAndFilter(tenantId, filter, pageLink);
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findAllSchedulerEventsByTenantIdAndEventTimeFilter(TenantId tenantId, SchedulerEventTimeFilter filter, String searchText) {
        log.trace("Executing findAllSchedulerEventsByTenantIdAndEventTimeFilter [{}][{}]", tenantId, filter);
        List<SchedulerEventWithCustomerInfo> events = schedulerEventDao
                .findByTenantIdAndFilter(tenantId, filter.toFilter(), new PageLink(Integer.MAX_VALUE, 0, searchText)).getData();
        long startTime = filter.getStartTime();
        long endTime = filter.getEndTime();
        List<SchedulerEventWithCustomerInfo> result = new ArrayList<>();
        for (SchedulerEventWithCustomerInfo event : events) {
            SchedulerEventDescriptor descriptor = event.toDescriptor();
            SchedulerRepeat repeat = descriptor.repeat();
            // High-frequency timers would produce huge timestamp lists — return them without expansion.
            if (repeat instanceof TimerRepeat timerRepeat
                    && timerRepeat.getTimeUnit().toMillis(timerRepeat.getRepeatInterval()) < TimeUnit.DAYS.toMillis(1)) {
                result.add(event);
                continue;
            }
            List<Long> timestamps = new ArrayList<>();
            long lastEventTime = startTime - 1;
            long eventTime;
            while ((eventTime = descriptor.getNextEventTime(lastEventTime)) != 0 && eventTime <= endTime) {
                timestamps.add(eventTime);
                lastEventTime = eventTime;
            }
            if (!timestamps.isEmpty()) {
                event.setTimestamps(timestamps);
                result.add(event);
            }
        }
        return result;
    }

    @Override
    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        log.trace("Executing saveSchedulerEvent [{}]", schedulerEvent);
        schedulerEventValidator.validate(schedulerEvent, SchedulerEventInfo::getTenantId);
        boolean created = schedulerEvent.getId() == null;
        SchedulerEvent saved = schedulerEventDao.save(schedulerEvent.getTenantId(), schedulerEvent);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId()).entityId(saved.getId())
                .entity(saved).created(created).broadcastEvent(true).build());
        return saved;
    }

    @Override
    public void deleteSchedulerEvent(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing deleteSchedulerEvent [{}]", schedulerEventId);
        schedulerEventDao.removeById(tenantId, schedulerEventId);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(schedulerEventId).build());
    }

    @Override
    public void deleteSchedulerEventsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteSchedulerEventsByTenantId [{}]", tenantId);
        for (UUID id : schedulerEventDao.findIdsByTenantId(tenantId)) {
            deleteSchedulerEvent(tenantId, new SchedulerEventId(id));
        }
    }

    @Override
    public void deleteSchedulerEventsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteSchedulerEventsByTenantIdAndCustomerId [{}][{}]", tenantId, customerId);
        for (UUID id : schedulerEventDao.findIdsByTenantIdAndCustomerId(tenantId, customerId.getId())) {
            deleteSchedulerEvent(tenantId, new SchedulerEventId(id));
        }
    }

}
