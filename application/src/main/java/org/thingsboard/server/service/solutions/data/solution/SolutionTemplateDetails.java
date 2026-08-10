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

import java.util.List;

/** Details-page view: screenshots plus the markdown bodies shipped with the template. */
@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SolutionTemplateDetails extends SolutionTemplate {

    @Schema(description = "Screenshot URLs")
    private List<String> imageUrls;

    @Schema(description = "Rendered content of highlights.md")
    private String highlights;

    @Schema(description = "Rendered content of description.md")
    private String description;

    public SolutionTemplateDetails(SolutionDescriptor descriptor, String highlights, String description) {
        super(descriptor);
        this.imageUrls = descriptor.getImageUrls();
        this.highlights = highlights;
        this.description = description;
    }

    public SolutionTemplateDetails(SolutionTemplateDetails details) {
        super(details);
        this.imageUrls = details.getImageUrls();
        this.highlights = details.getHighlights();
        this.description = details.getDescription();
    }

}
