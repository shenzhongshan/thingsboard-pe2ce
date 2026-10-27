import {Injectable, NgZone} from '@angular/core';
import {TelemetryWebsocketService} from '@core/ws/telemetry-websocket.service';
import {ProductRealtimeService} from '@shared/realtime/product-realtime.service';
import {ELECTRICAL_SAFETY_REALTIME_TELEMETRY_KEYS} from '../monitoring/electrical-safety-monitoring.data';
export type {DeviceTelemetryChange, AlarmRealtimeChange} from '@shared/realtime/product-realtime.service';

/** 电气安全模块独立的设备、告警和测点订阅范围，公共层负责连接与原生查询。 */
@Injectable({providedIn: 'root'})
export class ElectricalSafetyRealtimeService extends ProductRealtimeService {
  protected override readonly productCategory = 'electrical_safety';
  protected override readonly telemetryKeys = ELECTRICAL_SAFETY_REALTIME_TELEMETRY_KEYS;

  constructor(websocket: TelemetryWebsocketService, zone: NgZone) {
    super(websocket, zone);
  }
}
