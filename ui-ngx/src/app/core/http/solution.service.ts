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
  SolutionInstallResponse,
  TenantSolutionTemplateDetails,
  TenantSolutionTemplateInfo,
  TenantSolutionTemplateInstructions
} from '@shared/models/solution-template.models';

@Injectable({
  providedIn: 'root'
})
export class SolutionService {

  constructor(private http: HttpClient) {
  }

  public getSolutionTemplateInfos(config?: RequestConfig): Observable<TenantSolutionTemplateInfo[]> {
    return this.http.get<TenantSolutionTemplateInfo[]>('/api/solutions/templates/infos', defaultHttpOptionsFromConfig(config));
  }

  public getSolutionTemplateDetails(solutionTemplateId: string, config?: RequestConfig): Observable<TenantSolutionTemplateDetails> {
    return this.http.get<TenantSolutionTemplateDetails>(`/api/solutions/templates/details/${solutionTemplateId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getSolutionTemplateInstructions(solutionTemplateId: string,
                                          config?: RequestConfig): Observable<TenantSolutionTemplateInstructions> {
    return this.http.get<TenantSolutionTemplateInstructions>(`/api/solutions/templates/instructions/${solutionTemplateId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public installSolutionTemplate(solutionTemplateId: string, config?: RequestConfig): Observable<SolutionInstallResponse> {
    return this.http.post<SolutionInstallResponse>(`/api/solutions/templates/install/${solutionTemplateId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public uninstallSolutionTemplate(solutionTemplateId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/solutions/templates/uninstall/${solutionTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

}
