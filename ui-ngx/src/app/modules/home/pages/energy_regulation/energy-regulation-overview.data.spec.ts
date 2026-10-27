import assert from 'node:assert/strict';
import {formatEnergyOverviewChartValue, subtractEnergyOverviewValues} from './energy-regulation-overview.data';

// 缺数及图表空值标记不应泄露为undefined/null文本，也不能被转成有效零值。
for (const unit of ['kWh', 'kW'] as const) {
  for (const value of [undefined, null, NaN, Infinity, -Infinity, '', '-']) {
    assert.equal(formatEnergyOverviewChartValue(value, unit), '__');
  }
  assert.equal(formatEnergyOverviewChartValue(0, unit), `0 ${unit}`);
  assert.equal(formatEnergyOverviewChartValue(3.45, unit), `3.45 ${unit}`);
}

// 取消展示精度限制后，保留原小数，不引入二进制减法的长尾噪声。
assert.equal(formatEnergyOverviewChartValue(0.115, 'kWh'), '0.115 kWh');
assert.equal(formatEnergyOverviewChartValue(1200.123456789, 'kWh'), '1200.123456789 kWh');
assert.equal(subtractEnergyOverviewValues(1200.115, 1200), 0.115);
assert.equal(subtractEnergyOverviewValues(0.3, 0.1), 0.2);
assert.equal(subtractEnergyOverviewValues(1.2e-7, 1e-7), 2e-8);
assert.equal(subtractEnergyOverviewValues(1200, 1200.115), -0.115);
assert.equal(subtractEnergyOverviewValues(null, 1200), null);
