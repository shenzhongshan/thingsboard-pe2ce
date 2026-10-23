/** 文件职责：按电气安全设备型号定义趋势图分组，并转换历史遥测点。 */
import type {TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import type {ElectricalSafetyDeviceModel} from './electrical-safety-monitoring.data';
import {
  getElectricalSafetyRealtimeMetricDefinitions,
  resolveElectricalSafetyDeviceModel
} from './electrical-safety-monitoring.data';
import {getElectricalSafetyDetailWindow} from './electrical-safety-device-detail.data';
import type {ElectricalSafetyDetailPeriod} from './electrical-safety-device-detail.data';

/** 单张趋势图中的规范测点曲线，键名必须与 ThingsBoard 时序键完全一致。 */
export interface ElectricalSafetyDetailSeriesDefinition {
  key: string;
  label: string;
  unit: string;
}

/** 按型号和业务测量类型配置的趋势图；priority 越小越优先展示。 */
export interface ElectricalSafetyDetailChartDefinition {
  id: string;
  title: string;
  priority: number;
  unit: string;
  series: readonly ElectricalSafetyDetailSeriesDefinition[];
}

/** 一条历史曲线及其毫秒时间戳数据点。 */
export interface ElectricalSafetyDetailChartSeries {
  definition: ElectricalSafetyDetailSeriesDefinition;
  points: Array<[number, number]>;
}

/** 用于声明一条曲线，名称和单位从该型号的测点映射读取。 */
const series = (
  key: string,
  label: string,
  unit: string
): ElectricalSafetyDetailSeriesDefinition => ({key, label, unit});

/** 从型号测点定义生成曲线，避免另行维护重复的中文名称和单位。 */
function modelSeries(
  model: ElectricalSafetyDeviceModel,
  keys: readonly string[]
): ElectricalSafetyDetailSeriesDefinition[] {
  const definitions = new Map(getElectricalSafetyRealtimeMetricDefinitions(model).map((item) => [item.key, item]));
  const result: ElectricalSafetyDetailSeriesDefinition[] = [];
  for (const key of keys) {
    const definition = definitions.get(key);
    if (definition) {
      result.push(series(definition.key, definition.label, definition.unit === '无' ? '' : definition.unit));
    }
  }
  return result;
}

/** 创建趋势图卡；同一卡片内曲线使用同一物理量单位。 */
function chart(
  id: string,
  title: string,
  priority: number,
  chartSeries: readonly ElectricalSafetyDetailSeriesDefinition[]
): ElectricalSafetyDetailChartDefinition {
  return {id, title, priority, unit: chartSeries[0]?.unit || '', series: chartSeries};
}

/** 按工作簿逐型号配置同物理量曲线；状态测点保留在实时卡片，不混入连续量图。 */
export const ELECTRICAL_SAFETY_CHART_DEFINITIONS: Readonly<
  Record<ElectricalSafetyDeviceModel, readonly ElectricalSafetyDetailChartDefinition[]>
> = {
  LDH1: [],
  FAP1: [],
  ESF1: [
    chart('residual-current', '剩余电流', 1, modelSeries('ESF1', ['ResCurr'])),
    chart('temperature', '温度', 2, modelSeries('ESF1', ['Temp1', 'Temp2', 'Temp3', 'Temp4']))
  ],
  E1T2: [
    chart('temperature', '温度', 1, modelSeries('E1T2', ['Temp1', 'Temp2', 'Temp3', 'Temp4', 'Temp5', 'Temp6', 'Temp7', 'Temp8']))
  ],
  E2C3: [
    chart('residual-current', '剩余电流', 1, modelSeries('E2C3', ['ResCurr1', 'ResCurr2', 'ResCurr3']))
  ],
  E1I2: [],
  CBP4: [
    chart('line-voltage', '线电压', 1, modelSeries('CBP4', ['LineVoltAB', 'LineVoltAC', 'LineVoltBC'])),
    chart('phase-voltage', '相电压', 2, modelSeries('CBP4', ['PhAVolt', 'PhBVolt', 'PhCVolt'])),
    chart('zeroseqv', '零序电压', 3, modelSeries('CBP4', ['ZeroSeqV'])),
    chart('phase-current', '相电流', 4, modelSeries('CBP4', ['PhACurr', 'PhBCurr', 'PhCCurr'])),
    chart('leakvalue', '漏电值', 5, modelSeries('CBP4', ['LeakValue'])),
    chart('active-power', '有功功率', 6, modelSeries('CBP4', ['ActPwrTot', 'ActPwrA', 'ActPwrB', 'ActPwrC'])),
    chart('reactive-power', '无功功率', 7, modelSeries('CBP4', ['RctPwrTot', 'RctPwrA', 'RctPwrB', 'RctPwrC'])),
    chart('power-factor', '功率因数', 8, modelSeries('CBP4', ['PwrFactT', 'PwrFactA', 'PwrFactB', 'PwrFactC'])),
    chart('totenergy', '总电量', 9, modelSeries('CBP4', ['TotEnergy'])),
    chart('temperature', '温度', 10, modelSeries('CBP4', ['PhATemp', 'PhBTemp', 'PhCTemp', 'PhNTemp'])),
    chart('swopscnt', '开关动作次数', 14, modelSeries('CBP4', ['SwOpsCnt'])),
    chart('phase-energy', '各相电量', 15, modelSeries('CBP4', ['EnergyA', 'EnergyB', 'EnergyC'])),
    chart('forward-active-energy', '正向有功电能', 18, modelSeries('CBP4', ['FwdActET2'])),
    chart('forward-reactive-energy', '正向无功电能', 19, modelSeries('CBP4', ['FwdRctET2'])),
    chart('reverse-active-energy', '反向有功电能', 20, modelSeries('CBP4', ['RevActET2'])),
    chart('reverse-reactive-energy', '反向无功电能', 21, modelSeries('CBP4', ['RevRctET2'])),
    chart('back-temperature', '后端温度', 22, modelSeries('CBP4', ['PhATmpBk', 'PhBTmpBk', 'PhCTmpBk', 'PhNTmpBk'])),
    chart('currtot', '总电流', 23, modelSeries('CBP4', ['CurrTot']))
  ],
  CBP3: [
    chart('line-voltage', '线电压', 1, modelSeries('CBP3', ['LineVoltAB', 'LineVoltAC', 'LineVoltBC'])),
    chart('phase-voltage', '相电压', 2, modelSeries('CBP3', ['PhAVolt', 'PhBVolt', 'PhCVolt'])),
    chart('zeroseqv', '零序电压', 3, modelSeries('CBP3', ['ZeroSeqV'])),
    chart('phase-current', '相电流', 4, modelSeries('CBP3', ['PhACurr', 'PhBCurr', 'PhCCurr'])),
    chart('leakvalue', '漏电值', 5, modelSeries('CBP3', ['LeakValue'])),
    chart('active-power', '有功功率', 6, modelSeries('CBP3', ['ActPwrTot', 'ActPwrA', 'ActPwrB', 'ActPwrC'])),
    chart('reactive-power', '无功功率', 7, modelSeries('CBP3', ['RctPwrTot', 'RctPwrA', 'RctPwrB', 'RctPwrC'])),
    chart('power-factor', '功率因数', 8, modelSeries('CBP3', ['PwrFactT', 'PwrFactA', 'PwrFactB', 'PwrFactC'])),
    chart('totenergy', '总电量', 9, modelSeries('CBP3', ['TotEnergy'])),
    chart('temperature', '温度', 10, modelSeries('CBP3', ['PhATemp', 'PhBTemp', 'PhCTemp', 'PhNTemp'])),
    chart('swopscnt', '开关动作次数', 14, modelSeries('CBP3', ['SwOpsCnt'])),
    chart('phase-energy', '各相电量', 15, modelSeries('CBP3', ['EnergyA', 'EnergyB', 'EnergyC'])),
    chart('forward-active-energy', '正向有功电能', 18, modelSeries('CBP3', ['FwdActET2'])),
    chart('forward-reactive-energy', '正向无功电能', 19, modelSeries('CBP3', ['FwdRctET2'])),
    chart('reverse-active-energy', '反向有功电能', 20, modelSeries('CBP3', ['RevActET2'])),
    chart('reverse-reactive-energy', '反向无功电能', 21, modelSeries('CBP3', ['RevRctET2'])),
    chart('back-temperature', '后端温度', 22, modelSeries('CBP3', ['PhATmpBk', 'PhBTmpBk', 'PhCTmpBk', 'PhNTmpBk'])),
    chart('currtot', '总电流', 23, modelSeries('CBP3', ['CurrTot']))
  ],
  CBP2: [
    chart('line-voltage', '线电压', 1, modelSeries('CBP2', ['LineVoltAB', 'LineVoltAC', 'LineVoltBC'])),
    chart('phase-voltage', '相电压', 2, modelSeries('CBP2', ['PhAVolt', 'PhBVolt', 'PhCVolt'])),
    chart('zeroseqv', '零序电压', 3, modelSeries('CBP2', ['ZeroSeqV'])),
    chart('phase-current', '相电流', 4, modelSeries('CBP2', ['PhACurr', 'PhBCurr', 'PhCCurr'])),
    chart('leakvalue', '漏电值', 5, modelSeries('CBP2', ['LeakValue'])),
    chart('active-power', '有功功率', 6, modelSeries('CBP2', ['ActPwrTot', 'ActPwrA', 'ActPwrB', 'ActPwrC'])),
    chart('reactive-power', '无功功率', 7, modelSeries('CBP2', ['RctPwrTot', 'RctPwrA', 'RctPwrB', 'RctPwrC'])),
    chart('power-factor', '功率因数', 8, modelSeries('CBP2', ['PwrFactT', 'PwrFactA', 'PwrFactB', 'PwrFactC'])),
    chart('totenergy', '总电量', 9, modelSeries('CBP2', ['TotEnergy'])),
    chart('temperature', '温度', 10, modelSeries('CBP2', ['PhATemp', 'PhBTemp', 'PhCTemp', 'PhNTemp'])),
    chart('swopscnt', '开关动作次数', 14, modelSeries('CBP2', ['SwOpsCnt'])),
    chart('phase-energy', '各相电量', 15, modelSeries('CBP2', ['EnergyA', 'EnergyB', 'EnergyC'])),
    chart('forward-active-energy', '正向有功电能', 18, modelSeries('CBP2', ['FwdActET2'])),
    chart('forward-reactive-energy', '正向无功电能', 19, modelSeries('CBP2', ['FwdRctET2'])),
    chart('reverse-active-energy', '反向有功电能', 20, modelSeries('CBP2', ['RevActET2'])),
    chart('reverse-reactive-energy', '反向无功电能', 21, modelSeries('CBP2', ['RevRctET2'])),
    chart('back-temperature', '后端温度', 22, modelSeries('CBP2', ['PhATmpBk', 'PhBTmpBk', 'PhCTmpBk', 'PhNTmpBk'])),
    chart('currtot', '总电流', 23, modelSeries('CBP2', ['CurrTot']))
  ],
  CBP1: [
    chart('line-voltage', '线电压', 1, modelSeries('CBP1', ['LineVoltAB', 'LineVoltAC', 'LineVoltBC'])),
    chart('phase-voltage', '相电压', 2, modelSeries('CBP1', ['PhAVolt', 'PhBVolt', 'PhCVolt'])),
    chart('zeroseqv', '零序电压', 3, modelSeries('CBP1', ['ZeroSeqV'])),
    chart('phase-current', '相电流', 4, modelSeries('CBP1', ['PhACurr', 'PhBCurr', 'PhCCurr'])),
    chart('leakvalue', '漏电值', 5, modelSeries('CBP1', ['LeakValue'])),
    chart('active-power', '有功功率', 6, modelSeries('CBP1', ['ActPwrTot', 'ActPwrA', 'ActPwrB', 'ActPwrC'])),
    chart('reactive-power', '无功功率', 7, modelSeries('CBP1', ['RctPwrTot', 'RctPwrA', 'RctPwrB', 'RctPwrC'])),
    chart('power-factor', '功率因数', 8, modelSeries('CBP1', ['PwrFactT', 'PwrFactA', 'PwrFactB', 'PwrFactC'])),
    chart('totenergy', '总电量', 9, modelSeries('CBP1', ['TotEnergy'])),
    chart('temperature', '温度', 10, modelSeries('CBP1', ['PhATemp', 'PhBTemp', 'PhCTemp', 'PhNTemp'])),
    chart('swopscnt', '开关动作次数', 14, modelSeries('CBP1', ['SwOpsCnt'])),
    chart('phase-energy', '各相电量', 15, modelSeries('CBP1', ['EnergyA', 'EnergyB', 'EnergyC'])),
    chart('forward-active-energy', '正向有功电能', 18, modelSeries('CBP1', ['FwdActET2'])),
    chart('forward-reactive-energy', '正向无功电能', 19, modelSeries('CBP1', ['FwdRctET2'])),
    chart('reverse-active-energy', '反向有功电能', 20, modelSeries('CBP1', ['RevActET2'])),
    chart('reverse-reactive-energy', '反向无功电能', 21, modelSeries('CBP1', ['RevRctET2'])),
    chart('back-temperature', '后端温度', 22, modelSeries('CBP1', ['PhATmpBk', 'PhBTmpBk', 'PhCTmpBk', 'PhNTmpBk'])),
    chart('currtot', '总电流', 23, modelSeries('CBP1', ['CurrTot']))
  ],
  LDA1: [],
  E1B2: [
    chart('phase-voltage', '相电压', 1, modelSeries('E1B2', ['PhAVolt', 'PhBVolt', 'PhCVolt'])),
    chart('line-voltage', '线电压', 2, modelSeries('E1B2', ['LineVoltAB', 'LineVoltBC', 'LineVoltCA'])),
    chart('freq', '频率', 3, modelSeries('E1B2', ['Freq'])),
    chart('phase-current', '相电流', 4, modelSeries('E1B2', ['PhACurr', 'PhBCurr', 'PhCCurr'])),
    chart('currvecsum', '三相电流矢量和', 5, modelSeries('E1B2', ['CurrVecSum'])),
    chart('active-power', '有功功率', 6, modelSeries('E1B2', ['ActPwrA', 'ActPwrB', 'ActPwrC', 'ActPwrTot'])),
    chart('reactive-power', '无功功率', 7, modelSeries('E1B2', ['RctPwrA', 'RctPwrB', 'RctPwrC', 'RctPwrTot'])),
    chart('apparent-power', '视在功率', 8, modelSeries('E1B2', ['AppPwrA', 'AppPwrB', 'AppPwrC', 'AppPwrTot'])),
    chart('power-factor', '功率因数', 9, modelSeries('E1B2', ['PwrFactA', 'PwrFactB', 'PwrFactC', 'PwrFactT'])),
    chart('forward-active-energy', '正向有功电能', 10, modelSeries('E1B2', ['FwdActEA', 'FwdActEB', 'FwdActEC', 'FwdActET'])),
    chart('voltage-angle', '电压相角', 11, modelSeries('E1B2', ['VoltAngA', 'VoltAngB', 'VoltAngC'])),
    chart('current-angle', '电流相角', 12, modelSeries('E1B2', ['CurrAngA', 'CurrAngB', 'CurrAngC'])),
    chart('voltage-deviation', '相电压偏差', 13, modelSeries('E1B2', ['VoltDevA', 'VoltDevB', 'VoltDevC'])),
    chart('line-deviation', '线电压偏差', 14, modelSeries('E1B2', ['LineDevAB', 'LineDevBC', 'LineDevCA'])),
    chart('freqdev', '频率偏差', 15, modelSeries('E1B2', ['FreqDev'])),
    chart('voltage-unbalance', '电压不平衡度', 16, modelSeries('E1B2', ['ZSeqVUnb', 'NSeqVUnb'])),
    chart('current-unbalance', '电流不平衡度', 17, modelSeries('E1B2', ['ZSeqIUnb', 'NSeqIUnb']))
  ],
  E1P1: [
    chart('ug', '零地电压', 1, modelSeries('E1P1', ['UG']))
  ],
  E1E2: [
    chart('phase-voltage', '相电压', 1, modelSeries('E1E2', ['PhAVolt', 'PhBVolt', 'PhCVolt'])),
    chart('line-voltage', '线电压', 2, modelSeries('E1E2', ['LineVoltAB', 'LineVoltBC', 'LineVoltCA'])),
    chart('freq', '频率', 3, modelSeries('E1E2', ['Freq'])),
    chart('phase-current', '相电流', 4, modelSeries('E1E2', ['PhACurr', 'PhBCurr', 'PhCCurr'])),
    chart('currvecsum', '三相电流矢量和', 5, modelSeries('E1E2', ['CurrVecSum'])),
    chart('active-power', '有功功率', 6, modelSeries('E1E2', ['ActPwrA', 'ActPwrB', 'ActPwrC', 'ActPwrTot'])),
    chart('reactive-power', '无功功率', 7, modelSeries('E1E2', ['RctPwrA', 'RctPwrB', 'RctPwrC', 'RctPwrTot'])),
    chart('apparent-power', '视在功率', 8, modelSeries('E1E2', ['AppPwrA', 'AppPwrB', 'AppPwrC', 'AppPwrTot'])),
    chart('power-factor', '功率因数', 9, modelSeries('E1E2', ['PwrFactA', 'PwrFactB', 'PwrFactC', 'PwrFactT'])),
    chart('forward-active-energy', '正向有功电能', 10, modelSeries('E1E2', ['FwdActEA', 'FwdActEB', 'FwdActEC', 'FwdActET'])),
    chart('voltage-angle', '电压相角', 11, modelSeries('E1E2', ['VoltAngA', 'VoltAngB', 'VoltAngC'])),
    chart('current-angle', '电流相角', 12, modelSeries('E1E2', ['CurrAngA', 'CurrAngB', 'CurrAngC'])),
    chart('voltage-deviation', '相电压偏差', 13, modelSeries('E1E2', ['VoltDevA', 'VoltDevB', 'VoltDevC'])),
    chart('line-deviation', '线电压偏差', 14, modelSeries('E1E2', ['LineDevAB', 'LineDevBC', 'LineDevCA'])),
    chart('freqdev', '频率偏差', 15, modelSeries('E1E2', ['FreqDev'])),
    chart('voltage-unbalance', '电压不平衡度', 16, modelSeries('E1E2', ['ZSeqVUnb', 'NSeqVUnb'])),
    chart('current-unbalance', '电流不平衡度', 17, modelSeries('E1E2', ['ZSeqIUnb', 'NSeqIUnb'])),
    chart('voltage-thd', '电压总谐波畸变率', 18, modelSeries('E1E2', ['VoltThdA', 'VoltThdB', 'VoltThdC'])),
    chart('current-thd', '电流总谐波畸变率', 19, modelSeries('E1E2', ['CurrThdA', 'CurrThdB', 'CurrThdC'])),
    chart('voltage-harmonic-2', '电压2次谐波含有率', 20, modelSeries('E1E2', ['VoltH2A', 'VoltH2B', 'VoltH2C'])),
    chart('voltage-harmonic-3', '电压3次谐波含有率', 21, modelSeries('E1E2', ['VoltH3A', 'VoltH3B', 'VoltH3C'])),
    chart('voltage-harmonic-4', '电压4次谐波含有率', 22, modelSeries('E1E2', ['VoltH4A', 'VoltH4B', 'VoltH4C'])),
    chart('voltage-harmonic-5', '电压5次谐波含有率', 23, modelSeries('E1E2', ['VoltH5A', 'VoltH5B', 'VoltH5C'])),
    chart('voltage-harmonic-6', '电压6次谐波含有率', 24, modelSeries('E1E2', ['VoltH6A', 'VoltH6B', 'VoltH6C'])),
    chart('voltage-harmonic-7', '电压7次谐波含有率', 25, modelSeries('E1E2', ['VoltH7A', 'VoltH7B', 'VoltH7C'])),
    chart('voltage-harmonic-8', '电压8次谐波含有率', 26, modelSeries('E1E2', ['VoltH8A', 'VoltH8B', 'VoltH8C'])),
    chart('voltage-harmonic-9', '电压9次谐波含有率', 27, modelSeries('E1E2', ['VoltH9A', 'VoltH9B', 'VoltH9C'])),
    chart('voltage-harmonic-10', '电压10次谐波含有率', 28, modelSeries('E1E2', ['VoltH10A', 'VoltH10B', 'VoltH10C'])),
    chart('voltage-harmonic-11', '电压11次谐波含有率', 29, modelSeries('E1E2', ['VoltH11A', 'VoltH11B', 'VoltH11C'])),
    chart('voltage-harmonic-12', '电压12次谐波含有率', 30, modelSeries('E1E2', ['VoltH12A', 'VoltH12B', 'VoltH12C'])),
    chart('voltage-harmonic-13', '电压13次谐波含有率', 31, modelSeries('E1E2', ['VoltH13A', 'VoltH13B', 'VoltH13C'])),
    chart('voltage-harmonic-14', '电压14次谐波含有率', 32, modelSeries('E1E2', ['VoltH14A', 'VoltH14B', 'VoltH14C'])),
    chart('voltage-harmonic-15', '电压15次谐波含有率', 33, modelSeries('E1E2', ['VoltH15A', 'VoltH15B', 'VoltH15C'])),
    chart('voltage-harmonic-16', '电压16次谐波含有率', 34, modelSeries('E1E2', ['VoltH16A', 'VoltH16B', 'VoltH16C'])),
    chart('voltage-harmonic-17', '电压17次谐波含有率', 35, modelSeries('E1E2', ['VoltH17A', 'VoltH17B', 'VoltH17C'])),
    chart('voltage-harmonic-18', '电压18次谐波含有率', 36, modelSeries('E1E2', ['VoltH18A', 'VoltH18B', 'VoltH18C'])),
    chart('voltage-harmonic-19', '电压19次谐波含有率', 37, modelSeries('E1E2', ['VoltH19A', 'VoltH19B', 'VoltH19C'])),
    chart('voltage-harmonic-20', '电压20次谐波含有率', 38, modelSeries('E1E2', ['VoltH20A', 'VoltH20B', 'VoltH20C'])),
    chart('voltage-harmonic-21', '电压21次谐波含有率', 39, modelSeries('E1E2', ['VoltH21A', 'VoltH21B', 'VoltH21C'])),
    chart('current-harmonic-2', '电流2次谐波含有率', 40, modelSeries('E1E2', ['CurrH2A', 'CurrH2B', 'CurrH2C'])),
    chart('current-harmonic-3', '电流3次谐波含有率', 41, modelSeries('E1E2', ['CurrH3A', 'CurrH3B', 'CurrH3C'])),
    chart('current-harmonic-4', '电流4次谐波含有率', 42, modelSeries('E1E2', ['CurrH4A', 'CurrH4B', 'CurrH4C'])),
    chart('current-harmonic-5', '电流5次谐波含有率', 43, modelSeries('E1E2', ['CurrH5A', 'CurrH5B', 'CurrH5C'])),
    chart('current-harmonic-6', '电流6次谐波含有率', 44, modelSeries('E1E2', ['CurrH6A', 'CurrH6B', 'CurrH6C'])),
    chart('current-harmonic-7', '电流7次谐波含有率', 45, modelSeries('E1E2', ['CurrH7A', 'CurrH7B', 'CurrH7C'])),
    chart('current-harmonic-8', '电流8次谐波含有率', 46, modelSeries('E1E2', ['CurrH8A', 'CurrH8B', 'CurrH8C'])),
    chart('current-harmonic-9', '电流9次谐波含有率', 47, modelSeries('E1E2', ['CurrH9A', 'CurrH9B', 'CurrH9C'])),
    chart('current-harmonic-10', '电流10次谐波含有率', 48, modelSeries('E1E2', ['CurrH10A', 'CurrH10B', 'CurrH10C'])),
    chart('current-harmonic-11', '电流11次谐波含有率', 49, modelSeries('E1E2', ['CurrH11A', 'CurrH11B', 'CurrH11C'])),
    chart('current-harmonic-12', '电流12次谐波含有率', 50, modelSeries('E1E2', ['CurrH12A', 'CurrH12B', 'CurrH12C'])),
    chart('current-harmonic-13', '电流13次谐波含有率', 51, modelSeries('E1E2', ['CurrH13A', 'CurrH13B', 'CurrH13C'])),
    chart('current-harmonic-14', '电流14次谐波含有率', 52, modelSeries('E1E2', ['CurrH14A', 'CurrH14B', 'CurrH14C'])),
    chart('current-harmonic-15', '电流15次谐波含有率', 53, modelSeries('E1E2', ['CurrH15A', 'CurrH15B', 'CurrH15C'])),
    chart('current-harmonic-16', '电流16次谐波含有率', 54, modelSeries('E1E2', ['CurrH16A', 'CurrH16B', 'CurrH16C'])),
    chart('current-harmonic-17', '电流17次谐波含有率', 55, modelSeries('E1E2', ['CurrH17A', 'CurrH17B', 'CurrH17C'])),
    chart('current-harmonic-18', '电流18次谐波含有率', 56, modelSeries('E1E2', ['CurrH18A', 'CurrH18B', 'CurrH18C'])),
    chart('current-harmonic-19', '电流19次谐波含有率', 57, modelSeries('E1E2', ['CurrH19A', 'CurrH19B', 'CurrH19C'])),
    chart('current-harmonic-20', '电流20次谐波含有率', 58, modelSeries('E1E2', ['CurrH20A', 'CurrH20B', 'CurrH20C'])),
    chart('current-harmonic-21', '电流21次谐波含有率', 59, modelSeries('E1E2', ['CurrH21A', 'CurrH21B', 'CurrH21C']))
  ],
  SFE: [],
  SFA: [
    chart('phase-voltage', '相电压', 1, modelSeries('SFA', ['PhAVolt', 'PhBVolt', 'PhCVolt'])),
    chart('line-voltage', '线电压', 2, modelSeries('SFA', ['LineVoltAB', 'LineVoltBC', 'LineVoltCA'])),
    chart('freq', '频率', 3, modelSeries('SFA', ['Freq'])),
    chart('phase-current', '相电流', 4, modelSeries('SFA', ['PhACurr', 'PhBCurr', 'PhCCurr'])),
    chart('currvecsum', '三相电流矢量和', 5, modelSeries('SFA', ['CurrVecSum'])),
    chart('residual-current', '剩余电流', 6, modelSeries('SFA', ['ResCurr'])),
    chart('active-power', '有功功率', 7, modelSeries('SFA', ['ActPwrA', 'ActPwrB', 'ActPwrC', 'ActPwrTot'])),
    chart('reactive-power', '无功功率', 8, modelSeries('SFA', ['RctPwrA', 'RctPwrB', 'RctPwrC', 'RctPwrTot'])),
    chart('apparent-power', '视在功率', 9, modelSeries('SFA', ['AppPwrA', 'AppPwrB', 'AppPwrC', 'AppPwrTot'])),
    chart('power-factor', '功率因数', 10, modelSeries('SFA', ['PwrFactA', 'PwrFactB', 'PwrFactC', 'PwrFactT'])),
    chart('forward-active-energy', '正向有功电能', 11, modelSeries('SFA', ['FwdActEA', 'FwdActEB', 'FwdActEC', 'FwdActET'])),
    chart('reverse-reactive-energy', '反向无功电能', 12, modelSeries('SFA', ['RevRctEA', 'RevRctEB', 'RevRctEC', 'RevRctET'])),
    chart('forward-reactive-energy', '正向无功电能', 13, modelSeries('SFA', ['FwdRctEA', 'FwdRctEB', 'FwdRctEC', 'FwdRctET']))
  ]
};

/** 返回型号对应的完整趋势职责表，未知型号不借用其他型号的曲线。 */
export function getElectricalSafetyChartDefinitions(
  deviceProfileName?: string | null
): ElectricalSafetyDetailChartDefinition[] {
  const model = resolveElectricalSafetyDeviceModel(deviceProfileName);
  return model
    ? ELECTRICAL_SAFETY_CHART_DEFINITIONS[model].map((definition) => ({
      ...definition,
      series: [...definition.series]
    }))
    : [];
}

/** 只保留设备实际拥有的时序键，并按优先级限制首屏图卡数量。 */
export function filterElectricalSafetyChartDefinitions(
  definitions: readonly ElectricalSafetyDetailChartDefinition[],
  availableKeys: readonly string[],
  maxCharts = 4
): ElectricalSafetyDetailChartDefinition[] {
  const available = new Set(availableKeys);
  return definitions
    .slice()
    .sort((left, right) => left.priority - right.priority)
    .map((definition) => ({
      ...definition,
      series: definition.series.filter((item) => available.has(item.key))
    }))
    .filter((definition) => definition.series.length > 0)
    .slice(0, maxCharts);
}

/** 汇总指定图表所需的时序键，去重后供单次历史接口请求使用。 */
export function buildElectricalSafetyChartReadKeys(
  definitions: readonly ElectricalSafetyDetailChartDefinition[]
): string[] {
  const keys = new Set<string>();
  for (const definition of definitions) {
    for (const item of definition.series) {
      keys.add(item.key);
    }
  }
  return [...keys];
}

/** 把 ThingsBoard 历史时序转换为排序后的数值点，忽略空值及非法时间/数值。 */
export function buildElectricalSafetyChartSeriesData(
  definition: ElectricalSafetyDetailChartDefinition,
  timeseries: TimeseriesData
): ElectricalSafetyDetailChartSeries[] {
  const result: ElectricalSafetyDetailChartSeries[] = [];
  for (const item of definition.series) {
    const samples = Array.isArray(timeseries[item.key]) ? timeseries[item.key] : [];
    const points = samples
      .filter((sample) => sample.value !== null && sample.value !== undefined && String(sample.value).trim() !== '')
      .map((sample) => [Number(sample.ts), Number(sample.value)] as [number, number])
      .filter(([timestamp, value]) => Number.isFinite(timestamp) && Number.isFinite(value))
      .sort((left, right) => left[0] - right[0]);
    if (points.length) {
      result.push({definition: item, points});
    }
  }
  return result;
}

/** 按防雷趋势图的真实数据留白规则计算纵轴边界。 */
export function buildElectricalSafetyYAxisBounds(
  seriesData: readonly ElectricalSafetyDetailChartSeries[]
): {min: number; max: number} | undefined {
  const values: number[] = [];
  for (const item of seriesData) {
    for (const point of item.points) {
      if (Number.isFinite(point[1])) {
        values.push(point[1]);
      }
    }
  }
  if (!values.length) {
    return undefined;
  }
  const rawMin = Math.min(...values);
  const rawMax = Math.max(...values);
  const range = rawMax - rawMin;
  const padding = range > 0 ? range * 0.1 : Math.max(Math.abs(rawMax) * 0.1, 1);
  return {
    min: rawMin > 0 ? Math.max(Number.EPSILON, rawMin - padding) : rawMin - padding,
    max: rawMax < 0 ? Math.min(-Number.EPSILON, rawMax + padding) : rawMax + padding
  };
}

/** 统一转发详情页周期窗口，防止日/周/月的时间口径出现两套实现。 */
export function getElectricalSafetyChartWindow(
  period: ElectricalSafetyDetailPeriod,
  now = Date.now()
): {startTs: number; endTs: number} {
  return getElectricalSafetyDetailWindow(period, now);
}
