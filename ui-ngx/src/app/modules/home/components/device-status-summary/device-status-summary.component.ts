import { ChangeDetectionStrategy, ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { distinctUntilChanged, filter, map, retry, Subject, takeUntil } from 'rxjs';
import { AppState } from '@core/core.state';
import { selectAuth } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { AuthUser } from '@shared/models/user.model';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AlarmCountCmd, EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityKeyType } from '@shared/models/query/query.models';
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
  private activeStatusSubscriber?: TelemetrySubscriber;
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
      // authUser 非空不代表登录初始化已经完成。必须等待 isUserLoaded，
      // 并持续观察最终用户状态；令牌刷新期间的临时权限不能让卡片永久隐藏。
      select(selectAuth),
      filter((authState) => authState.isUserLoaded),
      map((authState) => authState.authUser),
      distinctUntilChanged((previousUser, currentUser) =>
        previousUser?.userId === currentUser?.userId && previousUser?.authority === currentUser?.authority),
      takeUntil(this.destroy$)
    ).subscribe((authState) => {
      const authUser = authState as AuthUser | null;
      this.authUser = authUser;
      this.visible = !!authUser && (authUser.authority === Authority.TENANT_ADMIN || authUser.authority === Authority.CUSTOMER_USER);
      if (this.visible) {
        this.loadSummary();
        if (!this.alarmCountSubscriber) {
          this.subscribeToAlarmChanges();
        }
        if (!this.activeStatusSubscriber) {
          this.subscribeToActiveStatusChanges();
        }
      } else {
        this.stopWebsocketSubscriptions();
      }
      this.cd.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopWebsocketSubscriptions();
  }

  private stopWebsocketSubscriptions(): void {
    this.alarmCountSubscriber?.unsubscribe();
    this.alarmCountSubscriber = undefined;
    this.activeStatusSubscriber?.unsubscribe();
    this.activeStatusSubscriber = undefined;
    this.initialAlarmCountReceived = false;
  }

  private loadSummary(): void {
    if (!this.authUser) {
      return;
    }
    this.loading = true;
    this.loadError = false;
    this.monitoringApi.getDeviceStatusSummary().pipe(
      // 后端刚启动、令牌刚刷新等短暂失败不要求用户手动刷新整个页面。
      retry({count: 2, delay: 500}),
      takeUntil(this.destroy$)
    ).subscribe({
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

  /**
   * 订阅所有可见设备的 active 最新遥测变化。
   * ENTITY_DATA 订阅只用来触发重新聚合，具体正常/异常数量仍由后端统一计算。
   */
  private subscribeToActiveStatusChanges(): void {
    const activeKey = {key: 'active', type: EntityKeyType.TIME_SERIES};
    const command = new EntityDataCmd();
    command.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      pageLink: {pageSize: 10_000, page: 0},
      latestValues: [activeKey]
    };
    command.latestCmd = {keys: [activeKey]};

    this.activeStatusSubscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    this.activeStatusSubscriber.subscriptionCommands.push(command);
    this.activeStatusSubscriber.entityData$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.loadSummary();
    });
    this.activeStatusSubscriber.subscribe();
  }
}
