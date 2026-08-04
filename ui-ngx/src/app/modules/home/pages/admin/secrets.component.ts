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
import { SecretService } from '@core/http/secret.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { SecretInfo, secretTypeTranslations } from '@shared/models/secret.models';
import { SecretDialogComponent } from '@home/pages/admin/secret-dialog.component';
import { SecretChangeValueDialogComponent } from '@home/pages/admin/secret-change-value-dialog.component';
import { SecretEditDescriptionDialogComponent } from '@home/pages/admin/secret-edit-description-dialog.component';

@Component({
  selector: 'tb-secrets',
  templateUrl: './secrets.component.html',
  styleUrls: ['./secrets.component.scss'],
  standalone: false
})
export class SecretsComponent extends PageComponent implements OnInit {

  secrets: SecretInfo[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  secretTypeTranslations = secretTypeTranslations;

  constructor(protected store: Store<AppState>,
              private secretService: SecretService,
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
      { property: 'name', direction: Direction.ASC });
    this.secretService.getSecretInfos(pageLink).subscribe((pageData) => {
      this.secrets = pageData.data;
      this.totalElements = pageData.totalElements;
    });
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

  addSecret() {
    this.dialog.open<SecretDialogComponent, undefined, boolean>(
      SecretDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog']
      }).afterClosed().subscribe((added) => {
      if (added) {
        this.reload();
      }
    });
  }

  changeValue($event: Event, secret: SecretInfo) {
    $event?.stopPropagation();
    this.dialog.open<SecretChangeValueDialogComponent, {secret: SecretInfo}, boolean>(
      SecretChangeValueDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data: { secret }
      }).afterClosed().subscribe();
  }

  editDescription($event: Event, secret: SecretInfo) {
    $event?.stopPropagation();
    this.dialog.open<SecretEditDescriptionDialogComponent, {secret: SecretInfo}, boolean>(
      SecretEditDescriptionDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data: { secret }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  deleteSecret($event: Event, secret: SecretInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('secret.delete-secret-title', {secretName: secret.name}),
      this.translate.instant('secret.delete-secret-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.secretService.deleteSecret(secret.id.id).subscribe((result) => {
          if (result.success) {
            this.reload();
          } else {
            this.dialogService.alert(
              this.translate.instant('secret.delete-secret-title', {secretName: secret.name}),
              this.translate.instant('secret.delete-secret-in-use-text')
            ).subscribe();
          }
        });
      }
    });
  }

}
