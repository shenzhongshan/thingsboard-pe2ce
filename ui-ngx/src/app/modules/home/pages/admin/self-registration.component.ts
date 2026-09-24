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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { EditorOptions } from 'tinymce';
import { SelfRegistrationService } from '@core/http/self-registration.service';
import { DomainService } from '@core/http/domain.service';
import { EntityType } from '@shared/models/entity-type.models';
import { PageLink } from '@shared/models/page/page-link';
import { DomainInfo } from '@shared/models/oauth2.models';
import {
  captchaVersionTranslations,
  CaptchaVersion,
  SignUpFieldId,
  WebSelfRegistrationParams
} from '@shared/models/self-registration.models';

@Component({
  selector: 'tb-self-registration',
  templateUrl: './self-registration.component.html',
  styleUrls: ['./self-registration.component.scss', './settings-card.scss'],
  standalone: false
})
export class SelfRegistrationComponent extends PageComponent implements OnInit, HasConfirmForm {

  selfRegistrationForm: UntypedFormGroup;
  captchaVersions = Object.values(CaptchaVersion);
  captchaVersionTranslations = captchaVersionTranslations;
  CaptchaVersion = CaptchaVersion;
  EntityType = EntityType;
  domains: DomainInfo[] = [];

  /** Last loaded params, kept so buildParams() can preserve fields this page does not expose. */
  private loadedParams: WebSelfRegistrationParams = {} as WebSelfRegistrationParams;

