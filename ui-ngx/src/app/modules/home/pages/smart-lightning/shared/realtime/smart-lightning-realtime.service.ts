import {Injectable, NgZone} from '@angular/core';
import {TelemetryWebsocketService} from '@core/ws/telemetry-websocket.service';
import {ProductRealtimeService} from '@shared/realtime/product-realtime.service';
import {SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS} from './smart-lightning-realtime.keys';

export {SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS} from './smart-lightning-realtime.keys';
export type {DeviceTelemetryChange as SmartLightningDeviceTelemetryChange,
  AlarmRealtimeChange as SmartLightningAlarmRealtimeChange} from '@shared/realtime/product-realtime.service';

/** 防雷页面共享的分类订阅；仅释放自己的命令，底层连接由平台公共WebSocket服务管理。 */
@Injectable({providedIn: 'root'})
export class SmartLightningRealtimeService extends ProductRealtimeService {
  protected override readonly productCategory = 'lightning';
  protected override readonly telemetryKeys = SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS;

  constructor(websocket: TelemetryWebsocketService, zone: NgZone) {
    super(websocket, zone);
  }
}
