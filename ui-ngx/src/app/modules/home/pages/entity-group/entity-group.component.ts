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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatDialog } from '@angular/material/dialog';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityGroup, isGroupAll } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  EntityGroupShareDialogComponent,
  EntityGroupShareDialogData
} from '@home/pages/entity-group/entity-group-share-dialog.component';
import { EntityGroupService } from '@core/http/entity-group.service';
import { OtaUpdateType } from '@shared/models/ota-package.models';

@Component({
    selector: 'tb-entity-group',
    templateUrl: './entity-group.component.html',
    styleUrls: ['./entity-group.component.scss'],
    standalone: false
})
export class EntityGroupComponent extends EntityComponent<EntityGroup> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private dialog: MatDialog,
              private entityGroupService: EntityGroupService,
              @Inject('entity') protected entityValue: EntityGroup,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<EntityGroup>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  openShareDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EntityGroupShareDialogComponent, EntityGroupShareDialogData, boolean>(
      EntityGroupShareDialogComponent, {
        disableClose: true,
        data: {entityGroup: this.entity}
      });
  }

  get isAll(): boolean {
    return isGroupAll(this.entity);
  }

  get isPublic(): boolean {
    return this.entity?.additionalInfo?.isPublic === true;
  }

  get showOpenButton(): boolean {
    return this.entitiesTableConfig?.componentsData?.showOpenButton !== false;
  }

  get isDeviceGroup(): boolean {
    return this.entity?.type === EntityType.DEVICE;
  }

  buildForm(entity: EntityGroup): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      firmwareId: [null],
      softwareId: [null],
      description: [entity?.additionalInfo?.description || '']
    });
  }

  updateForm(entity: EntityGroup) {
    this.entityForm.patchValue({
      name: entity.name,
      firmwareId: null,
      softwareId: null,
      description: entity?.additionalInfo?.description || ''
    }, {emitEvent: false});
    if (this.isDeviceGroup && entity?.id?.id) {
      this.entityGroupService.getDeviceGroupOtaPackage(entity.id.id, OtaUpdateType.FIRMWARE, {ignoreErrors: true}).subscribe({
        next: pkg => this.entityForm.patchValue({firmwareId: pkg?.otaPackageId || null}, {emitEvent: false}),
        error: () => {}
      });
      this.entityGroupService.getDeviceGroupOtaPackage(entity.id.id, OtaUpdateType.SOFTWARE, {ignoreErrors: true}).subscribe({
        next: pkg => this.entityForm.patchValue({softwareId: pkg?.otaPackageId || null}, {emitEvent: false}),
        error: () => {}
      });
    }
  }

  prepareFormValue(formValue: any): any {
    const value = super.prepareFormValue(formValue);
    return {
      name: value.name,
      additionalInfo: {...(this.entity?.additionalInfo || {}), description: value.description},
      // Not real EntityGroup fields — read and stripped by createGroupDetailsConfig()'s saveEntity
      // wrapper, which persists them via the separate DeviceGroupOtaPackage API.
      pendingFirmwareId: this.isDeviceGroup ? (value.firmwareId ?? null) : undefined,
      pendingSoftwareId: this.isDeviceGroup ? (value.softwareId ?? null) : undefined
    };
  }

  onGroupIdCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('entity-group.id-copied-message'),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

}
