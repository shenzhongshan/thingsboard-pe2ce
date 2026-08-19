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
package org.thingsboard.server.common.data.job.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.JobType;

/** Replays one calculated field over one entity's history within [startTs, endTs]. */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class CfReprocessingTask extends Task<CfReprocessingTaskResult> {

    private CalculatedField calculatedField;
    private EntityInfo entityInfo;
    private long startTs;
    private long endTs;

    @Override
    public CfReprocessingTaskResult toFailed(Throwable error) {
        return CfReprocessingTaskResult.failed(this, error);
    }

    @Override
    public CfReprocessingTaskResult toDiscarded() {
        return CfReprocessingTaskResult.discarded(this);
    }

    @Override
    public EntityId getEntityId() {
        return entityInfo.getId();
    }

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
    }

}
