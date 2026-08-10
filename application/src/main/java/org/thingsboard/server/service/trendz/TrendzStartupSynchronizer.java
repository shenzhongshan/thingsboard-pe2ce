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
package org.thingsboard.server.service.trendz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Re-runs the handshake on boot so a Trendz that was down at the last attempt gets picked up
 * without an administrator having to press anything.
 */
@Slf4j
@Component
@TbCoreComponent
@ConditionalOnProperty(prefix = "trendz", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TrendzStartupSynchronizer {

    private final TrendzSyncService trendzSyncService;
    private final PartitionService partitionService;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void startSyncProcess() {
        // One node does the handshake; the others would only duplicate the same call to Trendz.
        if (!partitionService.isSystemPartitionMine(ServiceType.TB_CORE)) {
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("trendz-startup-sync"));
        executor.submit(() -> {
            try {
                trendzSyncService.performSyncIfNeeded();
            } catch (Exception e) {
                log.error("Failed to perform Trendz startup synchronization", e);
            } finally {
                executor.shutdown();
            }
        });
    }

}
