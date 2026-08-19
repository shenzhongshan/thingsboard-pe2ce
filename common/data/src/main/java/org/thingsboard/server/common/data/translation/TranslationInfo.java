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
package org.thingsboard.server.common.data.translation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Schema
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationInfo implements Serializable {

    @Schema(description = "Locale code formed by combining the ISO 639-1 language code and the ISO 3166-1 region code. For example, \"en_US\"")
    private String localeCode;

    @Schema(description = "Locale code language display name. For example, \"Polish (Polski)\"")
    private String language;

    @Schema(description = "Locale code country display name. For example, \"Poland\"")
    private String country;

    @Schema(description = "Number representing translation percentage progress. For example, 40 that means 40% of all keys are translated.")
    private int progress;

    @Schema(description = "Boolean representing if current language has customization.")
    private boolean customized;

}
