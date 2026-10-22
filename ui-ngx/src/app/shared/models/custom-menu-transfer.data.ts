import {CMItemType, CMScope, CustomMenuConfig, CustomMenuInfo, MenuItemType} from './custom-menu.models';

/** 与控制台脚本兼容的菜单交换格式，仅包含名称、范围及菜单配置，不携带实体ID或分配用户。 */
export interface CustomMenuTransferPayload {
  format: 'thingsboard-custom-menu-transfer';
  formatVersion: 1;
  exportedAt?: string;
  sourceOrigin?: string;
  name: string;
  scope: CMScope;
  config: CustomMenuConfig;
}

/** 导入失败的可翻译错误；menuId存在时说明新菜单清理未确认，禁止直接重复提交。 */
export class CustomMenuTransferError extends Error {
  constructor(public readonly messageKey: string, public readonly menuId?: string) {
    super(messageKey);
  }
}

const MAX_MENU_DEPTH = 32;
const MAX_MENU_ITEMS = 5000;

/** 校验版本、范围及完整菜单树，兼容旧脚本导出的test菜单，但不再限制名称和来源环境。 */
export function parseCustomMenuTransfer(value: unknown): CustomMenuTransferPayload {
  const fail = (): never => { throw new CustomMenuTransferError('custom-menu.transfer-invalid-file'); };
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return fail();
  }
  const payload = value as Record<string, unknown>;
  if (payload.format !== 'thingsboard-custom-menu-transfer' || payload.formatVersion !== 1 ||
      typeof payload.name !== 'string' || !payload.name.trim() ||
      ![CMScope.SYSTEM, CMScope.TENANT].includes(payload.scope as CMScope)) {
    return fail();
  }
  const config = payload.config as Record<string, unknown>;
  if (!config || typeof config !== 'object' || Array.isArray(config) || !Array.isArray(config.items)) {
    return fail();
  }
  let itemCount = 0;
  const visit = (items: unknown[], depth: number): void => {
    if (depth > MAX_MENU_DEPTH) {
      fail();
    }
    for (const item of items) {
      if (++itemCount > MAX_MENU_ITEMS || !item || typeof item !== 'object' || Array.isArray(item)) {
        fail();
      }
      const node = item as Record<string, unknown>;
      if (![MenuItemType.HOME, MenuItemType.DEFAULT, MenuItemType.CUSTOM].includes(node.type as MenuItemType) ||
          (node.visible != null && typeof node.visible !== 'boolean')) {
        fail();
      }
      if (node.type === MenuItemType.DEFAULT && (typeof node.id !== 'string' || !node.id)) {
        fail();
      }
      if (node.type === MenuItemType.CUSTOM && (typeof node.name !== 'string' || !node.name.trim() ||
          ![CMItemType.LINK, CMItemType.SECTION].includes(node.menuItemType as CMItemType))) {
        fail();
      }
      if (node.pages != null) {
        if (!Array.isArray(node.pages)) {
          fail();
        }
        visit(node.pages as unknown[], depth + 1);
      }
    }
  };
  visit(config.items, 0);
  return {
    format: 'thingsboard-custom-menu-transfer', formatVersion: 1,
    name: payload.name.trim(), scope: payload.scope as CMScope,
    config: JSON.parse(JSON.stringify(config)),
    exportedAt: typeof payload.exportedAt === 'string' ? payload.exportedAt : undefined,
    sourceOrigin: typeof payload.sourceOrigin === 'string' ? payload.sourceOrigin : undefined
  };
}

/** 显式构造可迁移数据，避免将服务端实体ID、租户ID及用户分配带入另一个环境。 */
export function createCustomMenuTransfer(menu: CustomMenuInfo, config: CustomMenuConfig): CustomMenuTransferPayload {
  return parseCustomMenuTransfer({
    format: 'thingsboard-custom-menu-transfer', formatVersion: 1,
    exportedAt: new Date().toISOString(), sourceOrigin: location.origin,
    name: menu.name, scope: menu.scope, config
  });
}

/** 对象键顺序不影响配置回读校验，数组顺序保留以保证菜单排序一致。 */
export function canonicalCustomMenuConfig(value: unknown): string {
  const normalize = (item: unknown): unknown => {
    if (Array.isArray(item)) {
      return item.map(normalize);
    }
    if (item && typeof item === 'object') {
      const object = item as Record<string, unknown>;
      return Object.fromEntries(Object.keys(object).sort().map(key => [key, normalize(object[key])]));
    }
    return item;
  };
  return JSON.stringify(normalize(value));
}
