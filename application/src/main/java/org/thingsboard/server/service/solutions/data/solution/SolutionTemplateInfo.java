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

/** Catalogue-tile view of a template: the common fields plus preview media. */
@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SolutionTemplateInfo extends SolutionTemplate {

    @Schema(description = "Preview image URL for the catalogue tile")
    private String previewImageUrl;

    @Schema(description = "Poster image URL for the preview video")
    private String videoPreviewImageUrl;

    @Schema(description = "Preview video URL (mp4)")
    private String previewMp4Url;

    @Schema(description = "Preview video URL (webm)")
    private String previewWebmUrl;

    public SolutionTemplateInfo(SolutionDescriptor descriptor) {
        super(descriptor);
        this.previewImageUrl = descriptor.getPreviewImageUrl();
        this.videoPreviewImageUrl = descriptor.getVideoPreviewImageUrl();
        this.previewMp4Url = descriptor.getPreviewMp4Url();
        this.previewWebmUrl = descriptor.getPreviewWebmUrl();
    }

    public SolutionTemplateInfo(SolutionTemplateInfo info) {
        super(info);
        this.previewImageUrl = info.getPreviewImageUrl();
        this.videoPreviewImageUrl = info.getVideoPreviewImageUrl();
        this.previewMp4Url = info.getPreviewMp4Url();
        this.previewWebmUrl = info.getPreviewWebmUrl();
    }

}
