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
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { IntegrationService } from '@core/http/integration.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Integration, IntegrationStatus } from '@shared/models/integration.models';
import {
  IntegrationDialogComponent,
  IntegrationDialogData
} from '@home/pages/integration/integration/integration-dialog.component';

@Component({
  selector: 'tb-integrations',
  templateUrl: './integrations.component.html',
  styleUrls: ['./integrations.component.scss'],
  standalone: false
})
export class IntegrationsComponent extends PageComponent implements OnInit {

  integrations: Integration[] = [];
  statuses = new Map<string, IntegrationStatus>();
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  constructor(protected store: Store<AppState>,
              private integrationService: IntegrationService,
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
    this.integrationService.getIntegrations(pageLink).subscribe((pageData) => {
      this.integrations = pageData.data;
      this.totalElements = pageData.totalElements;
      this.loadStatuses();
    });
  }

  private loadStatuses() {
    this.statuses.clear();
    if (!this.integrations.length) {
      return;
    }
    forkJoin(this.integrations.map((i) =>
      this.integrationService.getIntegrationStatus(i.id.id, {ignoreErrors: true, ignoreLoading: true})
        .pipe(catchError(() => of({status: 'STOPPED'} as IntegrationStatus))))
    ).subscribe((results) => {
      this.integrations.forEach((i, idx) => this.statuses.set(i.id.id, results[idx]));
    });
  }

  statusOf(integration: Integration): IntegrationStatus {
    return this.statuses.get(integration.id.id) || {status: 'STOPPED'};
  }

  pageChanged(event: {pageIndex: number; pageSize: number}) {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload();
  }

  enterSearchMode() { this.searchMode = true; }

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

  addIntegration() {
    this.openDialog(null);
  }

  editIntegration($event: Event, integration: Integration) {
    $event?.stopPropagation();
    this.openDialog(integration.id.id);
  }

  private openDialog(integrationId: string) {
    this.dialog.open<IntegrationDialogComponent, IntegrationDialogData, boolean>(
      IntegrationDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { integrationId }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  deleteIntegration($event: Event, integration: Integration) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('integration.delete-integration-title', {integrationName: integration.name}),
      this.translate.instant('integration.delete-integration-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.integrationService.deleteIntegration(integration.id.id).subscribe(() => this.reload());
      }
    });
  }

}
