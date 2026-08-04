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
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { RoleService } from '@core/http/role.service';
import {
  Role,
  roleOperations,
  roleOperationTranslations,
  roleResources,
  roleResourceTranslations,
  RoleType
} from '@shared/models/role.models';

export interface RoleDialogData {
  roleId?: string;
}

@Component({
  selector: 'tb-role-dialog',
  templateUrl: './role-dialog.component.html',
  standalone: false
})
export class RoleDialogComponent implements OnInit {

  form: UntypedFormGroup;
  isEdit = false;
  loaded = false;

  RoleType = RoleType;
  roleResources = roleResources;
  roleOperations = roleOperations;
  roleResourceTranslations = roleResourceTranslations;
  roleOperationTranslations = roleOperationTranslations;

  private role: Role;

  constructor(@Inject(MAT_DIALOG_DATA) public data: RoleDialogData,
              private dialogRef: MatDialogRef<RoleDialogComponent, boolean>,
              protected store: Store<AppState>,
              private roleService: RoleService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.roleId;
  }

  ngOnInit() {
    if (this.isEdit) {
      this.roleService.getRole(this.data.roleId).subscribe((role) => {
        this.role = role;
        this.buildForm(role);
        this.loaded = true;
      });
    } else {
      this.role = {name: '', type: RoleType.GENERIC, permissions: {}, additionalInfo: {}};
      this.buildForm(this.role);
      this.loaded = true;
    }
  }

  private buildForm(role: Role) {
    const genericRows: {resource: string; operations: string[]}[] = [];
    let groupOperations: string[] = [];
    if (role.type === RoleType.GENERIC && role.permissions && !Array.isArray(role.permissions)) {
      for (const [resource, operations] of Object.entries(role.permissions)) {
        genericRows.push({resource, operations: operations as string[]});
      }
    } else if (role.type === RoleType.GROUP && Array.isArray(role.permissions)) {
      groupOperations = role.permissions as string[];
    }
    this.form = this.fb.group({
      name: [role.name, [Validators.required]],
      type: [{value: role.type, disabled: this.isEdit}],
      description: [role.additionalInfo?.description || ''],
      genericPermissions: this.fb.array(genericRows.map((row) => this.buildPermissionRow(row.resource, row.operations))),
      groupOperations: [groupOperations]
    });
  }

  private buildPermissionRow(resource: string, operations: string[]): UntypedFormGroup {
    return this.fb.group({
      resource: [resource, [Validators.required]],
      operations: [operations, [Validators.required]]
    });
  }

  get genericPermissionsArray(): UntypedFormArray {
    return this.form.get('genericPermissions') as UntypedFormArray;
  }

  addPermissionRow() {
    this.genericPermissionsArray.push(this.buildPermissionRow(null, []));
  }

  removePermissionRow(i: number) {
    this.genericPermissionsArray.removeAt(i);
  }

  get roleType(): RoleType {
    return this.form?.get('type')?.value;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    let permissions: any;
    if (v.type === RoleType.GENERIC) {
      permissions = {};
      for (const row of v.genericPermissions) {
        if (row.resource) {
          permissions[row.resource] = row.operations;
        }
      }
    } else {
      permissions = v.groupOperations || [];
    }
    const role: Role = {
      ...this.role,
      name: v.name,
      type: v.type,
      permissions,
      additionalInfo: { ...(this.role.additionalInfo || {}), description: v.description }
    };
    this.roleService.saveRole(role).subscribe(() => this.dialogRef.close(true));
  }

}
