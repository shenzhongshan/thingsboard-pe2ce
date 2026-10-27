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
/** 图表缺失、非法值统一显示占位符；有效零值仍带单位，避免直接拼接出undefined或null。 */
export function formatEnergyOverviewChartValue(value: unknown, unit: 'kWh' | 'kW'): string {
  return typeof value === 'number' && Number.isFinite(value) ? `${value} ${unit}` : '__';
}
/** 按两个接口数值的十进制表示相减，不固定小数位，避免1200.115-1200产生浮点长尾。 */
export function subtractEnergyOverviewValues(current: number | null, previous: number | null): number | null {
  if (current === null || previous === null || !Number.isFinite(current) || !Number.isFinite(previous)) {
    return null;
  }
  const parseDecimal = (value: number): {integer: bigint; scale: number} => {
    const [mantissa, exponent = '0'] = String(value).toLowerCase().split('e');
    const [integer, fraction = ''] = mantissa.split('.');
    return {integer: BigInt(integer + fraction), scale: fraction.length - Number(exponent)};
  };
  const left = parseDecimal(current);
  const right = parseDecimal(previous);
  const scale = Math.max(0, left.scale, right.scale);
  const difference = left.integer * 10n ** BigInt(scale - left.scale) - right.integer * 10n ** BigInt(scale - right.scale);
  const sign = difference < 0n ? '-' : '';
  const digits = String(difference < 0n ? -difference : difference).padStart(scale + 1, '0');
  return Number(scale ? `${sign}${digits.slice(0, -scale)}.${digits.slice(-scale)}` : `${sign}${digits}`);
}

/** 用电量来自累计表底差；功率来自独立小时AVG接口，两者不可混用。 */
export function buildEnergyOverviewSnapshot(energy?: EnergyRegulationEnergyConsumption | null,
  power?: EnergyPowerTrend | null): EnergyOverviewSnapshot {
  const comparisons = (['day','week','month'] as const).map((period, index) => {
    const value = energy?.energyConsumptionByPeriod[period];
    const currentKwh = value?.valueKwh ?? null; const previousKwh = value?.previousValueKwh ?? null;
    return {period, currentLabel: ['今日用电','本周用电','本月用电'][index],
      previousLabel: ['昨日用电','上周用电','上月用电'][index], currentKwh, previousKwh,
      deltaKwh: subtractEnergyOverviewValues(currentKwh, previousKwh),
      changePercent: value?.changePercent ?? null};
  });
  return {comparisons, energySeries: energy?.energySeries ?? {day: {labels: [], values: []},
    week: {labels: [], values: []}, month: {labels: [], values: []}}, powerLabels: power?.labels ?? [],
    todayPower: power?.today ?? [], yesterdayPower: power?.yesterday ?? [],
    todayPeakKw: power?.todayPeakKw ?? null, yesterdayPeakKw: power?.yesterdayPeakKw ?? null};
}
