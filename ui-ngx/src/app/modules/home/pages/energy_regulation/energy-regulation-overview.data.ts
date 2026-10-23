/** 将能源监管真实接口转换为概况，缺数保留null，不生成样例曲线。 */
import type {EnergyRegulationPeriod} from './energy-regulation-dashboard.data';
import type {EnergyRegulationEnergyConsumption, EnergyPowerTrend} from './energy-regulation-api.service';
export interface EnergyOverviewComparisonRow {
  period: EnergyRegulationPeriod; currentLabel: string; previousLabel: string;
  currentKwh: number | null; previousKwh: number | null; deltaKwh: number | null; changePercent: number | null;
}
export interface EnergyOverviewSeries {labels: string[]; values: Array<number | null>;}
export interface EnergyOverviewSnapshot {
  comparisons: EnergyOverviewComparisonRow[]; energySeries: Record<EnergyRegulationPeriod, EnergyOverviewSeries>;
  powerLabels: string[]; todayPower: Array<number | null>; yesterdayPower: Array<number | null>;
  todayPeakKw: number | null; yesterdayPeakKw: number | null;
}
/** 用电量来自累计表底差；功率来自独立小时AVG接口，两者不可混用。 */
export function buildEnergyOverviewSnapshot(energy?: EnergyRegulationEnergyConsumption | null,
  power?: EnergyPowerTrend | null): EnergyOverviewSnapshot {
  const comparisons = (['day','week','month'] as const).map((period, index) => {
    const value = energy?.energyConsumptionByPeriod[period];
    const currentKwh = value?.valueKwh ?? null; const previousKwh = value?.previousValueKwh ?? null;
    return {period, currentLabel: ['今日用电','本周用电','本月用电'][index],
      previousLabel: ['昨日用电','上周用电','上月用电'][index], currentKwh, previousKwh,
      deltaKwh: currentKwh === null || previousKwh === null ? null : currentKwh - previousKwh,
      changePercent: value?.changePercent ?? null};
  });
  return {comparisons, energySeries: energy?.energySeries ?? {day: {labels: [], values: []},
    week: {labels: [], values: []}, month: {labels: [], values: []}}, powerLabels: power?.labels ?? [],
    todayPower: power?.today ?? [], yesterdayPower: power?.yesterday ?? [],
    todayPeakKw: power?.todayPeakKw ?? null, yesterdayPeakKw: power?.yesterdayPeakKw ?? null};
}
