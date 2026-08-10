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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.page.PageData;

import java.io.Serializable;
import java.util.List;

/** Trendz's own page envelope, converted to ThingsBoard's {@link PageData} at the boundary. */
public record TrendzPaginationData<T>(@JsonProperty("content") List<T> content,
                                     @JsonProperty("pageNumber") int pageNumber,
                                     @JsonProperty("totalPages") int totalPages,
                                     @JsonProperty("totalElements") long totalElements) implements Serializable {

    public PageData<T> toPageData() {
        // Trendz numbers pages from 0, so the last page is the one whose index+1 equals the total.
        return new PageData<>(content, totalPages, totalElements, pageNumber + 1 != totalPages);
    }

}
