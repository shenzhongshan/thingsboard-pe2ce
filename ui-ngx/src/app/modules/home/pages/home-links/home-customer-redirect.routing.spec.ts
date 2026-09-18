/** 文件职责：验证客户用户首页分流只发生在 /home 页面路由，不侵入全局登录认证流程。 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const routingSource = readFileSync(join(currentDirectory, 'home-links-routing.module.ts'), 'utf8');
const authServiceSource = readFileSync(
  resolve(currentDirectory, '../../../../core/auth/auth.service.ts'),
  'utf8'
);

// 客户用户访问原生 /home 时，由页面级守卫跳转到独立的 /platform-home。
assert.match(routingSource, /customerHomeRedirectGuard/);
assert.match(routingSource, /Authority\.CUSTOMER_USER/);
assert.match(routingSource, /parseUrl\('\/platform-home'\)/);
assert.match(routingSource, /canActivate:\s*\[customerHomeRedirectGuard\]/);

// 登录、MFA、默认仪表板等全局认证逻辑不能引用自定义首页。
assert.doesNotMatch(authServiceSource, /platform-home/);
