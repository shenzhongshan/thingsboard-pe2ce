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

export class EntityGroupId implements HasUUID {
  entityType = EntityType.ENTITY_GROUP;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export const GROUP_ALL_NAME = 'All';

// CE port: group types limited to these six.
export const entityGroupTypes: EntityType[] = [
  EntityType.USER, EntityType.CUSTOMER, EntityType.DEVICE, EntityType.ASSET,
  EntityType.ENTITY_VIEW, EntityType.DASHBOARD
];

export interface EntityGroup extends BaseData<EntityGroupId> {
  type: EntityType;
  name: string;
  tenantId?: TenantId;
  // Customer id when the group is customer-owned (hierarchy view); absent for tenant-owned groups.
  ownerId?: {entityType: string; id: string};
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
