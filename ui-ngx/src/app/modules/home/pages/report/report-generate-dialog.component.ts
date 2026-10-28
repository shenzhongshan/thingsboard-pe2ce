/**
 * 文件职责：用户报表生成弹窗。
 * 只复用平台报表模板查询和生成接口，外观与系统原生弹窗隔离。
 */
import { Component, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Inject } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ReportService } from '@core/http/report.service';
import { ReportTemplateService } from '@core/http/report-template.service';
import { PageLink } from '@shared/models/page/page-link';
import { ReportTemplateInfo } from '@shared/models/report.models';
import { getDefaultTimezone } from '@shared/models/time/time.models';

export interface ReportGenerateDialogData {
}

@Component({
  selector: 'tb-report-generate-dialog',
  templateUrl: './report-generate-dialog.component.html',
  styleUrls: ['./report-generate-dialog.component.scss'],
  standalone: false
})
export class ReportGenerateDialogComponent implements OnInit {

  form: UntypedFormGroup;
  templates: ReportTemplateInfo[] = [];
  loadingTemplates = false;
  generating = false;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ReportGenerateDialogData,
              private dialogRef: MatDialogRef<ReportGenerateDialogComponent, boolean>,
              private reportTemplateService: ReportTemplateService,
              private reportService: ReportService,
              private fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      reportTemplateId: [null, Validators.required],
      timezone: [getDefaultTimezone(), Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadingTemplates = true;
    this.reportTemplateService.getReportTemplateInfos(new PageLink(100)).subscribe({
      next: (page) => {
        this.templates = page.data;
        this.loadingTemplates = false;
      },
      error: () => {
        this.templates = [];
        this.loadingTemplates = false;
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  generate(): void {
    if (this.form.invalid || this.generating) {
      this.form.markAllAsTouched();
      return;
    }
    this.generating = true;
    const value = this.form.value;
    this.reportService.generateReport(value.reportTemplateId, value.timezone).subscribe({
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
