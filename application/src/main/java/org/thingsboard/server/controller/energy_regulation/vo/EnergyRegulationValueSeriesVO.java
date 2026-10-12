/** 文件职责：向能源监管页面返回用电趋势的横轴和数值。 */
package org.thingsboard.server.controller.energy_regulation.vo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** 无有效遥测的时间桶以 null 表示，不伪装成零用电。 */
@Getter
@Builder
public class EnergyRegulationValueSeriesVO {

    /** 图表横轴标签，位置与 values 中的时间桶一一对应。 */
    private final List<String> labels;
    /** 各时间桶用电量，单位 kWh；缺少有效表底差时为 null。 */
    private final List<BigDecimal> values;
}
