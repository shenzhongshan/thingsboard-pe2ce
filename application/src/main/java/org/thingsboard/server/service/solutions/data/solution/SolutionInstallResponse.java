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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Result of an install attempt: the instructions to show, plus whether it succeeded. */
@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SolutionInstallResponse extends TenantSolutionTemplateInstructions {

    @Schema(description = "Whether the template installed successfully")
    private boolean success;

    public SolutionInstallResponse(TenantSolutionTemplateInstructions instructions, boolean success) {
        super(instructions);
        this.success = success;
    }

}
