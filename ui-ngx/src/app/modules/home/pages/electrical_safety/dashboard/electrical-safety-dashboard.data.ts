/** 文件职责：定义电气安全看板的页面样例数据和状态占比转换。 */
import type {SmartLightningMapDevice} from '../../smart-lightning/shared/map/smart-lightning-device-map.models';
import type {DeviceAlarmDisplayColumn, DeviceAlarmDisplayRow, DeviceAlarmSeverityTone} from '@home/pages/deviceAlarms/shared/alarm-list/device-alarm-records.model';

/** 右侧单张电气监测卡片展示的数量统计。 */
export interface ElectricalSafetyDashboardSummary {
  /** 卡片类别键，用于模板追踪和分类样式。 */
  key: 'fire' | 'safe-electricity' | 'power-quality';
  /** 卡片标题及配套 Material 图标名。 */
  title: string;
  icon: string;
  /** 当前类别的在线、正常、离线和告警数量。 */
  onlineCount: number;
  normalCount: number;
  offlineCount: number;
  alarmCount: number;
}

/** 设备总览卡片展示的整体在线状态统计。 */
export interface ElectricalSafetyDeviceOverview {
  total: number;
  online: number;
  offline: number;
  alarm: number;
  onlineRate: number;
}

/** 告警柱状图中的单个分类及已归一化柱高。 */
export interface ElectricalSafetyAlarmChartItem {
  category: string;
  count: number;
  heightPercent: number;
}

/** 运维工单图中的一个分类和三种处理状态数量。 */
export interface ElectricalSafetyWorkOrderChartItem {
  category: string;
  pending: number;
  unassigned: number;
  processed: number;
  pendingHeight: number;
  unassignedHeight: number;
  processedHeight: number;
}

/** 告警记录表格行，仅保留页面展示所需字段。 */
export interface ElectricalSafetyAlarmRecord {
  deviceName: string;
  deviceType: string;
  location: string;
  severity: string;
  severityTone: Exclude<DeviceAlarmSeverityTone, 'indeterminate'>;
  description: string;
  occurredAt: string;
}

/** 电气安全告警表沿用原有六列语义，并使用共享组件统一绘制。 */
export const ELECTRICAL_SAFETY_ALARM_COLUMNS: readonly DeviceAlarmDisplayColumn[] = [
  {field: 'deviceName', label: '设备名称', widthPercent: 21},
  {field: 'deviceType', label: '设备类型', widthPercent: 19},
  {field: 'source', label: '设备位置', widthPercent: 14},
  {field: 'severity', label: '报警级别', widthPercent: 9},
  {field: 'type', label: '报警情况', widthPercent: 21},
  {field: 'startTs', label: '异常发生时间', widthPercent: 16}
];

/** 将电气安全样例记录映射成共享告警表契约，不补造当前数据源没有的字段。 */
export function mapElectricalSafetyAlarmRecords(records: readonly ElectricalSafetyAlarmRecord[]): DeviceAlarmDisplayRow[] {
  return records.map((record) => ({
    source: record.location,
    secondaryLocation: '--',
    device: record.deviceName,
    deviceType: record.deviceType,
    deviceName: record.deviceName,
    type: record.description,
    severity: record.severity,
    severityTone: record.severityTone,
    status: '--',
    startTs: parseElectricalSafetyAlarmTime(record.occurredAt)
  }));
}

/** 将现有本地时间字符串转成日期管道使用的毫秒时间戳。 */
function parseElectricalSafetyAlarmTime(value: string): number {
  const match = /^(\d{4})\/(\d{1,2})\/(\d{1,2})\s+(\d{1,2}):(\d{1,2}):(\d{1,2})$/.exec(value);
  if (!match) {
    return Number.NaN;
  }
  const [, year, month, day, hour, minute, second] = match;
  return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second)).getTime();
}

/** 右侧卡片可直接绑定的百分比展示模型。 */
export interface ElectricalSafetyDashboardCard extends ElectricalSafetyDashboardSummary {
  onlinePercent: number;
  offlinePercent: number;
  alarmPercent: number;
  normalPercent: number;
}

/** 看板样例统计：用于先搭建完整页面布局，页面本身不显示样例数据提示。 */
const ELECTRICAL_SAFETY_DASHBOARD_SAMPLE_SUMMARIES: readonly ElectricalSafetyDashboardSummary[] = [
  {
    key: 'fire',
    title: '电气火灾监测',
    icon: 'local_fire_department',
    onlineCount: 0,
    normalCount: 0,
    offlineCount: 0,
    alarmCount: 0
  },
  {
    key: 'safe-electricity',
    title: '安全用电监测',
    icon: 'electrical_services',
    onlineCount: 0,
    normalCount: 0,
    offlineCount: 0,
    alarmCount: 0
  },
  {
    key: 'power-quality',
    title: '电力质量监测',
    icon: 'monitoring',
    onlineCount: 2,
    normalCount: 0,
    offlineCount: 8,
    alarmCount: 10
  }
];

