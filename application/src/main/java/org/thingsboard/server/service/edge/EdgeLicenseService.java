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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

/**
 * 云端代 edge 向许可门户转发请求。edge 启动时校验自己的 license，但它只认识自己的云 ——
 * 请求打到云的 /api/license/*，由云转发出去。云上没有这两个端点，PE edge 会在启动校验
 * 阶段拿到 404 直接退出。
 */
public interface EdgeLicenseService {

    ResponseEntity<JsonNode> checkInstance(JsonNode request);

    ResponseEntity<JsonNode> activateInstance(String licenseSecret, String releaseDate);

}
