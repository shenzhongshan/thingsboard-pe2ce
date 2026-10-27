package org.thingsboard.server.service.energy_regulation.util;

import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** 纯查询条件工厂：累计用电按边界取原始表底，功率按自然日生成小时AVG查询，不执行数据库访问。 */
public final class EnergyRegulationTelemetryQueryFactory {

    /** 功率趋势及周期峰值保持自然小时平均值，单位毫秒。 */
    private static final long HOUR_MILLIS = 3_600_000L;
    /** 上海自然日最多24个小时桶，不以放大全局数据库限制绕过长范围查询。 */
    private static final int HOURS_PER_DAY = 24;

    private EnergyRegulationTelemetryQueryFactory() {
    }

    /** 为各累计电能候选键生成必要边界的最近一条原始读数查询。
     * @param now 本次统计固定的上海时区时间
     * @return 原生时序查询列表，按键及时间边界排列；无小时聚合、无倍率换算
     */
    public static List<ReadTsKvQuery> createConsumptionQueries(ZonedDateTime now) {
        List<Long> boundaries = EnergyRegulationEnergyConsumptionCalculator.findReadBoundaryTimestamps(now);
        List<ReadTsKvQuery> queries = new ArrayList<>();
        for (String key : EnergyRegulationEnergyMetricDefinitions.TOTAL_FORWARD_ACTIVE_ENERGY_KEYS) {
            for (long boundary : boundaries) {
                // SQL原始时序区间为[startTs,endTs)，与计算器lowerEntry的严格早于边界规则一致。
                // DESC LIMIT 1通过时序索引定位最近表底；保留真实采样时间，不伪造为整点。
                queries.add(new BaseReadTsKvQuery(key, 0, boundary, 1, "DESC"));
            }
        }
        return List.copyOf(queries);
    }

    /** 为日曲线及本日/周/月峰值生成按自然日分段的小时平均功率查询。
     * @param now 本次统计固定的上海时区时间
     * @return 每段不超过24个小时的AVG查询，连续时间段左闭右开，不重复跨段样本
     */
    public static List<ReadTsKvQuery> createPowerQueries(ZonedDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate firstDate = today.minusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        if (monthStart.isBefore(firstDate)) {
            firstDate = monthStart;
        }
        if (weekStart.isBefore(firstDate)) {
            firstDate = weekStart;
        }
        long nowTs = now.toInstant().toEpochMilli();
        List<ReadTsKvQuery> queries = new ArrayList<>();
        for (LocalDate day = firstDate; !day.isAfter(today); day = day.plusDays(1)) {
            long startTs = day.atStartOfDay(now.getZone()).toInstant().toEpochMilli();
            long endTs = Math.min(day.plusDays(1).atStartOfDay(now.getZone()).toInstant().toEpochMilli(), nowTs);
            if (startTs < endTs) {
                queries.add(new BaseReadTsKvQuery(
                        EnergyRegulationEnergyMetricDefinitions.TOTAL_ACTIVE_POWER_KEY,
                        startTs, endTs, HOUR_MILLIS, HOURS_PER_DAY, Aggregation.AVG, "ASC"));
            }
        }
        return List.copyOf(queries);
    }
}
