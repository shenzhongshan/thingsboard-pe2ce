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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceSimulatorService } from '@core/http/device-simulator.service';
import {
  defaultEmulatorDefinition,
  defaultValueStrategy,
  DeviceSimulator,
  DeviceSimulatorInfo,
  EmulatorDefinition,
  TelemetryProfile,
  ValueStrategyType
} from '@shared/models/device-simulator.models';

export interface DeviceSimulatorDialogData {
  simulator?: DeviceSimulatorInfo;
}

@Component({
  selector: 'tb-device-simulator-dialog',
  templateUrl: './device-simulator-dialog.component.html',
  styleUrls: ['./device-simulator-dialog.component.scss'],
  standalone: false
})
export class DeviceSimulatorDialogComponent extends DialogComponent<DeviceSimulatorDialogComponent, boolean> {

  EntityType = EntityType;
  ValueStrategyType = ValueStrategyType;

  isAdd: boolean;
  name: string;
  deviceId: EntityId;
  definition: EmulatorDefinition;

  jsonMode = false;
  jsonText = '';
  jsonError: string;

  private simulatorId: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<DeviceSimulatorDialogComponent, boolean>,
              private translate: TranslateService,
              private deviceSimulatorService: DeviceSimulatorService,
              @Inject(MAT_DIALOG_DATA) public data: DeviceSimulatorDialogData) {
    super(store, router, dialogRef);
    this.isAdd = !data.simulator;
    if (data.simulator) {
      this.simulatorId = data.simulator.id.id;
      this.name = data.simulator.name;
      this.deviceId = data.simulator.deviceId;
      this.definition = data.simulator.definition ?? defaultEmulatorDefinition();
    } else {
      this.name = '';
      this.deviceId = null;
      this.definition = defaultEmulatorDefinition();
    }
  }

  addProfile(): void {
    const profile: TelemetryProfile = {
      key: '',
      valueStrategy: defaultValueStrategy(ValueStrategyType.CONSTANT)
    };
    this.definition.telemetryProfiles = [...this.definition.telemetryProfiles, profile];
  }

  removeProfile(index: number): void {
    this.definition.telemetryProfiles.splice(index, 1);
  }

  toggleJsonMode(): void {
    if (!this.jsonMode) {
      this.jsonText = JSON.stringify(this.definition, null, 2);
      this.jsonMode = true;
      this.jsonError = null;
    } else {
      try {
        const parsed = JSON.parse(this.jsonText);
        if (!Array.isArray(parsed.telemetryProfiles)) {
          throw new Error(this.translate.instant('device-simulator.json-missing-telemetry-profiles'));
        }
        this.definition = parsed;
        this.jsonMode = false;
        this.jsonError = null;
      } catch (e) {
        this.jsonError = e.message;
      }
    }
  }

  isValid(): boolean {
    if (!this.name || !this.name.trim() || !this.deviceId) {
      return false;
    }
    if (this.jsonMode) {
      return false;
    }
    return this.definition.telemetryProfiles.length > 0 &&
      this.definition.telemetryProfiles.every(p => !!p.key && p.key.trim().length > 0);
  }

  save(): void {
    if (!this.isValid()) {
      return;
    }
    const simulator: DeviceSimulator = {
      name: this.name,
      deviceId: this.deviceId,
      definition: this.definition
    };
    const obs = this.isAdd ?
      this.deviceSimulatorService.saveDeviceSimulator(simulator) :
      this.deviceSimulatorService.updateDeviceSimulator(this.simulatorId, simulator);
    obs.subscribe({
      next: () => {
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.store.dispatch(new ActionNotificationShow({
          message: error?.error?.message || this.translate.instant('device-simulator.save-failed'),
          type: 'error'
        }));
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
