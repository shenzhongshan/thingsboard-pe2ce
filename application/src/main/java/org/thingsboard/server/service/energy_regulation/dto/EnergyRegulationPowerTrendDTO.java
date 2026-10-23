package org.thingsboard.server.service.energy_regulation.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 当前用户可见能源电表的小时平均总有功功率，单位kW；缺少任一电表样本的桶为空。 */
@Getter
@Builder
public class EnergyRegulationPowerTrendDTO {
    /** 按上海时区排列的24个小时标签。 */
    private final List<String> labels;
    /** 今日和昨日对应小时的平均总功率；未来及缺数桶为null。 */
    private final List<BigDecimal> today;
    private final List<BigDecimal> yesterday;
    /** 小时平均总功率的最大值，不冒充瞬时尖峰。 */
    private final BigDecimal todayPeakKw;
    private final BigDecimal yesterdayPeakKw;
    /** 当前自然日、周、月内小时平均总功率的最大值，day/week/month分别独立。 */
    private final Map<String, BigDecimal> periodPeaks;
}
