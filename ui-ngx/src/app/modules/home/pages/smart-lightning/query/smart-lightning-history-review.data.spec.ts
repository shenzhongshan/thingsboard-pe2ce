import assert from 'node:assert/strict';
import {
  buildSmartLightningHistoryPoints,
  filterSmartLightningTelemetryParameters,
  getSmartLightningTelemetryParameterLabel
} from './smart-lightning-history-review.data';

const points = buildSmartLightningHistoryPoints({
  leakageA: [
    {ts: 3000, value: '3.5'},
    {ts: 1000, value: '1.5'},
    {ts: 2000, value: 'not-a-number'}
  ]
}, 'leakageA');

assert.deepEqual(points, [
  [1000, 1.5],
  [3000, 3.5]
]);
assert.equal(getSmartLightningTelemetryParameterLabel('DESA', 'leakageA'), 'A相漏电流（leakageA）');
assert.equal(getSmartLightningTelemetryParameterLabel('DESA', 'customKey'), 'customKey');
assert.deepEqual(
  filterSmartLightningTelemetryParameters('DESA', ['breakerStatus', 'leakageA', 'groundStatus', 'temperatureA', 'spdStatus']),
  ['leakageA', 'temperatureA']
);
