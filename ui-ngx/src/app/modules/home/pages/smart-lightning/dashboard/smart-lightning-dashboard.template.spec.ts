/** 文件职责：看板模块的 smart-lightning-dashboard.template.spec，验证模板结构、路由面包屑与关键文案。 */
/** 看板模板静态检查：读取模板文本断言图表面板与关键标记存在。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {dirname, join} from 'node:path';
import {buildSmartLightningMapPoints, filterSmartLightningMapDevicesWithoutPoints} from './smart-lightning-map.data.ts';

const currentDirectory = dirname(fileURLToPath(import.meta.url));

const template = readFileSync(join(currentDirectory, 'smart-lightning-dashboard.component.html'), 'utf8');

assert.ok(!template.includes('class="lightning-heading"'));
assert.ok(!template.includes('<h1>智能防雷看板</h1>'));
assert.ok(template.includes('aria-label="智能防雷看板"'));
assert.ok(!template.includes('heading-subtitle'));
assert.ok(!template.includes('module-tags'));
assert.ok(!template.includes('class="status-strip"'));
assert.ok(!template.includes('class="status-chip"'));

// 地网模块保留原卡片位置，但内容改为当前设备、告警和阻值三组汇总信息。
assert.ok(template.includes('class="ground-overview"'));
assert.ok(template.includes('地网设备'));
assert.ok(template.includes('最大阻值'));
assert.ok(template.includes('最小阻值'));
assert.ok(template.includes('最大漏流'));
assert.ok(template.includes('接地正常'));
assert.ok(template.includes('SPD正常'));
assert.ok(template.includes('温度正常'));
assert.ok(template.includes('寿命正常'));
assert.ok(template.includes('雷击次数'));
assert.ok(template.includes('最小峰值'));
assert.ok(template.includes('formatLightningAlarmTooltip(lightningSummary)'));
assert.ok(template.includes('formatLightningMinimumTooltip(lightningSummary)'));
assert.ok(!template.includes("{{lightningSummary.maxPeakDeviceName || '--'}}"));
assert.ok(!template.includes("{{lightningSummary.maxPeakPolarity || '--'}}"));
assert.ok(!template.includes('data-chart="spd"'));
assert.ok(!template.includes('data-chart="lightning"'));

// 服务器属性只有在经纬度成对且处于有效范围时，才应转换为地图点位。
const savedMapPoints = buildSmartLightningMapPoints([
  {deviceId: 'desa-001', deviceName: 'DESA-001', productName: 'FS防雷监测模块（多要素）', latitude: '22.509266', longitude: 114.052357},
  {deviceId: 'missing-longitude', deviceName: '缺少经度', latitude: 22.5},
  {deviceId: 'invalid-latitude', deviceName: '无效纬度', latitude: 91, longitude: 114}
]);
assert.deepEqual(savedMapPoints, [{
  deviceId: 'desa-001',
  deviceName: 'DESA-001',
  productName: 'FS防雷监测模块（多要素）',
  latitude: 22.509266,
  longitude: 114.052357
}]);

// 下拉框只排除已经实际绘制到地图上的设备，不能把所有具有坐标上下文的设备全部排除。
const mapCandidates = [
  {deviceId: 'desa-001', deviceName: 'DESA-001'},
  {deviceId: 'desa-002', deviceName: 'DESA-002'}
];
assert.deepEqual(
  filterSmartLightningMapDevicesWithoutPoints(mapCandidates, new Set(['desa-001'])),
  [{deviceId: 'desa-002', deviceName: 'DESA-002'}]
);
