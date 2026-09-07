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

import { Injectable, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Observable } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { DeviceSimulatorService } from '@core/http/device-simulator.service';
import { DeviceSimulatorInfo } from '@shared/models/device-simulator.models';
import { DeviceSimulatorsComponent } from '@home/pages/device-simulator/device-simulators.component';

@Injectable()
export class DeviceSimulatorsResolver {

  constructor(private deviceSimulatorService: DeviceSimulatorService) {
  }

  resolve(): Observable<DeviceSimulatorInfo[]> {
    return this.deviceSimulatorService.getDeviceSimulators();
  }

}

const routes: Routes = [
  {
    path: 'deviceSimulators',
    data: {
      breadcrumb: {
        menuId: MenuId.device_simulators
      }
    },
    children: [
      {
        path: '',
        component: DeviceSimulatorsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'device-simulator.device-simulators'
        },
        resolve: {
          deviceSimulators: DeviceSimulatorsResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DeviceSimulatorsResolver
  ]
})
export class DeviceSimulatorRoutingModule { }
