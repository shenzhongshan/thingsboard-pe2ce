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
package org.thingsboard.server.service.entitiy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.scheduler.SchedulerService;

/**
 * CE simplification vs PE: notifies the runtime scheduler with direct in-JVM calls (monolith)
 * instead of queue protos, and skips audit logging (SchedulerEvent has no CE EntityType).
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbSchedulerService implements TbSchedulerService {

    private final SchedulerEventService schedulerEventService;
    private final SchedulerService schedulerService;

    @Override
    public SchedulerEvent save(SchedulerEvent schedulerEvent, User user) throws ThingsboardException {
        boolean created = schedulerEvent.getId() == null;
        SchedulerEvent saved = schedulerEventService.saveSchedulerEvent(schedulerEvent);
        if (created) {
            schedulerService.onSchedulerEventAdded(saved);
        } else {
            schedulerService.onSchedulerEventUpdated(saved);
        }
        return saved;
    }

    @Override
    public void delete(SchedulerEvent schedulerEvent, User user) throws ThingsboardException {
        schedulerEventService.deleteSchedulerEvent(user.getTenantId(), schedulerEvent.getId());
        schedulerService.onSchedulerEventDeleted(schedulerEvent);
    }

}
