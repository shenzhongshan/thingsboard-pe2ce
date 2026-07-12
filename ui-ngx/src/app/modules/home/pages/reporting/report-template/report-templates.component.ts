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
import { ReportTemplateService } from '@core/http/report-template.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { ReportTemplateInfo, ReportTemplateType } from '@shared/models/report.models';
import {
  ReportTemplateCreateDialogComponent,
  ReportTemplateCreateDialogData
} from '@home/pages/reporting/report-template/report-template-create-dialog.component';
import {
  ReportTemplateEditorComponent,
  ReportTemplateEditorData
} from '@home/pages/reporting/report-template/report-template-editor.component';

@Component({
  selector: 'tb-report-templates',
  templateUrl: './report-templates.component.html',
  styleUrls: ['./report-templates.component.scss'],
  standalone: false
})
export class ReportTemplatesComponent extends PageComponent implements OnInit {

  templates: ReportTemplateInfo[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  ReportTemplateType = ReportTemplateType;

  constructor(protected store: Store<AppState>,
              private reportTemplateService: ReportTemplateService,
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
    this.reportTemplateService.getReportTemplateInfos(pageLink).subscribe((pageData) => {
      this.templates = pageData.data;
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

  addTemplate() {
    this.dialog.open<ReportTemplateCreateDialogComponent, ReportTemplateCreateDialogData, string>(
      ReportTemplateCreateDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data: {}
      }).afterClosed().subscribe((createdTemplateId) => {
      if (createdTemplateId) {
        this.reload();
        this.openEditor(createdTemplateId);
      }
    });
  }

  openTemplate(template: ReportTemplateInfo) {
    this.openEditor(template.id.id);
  }

  private openEditor(reportTemplateId: string) {
    this.dialog.open<ReportTemplateEditorComponent, ReportTemplateEditorData, boolean>(
      ReportTemplateEditorComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { reportTemplateId }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  deleteTemplate($event: Event, template: ReportTemplateInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('report.delete-template-title', {templateName: template.name}),
      this.translate.instant('report.delete-template-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.reportTemplateService.deleteReportTemplate(template.id.id).subscribe(() => this.reload());
      }
    });
  }

}
