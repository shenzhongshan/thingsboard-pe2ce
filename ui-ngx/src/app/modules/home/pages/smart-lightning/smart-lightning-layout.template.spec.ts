/** 文件职责：智能防雷页面公共层模块的 smart-lightning-layout.template.spec，验证模板结构、路由面包屑与关键文案。 */
/** 智能防雷各页面布局模板静态检查：读取模板文本断言关键布局标记存在。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

const smartLightningDirectory = dirname(fileURLToPath(import.meta.url));
const pagesDirectory = join(smartLightningDirectory, '..');

const rootAssetFilter = readFileSync(join(smartLightningDirectory, 'smart-lightning-root-asset-filter.component.html'), 'utf8');
const dataQueryTemplate = readFileSync(
  join(smartLightningDirectory, 'query', 'smart-lightning-data-query.component.html'),
  'utf8'
);
const assetPanelTemplates = [
  join(smartLightningDirectory, 'monitoring', 'smart-lightning-monitoring.component.html'),
  join(smartLightningDirectory, 'monitoring', 'smart-lightning-device-detail.component.html'),
  join(smartLightningDirectory, 'query', 'smart-lightning-data-query.component.html'),
  join(pagesDirectory, 'deviceAlarms', 'list', 'smart-lightning-alarm-list.component.html')
];
const pageStyles = [
  join(smartLightningDirectory, 'dashboard', 'smart-lightning-dashboard.component.scss'),
  join(smartLightningDirectory, 'monitoring', 'smart-lightning-monitoring.component.scss'),
  join(smartLightningDirectory, 'monitoring', 'smart-lightning-device-detail.component.scss'),
  join(smartLightningDirectory, 'query', 'smart-lightning-data-query.component.scss'),
  join(pagesDirectory, 'deviceAlarms', 'list', 'smart-lightning-alarm-list.component.scss'),
  join(pagesDirectory, 'deviceAlarms', 'analysis', 'smart-lightning-alarm-analysis.component.scss')
];

// 根资产下拉框保留无障碍名称，不再重复显示可见标签。
assert.ok(!rootAssetFilter.includes('<span>根资产</span>'));
assert.ok(rootAssetFilter.includes('aria-label="按根资产筛选"'));

// 数据查询筛选即时生效：顶部只保留重置，不再提交无实际作用的“查询”表单。
assert.ok(!dataQueryTemplate.includes('class="query-button"'));
assert.ok(dataQueryTemplate.includes('class="reset-button"'));
assert.ok(!dataQueryTemplate.includes('(ngSubmit)="query()"'));
assert.ok(!dataQueryTemplate.includes('<form class="filter-panel"'));

// 使用资产树的页面不再在搜索框上方重复显示标题或统计说明。
for (const templatePath of assetPanelTemplates) {
  const template = readFileSync(templatePath, 'utf8');
  assert.ok(!template.includes('asset-panel-title'), templatePath);
}

// 两组业务页面统一使用同一个页面间距变量。
for (const stylePath of pageStyles) {
  const stylesheet = readFileSync(stylePath, 'utf8');
  assert.ok(stylesheet.includes('padding: var(--tb-smart-page-spacing)'), stylePath);
}
