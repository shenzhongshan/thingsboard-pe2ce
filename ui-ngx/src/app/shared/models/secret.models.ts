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

export class SecretId implements HasUUID {
  entityType = EntityType.SECRET;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export enum SecretType {
  TEXT = 'TEXT',
  TEXT_FILE = 'TEXT_FILE'
}

export const secretTypeTranslations = new Map<SecretType, string>([
  [SecretType.TEXT, 'secret.type-text'],
  [SecretType.TEXT_FILE, 'secret.type-text-file']
]);

// Value is write-only: never populated by the API. Present only in create/update request bodies.
export interface SecretInfo extends BaseData<SecretId> {
  tenantId?: TenantId;
  name: string;
  type: SecretType;
  description?: string;
}

export interface Secret extends SecretInfo {
  value?: string;
}
