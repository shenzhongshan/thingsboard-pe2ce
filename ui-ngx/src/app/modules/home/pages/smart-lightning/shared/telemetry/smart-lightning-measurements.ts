/** 文件职责：共享遥测模块的 smart-lightning-measurements，承载该模块文件的实现细节。 */
/**
 * 智能防雷模块的测量单位与公共显示格式。
 *
 * 所有页面只从这里取得单位，避免设备详情、看板曲线和监测卡片
 * 分别维护相同的字符串。这里只保存展示层定义，不改变后端遥测值。
 */
export const SMART_LIGHTNING_UNITS = {
  resistance: 'Ω',
  leakageCurrent: 'mA',
  lightningCurrent: 'kA',
  voltage: 'V',
  highVoltage: 'kV',
  temperature: '℃',
  percentage: '%',
  count: '次',
  interval: 'min',
  empty: ''
} as const;

export type SmartLightningMeasurementUnit = typeof SMART_LIGHTNING_UNITS[keyof typeof SMART_LIGHTNING_UNITS];

/**
 * 统一格式化监测数值。
 *
 * @param value 待展示数值
 * @param unit 已由 SMART_LIGHTNING_UNITS 提供的单位
 * @param separator 单位前的分隔符；详情指标保持原有无空格格式
 */
export const formatSmartLightningMeasurement = (
  value: number | string,
  unit: SmartLightningMeasurementUnit | string = SMART_LIGHTNING_UNITS.empty,
  separator = ''
): string => `${value}${separator}${unit}`;
