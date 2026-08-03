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
import { RoleService } from '@core/http/role.service';
import { EntityGroup } from '@shared/models/entity-group.models';
import { PageLink } from '@shared/models/page/page-link';
import { mergeMap } from 'rxjs/operators';
import { of } from 'rxjs';

const idOf = (value: any): string => value?.id || value;

export interface EntityGroupDialogData {
  entityGroupId?: string;
  groupType: EntityType;
  // customer id when creating a customer-owned group (hierarchy view)
  ownerId?: string;
}

@Component({
  selector: 'tb-entity-group-dialog',
  templateUrl: './entity-group-dialog.component.html',
  standalone: false
})
export class EntityGroupDialogComponent implements OnInit {

  @ViewChild('addEntityGroupStepper', {static: false}) stepper: MatStepper;

  entityType = EntityType;
  form: UntypedFormGroup;
  shareForm: UntypedFormGroup;
  isEdit = false;
  loaded = false;
  availableUserGroups: EntityGroup[] = [];

  private entityGroup: EntityGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EntityGroupDialogData,
              private dialogRef: MatDialogRef<EntityGroupDialogComponent, boolean>,
              protected store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              private roleService: RoleService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.entityGroupId;
  }

  ngOnInit() {
    this.shareForm = this.fb.group({
      enabled: [true],
      customerId: [null],
      allUsers: [true],
      userGroupId: [null],
      permission: ['READ', [Validators.required]]
    });
    this.shareForm.get('customerId').valueChanges.subscribe((customer) => {
      this.availableUserGroups = [];
      this.shareForm.get('userGroupId').setValue(null);
      const customerId = idOf(customer);
      if (customerId) {
        this.entityGroupService.getEntityGroupsByType(EntityType.USER, new PageLink(100, 0), customerId)
          .subscribe(page => this.availableUserGroups = page.data);
      }
    });
    this.shareForm.get('enabled').valueChanges.subscribe(() => this.updateShareValidators());
    this.shareForm.get('allUsers').valueChanges.subscribe(() => this.updateShareValidators());
    this.updateShareValidators();
    if (this.isEdit) {
      this.entityGroupService.getEntityGroup(this.data.entityGroupId).subscribe((group) => {
        this.entityGroup = group;
        this.buildForm(group);
        this.loaded = true;
      });
    } else {
      this.entityGroup = {type: this.data.groupType, name: '', additionalInfo: {}};
      if (this.data.ownerId) {
        this.entityGroup.ownerId = {entityType: 'CUSTOMER', id: this.data.ownerId};
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
    const allUsers = this.shareForm.get('allUsers').value;
    const customerIdCtrl = this.shareForm.get('customerId');
    const userGroupIdCtrl = this.shareForm.get('userGroupId');
    customerIdCtrl.setValidators(enabled ? [Validators.required] : []);
    userGroupIdCtrl.setValidators(enabled && !allUsers ? [Validators.required] : []);
    customerIdCtrl.updateValueAndValidity({emitEvent: false});
    userGroupIdCtrl.updateValueAndValidity({emitEvent: false});
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
        const customerId = idOf(shareValue.customerId);
        const userGroupId$ = shareValue.allUsers
          ? this.entityGroupService.getAllGroupId(EntityType.USER, customerId)
          : of(idOf(shareValue.userGroupId));
        return userGroupId$.pipe(
          mergeMap(userGroupId => this.roleService.getRoles(new PageLink(1024, 0)).pipe(
            mergeMap(page => {
              const roleName = shareValue.permission === 'READ' ? 'Read Only' : 'Customer Administrator';
              const role = page.data.find(r => r.name === roleName) || page.data[0];
              if (!role) {
                return of(savedGroup);
              }
              const permission: any = {
                userGroupId: {entityType: 'ENTITY_GROUP', id: userGroupId},
                roleId: {entityType: 'ROLE', id: role.id.id},
                entityGroupId: savedGroup.id,
                entityGroupType: savedGroup.type
              };
              return this.roleService.saveGroupPermission(permission).pipe(mergeMap(() => of(savedGroup)));
            })
          ))
        );
      })
    ).subscribe(() => this.dialogRef.close(true));
  }

}
