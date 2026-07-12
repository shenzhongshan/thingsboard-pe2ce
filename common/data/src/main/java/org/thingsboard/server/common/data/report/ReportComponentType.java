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
package org.thingsboard.server.common.data.report;

/**
 * CE port of PE's report component type enum. Unlike PE (which backs each type with its own
 * strongly-typed configuration POJO across ~260 classes), CE stores component configuration as
 * opaque JSON (see {@link ReportTemplate#getConfiguration()}) and the rendering engine switches on
 * this type to interpret the relevant fields — same simplification pattern as Role.permissions /
 * EntityGroup.configuration elsewhere in this port.
 */
public enum ReportComponentType {
    HEADING,
    RICH_TEXT,
    ENTITY_TABLE,
    TIME_SERIES_TABLE,
    ALARM_TABLE,
    TIME_SERIES_CHART,
    LATEST_CHART,
    DASHBOARD,
    IMAGE,
    SUB_REPORT,
    PAGE_BREAK,
    DIVIDER
}
