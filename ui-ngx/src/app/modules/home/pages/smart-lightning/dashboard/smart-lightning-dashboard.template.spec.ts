import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {join} from 'node:path';

const template = readFileSync(join(__dirname, 'smart-lightning-dashboard.component.html'), 'utf8');

assert.ok(template.includes('<h1>智能防雷看板</h1>'));
assert.ok(!template.includes('heading-subtitle'));
assert.ok(!template.includes('module-tags'));
assert.ok(!template.includes('class="status-strip"'));
assert.ok(!template.includes('class="status-chip"'));
