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
package org.thingsboard.server.service.job.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.CfReprocessingTask;
import org.thingsboard.server.common.data.job.task.CfReprocessingTaskResult;
import org.thingsboard.server.queue.task.TaskProcessor;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.CalculatedFieldReprocessingService;

/**
 * Runs on rule engine nodes: registering this bean is what advertises CF_REPROCESSING support in ServiceInfo,
 * so that the job manager routes reprocessing tasks here.
 */
@Component
@TbRuleEngineComponent
public class CfReprocessingTaskProcessor extends TaskProcessor<CfReprocessingTask, CfReprocessingTaskResult> {

    @Autowired
    @Lazy
    private CalculatedFieldReprocessingService cfReprocessingService;

    @Value("${queue.calculated_fields.reprocessing_timeout:300000}")
    private int timeoutMs;

    @Override
    public CfReprocessingTaskResult process(CfReprocessingTask task) throws Exception {
        cfReprocessingService.reprocess(task);
        return CfReprocessingTaskResult.success(task);
    }

    @Override
    public long getProcessingTimeout(CfReprocessingTask task) {
        return timeoutMs;
    }

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
    }

}
