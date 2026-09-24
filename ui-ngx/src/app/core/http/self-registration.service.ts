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
import { LoginResponse } from '@shared/models/login.models';
import {
  SignUpRequest,
  SignUpResult,
  SignUpSelfRegistrationParams,
  WebSelfRegistrationParams
} from '@shared/models/self-registration.models';

@Injectable({
  providedIn: 'root'
})
export class SelfRegistrationService {

  constructor(private http: HttpClient) {
  }

  // --- Tenant admin: manage self-registration params ---
  public saveSelfRegistrationParams(params: WebSelfRegistrationParams,
                                    config?: RequestConfig): Observable<WebSelfRegistrationParams> {
    return this.http.post<WebSelfRegistrationParams>('/api/selfRegistration/selfRegistrationParams', params,
      defaultHttpOptionsFromConfig(config));
  }

  public getSelfRegistrationParams(config?: RequestConfig): Observable<WebSelfRegistrationParams> {
    return this.http.get<WebSelfRegistrationParams>('/api/selfRegistration/selfRegistrationParams',
      defaultHttpOptionsFromConfig(config));
  }

  public deleteSelfRegistrationParams(config?: RequestConfig): Observable<void> {
    return this.http.delete<void>('/api/selfRegistration/selfRegistrationParams', defaultHttpOptionsFromConfig(config));
  }

  // --- Public (no auth): sign-up form descriptor + legal text ---
  public getSignUpSelfRegistrationParams(config?: RequestConfig): Observable<SignUpSelfRegistrationParams> {
    return this.http.get<SignUpSelfRegistrationParams>('/api/noauth/selfRegistration/signUpSelfRegistrationParams',
      defaultHttpOptionsFromConfig(config));
  }

  public getPrivacyPolicy(config?: RequestConfig): Observable<string> {
    return this.http.get('/api/noauth/selfRegistration/privacyPolicy',
      { ...defaultHttpOptionsFromConfig(config), responseType: 'text' as 'json' }) as Observable<string>;
  }

  public getTermsOfUse(config?: RequestConfig): Observable<string> {
    return this.http.get('/api/noauth/selfRegistration/termsOfUse',
      { ...defaultHttpOptionsFromConfig(config), responseType: 'text' as 'json' }) as Observable<string>;
  }

  // --- Public (no auth): sign-up flow ---
  public signUp(signUpRequest: SignUpRequest, config?: RequestConfig): Observable<SignUpResult> {
    return this.http.post<SignUpResult>('/api/noauth/signup', signUpRequest, defaultHttpOptionsFromConfig(config));
  }

  public resendEmailActivation(email: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/noauth/resendEmailActivation?email=${encodeURIComponent(email)}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  /** Returns a JWT pair so the activated user lands logged in rather than back at the login form. */
  public activateUserByEmailCode(emailCode: string, config?: RequestConfig): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`/api/noauth/activateByEmailCode?emailCode=${encodeURIComponent(emailCode)}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  // --- Authenticated: privacy policy / terms acceptance ---
  public privacyPolicyAccepted(config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>('/api/signup/privacyPolicyAccepted', defaultHttpOptionsFromConfig(config));
  }

  public acceptPrivacyPolicy(config?: RequestConfig): Observable<any> {
    return this.http.post<any>('/api/signup/acceptPrivacyPolicy', null, defaultHttpOptionsFromConfig(config));
  }

  public termsOfUseAccepted(config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>('/api/signup/termsOfUseAccepted', defaultHttpOptionsFromConfig(config));
  }

  public acceptTermsOfUse(config?: RequestConfig): Observable<any> {
    return this.http.post<any>('/api/signup/acceptTermsOfUse', null, defaultHttpOptionsFromConfig(config));
  }
}
