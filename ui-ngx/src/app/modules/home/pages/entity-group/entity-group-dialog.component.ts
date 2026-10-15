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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatStepper } from '@angular/material/stepper';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, ShareGroupRequest } from '@shared/models/entity-group.models';
import { RoleType } from '@shared/models/role.models';
import { EntityId } from '@shared/models/id/entity-id';
import { mergeMap } from 'rxjs/operators';
import { of } from 'rxjs';

export interface EntityGroupDialogData {
  entityGroupId?: string;
  groupType: EntityType;
  // customer id when creating a customer-owned group (hierarchy view)
  ownerId?: string;
  // pre-fills the name when the dialog is opened from a picker's "create a new one" action
  name?: string;
}

@Component({
  selector: 'tb-entity-group-dialog',
  templateUrl: './entity-group-dialog.component.html',
  standalone: false
})
export class EntityGroupDialogComponent implements OnInit {

  @ViewChild('addEntityGroupStepper', {static: false}) stepper: MatStepper;

  entityType = EntityType;
  roleType = RoleType;
  form: UntypedFormGroup;
  shareForm: UntypedFormGroup;
  isEdit = false;
  loaded = false;

  private entityGroup: EntityGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EntityGroupDialogData,
              // Closes with the saved group so a picker can select it right away; false on cancel.
              private dialogRef: MatDialogRef<EntityGroupDialogComponent, EntityGroup | false>,
              protected store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.entityGroupId;
  }

  ngOnInit() {
    this.shareForm = this.fb.group({
      enabled: [false],
      ownerId: [null],
      allUserGroup: [false],
      userGroupId: [null],
      permission: [0, [Validators.required]],
      roleIds: [null]
    });
    this.shareForm.get('enabled').valueChanges.subscribe(() => this.updateShareValidators());
    this.shareForm.get('allUserGroup').valueChanges.subscribe(() => this.updateShareValidators());
    this.shareForm.get('permission').valueChanges.subscribe(() => this.updateShareValidators());
    this.updateShareValidators();
    if (this.isEdit) {
      this.entityGroupService.getEntityGroup(this.data.entityGroupId).subscribe((group) => {
        this.entityGroup = group;
        this.buildForm(group);
        this.loaded = true;
      });
    } else {
      this.entityGroup = {type: this.data.groupType, name: this.data.name || '', additionalInfo: {}};
      if (this.data.ownerId) {
        this.entityGroup.ownerId = {entityType: EntityType.CUSTOMER, id: this.data.ownerId};
      }
      this.buildForm(this.entityGroup);
      this.loaded = true;
    }
  }

  private buildForm(group: EntityGroup) {
    this.form = this.fb.group({
      name: [group.name, [Validators.required]],
      description: [group.additionalInfo?.description || '']
    });
  }

  private updateShareValidators(): void {
    const enabled = this.shareForm.get('enabled').value;
    const allUserGroup = this.shareForm.get('allUserGroup').value;
    const other = this.shareForm.get('permission').value === 2;
    const ownerIdCtrl = this.shareForm.get('ownerId');
    const userGroupIdCtrl = this.shareForm.get('userGroupId');
    const roleIdsCtrl = this.shareForm.get('roleIds');
    ownerIdCtrl.setValidators(enabled ? [Validators.required] : []);
    userGroupIdCtrl.setValidators(enabled && !allUserGroup ? [Validators.required] : []);
    roleIdsCtrl.setValidators(enabled && other ? [Validators.required] : []);
    ownerIdCtrl.updateValueAndValidity({emitEvent: false});
    userGroupIdCtrl.updateValueAndValidity({emitEvent: false});
    roleIdsCtrl.updateValueAndValidity({emitEvent: false});
  }

  nextStep(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    this.stepper.next();
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      if (this.stepper) {
        this.stepper.selectedIndex = 0;
      }
      return;
    }
    if (this.shareForm.get('enabled').value && !this.shareForm.valid) {
      this.shareForm.markAllAsTouched();
      if (this.stepper) {
        this.stepper.selectedIndex = 1;
      }
      return;
    }
    const v = this.form.value;
    const group: EntityGroup = {
      ...this.entityGroup,
      name: v.name,
      additionalInfo: { ...(this.entityGroup.additionalInfo || {}), description: v.description }
    };
    this.entityGroupService.saveEntityGroup(group).pipe(
      mergeMap((savedGroup) => {
        const shareValue = this.shareForm.value;
        if (!shareValue.enabled) {
          return of(savedGroup);
        }
        const roleIds: string[] = shareValue.roleIds;
        const shareGroupRequest: ShareGroupRequest = {
          ownerId: shareValue.ownerId as EntityId,
          allUserGroup: shareValue.allUserGroup,
          userGroupId: shareValue.allUserGroup ? null : shareValue.userGroupId as EntityId,
          readElseWrite: shareValue.permission === 0,
          roleIds: shareValue.permission === 2 && roleIds?.length ?
            roleIds.map(id => ({entityType: EntityType.ROLE, id} as EntityId)) : null
        };
        return this.entityGroupService.shareEntityGroup(savedGroup.id.id, shareGroupRequest).pipe(
          mergeMap(() => of(savedGroup))
        );
      })
    ).subscribe(savedGroup => this.dialogRef.close(savedGroup));
  }

}
