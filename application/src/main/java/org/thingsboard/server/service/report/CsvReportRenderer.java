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
package org.thingsboard.server.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.report.ReportComponentType;
import org.thingsboard.server.common.data.report.ReportTemplate;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV export: only the table-like component types (TIME_SERIES_TABLE, ALARM_TABLE, ENTITY_TABLE)
 * make sense as CSV rows; other component types (headings, charts, images, dashboards) are skipped
 * — matches PE's own CSV format restriction (its CsvReportService only supports table/sub-report
 * components).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvReportRenderer {

    private final ReportDataFetcher dataFetcher;

    public byte[] render(TenantId tenantId, ReportTemplate template) throws Exception {
        JsonNode config = template.getConfiguration();
        long now = System.currentTimeMillis();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), CSVFormat.DEFAULT)) {
            JsonNode components = config.path("components");
            if (components.isArray()) {
                for (JsonNode component : components) {
                    renderComponent(tenantId, component, now, printer);
                }
            }
        }
        return out.toByteArray();
    }

    private void renderComponent(TenantId tenantId, JsonNode component, long now, CSVPrinter printer) throws Exception {
        String typeStr = component.path("type").asText(null);
        if (typeStr == null) {
            return;
        }
        ReportComponentType type;
        try {
            type = ReportComponentType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return;
        }
        switch (type) {
            case TIME_SERIES_TABLE -> printTimeSeries(tenantId, component, now, printer);
            case ALARM_TABLE -> printAlarms(tenantId, component, now, printer);
            case ENTITY_TABLE -> printAttributes(tenantId, component, printer);
            default -> log.debug("Component type {} is not representable as CSV, skipping", type);
        }
    }

    private EntityId entityIdOf(JsonNode component) {
        JsonNode ref = component.path("entityId");
        return EntityIdFactory.getByTypeAndUuid(ref.path("entityType").asText(), ref.path("id").asText());
    }

    private List<String> keysOf(JsonNode component) {
        List<String> keys = new ArrayList<>();
        component.path("keys").forEach(k -> keys.add(k.asText()));
        return keys;
    }

    private long[] timeRangeOf(JsonNode component, long now) {
        if (component.hasNonNull("lastMs")) {
            long lastMs = component.path("lastMs").asLong();
            return new long[]{now - lastMs, now};
        }
        return new long[]{component.path("startTs").asLong(now - 86400000L), component.path("endTs").asLong(now)};
    }

    private void printTimeSeries(TenantId tenantId, JsonNode component, long now, CSVPrinter printer) throws Exception {
        EntityId entityId = entityIdOf(component);
        long[] range = timeRangeOf(component, now);
        printer.printRecord("Entity", "Time", "Key", "Value");
        String entityName = dataFetcher.entityName(tenantId, entityId);
        for (TsKvEntry entry : dataFetcher.fetchTimeseries(tenantId, entityId, keysOf(component), range[0], range[1])) {
            printer.printRecord(entityName, entry.getTs(), entry.getKey(), entry.getValueAsString());
        }
    }

    private void printAlarms(TenantId tenantId, JsonNode component, long now, CSVPrinter printer) {
        EntityId entityId = entityIdOf(component);
        long[] range = timeRangeOf(component, now);
        try {
            printer.printRecord("Entity", "Created", "Type", "Severity", "Status");
            String entityName = dataFetcher.entityName(tenantId, entityId);
            for (AlarmInfo alarm : dataFetcher.fetchAlarms(tenantId, entityId, range[0], range[1])) {
                String status = alarm.isCleared() ? "CLEARED" : (alarm.isAcknowledged() ? "ACK" : "ACTIVE");
                printer.printRecord(entityName, alarm.getCreatedTime(), alarm.getType(), alarm.getSeverity(), status);
            }
        } catch (Exception e) {
            log.warn("Failed to print alarm CSV rows", e);
        }
    }

    private void printAttributes(TenantId tenantId, JsonNode component, CSVPrinter printer) throws Exception {
        EntityId entityId = entityIdOf(component);
        printer.printRecord("Entity", "Attribute", "Value", "Last Updated");
        String entityName = dataFetcher.entityName(tenantId, entityId);
        for (var attr : dataFetcher.fetchAttributes(tenantId, entityId, keysOf(component))) {
            printer.printRecord(entityName, attr.getKey(), attr.getValueAsString(), attr.getLastUpdateTs());
        }
    }

}
