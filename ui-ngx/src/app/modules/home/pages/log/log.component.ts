/**
 * 文件职责：设备日志列表页面。
 *
 * <p>列表展示设备名称、设备配置类型、共享 active 实时状态和操作按钮。属性、其他最新遥测
 * 及时间轴在用户打开右侧抽屉后按需加载，避免页面初始化时额外发起时序请求。</p>
 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subject, takeUntil } from 'rxjs';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DeviceActiveStatusChange, DeviceActiveStatusService } from '@core/services/device-active-status.service';
import { LogApiService } from './log-api.service';
import { LogDevice } from './log.models';
import { copyTextToClipboard } from './log-clipboard.util';

@Component({
  selector: 'tb-device-log',
  standalone: false,
  templateUrl: './log.component.html',
  styleUrls: ['./log.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LogComponent implements OnInit, OnDestroy {

  readonly pageSize = 20;
  devices: LogDevice[] = [];
  search = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  loadError = false;
  selectedDevice: LogDevice;

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly logApi: LogApiService,
              private readonly activeStatusService: DeviceActiveStatusService,
              private readonly cd: ChangeDetectorRef,
              private readonly store: Store<AppState>) {}

  ngOnInit(): void {
    this.activeStatusService.activeStatusChanged$.pipe(takeUntil(this.destroy$)).subscribe((change) => {
      this.applyActiveStatusChange(change);
    });
    this.activeStatusService.acquire();
    this.loadDevices();
  }

  loadDevices(): void {
    this.loading = true;
    this.loadError = false;
    this.logApi.getDevices(this.pageSize, this.page, this.search).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => {
        this.devices = (pageData.data || []).map((device) => ({
          ...device,
          active: this.activeStatusService.getLatestActiveStatus(device.deviceId) ?? null
        }));
        this.totalPages = pageData.totalPages || 0;
        this.totalElements = pageData.totalElements || 0;
        this.loading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.devices = [];
        this.totalPages = 0;
        this.totalElements = 0;
        this.loading = false;
        this.loadError = true;
        this.cd.markForCheck();
      }
    });
  }

  submitSearch(): void {
    this.page = 0;
    this.loadDevices();
  }

  resetSearch(): void {
    this.search = '';
    this.submitSearch();
  }

  changePage(page: number): void {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }
    this.page = page;
    this.loadDevices();
  }

  openDevice(device: LogDevice): void {
    this.selectedDevice = device;
    this.cd.markForCheck();
  }

  /** 复制设备 ID，保持与原生设备详情页相同的复制目标和交互语义。 */
  copyDeviceId(device: LogDevice): void {
    copyTextToClipboard(device.deviceId).then((copied) => {
      this.store.dispatch(new ActionNotificationShow({
        message: copied ? '设备 ID 已复制' : '设备 ID 复制失败',
        type: copied ? 'success' : 'error',
        duration: 1200,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
    });
  }

  closeDrawer(): void {
    this.selectedDevice = undefined;
    this.cd.markForCheck();
  }

  trackByDeviceId(_index: number, device: LogDevice): string {
    return device.deviceId;
  }

  /** 只更新共享 active 遥测本帧涉及的当前页设备，保留列表分页与筛选结果。 */
  private applyActiveStatusChange(change: DeviceActiveStatusChange): void {
    if (!change.deviceIds.size && !change.initialSnapshot) {
      return;
    }
    const changedDeviceIds = change.deviceIds;
    this.devices = this.devices.map((device) => {
      if (!change.initialSnapshot && !changedDeviceIds.has(device.deviceId)) {
        return device;
      }
      return {
        ...device,
        active: this.activeStatusService.getLatestActiveStatus(device.deviceId) ?? null
      };
    });
    this.cd.markForCheck();
  }

  ngOnDestroy(): void {
    this.activeStatusService.release();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
