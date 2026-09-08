import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {join} from 'node:path';

const template = readFileSync(join(__dirname, 'smart-lightning-dashboard.component.html'), 'utf8');

assert.ok(template.includes('<h1>智能防雷看板</h1>'));
assert.ok(!template.includes('heading-subtitle'));
assert.ok(!template.includes('module-tags'));
assert.ok(!template.includes('class="status-strip"'));
assert.ok(!template.includes('class="status-chip"'));

// 地网模块保留原卡片位置，但内容改为当前设备、告警和阻值三组汇总信息。
assert.ok(template.includes('class="ground-overview"'));
assert.ok(template.includes('地网设备'));
assert.ok(template.includes('最大阻值'));
assert.ok(template.includes('最小阻值'));
