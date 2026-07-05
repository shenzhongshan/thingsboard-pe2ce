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
import { RoleService } from '@core/http/role.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Role, roleTypeTranslations } from '@shared/models/role.models';
import { RoleDialogComponent, RoleDialogData } from '@home/pages/entity-group/role-dialog.component';

@Component({
  selector: 'tb-roles',
  templateUrl: './roles.component.html',
  styleUrls: ['./entity-groups.component.scss'],
  standalone: false
})
export class RolesComponent extends PageComponent implements OnInit {

  roles: Role[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  roleTypeTranslations = roleTypeTranslations;

  constructor(protected store: Store<AppState>,
              private roleService: RoleService,
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
    this.roleService.getRoles(pageLink).subscribe((pageData) => {
      this.roles = pageData.data;
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

  addRole() {
    this.openDialog(null);
  }

  editRole($event: Event, role: Role) {
    $event?.stopPropagation();
    this.openDialog(role.id.id);
  }

  private openDialog(roleId: string) {
    this.dialog.open<RoleDialogComponent, RoleDialogData, boolean>(
      RoleDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { roleId }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  deleteRole($event: Event, role: Role) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('role.delete-role-title', {roleName: role.name}),
      this.translate.instant('role.delete-role-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.roleService.deleteRole(role.id.id).subscribe(() => this.reload());
      }
    });
  }

}
