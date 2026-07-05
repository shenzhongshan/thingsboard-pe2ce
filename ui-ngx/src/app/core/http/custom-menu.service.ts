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
import {
  CMAssigneeType,
  CMScope,
  CustomMenu,
  CustomMenuAssignee,
  CustomMenuConfig,
  CustomMenuDeleteResult,
  CustomMenuInfo
} from '@shared/models/custom-menu.models';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';

@Injectable({
  providedIn: 'root'
})
export class CustomMenuService {

  constructor(private http: HttpClient) {
  }

  public getCustomMenuInfos(pageLink: PageLink, scope?: CMScope,
                            config?: RequestConfig): Observable<PageData<CustomMenuInfo>> {
    let url = `/api/customMenu/infos${pageLink.toQuery()}`;
    if (scope) {
      url += `&scope=${scope}`;
    }
    return this.http.get<PageData<CustomMenuInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public createCustomMenu(customMenuInfo: CustomMenuInfo, assignToIds?: string[], force = false,
                          config?: RequestConfig): Observable<CustomMenu> {
    let url = `/api/customMenu?force=${force}`;
    if (assignToIds && assignToIds.length) {
      url += `&assignToIds=${assignToIds.join(',')}`;
    }
    return this.http.post<CustomMenu>(url, customMenuInfo, defaultHttpOptionsFromConfig(config));
  }

  public getCustomMenuInfoById(customMenuId: string, config?: RequestConfig): Observable<CustomMenuInfo> {
    return this.http.get<CustomMenuInfo>(`/api/customMenu/${customMenuId}/info`, defaultHttpOptionsFromConfig(config));
  }

  public getCustomMenuConfigById(customMenuId: string, config?: RequestConfig): Observable<CustomMenuConfig> {
    return this.http.get<CustomMenuConfig>(`/api/customMenu/${customMenuId}/config`, defaultHttpOptionsFromConfig(config));
  }

  public updateCustomMenuConfig(customMenuId: string, menuConfig: CustomMenuConfig,
                                config?: RequestConfig): Observable<CustomMenu> {
    return this.http.put<CustomMenu>(`/api/customMenu/${customMenuId}/config`, menuConfig,
      defaultHttpOptionsFromConfig(config));
  }

  public updateCustomMenuName(customMenuId: string, name: string, config?: RequestConfig): Observable<CustomMenu> {
    return this.http.put<CustomMenu>(`/api/customMenu/${customMenuId}/name?name=${encodeURIComponent(name)}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public updateCustomMenuAssigneeList(customMenuId: string, assigneeType: CMAssigneeType,
                                      assignToIds?: string[], force = false,
                                      config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/customMenu/${customMenuId}/assign/${assigneeType}?force=${force}`,
      assignToIds || [], defaultHttpOptionsFromConfig(config));
  }

  public getCustomMenuAssigneeList(customMenuId: string, config?: RequestConfig): Observable<CustomMenuAssignee[]> {
    return this.http.get<CustomMenuAssignee[]>(`/api/customMenu/${customMenuId}/assigneeList`,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteCustomMenu(customMenuId: string, force = false, config?: RequestConfig): Observable<CustomMenuDeleteResult> {
    return this.http.delete<CustomMenuDeleteResult>(`/api/customMenu/${customMenuId}?force=${force}`,
      defaultHttpOptionsFromConfig(config));
  }

  // Effective menu for the current user (resolved server-side: user override → customer
  // override → tenant default → system-provided tenant default → empty).
  public getCustomMenu(config?: RequestConfig): Observable<CustomMenuConfig> {
    return this.http.get<CustomMenuConfig>('/api/customMenu', defaultHttpOptionsFromConfig(config));
  }
}
