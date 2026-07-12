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
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ReportService } from '@core/http/report.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { ReportInfo } from '@shared/models/report.models';
import {
  GenerateReportDialogComponent,
  GenerateReportDialogData
} from '@home/pages/reporting/report/generate-report-dialog.component';

@Component({
  selector: 'tb-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss'],
  standalone: false
})
export class ReportsComponent extends PageComponent implements OnInit {

  reports: ReportInfo[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  constructor(protected store: Store<AppState>,
              private reportService: ReportService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit() {
    this.reload();
  }

  reload() {
    const pageLink = new PageLink(this.pageSize, this.page, this.textSearch,
      { property: 'createdTime', direction: Direction.DESC });
    this.reportService.getReportInfos(pageLink).subscribe((pageData) => {
      this.reports = pageData.data;
      this.totalElements = pageData.totalElements;
    });
  }

  pageChanged(event: {pageIndex: number; pageSize: number}) {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload();
  }

  enterSearchMode() {
    this.searchMode = true;
  }

  exitSearchMode() {
    this.searchMode = false;
    if (this.textSearch) {
      this.textSearch = null;
      this.reload();
    }
  }

  searchChanged() {
    this.page = 0;
    this.reload();
  }

  generateReport() {
    this.dialog.open<GenerateReportDialogComponent, GenerateReportDialogData, boolean>(
      GenerateReportDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data: {}
      }).afterClosed().subscribe((generated) => {
      if (generated) {
        this.reload();
      }
    });
  }

  downloadReport($event: Event, report: ReportInfo) {
    $event?.stopPropagation();
    this.reportService.downloadReport(report.id.id, report.name).subscribe();
  }

  deleteReport($event: Event, report: ReportInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('report.delete-report-title', {reportName: report.name}),
      this.translate.instant('report.delete-report-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.reportService.deleteReport(report.id.id).subscribe(() => this.reload());
      }
    });
  }

}
