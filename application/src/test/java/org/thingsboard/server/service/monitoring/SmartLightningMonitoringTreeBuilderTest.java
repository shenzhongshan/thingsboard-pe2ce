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
package org.thingsboard.server.service.monitoring;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmartLightningMonitoringTreeBuilderTest {

    private final SmartLightningMonitoringTreeBuilder treeBuilder = new SmartLightningMonitoringTreeBuilder();

    @Test
    void buildsDeepAssetBranchesAndDeviceLeavesWithoutStandaloneDevices() {
        UUID parkId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        UUID cabinetId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID unrelatedDeviceId = UUID.randomUUID();
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> entities = List.of(
                new SmartLightningMonitoringTreeBuilder.VisibleEntity(parkId, "智慧园区", EntityType.ASSET),
                new SmartLightningMonitoringTreeBuilder.VisibleEntity(buildingId, "1号楼", EntityType.ASSET),
                new SmartLightningMonitoringTreeBuilder.VisibleEntity(cabinetId, "配电柜 A", EntityType.ASSET),
                new SmartLightningMonitoringTreeBuilder.VisibleEntity(deviceId, "防雷终端 A-01", EntityType.DEVICE),
                new SmartLightningMonitoringTreeBuilder.VisibleEntity(unrelatedDeviceId, "未挂载设备", EntityType.DEVICE)
        );

        var tree = treeBuilder.build(entities, List.of(
                new EntityRelation(new AssetId(parkId), new AssetId(buildingId), "Contains"),
                new EntityRelation(new AssetId(buildingId), new AssetId(cabinetId), "Contains"),
                new EntityRelation(new AssetId(cabinetId), new DeviceId(deviceId), "Contains")
        ));

        assertThat(tree.roots()).singleElement().satisfies(park -> {
            assertThat(park.name()).isEqualTo("智慧园区");
            assertThat(park.children()).singleElement().satisfies(building -> {
                assertThat(building.name()).isEqualTo("1号楼");
                assertThat(building.children()).singleElement().satisfies(cabinet -> {
                    assertThat(cabinet.name()).isEqualTo("配电柜 A");
                    assertThat(cabinet.children()).extracting(SmartLightningMonitoringTreeBuilder.TreeNode::name)
                            .containsExactly("防雷终端 A-01");
                });
            });
        });
    }

    @Test
    void ignoresRelationsToEntitiesOutsideTheVisibleScope() {
        UUID assetId = UUID.randomUUID();
        UUID hiddenDeviceId = UUID.randomUUID();
        var tree = treeBuilder.build(List.of(
                new SmartLightningMonitoringTreeBuilder.VisibleEntity(assetId, "客户可见资产", EntityType.ASSET)
        ), List.of(new EntityRelation(new AssetId(assetId), new DeviceId(hiddenDeviceId), "Contains")));

        assertThat(tree.roots()).singleElement().satisfies(asset -> assertThat(asset.children()).isEmpty());
    }
}
