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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroup } from '@shared/models/entity-group.models';
import { CreateEntityGroupFunction } from '@shared/components/entity/entity-group-list.component';
import {
  EntityGroupDialogComponent,
  EntityGroupDialogData
} from '@home/pages/entity-group/entity-group-dialog.component';

export interface OwnerAndGroups {
  owner: EntityInfoData | EntityId;
  groups: Array<EntityInfoData>;
}

/**
 * Owner + groups pair used when creating a group-owned entity. PE parity: `tb-owner-and-groups`.
 * The groups field stays disabled until an owner is picked, because groups are listed per owner.
 */
@Component({
  selector: 'tb-owner-and-groups',
  templateUrl: './owner-and-groups.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OwnerAndGroupsComponent),
      multi: true
    }
  ],
  standalone: false
})
export class OwnerAndGroupsComponent implements ControlValueAccessor, OnInit {

  ownerAndGroupsFormGroup: UntypedFormGroup;

  private modelValue: OwnerAndGroups;

  @Input()
  entityType: EntityType;

  @Input()
  defaultOwnerId: EntityId;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  disabled: boolean;

  createGroupFunction: CreateEntityGroupFunction;

  private propagateChange = (_v: any) => { };

  constructor(private dialog: MatDialog,
              private destroyRef: DestroyRef,
              private fb: UntypedFormBuilder) {
    this.ownerAndGroupsFormGroup = this.fb.group({
      owner: [null, [Validators.required]],
      groups: [null]
    });
  }

  ngOnInit() {
    this.createGroupFunction = (groupType, groupName, ownerId) => this.createEntityGroup(groupType, groupName, ownerId);
    this.ownerAndGroupsFormGroup.get('groups').disable({emitEvent: false});
    this.ownerAndGroupsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      if (value.owner) {
        this.ownerAndGroupsFormGroup.get('groups').enable({emitEvent: false});
      } else {
        this.ownerAndGroupsFormGroup.get('groups').disable({emitEvent: false});
      }
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.ownerAndGroupsFormGroup.disable({emitEvent: false});
    } else {
      this.ownerAndGroupsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: OwnerAndGroups): void {
    this.modelValue = value;
    this.ownerAndGroupsFormGroup.patchValue(value || {}, {emitEvent: false});
  }

  /** The group list needs a plain id — the owner control may hold either shape. */
  ownerId(): EntityId | null {
    const owner = this.ownerAndGroupsFormGroup.get('owner').value;
    if (!owner) {
      return null;
    }
    return (owner as EntityInfoData).name !== undefined ? (owner as EntityInfoData).id : owner as EntityId;
  }

  private updateModel() {
    this.modelValue = this.ownerAndGroupsFormGroup.getRawValue() || {};
    this.propagateChange(this.ownerAndGroupsFormGroup.valid ? this.modelValue : null);
  }

  /**
   * Creates a group under the currently selected owner and returns it, so the picker can add the
   * new group straight to the chip list.
   */
  private createEntityGroup(groupType: EntityType, groupName: string,
                            ownerId: EntityId): Observable<EntityInfoData | null> {
    const data: EntityGroupDialogData = {groupType, name: groupName};
    if (ownerId && ownerId.entityType === EntityType.CUSTOMER) {
      data.ownerId = ownerId.id;
    }
    return this.dialog.open<EntityGroupDialogComponent, EntityGroupDialogData, EntityGroup | false>(
      EntityGroupDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data
      }).afterClosed().pipe(
      map(created => created ? {id: created.id, name: created.name} as EntityInfoData : null)
    );
  }
}
