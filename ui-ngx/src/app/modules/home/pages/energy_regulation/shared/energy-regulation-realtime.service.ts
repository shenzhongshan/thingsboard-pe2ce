import {Injectable, NgZone} from '@angular/core';
import {TelemetryWebsocketService} from '@core/ws/telemetry-websocket.service';
import {ProductRealtimeService} from '@shared/realtime/product-realtime.service';
import {ENERGY_REALTIME_KEYS} from './energy-regulation-measurements';
export type {DeviceTelemetryChange, AlarmRealtimeChange} from '@shared/realtime/product-realtime.service';

/** 能源监管模块独立的设备、告警和测点订阅范围，公共层负责连接与原生查询。 */
@Injectable({providedIn: 'root'})
export class EnergyRegulationRealtimeService extends ProductRealtimeService {
  protected override readonly productCategory = 'energy_regulation';
  protected override readonly telemetryKeys = ENERGY_REALTIME_KEYS;

  constructor(websocket: TelemetryWebsocketService, zone: NgZone) {
    super(websocket, zone);
  }
}
