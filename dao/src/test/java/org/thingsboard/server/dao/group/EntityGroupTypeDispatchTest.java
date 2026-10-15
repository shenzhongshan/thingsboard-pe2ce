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
package org.thingsboard.server.dao.group;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * EntityGroup.groupTypes 是一个分派键：BaseEntityGroupService 里有若干
 * {@code switch (type) { ... default -> throw }}。往 groupTypes 里加一个类型
 * 而忘了加 case，编译完全通过，只有在运行到那个分组时才抛
 * "Unsupported entity group type!"。
 *
 * 这个用例直接读源文件，断言每个 groupType 在每个分派 switch 里都有对应的 case。
 * 不优雅，但它能在编译期之后立刻发现遗漏，而不是等到线上点开某个分组。
 */
class EntityGroupTypeDispatchTest {

    private static final Path SERVICE = Path.of("src/main/java/org/thingsboard/server/dao/group/BaseEntityGroupService.java");

    /** 这些方法按 group type 分派且以 default -> throw 收尾。 */
    private static final List<String> DISPATCH_METHODS = List.of(
            "findAllTenantEntities", "findAllCustomerEntities");

    @Test
    void everyGroupTypeIsHandledByEveryDispatchSwitch() throws IOException {
        String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
        List<String> problems = new ArrayList<>();

        for (String method : DISPATCH_METHODS) {
            String body = methodBody(source, method);
            for (EntityType type : EntityGroup.groupTypes) {
                if (!Pattern.compile("case\\s+" + type.name() + "\\b").matcher(body).find()) {
                    problems.add(method + " 缺少 case " + type.name());
                }
            }
        }
        if (!problems.isEmpty()) {
            fail("groupTypes 与分派 switch 不同步:\n  " + String.join("\n  ", problems));
        }
    }

    @Test
    void edgeIsAGroupType() {
        // PE 4.3.1.1 允许把 edge 实例本身放进实体分组。
        assertTrue(Arrays.asList(EntityGroup.groupTypes).contains(EntityType.EDGE));
    }

    private String methodBody(String source, String methodName) {
        Matcher m = Pattern.compile("\\b" + methodName + "\\s*\\([^)]*\\)\\s*\\{").matcher(source);
        if (!m.find()) {
            fail("在 " + SERVICE + " 里找不到方法 " + methodName + "，用例本身已失效，请更新 DISPATCH_METHODS");
        }
        int depth = 0;
        int i = m.end() - 1;
        int start = i;
        do {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        } while (depth > 0 && i < source.length());
        return source.substring(start, i);
    }
}
