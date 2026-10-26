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
package org.thingsboard.server.service.custommenu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Makes sure the three per-scope system default custom menus exist.
 *
 * <p>{@code DefaultSystemDataLoaderService} is {@code @Profile("install")}, so the install and
 * the offline-upgrade paths are covered there — but a patch release inside the same LTS family
 * ships as "replace the jar and restart", which runs neither. Without this listener such a
 * database would be missing the CUSTOMER scope default row and customer users would have no
 * inheritable menu at the end of their resolution chain.
 *
 * <p>Three indexed lookups on a normal boot, and fail-soft: a seeding problem must not keep the
 * platform from starting.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class CustomMenuDefaultsInitializer {

    private final CustomMenuService customMenuService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            customMenuService.createDefaultCustomMenus();
        } catch (Exception e) {
            log.warn("Failed to ensure the system default custom menus exist", e);
        }
    }

}
