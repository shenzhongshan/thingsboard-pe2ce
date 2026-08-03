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
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { GroupPermission, GroupPermissionInfo, Role } from '@shared/models/role.models';

@Injectable({
  providedIn: 'root'
})
export class RoleService {

  constructor(private http: HttpClient) {
  }

  public getRoles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Role>> {
    return this.http.get<PageData<Role>>(`/api/roles${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getRole(roleId: string, config?: RequestConfig): Observable<Role> {
    return this.http.get<Role>(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveRole(role: Role, config?: RequestConfig): Observable<Role> {
    return this.http.post<Role>('/api/role', role, defaultHttpOptionsFromConfig(config));
  }

  public deleteRole(roleId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveGroupPermission(groupPermission: GroupPermission, config?: RequestConfig): Observable<GroupPermission> {
    return this.http.post<GroupPermission>('/api/groupPermission', groupPermission, defaultHttpOptionsFromConfig(config));
  }

  public getUserGroupPermissions(userGroupId: string, config?: RequestConfig): Observable<GroupPermissionInfo[]> {
    return this.http.get<GroupPermissionInfo[]>(`/api/userGroup/${userGroupId}/groupPermissions`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupPermissions(entityGroupId: string, config?: RequestConfig): Observable<GroupPermissionInfo[]> {
    return this.http.get<GroupPermissionInfo[]>(`/api/entityGroup/${entityGroupId}/groupPermissions`,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteGroupPermission(groupPermissionId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/groupPermission/${groupPermissionId}`, defaultHttpOptionsFromConfig(config));
  }

}
