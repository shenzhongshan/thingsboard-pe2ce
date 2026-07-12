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

@Injectable({
  providedIn: 'root'
})
export class CustomTranslationService {

  constructor(private http: HttpClient) {
  }

  public getMergedCustomTranslation(localeCode: string, customerId?: string, config?: RequestConfig): Observable<any> {
    const url = customerId ? `/api/translation/custom/merged/${localeCode}?customerId=${customerId}`
      : `/api/translation/custom/merged/${localeCode}`;
    return this.http.get<any>(url, defaultHttpOptionsFromConfig(config));
  }

  public getCustomTranslation(localeCode: string, customerId?: string, config?: RequestConfig): Observable<any> {
    const url = customerId ? `/api/translation/custom/${localeCode}?customerId=${customerId}`
      : `/api/translation/custom/${localeCode}`;
    return this.http.get<any>(url, defaultHttpOptionsFromConfig(config));
  }

  public getCustomizedLocales(customerId?: string, config?: RequestConfig): Observable<string[]> {
    const url = customerId ? `/api/translation/custom/locales?customerId=${customerId}`
      : '/api/translation/custom/locales';
    return this.http.get<string[]>(url, defaultHttpOptionsFromConfig(config));
  }

  public saveCustomTranslation(localeCode: string, value: any, customerId?: string, config?: RequestConfig): Observable<any> {
    const url = customerId ? `/api/translation/custom/${localeCode}?customerId=${customerId}`
      : `/api/translation/custom/${localeCode}`;
    return this.http.post<any>(url, value, defaultHttpOptionsFromConfig(config));
  }

  public patchCustomTranslation(localeCode: string, patch: any, customerId?: string, config?: RequestConfig): Observable<any> {
    const url = customerId ? `/api/translation/custom/${localeCode}?customerId=${customerId}`
      : `/api/translation/custom/${localeCode}`;
    return this.http.patch<any>(url, patch, defaultHttpOptionsFromConfig(config));
  }

  public deleteCustomTranslationKey(localeCode: string, keyPath: string, customerId?: string, config?: RequestConfig): Observable<void> {
    const url = customerId ? `/api/translation/custom/${localeCode}/${encodeURIComponent(keyPath)}?customerId=${customerId}`
      : `/api/translation/custom/${localeCode}/${encodeURIComponent(keyPath)}`;
    return this.http.delete<void>(url, defaultHttpOptionsFromConfig(config));
  }

  public deleteCustomTranslation(localeCode: string, customerId?: string, config?: RequestConfig): Observable<void> {
    const url = customerId ? `/api/translation/custom/${localeCode}?customerId=${customerId}`
      : `/api/translation/custom/${localeCode}`;
    return this.http.delete<void>(url, defaultHttpOptionsFromConfig(config));
  }

}
