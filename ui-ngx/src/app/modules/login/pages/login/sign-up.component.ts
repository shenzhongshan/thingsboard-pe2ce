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
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { validateEmail } from '@app/core/utils';
import { SelfRegistrationService } from '@core/http/self-registration.service';
import {
  CaptchaVersion,
  SignUpFieldId,
  SignUpRequest,
  SignUpResult,
  SignUpSelfRegistrationParams
} from '@shared/models/self-registration.models';

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
  captchaError: string = null;
  privacyPolicyText: string = null;
  termsOfUseText: string = null;
  showPrivacyPolicyText = false;
  showTermsOfUseText = false;

  private recaptchaResponse: string = null;
  private recaptchaWidgetRendered = false;
  private recaptchaContainerEl: HTMLElement = null;

  @ViewChild('recaptchaContainer')
  set recaptchaContainer(ref: ElementRef) {
    this.recaptchaContainerEl = ref?.nativeElement || null;
    this.tryRenderRecaptcha();
  }

  signUpForm = this.fb.group({
    email: ['', [Validators.required, validateEmail]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    password: ['', [Validators.required]],
    acceptPrivacyPolicy: [false],
    acceptTermsOfUse: [false]
  });

  constructor(protected store: Store<AppState>,
              private selfRegistrationService: SelfRegistrationService,
              private translate: TranslateService,
              private router: Router,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.selfRegistrationService.getSignUpSelfRegistrationParams({ ignoreErrors: true }).subscribe(
      (params) => {
        this.params = params;
        if (params?.showPrivacyPolicy) {
          this.selfRegistrationService.getPrivacyPolicy({ ignoreErrors: true }).subscribe(
            (text) => this.privacyPolicyText = this.unwrapText(text));
        }
        if (params?.showTermsOfUse) {
          this.selfRegistrationService.getTermsOfUse({ ignoreErrors: true }).subscribe(
            (text) => this.termsOfUseText = this.unwrapText(text));
        }
        this.loadRecaptchaScript();
      }
    );
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
    if (this.isV3) {
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
    const v = this.signUpForm.value;
    const request: SignUpRequest = {
      fields: {
        [SignUpFieldId.EMAIL]: v.email,
        [SignUpFieldId.FIRST_NAME]: v.firstName,
        [SignUpFieldId.LAST_NAME]: v.lastName,
        [SignUpFieldId.PASSWORD]: v.password
      },
      recaptchaResponse
    };
    this.selfRegistrationService.signUp(request).subscribe({
      next: (result) => {
        if (result === SignUpResult.INACTIVE_USER_EXISTS) {
          this.store.dispatch(new ActionNotificationShow({
            message: this.translate.instant('signup.email-verification-sent'),
            type: 'warn'
          }));
        }
        this.emailSent = true;
      },
      error: () => {
        // Server rejected (e.g. captcha invalid) — force a fresh captcha for the next attempt.
        if (!this.isV3 && this.recaptchaWidgetRendered && typeof grecaptcha !== 'undefined' && grecaptcha.reset) {
          grecaptcha.reset();
          this.recaptchaResponse = null;
        }
      }
    });
  }

  private showError(message: string) {
    this.store.dispatch(new ActionNotificationShow({ message, type: 'error' }));
  }

  gotoLogin() {
    this.router.navigateByUrl('login');
  }
}
