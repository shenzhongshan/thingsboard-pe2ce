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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ReportTemplateService } from '@core/http/report-template.service';
import { newReportTemplate, TbReportFormat, ReportTemplateType } from '@shared/models/report.models';

export interface ReportTemplateCreateDialogData {
}

@Component({
  selector: 'tb-report-template-create-dialog',
  templateUrl: './report-template-create-dialog.component.html',
  standalone: false
})
export class ReportTemplateCreateDialogComponent {

  TbReportFormat = TbReportFormat;
  ReportTemplateType = ReportTemplateType;

  form: UntypedFormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ReportTemplateCreateDialogData,
              private dialogRef: MatDialogRef<ReportTemplateCreateDialogComponent, string>,
              private reportTemplateService: ReportTemplateService,
              private fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      format: [TbReportFormat.PDF],
      type: [ReportTemplateType.REPORT],
      description: ['']
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const template = {
      ...newReportTemplate(),
      name: v.name,
      format: v.format,
      type: v.type,
      description: v.description
    };
    this.reportTemplateService.saveReportTemplate(template).subscribe((saved) => {
      this.dialogRef.close(saved.id.id);
    });
  }

}
