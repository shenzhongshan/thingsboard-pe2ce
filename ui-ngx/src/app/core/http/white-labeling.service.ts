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
  LoginWhiteLabelingParams,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';

@Injectable({
  providedIn: 'root'
})
export class WhiteLabelingService {

  constructor(private http: HttpClient) {
  }

  public getMergedWhiteLabelParams(config?: RequestConfig): Observable<WhiteLabelingParams> {
    return this.http.get<WhiteLabelingParams>('/api/whiteLabel/whiteLabelParams', defaultHttpOptionsFromConfig(config));
  }

  public getLoginWhiteLabelParams(config?: RequestConfig): Observable<LoginWhiteLabelingParams> {
    return this.http.get<LoginWhiteLabelingParams>('/api/noauth/whiteLabel/loginWhiteLabelParams',
      defaultHttpOptionsFromConfig(config));
  }

  public getCurrentWhiteLabelParams(customerId?: string, config?: RequestConfig): Observable<WhiteLabelingParams> {
    const url = customerId ? `/api/whiteLabel/currentWhiteLabelParams?customerId=${customerId}`
      : '/api/whiteLabel/currentWhiteLabelParams';
    return this.http.get<WhiteLabelingParams>(url, defaultHttpOptionsFromConfig(config));
  }

  public getCurrentLoginWhiteLabelParams(customerId?: string, config?: RequestConfig): Observable<LoginWhiteLabelingParams> {
    const url = customerId ? `/api/whiteLabel/currentLoginWhiteLabelParams?customerId=${customerId}`
      : '/api/whiteLabel/currentLoginWhiteLabelParams';
    return this.http.get<LoginWhiteLabelingParams>(url, defaultHttpOptionsFromConfig(config));
  }

  public saveWhiteLabelParams(whiteLabelingParams: WhiteLabelingParams, customerId?: string,
                              config?: RequestConfig): Observable<WhiteLabelingParams> {
    const url = customerId ? `/api/whiteLabel/whiteLabelParams?customerId=${customerId}`
      : '/api/whiteLabel/whiteLabelParams';
    return this.http.post<WhiteLabelingParams>(url, whiteLabelingParams, defaultHttpOptionsFromConfig(config));
  }

  public saveLoginWhiteLabelParams(loginWhiteLabelingParams: LoginWhiteLabelingParams, customerId?: string,
                                   config?: RequestConfig): Observable<LoginWhiteLabelingParams> {
    const url = customerId ? `/api/whiteLabel/loginWhiteLabelParams?customerId=${customerId}`
      : '/api/whiteLabel/loginWhiteLabelParams';
    return this.http.post<LoginWhiteLabelingParams>(url, loginWhiteLabelingParams, defaultHttpOptionsFromConfig(config));
  }

  public previewWhiteLabelParams(whiteLabelingParams: WhiteLabelingParams,
                                 config?: RequestConfig): Observable<WhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/previewWhiteLabelParams', whiteLabelingParams,
      defaultHttpOptionsFromConfig(config));
  }

  public isWhiteLabelingAllowed(config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>('/api/whiteLabel/isWhiteLabelingAllowed', defaultHttpOptionsFromConfig(config));
  }

  public isCustomerWhiteLabelingAllowed(config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>('/api/whiteLabel/isCustomerWhiteLabelingAllowed', defaultHttpOptionsFromConfig(config));
  }

  public getMailTemplates(systemByDefault = false, config?: RequestConfig): Observable<any> {
    return this.http.get<any>(`/api/whiteLabel/mailTemplates?systemByDefault=${systemByDefault}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveMailTemplates(mailTemplates: any, config?: RequestConfig): Observable<any> {
    return this.http.post<any>('/api/whiteLabel/mailTemplates', mailTemplates, defaultHttpOptionsFromConfig(config));
  }

  public deleteWhiteLabelParams(customerId?: string, config?: RequestConfig): Observable<void> {
    const url = customerId ? `/api/whiteLabel/currentWhiteLabelParams?customerId=${customerId}`
      : '/api/whiteLabel/currentWhiteLabelParams';
    return this.http.delete<void>(url, defaultHttpOptionsFromConfig(config));
  }

  public deleteLoginWhiteLabelParams(customerId?: string, config?: RequestConfig): Observable<void> {
    const url = customerId ? `/api/whiteLabel/currentLoginWhiteLabelParams?customerId=${customerId}`
      : '/api/whiteLabel/currentLoginWhiteLabelParams';
    return this.http.delete<void>(url, defaultHttpOptionsFromConfig(config));
  }
}
