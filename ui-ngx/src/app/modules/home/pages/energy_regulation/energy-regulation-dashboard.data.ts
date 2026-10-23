import type {
  EnergyRegulationDeviceContext,
  EnergyRegulationEnergyConsumption
} from './energy-regulation-api.service';
import type {SmartLightningMapDevice} from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.models';

export type EnergyRegulationPeriod = 'day' | 'week' | 'month';
export type EnergyRegulationChartId = 'consumption' | 'energy' | 'alarms' | 'carbon';
export type EnergyRegulationPeriods = Record<EnergyRegulationChartId, EnergyRegulationPeriod>;

/** 在线与离线按设备 active 状态统计；告警设备单独去重，可与在线或离线状态重叠。 */
export interface EnergyRegulationDeviceSummary {
  total: number;
  online: number;
  offline: number;
  alarming: number;
}

/** 按平台设备档案统计设备数量，不从设备名称或型号推断设备分类。 */
export interface EnergyRegulationDeviceProfileCount {
  profileName: string;
  count: number;
}

/** 设备概况接口的纯转换结果，供页面状态卡片、分布列表和图表共用。 */
export interface EnergyRegulationDeviceOverview {
  summary: EnergyRegulationDeviceSummary;
  profiles: EnergyRegulationDeviceProfileCount[];
}

/** 告警记录仅用于展示视觉效果，不对应真实告警实体或处理操作。 */
export interface EnergyRegulationAlarmSample {
  deviceName: string;
  productName: string;
  location: string;
  severity: '一般' | '紧急' | '严重';
  message: string;
  occurredAt: string;
}

/** 周期趋势的横轴与指标序列。 */
export interface EnergyRegulationValueSeries {
  labels: string[];
  values: Array<number | null>;
}

/** 告警趋势按状态堆叠展示。 */
export interface EnergyRegulationAlarmSeries {
  labels: string[];
  pending: number[];
  assigned: number[];
  processing: number[];
}

/** 用能统计卡在日、周、月周期下展示的汇总数值与比较信息。 */
export interface EnergyRegulationConsumptionSummary {
  label: string;
  valueKwh: number;
  previousValueKwh: number;
  comparisonLabel: string;
  changePercent: number;
  changeDirection: 'up' | 'down';
}

/** 旧纯函数导出契约，应用页面已改为能源接口驱动。 */
export interface EnergyRegulationDashboardData {
  energyMetrics: { todayKwh: number; totalKwh: number; monthKwh: number; monthChangePercent: number };
  energyConsumptionByPeriod: Record<EnergyRegulationPeriod, EnergyRegulationConsumptionSummary>;
  alarmSummary: { todayTotal: number; todayProcessed: number; urgentActive: number; resolutionRate: number };
  energySeries: Record<EnergyRegulationPeriod, EnergyRegulationValueSeries>;
  alarmSeries: Record<EnergyRegulationPeriod, EnergyRegulationAlarmSeries>;
  carbonSeries: Record<EnergyRegulationPeriod, EnergyRegulationValueSeries>;
  alarmSamples: EnergyRegulationAlarmSample[];
}

/** 兼容历史导出名称的空结构；业务页面只消费真实接口，不再载入样例。 */
export const DEMO_ENERGY_MAP_DEVICES: SmartLightningMapDevice[] = [];
export const DEMO_ENERGY_REGULATION_DASHBOARD_DATA: EnergyRegulationDashboardData = {
  energyMetrics: {todayKwh: 0, totalKwh: 0, monthKwh: 0, monthChangePercent: 0},
  energyConsumptionByPeriod: Object.fromEntries(['day','week','month'].map((key) => [key,
    {label: '', valueKwh: 0, previousValueKwh: 0, comparisonLabel: '', changePercent: 0, changeDirection: 'up'}])) as Record<EnergyRegulationPeriod, EnergyRegulationConsumptionSummary>,
  alarmSummary: {todayTotal: 0, todayProcessed: 0, urgentActive: 0, resolutionRate: 0},
  energySeries: {day: {labels: [], values: []}, week: {labels: [], values: []}, month: {labels: [], values: []}},
  carbonSeries: {day: {labels: [], values: []}, week: {labels: [], values: []}, month: {labels: [], values: []}},
  alarmSeries: {day: {labels: [], pending: [], assigned: [], processing: []},
    week: {labels: [], pending: [], assigned: [], processing: []}, month: {labels: [], pending: [], assigned: [], processing: []}},
  alarmSamples: []
};

/** 从能源监管设备上下文和活动告警设备 ID 生成在线、离线及独立告警设备统计。 */
export function buildEnergyRegulationDeviceOverview(
  devices: EnergyRegulationDeviceContext[],
  alarmedDeviceIds: ReadonlySet<string>
): EnergyRegulationDeviceOverview {
  let online = 0;
  let offline = 0;
  let alarming = 0;
  const profileCounts = new Map<string, number>();

  for (const device of devices) {
    // 环图只看在线状态；告警数单独统计，因此告警设备仍计入在线或离线数量。
    if (device.active) {
      online++;
    } else {
      offline++;
    }
    if (alarmedDeviceIds.has(device.deviceId)) {
      alarming++;
    }

    const profileName = device.deviceProfileName?.trim() || '未配置设备档案';
    profileCounts.set(profileName, (profileCounts.get(profileName) || 0) + 1);
  }

  const profiles = Array.from(profileCounts.entries())
    .map(([profileName, count]) => ({profileName, count}))
    .sort((left, right) => right.count - left.count || left.profileName.localeCompare(right.profileName, 'zh-CN'));

  return {
    summary: {total: devices.length, online, offline, alarming},
    profiles
  };
}

/** 按指定周期读取用能卡汇总，供界面绑定和单元测试共用。 */
export function getEnergyConsumptionForPeriod(
  data: EnergyRegulationDashboardData,
  period: EnergyRegulationPeriod
): EnergyRegulationConsumptionSummary {
  return data.energyConsumptionByPeriod[period];
}

/** 将接口周期汇总转换为用能卡文案；没有接口数值时保留 null，不回退为演示值。 */
export function getLiveEnergyConsumptionForPeriod(
  data: EnergyRegulationEnergyConsumption,
  period: EnergyRegulationPeriod
): EnergyRegulationConsumptionSummaryView {
  const summary = data.energyConsumptionByPeriod[period];
  const labels: Record<EnergyRegulationPeriod, {label: string; comparisonLabel: string}> = {
    day: {label: '今日用电量', comparisonLabel: '较昨日'},
    week: {label: '本周用电量', comparisonLabel: '较上周'},
    month: {label: '本月用电量', comparisonLabel: '较上月'}
  };
  return {...summary, ...labels[period]};
}

/** 用能卡展示真实接口中的当前、上一周期和变化率。 */
export interface EnergyRegulationConsumptionSummaryView {
  label: string;
  valueKwh: number | null;
  previousValueKwh: number | null;
  comparisonLabel: string;
  changePercent: number | null;
  includedMeterCount: number;
  previousIncludedMeterCount: number;
}

/** 看板首次显示时每个图表各自选择日视图。 */
export const INITIAL_ENERGY_REGULATION_PERIODS: EnergyRegulationPeriods = {
  consumption: 'day',
  energy: 'day',
  alarms: 'day',
  carbon: 'day'
};

export function setEnergyRegulationPeriod(
  periods: EnergyRegulationPeriods,
  chartId: EnergyRegulationChartId,
  period: EnergyRegulationPeriod
): EnergyRegulationPeriods {
  // 仅返回目标图表的新周期状态，避免覆盖其他图表或修改原对象。
  return {...periods, [chartId]: period};
}
