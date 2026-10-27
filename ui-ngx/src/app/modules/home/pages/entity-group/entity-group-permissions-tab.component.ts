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

import { AfterViewInit, Component, Input, OnChanges, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSort } from '@angular/material/sort';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { SelectionModel } from '@angular/cdk/collections';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { RoleService } from '@core/http/role.service';
import { DialogService } from '@core/services/dialog.service';
import { EntityGroup } from '@shared/models/entity-group.models';
import { GroupPermissionInfo } from '@shared/models/role.models';
import {
  EntityGroupPermissionDialogComponent,
  EntityGroupPermissionDialogData
} from '@home/pages/entity-group/entity-group-permission-dialog.component';
import { RoleViewDialogComponent, RoleViewDialogData } from '@home/pages/entity-group/role-view-dialog.component';

@Component({
    selector: 'tb-entity-group-permissions-tab',
    templateUrl: './entity-group-permissions-tab.component.html',
    standalone: false
})
export class EntityGroupPermissionsTabComponent implements OnChanges, AfterViewInit {

  @Input() entity: EntityGroup;

  dataSource = new MatTableDataSource<GroupPermissionInfo>([]);
  selection = new SelectionModel<GroupPermissionInfo>(true, []);
  searchMode = false;
  textSearch = '';

  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatPaginator) paginator: MatPaginator;

  constructor(private roleService: RoleService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService) {
    this.dataSource.filterPredicate = (p, filter) =>
      !!(p.roleName?.toLowerCase().includes(filter) || p.userGroupName?.toLowerCase().includes(filter));
  }

  ngOnChanges() {
    if (this.entity?.id?.id) {
      this.reload();
    }
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  reload() {
    this.roleService.getEntityGroupPermissions(this.entity.id.id).subscribe(infos => {
      this.dataSource.data = infos;
      this.selection.clear();
    });
  }

  enterSearchMode() {
    this.searchMode = true;
  }

  exitSearchMode() {
    this.searchMode = false;
    this.textSearch = '';
    this.dataSource.filter = '';
  }

  searchChanged() {
    this.dataSource.filter = this.textSearch?.trim().toLowerCase() || '';
  }

  isAllSelected(): boolean {
    return this.dataSource.data.length > 0 && this.selection.selected.length === this.dataSource.data.length;
  }

  toggleAll() {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.selection.select(...this.dataSource.data);
    }
  }

  addPermission() {
    this.dialog.open<EntityGroupPermissionDialogComponent, EntityGroupPermissionDialogData, boolean>(
      EntityGroupPermissionDialogComponent, {
        disableClose: true,
        data: {entityGroup: this.entity}
      }).afterClosed().subscribe((added) => {
      if (added) {
        this.reload();
      }
    });
  }

  viewRole($event: Event, info: GroupPermissionInfo) {
    $event?.stopPropagation();
    this.dialog.open<RoleViewDialogComponent, RoleViewDialogData>(
      RoleViewDialogComponent, {disableClose: true, data: {roleId: info.roleId.id}});
  }

  deletePermission($event: Event, info: GroupPermissionInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('group-permission.delete-group-permission-title', {roleName: info.roleName}),
      this.translate.instant('group-permission.delete-group-permission-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.roleService.deleteGroupPermission((info as any).id.id).subscribe(() => this.reload());
      }
    });
  }

  deleteSelected($event: Event) {
    $event?.stopPropagation();
    if (!this.selection.selected.length) {
      return;
    }
    this.dialogService.confirm(
      this.translate.instant('group-permission.delete-group-permissions-title', {count: this.selection.selected.length}),
      this.translate.instant('group-permission.delete-group-permissions-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        forkJoin(this.selection.selected.map(p => this.roleService.deleteGroupPermission((p as any).id.id)))
          .subscribe(() => this.reload());
      }
    });
  }

}