/** 页面其他区域的数据样例，结构对齐防雷看板的设备、图表、地图和告警面板。 */
export const ELECTRICAL_SAFETY_DASHBOARD_DATA = {
  deviceOverview: {
    total: 10,
    online: 2,
    offline: 8,
    alarm: 10,
    onlineRate: 20
  } satisfies ElectricalSafetyDeviceOverview,
  alarmChart: [
    {category: '电气火灾', count: 1, heightPercent: 20},
    {category: '安全用电', count: 2, heightPercent: 40},
    {category: '电力质量', count: 5, heightPercent: 100}
  ] satisfies ElectricalSafetyAlarmChartItem[],
  workOrderChart: [
    {category: '电气火灾', pending: 2, unassigned: 1, processed: 1, pendingHeight: 67, unassignedHeight: 34, processedHeight: 34},
    {category: '安全用电', pending: 1, unassigned: 2, processed: 3, pendingHeight: 34, unassignedHeight: 67, processedHeight: 100},
    {category: '电力质量', pending: 3, unassigned: 1, processed: 2, pendingHeight: 100, unassignedHeight: 34, processedHeight: 67}
  ] satisfies ElectricalSafetyWorkOrderChartItem[],
  alarmRecords: [
    {
      deviceName: 'SFA电能质量监测器-01',
      deviceType: 'SFA电能质量监测器',
      location: 'A栋配电室',
      severity: '严重',
      severityTone: 'critical',
      description: 'A相电压异常，当前值 232.4 V',
      occurredAt: '2026/10/12 09:16:42'
    },
    {
      deviceName: 'SFA电能质量监测器-02',
      deviceType: 'SFA电能质量监测器',
      location: 'B栋配电室',
      severity: '重要',
      severityTone: 'major',
      description: 'B相电压波动超限',
      occurredAt: '2026/10/12 09:12:08'
    },
    {
      deviceName: '智能安全用电监测器-03',
      deviceType: '安全用电监测器',
      location: 'C栋一层总配电箱',
      severity: '警告',
      severityTone: 'warning',
      description: '剩余电流持续偏高',
      occurredAt: '2026/10/12 08:57:31'
    },
    {
      deviceName: '电气火灾监测器-04',
      deviceType: '电气火灾监测器',
      location: 'D栋配电间',
      severity: '次要',
      severityTone: 'minor',
      description: '线缆温度达到预警值',
      occurredAt: '2026/10/12 08:43:19'
    }
  ] satisfies ElectricalSafetyAlarmRecord[]
};

/** 地图展示的设备样例点位，统一落在共享地图配置的默认视野附近。 */
export const ELECTRICAL_SAFETY_MAP_DEVICES: SmartLightningMapDevice[] = [
  {id: 'electrical-safety-demo-01', name: '电能质量监测器-01', productName: 'SFA电能质量监测器', profileName: 'SFA', productCategory: 'electrical_safety', active: true, latitude: 22.5031, longitude: 114.0588, assetPath: '项目 / A栋 / 配电室', alarmNames: ['A相电压异常']},
  {id: 'electrical-safety-demo-02', name: '电能质量监测器-02', productName: 'SFA电能质量监测器', profileName: 'SFA', productCategory: 'electrical_safety', active: true, latitude: 22.4979, longitude: 114.0631, assetPath: '项目 / B栋 / 配电室'},
  {id: 'electrical-safety-demo-03', name: '安全用电监测器-03', productName: '安全用电监测器', profileName: '安全用电', productCategory: 'electrical_safety', active: false, latitude: 22.5013, longitude: 114.0662, assetPath: '项目 / C栋 / 配电室'},
  {id: 'electrical-safety-demo-04', name: '电气火灾监测器-04', productName: '电气火灾监测器', profileName: '电气火灾', productCategory: 'electrical_safety', active: false, latitude: 22.4954, longitude: 114.0563, assetPath: '项目 / D栋 / 配电间'},
  {id: 'electrical-safety-demo-05', name: '安全用电监测器-05', productName: '安全用电监测器', profileName: '安全用电', productCategory: 'electrical_safety', active: false, latitude: 22.5062, longitude: 114.0678, assetPath: '项目 / E栋 / 配电室'},
  {id: 'electrical-safety-demo-06', name: '电气火灾监测器-06', productName: '电气火灾监测器', profileName: '电气火灾', productCategory: 'electrical_safety', active: false, latitude: 22.4928, longitude: 114.0617, assetPath: '项目 / F栋 / 配电间'}
];

/** 按状态数量计算百分比，空数据时返回 0，避免产生无效宽度。 */
function calculatePercentage(value: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return Math.round(value / total * 100);
}

/** 为三张监测卡片构造可直接呈现的在线、离线、告警和正常占比。 */
export function buildElectricalSafetyDashboardCards(): ElectricalSafetyDashboardCard[] {
  return ELECTRICAL_SAFETY_DASHBOARD_SAMPLE_SUMMARIES.map((summary) => {
    const connectivityTotal = summary.onlineCount + summary.offlineCount;
    const conditionTotal = summary.alarmCount + summary.normalCount;
    return {
      ...summary,
      onlinePercent: calculatePercentage(summary.onlineCount, connectivityTotal),
      offlinePercent: calculatePercentage(summary.offlineCount, connectivityTotal),
      alarmPercent: calculatePercentage(summary.alarmCount, conditionTotal),
      normalPercent: calculatePercentage(summary.normalCount, conditionTotal)
    };
  });
}
