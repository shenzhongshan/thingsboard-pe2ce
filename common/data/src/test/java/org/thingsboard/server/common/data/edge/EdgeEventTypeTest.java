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
package org.thingsboard.server.common.data.edge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * allEdgesRelated 决定 BaseEdgeProcessor.processEntityNotification 走哪条分支：
 * 为 true 时走 processEntityNotificationForAllEdges，**丢弃 edgeId 和 entityGroupId**，
 * 把事件广播给租户下所有 edge。
 *
 * 所以对"按 edge 逐个分配"的实体类型，这个标志必须是 false，否则
 * "把设备组分配给 1 台 edge" 会静默变成 "推给所有 edge" —— 编译和运行都不报错。
 * 这个类曾经就标错过（commit 3dcd1ed2eb 把四个类型都标成了 true）。
 */
class EdgeEventTypeTest {

    /** 这些类型在 PE 4.3.1.1 里都是 allEdgesRelated=false，对齐之。 */
    enum PerEdgeAssigned {
        ENTITY_GROUP, CONVERTER, INTEGRATION, SCHEDULER_EVENT,
        DEVICE, ASSET, ENTITY_VIEW, DASHBOARD, RULE_CHAIN
    }

    @ParameterizedTest
    @EnumSource(PerEdgeAssigned.class)
    void perEdgeAssignedTypesMustNotBeAllEdgesRelated(PerEdgeAssigned type) {
        EdgeEventType edgeEventType = EdgeEventType.valueOf(type.name());
        assertFalse(edgeEventType.isAllEdgesRelated(),
                edgeEventType + " is assigned per edge; marking it allEdgesRelated would discard "
                        + "edgeId/entityGroupId and broadcast to every edge of the tenant");
    }

    @Test
    void entityGroupIsResolvableFromEntityType() {
        // EdgeUtils 的映射表是从 EdgeEventType.values() 动态构建的；ENTITY_GROUP 的
        // entityType 若为 null，sendNotificationMsgToEdge 会因 type==null 静默丢弃通知。
        assertNotNull(EdgeEventType.ENTITY_GROUP.getEntityType());
    }
}
