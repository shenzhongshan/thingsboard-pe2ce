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
import { Converter, ConverterScriptLanguage } from '@shared/models/converter.models';

export interface TestConverterInput {
  metadata: {[key: string]: string};
  payload: string; // base64
  decoder?: string;
  // downlink
  msg?: string;
  msgType?: string;
  integrationMetadata?: {[key: string]: string};
  encoder?: string;
}

export interface TestConverterResult {
  output: string;
  error: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConverterService {

  constructor(private http: HttpClient) {
  }

  public getConverters(pageLink: PageLink, integrationType?: string,
                       config?: RequestConfig): Observable<PageData<Converter>> {
    let url = `/api/converters${pageLink.toQuery()}`;
    if (integrationType) {
      url += `&integrationType=${integrationType}`;
    }
    return this.http.get<PageData<Converter>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getConverter(converterId: string, config?: RequestConfig): Observable<Converter> {
    return this.http.get<Converter>(`/api/converter/${converterId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveConverter(converter: Converter, config?: RequestConfig): Observable<Converter> {
    return this.http.post<Converter>('/api/converter', converter, defaultHttpOptionsFromConfig(config));
  }

  public deleteConverter(converterId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/converter/${converterId}`, defaultHttpOptionsFromConfig(config));
  }

  public testUpLink(input: TestConverterInput, scriptLang: ConverterScriptLanguage,
                    config?: RequestConfig): Observable<TestConverterResult> {
    return this.http.post<TestConverterResult>(`/api/converter/testUpLink?scriptLang=${scriptLang}`,
      input, defaultHttpOptionsFromConfig(config));
  }

  public testDownLink(input: TestConverterInput, scriptLang: ConverterScriptLanguage,
                      config?: RequestConfig): Observable<TestConverterResult> {
    return this.http.post<TestConverterResult>(`/api/converter/testDownLink?scriptLang=${scriptLang}`,
      input, defaultHttpOptionsFromConfig(config));
  }

}
