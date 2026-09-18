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
package org.thingsboard.server.common.data.edqs.query;

import org.thingsboard.server.common.data.permission.QueryPermissions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EdqsRequest {

    private EntityDataQuery entityDataQuery;
    private EntityCountQuery entityCountQuery;
    /**
     * What the caller may see. PE ships the whole MergedUserPermissions here; CE sends only the two
     * decisions the engines need, riding the existing JSON payload of EdqsRequestMsg - no proto change.
     */
    private QueryPermissions queryPermissions;

}
