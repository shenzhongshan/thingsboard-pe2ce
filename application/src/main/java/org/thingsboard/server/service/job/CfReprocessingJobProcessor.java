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
package org.thingsboard.server.service.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.CfReprocessingJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.CfReprocessingTask;
import org.thingsboard.server.common.data.job.task.CfReprocessingTaskResult;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fans a calculated field reprocessing job out into one task per target entity: the CF entity itself when it is
 * bound to a single device/asset, or every device/asset of the profile when it is bound to a profile.
 */
@Component
@RequiredArgsConstructor
public class CfReprocessingJobProcessor implements JobProcessor {

    private static final int ENTITY_PACK_SIZE = 512;

    private final CalculatedFieldService calculatedFieldService;
    private final DeviceService deviceService;
    private final AssetService assetService;

    @Override
    public int process(Job job, Consumer<Task<?>> taskConsumer) throws Exception {
        CfReprocessingJobConfiguration configuration = job.getConfiguration();
        TenantId tenantId = job.getTenantId();
        CalculatedField calculatedField = calculatedFieldService.findById(tenantId, configuration.getCalculatedFieldId());
        EntityId cfEntityId = calculatedField.getEntityId();

        int tasksCount = 0;
        for (EntityInfo entityInfo : findTargetEntities(tenantId, cfEntityId)) {
            taskConsumer.accept(createTask(job, configuration, calculatedField, entityInfo));
            tasksCount++;
        }
        return tasksCount;
    }

    private Iterable<EntityInfo> findTargetEntities(TenantId tenantId, EntityId cfEntityId) {
        return switch (cfEntityId.getEntityType()) {
            case DEVICE -> List.of(deviceService.findDeviceEntityInfoById(tenantId, (DeviceId) cfEntityId));
            case ASSET -> List.of(assetService.findAssetEntityInfoById(tenantId, (AssetId) cfEntityId));
            case DEVICE_PROFILE -> new PageDataIterable<>(pageLink ->
                    deviceService.findDeviceEntityInfosByTenantIdAndDeviceProfileId(tenantId, (DeviceProfileId) cfEntityId, pageLink), ENTITY_PACK_SIZE);
            case ASSET_PROFILE -> new PageDataIterable<>(pageLink ->
                    assetService.findAssetEntityInfosByTenantIdAndAssetProfileId(tenantId, (AssetProfileId) cfEntityId, pageLink), ENTITY_PACK_SIZE);
            default -> throw new IllegalArgumentException("Unsupported CF entity type " + cfEntityId.getEntityType());
        };
    }

    @Override
    public void reprocess(Job job, List<TaskResult> taskFailures, Consumer<Task<?>> taskConsumer) throws Exception {
        CfReprocessingJobConfiguration configuration = job.getConfiguration();
        CalculatedField calculatedField = calculatedFieldService.findById(job.getTenantId(), configuration.getCalculatedFieldId());
        for (TaskResult taskFailure : taskFailures) {
            EntityInfo entityInfo = ((CfReprocessingTaskResult) taskFailure).getFailure().getEntityInfo();
            taskConsumer.accept(createTask(job, configuration, calculatedField, entityInfo));
        }
    }

    private CfReprocessingTask createTask(Job job, CfReprocessingJobConfiguration configuration, CalculatedField calculatedField, EntityInfo entityInfo) {
        return CfReprocessingTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .key(configuration.getTasksKey())
                .retries(0)
                .calculatedField(calculatedField)
                .entityInfo(entityInfo)
                .startTs(configuration.getStartTs())
                .endTs(configuration.getEndTs())
                .build();
    }

    @Override
    public JobType getType() {
        return JobType.CF_REPROCESSING;
    }

}
