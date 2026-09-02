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

import { Component, DestroyRef, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { of } from 'rxjs';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, entityGroupTypes } from '@shared/models/entity-group.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';

/**
 * Entity-type select + entity-group autocomplete on one row — the pair PE renders whenever a form
 * targets "some group of some type". PE parity: `tb-entity-group-select`.
 *
 * The model is an `{entityType: ENTITY_GROUP, id}` {@link EntityId}. When only one group type is
 * allowed the type select collapses away, exactly as in PE.
 */
@Component({
  selector: 'tb-entity-group-select',
  templateUrl: './entity-group-select.component.html',
  styleUrls: ['./entity-group-select.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityGroupSelectComponent),
      multi: true
    }
  ],
  standalone: false
})
export class EntityGroupSelectComponent implements ControlValueAccessor, OnInit {

  entityGroupSelectFormGroup: UntypedFormGroup;

  modelValue: string | null;

  @Input()
  allowedGroupTypes: EntityType[];

  @Input()
  excludeGroupTypes: EntityType[];

  @Input()
  defaultGroupType: EntityType;

  @Input()
  excludeGroupIds: string[];

  @Input()
  @coerceBoolean()
  excludeGroupAll: boolean;

  @Input()
  placeholderText = 'entity-group.entity-group';

  @Input()
  notFoundText = 'entity-group.no-entity-groups-matching';

  @Input()
  requiredText = 'entity-group.entity-group-required';

  @Input()
  ownerId: EntityId;

  /** The owning `tb-originator-select` mode — used only to hide this row until an owner is picked. */
  @Input()
  originator: string;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Output()
  currentGroupType = new EventEmitter<EntityType>();

  @Output()
  currentGroupInfo = new EventEmitter<EntityGroup>();

  entityGroupTypeList: EntityType[];

  displayGroupTypeSelect: boolean;

  private propagateChange = (_v: any) => { };

  constructor(private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.entityGroupTypeList = [...entityGroupTypes];
    if (this.allowedGroupTypes && this.allowedGroupTypes.length) {
      this.entityGroupTypeList = entityGroupTypes.filter(type => this.allowedGroupTypes.indexOf(type) !== -1);
    }
    if (this.excludeGroupTypes && this.excludeGroupTypes.length) {
      this.entityGroupTypeList = this.entityGroupTypeList.filter(type => this.excludeGroupTypes.indexOf(type) === -1);
    }
    if (this.entityGroupTypeList.length === 1) {
      this.displayGroupTypeSelect = false;
      this.defaultGroupType = this.entityGroupTypeList[0];
    } else {
      this.displayGroupTypeSelect = true;
    }
    this.entityGroupSelectFormGroup = this.fb.group({
      groupType: [this.defaultGroupType],
      groupId: [null]
    });
    this.currentGroupType.next(this.defaultGroupType);
    this.entityGroupSelectFormGroup.get('groupType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((groupType: EntityType) => this.currentGroupType.next(groupType));
    this.entityGroupSelectFormGroup.get('groupId').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((groupId: string) => this.updateView(groupId));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityGroupSelectFormGroup.disable();
    } else {
      this.entityGroupSelectFormGroup.enable();
    }
  }

  writeValue(value: EntityId | null): void {
    this.entityGroupSelectFormGroup.get('groupId').patchValue(value ? value.id : null, {emitEvent: true});
    if (value) {
      this.modelValue = value.id;
    }
  }

  /** The autocomplete resolved (or cleared) a group — mirror its type into the type select. */
  entityGroupLoaded(group: EntityInfoData) {
    (group ? this.entityGroupService.getEntityGroup(group.id.id, {ignoreLoading: true, ignoreErrors: true}) :
      of(null as EntityGroup)).subscribe((entityGroup) => {
      this.currentGroupInfo.next(entityGroup);
      this.entityGroupSelectFormGroup.get('groupType').patchValue(entityGroup ? entityGroup.type : null, {emitEvent: true});
    });
  }

  getCurrentGroupType(): EntityType {
    return this.entityGroupSelectFormGroup?.get('groupType').value ?? this.defaultGroupType;
  }

  private updateView(groupId: string) {
    if (this.modelValue !== groupId) {
      this.modelValue = groupId;
      this.propagateChange(this.modelValue ? {entityType: EntityType.ENTITY_GROUP, id: this.modelValue} as EntityId : null);
    }
  }
}
