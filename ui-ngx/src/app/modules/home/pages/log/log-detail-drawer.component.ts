/**
 * 文件职责：设备日志右侧详情抽屉。
 *
 * <p>抽屉分为服务器属性、最新遥测和时间线三个自定义页签。数据按设备按需读取，
 * 关闭抽屉后销毁当前订阅，避免多个设备同时占用时序请求。</p>
 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Injector, Input, NgZone, OnChanges, OnDestroy, Output, SimpleChanges, StaticProvider, ViewContainerRef } from '@angular/core';
import { ComponentPortal } from '@angular/cdk/portal';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { PageEvent } from '@angular/material/paginator';
import { Store } from '@ngrx/store';
import { DialogService } from '@core/services/dialog.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DeviceActiveStatusService } from '@core/services/device-active-status.service';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeData, LatestTelemetry, SharedTelemetrySubscriber, TimeseriesDeleteStrategy, TimeseriesData } from '@shared/models/telemetry/telemetry.models';
import { Subject, Subscription, catchError, forkJoin, map, of, switchMap, take, takeUntil } from 'rxjs';
import {
  DeleteTimeseriesPanelComponent,
  DeleteTimeseriesPanelResult
} from '@home/components/attribute/delete-timeseries-panel.component';
import { LogApiService } from './log-api.service';
import { LogDevice, LogDeviceSnapshot, LogTimelineItem, LogTimelinePage } from './log.models';
import { copyTextToClipboard } from './log-clipboard.util';
import { buildLogTelemetryDeleteOptions } from './log-telemetry-delete.util';

@Component({
  selector: 'tb-device-log-detail-drawer',
  standalone: false,
  templateUrl: './log-detail-drawer.component.html',
  styleUrls: ['./log-detail-drawer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LogDetailDrawerComponent implements OnChanges, OnDestroy {

  @Input() device: LogDevice;
  @Output() close = new EventEmitter<void>();

  activeTab: 'attributes' | 'telemetry' | 'timeline' = 'attributes';
  snapshot: LogDeviceSnapshot = {attributes: [], telemetry: {}, timeline: []};
  loading = false;
  loadError = false;
  loadingMoreTimeline = false;
  timelineHasNext = false;
  timelineCursor: string | null = null;
  telemetryPageIndex = 0;
  telemetryPageSize = 10;
  readonly telemetryPageSizeOptions = [10, 20, 50];
  deletingTelemetryKey: string | null = null;
  editingAttributeKey: string | null = null;
  editingAttributeValue: unknown = null;
  attributeSavingKey: string | null = null;
  attributeEditError = '';

  private readonly destroy$ = new Subject<void>();
  private readonly reload$ = new Subject<void>();
  private telemetrySubscriber?: SharedTelemetrySubscriber;
  private telemetrySubscription?: Subscription;
  private realtimeTelemetryReceived = false;
  private telemetryDeleteOverlay?: OverlayRef;

  constructor(private readonly logApi: LogApiService,
              private readonly telemetryWsService: TelemetryWebsocketService,
              private readonly activeStatusService: DeviceActiveStatusService,
              private readonly zone: NgZone,
              private readonly cd: ChangeDetectorRef,
              private readonly store: Store<AppState>,
              private readonly overlay: Overlay,
              private readonly viewContainerRef: ViewContainerRef,
              private readonly dialogService: DialogService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['device']?.currentValue) {
      this.cancelAttributeEdit();
      this.loadSnapshot();
    }
  }

  private loadSnapshot(): void {
    this.reload$.next();
    this.closeTelemetryDeletePanel();
    this.deletingTelemetryKey = null;
    this.stopTelemetrySubscription();
    this.realtimeTelemetryReceived = false;
    this.telemetryPageIndex = 0;
    this.loading = true;
    this.loadError = false;
    const entityId = {entityType: EntityType.DEVICE, id: this.device.deviceId};
    this.subscribeToLatestTelemetry(entityId);
    forkJoin({
      attributes: this.logApi.getServerAttributes(entityId),
      telemetry: this.logApi.getLatestTelemetry(entityId),
      timeline: this.logApi.getRecentTimeline(this.device.deviceId)
    }).pipe(takeUntil(this.destroy$), takeUntil(this.reload$)).subscribe({
      next: ({attributes, telemetry, timeline}) => {
        const latestTelemetry = this.realtimeTelemetryReceived ? this.snapshot.telemetry : (telemetry || {});
        this.updateSharedActiveStatus(latestTelemetry);
        this.normalizeTelemetryPage(Object.keys(latestTelemetry).length);
        this.snapshot = {
          attributes: attributes || [],
          telemetry: latestTelemetry,
          timeline: timeline?.data || []
        };
        this.timelineHasNext = timeline?.hasNext || false;
        this.timelineCursor = timeline?.nextCursor || null;
        this.loading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.snapshot = {attributes: [], telemetry: {}, timeline: []};
        this.timelineHasNext = false;
        this.timelineCursor = null;
        this.loading = false;
        this.loadError = true;
        this.cd.markForCheck();
      }
    });
  }

  /**
   * 订阅设备全部最新遥测键。
   *
   * <p>这里故意不传 keys 参数，沿用 ThingsBoard 原生设备详情页的订阅方式，
   * 避免只订阅固定键导致新增遥测字段无法在日志抽屉中实时出现。</p>
   */
  private subscribeToLatestTelemetry(entityId: {entityType: EntityType; id: string}): void {
    this.telemetrySubscriber = SharedTelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService,
      entityId,
      LatestTelemetry.LATEST_TELEMETRY,
      this.zone
    );
    this.telemetrySubscriber.subscribe();
    this.telemetrySubscription = this.telemetrySubscriber.attributeData$
      .pipe(takeUntil(this.destroy$), takeUntil(this.reload$))
      .subscribe((data: AttributeData[]) => {
        this.realtimeTelemetryReceived = true;
        const telemetry = this.toTimeseriesData(data);
        this.updateSharedActiveStatus(telemetry);
        this.normalizeTelemetryPage(Object.keys(telemetry).length);
        this.snapshot = {
          ...this.snapshot,
          telemetry
        };
        this.cd.markForCheck();
      });
  }

  /** 将原生订阅返回的最新值列表转换为日志模板使用的 TimeseriesData 结构。 */
  private toTimeseriesData(data: AttributeData[]): TimeseriesData {
    return (data || []).reduce<TimeseriesData>((result, item) => {
      result[item.key] = [{
        ts: item.lastUpdateTs || 0,
        value: item.value === null || item.value === undefined ? '' : String(item.value)
      }];
      return result;
    }, {});
  }

  /** 将抽屉已有的 active 最新值同步给列表行，不为当前设备另建遥测订阅。 */
  private updateSharedActiveStatus(telemetry: TimeseriesData): void {
    const activeTelemetry = telemetry['active']?.[0];
    if (activeTelemetry) {
      this.activeStatusService.setLatestActiveStatus(
        this.device.deviceId,
        activeTelemetry.value,
        activeTelemetry.ts
      );
    }
  }

  /** 当遥测键数量变化时，确保当前页仍然落在有效范围内。 */
  private normalizeTelemetryPage(total: number): void {
    const pageCount = Math.max(Math.ceil(total / this.telemetryPageSize), 1);
    this.telemetryPageIndex = Math.min(this.telemetryPageIndex, pageCount - 1);
  }

  /** 停止当前设备的实时遥测订阅，防止切换设备后旧设备继续占用 WebSocket。 */
  private stopTelemetrySubscription(): void {
    this.telemetrySubscription?.unsubscribe();
    this.telemetrySubscription = undefined;
    this.telemetrySubscriber?.unsubscribe();
    this.telemetrySubscriber = undefined;
  }


  /** 加载更早的历史事件，只有用户点击按钮时才发起请求。 */
  loadMoreTimeline(): void {
    if (!this.device || !this.timelineHasNext || this.loadingMoreTimeline || !this.timelineCursor) {
      return;
    }
    this.loadingMoreTimeline = true;
    this.logApi.getHistoryTimeline(this.device.deviceId, this.timelineCursor)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: LogTimelinePage) => {
          const timeline = [...this.snapshot.timeline, ...(page.data || [])]
            .sort((left, right) => right.timestamp - left.timestamp);
          this.snapshot = {...this.snapshot, timeline};
          this.timelineHasNext = page.hasNext;
          this.timelineCursor = page.nextCursor;
          this.loadingMoreTimeline = false;
          this.cd.markForCheck();
        },
        error: () => {
          this.loadingMoreTimeline = false;
          this.cd.markForCheck();
        }
      });
  }

  formatTime(timestamp: number): string {
    return timestamp ? new Date(timestamp).toLocaleString('zh-CN', {hour12: false}) : '暂无时间';
  }

  /** 将属性值格式化为可复制、可读的文本；对象值不再显示为 [object Object]。 */
  formatValue(value: unknown): string {
    if (typeof value === 'string') {
      return value;
    }
    try {
      const serialized = JSON.stringify(value);
      return serialized === undefined ? String(value ?? '') : serialized;
    } catch {
      return String(value ?? '');
    }
  }

  /** 复制当前设备 ID，复制目标与原生设备详情页保持一致。 */
  copyDeviceId(): void {
    copyTextToClipboard(this.device.deviceId).then((copied) => this.notifyCopyResult(copied, '设备 ID'));
  }

  /** 进入服务器属性值编辑状态；属性键保持只读，避免误改平台索引键。 */
  startAttributeEdit(attribute: AttributeData): void {
    this.editingAttributeKey = attribute.key;
    // tb-value-input 会根据原始值自动选择类型，并允许用户在编辑时切换类型。
    this.editingAttributeValue = attribute.value;
    this.attributeEditError = '';
  }

  /** 取消当前属性编辑，不向后端发起请求。 */
  cancelAttributeEdit(): void {
    this.editingAttributeKey = null;
    this.editingAttributeValue = null;
    this.attributeSavingKey = null;
    this.attributeEditError = '';
  }

  /**
   * 保存服务器属性值。
   *
   * <p>字符串属性按原文保存；数字、布尔值和 JSON 对象按 JSON 解析，尽量保持原始类型，
   * 避免通过普通输入框编辑后把数值意外变成字符串。</p>
   */
  saveAttribute(attribute: AttributeData): void {
    if (this.editingAttributeKey !== attribute.key || this.attributeSavingKey) {
      return;
    }
    if (this.editingAttributeValue === null || this.editingAttributeValue === undefined) {
      this.attributeEditError = '属性值不能为空';
      this.cd.markForCheck();
      return;
    }
    this.attributeSavingKey = attribute.key;
    this.attributeEditError = '';
    const entityId = {entityType: EntityType.DEVICE, id: this.device.deviceId};
    this.logApi.updateServerAttribute(entityId, {key: attribute.key, value: this.editingAttributeValue})
      .pipe(
        switchMap(() => this.logApi.getServerAttributes(entityId)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (attributes) => {
          this.snapshot = {...this.snapshot, attributes: attributes || []};
          this.cancelAttributeEdit();
          this.cd.markForCheck();
        },
        error: () => {
          this.attributeSavingKey = null;
          this.attributeEditError = '属性保存失败，请稍后重试';
          this.cd.markForCheck();
        }
      });
  }

  /** 单独复制服务器属性键名，方便粘贴到查询条件或规则链配置中。 */
  copyAttributeKey(attribute: AttributeData): void {
    copyTextToClipboard(attribute.key)
      .then((copied) => this.notifyCopyResult(copied, '属性键名'));
  }

  /** 单独复制服务器属性值，复杂值按 JSON 文本输出。 */
  copyAttributeValue(attribute: AttributeData): void {
    copyTextToClipboard(this.formatValue(attribute.value))
      .then((copied) => this.notifyCopyResult(copied, '属性值'));
  }

  /** 使用平台统一通知条反馈复制结果。 */
  private notifyCopyResult(copied: boolean, target: string): void {
    this.store.dispatch(new ActionNotificationShow({
      message: copied ? `${target}已复制` : `${target}复制失败`,
      type: copied ? 'success' : 'error',
      duration: 1200,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  /** 将时间线分类转换成用户可直接理解的来源名称。 */
  formatTimelineSource(category: LogTimelineItem['category']): string {
    return category === '审计事件' ? '审计日志' : category;
  }

  telemetryRows(): Array<{key: string; value: unknown; timestamp: number}> {
    // 先按遥测键名称升序排列，再进行分页，避免不同批次推送导致页面顺序跳动。
    const rows = Object.keys(this.snapshot.telemetry)
      .map(key => ({
        key,
        value: this.snapshot.telemetry[key]?.[0]?.value,
        timestamp: this.snapshot.telemetry[key]?.[0]?.ts || 0
      }))
      .sort((left, right) => left.key.localeCompare(right.key));
    const start = this.telemetryPageIndex * this.telemetryPageSize;
    return rows.slice(start, start + this.telemetryPageSize);
  }

  telemetryTotal(): number {
    return Object.keys(this.snapshot.telemetry).length;
  }

  onTelemetryPage(event: PageEvent): void {
    this.telemetryPageIndex = event.pageIndex;
    this.telemetryPageSize = event.pageSize;
    this.normalizeTelemetryPage(this.telemetryTotal());
    this.cd.markForCheck();
  }

  trackTelemetryKey(_index: number, item: {key: string}): string {
    return item.key;
  }

  /** 打开平台原生删除策略面板，保持四种删除方式及时间范围交互一致。 */
  deleteTelemetry($event: MouseEvent, item: {key: string; value: unknown; timestamp: number}): void {
    $event.stopPropagation();
    if (this.deletingTelemetryKey) {
      return;
    }
    this.closeTelemetryDeletePanel();
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxWidth: 488,
      width: '100%'
    });
    const connectedPosition: ConnectedPosition = {
      originX: 'start',
      originY: 'top',
      overlayX: 'end',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo($event.currentTarget as HTMLElement)
      .withPositions([connectedPosition]);
    const overlayRef = this.overlay.create(config);
    this.telemetryDeleteOverlay = overlayRef;
    overlayRef.backdropClick().pipe(take(1)).subscribe(() => overlayRef.dispose());

    const providers: StaticProvider[] = [{provide: OverlayRef, useValue: overlayRef}];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(
      DeleteTimeseriesPanelComponent,
      this.viewContainerRef,
      injector
    ));
    // 行级删除默认定位到“仅删除最新值”，避免弹窗初始即选中范围最大的历史清理。
    componentRef.instance.deleteTimeseriesFormGroup.patchValue({
      strategy: TimeseriesDeleteStrategy.DELETE_LATEST_VALUE
    });
    componentRef.onDestroy(() => {
      if (this.telemetryDeleteOverlay === overlayRef) {
        this.telemetryDeleteOverlay = undefined;
      }
      const result = componentRef.instance.result;
      if (result) {
        this.confirmTelemetryDelete(item, result, this.device);
      }
    });
  }

  /** 对会清理整段历史的策略再确认一次，避免误删不可恢复的遥测记录。 */
  private confirmTelemetryDelete(item: {key: string; value: unknown; timestamp: number},
                                 result: DeleteTimeseriesPanelResult,
                                 device: LogDevice): void {
    const deleteAllHistory = result.strategy === TimeseriesDeleteStrategy.DELETE_ALL_DATA ||
      result.strategy === TimeseriesDeleteStrategy.DELETE_ALL_DATA_EXCEPT_LATEST_VALUE;
    if (!deleteAllHistory) {
      this.executeTelemetryDelete(item, result, device.deviceId);
      return;
    }
    const scope = result.strategy === TimeseriesDeleteStrategy.DELETE_ALL_DATA ?
      '全部遥测历史（包括最新值）' : '除最新值外的全部遥测历史';
    this.dialogService.confirm(
      '确认删除遥测数据',
      '设备“' + device.deviceName + '”的键“' + item.key + '”将删除' + scope +
        '，该操作不可恢复。是否继续？',
      '取消',
      '删除'
    ).pipe(take(1)).subscribe(confirmed => {
      if (confirmed) {
        this.executeTelemetryDelete(item, result, device.deviceId);
      }
    });
  }

  /** 执行已选的单键删除策略；成功后重新读取服务端最新遥测快照。 */
  private executeTelemetryDelete(item: {key: string; value: unknown; timestamp: number},
                                 result: DeleteTimeseriesPanelResult,
                                 deviceId: string): void {
    const options = buildLogTelemetryDeleteOptions(result, item.timestamp);
    if (!options) {
      this.notifyTelemetryDelete('删除范围或最新值时间无效，未执行删除', 'error');
      return;
    }
    const entityId = {entityType: EntityType.DEVICE, id: deviceId};
    this.deletingTelemetryKey = item.key;
    this.cd.markForCheck();
    this.logApi.deleteTelemetryKey(entityId, item.key, options).pipe(
      switchMap(() => this.logApi.getLatestTelemetry(entityId).pipe(
        map(telemetry => ({telemetry, refreshFailed: false})),
        catchError(() => of({telemetry: null, refreshFailed: true}))
      )),
      takeUntil(this.destroy$),
      takeUntil(this.reload$)
    ).subscribe({
      next: ({telemetry, refreshFailed}) => {
        if (telemetry !== null) {
          this.snapshot = {...this.snapshot, telemetry: telemetry || {}};
          this.normalizeTelemetryPage(this.telemetryTotal());
        }
        this.deletingTelemetryKey = null;
        this.notifyTelemetryDelete(
          refreshFailed ? '遥测数据已删除，但最新值刷新失败，请重新打开详情' : '遥测键 ' + item.key + ' 已删除',
          'success'
        );
        this.cd.markForCheck();
      },
      error: () => {
        this.deletingTelemetryKey = null;
        this.notifyTelemetryDelete('遥测键 ' + item.key + ' 删除失败，请检查权限或稍后重试', 'error');
        this.cd.markForCheck();
      }
    });
  }

  /** 关闭可能仍处于打开状态的策略面板，避免切换设备或关闭抽屉后遗留浮层。 */
  private closeTelemetryDeletePanel(): void {
    this.telemetryDeleteOverlay?.dispose();
    this.telemetryDeleteOverlay = undefined;
  }

  /** 使用平台统一通知条反馈遥测删除结果。 */
  private notifyTelemetryDelete(message: string, type: 'success' | 'error'): void {
    this.store.dispatch(new ActionNotificationShow({
      message,
      type,
      duration: 2500,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  ngOnDestroy(): void {
    this.closeTelemetryDeletePanel();
    this.stopTelemetrySubscription();
    this.reload$.next();
    this.reload$.complete();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
