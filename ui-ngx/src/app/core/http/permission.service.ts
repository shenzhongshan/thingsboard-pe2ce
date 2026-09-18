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
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { AllowedPermissionsInfo } from '@shared/models/role.models';

/**
 * Serves the permission vocabulary the role editor needs: which resources this authority may be
 * granted, and which operations each of them accepts.
 *
 * Deliberately server-driven, matching PE: the authoritative lists are
 * {@code Resource.operationsByResource} and {@code Operation.allowedForGroupRoleOperations} on the
 * backend. Mirroring them as constants in the UI is how they silently drift — the previous version
 * of this editor offered every operation for every resource, so a role could be saved granting e.g.
 * WRITE_CREDENTIALS on AUDIT_LOG, which the backend never checks.
 */
@Injectable({
  providedIn: 'root'
})
export class PermissionService {

  private allowedPermissions$: Observable<AllowedPermissionsInfo>;

  constructor(private http: HttpClient) {}

  /**
   * Cached for the lifetime of the app: the payload is a static vocabulary that only depends on the
   * caller's authority, so re-fetching it per dialog open would be pure overhead.
   */
  getAllowedPermissions(config?: RequestConfig): Observable<AllowedPermissionsInfo> {
    if (!this.allowedPermissions$) {
      this.allowedPermissions$ = this.http.get<AllowedPermissionsInfo>(
        '/api/permissions/allowedPermissions', defaultHttpOptionsFromConfig(config)
      ).pipe(
        shareReplay({bufferSize: 1, refCount: false})
      );
    }
    return this.allowedPermissions$;
  }

  /** Resources the current authority may appear in a GENERIC role, sorted for stable display. */
  getAllowedResources(config?: RequestConfig): Observable<string[]> {
    return this.getAllowedPermissions(config).pipe(
      map((info) => (info.allowedResources || []).slice().sort())
    );
  }

  clearCache() {
    this.allowedPermissions$ = null;
  }
}
