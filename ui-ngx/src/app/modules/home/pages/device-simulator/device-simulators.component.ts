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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { DialogService } from '@core/services/dialog.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DeviceSimulatorService } from '@core/http/device-simulator.service';
import { DeviceSimulatorInfo, SimulatorRunMode } from '@shared/models/device-simulator.models';
import {
  DeviceSimulatorDialogComponent,
  DeviceSimulatorDialogData
} from '@home/pages/device-simulator/device-simulator-dialog.component';

const POLL_INTERVAL_MS = 5000;

@Component({
  selector: 'tb-device-simulators',
  templateUrl: './device-simulators.component.html',
  styleUrls: ['./device-simulators.component.scss'],
  standalone: false
})
export class DeviceSimulatorsComponent extends PageComponent implements OnInit, OnDestroy {

  simulators: DeviceSimulatorInfo[];
  SimulatorRunMode = SimulatorRunMode;

  displayedColumns = ['name', 'device', 'status', 'published', 'actions'];

  private pollHandle: ReturnType<typeof setInterval>;

  constructor(protected store: Store<AppState>,
              private deviceSimulatorService: DeviceSimulatorService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService,
              private route: ActivatedRoute) {
    super(store);
    this.simulators = this.route.snapshot.data.deviceSimulators;
  }

  ngOnInit(): void {
    this.pollHandle = setInterval(() => {
      if (this.simulators?.some(s => s.running)) {
        this.reload();
      }
    }, POLL_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.pollHandle) {
      clearInterval(this.pollHandle);
    }
  }

  reload(): void {
    this.deviceSimulatorService.getDeviceSimulators().subscribe(simulators => {
      this.simulators = simulators;
    });
  }

  add(): void {
    this.openDialog({});
  }

  edit(simulator: DeviceSimulatorInfo): void {
    this.openDialog({simulator});
  }

  private openDialog(data: DeviceSimulatorDialogData): void {
    this.dialog.open<DeviceSimulatorDialogComponent, DeviceSimulatorDialogData, boolean>(DeviceSimulatorDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog'],
      data
    }).afterClosed().subscribe(saved => {
      if (saved) {
        this.reload();
      }
    });
  }

  start(simulator: DeviceSimulatorInfo, mode: SimulatorRunMode): void {
    this.deviceSimulatorService.startDeviceSimulator(simulator.id.id, mode).subscribe({
      next: () => this.reload(),
      error: (error) => this.store.dispatch(new ActionNotificationShow({
        message: error?.error?.message || this.translate.instant('device-simulator.start-failed'),
        type: 'error'
      }))
    });
  }

  stop(simulator: DeviceSimulatorInfo): void {
    this.deviceSimulatorService.stopDeviceSimulator(simulator.id.id).subscribe(() => this.reload());
  }

  remove(simulator: DeviceSimulatorInfo): void {
    this.dialogService.confirm(
      this.translate.instant('device-simulator.delete-title', {name: simulator.name}),
      this.translate.instant('device-simulator.delete-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(result => {
      if (result) {
        this.deviceSimulatorService.deleteDeviceSimulator(simulator.id.id).subscribe(() => this.reload());
      }
    });
  }

}
