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
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';
import { EntityGroup } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { isEqual } from '@core/utils';

export type OriginatorTargetMode = 'entity' | 'groupTenant' | 'ownerGroup';

/**
 * Three-way "what does this act on" picker: a single entity, a group of entities, or a group
 * belonging to another owner. PE parity: `tb-originator-select`.
 *
 * The model is always a plain {@link EntityId} — either the picked entity, or the picked group as an
 * `ENTITY_GROUP` id. `groupTenant` and `ownerGroup` therefore produce the same shape; on re-open the
 * mode is re-derived from the resolved group's owner (see {@link entityGroupLoaded}), exactly as PE does.
 */
@Component({
  selector: 'tb-originator-select',
  templateUrl: './originator-select.component.html',
  styleUrls: ['./originator-select.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OriginatorSelectComponent),
      multi: true
    }
  ],
  standalone: false
})
export class OriginatorSelectComponent implements ControlValueAccessor, OnInit {

  originatorFormGroup: UntypedFormGroup;

  modelValue: EntityId | null;

  @Input()
  allowedEntityTypes: EntityType[];

  @Input()
  singleEntityText = 'scheduler.single-entity';

  @Input()
  groupOfEntitiesText = 'scheduler.group-entities';

  @Input()
  entitiesGroupOwnerText = 'scheduler.entities-group-owner';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Output()
  currentGroupType = new EventEmitter<EntityType>();

  headerOptions: ToggleHeaderOption[];

  currentUser = getCurrentAuthUser(this.store);

  excludeOwnerIds = [this.currentUser.tenantId];

  /** Set while a group id written from the model is still being resolved to its owner. */
  private loadData = false;

  private propagateChange = (_v: any) => { };

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    this.originatorFormGroup = this.fb.group({
      originator: ['entity'],
      entityOriginatorId: [null],
      groupOriginatorId: [null],
      groupOwnerId: [null]
    });
  }

  ngOnInit() {
    this.headerOptions = [
      {name: this.translate.instant(this.singleEntityText), value: 'entity'},
      {name: this.translate.instant(this.groupOfEntitiesText), value: 'groupTenant'},
      {name: this.translate.instant(this.entitiesGroupOwnerText), value: 'ownerGroup'}
    ];
    this.originatorFormGroup.get('originator').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((mode: OriginatorTargetMode) => {
      if (mode === 'groupTenant' || mode === 'ownerGroup') {
        this.originatorFormGroup.patchValue({
          groupOriginatorId: {entityType: EntityType.ENTITY_GROUP, id: null} as EntityId,
          groupOwnerId: null
        }, {emitEvent: false});
      } else {
        this.originatorFormGroup.get('entityOriginatorId').patchValue(null, {emitEvent: false});
      }
    });
    this.originatorFormGroup.get('groupOwnerId').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.originatorFormGroup.patchValue({groupOriginatorId: null}, {emitEvent: false}));
    this.originatorFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => this.updateView(value));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.originatorFormGroup.disable({emitEvent: false});
    } else {
      this.originatorFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityId | null): void {
    this.modelValue = value;
    if (value && value.entityType === EntityType.ENTITY_GROUP) {
      this.originatorFormGroup.patchValue({originator: 'groupTenant', groupOriginatorId: value}, {emitEvent: false});
      this.loadData = true;
    } else {
      this.originatorFormGroup.patchValue({originator: 'entity', entityOriginatorId: value, groupOriginatorId: null},
        {emitEvent: false});
    }
  }

  /**
   * A group id alone doesn't say whether it was picked under this tenant or under a customer — once
   * the group resolves, switch to `ownerGroup` mode if it belongs to someone else.
   */
  entityGroupLoaded(group: EntityGroup) {
    if (this.loadData) {
      this.loadData = false;
      if (group) {
        if (this.currentUser.authority === Authority.TENANT_ADMIN && group.ownerId?.id !== this.currentUser.tenantId) {
          this.originatorFormGroup.get('originator').patchValue('ownerGroup', {emitEvent: false});
          this.originatorFormGroup.get('groupOwnerId').patchValue(group.ownerId, {emitEvent: false});
        } else {
          this.originatorFormGroup.get('originator').patchValue('groupTenant', {emitEvent: false});
          this.originatorFormGroup.get('groupOwnerId').patchValue(null, {emitEvent: false});
        }
      } else {
        this.originatorFormGroup.patchValue({originator: 'groupTenant', groupOwnerId: null, groupOriginatorId: null},
          {emitEvent: true});
      }
    }
  }

  private updateView(value: any) {
    const originatorId: EntityId = value ?
      (value.originator !== 'entity' ? value.groupOriginatorId : value.entityOriginatorId) : null;
    if (!isEqual(this.modelValue, originatorId)) {
      this.modelValue = originatorId;
      this.propagateChange(this.modelValue?.id ? this.modelValue : null);
    }
  }
}
