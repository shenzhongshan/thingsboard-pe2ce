/** 文件职责：告警页面公共层模块的 device-alarms-breadcrumb.template.spec，验证模板结构、路由面包屑与关键文案。 */
/** 告警模块路由面包屑模板静态检查：读取模板文本断言关键标记存在。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const routingSource = readFileSync(
  join(currentDirectory, '../alarm/alarm-routing.module.ts'),
  'utf8'
);
const customAlarmRoutes = routingSource.slice(
  routingSource.indexOf("path: 'deviceAlarms'"),
  routingSource.indexOf("path: 'alarms'")
);
const customAlarmParentRoute = customAlarmRoutes.slice(0, customAlarmRoutes.indexOf('children: ['));

// “告警管理”只是菜单分组，不是可访问页面，因此不生成父级面包屑。
assert.doesNotMatch(customAlarmParentRoute, /breadcrumb\s*:/);

// 每个子页只显示自己的名称，不能借用 MenuId.alarms 而全部显示成“告警列表”。
assert.equal(customAlarmRoutes.match(/breadcrumb\s*:\s*{\s*label\s*:\s*'告警处理'/g)?.length, 2);
assert.match(customAlarmRoutes, /breadcrumb\s*:\s*{\s*label\s*:\s*'告警派发'/);
assert.match(customAlarmRoutes, /breadcrumb\s*:\s*{\s*label\s*:\s*'告警分析'/);
assert.doesNotMatch(customAlarmRoutes, /menuId\s*:\s*MenuId\.alarms/);
