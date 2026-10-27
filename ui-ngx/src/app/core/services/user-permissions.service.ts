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

import { Injectable } from '@angular/core';
import { PermissionService } from '@core/http/permission.service';
import { AllowedPermissionsInfo } from '@shared/models/role.models';

/**
 * Synchronous reads over the same payload {@link PermissionService} already fetches
 * (GET /api/permissions/allowedPermissions) — the `userPermissions`/`userOwnerId` half of it, which
 * until now had zero consumers in the CE frontend even though the backend always sent it. PE parity:
 * its `UserPermissionsService.hasGenericPermission` et al., trimmed to what CE's
 * `MergedUserPermissions` actually carries (CE has no `readGroupPermissions` UI-hint map — see
 * `MergedUserPermissions`'s class doc on the backend).
 *
 * Unlike PE, this isn't populated by an eager load at login; it piggybacks on whatever first
 * triggers {@link PermissionService#getAllowedPermissions}. Consumers that need correctness before
 * that resolves should treat a `false` read as "not yet known" rather than "denied", which is why
 * {@link HasGenericPermissionPipe} is impure — it re-checks every change detection cycle rather than
 * caching the pre-load `false`.
 */
@Injectable({
  providedIn: 'root'
})
export class UserPermissionsService {

  private info: AllowedPermissionsInfo;

  constructor(private permissionService: PermissionService) {
    this.permissionService.getAllowedPermissions().subscribe(info => {
      this.info = info;
    });
  }

  /** A blanket grant from a GENERIC-type role, covering every entity of the resource (or every
   * resource, for an ALL-scoped role) regardless of which entity group it's in. */
  hasGenericPermission(resource: string, operation: string): boolean {
    const perms = this.info?.userPermissions;
    if (!perms) {
      return false;
    }
    return this.checkOperations(perms.genericPermissions?.[resource], operation)
      || this.checkOperations(perms.genericPermissions?.ALL, operation);
  }

  /** A grant from a GROUP-type role, scoped to one specific entity group. */
  hasGroupPermission(entityGroupId: string, operation: string): boolean {
    const info = this.info?.userPermissions?.groupPermissions?.[entityGroupId];
    return this.checkOperations(info?.operations, operation);
  }

  getUserOwnerId(): {entityType: string; id: string} | undefined {
    return this.info?.userOwnerId;
  }

  /** Whether the given owner is the current user's own tenant/customer — the owner-only operations
   * in {@link AllowedPermissionsInfo#allowedForGroupOwnerOnlyOperations} only apply when this is
   * true for the entity's owner. */
  isCurrentUserOwner(ownerId: {entityType: string; id: string}): boolean {
    const mine = this.info?.userOwnerId;
    return !!mine && !!ownerId && mine.entityType === ownerId.entityType && mine.id === ownerId.id;
  }

  private checkOperations(operations: string[] | undefined, operation: string): boolean {
    return !!operations && (operations.includes('ALL') || operations.includes(operation));
  }

}
