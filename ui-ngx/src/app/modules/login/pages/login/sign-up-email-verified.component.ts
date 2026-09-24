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
import { ActivatedRoute, Router } from '@angular/router';
import { SelfRegistrationService } from '@core/http/self-registration.service';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'tb-sign-up-email-verified',
  templateUrl: './sign-up-email-verified.component.html',
  styleUrls: ['./password.component.scss'],
  standalone: false
})
export class SignUpEmailVerifiedComponent extends PageComponent implements OnInit {

  activated = false;
  failed = false;

  constructor(protected store: Store<AppState>,
              private selfRegistrationService: SelfRegistrationService,
              private authService: AuthService,
              private route: ActivatedRoute,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
    const emailCode = this.route.snapshot.queryParamMap.get('emailCode');
    if (!emailCode) {
      this.failed = true;
      return;
    }
    this.selfRegistrationService.activateUserByEmailCode(emailCode, { ignoreErrors: true }).subscribe({
      next: (jwtPair) => {
        this.activated = true;
        // The endpoint returns a token pair precisely so the freshly activated user does not
        // have to log in again; discarding it would send them back to the login form.
        if (jwtPair?.token && jwtPair?.refreshToken) {
          this.authService.setUserFromJwtToken(jwtPair.token, jwtPair.refreshToken, true).subscribe();
        }
      },
      error: () => this.failed = true
    });
  }

  gotoLogin() {
    this.router.navigateByUrl('login');
  }
}
