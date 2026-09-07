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
package org.thingsboard.server.common.data.kv;

import java.io.Serializable;
import java.util.UUID;

/**
 * 属性值与所属实体 ID 的组合结果。
 *
 * <p>普通 {@link AttributeKvEntry} 只携带 key/value，不携带 entityId，
 * 因此多设备批量查询时必须使用这个结构保留设备归属。</p>
 */
public record EntityAttributeKvEntry(UUID entityId, AttributeKvEntry attribute) implements Serializable {
}
