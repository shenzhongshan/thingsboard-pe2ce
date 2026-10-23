/** 能源电表测点规范；名称和单位来自用户测点表，业务页面不引用其他模块的映射。 */
export interface EnergyMeasurementDefinition {key: string; label: string; unit: string; status: boolean;}
export const ENERGY_METER_MEASUREMENTS: readonly EnergyMeasurementDefinition[] = [
  {"key": "DOut1", "label": "DO1输出状态", "unit": "", "status": true},
  {"key": "DIn1", "label": "DI1输入状态", "unit": "", "status": true},
  {"key": "DIn2", "label": "DI2输入状态", "unit": "", "status": true},
  {"key": "PhAVolt", "label": "A相电压", "unit": "V", "status": false},
  {"key": "PhBVolt", "label": "B相电压", "unit": "V", "status": false},
  {"key": "PhCVolt", "label": "C相电压", "unit": "V", "status": false},
  {"key": "LineVoltAB", "label": "AB线电压", "unit": "V", "status": false},
  {"key": "LineVoltBC", "label": "BC线电压", "unit": "V", "status": false},
  {"key": "LineVoltCA", "label": "CA线电压", "unit": "V", "status": false},
  {"key": "Freq", "label": "频率", "unit": "Hz", "status": false},
  {"key": "PhACurr", "label": "A相电流", "unit": "A", "status": false},
  {"key": "PhBCurr", "label": "B相电流", "unit": "A", "status": false},
  {"key": "PhCCurr", "label": "C相电流", "unit": "A", "status": false},
  {"key": "CurrVecSum", "label": "三相电流矢量和", "unit": "A", "status": false},
  {"key": "ActPwrA", "label": "A相有功功率", "unit": "kW", "status": false},
  {"key": "ActPwrB", "label": "B相有功功率", "unit": "kW", "status": false},
  {"key": "ActPwrC", "label": "C相有功功率", "unit": "kW", "status": false},
  {"key": "ActPwrTot", "label": "总有功功率", "unit": "kW", "status": false},
  {"key": "RctPwrA", "label": "A相无功功率", "unit": "kvar", "status": false},
  {"key": "RctPwrB", "label": "B相无功功率", "unit": "kvar", "status": false},
  {"key": "RctPwrC", "label": "C相无功功率", "unit": "kvar", "status": false},
  {"key": "RctPwrTot", "label": "总无功功率", "unit": "kvar", "status": false},
  {"key": "AppPwrA", "label": "A相视在功率", "unit": "kVA", "status": false},
  {"key": "AppPwrB", "label": "B相视在功率", "unit": "kVA", "status": false},
  {"key": "AppPwrC", "label": "C相视在功率", "unit": "kVA", "status": false},
  {"key": "AppPwrTot", "label": "总视在功率", "unit": "kVA", "status": false},
  {"key": "PwrFactA", "label": "A相功率因数", "unit": "", "status": false},
  {"key": "PwrFactB", "label": "B相功率因数", "unit": "", "status": false},
  {"key": "PwrFactC", "label": "C相功率因数", "unit": "", "status": false},
  {"key": "PwrFactT", "label": "总功率因数", "unit": "", "status": false},
  {"key": "FwdActEA", "label": "A相正向有功电能", "unit": "kWh", "status": false},
  {"key": "FwdActEB", "label": "B相正向有功电能", "unit": "kWh", "status": false},
  {"key": "FwdActEC", "label": "C相正向有功电能", "unit": "kWh", "status": false},
  {"key": "FwdActET", "label": "总正向有功电能", "unit": "kWh", "status": false}
];
/** 所有能源页面共用规范键；不对设备上报值做单位换算。 */
export const ENERGY_REALTIME_KEYS = ENERGY_METER_MEASUREMENTS.map((item) => item.key);
export const ENERGY_TOTAL_KEY = 'FwdActET';
export const ENERGY_POWER_KEY = 'ActPwrTot';
