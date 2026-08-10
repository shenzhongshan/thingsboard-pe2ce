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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Common fields of a solution template, as declared in data/json/solutions/solutions.json. */
@Schema
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolutionTemplate {

    @Schema(description = "ID of the solution template", example = "smart_office")
    private String id;

    @Schema(description = "Template title", example = "Smart office")
    private String title;

    @Schema(description = "Subscription level the template is published under", example = "PROTOTYPE")
    private SolutionTemplateLevel level;

    @Schema(description = "How long the installation UI should wait while the template installs")
    private long installTimeoutMs;

    @Schema(description = "Tenant telemetry keys to delete on uninstall")
    private List<String> tenantTelemetryKeys;

    @Schema(description = "Tenant attribute keys to delete on uninstall")
    private List<String> tenantAttributeKeys;

    public SolutionTemplate(SolutionTemplate solution) {
        this.id = solution.getId();
        this.title = solution.getTitle();
        this.level = solution.getLevel();
        this.installTimeoutMs = solution.getInstallTimeoutMs();
        this.tenantTelemetryKeys = solution.getTenantTelemetryKeys();
        this.tenantAttributeKeys = solution.getTenantAttributeKeys();
    }

}
