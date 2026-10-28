import assert from 'node:assert/strict';
import {
  DEMO_ENERGY_MAP_DEVICES,
  DEMO_ENERGY_REGULATION_DASHBOARD_DATA,
  INITIAL_ENERGY_REGULATION_PERIODS,
  getEnergyConsumptionForPeriod,
  setEnergyRegulationPeriod
} from './energy-regulation-dashboard.data';

// 更新用能统计周期时，趋势图周期应保持不变，且不能修改原周期对象。
const updated = setEnergyRegulationPeriod({...INITIAL_ENERGY_REGULATION_PERIODS, alarms: 'month'}, 'consumption', 'week');

assert.deepEqual(updated, {consumption: 'week', energy: 'day', alarms: 'month', carbon: 'day'});
assert.deepEqual(INITIAL_ENERGY_REGULATION_PERIODS, {consumption: 'day', energy: 'day', alarms: 'day', carbon: 'day'});

// 地图演示点必须含共享地图渲染所需的名称、类别和合法坐标。
assert.ok(DEMO_ENERGY_MAP_DEVICES.length > 0);
for (const device of DEMO_ENERGY_MAP_DEVICES) {
  assert.ok(device.name.length > 0);
  assert.equal(device.productCategory, 'energy_regulation');
  assert.ok(device.latitude >= -90 && device.latitude <= 90);
  assert.ok(device.longitude >= -180 && device.longitude <= 180);
}

// 图表各周期都必须有可绘制的数据，空数组会造成看板与样例的落差。
for (const period of ['day', 'week', 'month'] as const) {
  assert.ok(DEMO_ENERGY_REGULATION_DASHBOARD_DATA.energySeries[period].values.length > 0);
  assert.ok(DEMO_ENERGY_REGULATION_DASHBOARD_DATA.alarmSeries[period].labels.length > 0);
  assert.ok(DEMO_ENERGY_REGULATION_DASHBOARD_DATA.carbonSeries[period].values.length > 0);
}

// 用能统计卡必须分别提供日、周、月汇总值，不能始终显示日用电数据。
const weeklyConsumption = getEnergyConsumptionForPeriod(DEMO_ENERGY_REGULATION_DASHBOARD_DATA, 'week');
assert.equal(weeklyConsumption.label, '本周用电量');
assert.equal(weeklyConsumption.valueKwh, 13268.4);
