import { ChangeDetectionStrategy, ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { filter, Subject, take, takeUntil } from 'rxjs';
import { AppState } from '@core/core.state';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { AuthUser } from '@shared/models/user.model';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AlarmCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { SmartLightningDeviceStatusSummary, SmartLightningMonitoringApiService } from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring-api.service';

@Component({
  selector: 'tb-device-status-summary',
  templateUrl: './device-status-summary.component.html',
  styleUrls: ['./device-status-summary.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class DeviceStatusSummaryComponent implements OnInit, OnDestroy {

  summary: SmartLightningDeviceStatusSummary | null = null;
  loading = true;
  loadError = false;
  visible = false;

  private readonly destroy$ = new Subject<void>();
  private alarmCountSubscriber?: TelemetrySubscriber;
  private initialAlarmCountReceived = false;
  private authUser?: AuthUser;

  constructor(
    private readonly store: Store<AppState>,
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly telemetryWebsocketService: TelemetryWebsocketService,
    private readonly ngZone: NgZone,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.store.pipe(
      select(selectAuthUser),
      filter((authUser): authUser is AuthUser => !!authUser),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe((authUser) => {
      this.authUser = authUser;
      this.visible = authUser.authority === Authority.TENANT_ADMIN || authUser.authority === Authority.CUSTOMER_USER;
      if (this.visible) {
        this.loadSummary();
        this.subscribeToAlarmChanges();
      }
      this.cd.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.alarmCountSubscriber?.unsubscribe();
  }

  private loadSummary(): void {
    if (!this.authUser) {
      return;
    }
    this.loading = true;
    this.loadError = false;
    this.monitoringApi.getDeviceStatusSummary().pipe(takeUntil(this.destroy$)).subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.summary = null;
        this.loading = false;
        this.loadError = true;
        this.cd.markForCheck();
      }
    });
  }

  private subscribeToAlarmChanges(): void {
    const command = new AlarmCountCmd();
    command.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      statusList: [AlarmSearchStatus.ACTIVE]
    };
    this.alarmCountSubscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    this.alarmCountSubscriber.subscriptionCommands.push(command);
    this.alarmCountSubscriber.alarmCount$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.initialAlarmCountReceived) {
        this.loadSummary();
      }
      this.initialAlarmCountReceived = true;
    });
    this.alarmCountSubscriber.subscribe();
  }
}
