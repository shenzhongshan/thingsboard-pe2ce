import {Injectable, OnDestroy} from '@angular/core';
import {BehaviorSubject, Subject, Subscription, auditTime, merge, forkJoin, filter} from 'rxjs';
import {EnergyRegulationApiService, EnergyRegulationAssetTreeNode, EnergyRegulationDeviceContext} from '../energy-regulation-api.service';
import {EnergyRegulationRealtimeService} from './energy-regulation-realtime.service';
import {AlarmInfo} from '@shared/models/alarm.models';

/** 能源页面只依赖本模块适配层；底层WebSocket为全系统公共连接，不复制连接与订阅。 */
export interface EnergyLiveSnapshot {
  contexts: EnergyRegulationDeviceContext[]; roots: EnergyRegulationAssetTreeNode[];
  alarms: AlarmInfo[]; loading: boolean; error: boolean;
}
@Injectable({providedIn: 'root'})
export class EnergyRegulationLiveService implements OnDestroy {
  private consumers = 0;
  private subscriptions = new Subscription();
  private request = 0;
  private readonly subject = new BehaviorSubject<EnergyLiveSnapshot>({contexts: [], roots: [], alarms: [], loading: true, error: false});
  readonly snapshot$ = this.subject.asObservable();
  readonly telemetryChanged$ = new Subject<void>();
  constructor(private readonly api: EnergyRegulationApiService, private readonly realtime: EnergyRegulationRealtimeService) {}
  /** 首个能源页面建立监听，后续页面复用；离开最后一个页面后释放。 */
  acquire(): void {
    if (this.consumers++ > 0) { return; }
    this.subscriptions = new Subscription();
    // 新一轮页面生命周期先清空旧上下文，避免跨账号或重新进入时展示上一次权限范围。
    this.subject.next({contexts: [], roots: [], alarms: [], loading: true, error: false});
    this.realtime.acquire();
    this.reload();
    this.subscriptions.add(merge(this.realtime.deviceMembershipChanged$, this.realtime.deviceContextChanged$,
      this.realtime.assetMembershipChanged$, this.realtime.alarmChanged$).pipe(auditTime(600)).subscribe(() => this.reload()));
    this.subscriptions.add(merge(
      this.realtime.deviceTelemetryChanged$.pipe(filter((change) => this.subject.value.contexts.some((device) => device.deviceId === change.deviceId))),
      this.realtime.entitySnapshotReady$.pipe(filter((ready) => ready)))
      .pipe(auditTime(200)).subscribe(() => this.telemetryChanged$.next()));
  }
  /** 读取共享缓存中的规范遥测值，不另建单设备连接。 */
  latest(deviceId: string) { return this.realtime.getLatestTelemetryValues(deviceId); }
  /** 模块快照的设备、资产与完整告警具有统一权限口径，失败时清空，避免伪装成功。 */
  private reload(): void {
    const id = ++this.request;
    this.subscriptions.add(forkJoin({contexts: this.api.getDeviceContexts(), tree: this.api.getAssetTree(), alarms: this.api.getAllAlarms()})
      .subscribe({next: ({contexts, tree, alarms}) => {
        if (id === this.request) { this.subject.next({contexts, roots: tree.roots, alarms, loading: false, error: false}); }
      }, error: () => {
        if (id === this.request) { this.subject.next({contexts: [], roots: [], alarms: [], loading: false, error: true}); }
      }}));
  }
  release(): void {
    if (this.consumers === 0 || --this.consumers > 0) { return; }
    this.request++;
    this.subscriptions.unsubscribe();
    this.realtime.release();
  }
  ngOnDestroy(): void { if (this.consumers) { this.consumers = 1; this.release(); } }
}
