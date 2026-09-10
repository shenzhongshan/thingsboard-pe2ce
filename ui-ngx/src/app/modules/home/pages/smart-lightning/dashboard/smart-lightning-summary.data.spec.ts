/** 文件职责：看板模块的 smart-lightning-summary.data.spec，验证数据模型、映射与纯函数行为。 */
/** 摘要格式化函数单测：空值占位与时间格式化（node assert 轻量运行）。 */
import assert from 'node:assert/strict';
import {
  buildSmartLightningMaximumTooltip,
  calculateLightningCounterDelta,
  formatSmartLightningMetric,
  getMaximumFiniteValue
} from './smart-lightning-summary.data.ts';

assert.equal(formatSmartLightningMetric(null, 'mA'), '--');
assert.equal(formatSmartLightningMetric(18.4, 'mA'), '18.40 mA');
assert.equal(formatSmartLightningMetric(0, 'mA'), '0.00 mA');
assert.equal(getMaximumFiniteValue([null, 3.5, Number.NaN, 8.2, undefined]), 8.2);
assert.equal(getMaximumFiniteValue([null, Number.NaN]), null);

assert.equal(calculateLightningCounterDelta(12, 15), 3);
assert.equal(calculateLightningCounterDelta(15, 15), 0);
// 计数器回退表示设备重启或归零，本次增量按当前计数处理。
assert.equal(calculateLightningCounterDelta(15, 2), 2);

assert.equal(
  buildSmartLightningMaximumTooltip('最大漏流', 18.4, 'mA', 'DEA4-001', '相位', 'B相', new Date(2026, 8, 9, 14, 28, 16).getTime()),
  '最大漏流：18.40mA\n所属设备：DEA4-001\n相位：B相\n发生时间：2026-09-09 14:28:16'
);
