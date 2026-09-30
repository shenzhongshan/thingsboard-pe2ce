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
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RoleService } from '@core/http/role.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, entityGroupTypes } from '@shared/models/entity-group.models';
import { GroupPermission, Role, RoleType } from '@shared/models/role.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { forkJoin } from 'rxjs';

/**
 * Picks a role (and, for GROUP roles, the entity group it applies to) for the self-registration
 * settings. Unlike {@code EntityGroupPermissionDialogComponent} this never persists anything:
 * the row only exists inside the self-registration params blob until someone actually signs up,
 * at which point SignUpController writes it against that customer's new user group.
 */
@Component({
  selector: 'tb-self-registration-permission-dialog',
  templateUrl: './self-registration-permission-dialog.component.html',
  standalone: false
})
export class SelfRegistrationPermissionDialogComponent implements OnInit {

  form: UntypedFormGroup;
  roles: Role[] = [];
  groupsByType = new Map<EntityType, EntityGroup[]>();
  groupTypes = entityGroupTypes;
  entityTypeTranslations = entityTypeTranslations;
  RoleType = RoleType;

  constructor(private dialogRef: MatDialogRef<SelfRegistrationPermissionDialogComponent, GroupPermission>,
              private roleService: RoleService,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      roleId: [null, [Validators.required]],
      entityGroupType: [null],
      entityGroupId: [null]
    });
  }

  ngOnInit() {
    this.roleService.getRoles(new PageLink(1024, 0)).subscribe(page => this.roles = page.data || []);
    forkJoin(this.groupTypes.map(t => this.entityGroupService.getEntityGroupsByType(t, new PageLink(1024, 0))))
      .subscribe(pages => pages.forEach((p, i) => this.groupsByType.set(this.groupTypes[i], p.data || [])));
    this.form.get('roleId').valueChanges.subscribe(() => this.syncGroupValidators());
    this.form.get('entityGroupType').valueChanges.subscribe(() => {
      this.form.get('entityGroupId').setValue(null, {emitEvent: false});
    });
  }

  get selectedRole(): Role {
    return this.roles.find(r => r.id.id === this.form.get('roleId').value);
  }

  get isGroupRole(): boolean {
    return this.selectedRole?.type === RoleType.GROUP;
  }

  get availableGroups(): EntityGroup[] {
    return this.groupsByType.get(this.form.get('entityGroupType').value) || [];
  }

  /**
   * A GROUP role is meaningless without the group it applies to, so both controls become required
   * only while such a role is selected. Clearing them on the way out matters: a stale `required`
   * on a hidden control leaves the form permanently invalid with nothing visible to fix.
   */
  private syncGroupValidators() {
    const typeCtrl = this.form.get('entityGroupType');
    const idCtrl = this.form.get('entityGroupId');
    if (this.isGroupRole) {
      typeCtrl.addValidators(Validators.required);
      idCtrl.addValidators(Validators.required);
    } else {
      typeCtrl.clearValidators();
      idCtrl.clearValidators();
      typeCtrl.setValue(null, {emitEvent: false});
      idCtrl.setValue(null, {emitEvent: false});
    }
    typeCtrl.updateValueAndValidity({emitEvent: false});
    idCtrl.updateValueAndValidity({emitEvent: false});
  }

  add(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const permission = {
      roleId: {entityType: EntityType.ROLE, id: v.roleId}
    } as GroupPermission;
    if (this.isGroupRole) {
      permission.entityGroupId = {entityType: EntityType.ENTITY_GROUP, id: v.entityGroupId};
      permission.entityGroupType = v.entityGroupType;
    }
    this.dialogRef.close(permission);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

}
