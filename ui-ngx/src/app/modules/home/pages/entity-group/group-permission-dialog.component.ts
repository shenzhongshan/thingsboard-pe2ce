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
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { RoleService } from '@core/http/role.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { entityGroupTypes } from '@shared/models/entity-group.models';
import { GroupPermission, GroupPermissionInfo, Role, RoleType, roleTypeTranslations } from '@shared/models/role.models';

export interface GroupPermissionDialogData {
  /** The USER group these permissions belong to — bound as userGroupId on save. */
  userGroupId: EntityId;
  /** Owner to default the target-group picker to (the user group's own owner). */
  defaultOwnerId: EntityId;
  /** Present when editing an existing row; absent when adding. */
  permission?: GroupPermissionInfo;
}

/**
 * Add/edit one group permission for a USER group. PE parity: role type is its own first dropdown
 * and filters the role list; only a GROUP-type role reveals the 实体组 section (owner → type →
 * group), because a GENERIC role grants tenant/customer-wide access with no target group at all.
 */
@Component({
    selector: 'tb-group-permission-dialog',
    templateUrl: './group-permission-dialog.component.html',
    standalone: false
})
export class GroupPermissionDialogComponent implements OnInit {

  form: UntypedFormGroup;
  isEdit: boolean;

  roleTypes = Object.values(RoleType);
  roleTypeTranslations = roleTypeTranslations;
  entityGroupTypes = entityGroupTypes;
  RoleType = RoleType;

  rolesOfType: Role[] = [];

  private allRoles: Role[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: GroupPermissionDialogData,
              private dialogRef: MatDialogRef<GroupPermissionDialogComponent, boolean>,
              private roleService: RoleService,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.permission;
  }

  ngOnInit() {
    const p = this.data.permission;
    this.form = this.fb.group({
      roleType: [null, [Validators.required]],
      roleId: [null, [Validators.required]],
      entityGroupOwnerId: [this.data.defaultOwnerId],
      entityGroupType: [null],
      entityGroupId: [null]
    });
    this.roleService.getRoles(new PageLink(1024, 0)).subscribe((page) => {
      this.allRoles = page.data;
      // Seed the edit case only once the role list is in, so the role control has options to match.
      if (p) {
        const role = this.allRoles.find((r) => r.id.id === p.roleId?.id);
        this.form.patchValue({
          roleType: role?.type ?? null,
          entityGroupOwnerId: this.data.defaultOwnerId,
          entityGroupType: p.entityGroupType ?? null,
          entityGroupId: p.entityGroupId?.id ?? null
        }, {emitEvent: false});
        this.applyRoleType(role?.type ?? null);
        this.form.get('roleId').setValue(p.roleId?.id ?? null, {emitEvent: false});
      }
      this.filterRoles();
    });
    this.form.get('roleType').valueChanges.subscribe((type: RoleType) => {
      // Switching type invalidates the picked role (the lists are disjoint) and, when leaving
      // GROUP, the whole target-group selection.
      this.form.get('roleId').setValue(null, {emitEvent: false});
      this.applyRoleType(type);
      this.filterRoles();
    });
    this.form.get('entityGroupOwnerId').valueChanges.subscribe(() => {
      this.form.get('entityGroupId').setValue(null, {emitEvent: false});
    });
    this.form.get('entityGroupType').valueChanges.subscribe(() => {
      this.form.get('entityGroupId').setValue(null, {emitEvent: false});
    });
  }

  /** The 实体组 section is required for a GROUP role and meaningless for a GENERIC one. */
  private applyRoleType(type: RoleType | null) {
    const groupCtrls = ['entityGroupOwnerId', 'entityGroupType', 'entityGroupId'];
    if (type === RoleType.GROUP) {
      groupCtrls.forEach((c) => this.form.get(c).setValidators([Validators.required]));
    } else {
      groupCtrls.forEach((c) => {
        this.form.get(c).clearValidators();
        if (c !== 'entityGroupOwnerId') {
          this.form.get(c).setValue(null, {emitEvent: false});
        }
      });
    }
    groupCtrls.forEach((c) => this.form.get(c).updateValueAndValidity({emitEvent: false}));
  }

  private filterRoles() {
    const type = this.form.get('roleType').value;
    this.rolesOfType = type ? this.allRoles.filter((r) => r.type === type) : [];
  }

  get isGroupRole(): boolean {
    return this.form?.get('roleType')?.value === RoleType.GROUP;
  }

  /** The owner picker emits either a plain EntityId or an EntityInfoData — unwrap either shape. */
  ownerId(): EntityId | null {
    const owner = this.form?.get('entityGroupOwnerId')?.value as EntityInfoData | EntityId;
    if (!owner) {
      return null;
    }
    return (owner as EntityInfoData).name !== undefined ? (owner as EntityInfoData).id : owner as EntityId;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const permission: GroupPermission = {
      ...(this.data.permission || {} as GroupPermission),
      userGroupId: {entityType: EntityType.ENTITY_GROUP, id: this.data.userGroupId.id},
      roleId: {entityType: EntityType.ROLE, id: v.roleId}
    } as GroupPermission;
    if (v.roleType === RoleType.GROUP) {
      permission.entityGroupId = {entityType: EntityType.ENTITY_GROUP, id: v.entityGroupId};
      permission.entityGroupType = v.entityGroupType;
    } else {
      // A GENERIC role is not scoped to a group; clear any target left over from an edit.
      delete permission.entityGroupId;
      delete permission.entityGroupType;
    }
    this.roleService.saveGroupPermission(permission).subscribe(() => this.dialogRef.close(true));
  }

}
