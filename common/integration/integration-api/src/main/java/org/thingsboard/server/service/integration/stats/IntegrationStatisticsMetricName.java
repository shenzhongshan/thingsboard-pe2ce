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
package org.thingsboard.server.service.integration.stats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The metrics tracked per integration type. Names match PE so dashboards carry over. */
@Getter
@RequiredArgsConstructor
public enum IntegrationStatisticsMetricName {

    /** Integration lifecycle: counted on start, gauged as the started/failed population. */
    START("start"),
    /** Messages received from the outside world and handed to the converter. */
    MSGS_UPLINK("msgUplink"),
    /** Messages pushed back out to devices. */
    MSGS_DOWNLINK("msgDownlink");

    private final String name;

}
