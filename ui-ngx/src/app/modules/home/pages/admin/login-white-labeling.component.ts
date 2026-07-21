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
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { DomainService } from '@core/http/domain.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { LoginWhiteLabelingParams } from '@shared/models/white-labeling.models';
import {
  CustomCssDialogComponent,
  CustomCssDialogData
} from '@home/pages/admin/custom-css-dialog.component';

@Component({
  selector: 'tb-login-white-labeling',
  templateUrl: './login-white-labeling.component.html',
  styleUrls: ['./white-labeling.component.scss', './settings-card.scss'],
  standalone: false
})
export class LoginWhiteLabelingComponent extends PageComponent implements OnInit, HasConfirmForm {

  loginWhiteLabelingForm: UntypedFormGroup;

  private customCss: string = null;
  private readonly isSysAdmin = getCurrentAuthState(this.store).authUser.authority === Authority.SYS_ADMIN;

  constructor(protected store: Store<AppState>,
              private whiteLabelingService: WhiteLabelingService,
              private domainService: DomainService,
              private translate: TranslateService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.loginWhiteLabelingForm = this.fb.group({
      baseUrl: [null, [Validators.required]],
      prohibitDifferentUrl: [false],
      appTitle: [null],
      favicon: [null],
      logo: [null],
      logoImageHeight: [null],
      primaryPalette: [null],
      accentPalette: [null],
      darkForeground: [false],
      pageBackgroundColor: [null],
      showNameBottom: [false]
    });
    this.load();
  }

  private load() {
    this.whiteLabelingService.getCurrentLoginWhiteLabelParams().subscribe(
      (params) => this.setParams(params)
    );
  }

  private setParams(params: LoginWhiteLabelingParams) {
    params = params || {};
    this.customCss = params.customCss || null;
    this.loginWhiteLabelingForm.reset({
      baseUrl: params.baseUrl,
      prohibitDifferentUrl: params.prohibitDifferentUrl || false,
      appTitle: params.appTitle,
      favicon: params.favicon?.url || null,
      logo: params.logoImageUrl || null,
      logoImageHeight: params.logoImageHeight,
      primaryPalette: params.paletteSettings?.primaryPalette || null,
      accentPalette: params.paletteSettings?.accentPalette || null,
      darkForeground: params.darkForeground || false,
      pageBackgroundColor: params.pageBackgroundColor || null,
      showNameBottom: params.showNameBottom || false
    });
  }

  private buildParams(): LoginWhiteLabelingParams {
    const v = this.loginWhiteLabelingForm.value;
    return {
      baseUrl: v.baseUrl,
      prohibitDifferentUrl: v.prohibitDifferentUrl,
      appTitle: v.appTitle,
      favicon: v.favicon ? { url: v.favicon } : null,
      logoImageUrl: v.logo,
      logoImageHeight: v.logoImageHeight,
      paletteSettings: {
        primaryPalette: v.primaryPalette,
        accentPalette: v.accentPalette
      },
      darkForeground: v.darkForeground,
      pageBackgroundColor: v.pageBackgroundColor,
      showNameBottom: v.showNameBottom,
      customCss: this.customCss
    };
  }

  openCustomCss() {
    this.dialog.open<CustomCssDialogComponent, CustomCssDialogData, string>(CustomCssDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: { customCss: this.customCss, readonly: false }
    }).afterClosed().subscribe((result) => {
      if (result !== null && result !== undefined) {
        this.customCss = result;
        this.loginWhiteLabelingForm.markAsDirty();
      }
    });
  }

  save(): void {
    if (!this.loginWhiteLabelingForm.valid) {
      this.loginWhiteLabelingForm.markAllAsTouched();
      return;
    }
    const params = this.buildParams();
    if (this.isSysAdmin) {
      this.doSave(params);
    } else {
      // Tenant/customer login white labeling is resolved by domain: the backend requires a
      // domainId matching the base URL host (domains are created by the system administrator).
      let host: string;
      try {
        host = new URL(params.baseUrl).host?.split(':')[0];
      } catch (e) {
        host = null;
      }
      if (!host) {
        this.showError(this.translate.instant('white-labeling.base-url-required'));
        return;
      }
      this.domainService.resolveDomainByName(host, { ignoreErrors: true }).subscribe({
        next: (domain) => {
          params.domainId = { entityType: EntityType.DOMAIN, id: domain.id.id };
          this.doSave(params);
        },
        error: () => this.showError(this.translate.instant('white-labeling.domain-not-found', { domain: host }))
      });
    }
  }

  private doSave(params: LoginWhiteLabelingParams) {
    this.whiteLabelingService.saveLoginWhiteLabelParams(params).subscribe(
      (saved) => {
        this.setParams(saved);
        this.loginWhiteLabelingForm.markAsPristine();
      }
    );
  }

  private showError(message: string) {
    this.store.dispatch(new ActionNotificationShow({ message, type: 'error' }));
  }

  reset(): void {
    this.whiteLabelingService.deleteLoginWhiteLabelParams().subscribe(
      () => this.load()
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.loginWhiteLabelingForm;
  }

}
