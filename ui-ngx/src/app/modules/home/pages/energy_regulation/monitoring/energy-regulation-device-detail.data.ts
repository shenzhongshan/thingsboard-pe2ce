/** ESA 详情纯数据定义；产品参数来自 A40070400 V3.0，遥测名称和单位复用能源规范。 */
import {ENERGY_METER_MEASUREMENTS} from '../shared/energy-regulation-measurements';

export type EnergyDetailPeriod = 'day' | 'week' | 'month';
/** 图表按同单位电气量分组；不把电压、电流等不同单位画在同一数轴。 */
export interface EnergyDetailChart {id: string; title: string; keys: string[];}
export interface EnergyDetailSample {ts: number; value: unknown;}

/** 说明书列出的额定电流为选型范围，不能用作某台设备实际额定值。 */
export const ENERGY_METER_SPECIFICATION: ReadonlyArray<{label: string; value: string}> = [
  {label: '产品名称', value: 'ESA智能电表'},
  {label: '适用回路', value: '低压三相回路'},
  {label: '工作电压', value: 'AC 180～250 V（220 V）'},
  {label: '产品功耗', value: '≤2 W'},
  {label: '额定电压', value: '220 V / 380 V；精度0.2级'},
  {label: '电流选型', value: '5 / 100 / 200 / 400 / 600 / 1000 A（按实际选型）'},
  {label: '电流精度', value: '0.2级'},
  {label: '频率范围', value: '45～65 Hz；精度0.01 Hz'},
  {label: '电能精度', value: '有功0.5级 / 无功1级'},
  {label: '开关量输入', value: '2路干接点，内置电源'},
  {label: '继电器输出', value: '1路常开；AC 250 V / 3 A，DC 30 V / 3 A'},
  {label: '通讯方式', value: 'RS485 / Modbus；默认9600、8N1'},
  {label: '工作环境', value: '-20～70 ℃；湿度<95%'},
  {label: '防护等级', value: 'IP20'},
  {label: '安装方式', value: '标准35 mm导轨'},
  {label: '外形尺寸', value: '36 × 110 × 69 mm'},
  {label: '监测功能', value: '电压、电流、频率、功率、功率因数、电能'},
  {label: '报警功能', value: 'DI输入、过欠压、过欠流、过欠功率、DO联动'}
];

/** 操作说明摘自同一产品说明书，不包含平台远程控制或虚构固件能力。 */
export const ENERGY_METER_USAGE: ReadonlyArray<{label: string; value: string}> = [
  {label: '显示操作', value: 'OLED轮询显示每10秒切换一次；选择键与确认键配合操作'},
  {label: '报警查看', value: '可在OLED查询当前报警信息和电能数据'},
  {label: '接线要求', value: '互感器注意电流箭头方向：黄-L1、绿-L2、红-L3'},
  {label: '供电要求', value: '220V交流输入和5V直流输入只能二选一'},
  {label: '指示灯', value: '有负载时脉冲灯闪烁；输入/输出报警灯正常绿、报警红'}
];

/** 前四组在主页面展示，其余通过更多参数弹窗展示；覆盖全部31个数值测点。 */
export const ENERGY_DETAIL_CHARTS: readonly EnergyDetailChart[] = [
  {id: 'voltage', title: '相电压', keys: ['PhAVolt', 'PhBVolt', 'PhCVolt']},
  {id: 'current', title: '相电流', keys: ['PhACurr', 'PhBCurr', 'PhCCurr']},
  {id: 'active-power', title: '有功功率', keys: ['ActPwrA', 'ActPwrB', 'ActPwrC', 'ActPwrTot']},
  {id: 'energy', title: '正向有功电能', keys: ['FwdActEA', 'FwdActEB', 'FwdActEC', 'FwdActET']},
  {id: 'line-voltage', title: '线电压', keys: ['LineVoltAB', 'LineVoltBC', 'LineVoltCA']},
  {id: 'frequency', title: '频率', keys: ['Freq']},
  {id: 'vector-current', title: '三相电流矢量和', keys: ['CurrVecSum']},
  {id: 'reactive-power', title: '无功功率', keys: ['RctPwrA', 'RctPwrB', 'RctPwrC', 'RctPwrTot']},
  {id: 'apparent-power', title: '视在功率', keys: ['AppPwrA', 'AppPwrB', 'AppPwrC', 'AppPwrTot']},
  {id: 'power-factor', title: '功率因数', keys: ['PwrFactA', 'PwrFactB', 'PwrFactC', 'PwrFactT']}
];

/** 与现有详情图表一致：日为当天，周/月为最近7/30天；各图自行维护周期。 */
export function getEnergyDetailWindow(period: EnergyDetailPeriod, now = Date.now()): {startTs: number; endTs: number} {
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  return {startTs: period === 'day' ? start.getTime() : now - (period === 'week' ? 7 : 30) * 86400000, endTs: now};
}

/** 缺值不伪造零；上报值原样展示，状态仅使用规范中已确认的0/1含义。 */
export function formatEnergyDetailValue(value: unknown, status: boolean, unit: string): string {
  if (value === null || value === undefined || String(value).trim() === '') { return '__'; }
  if (status && (String(value) === '0' || String(value) === '1')) { return String(value) === '0' ? '正常' : '报警'; }
  return `${value}${unit ? ' ' + unit : ''}`;
}

/** HTTP补读与WebSocket都按测点实际时间合并，迟到数据不覆盖最新值。 */
export function mergeEnergyDetailSamples(target: Map<string, EnergyDetailSample>, values: Readonly<Record<string, readonly EnergyDetailSample[]>>): void {
  for (const definition of ENERGY_METER_MEASUREMENTS) {
    for (const sample of values[definition.key] || []) {
      if (Number.isFinite(sample.ts) && sample.ts >= (target.get(definition.key)?.ts ?? -Infinity)) {
        target.set(definition.key, sample);
      }
    }
  }
}

/** 清理缺失/非数值采样并按原时间排序，保留零值且不做倍率换算。 */
export function buildEnergyDetailChartPoints(samples: readonly EnergyDetailSample[]): Array<[number, number]> {
  const points = new Map<number, number>();
  for (const sample of samples) {
    if (sample.value === null || sample.value === undefined || String(sample.value).trim() === '') { continue; }
    const value = Number(sample.value);
    if (Number.isFinite(sample.ts) && Number.isFinite(value)) { points.set(sample.ts, value); }
  }
  return Array.from(points.entries()).sort((a, b) => a[0] - b[0]);
}
