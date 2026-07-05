///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { BaseData } from '@shared/models/base-data';
import { CustomMenuId } from '@shared/models/id/custom-menu-id';
import { TenantId } from '@shared/models/id/tenant-id';

export enum MenuItemType {
  HOME = 'HOME',
  DEFAULT = 'DEFAULT',
  CUSTOM = 'CUSTOM'
}

export enum CMItemType {
  LINK = 'LINK',
  SECTION = 'SECTION'
}

export enum CMItemLinkType {
  URL = 'URL',
  DASHBOARD = 'DASHBOARD'
}

export const cmItemTypeTranslations = new Map<CMItemType, string>([
  [CMItemType.LINK, 'custom-menu.item-type-link'],
  [CMItemType.SECTION, 'custom-menu.item-type-section']
]);

export const cmItemLinkTypeTranslations = new Map<CMItemLinkType, string>([
  [CMItemLinkType.URL, 'custom-menu.link-type-url'],
  [CMItemLinkType.DASHBOARD, 'custom-menu.link-type-dashboard']
]);

export interface MenuItem {
  type: MenuItemType;
  visible?: boolean;
}

export interface CustomMenuItem extends MenuItem {
  type: MenuItemType.CUSTOM;
  name: string;
  icon?: string;
  menuItemType: CMItemType;
  linkType?: CMItemLinkType;
  dashboardId?: string;
  hideDashboardToolbar?: boolean;
  url?: string;
  setAccessToken?: boolean;
  pages?: CustomMenuItem[];
}

// Built-in menu item override: hide/rename/reorder a default menu entry (id = MenuId).
export interface DefaultMenuItem extends MenuItem {
  type: MenuItemType.DEFAULT;
  id: string;
  name?: string;
  icon?: string;
  pages?: MenuItem[];
}

export interface CustomMenuConfig {
  items: MenuItem[];
}

export enum CMScope {
  SYSTEM = 'SYSTEM',
  TENANT = 'TENANT'
}

export enum CMAssigneeType {
  NO_ASSIGN = 'NO_ASSIGN',
  ALL = 'ALL',
  CUSTOMERS = 'CUSTOMERS',
  USERS = 'USERS'
}

export const cmScopeTranslations = new Map<CMScope, string>([
  [CMScope.SYSTEM, 'custom-menu.scope-system'],
  [CMScope.TENANT, 'custom-menu.scope-tenant']
]);

export const cmAssigneeTypeTranslations = new Map<CMAssigneeType, string>([
  [CMAssigneeType.NO_ASSIGN, 'custom-menu.assignee-type-no-assign'],
  [CMAssigneeType.ALL, 'custom-menu.assignee-type-all'],
  [CMAssigneeType.CUSTOMERS, 'custom-menu.assignee-type-customers'],
  [CMAssigneeType.USERS, 'custom-menu.assignee-type-users']
]);

// Metadata of a named custom menu (list page row); CustomMenu adds the item tree.
export interface CustomMenuInfo extends BaseData<CustomMenuId> {
  tenantId?: TenantId;
  name: string;
  scope: CMScope;
  assigneeType: CMAssigneeType;
}

export interface CustomMenu extends CustomMenuInfo {
  config: CustomMenuConfig;
}

// Assignee entry as returned by GET /api/customMenu/{id}/assigneeList
export interface CustomMenuAssignee {
  id: {
    entityType: string;
    id: string;
  };
  name: string;
}

export interface CustomMenuDeleteResult {
  success: boolean;
  assigneeType?: CMAssigneeType;
  assigneeList: CustomMenuAssignee[];
}

export function emptyCustomMenuConfig(): CustomMenuConfig {
  return { items: [] };
}

export function newCustomMenuItem(): CustomMenuItem {
  return {
    type: MenuItemType.CUSTOM,
    name: '',
    icon: 'link',
    menuItemType: CMItemType.LINK,
    linkType: CMItemLinkType.URL,
    url: '',
    visible: true,
    pages: []
  };
}
