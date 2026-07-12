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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ReportTemplateService } from '@core/http/report-template.service';
import { ReportService } from '@core/http/report.service';
import { PageLink } from '@shared/models/page/page-link';
import { ReportTemplateInfo } from '@shared/models/report.models';
import { getDefaultTimezone } from '@shared/models/time/time.models';

export interface GenerateReportDialogData {
}

@Component({
  selector: 'tb-generate-report-dialog',
  templateUrl: './generate-report-dialog.component.html',
  standalone: false
})
export class GenerateReportDialogComponent implements OnInit {

  form: UntypedFormGroup;
  templates: ReportTemplateInfo[] = [];
  generating = false;

  constructor(@Inject(MAT_DIALOG_DATA) public data: GenerateReportDialogData,
              private dialogRef: MatDialogRef<GenerateReportDialogComponent, boolean>,
              private reportTemplateService: ReportTemplateService,
              private reportService: ReportService,
              private fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      reportTemplateId: [null, [Validators.required]],
      timezone: [getDefaultTimezone(), [Validators.required]]
    });
  }

  ngOnInit() {
    this.reportTemplateService.getReportTemplateInfos(new PageLink(100)).subscribe((page) => {
      this.templates = page.data;
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  generate(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    this.generating = true;
    this.reportService.generateReport(v.reportTemplateId, v.timezone).subscribe({
      next: () => {
        this.generating = false;
        this.dialogRef.close(true);
      },
      error: () => {
        this.generating = false;
      }
    });
  }

}
