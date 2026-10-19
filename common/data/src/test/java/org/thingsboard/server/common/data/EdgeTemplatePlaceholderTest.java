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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code ${{key}}} 是 edge 模板的参数化机制 —— 一份集成模板下发到 N 台 edge，每台用自己的
 * SERVER_SCOPE 属性替换占位符。这层全是正则转义，最容易写错且错了不报错（只是替换不生效，
 * edge 拿到的是字面量 "${{brokerHost}}" 去连接），所以把它钉住。
 *
 * <p>注意是**双大括号**：规则引擎用的是单括号 {@code ${}}，两者不能混。
 */
class EdgeTemplatePlaceholderTest {

    @Test
    void extractsKeysFromConfiguration() {
        String config = "{\"clientConfiguration\":{\"host\":\"${{brokerHost}}\",\"port\":\"${{brokerPort}}\"}}";
        assertEquals(Set.of("brokerHost", "brokerPort"), EdgeUtils.getAttributeKeysFromConfiguration(config));
    }

    @Test
    void handlesConfigurationWithoutPlaceholders() {
        assertTrue(EdgeUtils.getAttributeKeysFromConfiguration("{\"host\":\"broker.example.com\"}").isEmpty());
        assertTrue(EdgeUtils.getAttributeKeysFromConfiguration(null).isEmpty());
    }

    @Test
    void singleBracePlaceholdersAreNotTouched() {
        // 规则引擎的 ${host} 不属于这套机制，误识别会把规则引擎的变量在下发时提前替换掉。
        assertTrue(EdgeUtils.getAttributeKeysFromConfiguration("{\"host\":\"${host}\"}").isEmpty());
    }

    @Test
    void placeholderRoundTripsThroughReplacement() {
        // 端到端：生成占位符 -> 提取 key -> 用正则形式替换掉。任何一处转义写错这里就挂。
        String placeholder = EdgeUtils.formatAttributeKeyToPlaceholderFormat("brokerHost");
        assertEquals("${{brokerHost}}", placeholder);

        String config = "{\"host\":\"" + placeholder + "\"}";
        Set<String> keys = EdgeUtils.getAttributeKeysFromConfiguration(config);
        assertEquals(Set.of("brokerHost"), keys);

        String replaced = config.replaceAll(
                EdgeUtils.formatAttributeKeyToRegexpPlaceholderFormat("brokerHost"), "10.0.0.1");
        assertEquals("{\"host\":\"10.0.0.1\"}", replaced);
    }

    @Test
    void replacementLeavesOtherPlaceholdersAlone() {
        String config = "{\"host\":\"${{brokerHost}}\",\"port\":\"${{brokerPort}}\"}";
        String replaced = config.replaceAll(
                EdgeUtils.formatAttributeKeyToRegexpPlaceholderFormat("brokerHost"), "10.0.0.1");
        assertEquals("{\"host\":\"10.0.0.1\",\"port\":\"${{brokerPort}}\"}", replaced);
    }
}
