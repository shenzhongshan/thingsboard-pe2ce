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

import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { validateEmail } from '@app/core/utils';
import { SelfRegistrationService } from '@core/http/self-registration.service';
import { passwordsMatchValidator, passwordStrengthValidator } from '@shared/models/password.models';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import {
  CaptchaVersion,
  passwordSignUpFields,
  SignUpField,
  SignUpFieldId,
  signUpFieldIdTranslations,
  SignUpRequest,
  SignUpResult,
  SignUpSelfRegistrationParams
} from '@shared/models/self-registration.models';

/** Matches the backend fallback in SignUpController#validateRequiredFields. */
const DEFAULT_SIGN_UP_FIELDS: SignUpField[] = [
  { id: SignUpFieldId.EMAIL, required: true },
  { id: SignUpFieldId.FIRST_NAME, required: true },
  { id: SignUpFieldId.LAST_NAME, required: true },
  { id: SignUpFieldId.PASSWORD, required: true }
];

declare const grecaptcha: any;

// www.google.com is blocked in some regions (e.g. mainland China); recaptcha.net serves
// the exact same API worldwide.
const RECAPTCHA_SCRIPT_URL = 'https://www.recaptcha.net/recaptcha/api.js?render=explicit';

@Component({
  selector: 'tb-sign-up',
  templateUrl: './sign-up.component.html',
  styleUrls: ['./password.component.scss'],
  standalone: false
})
export class SignUpComponent extends PageComponent implements OnInit {

  params: SignUpSelfRegistrationParams = null;
  emailSent = false;
  inactiveUserExists = false;
  resending = false;
  captchaError: string = null;
  privacyPolicyText: string = null;
  termsOfUseText: string = null;
  showPrivacyPolicyText = false;
  showTermsOfUseText = false;

  /** The fields actually rendered, driven by the tenant's self-registration configuration. */
  fields: SignUpField[] = [];
  signUpFieldIdTranslations = signUpFieldIdTranslations;
  SignUpFieldId = SignUpFieldId;

  localCaptchaImage: string = null;
  localCaptchaId: string = null;

  private passwordPolicy: UserPasswordPolicy;
  private submittedEmail: string = null;
  private recaptchaResponse: string = null;
  private recaptchaWidgetRendered = false;
  private recaptchaContainerEl: HTMLElement = null;

  @ViewChild('recaptchaContainer')
  set recaptchaContainer(ref: ElementRef) {
    this.recaptchaContainerEl = ref?.nativeElement || null;
    this.tryRenderRecaptcha();
  }

  signUpForm = this.fb.group({
    acceptPrivacyPolicy: [false],
    acceptTermsOfUse: [false]
  });

  constructor(protected store: Store<AppState>,
              private selfRegistrationService: SelfRegistrationService,
              private translate: TranslateService,
              private router: Router,
              private route: ActivatedRoute,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.passwordPolicy = this.route.snapshot.data.passwordPolicy;
    this.selfRegistrationService.getSignUpSelfRegistrationParams({ ignoreErrors: true }).subscribe(
      (params) => {
        this.params = params;
        this.buildFields();
        if (params?.showPrivacyPolicy) {
          this.selfRegistrationService.getPrivacyPolicy({ ignoreErrors: true }).subscribe(
            (text) => this.privacyPolicyText = this.unwrapText(text));
        }
        if (params?.showTermsOfUse) {
          this.selfRegistrationService.getTermsOfUse({ ignoreErrors: true }).subscribe(
            (text) => this.termsOfUseText = this.unwrapText(text));
        }
        if (this.isLocal) {
          this.signUpForm.addControl('localCaptchaAnswer', this.fb.control('', [Validators.required]));
          this.loadLocalCaptcha();
        } else {
          this.loadRecaptchaScript();
        }
      }
    );
  }

  get isLocal(): boolean {
    return this.params?.captcha?.version === CaptchaVersion.LOCAL;
  }

