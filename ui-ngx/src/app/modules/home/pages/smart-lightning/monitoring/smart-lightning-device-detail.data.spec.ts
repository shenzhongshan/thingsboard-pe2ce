/** 文件职责：数据监测模块的 smart-lightning-device-detail.data.spec，验证数据模型、映射与纯函数行为。 */
/** 设备详情数据层单测：实时指标映射、模拟曲线与时间轴构造（node assert 轻量运行）。 */
import assert from 'node:assert/strict';
import { buildSmartLightningDeviceDetailMock, buildSmartLightningChartTimes, fitSmartLightningMockSeries } from './smart-lightning-device-detail.data.ts';
import { buildSmartLightningRealtimeMetrics, getSmartLightningRealtimeMetricDefinitions } from './smart-lightning-device-detail.data.ts';
import { getSmartLightningProductSpecification } from './smart-lightning-product-specifications.ts';

const detail = buildSmartLightningDeviceDetailMock('1234567890');
assert.equal(detail.title, '设备监测 · 12345678');
assert.equal(detail.metrics.length, 12);
assert.deepEqual(getSmartLightningRealtimeMetricDefinitions('DESA').map((metric) => metric.key), [
  'breakerStatus', 'groundStatus', 'voltageA', 'voltageB', 'voltageC', 'leakageA', 'leakageB', 'leakageC', 'lifePercent',
  'lightningCount', 'spdStatus', 'temperatureA', 'temperatureB'
]);
const desaMetrics = buildSmartLightningRealtimeMetrics('DESA', new Map([['U1', 220.5], ['temperatureA', 29.6]]), detail.metrics);
assert.equal(desaMetrics.find((metric) => metric.label === 'A相电压')?.value, '220.5V');
assert.equal(desaMetrics.find((metric) => metric.label === 'A相温度')?.value, '29.6℃');
assert.equal(desaMetrics.find((metric) => metric.label === 'B相温度')?.value, '23.1℃');
// DES9 的 J 是雷击次数，不是 0/1 状态。
const des9Metrics = buildSmartLightningRealtimeMetrics('DES9', new Map([['J', 12]]), []);
assert.equal(des9Metrics.find((metric) => metric.label === '雷击次数')?.value, '12次');
// 详情趋势图的时间轴以当前时间为基准，日/周/月分别生成不同粒度的时间点。
const chartNow = new Date(2026, 8, 4, 12, 30);
const dayTimes = buildSmartLightningChartTimes('day', chartNow);
assert.equal(dayTimes.length, 24);
assert.equal(dayTimes.at(-1), '09/04 12:30');
assert.equal(buildSmartLightningChartTimes('week', chartNow).length, 7);
assert.equal(buildSmartLightningChartTimes('month', chartNow).length, 30);
// 扩展到不同时间粒度时使用线性插值，避免 mock 曲线出现重复平台。
assert.deepEqual(fitSmartLightningMockSeries([0, 10], 5), [0, 2.5, 5, 7.5, 10]);
// 告警由接口加载，详情初始模型不再预置演示记录。
assert.equal(detail.alarms.length, 0);

// 规则链未输出规范键时，页面也必须能够读取原始测点并完成中文状态转换。
const ldh1Definitions = getSmartLightningRealtimeMetricDefinitions('LDH1');
assert.deepEqual(ldh1Definitions.map((metric) => metric.key), ['deviceStatus', 'arcFaultCount']);
const ldh1Metrics = buildSmartLightningRealtimeMetrics(
  'LDH1',
  new Map<string, unknown>([['ALM', 1], ['AJ', 7]]),
  []
);
assert.equal(ldh1Metrics.find((metric) => metric.label === '设备状态')?.value, '报警');
assert.equal(ldh1Metrics.find((metric) => metric.label === '故障电弧次数')?.value, '7次');

// 状态键统一使用 0=正常、1=报警；数值型测点保留单位。
const g1a3Metrics = buildSmartLightningRealtimeMetrics(
  'G1A3',
  new Map<string, unknown>([['DR', 0.35], ['DJ', 8], ['MIN', 15]]),
  []
);
assert.equal(g1a3Metrics.find((metric) => metric.label === '地网阻值')?.value, '0.35Ω');
assert.equal(g1a3Metrics.find((metric) => metric.label === '采集周期')?.value, '15min');

const esm3Metrics = buildSmartLightningRealtimeMetrics(
  'ESM3',
  new Map<string, unknown>([['Y', 1], ['U1', 220], ['I1', 3.5], ['T1', 26]]),
  []
);
assert.equal(esm3Metrics.find((metric) => metric.label === '防雷器状态')?.value, '报警');
assert.equal(esm3Metrics.find((metric) => metric.label === 'A相电压')?.value, '220V');
assert.equal(esm3Metrics.find((metric) => metric.label === 'A相漏流')?.value, '3.5mA');
assert.equal(esm3Metrics.find((metric) => metric.label === '环境温度')?.value, '26℃');

// 雷电设备规则链输出规范键时，详情页应直接读取 surgePeak/surgeCount/surgePolarity。
const m1aiMetrics = buildSmartLightningRealtimeMetrics(
  'M1AI',
  new Map<string, unknown>([['surgePeak', 125.4], ['surgeCount', 5], ['surgePolarity', 1]]),
  []
);
assert.equal(m1aiMetrics.find((metric) => metric.label === '雷电流峰值')?.value, '125.4kA');
assert.equal(m1aiMetrics.find((metric) => metric.label === '电涌次数')?.value, '5次');
assert.equal(m1aiMetrics.find((metric) => metric.label === '极性')?.value, '正极');

// 设备详情左上摘要卡片按设备配置显示说明书中的静态产品参数。
assert.equal(getSmartLightningProductSpecification('DESA')?.model, 'FS-33211');
assert.match(getSmartLightningProductSpecification('G1A3')?.items.find((item) => item.label === '阻值范围')?.value || '', /200/);
assert.equal(getSmartLightningProductSpecification('FAP1'), undefined);

for (const profile of ['DESA', 'DES4', 'DES9', 'DEM2', 'DEM4', 'DEA2', 'DEA4', 'ESM0', 'ESM3', 'G1A3', 'M1AI', 'L2AI', 'LDH1', 'FAP1']) {
  assert.ok(getSmartLightningRealtimeMetricDefinitions(profile).length > 0, `${profile} must have metric mappings`);
}
