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

import {Component, OnDestroy, OnInit} from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Constants } from '@shared/models/constants';
import { Router } from '@angular/router';
import { OAuth2ClientLoginInfo } from '@shared/models/oauth2.models';
import { validateEmail } from '@app/core/utils';
import { PageComponent } from '@shared/components/page.component';
import { WhiteLabelingApplyService } from '@core/services/white-labeling-apply.service';


import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    standalone: false
})
export class LoginComponent extends PageComponent implements OnInit, OnDestroy {

  // 登录成功跳转到主页时发出通知，取消尚未完成的登录页白标请求。
  // 否则接口返回的空 customCss 可能在主页注入 CSS 后将其删除。
  private readonly destroy$ = new Subject<void>();

  passwordViolation = false;
  isLoading = false;

  loginFormGroup = this.fb.group({
    username: ['', [Validators.required, validateEmail]],
    password: ['']
  });
  oauth2Clients: Array<OAuth2ClientLoginInfo> = null;
  logo: string = null;

  constructor(private authService: AuthService,
              public fb: UntypedFormBuilder,
              private whiteLabelingApplyService: WhiteLabelingApplyService,
              private router: Router) {
    super();
  }

  ngOnInit() {
    this.oauth2Clients = this.authService.oauth2Clients;
    this.logo = this.whiteLabelingApplyService.logo;

    // 离开登录页后停止接收 Logo 更新，避免遗留订阅。
    this.whiteLabelingApplyService.logo$
      .pipe(takeUntil(this.destroy$))
      .subscribe((logo) => this.logo = logo);

    // 登录页白标配置可能不含高级 CSS；组件销毁时取消此请求，
    // 防止它晚于主页白标请求完成并移除主页已注入的自定义样式。
    this.whiteLabelingApplyService.loadAndApplyLogin()
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  login(): void {
    if (this.loginFormGroup.valid) {
      this.isLoading = true;
      this.authService.login(this.loginFormGroup.value).pipe(
        finalize(() => {this.isLoading = false;})
      ).subscribe({
        error: (error: HttpErrorResponse) => {
          if (error && error.error && error.error.errorCode) {
            if (error.error.errorCode === Constants.serverErrorCode.credentialsExpired) {
              this.router.navigateByUrl(`login/resetExpiredPassword?resetToken=${error.error.resetToken}`);
            } else if (error.error.errorCode === Constants.serverErrorCode.passwordViolation) {
              this.passwordViolation = true;
            }
          }
        }
      });
    } else {
      this.loginFormGroup.markAllAsTouched();
    }
  }

  getOAuth2Uri(oauth2Client: OAuth2ClientLoginInfo): string {
    let result = "";
    if (this.authService.redirectUrl) {
      result += "?prevUri=" + encodeURIComponent(this.authService.redirectUrl);
    }
    return oauth2Client.url + result;
  }

  ngOnDestroy(): void {
    // 通知所有 takeUntil(destroy$) 管理的订阅立即取消。
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }
}