  tinyMceOptions: Partial<EditorOptions> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['link', 'table', 'image', 'lists', 'code', 'fullscreen'],
    menubar: 'edit insert tools view format table',
    toolbar: 'undo redo | fontfamily fontsize blocks | bold italic strikethrough | forecolor backcolor ' +
      '| link table image | alignleft aligncenter alignright alignjustify ' +
      '| numlist bullist | outdent indent | removeformat | code | fullscreen',
    toolbar_mode: 'sliding',
    height: 320,
    autofocus: false,
    branding: false,
    promotion: false,
    relative_urls: false,
    urlconverter_callback: (url) => url
  };

  constructor(protected store: Store<AppState>,
              private selfRegistrationService: SelfRegistrationService,
              private domainService: DomainService,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.domainService.getTenantDomainInfos(new PageLink(100), { ignoreErrors: true }).subscribe(
      (page) => this.domains = page.data || []
    );
    this.selfRegistrationForm = this.fb.group({
      domainId: [null, [Validators.required]],
      captchaVersion: [CaptchaVersion.V2],
      captchaLogActionName: [''],
      captchaSiteKey: ['', [Validators.required]],
      captchaSecretKey: ['', [Validators.required]],
      notificationRecipient: [null, [Validators.required]],
      title: ['', [Validators.maxLength(200)]],
      defaultDashboardId: [null],
      defaultDashboardFullscreen: [false],
      permissions: [[]],
      showPrivacyPolicy: [false],
      privacyPolicy: [''],
      showTermsOfUse: [false],
      termsOfUse: ['']
    });
    // captchaSiteKey/captchaSecretKey are meaningless for the local (self-hosted) captcha - clear
    // their validators when it's selected, or the form is left permanently invalid+invisible once
    // those fields are hidden (see the project's own past "conditional required validator leak"
    // incident: hiding a field in the template is not enough, the validator must be cleared too).
    this.selfRegistrationForm.get('captchaVersion').valueChanges.subscribe((version: CaptchaVersion) => {
      this.updateCaptchaValidators(version);
    });
    this.selfRegistrationService.getSelfRegistrationParams().subscribe(
      (params) => this.setParams(params)
    );
  }

  private updateCaptchaValidators(version: CaptchaVersion): void {
    const siteKeyCtrl = this.selfRegistrationForm.get('captchaSiteKey');
    const secretKeyCtrl = this.selfRegistrationForm.get('captchaSecretKey');
    if (version === CaptchaVersion.LOCAL) {
      siteKeyCtrl.clearValidators();
      secretKeyCtrl.clearValidators();
    } else {
      siteKeyCtrl.setValidators([Validators.required]);
      secretKeyCtrl.setValidators([Validators.required]);
    }
    siteKeyCtrl.updateValueAndValidity({ emitEvent: false });
    secretKeyCtrl.updateValueAndValidity({ emitEvent: false });
  }

  get signUpUrl(): string {
    const domainId = this.selfRegistrationForm?.get('domainId').value;
    const domain = this.domains.find((d) => d.id.id === domainId);
    return domain ? `https://${domain.name}/signup` : '';
  }

  private setParams(params: WebSelfRegistrationParams) {
    params = params || {} as WebSelfRegistrationParams;
    this.loadedParams = params;
    this.selfRegistrationForm.reset({
      domainId: params.domainId?.id || null,
      captchaVersion: params.captcha?.version || CaptchaVersion.V2,
      captchaLogActionName: params.captcha?.logActionName || '',
      captchaSiteKey: params.captcha?.siteKey || '',
      captchaSecretKey: params.captcha?.secretKey || '',
      notificationRecipient: params.notificationRecipient?.id || null,
      title: params.title || '',
      defaultDashboardId: params.defaultDashboard?.id || null,
      defaultDashboardFullscreen: params.defaultDashboard?.fullscreen === true,
      permissions: params.permissions || [],
      showPrivacyPolicy: params.showPrivacyPolicy === true,
      privacyPolicy: params.privacyPolicy || '',
      showTermsOfUse: params.showTermsOfUse === true,
      termsOfUse: params.termsOfUse || ''
    });
    // reset() re-applies whatever validators were set at group-construction time, so the
    // subscription above does not by itself cover "load an already-local-captcha-configured
    // domain" - re-run it explicitly after every reset.
    this.updateCaptchaValidators(this.selfRegistrationForm.get('captchaVersion').value);
  }

  private buildParams(): WebSelfRegistrationParams {
    const v = this.selfRegistrationForm.value;
    // Spread what was loaded first: this page deliberately exposes only the fields PE's tenant
    // self-registration page has, but the backend model also carries customerGroupId,
    // customMenuId, customerTitlePrefix, homeDashboard and a configurable signUpFields list.
    // Rebuilding from scratch would silently wipe any of those set through the REST API.
    const params: WebSelfRegistrationParams = {
      ...this.loadedParams,
      type: 'WEB',
      enabled: true,
      domainId: v.domainId ? { entityType: EntityType.DOMAIN, id: v.domainId } as any : null,
      title: v.title,
      captcha: {
        version: v.captchaVersion,
        siteKey: v.captchaVersion === CaptchaVersion.LOCAL ? null : v.captchaSiteKey,
        secretKey: v.captchaVersion === CaptchaVersion.LOCAL ? null : v.captchaSecretKey,
        logActionName: v.captchaVersion === CaptchaVersion.V3 ? v.captchaLogActionName : null
      },
      notificationRecipient: v.notificationRecipient
        ? { entityType: EntityType.NOTIFICATION_TARGET, id: v.notificationRecipient } as any : null,
      permissions: v.permissions || [],
      showPrivacyPolicy: v.showPrivacyPolicy,
      privacyPolicy: v.privacyPolicy,
      showTermsOfUse: v.showTermsOfUse,
      termsOfUse: v.termsOfUse,
      signUpFields: this.loadedParams?.signUpFields?.length ? this.loadedParams.signUpFields : [
        { id: SignUpFieldId.EMAIL, required: true },
        { id: SignUpFieldId.FIRST_NAME, required: true },
        { id: SignUpFieldId.LAST_NAME, required: true },
        { id: SignUpFieldId.PASSWORD, required: true }
      ]
    };
    if (v.defaultDashboardId) {
      params.defaultDashboard = {
        id: v.defaultDashboardId,
        fullscreen: v.defaultDashboardFullscreen === true
      };
    } else {
      delete params.defaultDashboard;
    }
    return params;
  }

  save(): void {
    if (!this.selfRegistrationForm.valid) {
      this.selfRegistrationForm.markAllAsTouched();
      return;
    }
    this.selfRegistrationService.saveSelfRegistrationParams(this.buildParams()).subscribe(
      (params) => {
        this.setParams(params);
        this.selfRegistrationForm.markAsPristine();
      }
    );
  }

  reset(): void {
    this.selfRegistrationService.deleteSelfRegistrationParams().subscribe(
      () => {
        this.setParams(null);
        this.selfRegistrationForm.markAsPristine();
      }
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.selfRegistrationForm;
  }

}
