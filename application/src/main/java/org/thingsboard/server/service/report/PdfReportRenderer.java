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
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.report.ReportComponentType;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Renders a {@link ReportTemplate} (format=PDF) to PDF bytes: builds one HTML fragment per
 * configured component, wraps them in a hand-written Thymeleaf page template (PE's actual
 * templates/fonts aren't available in this port's source tree — see project memory), and converts
 * the assembled HTML/CSS to PDF via Flying Saucer (backed by iText/OpenPDF).
 * <p>
 * DASHBOARD components are satisfied by re-rendering the dashboard's time-series/latest-value
 * widgets natively through this same chart/table pipeline (grouped under the dashboard's title),
 * rather than PE's approach of screenshotting the live dashboard via a separate microservice —
 * a deliberate, documented substitution given no headless-browser infrastructure exists here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfReportRenderer {

    private final ReportDataFetcher dataFetcher;
    private final ReportChartFactory chartFactory;
    private final DashboardService dashboardService;
    private final ReportTemplateService reportTemplateService;

    private final TemplateEngine templateEngine = buildTemplateEngine();

    private static TemplateEngine buildTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("report/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    public byte[] render(TenantId tenantId, ReportTemplate template, String timezone) throws Exception {
        JsonNode config = template.getConfiguration();
        long now = System.currentTimeMillis();

        List<String> componentsHtml = new ArrayList<>();
        JsonNode components = config.path("components");
        if (components.isArray()) {
            for (JsonNode component : components) {
                componentsHtml.add(renderComponent(tenantId, component, now, timezone, 0));
            }
        }

        Context ctx = new Context();
        ctx.setVariable("pageSize", config.path("pageSize").asText("A4"));
        ctx.setVariable("pageOrientation", config.path("pageOrientation").asText("portrait"));
        ctx.setVariable("pageMargin", config.path("pageMargin").asText("2cm"));
        ctx.setVariable("header", config.path("header").isMissingNode() ? null : config.path("header").asText());
        ctx.setVariable("footer", config.path("footer").isMissingNode() ? null : config.path("footer").asText());
        ctx.setVariable("componentsHtml", componentsHtml);

        String html = templateEngine.process("report-template", ctx);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.createPDF(out);
        return out.toByteArray();
    }

    private String renderComponent(TenantId tenantId, JsonNode component, long now, String timezone, int subReportDepth) throws Exception {
        String typeStr = component.path("type").asText(null);
        if (typeStr == null) {
            return "";
        }
        ReportComponentType type;
        try {
            type = ReportComponentType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            log.debug("Unknown report component type '{}', skipping", typeStr);
            return "";
        }
        return switch (type) {
            case HEADING -> renderHeading(component);
            case RICH_TEXT -> component.path("html").asText("");
            case IMAGE -> "<img class=\"tb-report-image\" width=\"600\" height=\"400\" src=\""
                    + escapeAttr(component.path("url").asText("")) + "\"/>";
            case DIVIDER -> "<hr class=\"tb-report-divider\"/>";
            case PAGE_BREAK -> "<div class=\"tb-report-page-break\"></div>";
            case ENTITY_TABLE -> renderEntityTable(tenantId, component);
            case TIME_SERIES_TABLE -> renderTimeSeriesTable(tenantId, component, now, timezone);
            case ALARM_TABLE -> renderAlarmTable(tenantId, component, now, timezone);
            case TIME_SERIES_CHART -> renderTimeSeriesChart(tenantId, component, now);
            case LATEST_CHART -> renderLatestChart(tenantId, component);
            case DASHBOARD -> renderDashboard(tenantId, component, now, timezone);
            case SUB_REPORT -> renderSubReport(tenantId, component, now, timezone, subReportDepth);
        };
    }

    private String renderHeading(JsonNode component) {
        int level = Math.clamp(component.path("level").asInt(1), 1, 3);
        return "<h" + level + ">" + escape(component.path("text").asText("")) + "</h" + level + ">";
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

    private String renderEntityTable(TenantId tenantId, JsonNode component) throws Exception {
        EntityId entityId = entityIdOf(component);
        List<String> keys = keysOf(component);
        List<AttributeKvEntry> attrs = dataFetcher.fetchAttributes(tenantId, entityId, keys);
        StringBuilder sb = new StringBuilder();
        appendTitle(sb, component, dataFetcher.entityName(tenantId, entityId));
        sb.append("<table class=\"tb-report-table\"><tr><th>Attribute</th><th>Value</th><th>Last Updated</th></tr>");
        SimpleDateFormat fmt = dateFormat(null);
        for (AttributeKvEntry attr : attrs) {
            sb.append("<tr><td>").append(escape(attr.getKey())).append("</td><td>")
                    .append(escape(attr.getValueAsString())).append("</td><td>")
                    .append(escape(fmt.format(new java.util.Date(attr.getLastUpdateTs())))).append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String renderTimeSeriesTable(TenantId tenantId, JsonNode component, long now, String timezone) throws Exception {
        EntityId entityId = entityIdOf(component);
        List<String> keys = keysOf(component);
        long[] range = timeRangeOf(component, now);
        List<TsKvEntry> entries = dataFetcher.fetchTimeseries(tenantId, entityId, keys, range[0], range[1]);
        StringBuilder sb = new StringBuilder();
        appendTitle(sb, component, dataFetcher.entityName(tenantId, entityId));
        sb.append("<table class=\"tb-report-table\"><tr><th>Time</th><th>Key</th><th>Value</th></tr>");
        SimpleDateFormat fmt = dateFormat(timezone);
        for (TsKvEntry entry : entries) {
            sb.append("<tr><td>").append(escape(fmt.format(new java.util.Date(entry.getTs())))).append("</td><td>")
                    .append(escape(entry.getKey())).append("</td><td>")
                    .append(escape(entry.getValueAsString())).append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String renderAlarmTable(TenantId tenantId, JsonNode component, long now, String timezone) {
        EntityId entityId = entityIdOf(component);
        long[] range = timeRangeOf(component, now);
        List<AlarmInfo> alarms = dataFetcher.fetchAlarms(tenantId, entityId, range[0], range[1]);
        StringBuilder sb = new StringBuilder();
        appendTitle(sb, component, dataFetcher.entityName(tenantId, entityId));
        sb.append("<table class=\"tb-report-table\"><tr><th>Created</th><th>Type</th><th>Severity</th><th>Status</th></tr>");
        SimpleDateFormat fmt = dateFormat(timezone);
        for (AlarmInfo alarm : alarms) {
            String status = alarm.isCleared() ? "CLEARED" : (alarm.isAcknowledged() ? "ACK" : "ACTIVE");
            sb.append("<tr><td>").append(escape(fmt.format(new java.util.Date(alarm.getCreatedTime())))).append("</td><td>")
                    .append(escape(alarm.getType())).append("</td><td>")
                    .append(escape(String.valueOf(alarm.getSeverity()))).append("</td><td>")
                    .append(escape(status)).append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String renderTimeSeriesChart(TenantId tenantId, JsonNode component, long now) throws Exception {
        EntityId entityId = entityIdOf(component);
        List<String> keys = keysOf(component);
        long[] range = timeRangeOf(component, now);
        List<TsKvEntry> entries = dataFetcher.fetchTimeseries(tenantId, entityId, keys, range[0], range[1]);
        Map<String, List<TsKvEntry>> byKey = new LinkedHashMap<>();
        for (String key : keys) {
            byKey.put(key, new ArrayList<>());
        }
        for (TsKvEntry entry : entries) {
            byKey.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry);
        }
        String title = component.path("title").asText(dataFetcher.entityName(tenantId, entityId));
        String dataUri = chartFactory.timeSeriesChartDataUri(title, byKey);
        return "<img class=\"tb-report-image\" width=\"700\" height=\"350\" src=\"" + dataUri + "\"/>";
    }

    private String renderLatestChart(TenantId tenantId, JsonNode component) throws Exception {
        EntityId entityId = entityIdOf(component);
        List<String> keys = keysOf(component);
        List<TsKvEntry> latest = dataFetcher.fetchLatest(tenantId, entityId, keys);
        Map<String, Double> values = new LinkedHashMap<>();
        for (TsKvEntry entry : latest) {
            try {
                values.put(entry.getKey(), Double.parseDouble(entry.getValueAsString()));
            } catch (NumberFormatException ignored) {
                // non-numeric latest values can't be charted
            }
        }
        String title = component.path("title").asText(dataFetcher.entityName(tenantId, entityId));
        String chartType = component.path("chartType").asText("BAR");
        String dataUri = "PIE".equalsIgnoreCase(chartType)
                ? chartFactory.latestPieChartDataUri(title, values)
                : chartFactory.latestBarChartDataUri(title, values);
        return "<img class=\"tb-report-image\" width=\"700\" height=\"350\" src=\"" + dataUri + "\"/>";
    }

    private String renderDashboard(TenantId tenantId, JsonNode component, long now, String timezone) throws Exception {
        EntityId dashboardId = EntityIdFactory.getByTypeAndUuid("DASHBOARD", component.path("dashboardId").asText());
        DashboardInfo dashboard = null;
        try {
            dashboard = dashboardService.findDashboardInfoById(tenantId, new org.thingsboard.server.common.data.id.DashboardId(dashboardId.getId()));
        } catch (Exception e) {
            log.debug("Dashboard {} not found for report rendering", dashboardId, e);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(escape(dashboard != null ? dashboard.getName() : "Dashboard")).append("</h2>");
        // CE simplification: dashboard embedding renders each widget's datasource key(s) declared
        // directly on this component (rather than parsing the dashboard's live widget layout),
        // reusing the same chart pipeline as a standalone TIME_SERIES_CHART/LATEST_CHART.
        JsonNode widgets = component.path("widgets");
        if (widgets.isArray()) {
            for (JsonNode widget : widgets) {
                String widgetType = widget.path("type").asText("TIME_SERIES_CHART");
                if ("LATEST_CHART".equalsIgnoreCase(widgetType)) {
                    sb.append(renderLatestChart(tenantId, widget));
                } else {
                    sb.append(renderTimeSeriesChart(tenantId, widget, now));
                }
            }
        }
        return sb.toString();
    }

    private String renderSubReport(TenantId tenantId, JsonNode component, long now, String timezone, int depth) throws Exception {
        if (depth >= 3) {
            log.warn("Sub-report nesting too deep, skipping to avoid infinite recursion");
            return "";
        }
        String templateIdStr = component.path("reportTemplateId").asText(null);
        if (templateIdStr == null) {
            return "";
        }
        ReportTemplate subTemplate = reportTemplateService.findReportTemplateById(tenantId,
                new org.thingsboard.server.common.data.id.ReportTemplateId(java.util.UUID.fromString(templateIdStr)));
        if (subTemplate == null || subTemplate.getConfiguration() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        JsonNode subComponents = subTemplate.getConfiguration().path("components");
        if (subComponents.isArray()) {
            for (JsonNode subComponent : subComponents) {
                sb.append(renderComponent(tenantId, subComponent, now, timezone, depth + 1));
            }
        }
        return sb.toString();
    }

    private void appendTitle(StringBuilder sb, JsonNode component, String defaultTitle) {
        String title = component.path("title").asText(defaultTitle);
        if (title != null && !title.isEmpty()) {
            sb.append("<h2>").append(escape(title)).append("</h2>");
        }
    }

    private SimpleDateFormat dateFormat(String timezone) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (timezone != null && !timezone.isEmpty()) {
            fmt.setTimeZone(TimeZone.getTimeZone(ZoneId.of(timezone)));
        }
        return fmt;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeAttr(String s) {
        return escape(s).replace("\"", "&quot;");
    }

}
