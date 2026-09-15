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

/**
 * Shape of GET /api/permissions/allowedPermissions. The role editor is driven entirely by this:
 * the backend's Resource.operationsByResource / Operation.allowedForGroupRoleOperations are the
 * single source of truth, so the UI must never carry its own copy of these vocabularies.
 */
export interface AllowedPermissionsInfo {
  /** resource name -> operations that resource accepts */
  operationsByResource: {[resource: string]: string[]};
  /** operations a GROUP role may grant (a strict subset — group roles act within one entity group) */
  allowedForGroupRoleOperations: string[];
  allowedForGroupOwnerOnlyOperations: string[];
  allowedForGroupOwnerOnlyGroupOperations: string[];
  /** resources this authority may be granted at all */
  allowedResources: string[];
  userPermissions?: any;
  userOwnerId?: {entityType: string; id: string};
}

export const roleResourceTranslation = (resource: string): string => `role.resource-${resource}`;

export const roleOperationTranslation = (operation: string): string => `role.operation-${operation}`;

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
