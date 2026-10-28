/** 文件职责：传递按图表时间桶聚合后的用电趋势。 */
package org.thingsboard.server.service.energy_regulation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** 横轴标签与对应的用电量；无有效遥测的时间桶使用 null。 */
@Getter
@Builder
public class EnergyRegulationValueSeriesDTO {

    /** 图表横轴标签，按实际时间桶顺序排列。 */
    private final List<String> labels;
    /** 每个时间桶的用电量，单位 kWh；未来或缺少读数时为 null。 */
    private final List<BigDecimal> values;
}
