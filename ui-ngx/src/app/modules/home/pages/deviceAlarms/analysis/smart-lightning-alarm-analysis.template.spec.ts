/** 文件职责：告警分析模块模板静态检查，验证站点周期切换、设备直属父资产数据接入及表格精简。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const template = readFileSync(join(currentDirectory, 'smart-lightning-alarm-analysis.component.html'), 'utf8');
const component = readFileSync(join(currentDirectory, 'smart-lightning-alarm-analysis.component.ts'), 'utf8');

assert.ok(template.includes('tb-smart-lightning-period-switcher'));
assert.ok(!template.includes('class="date-filter"'));
assert.ok(!template.includes('<th scope="col">操作</th>'));
assert.ok(!template.includes('class="action-button"'));
assert.ok(component.includes('getLeafAssetAlarmCounts'));
assert.ok(component.includes('sitePeriod'));
