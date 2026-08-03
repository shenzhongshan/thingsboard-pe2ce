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
import { RoleService } from '@core/http/role.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup } from '@shared/models/entity-group.models';
import { Role } from '@shared/models/role.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityType } from '@shared/models/entity-type.models';

export interface EntityGroupPermissionDialogData {
  entityGroup: EntityGroup;
}

@Component({
    selector: 'tb-entity-group-permission-dialog',
    templateUrl: './entity-group-permission-dialog.component.html',
    standalone: false
})
export class EntityGroupPermissionDialogComponent implements OnInit {

  form: UntypedFormGroup;
  roles: Role[] = [];
  userGroups: EntityGroup[] = [];
  // Owner display of the granting user group — simplified to the target group's own owner scope
  // (tenant vs customer) rather than a full cross-owner picker.
  ownerLabel: string;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EntityGroupPermissionDialogData,
              private dialogRef: MatDialogRef<EntityGroupPermissionDialogComponent, boolean>,
              private roleService: RoleService,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      roleId: [null, [Validators.required]],
      userGroupId: [null, [Validators.required]]
    });
  }

  ngOnInit() {
    this.ownerLabel = this.data.entityGroup.ownerId ? 'entity-group.owner-customer' : 'entity-group.owner-tenant';
    this.roleService.getRoles(new PageLink(1024, 0)).subscribe(page => this.roles = page.data);
    this.entityGroupService.getEntityGroupsByType(EntityType.USER, new PageLink(1024, 0), this.data.entityGroup.ownerId?.id)
      .subscribe(page => this.userGroups = page.data);
  }

  add(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const permission: any = {
      userGroupId: {entityType: 'ENTITY_GROUP', id: v.userGroupId},
      roleId: {entityType: 'ROLE', id: v.roleId},
      entityGroupId: this.data.entityGroup.id,
      entityGroupType: this.data.entityGroup.type
    };
    this.roleService.saveGroupPermission(permission).subscribe(() => this.dialogRef.close(true));
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
