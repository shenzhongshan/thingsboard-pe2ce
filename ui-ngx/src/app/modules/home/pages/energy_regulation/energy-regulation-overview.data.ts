/** 文件职责：集中维护用能概况的站点样例和筛选后的环比、趋势数据。 */
import type {EnergyRegulationPeriod} from './energy-regulation-dashboard.data';

export interface EnergyOverviewComparisonRow {
  period: EnergyRegulationPeriod;
  currentLabel: string;
  previousLabel: string;
  currentKwh: number;
  previousKwh: number;
  deltaKwh: number;
  changePercent: number;
}

export interface EnergyOverviewSeries {
  labels: string[];
  values: number[];
}

export interface EnergyOverviewSnapshot {
  comparisons: EnergyOverviewComparisonRow[];
  energySeries: Record<EnergyRegulationPeriod, EnergyOverviewSeries>;
  powerLabels: string[];
  todayPower: number[];
  yesterdayPower: number[];
  todayPeakKw: number;
  yesterdayPeakKw: number;
}

/** 页面暂用的样例站点数据；指标接口接入前统一汇总展示，不绑定真实资产 ID。 */
const SITE_SAMPLES = [
  {
    day: 842.6, previousDay: 811.4, week: 5910.8, previousWeek: 5762.3,
    month: 24320.5, previousMonth: 23801.6,
    hourlyWeights: [21, 18, 17, 16, 17, 21, 28, 37, 44, 47, 46, 45, 43, 42, 44, 46, 48, 50, 46, 41, 35, 29, 25, 22],
    yesterdayWeights: [20, 17, 16, 16, 17, 20, 27, 35, 42, 44, 43, 42, 41, 40, 42, 44, 46, 47, 44, 39, 34, 28, 24, 21],
    weekWeights: [815, 849, 863, 879, 884, 827, 793],
    monthWeights: [5700, 5940, 6240, 6440]
  },
  {
    day: 426.2, previousDay: 439.7, week: 2978.4, previousWeek: 3045.8,
    month: 12188.7, previousMonth: 12402.1,
    hourlyWeights: [12, 11, 10, 10, 11, 13, 15, 19, 23, 25, 24, 23, 22, 21, 22, 23, 25, 26, 24, 21, 18, 15, 13, 12],
    yesterdayWeights: [13, 12, 11, 10, 11, 13, 16, 20, 24, 26, 25, 24, 23, 22, 23, 24, 26, 27, 25, 22, 19, 16, 14, 13],
    weekWeights: [418, 427, 434, 442, 446, 413, 398],
    monthWeights: [2890, 2980, 3110, 3209]
  }
];

/** 按目标总量缩放采样权重，使曲线与环比指标采用相同用电量。 */
function distribute(total: number, weights: number[]): number[] {
  const weightTotal = weights.reduce((sum, value) => sum + value, 0);
  if (weightTotal === 0) {
    return weights.map(() => 0);
  }
  const values = weights.map((weight) => Math.round(total * weight / weightTotal * 100) / 100);
  // 最后一个时间桶承接四舍五入余量，保证图表累计量与环比卡一致。
  values[values.length - 1] = Math.round((total - values.slice(0, -1).reduce((sum, value) => sum + value, 0)) * 100) / 100;
  return values;
}

/** 汇总当前全部样例站点；真实用能接口接入后再按根资产 ID 聚合。 */
export function buildEnergyOverviewSnapshot(): EnergyOverviewSnapshot {
  const selected = SITE_SAMPLES;
  const total = (key: 'day' | 'previousDay' | 'week' | 'previousWeek' | 'month' | 'previousMonth') =>
    selected.reduce((sum, site) => sum + site[key], 0);
  const sumSeries = (valueKey: 'day' | 'previousDay' | 'week' | 'month', weightKey: 'hourlyWeights' | 'yesterdayWeights' | 'weekWeights' | 'monthWeights') => {
    const length = selected[0]?.[weightKey].length ?? 0;
    const result = Array.from({length}, () => 0);
    for (const site of selected) {
      const values = distribute(site[valueKey], site[weightKey]);
      values.forEach((value, index) => result[index] += value);
    }
    return result.map((value) => Math.round(value * 100) / 100);
  };
  const currentDay = sumSeries('day', 'hourlyWeights');
  const previousDay = sumSeries('previousDay', 'yesterdayWeights');
  const comparisons: EnergyOverviewComparisonRow[] = [
    {period: 'day', currentLabel: '今日用电', previousLabel: '昨日用电', currentKwh: total('day'), previousKwh: total('previousDay'), deltaKwh: 0, changePercent: 0},
    {period: 'week', currentLabel: '本周用电', previousLabel: '上周用电', currentKwh: total('week'), previousKwh: total('previousWeek'), deltaKwh: 0, changePercent: 0},
    {period: 'month', currentLabel: '本月用电', previousLabel: '上月用电', currentKwh: total('month'), previousKwh: total('previousMonth'), deltaKwh: 0, changePercent: 0}
  ];
  for (const row of comparisons) {
    row.deltaKwh = Math.round((row.currentKwh - row.previousKwh) * 100) / 100;
    row.changePercent = row.previousKwh === 0 ? 0 : Math.round(row.deltaKwh / row.previousKwh * 10000) / 100;
  }
  const powerLabels = Array.from({length: 24}, (_, hour) => `${String(hour).padStart(2, '0')}:00`);
  return {
    comparisons,
    energySeries: {
      day: {labels: powerLabels, values: currentDay},
      week: {labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], values: sumSeries('week', 'weekWeights')},
      month: {labels: ['第1周', '第2周', '第3周', '第4周'], values: sumSeries('month', 'monthWeights')}
    },
    powerLabels,
    todayPower: currentDay,
    yesterdayPower: previousDay,
    todayPeakKw: Math.max(0, ...currentDay),
    yesterdayPeakKw: Math.max(0, ...previousDay)
  };
}
