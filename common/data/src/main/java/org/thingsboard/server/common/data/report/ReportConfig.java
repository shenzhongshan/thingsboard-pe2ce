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

import lombok.Data;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;
import java.util.UUID;

/**
 * Report-generation request payload, used by both the scheduler ("generateReport" event type) and
 * the "generate report" rule node. If {@code targets} + {@code notificationTemplateId} are set,
 * a completion notification is fired via the existing NotificationCenter once the report is ready.
 */
@Data
public class ReportConfig {

    private ReportTemplateId reportTemplateId;
    private UserId userId;
    private String timezone;
    private List<UUID> targets;
    private NotificationTemplateId notificationTemplateId;

}
