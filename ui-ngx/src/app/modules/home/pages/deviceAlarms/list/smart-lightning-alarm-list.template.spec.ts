/** 文件职责：告警列表模块的 smart-lightning-alarm-list.template.spec，验证模板结构、路由面包屑与关键文案。 */
/** 告警列表模板静态检查：读取模板文本断言操作列与关键标记存在。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const template = readFileSync(join(currentDirectory, 'smart-lightning-alarm-list.component.html'), 'utf8');
const component = readFileSync(join(currentDirectory, 'smart-lightning-alarm-list.component.ts'), 'utf8');
const componentStyles = readFileSync(join(currentDirectory, 'smart-lightning-alarm-list.component.scss'), 'utf8');
const dialogTemplate = readFileSync(join(currentDirectory, 'smart-lightning-alarm-details-dialog.component.html'), 'utf8');
const dialogComponent = readFileSync(join(currentDirectory, 'smart-lightning-alarm-details-dialog.component.ts'), 'utf8');
const globalStyles = readFileSync(join(currentDirectory, '../../../../../../styles.scss'), 'utf8');

// 告警页面保留设备节点，既显示具体告警设备，也显示向上传递后的资产标记。
assert.ok(template.includes('[showDeviceNodes]="true"'));

// 告警处理页的操作列只保留详情入口，并按单按钮宽度收窄。
assert.ok(template.includes('@if (!processingMode)'));
assert.ok(template.includes('[class.processing-mode]="processingMode"'));
assert.ok(template.includes('class="operation-actions"'));
assert.ok(template.includes('@if (processingMode)'));
assert.ok(template.includes('name="productName"'));
assert.ok(componentStyles.includes('--operation-column-width'));
assert.ok(componentStyles.includes('display: table-cell !important'));

// 操作表头和每行操作单元格必须共同固定在表格右侧，横向滚动时保持可见且边界对齐。
assert.ok(componentStyles.includes('.operation-column,\n.operation-cell {\n  position: sticky;'));
assert.ok(componentStyles.includes('right: 0;'));
assert.ok(componentStyles.includes('.table-wrap tbody tr:hover .operation-cell'));

// 固定详情列应向被遮挡的左侧内容投射阴影，不能使用常驻硬边框冒充层次。
assert.ok(!componentStyles.includes('border-left: 2px solid var(--operation-divider)'));
assert.ok(componentStyles.includes('box-shadow: -9px 0 16px rgba(8, 59, 120, .22)'));

// 操作列使用独立的浅蓝灰背景，与普通数据列形成轻微色差，并保留悬浮、选中状态反馈。
assert.ok(componentStyles.includes('--operation-column-bg: #f3f7fb'));
assert.ok(componentStyles.includes('--operation-column-header-bg: #cfe3f5'));
assert.ok(componentStyles.includes('background: var(--operation-column-bg)'));
assert.ok(componentStyles.includes('background: var(--operation-column-hover-bg)'));
assert.ok(componentStyles.includes('background: var(--operation-column-selected-bg)'));
assert.ok(componentStyles.includes('.alarm-list-page.processing-mode .time-range input'));
assert.ok(componentStyles.includes('.alarm-list-page.processing-mode .time-range {\n  grid-column: auto;\n}'));
assert.ok(componentStyles.includes('width: min(145px, 100%)'));

// 设备名属于普通数据列，应与表头一致居中；仅长文本描述列保留左对齐。
assert.ok(componentStyles.includes('.name-cell {\n  text-align: center !important;\n}'));
assert.ok(componentStyles.includes('.description-cell {\n  text-align: left !important;'));
assert.ok(!componentStyles.includes('.name-cell, .description-cell {\n  text-align: left !important;'));

// 未选择告警时，批量确认使用明确的中性灰禁用态，不能保留半透明绿色。
assert.ok(componentStyles.includes('.process-button:disabled {\n  color: #f7f9fb;\n  background: #929da8;\n  opacity: 1;'));

// 处理页必须打开本模块自定义弹窗，不得直接复用原生 AlarmDetailsDialogComponent。
assert.ok(!component.includes("import { AlarmDetailsDialogComponent"));
assert.ok(!component.includes('this.dialog.open<AlarmDetailsDialogComponent'));
assert.ok(component.includes('actionMode: this.processingMode'));

// 自定义弹窗内部按照“详情、派发、活动、确认/清除”顺序整合原生业务能力。
assert.ok(dialogTemplate.indexOf('class="detail-content"') < dialogTemplate.indexOf('class="dispatch-section"'));
assert.ok(dialogTemplate.indexOf('class="dispatch-section"') < dialogTemplate.indexOf('class="activity-section"'));
assert.ok(dialogTemplate.includes('<tb-alarm-assignee'));
assert.ok(dialogTemplate.includes('<tb-alarm-comment'));
assert.ok(dialogTemplate.includes('(click)="acknowledge()"'));
assert.ok(dialogTemplate.includes('(click)="clear()"'));
assert.ok(dialogComponent.includes('this.alarmService.ackAlarm'));
assert.ok(dialogComponent.includes('this.alarmService.clearAlarm'));

// 活动区占满弹窗宽度；确认和清除必须经过应用内二次确认框。
assert.ok(component.includes("'smart-lightning-alarm-detail-panel'"));
assert.ok(globalStyles.includes('.smart-lightning-alarm-detail-panel .alarm-detail-dialog .activity-section .tb-alarm-comments'));
assert.ok(globalStyles.includes('max-width: none !important'));
assert.ok(dialogComponent.includes("this.dialogService.confirm('确认告警'"));
assert.ok(dialogComponent.includes("this.dialogService.confirm('清除告警'"));

// 初次进入告警页面只展示前两级资产，不能由默认根筛选再次递归展开整棵树。
assert.ok(component.includes('getSmartLightningDefaultExpandedAssetIds'));
assert.ok(component.includes("this.selectRootAsset('', false)"));
