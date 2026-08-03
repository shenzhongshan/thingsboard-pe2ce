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
import { RoleService } from '@core/http/role.service';
import { EntityGroup } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { PageLink } from '@shared/models/page/page-link';
import { mergeMap } from 'rxjs/operators';
import { of } from 'rxjs';

export interface EntityGroupShareDialogData {
  entityGroup: EntityGroup;
}

// PE's 共享实体组 grants a customer's users access to this entity group. CE has no dedicated
// sharing/ACL table for this — reuses the same GroupPermission+Role mechanism the 权限 tab is built
// on, targeting the customer's "All" user group (auto-seeded per customer, see
// project_whitelabeling_ce_port.md's Entity Groups / customer-hierarchy notes) with a role picked to
// approximate the 读取/写入/其他 permission level. Simplification: 写入/其他 both map to the
// "Customer Administrator" default role (broader access) since CE doesn't have a distinct write-only
// default role the way PE's granular permission model does.
@Component({
    selector: 'tb-entity-group-share-dialog',
    templateUrl: './entity-group-share-dialog.component.html',
    standalone: false
})
export class EntityGroupShareDialogComponent {

  entityType = EntityType;
  form: UntypedFormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EntityGroupShareDialogData,
              private dialogRef: MatDialogRef<EntityGroupShareDialogComponent, boolean>,
              private entityGroupService: EntityGroupService,
              private roleService: RoleService,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      customerId: [null, [Validators.required]],
      allUsers: [true],
      permission: ['READ', [Validators.required]]
    });
  }

  share(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const customerId = v.customerId?.id || v.customerId;
    this.entityGroupService.getAllGroupId(EntityType.USER, customerId).pipe(
      mergeMap(userGroupId => this.roleService.getRoles(new PageLink(1024, 0)).pipe(
        mergeMap(page => {
          const roleName = v.permission === 'READ' ? 'Read Only' : 'Customer Administrator';
          const role = page.data.find(r => r.name === roleName) || page.data[0];
          if (!role) {
            return of(null);
          }
          const permission: any = {
            userGroupId: {entityType: 'ENTITY_GROUP', id: userGroupId},
            roleId: {entityType: 'ROLE', id: role.id.id},
            entityGroupId: this.data.entityGroup.id,
            entityGroupType: this.data.entityGroup.type
          };
          return this.roleService.saveGroupPermission(permission);
        })
      ))
    ).subscribe(() => this.dialogRef.close(true));
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
