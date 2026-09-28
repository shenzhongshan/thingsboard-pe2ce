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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AdminRoutingModule } from './admin-routing.module';
import { SharedModule } from '@app/shared/shared.module';
import { MailServerComponent } from '@modules/home/pages/admin/mail-server.component';
import { GeneralSettingsComponent } from '@modules/home/pages/admin/general-settings.component';
import { SecuritySettingsComponent } from '@modules/home/pages/admin/security-settings.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { SmsProviderComponent } from '@home/pages/admin/sms-provider.component';
import { SendTestSmsDialogComponent } from '@home/pages/admin/send-test-sms-dialog.component';
import { HomeSettingsComponent } from '@home/pages/admin/home-settings.component';
import { ResourceTabsComponent } from '@home/pages/admin/resource/resource-tabs.component';
import { ResourcesTableHeaderComponent } from '@home/pages/admin/resource/resources-table-header.component';
import { QueueComponent } from '@home/pages/admin/queue/queue.component';
import { RepositoryAdminSettingsComponent } from '@home/pages/admin/repository-admin-settings.component';
import { AutoCommitAdminSettingsComponent } from '@home/pages/admin/auto-commit-admin-settings.component';
import { TwoFactorAuthSettingsComponent } from '@home/pages/admin/two-factor-auth-settings.component';
import { OAuth2Module } from '@home/pages/admin/oauth2/oauth2.module';
import { JsLibraryTableHeaderComponent } from '@home/pages/admin/resource/js-library-table-header.component';
import { JsResourceComponent } from '@home/pages/admin/resource/js-resource.component';
import { NgxFlowModule } from '@flowjs/ngx-flow';
import { TrendzSettingsComponent } from '@home/pages/admin/trendz-settings.component';
import { ResourceLibraryTabsComponent } from '@home/pages/admin/resource/resource-library-tabs.component';
import { WhiteLabelingComponent } from '@home/pages/admin/white-labeling.component';
import { LoginWhiteLabelingComponent } from '@home/pages/admin/login-white-labeling.component';
import { MailTemplatesComponent } from '@home/pages/admin/mail-templates.component';
import { PaletteDialogComponent } from '@home/pages/admin/palette-dialog.component';
import { WlPaletteSelectComponent } from '@home/pages/admin/wl-palette-select.component';
import { CustomCssDialogComponent } from '@home/pages/admin/custom-css-dialog.component';
import { CustomMenuItemDialogComponent } from '@home/pages/admin/custom-menu-item-dialog.component';
import {
  CustomMenuDefaultItemDialogComponent
} from '@home/pages/admin/custom-menu-default-item-dialog.component';
import { SelfRegistrationComponent } from '@home/pages/admin/self-registration.component';
import {
  SelfRegistrationPermissionsComponent
} from '@home/pages/admin/self-registration-permissions.component';
import {
  SelfRegistrationPermissionDialogComponent
} from '@home/pages/admin/self-registration-permission-dialog.component';
import { CustomMenuComponent } from '@home/pages/admin/custom-menu.component';
import { CustomMenuListComponent } from '@home/pages/admin/custom-menu-list.component';
import { CustomMenuInfoDialogComponent } from '@home/pages/admin/custom-menu-info-dialog.component';
import { CustomTranslationListComponent } from '@home/pages/admin/custom-translation-list.component';
import { CustomTranslationComponent } from '@home/pages/admin/custom-translation.component';
import {
  CustomTranslationAddDialogComponent
} from '@home/pages/admin/custom-translation-add-dialog.component';
import { SecretsComponent } from '@home/pages/admin/secrets.component';
import { SecretDialogComponent } from '@home/pages/admin/secret-dialog.component';
import { SecretChangeValueDialogComponent } from '@home/pages/admin/secret-change-value-dialog.component';
import {
  SecretEditDescriptionDialogComponent
} from '@home/pages/admin/secret-edit-description-dialog.component';

@NgModule({
  declarations:
    [
      GeneralSettingsComponent,
      MailServerComponent,
      SmsProviderComponent,
      SendTestSmsDialogComponent,
      SecuritySettingsComponent,
      HomeSettingsComponent,
      ResourceTabsComponent,
      ResourceLibraryTabsComponent,
      ResourcesTableHeaderComponent,
      JsResourceComponent,
      JsLibraryTableHeaderComponent,
      QueueComponent,
      RepositoryAdminSettingsComponent,
      AutoCommitAdminSettingsComponent,
      TwoFactorAuthSettingsComponent,
      TrendzSettingsComponent,
      WhiteLabelingComponent,
      LoginWhiteLabelingComponent,
      MailTemplatesComponent,
      PaletteDialogComponent,
      WlPaletteSelectComponent,
      CustomCssDialogComponent,
      CustomMenuItemDialogComponent,
      CustomMenuDefaultItemDialogComponent,
      SelfRegistrationComponent,
      SelfRegistrationPermissionsComponent,
      SelfRegistrationPermissionDialogComponent,
      CustomMenuComponent,
      CustomMenuListComponent,
      CustomMenuInfoDialogComponent,
      CustomTranslationListComponent,
      CustomTranslationComponent,
      CustomTranslationAddDialogComponent,
      SecretsComponent,
      SecretDialogComponent,
      SecretChangeValueDialogComponent,
      SecretEditDescriptionDialogComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AdminRoutingModule,
    OAuth2Module,
    NgxFlowModule
  ]
})
export class AdminModule { }
