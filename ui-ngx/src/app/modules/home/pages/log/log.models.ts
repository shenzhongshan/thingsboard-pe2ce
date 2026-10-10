/** 文件职责：设备日志页面的前端模型，描述设备列表、属性、遥测和时间轴数据。 */
import { AttributeData, TimeseriesData } from '@shared/models/telemetry/telemetry.models';

/** 设备日志列表接口返回的一台设备，不含 WebSocket 派生状态。 */
export interface LogDeviceListItem {
  deviceId: string;
  deviceName: string;
  deviceProfileName: string;
}

/** 设备日志页面使用的设备行模型，包含当前列表实时状态。 */
export interface LogDevice extends LogDeviceListItem {
  /** active 最新遥测解析后的在线状态；null 表示共享 WebSocket 首帧尚未返回。 */
  active: boolean | null;
}

export interface LogDevicePage {
  data: LogDeviceListItem[];
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
}

export interface LogTimelineItem {
  timestamp: number;
  category: '服务器属性' | '最新遥测' | '审计事件' | '告警事件' | '设备状态';
  key: string;
  value: unknown;
  /** 审计事件解析后的字段，普通属性和遥测事件不填写。 */
  fields?: LogTimelineField[];
}

export interface LogTimelineField {
  label: string;
  value: string;
}

export interface LogTimelinePage {
  data: LogTimelineItem[];
  hasNext: boolean;
  nextCursor: string | null;
}

export interface LogDeviceSnapshot {
  attributes: AttributeData[];
  telemetry: TimeseriesData;
  timeline: LogTimelineItem[];
}
