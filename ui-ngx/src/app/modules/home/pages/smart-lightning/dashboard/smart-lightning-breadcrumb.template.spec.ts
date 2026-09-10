/** 文件职责：看板模块的 smart-lightning-breadcrumb.template.spec，验证模板结构、路由面包屑与关键文案。 */
/** 看板路由面包屑模板静态检查：读取模板文本断言关键标记存在。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const routingSource = readFileSync(
  join(currentDirectory, 'smart-lightning-dashboard-routing.module.ts'),
  'utf8'
);
const globalStyles = readFileSync(
  resolve(currentDirectory, '../../../../../../styles.scss'),
  'utf8'
);

// 智能防雷六条页面路由都应参与 TB 原生面包屑，不得再使用 skip 隐藏。
assert.doesNotMatch(routingSource, /breadcrumb\s*:\s*{\s*skip\s*:\s*true/);
assert.equal(routingSource.match(/breadcrumb\s*:\s*{\s*label\s*:/g)?.length, 6);
assert.match(
  routingSource,
  /path:\s*'smart-lightning\/monitoring'[\s\S]*?children:\s*\[[\s\S]*?path:\s*'device\/:deviceId'/
);
assert.match(
  routingSource,
  /path:\s*'smart-lightning\/query'[\s\S]*?children:\s*\[[\s\S]*?path:\s*'device\/:deviceId'/
);

// 顶部工具栏和面包屑颜色由 ThingsBoard 主题负责，全局定制样式不得强制覆盖。
assert.doesNotMatch(globalStyles, /mat-sidenav-content\s+\.tb-primary-toolbar\s*{/);
assert.doesNotMatch(globalStyles, /mat-sidenav-content\s+\.tb-primary-toolbar\s+(?:\.mat-icon|button)/);
