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
import { SolutionTemplatesRoutingModule } from '@home/pages/solution-templates/solution-templates-routing.module';
import { SolutionTemplatesComponent } from '@home/pages/solution-templates/solution-templates.component';
import { SolutionTemplateCardComponent } from '@home/pages/solution-templates/solution-template-card.component';
import {
  SolutionTemplateDetailsComponent
} from '@home/pages/solution-templates/solution-template-details.component';
import {
  SolutionInstallDialogComponent
} from '@home/pages/solution-templates/solution-install-dialog.component';

@NgModule({
  declarations: [
    SolutionTemplatesComponent,
    SolutionTemplateCardComponent,
    SolutionTemplateDetailsComponent,
    SolutionInstallDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SolutionTemplatesRoutingModule
  ]
})
export class SolutionTemplatesModule { }
