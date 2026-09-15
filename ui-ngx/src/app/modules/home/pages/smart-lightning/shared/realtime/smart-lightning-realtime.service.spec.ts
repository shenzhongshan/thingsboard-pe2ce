import {NgZone} from '@angular/core';
import {TelemetryWebsocketService} from '@core/ws/telemetry-websocket.service';
import {AlarmDataCmd, TelemetrySubscriber} from '@shared/models/telemetry/telemetry.models';
import {SmartLightningRealtimeService, SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS} from './smart-lightning-realtime.service';

describe('SmartLightningRealtimeService', () => {
  let service: SmartLightningRealtimeService;
  let websocketService: jasmine.SpyObj<TelemetryWebsocketService>;

  beforeEach(() => {
    websocketService = jasmine.createSpyObj<TelemetryWebsocketService>('TelemetryWebsocketService', [
      'subscribe', 'update', 'unsubscribe'
    ]);
    service = new SmartLightningRealtimeService(
      websocketService,
      new NgZone({enableLongStackTrace: false})
    );
  });

  it('should create one shared subscription for multiple consumers', () => {
    service.acquire();
    service.acquire();

    expect(websocketService.subscribe).toHaveBeenCalledTimes(1);

    service.release();
    expect(websocketService.unsubscribe).not.toHaveBeenCalled();
    service.release();
    expect(websocketService.unsubscribe).toHaveBeenCalledTimes(1);
  });

  it('should expose the active attribute and normalized realtime keys', () => {
    expect(SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS).toContain('spdStatus');
    expect(SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS).toContain('surgeCount');
    expect(SMART_LIGHTNING_REALTIME_TELEMETRY_KEYS).not.toContain('active');
  });

  it('should send non-null alarm field collections required by the server query repository', () => {
    service.acquire();

    const subscriber = (service as unknown as {subscriber: TelemetrySubscriber}).subscriber;
    const alarmDataCommand = subscriber.subscriptionCommands.find(
      (command) => command instanceof AlarmDataCmd
    ) as AlarmDataCmd;

    expect(alarmDataCommand.query?.alarmFields).toEqual([]);
    expect(alarmDataCommand.query?.entityFields).toEqual([]);
    expect(alarmDataCommand.query?.latestValues).toEqual([]);
  });
});
