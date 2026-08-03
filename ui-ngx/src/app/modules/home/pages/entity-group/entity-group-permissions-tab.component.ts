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

import { Component, Input, OnChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { RoleService } from '@core/http/role.service';
import { EntityGroup } from '@shared/models/entity-group.models';
import { GroupPermissionInfo } from '@shared/models/role.models';
import {
  EntityGroupPermissionDialogComponent,
  EntityGroupPermissionDialogData
} from '@home/pages/entity-group/entity-group-permission-dialog.component';

@Component({
    selector: 'tb-entity-group-permissions-tab',
    templateUrl: './entity-group-permissions-tab.component.html',
    standalone: false
})
export class EntityGroupPermissionsTabComponent implements OnChanges {

  @Input() entity: EntityGroup;

  permissions: GroupPermissionInfo[] = [];
  filteredPermissions: GroupPermissionInfo[] = [];
  searchMode = false;
  textSearch = '';

  constructor(private roleService: RoleService,
              private dialog: MatDialog) {
  }

  ngOnChanges() {
    if (this.entity?.id?.id) {
      this.reload();
    }
  }

  reload() {
    this.roleService.getEntityGroupPermissions(this.entity.id.id).subscribe(infos => {
      this.permissions = infos;
      this.applyFilter();
    });
  }

  enterSearchMode() {
    this.searchMode = true;
  }

  exitSearchMode() {
    this.searchMode = false;
    this.textSearch = '';
    this.applyFilter();
  }

  searchChanged() {
    this.applyFilter();
  }

  private applyFilter() {
    const search = this.textSearch?.toLowerCase();
    this.filteredPermissions = !search ? this.permissions :
      this.permissions.filter(p => p.roleName?.toLowerCase().includes(search) || p.userGroupName?.toLowerCase().includes(search));
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

  deletePermission(info: GroupPermissionInfo) {
    this.roleService.deleteGroupPermission((info as any).id.id).subscribe(() => this.reload());
  }

}
