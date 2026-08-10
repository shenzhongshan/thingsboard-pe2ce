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
package org.thingsboard.server.service.solutions.data.solution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;

/**
 * Post-install instructions: the markdown body plus pointers to what was created, so the UI can
 * link straight to the solution's main dashboard.
 */
@Schema
@Data
@NoArgsConstructor
public class TenantSolutionTemplateInstructions {

    @Schema(description = "Entity group holding the installed dashboards")
    private EntityGroupId dashboardGroupId;

    @Schema(description = "Main dashboard of the solution")
    private DashboardId dashboardId;

    @Schema(description = "Public customer id, when the main dashboard was made public")
    private CustomerId publicId;

    @Schema(description = "Whether the main dashboard is public")
    private boolean mainDashboardPublic;

    @Schema(description = "Rendered content of instructions.md")
    private String details;

    public TenantSolutionTemplateInstructions(TenantSolutionTemplateInstructions other) {
        this.dashboardGroupId = other.getDashboardGroupId();
        this.dashboardId = other.getDashboardId();
        this.publicId = other.getPublicId();
        this.mainDashboardPublic = other.isMainDashboardPublic();
        this.details = other.getDetails();
    }

}
