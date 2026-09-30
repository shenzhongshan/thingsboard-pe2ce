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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, ShareGroupRequest } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { RoleType } from '@shared/models/role.models';
import { EntityId } from '@shared/models/id/entity-id';

export interface EntityGroupShareDialogData {
  entityGroup: EntityGroup;
}

// PE parity: mirrors tb-share-entity-group + the wrapping dialog recovered from the running PE
// instance's bundle (chunk-IYCLUE5W.js). permission: 0 = READ, 1 = WRITE, 2 = OTHER (explicit roles).
@Component({
    selector: 'tb-entity-group-share-dialog',
    templateUrl: './entity-group-share-dialog.component.html',
    standalone: false
})
export class EntityGroupShareDialogComponent {

  entityType = EntityType;
  roleType = RoleType;
  form: UntypedFormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EntityGroupShareDialogData,
              private dialogRef: MatDialogRef<EntityGroupShareDialogComponent, boolean>,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      ownerId: [null, [Validators.required]],
      allUserGroup: [false],
      userGroupId: [null],
      permission: [0, [Validators.required]],
      roleIds: [null]
    });
    this.form.get('allUserGroup').valueChanges.subscribe(() => this.updateValidators());
    this.form.get('permission').valueChanges.subscribe(() => this.updateValidators());
    this.updateValidators();
  }

  private updateValidators(): void {
    const allUserGroup = this.form.get('allUserGroup').value;
    this.form.get('userGroupId').setValidators(allUserGroup ? [] : [Validators.required]);
    this.form.get('userGroupId').updateValueAndValidity({emitEvent: false});
    const other = this.form.get('permission').value === 2;
    this.form.get('roleIds').setValidators(other ? [Validators.required] : []);
    this.form.get('roleIds').updateValueAndValidity({emitEvent: false});
  }

  share(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const roleIds: string[] = v.roleIds;
    const shareGroupRequest: ShareGroupRequest = {
      ownerId: v.ownerId as EntityId,
      allUserGroup: v.allUserGroup,
      userGroupId: v.allUserGroup ? null : v.userGroupId as EntityId,
      readElseWrite: v.permission === 0,
      roleIds: v.permission === 2 && roleIds?.length ?
        roleIds.map(id => ({entityType: EntityType.ROLE, id} as EntityId)) : null
    };
    this.entityGroupService.shareEntityGroup(this.data.entityGroup.id.id, shareGroupRequest)
      .subscribe(() => this.dialogRef.close(true));
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
