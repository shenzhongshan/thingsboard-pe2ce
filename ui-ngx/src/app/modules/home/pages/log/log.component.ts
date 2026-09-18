/**
 * 文件职责：设备日志列表页面。
 *
 * <p>列表只显示设备名称、设备配置类型和操作按钮。属性、最新遥测和时间轴在用户打开
 * 右侧抽屉后按需加载，避免页面初始化时为所有设备发起时序请求。</p>
 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { LogApiService } from './log-api.service';
import { LogDevice } from './log.models';

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

  constructor(private readonly logApi: LogApiService, private readonly cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadDevices();
  }

  loadDevices(): void {
    this.loading = true;
    this.loadError = false;
    this.logApi.getDevices(this.pageSize, this.page, this.search).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => {
        this.devices = pageData.data || [];
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

  closeDrawer(): void {
    this.selectedDevice = undefined;
    this.cd.markForCheck();
  }

  trackByDeviceId(_index: number, device: LogDevice): string {
    return device.deviceId;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
