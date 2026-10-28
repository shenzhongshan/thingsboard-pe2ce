/** 文件职责：向能源监管页面返回一个周期的用电汇总。 */
package org.thingsboard.server.controller.energy_regulation.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 周期汇总单位为 kWh，边界数据不足时电量字段为 null。 */
@Getter
@Builder
public class EnergyRegulationPeriodConsumptionVO {

    /** 当前日、周或月的用电量，单位 kWh；缺少有效边界读数时为 null。 */
    private final BigDecimal valueKwh;
    /** 上一完整周期用电量，单位 kWh；缺少有效边界读数时为 null。 */
    private final BigDecimal previousValueKwh;
    /** 相比上一周期的变化百分比；无法比较时为 null。 */
    private final BigDecimal changePercent;
    /** 当前周期有有效表底差并参与汇总的电表数。 */
    private final int includedMeterCount;
    /** 上一周期有有效表底差并参与汇总的电表数。 */
    private final int previousIncludedMeterCount;
}
