/** 文件职责：把能源监管用电统计 DTO 转换为 HTTP VO。 */
package org.thingsboard.server.controller.energy_regulation.converter;

import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationEnergyConsumptionVO;
import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationPeriodConsumptionVO;
import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationValueSeriesVO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationEnergyConsumptionDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationPeriodConsumptionDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationValueSeriesDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** 用显式字段映射隔离服务 DTO 与 HTTP 响应契约。 */
public final class EnergyRegulationEnergyConsumptionVOConverter {

    /** 转换职责由静态映射方法承担。 */
    private EnergyRegulationEnergyConsumptionVOConverter() {
    }

    /** 将用电汇总、趋势及部分读取状态映射为前端响应对象。
     * @param source 服务层用电统计 DTO
     * @return Controller 返回给前端的 HTTP VO
     */
    public static EnergyRegulationEnergyConsumptionVO toVO(EnergyRegulationEnergyConsumptionDTO source) {
        // 先把服务层周期汇总逐项映射到 HTTP 周期 VO，保持接口字段显式可审查。
        Map<String, EnergyRegulationPeriodConsumptionVO> periods = new LinkedHashMap<>();
        for (Map.Entry<String, EnergyRegulationPeriodConsumptionDTO> entry
                : source.getEnergyConsumptionByPeriod().entrySet()) {
            EnergyRegulationPeriodConsumptionDTO value = entry.getValue();
            periods.put(entry.getKey(), EnergyRegulationPeriodConsumptionVO.builder()
                    .valueKwh(value.getValueKwh())
                    .previousValueKwh(value.getPreviousValueKwh())
                    .changePercent(value.getChangePercent())
                    .includedMeterCount(value.getIncludedMeterCount())
                    .previousIncludedMeterCount(value.getPreviousIncludedMeterCount())
                    .build());
        }
        // 再映射各趋势序列，并复制列表以隔离服务 DTO 与响应 VO 的集合引用。
        Map<String, EnergyRegulationValueSeriesVO> series = new LinkedHashMap<>();
        for (Map.Entry<String, EnergyRegulationValueSeriesDTO> entry : source.getEnergySeries().entrySet()) {
            EnergyRegulationValueSeriesDTO value = entry.getValue();
            series.put(entry.getKey(), EnergyRegulationValueSeriesVO.builder()
                    .labels(new ArrayList<>(value.getLabels()))
                    .values(new ArrayList<>(value.getValues()))
                    .build());
        }
        // 最后组装顶层统计字段和两组已转换的周期数据。
        return EnergyRegulationEnergyConsumptionVO.builder()
                .meterCount(source.getMeterCount())
                .readFailureCount(source.getReadFailureCount())
                .invalidCounterCount(source.getInvalidCounterCount())
                .totalKwh(source.getTotalKwh())
                .energyConsumptionByPeriod(periods)
                .energySeries(series)
                .build();
    }
}
