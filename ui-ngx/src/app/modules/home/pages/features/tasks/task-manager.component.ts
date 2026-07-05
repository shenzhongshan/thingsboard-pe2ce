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
import { HttpClient } from '@angular/common/http';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';

export interface JobInfo {
  id: {id: string};
  createdTime: number;
  type: string;
  entityId?: {entityType: string; id: string};
  entityName?: string;
  status: string;
  result?: {
    successfulCount?: number;
    failedCount?: number;
    discardedCount?: number;
    totalCount?: number;
    generalError?: string;
  };
}

export const jobStatusTranslations = new Map<string, string>([
  ['QUEUED', 'task.status-queued'],
  ['PENDING', 'task.status-pending'],
  ['RUNNING', 'task.status-running'],
  ['COMPLETED', 'task.status-completed'],
  ['FAILED', 'task.status-failed'],
  ['CANCELLED', 'task.status-cancelled']
]);

@Component({
  selector: 'tb-task-manager',
  templateUrl: './task-manager.component.html',
  styleUrls: ['./task-manager.component.scss'],
  standalone: false
})
export class TaskManagerComponent extends PageComponent implements OnInit {

  jobs: JobInfo[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  statusFilter: string = null;

  statuses = ['QUEUED', 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'];
  jobStatusTranslations = jobStatusTranslations;

  constructor(protected store: Store<AppState>,
              private http: HttpClient,
              private dialogService: DialogService,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit() {
    this.reload();
  }

  reload() {
    const pageLink = new PageLink(this.pageSize, this.page, null,
      { property: 'createdTime', direction: Direction.DESC });
    let url = `/api/jobs${pageLink.toQuery()}`;
    if (this.statusFilter) {
      url += `&statuses=${this.statusFilter}`;
    }
    this.http.get<PageData<JobInfo>>(url).subscribe((pageData) => {
      this.jobs = pageData.data;
      this.totalElements = pageData.totalElements;
    });
  }

  pageChanged(event: {pageIndex: number; pageSize: number}) {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload();
  }

  statusFilterChanged() {
    this.page = 0;
    this.reload();
  }

  progressText(job: JobInfo): string {
    const result = job.result;
    if (!result || result.totalCount === null || result.totalCount === undefined) {
      return '';
    }
    const done = (result.successfulCount || 0) + (result.failedCount || 0) + (result.discardedCount || 0);
    return `${done} / ${result.totalCount}`;
  }

  canCancel(job: JobInfo): boolean {
    return ['QUEUED', 'PENDING', 'RUNNING'].includes(job.status);
  }

  canReprocess(job: JobInfo): boolean {
    return ['FAILED', 'CANCELLED'].includes(job.status);
  }

  cancelJob($event: Event, job: JobInfo) {
    $event?.stopPropagation();
    this.http.post(`/api/job/${job.id.id}/cancel`, null).subscribe(() => this.reload());
  }

  reprocessJob($event: Event, job: JobInfo) {
    $event?.stopPropagation();
    this.http.post(`/api/job/${job.id.id}/reprocess`, null).subscribe(() => this.reload());
  }

  deleteJob($event: Event, job: JobInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('task.delete-task-title'),
      this.translate.instant('task.delete-task-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.http.delete(`/api/job/${job.id.id}`).subscribe(() => this.reload());
      }
    });
  }

}
