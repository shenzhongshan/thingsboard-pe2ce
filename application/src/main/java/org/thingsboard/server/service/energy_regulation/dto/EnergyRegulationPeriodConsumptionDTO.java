/** 文件职责：传递一个用电周期的当前值、对比值及有效表计数量。 */
package org.thingsboard.server.service.energy_regulation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 日、周或月用电汇总；缺少区间边界读数时对应电量为 null。 */
@Getter
@Builder
public class EnergyRegulationPeriodConsumptionDTO {

    /** 当前周期用电量，单位 kWh；缺少有效表底差时为 null。 */
    private final BigDecimal valueKwh;
    /** 上一周期用电量，单位 kWh；缺少有效表底差时为 null。 */
    private final BigDecimal previousValueKwh;
    /** 相比上一周期的变化百分比；上一周期为零或缺数时为 null。 */
    private final BigDecimal changePercent;
    /** 当前周期具有有效边界读数并参与合计的电表数。 */
    private final int includedMeterCount;
    /** 上一周期具有有效边界读数并参与合计的电表数。 */
    private final int previousIncludedMeterCount;
}
