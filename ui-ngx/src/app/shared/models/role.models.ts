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
import { HasUUID } from '@shared/models/id/has-uuid';

export class RoleId implements HasUUID {
  entityType = EntityType.ROLE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export enum RoleType {
  GENERIC = 'GENERIC',
  GROUP = 'GROUP'
}

export const roleTypeTranslations = new Map<RoleType, string>([
  [RoleType.GENERIC, 'role.type-generic'],
  [RoleType.GROUP, 'role.type-group']
]);

// PE permission resource names (data-model options only — CE does not enforce them).
export const roleResources: string[] = [
  'ALL', 'PROFILE', 'ADMIN_SETTINGS', 'ALARM', 'DEVICE', 'ASSET', 'CUSTOMER', 'DASHBOARD',
  'ENTITY_VIEW', 'TENANT', 'RULE_CHAIN', 'USER', 'WIDGETS_BUNDLE', 'WIDGET_TYPE', 'OAUTH2_CONFIGURATION_INFO',
  'DEVICE_PROFILE', 'ASSET_PROFILE', 'API_USAGE_STATE', 'TB_RESOURCE', 'OTA_PACKAGE', 'EDGE', 'RPC',
  'QUEUE', 'VERSION_CONTROL', 'NOTIFICATION', 'CONVERTER', 'INTEGRATION', 'SCHEDULER_EVENT',
  'BLOB_ENTITY', 'CUSTOMER_GROUP', 'DEVICE_GROUP', 'ASSET_GROUP', 'USER_GROUP', 'ENTITY_VIEW_GROUP',
  'EDGE_GROUP', 'DASHBOARD_GROUP', 'ROLE', 'GROUP_PERMISSION', 'WHITE_LABELING', 'AUDIT_LOG'
];

export const roleResourceTranslations = new Map<string, string>(
  roleResources.map(res => [res, `role.resource-${res}`])
);

// PE permission operation names.
export const roleOperations: string[] = [
  'ALL', 'CREATE', 'READ', 'WRITE', 'DELETE', 'RPC_CALL', 'READ_CREDENTIALS', 'WRITE_CREDENTIALS',
  'READ_ATTRIBUTES', 'WRITE_ATTRIBUTES', 'READ_TELEMETRY', 'WRITE_TELEMETRY', 'ADD_TO_GROUP',
  'REMOVE_FROM_GROUP', 'CHANGE_OWNER', 'IMPERSONATE', 'CLAIM_DEVICES', 'SHARE_GROUP', 'ASSIGN_TO_TENANT'
];

export const roleOperationTranslations = new Map<string, string>(
  roleOperations.map(op => [op, `role.operation-${op}`])
);

export interface Role extends BaseData<RoleId> {
  tenantId?: TenantId;
  name: string;
  type: RoleType;
  // GENERIC: {resource: [operations]}; GROUP: [operations]
  permissions: {[resource: string]: string[]} | string[];
  additionalInfo?: {description?: string; [key: string]: any};
}

export interface GroupPermission extends BaseData<any> {
  tenantId?: TenantId;
  userGroupId: {entityType: string; id: string};
  roleId: {entityType: string; id: string};
  entityGroupId?: {entityType: string; id: string};
  entityGroupType?: EntityType;
  public?: boolean;
}

export interface GroupPermissionInfo extends GroupPermission {
  roleName?: string;
  roleType?: string;
  entityGroupName?: string;
  userGroupName?: string;
  // 'tenant' | 'customer' — which kind of owner the granting user group belongs to.
  userGroupOwnerName?: string;
}
