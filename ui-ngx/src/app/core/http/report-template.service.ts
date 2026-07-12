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
import { ReportTemplate, ReportTemplateInfo } from '@shared/models/report.models';

@Injectable({
  providedIn: 'root'
})
export class ReportTemplateService {

  constructor(private http: HttpClient) {
  }

  public getReportTemplateInfos(pageLink: PageLink, customerId?: string,
                                 config?: RequestConfig): Observable<PageData<ReportTemplateInfo>> {
    let url = `/api/reportTemplate/infos${pageLink.toQuery()}`;
    if (customerId) {
      url += `&customerId=${customerId}`;
    }
    return this.http.get<PageData<ReportTemplateInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getReportTemplate(reportTemplateId: string, config?: RequestConfig): Observable<ReportTemplate> {
    return this.http.get<ReportTemplate>(`/api/reportTemplate/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveReportTemplate(reportTemplate: ReportTemplate, config?: RequestConfig): Observable<ReportTemplate> {
    return this.http.post<ReportTemplate>('/api/reportTemplate', reportTemplate, defaultHttpOptionsFromConfig(config));
  }

  public deleteReportTemplate(reportTemplateId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/reportTemplate/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

}
