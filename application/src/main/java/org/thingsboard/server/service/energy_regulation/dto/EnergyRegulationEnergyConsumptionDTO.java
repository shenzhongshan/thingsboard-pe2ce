/** 文件职责：传递能源监管看板所需的真实用电统计和趋势。 */
package org.thingsboard.server.service.energy_regulation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/** 所有统计只覆盖当前用户可见且能读取总正向有功电能候选键的能源设备。 */
@Getter
@Builder
public class EnergyRegulationEnergyConsumptionDTO {

    /** 当前有可读总正向有功电能表底的电表数量。 */
    private final int meterCount;
    /** 单设备遥测查询失败数量；成功返回的统计可能是部分数据。 */
    private final int readFailureCount;
    /** 检测到计数器回退、无法参与区间差值的电表数量。 */
    private final int invalidCounterCount;
    /** 当前可读电表的累计表底合计，单位 kWh；无读数时为 null。 */
    private final BigDecimal totalKwh;
    /** 日、周、月汇总，键为 day、week、month。 */
    private final Map<String, EnergyRegulationPeriodConsumptionDTO> energyConsumptionByPeriod;
    /** 日、周、月趋势，键为 day、week、month。 */
    private final Map<String, EnergyRegulationValueSeriesDTO> energySeries;
}