  private loadLocalCaptcha() {
    this.selfRegistrationService.getLocalCaptcha({ ignoreErrors: true }).subscribe({
      next: (challenge) => {
        this.localCaptchaId = challenge.captchaId;
        this.localCaptchaImage = challenge.image;
      },
      error: () => this.captchaError = this.translate.instant('signup.captcha-load-failed')
    });
  }

  refreshLocalCaptcha() {
    this.loadLocalCaptcha();
    this.signUpForm.get('localCaptchaAnswer')?.setValue('');
  }

  /**
   * Builds one control per configured sign-up field. Without this the form was hardcoded to
   * email/first/last/password, so PHONE, COUNTRY, STATE, CITY, ADDRESS, ADDRESS2 and ZIP were
   * never sent and the matching Customer/User columns stayed null no matter how the tenant
   * configured them.
   */
  private buildFields() {
    this.fields = (this.params?.signUpFields?.length ? this.params.signUpFields : DEFAULT_SIGN_UP_FIELDS)
      .filter(f => !!f?.id);
    for (const field of this.fields) {
      const validators = field.required ? [Validators.required] : [];
      if (field.id === SignUpFieldId.EMAIL) {
        validators.push(validateEmail);
      }
      if (field.id === SignUpFieldId.PASSWORD) {
        validators.push(passwordStrengthValidator(this.passwordPolicy));
      }
      this.signUpForm.addControl(field.id, this.fb.control('', validators));
    }
    if (this.hasField(SignUpFieldId.REPEAT_PASSWORD) && this.hasField(SignUpFieldId.PASSWORD)) {
      this.signUpForm.addValidators(
        passwordsMatchValidator(SignUpFieldId.PASSWORD, SignUpFieldId.REPEAT_PASSWORD));
      this.signUpForm.updateValueAndValidity({ emitEvent: false });
    }
  }

  hasField(id: SignUpFieldId): boolean {
    return this.fields.some(f => f.id === id);
  }

  fieldLabel(field: SignUpField): string {
    return field.label || this.translate.instant(signUpFieldIdTranslations.get(field.id) || field.id);
  }

  fieldInputType(field: SignUpField): string {
    if (passwordSignUpFields.includes(field.id)) {
      return 'password';
    }
    return field.id === SignUpFieldId.EMAIL ? 'email' : 'text';
  }

  private unwrapText(text: string): string {
    if (!text) {
      return null;
    }
    // The endpoint returns a JSON string literal ("<p>...</p>").
    try {
      const parsed = JSON.parse(text);
      return typeof parsed === 'string' ? parsed : text;
    } catch (e) {
      return text;
    }
  }

  get isV3(): boolean {
    return this.params?.captcha?.version === CaptchaVersion.V3;
  }

  private loadRecaptchaScript() {
    const siteKey = this.params?.captcha?.siteKey;
    if (!siteKey) {
      return;
    }
    if (typeof grecaptcha !== 'undefined') {
      this.tryRenderRecaptcha();
      return;
    }
    let script = document.getElementById('tb-recaptcha-script') as HTMLScriptElement;
    if (!script) {
      script = document.createElement('script');
      script.id = 'tb-recaptcha-script';
      script.src = RECAPTCHA_SCRIPT_URL + (this.isV3 ? `&render=${this.params.captcha.siteKey}` : '');
      script.async = true;
      script.defer = true;
      script.onload = () => setTimeout(() => this.tryRenderRecaptcha(), 300);
      script.onerror = () => this.captchaError = this.translate.instant('signup.captcha-load-failed');
      document.head.appendChild(script);
    } else {
      setTimeout(() => this.tryRenderRecaptcha(), 300);
    }
  }

  private tryRenderRecaptcha() {
    const siteKey = this.params?.captcha?.siteKey;
    if (this.isV3 || !siteKey || this.recaptchaWidgetRendered || !this.recaptchaContainerEl ||
        typeof grecaptcha === 'undefined') {
      return;
    }
    const render = () => {
      if (grecaptcha.render && this.recaptchaContainerEl.childElementCount === 0) {
        grecaptcha.render(this.recaptchaContainerEl, {
          sitekey: siteKey,
          callback: (token: string) => {
            this.recaptchaResponse = token;
            this.captchaError = null;
          },
          'expired-callback': () => this.recaptchaResponse = null
        });
        this.recaptchaWidgetRendered = true;
      }
    };
    if (grecaptcha.ready) {
      grecaptcha.ready(render);
    } else {
      render();
    }
  }

