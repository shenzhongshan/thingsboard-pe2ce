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
package org.thingsboard.server.service.edge;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.edge.EdgeEventType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * EdgeEventType 是 processor 的分派键：EdgeContextComponent.getProcessor 在查不到时
 * 直接抛 UnsupportedOperationException。写了一个 fetcher 却忘了配套的 processor，
 * 编译完全通过 —— 直到某台 edge 真的同步到那条事件，整轮同步才炸掉。
 *
 * <p>这个用例读源文件对账：所有 fetcher 会产出的 EdgeEventType，必须都有
 * processor 认领（含 EdgeContextComponent 里手工登记的白标别名）。
 */
class EdgeProcessorCoverageTest {

    private static final Path EDGE_ROOT = Path.of("src/main/java/org/thingsboard/server/service/edge");
    private static final Path FETCH_DIR = EDGE_ROOT.resolve("rpc/fetch");
    private static final Path PROCESSOR_DIR = EDGE_ROOT.resolve("rpc/processor");
    private static final Path CONTEXT = EDGE_ROOT.resolve("EdgeContextComponent.java");

    private static final Pattern EVENT_TYPE = Pattern.compile("EdgeEventType\\.([A-Z_]+)\\b");
    /** processor 认领事件类型的两种方式：getEdgeEventType 的返回值，或在 context 里手工 put。 */
    private static final Pattern DECLARED = Pattern.compile("getEdgeEventType\\(\\)\\s*\\{\\s*return\\s+EdgeEventType\\.([A-Z_]+)\\s*;");
    private static final Pattern REGISTERED = Pattern.compile("processorMap\\.put\\(EdgeEventType\\.([A-Z_]+)\\s*,");

    @Test
    void everyEventTypeProducedByAFetcherHasAProcessor() throws IOException {
        Set<String> produced = scan(FETCH_DIR, EVENT_TYPE);
        Set<String> handled = new TreeSet<>(scan(PROCESSOR_DIR, DECLARED));
        handled.addAll(matches(Files.readString(CONTEXT, StandardCharsets.UTF_8), REGISTERED));

        // 用例本身失效的保护：目录挪走或正则失配会让两个集合同时空掉，那时断言会假通过。
        assertFalse(produced.isEmpty(), "没从 " + FETCH_DIR + " 里扫到任何 EdgeEventType，用例已失效");
        assertFalse(handled.isEmpty(), "没从 " + PROCESSOR_DIR + " 里扫到任何 processor，用例已失效");

        List<String> missing = new ArrayList<>(produced);
        missing.removeAll(handled);
        if (!missing.isEmpty()) {
            fail("这些事件类型有 fetcher 产出但没有 processor 认领，同步到它们时会抛 "
                    + "UnsupportedOperationException:\n  " + String.join("\n  ", missing));
        }
    }

    @Test
    void everyDeclaredEventTypeNameIsARealEnumConstant() throws IOException {
        Set<String> names = new TreeSet<>(scan(FETCH_DIR, EVENT_TYPE));
        names.addAll(scan(PROCESSOR_DIR, DECLARED));
        List<String> unknown = new ArrayList<>();
        for (String name : names) {
            try {
                EdgeEventType.valueOf(name);
            } catch (IllegalArgumentException e) {
                unknown.add(name);
            }
        }
        if (!unknown.isEmpty()) {
            fail("源码里引用了不存在的 EdgeEventType: " + unknown);
        }
    }

    private Set<String> scan(Path dir, Pattern pattern) throws IOException {
        Set<String> result = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                result.addAll(matches(Files.readString(path, StandardCharsets.UTF_8), pattern));
            }
        }
        return result;
    }

    private Set<String> matches(String source, Pattern pattern) {
        Set<String> result = new TreeSet<>();
        Matcher m = pattern.matcher(source);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }
}
