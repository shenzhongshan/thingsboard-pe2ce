/** 文件职责：看板模块的 smart-lightning-page-headings.template.spec，验证模板结构、路由面包屑与关键文案。 */
/** 看板页标题模板静态检查：读取模板文本断言标题层级存在。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

const dashboardDirectory = dirname(fileURLToPath(import.meta.url));
const monitoringDirectory = join(dashboardDirectory, '..', 'monitoring');
const alarmAnalysisDirectory = join(dashboardDirectory, '..', '..', 'deviceAlarms', 'analysis');

const monitoringTemplate = readFileSync(join(monitoringDirectory, 'smart-lightning-monitoring.component.html'), 'utf8');
const alarmAnalysisTemplate = readFileSync(join(alarmAnalysisDirectory, 'smart-lightning-alarm-analysis.component.html'), 'utf8');

// 页面名称已经由系统面包屑承载，内容区不再重复渲染页面级标题。
assert.ok(!monitoringTemplate.includes('class="monitoring-heading"'));
assert.ok(!monitoringTemplate.includes('资产层级联动监测与设备状态概览'));
assert.ok(monitoringTemplate.includes('aria-label="智能防雷数据监测"'));

assert.ok(!alarmAnalysisTemplate.includes('class="page-heading"'));
assert.ok(!alarmAnalysisTemplate.includes('当前用户全部可见设备 · 告警趋势与处置效率'));
assert.ok(alarmAnalysisTemplate.includes('aria-label="告警分析"'));
