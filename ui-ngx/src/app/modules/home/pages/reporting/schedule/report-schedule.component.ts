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
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { SchedulerEventType, SchedulerEventWithCustomerInfo } from '@shared/models/scheduler-event.models';
import {
  SchedulerEventDialogComponent,
  SchedulerEventDialogData
} from '@home/pages/features/scheduler/scheduler-event-dialog.component';

@Component({
  selector: 'tb-report-schedule',
  templateUrl: './report-schedule.component.html',
  styleUrls: ['./report-schedule.component.scss'],
  standalone: false
})
export class ReportScheduleComponent extends PageComponent implements OnInit {

  events: SchedulerEventWithCustomerInfo[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  constructor(protected store: Store<AppState>,
              private schedulerEventService: SchedulerEventService,
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
    this.schedulerEventService.getSchedulerEvents(pageLink, SchedulerEventType.GENERATE_REPORT).subscribe((pageData) => {
      this.events = pageData.data;
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

  addEvent() {
    this.openDialog(null);
  }

  editEvent(event: SchedulerEventWithCustomerInfo) {
    this.openDialog(event.id.id);
  }

  private openDialog(schedulerEventId: string) {
    this.dialog.open<SchedulerEventDialogComponent, SchedulerEventDialogData, boolean>(
      SchedulerEventDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data: { schedulerEventId }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  deleteEvent($event: Event, event: SchedulerEventWithCustomerInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('report.delete-schedule-title', {eventName: event.name}),
      this.translate.instant('report.delete-schedule-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.schedulerEventService.deleteSchedulerEvent(event.id.id).subscribe(() => this.reload());
      }
    });
  }

}
