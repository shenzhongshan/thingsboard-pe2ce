/** 文件职责：共享遥测模块的 smart-lightning-measurements，承载该模块文件的实现细节。 */
/**
 * 智能防雷模块的测量单位与公共显示格式。
 *
 * 所有页面只从这里取得单位，避免设备详情、看板曲线和监测卡片
 * 分别维护相同的字符串。这里只保存展示层定义，不改变后端遥测值。
 */
export const SMART_LIGHTNING_UNITS = {
  resistance: 'Ω',
  leakageCurrent: 'μA',
  lightningCurrent: 'kA',
  transientCurrent: 'A',
  voltage: 'V',
  highVoltage: 'kV',
  temperature: '℃',
  percentage: '%',
  count: '次',
  interval: 'min',
  duration: 'μs',
  charge: 'As',
  electricCharge: 'C',
  specificEnergy: 'kJ/Ω',
  empty: ''
} as const;

/** 波形遥测键和采样间隔，供接入、解析与图表横轴共用。 */
export const SMART_LIGHTNING_WAVEFORM_KEY = 'WF';
export const SMART_LIGHTNING_WAVEFORM_SAMPLE_INTERVAL_US = 2.5;
