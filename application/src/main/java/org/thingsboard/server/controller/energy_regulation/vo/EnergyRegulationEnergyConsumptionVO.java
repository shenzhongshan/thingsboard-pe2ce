/** 文件职责：能源监管用电统计 HTTP 响应对象。 */
package org.thingsboard.server.controller.energy_regulation.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/** 仅返回当前用户可见设备的用电结果和可读性状态。 */
@Getter
@Builder
public class EnergyRegulationEnergyConsumptionVO {

    /** 至少有一条可解析总正向有功电能遥测的设备数。 */
    private final int meterCount;
    /** 查询时序失败的设备数；其余设备数据仍可能被部分汇总。 */
    private final int readFailureCount;
    /** 检测到累计表底回退的电表数，同一电表只计一次。 */
    private final int invalidCounterCount;
    /** 当前可读设备最新累计表底合计，单位 kWh；无读数时为 null。 */
    private final BigDecimal totalKwh;
    /** 按 day、week、month 标识区分的当前周期及上一周期汇总。 */
    private final Map<String, EnergyRegulationPeriodConsumptionVO> energyConsumptionByPeriod;
    /** 按 day、week、month 标识区分的趋势横轴和各桶电量。 */
    private final Map<String, EnergyRegulationValueSeriesVO> energySeries;
}
