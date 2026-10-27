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
import { EntityType } from '@shared/models/entity-type.models';
import { RoleService } from '@core/http/role.service';
import { DialogService } from '@core/services/dialog.service';
import { EntityGroup } from '@shared/models/entity-group.models';
import { GroupPermissionInfo } from '@shared/models/role.models';
import { RoleViewDialogComponent, RoleViewDialogData } from '@home/pages/entity-group/role-view-dialog.component';
import {
  GroupPermissionDialogComponent,
  GroupPermissionDialogData
} from '@home/pages/entity-group/group-permission-dialog.component';

/**
 * A USER group's "角色" tab: which roles this user group has been granted, and on which target
 * entity groups. PE parity: {@code tb-group-permissions} with {@code isUserGroup=true} — the
 * mirror image of {@link EntityGroupPermissionsTabComponent}, which answers "who can access this
 * group" for a non-USER group. The two are mutually exclusive per group type, never both shown.
 */
@Component({
    selector: 'tb-entity-group-roles-tab',
    templateUrl: './entity-group-roles-tab.component.html',
    standalone: false
})
export class EntityGroupRolesTabComponent implements OnChanges, AfterViewInit {

  @Input() entity: EntityGroup;

  dataSource = new MatTableDataSource<GroupPermissionInfo>([]);
  selection = new SelectionModel<GroupPermissionInfo>(true, []);

  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatPaginator) paginator: MatPaginator;

  constructor(private roleService: RoleService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog) {
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
    this.roleService.getUserGroupPermissions(this.entity.id.id).subscribe((infos) => {
      this.dataSource.data = infos;
      this.selection.clear();
    });
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
    this.openPermissionDialog();
  }

  editPermission($event: Event, info: GroupPermissionInfo) {
    $event?.stopPropagation();
    this.openPermissionDialog(info);
  }

  private openPermissionDialog(permission?: GroupPermissionInfo) {
    this.dialog.open<GroupPermissionDialogComponent, GroupPermissionDialogData, boolean>(
      GroupPermissionDialogComponent, {
        disableClose: true,
        data: {
          userGroupId: this.entity.id,
          defaultOwnerId: this.entity.ownerId,
          permission
        }
      }).afterClosed().subscribe((saved) => {
      if (saved) {
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
