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
package org.thingsboard.server.common.data.trendz;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Per-tenant pointer to a Trendz instance, used to embed Trendz views in dashboards.
 * <p>
 * Distinct from {@link TrendzIntegrationSettings}, which is the system-wide handshake state.
 */
@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendzSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Whether Trendz widgets are enabled for this tenant.")
    private boolean enabled;

    @Schema(description = "Base URL of the Trendz instance.", example = "https://trendz.domain.com")
    private String baseUrl;

    @Schema(description = "API key used by the Trendz widgets.")
    private String apiKey;

}
