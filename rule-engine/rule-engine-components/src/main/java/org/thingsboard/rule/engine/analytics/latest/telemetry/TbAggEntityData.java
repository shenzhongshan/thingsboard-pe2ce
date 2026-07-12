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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class TbAggEntityData {

    private final EntityId entityId;
    private final Map<String, AttributeKvEntry> clientAttributes = new HashMap<>();
    private final Map<String, AttributeKvEntry> serverAttributes = new HashMap<>();
    private final Map<String, AttributeKvEntry> sharedAttributes = new HashMap<>();
    private final Map<String, TsKvEntry> latestTs = new HashMap<>();
    private final Map<String, KvEntry> filterMap = new HashMap<>();

    @Setter
    private volatile ListenableFuture<List<TsKvEntry>> tsFuture;
    @Setter
    private volatile ListenableFuture<List<AttributeKvEntry>> clientAttributesFuture;
    @Setter
    private volatile ListenableFuture<List<AttributeKvEntry>> sharedAttributesFuture;
    @Setter
    private volatile ListenableFuture<List<AttributeKvEntry>> serverAttributesFuture;

    public TbAggEntityData(EntityId entityId) {
        this.entityId = entityId;
    }

    public void prepare() {
        putToMap(latestTs, tsFuture);
        putToMap(clientAttributes, clientAttributesFuture);
        putToMap(serverAttributes, serverAttributesFuture);
        putToMap(sharedAttributes, sharedAttributesFuture);
        putToMap(filterMap, clientAttributesFuture, null);
        putToMap(filterMap, sharedAttributesFuture, null);
        putToMap(filterMap, serverAttributesFuture, null);
        putToMap(filterMap, tsFuture, null);
        putToMap(filterMap, clientAttributesFuture, "cs_");
        putToMap(filterMap, sharedAttributesFuture, "shared_");
        putToMap(filterMap, serverAttributesFuture, "ss_");
    }

    private static <T extends KvEntry> void putToMap(Map<String, T> map, ListenableFuture<List<T>> future) {
        if (future == null) {
            return;
        }
        try {
            List<T> kvEntries = future.get();
            if (kvEntries != null) {
                kvEntries.forEach(e -> map.put(e.getKey(), e));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends KvEntry> void putToMap(Map<String, KvEntry> map, ListenableFuture<List<T>> future, String prefix) {
        if (future == null) {
            return;
        }
        try {
            List<T> kvEntries = future.get();
            if (kvEntries != null) {
                kvEntries.forEach(e -> map.put(StringUtils.isNotBlank(prefix) ? prefix + e.getKey() : e.getKey(), e));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TsKvEntry getLatestTs(String source) {
        return latestTs.get(source);
    }

    public Optional<KvEntry> getValue(String sourceScope, String source) {
        return Optional.ofNullable(switch (sourceScope) {
            case "LATEST_TELEMETRY" -> latestTs.get(source);
            case "CLIENT_SCOPE" -> clientAttributes.get(source);
            case "SHARED_SCOPE" -> sharedAttributes.get(source);
            case "SERVER_SCOPE" -> serverAttributes.get(source);
            default -> null;
        });
    }

}
