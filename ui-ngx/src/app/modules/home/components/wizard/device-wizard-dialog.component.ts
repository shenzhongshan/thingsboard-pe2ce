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

import { Component, Inject, Optional, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { Device, DeviceProfileInfo, DeviceTransportType } from '@shared/models/device.models';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable, of, throwError } from 'rxjs';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { DeviceService } from '@core/http/device.service';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { deepTrim } from '@core/utils';
import { CustomerId } from '@shared/models/id/customer-id';
import { HttpErrorResponse } from '@angular/common/http';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, isGroupAll } from '@shared/models/entity-group.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityId } from '@shared/models/id/entity-id';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TenantId } from '@shared/models/id/tenant-id';
import {
  EntityGroupDialogComponent,
  EntityGroupDialogData
} from '@home/pages/entity-group/entity-group-dialog.component';

export interface DeviceWizardDialogData {
  // When set, the group's owner and id pre-fill the owner/groups fields — matches PE's
  // "add device from within a group's member view" flow.
  presetGroup?: EntityGroup;
}

@Component({
    selector: 'tb-device-wizard',
    templateUrl: './device-wizard-dialog.component.html',
    styleUrls: ['./device-wizard-dialog.component.scss'],
    standalone: false
})
export class DeviceWizardDialogComponent extends DialogComponent<DeviceWizardDialogComponent, Device> {

  @ViewChild('addDeviceWizardStepper', {static: true}) addDeviceWizardStepper: MatStepper;

  stepperOrientation: Observable<StepperOrientation>;

  stepperLabelPosition: Observable<'bottom' | 'end'>;

  selectedIndex = 0;

  credentialsOptionalStep = true;

  showNext = true;

  entityType = EntityType;

  deviceWizardFormGroup: FormGroup;

  credentialsFormGroup: FormGroup;

  private currentDeviceProfileTransportType = DeviceTransportType.DEFAULT;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<DeviceWizardDialogComponent, Device>,
              private deviceService: DeviceService,
              private entityGroupService: EntityGroupService,
              private breakpointObserver: BreakpointObserver,
              private dialog: MatDialog,
              private fb: FormBuilder,
              @Optional() @Inject(MAT_DIALOG_DATA) private data: DeviceWizardDialogData | null) {
    super(store, router, dialogRef);

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'end' : 'bottom'));

    // A group's ownerId is null for plain tenant-owned groups — default to the current tenant
    // so the owner field pre-fills the same way PE's does, instead of showing blank.
    const presetOwner: EntityId = (this.data?.presetGroup?.ownerId as EntityId)
      ?? (this.data?.presetGroup ? new TenantId(getCurrentAuthUser(this.store).tenantId) : null);

    this.deviceWizardFormGroup = this.fb.group({
        name: ['', [Validators.required, Validators.maxLength(255)]],
        label: ['', Validators.maxLength(255)],
        gateway: [false],
        overwriteActivityTime: [false],
        ownerId: [presetOwner],
        // "All" groups have automatic membership (managed via customerId assignment) — nothing
        // to explicitly add to, so only preset a real, named group.
        groupIds: [this.data?.presetGroup && !isGroupAll(this.data.presetGroup) ? [this.data.presetGroup.id.id] : []],
        deviceProfileId: [null, Validators.required],
        description: ['']
      }
    );

    this.credentialsFormGroup  = this.fb.group({
        credential: []
      }
    );
  }

  createNewGroup($event: string): void {
    this.dialog.open<EntityGroupDialogComponent, EntityGroupDialogData, boolean>(EntityGroupDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog'],
      data: {groupType: EntityType.DEVICE}
    }).afterClosed().subscribe(created => {
      if (created) {
        this.entityGroupService.getEntityGroupsByType(EntityType.DEVICE, new PageLink(1, 0, $event)).subscribe(page => {
          if (page.data.length) {
            const groupIds: string[] = this.deviceWizardFormGroup.get('groupIds').value || [];
            this.deviceWizardFormGroup.get('groupIds').setValue([...groupIds, page.data[0].id.id]);
          }
        });
      }
    });
  }

  /** "所有者" allows picking either the tenant itself (= no customer) or a customer;
   * tb-entity-select emits a generic EntityId, so a TENANT selection (or nothing) converts to
   * null here. */
  private ownerAsCustomerId(): CustomerId | null {
    const owner = this.deviceWizardFormGroup.get('ownerId').value as EntityId;
    return owner && owner.entityType === EntityType.CUSTOMER ? new CustomerId(owner.id) : null;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addDeviceWizardStepper.previous();
  }

  nextStep(): void {
    this.addDeviceWizardStepper.next();
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return 'device.wizard.device-details';
      case 1:
        return 'device.credentials';
    }
  }

  get maxStepperIndex(): number {
    return this.addDeviceWizardStepper?._steps?.length - 1;
  }

  add(): void {
    if (this.allValid()) {
      this.createDevice().pipe(
        mergeMap(device => {
          const groupIds: string[] = this.deviceWizardFormGroup.get('groupIds').value || [];
          if (!groupIds.length) {
            return of(device);
          }
          return forkJoin(groupIds.map(groupId => this.entityGroupService.addEntitiesToEntityGroup(groupId, [device.id.id])))
            .pipe(map(() => device));
        })
      ).subscribe(
        (device) => this.dialogRef.close(device)
      );
    }
  }

  get deviceTransportType(): DeviceTransportType {
    return this.currentDeviceProfileTransportType;
  }

  deviceProfileChanged(deviceProfile: DeviceProfileInfo) {
    if (deviceProfile) {
      this.currentDeviceProfileTransportType = deviceProfile.transportType;
      this.credentialsOptionalStep = this.currentDeviceProfileTransportType !== DeviceTransportType.LWM2M;
    }
  }

  private createDevice(): Observable<Device> {
    const device: Device = {
      name: this.deviceWizardFormGroup.get('name').value,
      label: this.deviceWizardFormGroup.get('label').value,
      deviceProfileId: this.deviceWizardFormGroup.get('deviceProfileId').value,
      additionalInfo: {
        gateway: this.deviceWizardFormGroup.get('gateway').value,
        overwriteActivityTime: this.deviceWizardFormGroup.get('overwriteActivityTime').value,
        description: this.deviceWizardFormGroup.get('description').value
      },
      customerId: this.ownerAsCustomerId()
    };
    if (this.addDeviceWizardStepper.steps.last.completed || this.addDeviceWizardStepper.selectedIndex > 0) {
      return this.deviceService.saveDeviceWithCredentials(deepTrim(device), deepTrim(this.credentialsFormGroup.value.credential)).pipe(
        catchError((e: HttpErrorResponse) => {
          if (e.error.message.includes('Device credentials')) {
            this.addDeviceWizardStepper.selectedIndex = 1;
          } else {
            this.addDeviceWizardStepper.selectedIndex = 0;
          }
          return throwError(() => e);
        })
      );
    }
    return this.deviceService.saveDevice(deepTrim(device)).pipe(
      catchError(e => {
        this.addDeviceWizardStepper.selectedIndex = 0;
        return throwError(e);
      })
    );
  }

  allValid(): boolean {
    return !this.addDeviceWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addDeviceWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    this.showNext = this.selectedIndex !== this.maxStepperIndex;
  }
}
