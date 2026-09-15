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
import { ReportingRoutingModule } from '@home/pages/reporting/reporting-routing.module';
import { FeaturesModule } from '@home/pages/features/features.module';
import { ReportTemplatesComponent } from '@home/pages/reporting/report-template/report-templates.component';
import {
  ReportTemplateCreateDialogComponent
} from '@home/pages/reporting/report-template/report-template-create-dialog.component';
import { ReportTemplateEditorComponent } from '@home/pages/reporting/report-template/report-template-editor.component';
import { ReportsComponent } from '@home/pages/reporting/report/reports.component';
import { GenerateReportDialogComponent } from '@home/pages/reporting/report/generate-report-dialog.component';
import { ReportScheduleComponent } from '@home/pages/reporting/schedule/report-schedule.component';
import { ReportCreateComponent } from '@home/pages/report/report-create.component';
import { ReportGenerateDialogComponent } from '@home/pages/report/report-generate-dialog.component';

@NgModule({
  declarations: [
    ReportTemplatesComponent,
    ReportTemplateCreateDialogComponent,
    ReportTemplateEditorComponent,
    ReportsComponent,
    GenerateReportDialogComponent,
    ReportScheduleComponent,
    ReportCreateComponent,
    ReportGenerateDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    FeaturesModule,
    ReportingRoutingModule
  ]
})
export class ReportingModule { }
