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

import { ChangeDetectorRef, Component, Inject, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EntityComponent } from '@home/components/entity/entity.component';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {
  Role,
  roleOperationTranslation,
  roleResourceTranslation,
  RoleType
} from '@shared/models/role.models';

const ALL_SENTINEL = 'ALL';

/**
 * PE parity: ROLE as a first-class entity (generic list/detail framework, deep-linkable) rather than
 * a bespoke paginated dialog. `allowedResources`/`groupRoleOperations`/`operationsByResource` come
 * from {@link RolesTableConfigResolver}'s own resolve() step — they must be known synchronously by
 * the time `buildForm` runs (the base class calls it from its own constructor), so the resolver
 * stashes them on `entitiesTableConfig.componentsData` rather than this component fetching them.
 */
@Component({
    selector: 'tb-role',
    templateUrl: './role.component.html',
    standalone: false
})
export class RoleComponent extends EntityComponent<Role> {

  RoleType = RoleType;
  roleResourceTranslation = roleResourceTranslation;
  roleOperationTranslation = roleOperationTranslation;

  constructor(protected store: Store<AppState>,
              @Optional() @Inject('entity') protected entityValue: Role,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Role>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private translate: TranslateService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  private get allowedResources(): string[] {
    return this.entitiesTableConfig?.componentsData?.allowedResources || [];
  }

  private get groupRoleOperations(): string[] {
    return this.entitiesTableConfig?.componentsData?.groupRoleOperations || [];
  }

  private get operationsByResource(): {[resource: string]: string[]} {
    return this.entitiesTableConfig?.componentsData?.operationsByResource || {};
  }

  get roleType(): RoleType {
    return this.entityForm?.get('type')?.value;
  }

  get genericPermissionsArray(): UntypedFormArray {
    return this.entityForm.get('genericPermissions') as UntypedFormArray;
  }

  /** Options for a GENERIC row: what the server allows for that resource, plus anything already
   * persisted on the role — the union matters so a role saved under a since-narrowed vocabulary
   * still displays (and can be edited down) rather than silently dropping on the next save. */
  operationsFor(resource: string, selected?: string[]): string[] {
    const allowed = (resource && this.operationsByResource[resource]) || [];
    return this.mergeSorted(allowed, selected);
  }

  groupOperationsFor(selected?: string[]): string[] {
    return this.mergeSorted(this.groupRoleOperations, selected);
  }

  resourceOptions(selected?: string): string[] {
    return this.mergeSorted(this.allowedResources, selected ? [selected] : []);
  }

  /** ALL always sorts first — it is the "everything" wildcard, not just another alphabetic entry. */
  private mergeSorted(base: string[], extra?: string[]): string[] {
    const set = new Set(base);
    (extra || []).forEach((v) => {
      if (v) {
        set.add(v);
      }
    });
    return Array.from(set).sort((a, b) => {
      if (a === ALL_SENTINEL || b === ALL_SENTINEL) {
        return a === ALL_SENTINEL ? -1 : 1;
      }
      return a.localeCompare(b);
    });
  }

  /** Changing the resource invalidates operations the new resource does not accept — dropped here
   * rather than at save time, so what's on screen matches what will be persisted. */
  onResourceChanged(row: UntypedFormGroup) {
    const resource = row.get('resource')?.value;
    const allowed = new Set(this.operationsFor(resource));
    const current: string[] = row.get('operations')?.value || [];
    const kept = current.filter((op) => allowed.has(op));
    if (kept.length !== current.length) {
      row.get('operations').setValue(kept);
    }
  }

  /** ALL subsumes every other operation — picking it alongside anything else collapses the
   * selection down to just ALL, rather than leaving a redundant "ALL, READ, WRITE" list on screen. */
  onOperationsChanged(control: AbstractControl) {
    const value: string[] = control.value || [];
    if (value.includes(ALL_SENTINEL) && value.length > 1) {
      control.setValue([ALL_SENTINEL]);
    }
  }

  onRoleIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('role.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }
    ));
  }

  addPermissionRow() {
    this.genericPermissionsArray.push(this.buildPermissionRow(null, []));
  }

  removePermissionRow(i: number) {
    this.genericPermissionsArray.removeAt(i);
  }

  private buildPermissionRow(resource: string, operations: string[]): UntypedFormGroup {
    return this.fb.group({
      resource: [resource, [Validators.required]],
      operations: [operations, [Validators.required]]
    });
  }

  private splitPermissions(role: Role): {genericRows: {resource: string; operations: string[]}[]; groupOperations: string[]} {
    const genericRows: {resource: string; operations: string[]}[] = [];
    let groupOperations: string[] = [];
    if (role.type === RoleType.GENERIC && role.permissions && !Array.isArray(role.permissions)) {
      for (const [resource, operations] of Object.entries(role.permissions)) {
        genericRows.push({resource, operations: operations as string[]});
      }
    } else if (role.type === RoleType.GROUP && Array.isArray(role.permissions)) {
      groupOperations = role.permissions as string[];
    }
    return {genericRows, groupOperations};
  }

  buildForm(entity: Role): UntypedFormGroup {
    const role = entity || {} as Role;
    const type = role.type || RoleType.GENERIC;
    const {genericRows, groupOperations} = this.splitPermissions({...role, type});
    const form = this.fb.group({
      name: [role.name || '', [Validators.required, Validators.maxLength(255)]],
      type: [type, [Validators.required]],
      additionalInfo: this.fb.group({
        description: [role.additionalInfo?.description || '']
      }),
      genericPermissions: this.fb.array(genericRows.map((row) => this.buildPermissionRow(row.resource, row.operations))),
      groupOperations: [groupOperations]
    });
    // Switching type mid-edit makes the other side's permission shape stale (a GENERIC role's
    // {resource: [operations]} map has nothing in common with a GROUP role's flat operation list) —
    // clear it so a leftover value can't be silently resubmitted under the new type.
    let previousType = type;
    form.get('type').valueChanges.subscribe((t: RoleType) => {
      if (t !== previousType) {
        previousType = t;
        (form.get('genericPermissions') as UntypedFormArray).clear();
        form.get('groupOperations').setValue([]);
      }
    });
    return form;
  }

  updateForm(entity: Role) {
    const {genericRows, groupOperations} = this.splitPermissions(entity);
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type}, {emitEvent: false});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo?.description || ''}});
    const arr = this.genericPermissionsArray;
    arr.clear();
    genericRows.forEach((row) => arr.push(this.buildPermissionRow(row.resource, row.operations)));
    this.entityForm.patchValue({groupOperations});
  }

  prepareFormValue(formValue: any): any {
    let permissions: any;
    if (formValue.type === RoleType.GENERIC) {
      permissions = {};
      (formValue.genericPermissions || []).forEach((row: any) => {
        if (row.resource) {
          permissions[row.resource] = row.operations;
        }
      });
    } else {
      permissions = formValue.groupOperations || [];
    }
    delete formValue.genericPermissions;
    delete formValue.groupOperations;
    formValue.permissions = permissions;
    return super.prepareFormValue(formValue);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    }
    return false;
  }

}
