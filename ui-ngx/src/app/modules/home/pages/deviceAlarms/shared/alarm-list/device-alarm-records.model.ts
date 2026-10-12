/** 文件职责：定义共用告警记录组件的展示契约，不包含接口查询和设备分类逻辑。 */

/** 告警严重程度对应的展示色调，由各业务页面在组装展示行时确定。 */
export type DeviceAlarmSeverityTone = 'critical' | 'major' | 'minor' | 'warning' | 'indeterminate';

/** 已由业务页面完成格式化、可直接交给共用告警表格展示的一行数据。 */
export interface DeviceAlarmDisplayRow {
  /** 告警来源位置，例如设备的绝对资产路径或设备名称。 */
  source: string;
  /** 来源的次级位置；没有可用层级时由业务页面填入占位文案。 */
  secondaryLocation: string;
  /** 来源设备标识，用于生成稳定的告警行跟踪键。 */
  device: string;
  /** 设备类型文案。 */
  deviceType: string;
  /** 设备名称文案。 */
  deviceName: string;
  /** 告警类型文案。 */
  type: string;
  /** 告警严重程度本地化文案。 */
  severity: string;
  /** 与严重程度对应的样式色调。 */
  severityTone: DeviceAlarmSeverityTone;
  /** 告警确认/清除状态的本地化文案。 */
  status: string;
  /** 告警触发时间的 Unix 毫秒时间戳。 */
  startTs: number;
}

/** 共用告警表可配置的展示字段；设备标识和严重程度色调仅供内部渲染使用。 */
export type DeviceAlarmDisplayField = Exclude<keyof DeviceAlarmDisplayRow, 'device' | 'severityTone'>;

/** 单个告警表格列的标题和可选宽度，页面可按自己的告警语义调整列顺序。 */
export interface DeviceAlarmDisplayColumn {
  /** 对应告警展示行中的字段。 */
  field: DeviceAlarmDisplayField;
  /** 表头显示的中文名称。 */
  label: string;
  /** 可选列宽百分比；不传时由表格平均分配。 */
  widthPercent?: number;
}
