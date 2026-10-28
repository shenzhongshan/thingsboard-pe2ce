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

/** 周期趋势的横轴与能耗、碳排样例值。 */
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

/** 页面所有静态样例数据，集中维护以便后续替换成真实接口数据。 */
export interface EnergyRegulationDashboardData {
  energyMetrics: { todayKwh: number; totalKwh: number; monthKwh: number; monthChangePercent: number };
  energyConsumptionByPeriod: Record<EnergyRegulationPeriod, EnergyRegulationConsumptionSummary>;
  alarmSummary: { todayTotal: number; todayProcessed: number; urgentActive: number; resolutionRate: number };
  energySeries: Record<EnergyRegulationPeriod, EnergyRegulationValueSeries>;
  alarmSeries: Record<EnergyRegulationPeriod, EnergyRegulationAlarmSeries>;
  carbonSeries: Record<EnergyRegulationPeriod, EnergyRegulationValueSeries>;
  alarmSamples: EnergyRegulationAlarmSample[];
}

/** 原型演示地图点位；真实设备点位尚未接入，地图继续明确标记为演示数据。 */
export const DEMO_ENERGY_MAP_DEVICES: SmartLightningMapDevice[] = [
  {id: 'demo-energy-001', name: '配电房总表-01', productName: 'ESA智能电表', profileName: '智能电表', productCategory: 'energy_regulation', active: true, latitude: 22.5431, longitude: 114.0579, assetPath: '微物联/总部园区/1号配电房'},
  {id: 'demo-energy-002', name: '空调回路-02', productName: 'ESE电能质量监测器', profileName: '电能质量监测器', productCategory: 'energy_regulation', active: true, latitude: 22.5482, longitude: 114.0631, assetPath: '微物联/总部园区/空调机房'},
  {id: 'demo-energy-003', name: '动力柜-03', productName: 'ESB三相不平衡监测器', profileName: '三相不平衡监测器', productCategory: 'energy_regulation', active: false, latitude: 22.5372, longitude: 114.0521, assetPath: '微物联/总部园区/动力站', alarmNames: ['三相电流不平衡']},
  {id: 'demo-energy-004', name: '市电进线-04', productName: 'FD市电监测模块', profileName: '市电监测模块', productCategory: 'energy_regulation', active: true, latitude: 22.5512, longitude: 114.0498, assetPath: '微物联/总部园区/总配电室'}
];

/** 原型页面样例：只用于构建页面效果，尚未接入真实数据。 */
export const DEMO_ENERGY_REGULATION_DASHBOARD_DATA: EnergyRegulationDashboardData = {
  energyMetrics: {todayKwh: 1864.7, totalKwh: 284639.2, monthKwh: 42136.5, monthChangePercent: 4.8},
  energyConsumptionByPeriod: {
    day: {label: '今日用电量', valueKwh: 1864.7, previousValueKwh: 1942.4, comparisonLabel: '较昨日', changePercent: 4.0, changeDirection: 'down'},
    week: {label: '本周用电量', valueKwh: 13268.4, previousValueKwh: 13749.9, comparisonLabel: '较上周', changePercent: 3.5, changeDirection: 'down'},
    month: {label: '本月用电量', valueKwh: 42136.5, previousValueKwh: 44259.0, comparisonLabel: '较上月', changePercent: 4.8, changeDirection: 'down'}
  },
  alarmSummary: {todayTotal: 12, todayProcessed: 9, urgentActive: 2, resolutionRate: 75},
  energySeries: {
    day: {labels: ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'], values: [42, 38, 36, 44, 78, 112, 126, 119, 132, 105, 84, 62]},
    week: {labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], values: [1865, 1942, 1796, 2058, 1987, 1624, 1711]},
    month: {labels: ['第1周', '第2周', '第3周', '第4周'], values: [12860, 13642, 11980, 12415]}
  },
  alarmSeries: {
    day: {labels: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00'], pending: [0, 1, 1, 2, 1, 0], assigned: [0, 0, 1, 1, 2, 1], processing: [0, 0, 1, 2, 1, 0]},
    week: {labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], pending: [2, 1, 1, 3, 2, 0, 1], assigned: [1, 2, 1, 2, 1, 1, 1], processing: [2, 1, 3, 2, 2, 1, 1]},
    month: {labels: ['第1周', '第2周', '第3周', '第4周'], pending: [8, 6, 9, 5], assigned: [7, 8, 6, 7], processing: [12, 14, 11, 16]}
  },
  carbonSeries: {
    day: {labels: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00'], values: [21, 18, 44, 67, 72, 39]},
    week: {labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], values: [284, 296, 273, 312, 301, 246, 259]},
    month: {labels: ['第1周', '第2周', '第3周', '第4周'], values: [1924, 2041, 1796, 1862]}
  },
  alarmSamples: [
    {deviceName: '动力柜-03', productName: 'ESB三相不平衡监测器', location: '微物联/总部园区/动力站', severity: '严重', message: '三相电流不平衡', occurredAt: '2026-09-28 10:42:18'},
    {deviceName: '空调回路-02', productName: 'ESE电能质量监测器', location: '微物联/总部园区/空调机房', severity: '紧急', message: '电压暂降', occurredAt: '2026-09-28 09:18:06'},
    {deviceName: '市电进线-04', productName: 'FD市电监测模块', location: '微物联/总部园区/总配电室', severity: '一般', message: '功率因数偏低', occurredAt: '2026-09-28 08:55:43'},
    {deviceName: '配电房总表-01', productName: 'ESA智能电表', location: '微物联/总部园区/1号配电房', severity: '一般', message: '日用电量接近阈值', occurredAt: '2026-09-27 17:31:20'}
  ]
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
