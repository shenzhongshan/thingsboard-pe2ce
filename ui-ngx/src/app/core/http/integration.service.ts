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
import { Integration, IntegrationStatus } from '@shared/models/integration.models';

@Injectable({
  providedIn: 'root'
})
export class IntegrationService {

  constructor(private http: HttpClient) {
  }

  public getIntegrations(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Integration>> {
    return this.http.get<PageData<Integration>>(`/api/integrations${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getIntegration(integrationId: string, config?: RequestConfig): Observable<Integration> {
    return this.http.get<Integration>(`/api/integration/${integrationId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveIntegration(integration: Integration, config?: RequestConfig): Observable<Integration> {
    return this.http.post<Integration>('/api/integration', integration, defaultHttpOptionsFromConfig(config));
  }

  public deleteIntegration(integrationId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/integration/${integrationId}`, defaultHttpOptionsFromConfig(config));
  }

  public getIntegrationStatus(integrationId: string, config?: RequestConfig): Observable<IntegrationStatus> {
    return this.http.get<IntegrationStatus>(`/api/integration/${integrationId}/status`,
      defaultHttpOptionsFromConfig(config));
  }

}
