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
package org.thingsboard.server.controller;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * edge_license_key 是凭据：没有 EDGE CREATE/WRITE 的用户读任何 edge 接口都不该看到它。
 *
 * <p>这几个重载早先写成了 {@code return hideLicenseKeyIfNeeded(edges);} 的自我递归 ——
 * 泛型重载让编译器完全看不出问题，运行时每个列表/分页端点直接 StackOverflowError。
 * 抹除逻辑因此被拆成不含权限判断的静态方法，好让用例不用 Spring 上下文就能直接调。
 */
class EdgeLicenseKeyHidingTest {

    private static Edge edgeWithKey(String name, String key) {
        Edge edge = new Edge();
        edge.setName(name);
        edge.setEdgeLicenseKey(key);
        return edge;
    }

    @Test
    void hidesKeyOnEverySingleElement() {
        // 两条而非一条：一条过不了"只清了第一条"这种写法。
        List<Edge> edges = new ArrayList<>(List.of(edgeWithKey("a", "KEY-A"), edgeWithKey("b", "KEY-B")));
        List<Edge> result = EdgeController.hideLicenseKeys(edges, true);
        assertSame(edges, result);
        edges.forEach(edge -> assertNull(edge.getEdgeLicenseKey(), edge.getName() + " 的 license key 没被抹掉"));
    }

    @Test
    void keepsKeyWhenCallerMayWrite() {
        List<Edge> edges = new ArrayList<>(List.of(edgeWithKey("a", "KEY-A"), edgeWithKey("b", "KEY-B")));
        EdgeController.hideLicenseKeys(edges, false);
        assertEquals("KEY-A", edges.get(0).getEdgeLicenseKey());
        assertEquals("KEY-B", edges.get(1).getEdgeLicenseKey());
    }

    @Test
    void handlesSingleEdgeAndNulls() {
        Edge edge = edgeWithKey("a", "KEY-A");
        assertNull(EdgeController.hideLicenseKeys(edge, true).getEdgeLicenseKey());
        assertEquals("KEY-A", EdgeController.hideLicenseKeys(edgeWithKey("b", "KEY-A"), false).getEdgeLicenseKey());
        assertNull(EdgeController.hideLicenseKeys((Edge) null, true));
        assertNull(EdgeController.hideLicenseKeys((List<Edge>) null, true));
    }

    @Test
    void appliesToEdgeInfoSubclassToo() {
        // 列表端点返回的是 EdgeInfo，它继承了同一个字段 —— 泛型边界必须覆盖子类。
        EdgeInfo info = new EdgeInfo();
        info.setEdgeLicenseKey("KEY-A");
        List<EdgeInfo> infos = new ArrayList<>(List.of(info));
        EdgeController.hideLicenseKeys(infos, true);
        assertNull(infos.get(0).getEdgeLicenseKey());
    }
}
