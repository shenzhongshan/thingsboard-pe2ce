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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { DeviceSimulator, DeviceSimulatorInfo, SimulatorRunMode } from '@shared/models/device-simulator.models';

@Injectable({
  providedIn: 'root'
})
export class DeviceSimulatorService {

  constructor(private http: HttpClient) {
  }

  public getDeviceSimulators(config?: RequestConfig): Observable<DeviceSimulatorInfo[]> {
    return this.http.get<DeviceSimulatorInfo[]>('/api/device-simulators', defaultHttpOptionsFromConfig(config));
  }

  public getDeviceSimulatorById(deviceSimulatorId: string, config?: RequestConfig): Observable<DeviceSimulatorInfo> {
    return this.http.get<DeviceSimulatorInfo>(`/api/device-simulators/${deviceSimulatorId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveDeviceSimulator(simulator: DeviceSimulator, config?: RequestConfig): Observable<DeviceSimulator> {
    return this.http.post<DeviceSimulator>('/api/device-simulators', simulator, defaultHttpOptionsFromConfig(config));
  }

  public updateDeviceSimulator(deviceSimulatorId: string, simulator: DeviceSimulator,
                                config?: RequestConfig): Observable<DeviceSimulator> {
    return this.http.put<DeviceSimulator>(`/api/device-simulators/${deviceSimulatorId}`, simulator, defaultHttpOptionsFromConfig(config));
  }

  public deleteDeviceSimulator(deviceSimulatorId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/device-simulators/${deviceSimulatorId}`, defaultHttpOptionsFromConfig(config));
  }

  public startDeviceSimulator(deviceSimulatorId: string, mode: SimulatorRunMode,
                               config?: RequestConfig): Observable<DeviceSimulatorInfo> {
    return this.http.post<DeviceSimulatorInfo>(`/api/device-simulators/${deviceSimulatorId}/start?mode=${mode}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public stopDeviceSimulator(deviceSimulatorId: string, config?: RequestConfig): Observable<DeviceSimulatorInfo> {
    return this.http.post<DeviceSimulatorInfo>(`/api/device-simulators/${deviceSimulatorId}/stop`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public getDeviceSimulatorStatus(deviceSimulatorId: string, config?: RequestConfig): Observable<DeviceSimulatorInfo> {
    return this.http.get<DeviceSimulatorInfo>(`/api/device-simulators/${deviceSimulatorId}/status`, defaultHttpOptionsFromConfig(config));
  }

}
