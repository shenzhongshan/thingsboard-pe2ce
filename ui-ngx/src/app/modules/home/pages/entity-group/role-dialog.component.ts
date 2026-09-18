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
import { forkJoin, of } from 'rxjs';
import { AppState } from '@core/core.state';
import { RoleService } from '@core/http/role.service';
import { PermissionService } from '@core/http/permission.service';
import {
  AllowedPermissionsInfo,
  Role,
  roleOperationTranslation,
  roleResourceTranslation,
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
  roleResourceTranslation = roleResourceTranslation;
  roleOperationTranslation = roleOperationTranslation;

  /** Resources this authority may be granted, from the server. */
  allowedResources: string[] = [];
  /** Operations a GROUP role may grant — a strict subset of all operations. */
  groupRoleOperations: string[] = [];

  private permissions: AllowedPermissionsInfo;
  private role: Role;

  constructor(@Inject(MAT_DIALOG_DATA) public data: RoleDialogData,
              private dialogRef: MatDialogRef<RoleDialogComponent, boolean>,
              protected store: Store<AppState>,
              private roleService: RoleService,
              private permissionService: PermissionService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.roleId;
  }

  ngOnInit() {
    forkJoin({
      permissions: this.permissionService.getAllowedPermissions(),
      role: this.isEdit ? this.roleService.getRole(this.data.roleId) : of<Role>(
        {name: '', type: RoleType.GENERIC, permissions: {}, additionalInfo: {}} as Role)
    }).subscribe(({permissions, role}) => {
      this.permissions = permissions;
      this.allowedResources = (permissions.allowedResources || []).slice().sort();
      this.groupRoleOperations = (permissions.allowedForGroupRoleOperations || []).slice().sort();
      this.role = role;
      this.buildForm(role);
      this.loaded = true;
    });
  }

  /**
   * Options for a GENERIC row: what the server allows for that resource, plus anything already
   * persisted on the role. The union matters — roles saved by the previous editor may hold
   * operations this resource never accepted, and hiding them would drop them silently on the next
   * save instead of letting the user see and remove them.
   */
  operationsFor(resource: string, selected?: string[]): string[] {
    const allowed = (resource && this.permissions?.operationsByResource?.[resource]) || [];
    return this.mergeSorted(allowed, selected);
  }

  /** Same union rule as above, for the GROUP role's single operation list. */
  groupOperationsFor(selected?: string[]): string[] {
    return this.mergeSorted(this.groupRoleOperations, selected);
  }

  /** Resource options, plus any persisted resource the server no longer offers. */
  resourceOptions(selected?: string): string[] {
    return this.mergeSorted(this.allowedResources, selected ? [selected] : []);
  }

  private mergeSorted(base: string[], extra?: string[]): string[] {
    const set = new Set(base);
    (extra || []).forEach((v) => {
      if (v) {
        set.add(v);
      }
    });
    return Array.from(set).sort();
  }

  /**
   * Changing the resource invalidates operations that the new resource does not accept. Dropping
   * them here (rather than at save time) keeps what is on screen equal to what will be persisted.
   */
  onResourceChanged(row: UntypedFormGroup) {
    const resource = row.get('resource')?.value;
    const allowed = new Set(this.operationsFor(resource));
    const current: string[] = row.get('operations')?.value || [];
    const kept = current.filter((op) => allowed.has(op));
    if (kept.length !== current.length) {
      row.get('operations').setValue(kept);
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
