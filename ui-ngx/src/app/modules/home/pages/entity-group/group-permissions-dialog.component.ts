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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { forkJoin } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { RoleService } from '@core/http/role.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { EntityGroup, entityGroupTypes } from '@shared/models/entity-group.models';
import { GroupPermissionInfo, Role, RoleType } from '@shared/models/role.models';

export interface GroupPermissionsDialogData {
  userGroup: EntityGroup;
}

@Component({
  selector: 'tb-group-permissions-dialog',
  templateUrl: './group-permissions-dialog.component.html',
  standalone: false
})
export class GroupPermissionsDialogComponent implements OnInit {

  form: UntypedFormGroup;
  permissions: GroupPermissionInfo[] = [];
  roles: Role[] = [];
  targetGroups: EntityGroup[] = [];

  RoleType = RoleType;
  entityGroupTypes = entityGroupTypes;

  constructor(@Inject(MAT_DIALOG_DATA) public data: GroupPermissionsDialogData,
              private dialogRef: MatDialogRef<GroupPermissionsDialogComponent>,
              protected store: Store<AppState>,
              private roleService: RoleService,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      roleId: [null, [Validators.required]],
      entityGroupType: [EntityType.DEVICE],
      entityGroupId: [null]
    });
  }

  ngOnInit() {
    this.roleService.getRoles(new PageLink(1024, 0)).subscribe((page) => {
      this.roles = page.data;
    });
    this.form.get('entityGroupType').valueChanges.subscribe(() => this.loadTargetGroups());
    this.form.get('roleId').valueChanges.subscribe(() => {
      if (this.isGroupRoleSelected) {
        this.loadTargetGroups();
      }
    });
    this.reload();
  }

  reload() {
    this.roleService.getUserGroupPermissions(this.data.userGroup.id.id).subscribe((infos) => {
      this.permissions = infos;
    });
  }

  get isGroupRoleSelected(): boolean {
    const roleId = this.form?.get('roleId')?.value;
    const role = this.roles.find((r) => r.id.id === roleId);
    return role?.type === RoleType.GROUP;
  }

  private loadTargetGroups() {
    const type = this.form.get('entityGroupType').value;
    this.entityGroupService.getEntityGroupsByType(type, new PageLink(1024, 0)).subscribe((page) => {
      this.targetGroups = page.data;
    });
  }

  addPermission() {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    if (this.isGroupRoleSelected && !v.entityGroupId) {
      this.form.get('entityGroupId').setErrors({required: true});
      this.form.get('entityGroupId').markAsTouched();
      return;
    }
    const permission: any = {
      userGroupId: this.data.userGroup.id,
      roleId: {entityType: 'ROLE', id: v.roleId}
    };
    if (this.isGroupRoleSelected) {
      permission.entityGroupId = {entityType: 'ENTITY_GROUP', id: v.entityGroupId};
      permission.entityGroupType = v.entityGroupType;
    }
    this.roleService.saveGroupPermission(permission).subscribe(() => {
      this.form.get('roleId').reset();
      this.form.get('entityGroupId').reset();
      this.reload();
    });
  }

  deletePermission(info: GroupPermissionInfo) {
    this.roleService.deleteGroupPermission((info as any).id.id).subscribe(() => this.reload());
  }

  close(): void {
    this.dialogRef.close();
  }

}
