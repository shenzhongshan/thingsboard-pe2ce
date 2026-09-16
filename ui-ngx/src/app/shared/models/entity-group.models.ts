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
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { HasUUID } from '@shared/models/id/has-uuid';

export class EntityGroupId implements HasUUID {
  entityType = EntityType.ENTITY_GROUP;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export const GROUP_ALL_NAME = 'All';

// CE port: group types limited to these six.
// 与后端 EntityGroup.groupTypes 保持一致（PE 4.3.1.1 的 7 种）。
export const entityGroupTypes: EntityType[] = [
  EntityType.USER, EntityType.CUSTOMER, EntityType.DEVICE, EntityType.ASSET,
  EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE
];

export interface EntityGroup extends BaseData<EntityGroupId> {
  type: EntityType;
  name: string;
  tenantId?: TenantId;
  // Customer id when the group is customer-owned (hierarchy view); tenant id for tenant-owned groups.
  ownerId?: EntityId;
  additionalInfo?: {description?: string; isPublic?: boolean; [key: string]: any};
  configuration?: any;
}

export interface GroupEntityInfo {
  id: {entityType: string; id: string};
  name: string;
}

export function isGroupAll(group: EntityGroup): boolean {
  return group?.name === GROUP_ALL_NAME;
}

// Shape of EntityGroup.configuration.settings — a per-group override of the member table's
// EntityTableConfig knobs, edited from the entity-group details drawer's 设置 tab.
export interface EntityGroupSettings {
  tableTitle?: string;
  enableSearch?: boolean;
  enableAdd?: boolean;
  enableDelete?: boolean;
  enableSelection?: boolean;
  enableGroupTransfer?: boolean;
  rowClickAction?: 'openDetails' | 'select';
  displayPagination?: boolean;
  pageStepIncrement?: number;
  pageStepCount?: number;
  defaultPageSize?: number;
  enableCredentialsManagement?: boolean;
}

export type EntityGroupColumnType = 'ENTITY_FIELD' | 'ATTRIBUTE' | 'TIMESERIES';
export type EntityGroupColumnSortOrder = 'ASC' | 'DESC' | 'NONE';

export interface EntityGroupColumn {
  type: EntityGroupColumnType;
  key: string;
  label?: string;
  sortOrder?: EntityGroupColumnSortOrder;
  hideOnMobile?: boolean;
}

// Default 实体字段 (ENTITY_FIELD) column choices per group type — matches each type's real
// EntityTableColumn key/label pairs (devices-table-config.resolver.ts etc.), used both to
// pre-populate the 列 tab's default rows and as the 值 dropdown options for ENTITY_FIELD columns.
export const entityGroupDefaultFields: {[type: string]: {key: string; label: string}[]} = {
  DEVICE: [
    {key: 'name', label: 'device.name'},
    {key: 'deviceProfileName', label: 'device-profile.device-profile'},
    {key: 'label', label: 'device.label'}
  ],
  ASSET: [
    {key: 'name', label: 'asset.name'},
    {key: 'assetProfileName', label: 'asset-profile.asset-profile'},
    {key: 'label', label: 'asset.label'}
  ],
  CUSTOMER: [
    {key: 'title', label: 'customer.title'},
    {key: 'email', label: 'contact.email'}
  ],
  USER: [
    {key: 'email', label: 'user.email'}
  ],
  ENTITY_VIEW: [
    {key: 'name', label: 'entity-view.name'},
    {key: 'type', label: 'entity-view.entity-view-type'}
  ],
  DASHBOARD: [
    {key: 'title', label: 'dashboard.title'}
  ]
};

export function defaultEntityGroupColumns(groupType: EntityType): EntityGroupColumn[] {
  const columns: EntityGroupColumn[] = [
    {type: 'ENTITY_FIELD', key: 'createdTime', label: 'common.created-time', sortOrder: 'DESC', hideOnMobile: false}
  ];
  for (const field of entityGroupDefaultFields[groupType] || []) {
    columns.push({type: 'ENTITY_FIELD', key: field.key, label: field.label, sortOrder: 'NONE', hideOnMobile: false});
  }
  return columns;
}

export type EntityGroupActionType = 'openDashboard' | 'custom';
// Where in the member table this custom action button appears — matches PE's 操作来源 column.
export type EntityGroupActionSource = 'header' | 'row' | 'group';

export const entityGroupActionSources: EntityGroupActionSource[] = ['header', 'row', 'group'];

export interface EntityGroupAction {
  id?: string;
  source: EntityGroupActionSource;
  name: string;
  icon: string;
  type: EntityGroupActionType;
  dashboardId?: string;
  dashboardState?: string;
  customFunction?: string;
}
