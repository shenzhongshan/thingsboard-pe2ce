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

export type DeviceTargetMode = 'entity' | 'deviceProfile' | 'groupTenant' | 'ownerGroup';

/**
 * Device-flavoured target picker: a device, a device profile, a device group, or a device group
 * owned by a customer. PE parity: `tb-target-select` (used by the OTA scheduler event types).
 *
 * Emits the resolved target through {@link currentEntity} so the caller can scope its OTA package
 * list — a `DeviceProfileId` for the profile branch, or the {@link EntityGroup} for group branches.
 */
@Component({
  selector: 'tb-target-select',
  templateUrl: './target-select.component.html',
  styleUrls: ['./target-select.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TargetSelectComponent),
      multi: true
    }
  ],
  standalone: false
})
export class TargetSelectComponent implements ControlValueAccessor, OnInit {

  targetFormGroup: UntypedFormGroup;

  modelValue: EntityId | null;

  entityType = EntityType.DEVICE;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Output()
  currentEntity = new EventEmitter<EntityId | EntityGroup>();

  headerOptions: ToggleHeaderOption[];

  currentUser = getCurrentAuthUser(this.store);

  excludeOwnerIds = [this.currentUser.tenantId];

  private loadData = false;

  private propagateChange = (_v: any) => { };

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    this.targetFormGroup = this.fb.group({
      target: ['entity'],
      deviceTargetId: [null],
      deviceProfileTargetId: [null],
      groupOriginatorId: [null],
      groupOwnerId: [null]
    });
  }

  ngOnInit() {
    this.headerOptions = [
      {name: this.translate.instant('device.device'), value: 'entity'},
      {name: this.translate.instant('device-profile.device-profile'), value: 'deviceProfile'},
      {name: this.translate.instant('entity-group.device-group'), value: 'groupTenant'},
      {name: this.translate.instant('scheduler.devices-group-owner'), value: 'ownerGroup'}
    ];
    this.targetFormGroup.get('target').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((mode: DeviceTargetMode) => {
      if (mode === 'groupTenant' || mode === 'ownerGroup') {
        this.targetFormGroup.patchValue({groupOriginatorId: null, groupOwnerId: null}, {emitEvent: false});
      } else if (mode === 'entity') {
        this.targetFormGroup.get('deviceTargetId').patchValue(null, {emitEvent: false});
      } else {
        this.targetFormGroup.get('deviceProfileTargetId').patchValue(null, {emitEvent: false});
      }
    });
    this.targetFormGroup.get('groupOwnerId').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.targetFormGroup.patchValue({groupOriginatorId: null}, {emitEvent: false}));
    this.targetFormGroup.valueChanges.pipe(
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
      this.targetFormGroup.disable({emitEvent: false});
    } else {
      this.targetFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityId | null): void {
    this.modelValue = value;
    let mode: DeviceTargetMode = 'entity';
    if (value?.entityType === EntityType.ENTITY_GROUP) {
      mode = 'groupTenant';
    } else if (value?.entityType === EntityType.DEVICE_PROFILE) {
      mode = 'deviceProfile';
    }
    this.targetFormGroup.patchValue({
      target: mode,
      deviceTargetId: mode === 'entity' ? value : null,
      deviceProfileTargetId: mode === 'deviceProfile' ? value : null,
      groupOriginatorId: mode === 'groupTenant' ? value : null
    }, {emitEvent: false});
    this.loadData = true;
  }

  entityGroupLoaded(group: EntityGroup) {
    if (this.loadData && group) {
      this.loadData = false;
      if (this.currentUser.authority === Authority.TENANT_ADMIN && group.ownerId?.id !== this.currentUser.tenantId) {
        this.targetFormGroup.patchValue({target: 'ownerGroup', groupOwnerId: group.ownerId, groupOriginatorId: group.id},
          {emitEvent: false});
      } else {
        this.targetFormGroup.patchValue({target: 'groupTenant', groupOriginatorId: group.id}, {emitEvent: false});
      }
    }
    if (group) {
      this.currentEntity.emit(group);
    }
  }

  deviceProfileLoaded(deviceProfile: any) {
    if (deviceProfile?.id) {
      this.currentEntity.emit(deviceProfile.id);
    }
  }

  deviceLoaded(device: any) {
    if (device?.deviceProfileId) {
      this.currentEntity.emit(device.deviceProfileId);
    }
  }

  private updateView(value: any) {
    let target: EntityId = null;
    switch (value?.target) {
      case 'entity':
        target = value.deviceTargetId;
        break;
      case 'deviceProfile':
        target = value.deviceProfileTargetId;
        break;
      case 'groupTenant':
      case 'ownerGroup':
        target = value.groupOriginatorId;
        break;
    }
    if (!isEqual(this.modelValue, target)) {
      this.modelValue = target;
      this.propagateChange(this.modelValue?.id ? this.modelValue : null);
    }
  }
}
