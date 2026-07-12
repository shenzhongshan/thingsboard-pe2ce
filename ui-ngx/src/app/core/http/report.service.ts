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
import { map, Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { ReportInfo } from '@shared/models/report.models';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  constructor(private http: HttpClient) {
  }

  public getReportInfos(pageLink: PageLink, reportTemplateId?: string, userId?: string,
                        config?: RequestConfig): Observable<PageData<ReportInfo>> {
    let url = `/api/reports${pageLink.toQuery()}`;
    if (reportTemplateId) {
      url += `&reportTemplateId=${reportTemplateId}`;
    }
    if (userId) {
      url += `&userId=${userId}`;
    }
    return this.http.get<PageData<ReportInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public generateReport(reportTemplateId: string, timezone: string, config?: RequestConfig): Observable<ReportInfo> {
    return this.http.post<ReportInfo>(`/api/report/${reportTemplateId}/generate?timezone=${encodeURIComponent(timezone)}`,
      null, defaultHttpOptionsFromConfig(config));
  }

  public deleteReport(reportId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/report/${reportId}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadReport(reportId: string, fallbackName: string): Observable<void> {
    return this.http.get(`/api/report/${reportId}/download`, {
      responseType: 'arraybuffer',
      observe: 'response'
    }).pipe(map((response) => {
      const contentType = response.headers.get('content-type');
      let filename = fallbackName;
      const disposition = response.headers.get('content-disposition');
      const match = disposition && /filename\*=UTF-8''([^;]+)/.exec(disposition);
      if (match) {
        filename = decodeURIComponent(match[1]);
      }
      const blob = new Blob([response.body], {type: contentType});
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(url);
    }));
  }

}
