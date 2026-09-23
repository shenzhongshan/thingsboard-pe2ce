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
package org.thingsboard.server.common.data;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.group.EntityGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "[Edge] X All" 是 PE 为每台 edge 维护的镜像分组。名字是这套机制的唯一标识 ——
 * 生成端（getEdgeGroupAllName）和识别端（isEdgeGroupAll）必须严格互逆：
 *
 * <ul>
 *   <li>识别不出来 → 事件溯源不会跳过它，镜像分组被推回 edge 形成循环；</li>
 *   <li>生成规则变了而旧组没改名 → findOrCreateEdgeAllGroup 找不到旧组会再建一个，
 *       同一台 edge 出现两个镜像分组。</li>
 * </ul>
 */
class EdgeGroupAllNamingTest {

    @Test
    void tenantOwnedNameRoundTrips() {
        String name = EdgeUtils.getEdgeGroupAllName("MyEdge");
        assertEquals("[Edge] MyEdge All", name);
        assertTrue(EdgeUtils.isEdgeGroupAll(name));
    }

    @Test
    void customerOwnedNameRoundTrips() {
        String name = EdgeUtils.getEdgeGroupAllName("Acme", "MyEdge");
        assertEquals("[Edge][Acme] MyEdge All", name);
        assertTrue(EdgeUtils.isEdgeGroupAll(name));
    }

    @Test
    void entityGroupReportsItself() {
        EntityGroup group = new EntityGroup();
        group.setName(EdgeUtils.getEdgeGroupAllName("MyEdge"));
        assertTrue(group.isEdgeGroupAll());
        assertFalse(group.isGroupAll(), "镜像分组不是普通的 'All' 组，两者的跳过语义不同");
    }

    @Test
    void ordinaryGroupsAreNotMistakenForEdgeMirrors() {
        assertFalse(EdgeUtils.isEdgeGroupAll(null));
        assertFalse(EdgeUtils.isEdgeGroupAll(EntityGroup.GROUP_ALL_NAME));
        assertFalse(EdgeUtils.isEdgeGroupAll("Temperature sensors"));
        // 只占一头都不算
        assertFalse(EdgeUtils.isEdgeGroupAll("[Edge] MyEdge sensors"));
        assertFalse(EdgeUtils.isEdgeGroupAll("MyEdge All"));
    }

    @Test
    void plainAllGroupIsNotAnEdgeMirror() {
        EntityGroup group = new EntityGroup();
        group.setName(EntityGroup.GROUP_ALL_NAME);
        assertTrue(group.isGroupAll());
        assertFalse(group.isEdgeGroupAll());
    }
}