  signUp() {
    if (!this.signUpForm.valid) {
      this.signUpForm.markAllAsTouched();
      return;
    }
    if (this.params?.showPrivacyPolicy && !this.signUpForm.get('acceptPrivacyPolicy').value) {
      this.showError(this.translate.instant('signup.accept-privacy-policy-required'));
      return;
    }
    if (this.params?.showTermsOfUse && !this.signUpForm.get('acceptTermsOfUse').value) {
      this.showError(this.translate.instant('signup.accept-terms-of-use-required'));
      return;
    }
    if (this.isLocal) {
      const answer = this.signUpForm.get('localCaptchaAnswer')?.value;
      if (!answer) {
        this.captchaError = this.translate.instant('signup.captcha-required');
        return;
      }
      this.doSignUp(null);
    } else if (this.isV3) {
      if (typeof grecaptcha === 'undefined') {
        this.captchaError = this.translate.instant('signup.captcha-load-failed');
        return;
      }
      grecaptcha.ready(() => {
        grecaptcha.execute(this.params.captcha.siteKey,
          { action: this.params.captcha.logActionName || 'signup' }).then((token: string) => {
          this.doSignUp(token);
        });
      });
    } else {
      if (!this.recaptchaResponse) {
        this.captchaError = this.translate.instant('signup.captcha-required');
        return;
      }
      this.doSignUp(this.recaptchaResponse);
    }
  }

  private doSignUp(recaptchaResponse: string) {
    const fields: { [key in SignUpFieldId]?: string } = {};
    for (const field of this.fields) {
      // REPEAT_PASSWORD is a client-side confirmation only; the backend has no use for it.
      if (field.id === SignUpFieldId.REPEAT_PASSWORD) {
        continue;
      }
      const value = this.signUpForm.get(field.id)?.value;
      if (value !== null && value !== undefined && value !== '') {
        fields[field.id] = value;
      }
    }
    this.submittedEmail = fields[SignUpFieldId.EMAIL];
    const request: SignUpRequest = this.isLocal
      ? { fields, captchaId: this.localCaptchaId, captchaAnswer: this.signUpForm.get('localCaptchaAnswer')?.value }
      : { fields, recaptchaResponse };
    this.selfRegistrationService.signUp(request).subscribe({
      next: (result) => {
        if (result === SignUpResult.INACTIVE_USER_EXISTS) {
          this.inactiveUserExists = true;
        } else {
          this.emailSent = true;
        }
      },
      error: () => {
        if (this.isLocal) {
          // Server rejected (e.g. wrong answer, or the challenge already expired/was consumed) -
          // the challenge is one-shot, so a retry always needs a fresh image.
          this.refreshLocalCaptcha();
        } else if (!this.isV3 && this.recaptchaWidgetRendered && typeof grecaptcha !== 'undefined' && grecaptcha.reset) {
          // Server rejected (e.g. captcha invalid) — force a fresh captcha for the next attempt.
          grecaptcha.reset();
          this.recaptchaResponse = null;
        }
      }
    });
  }

  resendActivationEmail() {
    if (!this.submittedEmail || this.resending) {
      return;
    }
    this.resending = true;
    this.selfRegistrationService.resendEmailActivation(this.submittedEmail).subscribe({
      next: () => {
        this.resending = false;
        this.inactiveUserExists = false;
        this.emailSent = true;
      },
      error: () => this.resending = false
    });
  }

  private showError(message: string) {
    this.store.dispatch(new ActionNotificationShow({ message, type: 'error' }));
  }

  gotoLogin() {
    this.router.navigateByUrl('login');
  }
}
