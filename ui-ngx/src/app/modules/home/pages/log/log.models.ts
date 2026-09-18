/** 文件职责：设备日志页面的前端模型，描述设备列表、属性、遥测和时间轴数据。 */
import { AttributeData, TimeseriesData } from '@shared/models/telemetry/telemetry.models';

export interface LogDevice {
  deviceId: string;
  deviceName: string;
  deviceProfileName: string;
}

export interface LogDevicePage {
  data: LogDevice[];
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
}

export interface LogTimelineItem {
  timestamp: number;
  category: '服务器属性' | '最新遥测' | '审计事件' | '告警事件';
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
