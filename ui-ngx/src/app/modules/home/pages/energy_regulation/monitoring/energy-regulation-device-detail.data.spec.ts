import assert from 'node:assert/strict';
import {ENERGY_METER_MEASUREMENTS} from '../shared/energy-regulation-measurements';
import {ENERGY_DETAIL_CHARTS, ENERGY_METER_SPECIFICATION, formatEnergyDetailValue,
  getEnergyDetailWindow, mergeEnergyDetailSamples, buildEnergyDetailChartPoints} from './energy-regulation-device-detail.data';

// 每个数值测点恰好归属一组图，状态量不混入数值曲线。
const keys = ENERGY_DETAIL_CHARTS.flatMap((chart) => chart.keys);
assert.deepEqual([...keys].sort(), ENERGY_METER_MEASUREMENTS.filter((item) => !item.status).map((item) => item.key).sort());
assert.equal(new Set(keys).size, keys.length);
assert.equal(formatEnergyDetailValue(0, false, 'kW'), '0 kW');
assert.equal(formatEnergyDetailValue(undefined, false, 'kW'), '__');
assert.equal(formatEnergyDetailValue(1, true, ''), '报警');
assert.equal(formatEnergyDetailValue(0, true, ''), '正常');
assert.equal(formatEnergyDetailValue(6553.5, false, 'V'), '6553.5 V');
assert.ok(ENERGY_METER_SPECIFICATION.some((item) => item.label === '工作电压' && item.value.includes('180')));
const samples = new Map<string, {ts: number; value: unknown}>();
mergeEnergyDetailSamples(samples, {PhAVolt: [{ts: 20, value: 230}, {ts: 10, value: 220}]});
mergeEnergyDetailSamples(samples, {PhAVolt: [{ts: 15, value: 225}]});
assert.equal(samples.get('PhAVolt')?.value, 230);
assert.equal(buildEnergyDetailChartPoints([{ts: 2, value: '0'}, {ts: 1, value: '3.5'}, {ts: 3, value: ''}, {ts: 4, value: 'bad'}]).length, 2);
assert.deepEqual(buildEnergyDetailChartPoints([{ts: 2, value: '0'}, {ts: 1, value: '3.5'}]), [[1, 3.5], [2, 0]]);
const now = new Date(2026, 9, 26, 16, 0).getTime();
assert.equal(getEnergyDetailWindow('day', now).startTs, new Date(2026, 9, 26).getTime());
assert.equal(getEnergyDetailWindow('week', now).startTs, now - 7 * 86400000);
assert.equal(getEnergyDetailWindow('month', now).startTs, now - 30 * 86400000);
assert.equal(getEnergyDetailWindow('month', now).endTs, now);
console.log('能源设备详情数据用例通过');
