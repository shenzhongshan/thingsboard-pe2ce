/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.node.ArrayNode
 *  com.fasterxml.jackson.databind.node.NullNode
 *  com.fasterxml.jackson.databind.node.ObjectNode
 *  lombok.Generated
 *  org.apache.commons.lang3.math.NumberUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.thingsboard.common.util.JacksonUtil
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.query.EntityData
 *  org.thingsboard.server.common.data.query.EntityKeyType
 *  org.thingsboard.server.common.data.query.TsValue
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.TableSortOrder
 *  org.thingsboard.server.common.data.report.configuration.TableSortOrder$Direction
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold
 *  org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent
 *  org.thingsboard.server.common.data.report.configuration.components.DataReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.util.DataSourceUtils
 */
package org.thingsboard.server.report.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.util.DataSourceUtils;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;

public class ReportUtils {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ReportUtils.class);
    public static final Pattern REPORT_NAME_DATE_PATTERN = Pattern.compile("%d\\{([^\\}]*)\\}");
    public static final String DEFAULT_REPORT_NAME_PATTERN = "report-%d{yyyy-MM-dd_HH:mm:ss}";
    public static final Set<String> ENTITY_TIME_FIELDS = Set.of("ts", "createdTime", "startTime", "endTime", "ackTime", "clearTime", "assignTime");
    public static final String RAW_TS_PREFIX = "rawTs_";

    public static String prepareReportName(String namePattern, Date reportDate, String timeZoneStr) {
        return ReportUtils.prepareReportName(namePattern, reportDate, timeZoneStr, null);
    }

    public static String prepareReportName(String namePattern, Date reportDate, String timeZoneStr, String extension) {
        TimeZone timeZone = timeZoneStr == null ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneStr);
        String name = namePattern == null || namePattern.isEmpty() ? DEFAULT_REPORT_NAME_PATTERN : namePattern;
        Matcher matcher = REPORT_NAME_DATE_PATTERN.matcher((CharSequence)name);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            dateFormat.setTimeZone(timeZone);
            String replacement = dateFormat.format(reportDate);
            name = ((String)name).replace(toReplace, replacement);
        }
        if (extension != null && !((String)name).endsWith(extension)) {
            name = (String)name + extension;
        }
        return name;
    }

    public static void prepareReportComponent(ReportComponent component) {
        DataReportComponent dataReportComponent;
        List<DataSource> dataSources;
        if (component instanceof DataReportComponent && (dataSources = (dataReportComponent = (DataReportComponent)component).getDataSources()) != null && !dataSources.isEmpty()) {
            for (DataSource dataSource : dataSources) {
                if (dataSource == null) continue;
                ReportUtils.prepareDataKeys(dataSource.getDataKeys());
                ReportUtils.prepareDataKeys(dataSource.getLatestDataKeys());
            }
        }
    }

    private static void prepareDataKeys(List<DataKey> dataKeys) {
        if (dataKeys != null && !dataKeys.isEmpty()) {
            for (DataKey dataKey : dataKeys) {
                if (dataKey == null || !StringUtils.isBlank((String)dataKey.getLabel())) continue;
                dataKey.setLabel(dataKey.getName());
            }
        }
    }

    public static Optional<DataSource> getSingleDataSource(DataReportComponent component) {
        DataSource dataSource = null;
        if (ReportComponentType.ALARM_TABLE.equals((Object)component.getType())) {
            dataSource = ((AlarmTableComponent)component).getAlarmSource();
        } else {
            List dataSources = component.getDataSources();
            if (dataSources != null && !dataSources.isEmpty()) {
                dataSource = (DataSource)dataSources.get(0);
            }
        }
        if (ReportUtils.isDataSourceValid(dataSource)) {
            return Optional.of(dataSource);
        }
        return Optional.empty();
    }

    public static List<DataSource> getMultipleDataSources(DataReportComponent component) {
        List<DataSource> dataSources = component.getDataSources();
        dataSources = dataSources == null ? new ArrayList<DataSource>() : dataSources.stream().filter(ReportUtils::isDataSourceValid).toList();
        return dataSources;
    }

    public static boolean isDataSourceValid(DataSource dataSource) {
        if (dataSource == null) {
            return false;
        }
        switch (dataSource.getType()) {
            case DEVICE: {
                if (dataSource.getDeviceId() != null) break;
                return false;
            }
            case ENTITY: {
                if (dataSource.getEntityAliasId() != null) break;
                return false;
            }
        }
        return true;
    }

    public static String updateDashboardReportStateParamsWithEntity(String state, EntityData stateEntity) {
        ObjectNode stateParams;
        JsonNode stateData;
        ArrayNode stateObj = null;
        if (StringUtils.isNotBlank((String)state)) {
            try {
                String decoded = new String(Base64.getDecoder().decode(state));
                JsonNode parsed = JacksonUtil.toJsonNode((String)decoded);
                if (parsed.isArray() && !parsed.isEmpty()) {
                    stateObj = (ArrayNode)parsed;
                }
            }
            catch (Exception decoded) {
                // empty catch block
            }
        }
        if (stateObj == null) {
            stateObj = JacksonUtil.newArrayNode();
            ObjectNode stateData2 = JacksonUtil.newObjectNode();
            stateData2.set("id", (JsonNode)NullNode.getInstance());
            stateObj.add((JsonNode)stateData2);
        }
        if ((stateData = stateObj.get(stateObj.size() - 1)).has("params") && stateData.get("params").isObject()) {
            stateParams = (ObjectNode)stateData.get("params");
        } else {
            stateParams = JacksonUtil.newObjectNode();
            ((ObjectNode)stateData).set("params", (JsonNode)stateParams);
        }
        stateParams.set("entityId", JacksonUtil.valueToTree((Object)stateEntity.getEntityId()));
        Optional<String> entityName = DataSourceUtils.getEntityLatestValue((EntityData)stateEntity, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"name");
        Optional<String> entityLabel = DataSourceUtils.getEntityLatestValue((EntityData)stateEntity, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"label");
        stateParams.remove("entityName");
        stateParams.remove("entityLabel");
        entityName.ifPresent(s -> stateParams.put("entityName", s));
        entityLabel.ifPresent(s -> stateParams.put("entityLabel", s));
        String newStateJsonStr = JacksonUtil.toString((Object)stateObj);
        String b64 = Base64.getEncoder().encodeToString(newStateJsonStr.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(b64, StandardCharsets.UTF_8);
    }

    public static List<TsChartThresholdItem> collectThresholdItems(List<TimeSeriesChartThreshold> thresholds, List<EntityData> entityDatas, boolean latestElseEntity) {
        ArrayList<TsChartThresholdItem> thresholdItems = new ArrayList<TsChartThresholdItem>();
        if (!thresholds.isEmpty() && !entityDatas.isEmpty()) {
            EntityData entity = entityDatas.get(0);
            Map latestValues = entity.getLatest();
            for (TimeSeriesChartThreshold threshold : thresholds) {
                TsValue tsValue;
                EntityKeyType keyType = EntityKeyType.fromName((String)(latestElseEntity ? threshold.getLatestKeyType() : threshold.getEntityKeyType()));
                Map valuesByType = (Map)latestValues.get(keyType);
                if (valuesByType == null || (tsValue = (TsValue)valuesByType.get(latestElseEntity ? threshold.getLatestKey() : threshold.getEntityKey())) == null) continue;
                try {
                    double doubleValue = Double.parseDouble(tsValue.getValue());
                    thresholdItems.add(new TsChartThresholdItem(threshold, doubleValue));
                }
                catch (NumberFormatException numberFormatException) {}
            }
        }
        return thresholdItems;
    }

    public static String formatValueWithPrecisionAndUnits(String value, DataKey dataKey) {
        if (value == null || ((String)value).isBlank()) {
            return "";
        }
        try {
            if (dataKey.getDecimals() != null) {
                BigDecimal decimal = new BigDecimal((String)value);
                value = decimal.setScale((int)dataKey.getDecimals(), RoundingMode.HALF_UP).toPlainString();
            }
        }
        catch (ArithmeticException | NumberFormatException runtimeException) {
            // empty catch block
        }
        if (dataKey.getUnits() != null) {
            value = (String)value + dataKey.getUnits();
        }
        return value;
    }

    public static void sortRowsByTableSortOrder(List<Map<String, String>> rows, TableSortOrder tableSortOrder) {
        if (tableSortOrder == null || tableSortOrder.getColumn() == null || rows.isEmpty()) {
            return;
        }
        String column = tableSortOrder.getColumn();
        if (rows.get(0).containsKey(RAW_TS_PREFIX + (String)column)) {
            column = RAW_TS_PREFIX + (String)column;
        }
        String finalColumn = column;
        Comparator<Map> comparator = Comparator.comparing(row -> (String)row.getOrDefault(finalColumn, ""), ReportUtils::compareMixedValuesNullFirst);
        if (tableSortOrder.getDirection() == TableSortOrder.Direction.DESC) {
            comparator = comparator.reversed();
        }
        rows.sort(comparator);
    }

    public static int compareMixedValuesNullFirst(String v1, String v2) {
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return -1;
        }
        if (v2 == null) {
            return 1;
        }
        boolean isV1Numeric = NumberUtils.isParsable((String)v1);
        boolean isV2Numeric = NumberUtils.isParsable((String)v2);
        if (isV1Numeric && isV2Numeric) {
            return Double.compare(Double.parseDouble(v1), Double.parseDouble(v2));
        }
        return v1.compareToIgnoreCase(v2);
    }

    public static Object convertStringToTypedValue(String value) {
        if (StringUtils.isBlank((String)value)) {
            return value;
        }
        if (NumberUtils.isParsable((String)value)) {
            return Double.parseDouble(value);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    public static String formatTimestamp(long timestamp, String pattern, String timezone) {
        if (timestamp == 0L) {
            return "";
        }
        if (pattern == null || pattern.isEmpty() || pattern.equals("milliseconds")) {
            return String.valueOf(timestamp);
        }
        try {
            ZoneId zoneId = timezone != null && !timezone.isBlank() ? ZoneId.of(timezone) : ZoneId.systemDefault();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(zoneId);
            return formatter.format(Instant.ofEpochMilli(timestamp));
        }
        catch (Exception e) {
            return "Invalid timestamp: " + timestamp;
        }
    }

    public static String formatTimestamp(String timestampStr, String pattern, TbReportCtx ctx, String timezone) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            return ReportUtils.formatTimestamp(timestamp, pattern, ctx, timezone);
        }
        catch (NumberFormatException e) {
            return "Invalid timestamp string: " + timestampStr;
        }
    }

    public static String formatTimestamp(long timestamp, String pattern, TbReportCtx ctx) {
        return ReportUtils.formatTimestamp(timestamp, pattern, ctx, null);
    }

    public static String formatTimestamp(long timestamp, String pattern, TbReportCtx ctx, String timezone) {
        String effectivePattern = pattern != null && !pattern.isEmpty() ? pattern : ctx.getConfiguration().getTimeDataPattern();
        String targetTimezone = StringUtils.isNotBlank((String)timezone) ? timezone : ctx.getTimeZone();
        return ReportUtils.formatTimestamp(timestamp, effectivePattern, targetTimezone);
    }
}

