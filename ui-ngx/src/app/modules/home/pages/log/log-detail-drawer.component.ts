/**
 * 文件职责：设备日志右侧详情抽屉。
 *
 * <p>抽屉分为服务器属性、最新遥测和时间线三个自定义页签。数据按设备按需读取，
 * 关闭抽屉后销毁当前订阅，避免多个设备同时占用时序请求。</p>
 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { Subject, forkJoin, takeUntil } from 'rxjs';
import { LogApiService } from './log-api.service';
import { LogDevice, LogDeviceSnapshot, LogTimelineItem, LogTimelinePage } from './log.models';

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

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly logApi: LogApiService, private readonly cd: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['device']?.currentValue) {
      this.loadSnapshot();
    }
  }

  private loadSnapshot(): void {
    this.loading = true;
    this.loadError = false;
    const entityId = {entityType: EntityType.DEVICE, id: this.device.deviceId};
    forkJoin({
      attributes: this.logApi.getServerAttributes(entityId),
      telemetry: this.logApi.getLatestTelemetry(entityId),
      timeline: this.logApi.getRecentTimeline(this.device.deviceId)
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({attributes, telemetry, timeline}) => {
        this.snapshot = {attributes: attributes || [], telemetry: telemetry || {}, timeline: timeline?.data || []};
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

  /** 将时间线分类转换成用户可直接理解的来源名称。 */
  formatTimelineSource(category: LogTimelineItem['category']): string {
    return category === '审计事件' ? '审计日志' : category;
  }

  telemetryRows(): Array<{key: string; value: unknown; timestamp: number}> {
    return Object.keys(this.snapshot.telemetry).map(key => ({
      key,
      value: this.snapshot.telemetry[key]?.[0]?.value,
      timestamp: this.snapshot.telemetry[key]?.[0]?.ts || 0
    }));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
