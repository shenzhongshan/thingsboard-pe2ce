package org.thingsboard.server.controller.energy_regulation.converter;

import org.thingsboard.server.controller.energy_regulation.vo.EnergyRegulationPowerTrendVO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationPowerTrendDTO;

/** 将能源监管功率DTO显式转换为HTTP展示对象，不进行查询或计算。 */
public final class EnergyRegulationPowerTrendVOConverter {
    private EnergyRegulationPowerTrendVOConverter() { }
    /** 保留缺数及kW单位口径，不把空值改为零。 */
    public static EnergyRegulationPowerTrendVO toVO(EnergyRegulationPowerTrendDTO source) {
        return EnergyRegulationPowerTrendVO.builder().labels(source.getLabels()).today(source.getToday())
                .yesterday(source.getYesterday()).todayPeakKw(source.getTodayPeakKw())
                .yesterdayPeakKw(source.getYesterdayPeakKw()).periodPeaks(source.getPeriodPeaks()).build();
    }
}
