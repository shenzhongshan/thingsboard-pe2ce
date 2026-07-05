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
import { SharedModule } from '@shared/shared.module';
import { IntegrationCenterRoutingModule } from '@home/pages/integration/integration-center-routing.module';
import { IntegrationsComponent } from '@home/pages/integration/integration/integrations.component';
import { IntegrationDialogComponent } from '@home/pages/integration/integration/integration-dialog.component';
import { ConvertersComponent } from '@home/pages/integration/converter/converters.component';
import { ConverterDialogComponent } from '@home/pages/integration/converter/converter-dialog.component';

@NgModule({
  declarations: [
    IntegrationsComponent,
    IntegrationDialogComponent,
    ConvertersComponent,
    ConverterDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    IntegrationCenterRoutingModule
  ]
})
export class IntegrationCenterModule { }
