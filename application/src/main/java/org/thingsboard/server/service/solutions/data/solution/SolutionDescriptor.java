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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One entry of solutions.json — the on-disk descriptor, carrying everything needed to render
 * both the catalogue tile and the details page.
 */
@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolutionDescriptor extends SolutionTemplate {

    @Schema(description = "Screenshot URLs shown on the details page")
    private List<String> imageUrls;

    @Schema(description = "Preview image URL for the catalogue tile")
    private String previewImageUrl;

    @Schema(description = "Poster image URL for the preview video")
    private String videoPreviewImageUrl;

    @Schema(description = "Preview video URL (mp4)")
    private String previewMp4Url;

    @Schema(description = "Preview video URL (webm)")
    private String previewWebmUrl;

}
