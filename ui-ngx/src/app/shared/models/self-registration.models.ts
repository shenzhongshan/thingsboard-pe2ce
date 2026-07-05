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

import { CustomerId } from '@shared/models/id/customer-id';
import { NotificationTargetId } from '@shared/models/id/notification-target-id';
import { DomainId } from '@shared/models/id/domain-id';

export enum SignUpFieldId {
  EMAIL = 'EMAIL',
  FIRST_NAME = 'FIRST_NAME',
  LAST_NAME = 'LAST_NAME',
  PASSWORD = 'PASSWORD',
  PHONE = 'PHONE',
  COUNTRY = 'COUNTRY',
  STATE = 'STATE',
  CITY = 'CITY',
  ADDRESS = 'ADDRESS',
  ADDRESS2 = 'ADDRESS2',
  ZIP = 'ZIP'
}

export const signUpFieldIdTranslations = new Map<SignUpFieldId, string>([
  [SignUpFieldId.EMAIL, 'signup.field-email'],
  [SignUpFieldId.FIRST_NAME, 'signup.field-first-name'],
  [SignUpFieldId.LAST_NAME, 'signup.field-last-name'],
  [SignUpFieldId.PASSWORD, 'signup.field-password'],
  [SignUpFieldId.PHONE, 'signup.field-phone'],
  [SignUpFieldId.COUNTRY, 'signup.field-country'],
  [SignUpFieldId.STATE, 'signup.field-state'],
  [SignUpFieldId.CITY, 'signup.field-city'],
  [SignUpFieldId.ADDRESS, 'signup.field-address'],
  [SignUpFieldId.ADDRESS2, 'signup.field-address2'],
  [SignUpFieldId.ZIP, 'signup.field-zip']
]);

export interface SignUpField {
  id: SignUpFieldId;
  label?: string;
  required: boolean;
}

// Values must match the backend Jackson subtype ids for CaptchaParams (@JsonTypeInfo property="version").
export enum CaptchaVersion {
  V2 = 'v2',
  V3 = 'v3'
}

export interface CaptchaParams {
  version: CaptchaVersion;
  siteKey?: string;
  secretKey?: string;
  logActionName?: string;
}

export interface DefaultDashboardParams {
  id?: string;
  fullscreen?: boolean;
}

export interface HomeDashboardParams {
  id?: string;
  hideToolbar?: boolean;
}

export interface WebSelfRegistrationParams {
  type: 'WEB';
  enabled?: boolean;
  title?: string;
  captcha?: CaptchaParams;
  signUpFields?: SignUpField[];
  showPrivacyPolicy?: boolean;
  showTermsOfUse?: boolean;
  notificationRecipient?: NotificationTargetId;
  customerTitlePrefix?: string;
  customerId?: CustomerId;
  defaultDashboard?: DefaultDashboardParams;
  homeDashboard?: HomeDashboardParams;
  privacyPolicy?: string;
  termsOfUse?: string;
  domainName?: string;
  domainId?: DomainId;
}

// Public form descriptor returned by /api/noauth/selfRegistration/signUpSelfRegistrationParams
export interface SignUpSelfRegistrationParams {
  title?: string;
  captcha?: { version: CaptchaVersion; siteKey?: string; logActionName?: string };
  signUpFields?: SignUpField[];
  showPrivacyPolicy?: boolean;
  showTermsOfUse?: boolean;
}

export interface SignUpRequest {
  fields: { [key in SignUpFieldId]?: string };
  recaptchaResponse?: string;
  pkgName?: string;
  platform?: string;
  appSecret?: string;
}

export enum SignUpResult {
  SUCCESS = 'SUCCESS',
  INACTIVE_USER_EXISTS = 'INACTIVE_USER_EXISTS'
}

export function defaultWebSelfRegistrationParams(): WebSelfRegistrationParams {
  return {
    type: 'WEB',
    enabled: false,
    title: '',
    captcha: { version: CaptchaVersion.V2 },
    signUpFields: [
      { id: SignUpFieldId.EMAIL, required: true },
      { id: SignUpFieldId.FIRST_NAME, required: true },
      { id: SignUpFieldId.LAST_NAME, required: true },
      { id: SignUpFieldId.PASSWORD, required: true }
    ],
    showPrivacyPolicy: false,
    showTermsOfUse: false
  };
}
