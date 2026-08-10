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

import java.io.Serializable;

/**
 * System-wide state of the Trendz integration: where Trendz is and how the last handshake went.
 * <p>
 * Separate from the per-tenant {@link TrendzSettings}, which only points dashboard widgets at a
 * Trendz instance. This one is what the connect/healthcheck flow reads and writes.
 */
public record TrendzIntegrationSettings(TrendzConfiguration configuration,
                                        TrendzSynchronizationResult synchronizationResult) implements Serializable {
}
