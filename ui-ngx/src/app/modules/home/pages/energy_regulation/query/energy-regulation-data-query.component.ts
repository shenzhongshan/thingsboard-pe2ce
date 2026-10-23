/** 文件职责：用能数据查询页面组件，负责能源资产范围、设备筛选、表格操作和信息预览。 */
import {CommonModule} from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {SharedModule} from '@app/shared/shared.module';
import {EntityType} from '@shared/models/entity-type.models';
import {SmartLightningRootAssetFilterComponent} from '@home/pages/smart-lightning/smart-lightning-root-asset-filter.component';
import {SmartLightningAssetTreeModule} from '@home/pages/smart-lightning/shared/asset-tree/smart-lightning-asset-tree.module';
import {Subject, takeUntil} from 'rxjs';
import {EnergyRegulationLiveService, EnergyLiveSnapshot} from '../shared/energy-regulation-live.service';
import {ENERGY_METER_MEASUREMENTS} from '../shared/energy-regulation-measurements';
import {EnergyRegulationHistoryApiService} from './energy-regulation-history-api.service';
import {EnergyRegulationApiService} from '../energy-regulation-api.service';
import {
  buildEnergyMonitoringAssetTree,
  EnergyMonitoringAssetTree,
  getEnergyMonitoringSelectedAssetIds
} from '../monitoring/energy-regulation-monitoring.data';
import {
  buildEnergyQueryCsv,
  buildEnergyQueryDeviceCsv,
  buildEnergyQueryDeviceRows,
  buildEnergyQueryDevices,
  EnergyQueryDevice,
  EnergyQueryDeviceRow,
  EnergyQueryStatus,
  filterEnergyQueryDevices,
  getEnergyQueryDeviceTypes,
  getEnergyQueryFunctionCategories
} from './energy-regulation-data-query.data';
import {EnergyAssetNode} from '../monitoring/energy-regulation-monitoring.data';

interface QueryDraft {
  deviceType: string;
  functionCategory: string;
  keyword: string;
  startTime: string;
  endTime: string;
}

/**
 * 用能数据查询独立页面：左侧能源资产树联动筛选，右侧提供设备查询和结果操作。
 * 设备与资产使用能源监管接口；历史与最新遥测通过模块内服务封装平台公共接口。
 */
@Component({
  selector: 'tb-energy-regulation-data-query',
  templateUrl: './energy-regulation-data-query.component.html',
  styleUrls: ['./energy-regulation-data-query.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule, SmartLightningRootAssetFilterComponent, SmartLightningAssetTreeModule]
})
export class EnergyRegulationDataQueryComponent implements OnInit, OnDestroy {

  @ViewChild('reviewCloseButton') private reviewCloseButton?: ElementRef<HTMLButtonElement>;

  assetTree: EnergyMonitoringAssetTree = buildEnergyMonitoringAssetTree([]);
  assetsLoading = true;
  assetsLoadError = false;
  selectedRootAssetId = '';
  selectedAssetId = '';
  selectedAssetName = '全部资产';
  selectedAssetIds = new Set<string>();
  expandedAssetIds = new Set<string>();
  assetSearch = '';

  /** 筛选区草稿值；点击「查询」后才应用到表格（与原型图的查询/重置语义一致）。 */
  draft: QueryDraft = {deviceType: '', functionCategory: '', keyword: '', startTime: '', endTime: ''};
  applied: QueryDraft = {...this.draft};

  activeStatus: '全部' | EnergyQueryStatus = '全部';
  readonly statuses: Array<'全部' | EnergyQueryStatus> = ['全部', '在线', '离线', '未连接'];

  devices: EnergyQueryDevice[] = [];
  selectedRowIds = new Set<string>();
  page = 1;
  readonly pageSize = 20;

  /** 当前预览的设备及预览类型。 */
  reviewDevice: EnergyQueryDevice | null = null;
  reviewMode: '回顾' | '详情' = '回顾';
  private reviewTrigger: HTMLElement | null = null;

  readonly measurements = ENERGY_METER_MEASUREMENTS;
  reviewKey = 'FwdActET';
  reviewLoading = false;
  reviewError = false;
  historyPoints: Array<{time: string; value: string}> = [];
  detailPoints: Array<{label: string; value: string}> = [];
  private liveSnapshot?: EnergyLiveSnapshot;
  private readonly reviewRequest$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly energyApi: EnergyRegulationApiService,
    private readonly live: EnergyRegulationLiveService,
    private readonly historyApi: EnergyRegulationHistoryApiService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.live.acquire();
    this.live.snapshot$.pipe(takeUntil(this.destroy$)).subscribe((snapshot) => {
      this.liveSnapshot = snapshot; this.assetsLoadError = snapshot.error;
      if (!snapshot.loading) { this.initialize(snapshot.roots); }
    });
    this.live.telemetryChanged$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.devices = buildEnergyQueryDevices(this.assetTree, this.liveSnapshot?.contexts || [], (id) => this.live.latest(id));
      if (this.reviewDevice && this.reviewMode === '详情') {
        const latest = this.live.latest(this.reviewDevice.id);
        if (latest.size) {
          this.detailPoints = this.measurements.map((item) => ({label: item.label,
            value: latest.get(item.key)?.value !== undefined ? `${latest.get(item.key)!.value} ${item.unit}` : '__'}));
        }
      }
      this.cd.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.live.release();
    this.reviewRequest$.next(); this.reviewRequest$.complete();
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredDevices(): EnergyQueryDevice[] {
    return filterEnergyQueryDevices(this.devices, {
      status: this.activeStatus,
      deviceType: this.applied.deviceType,
      functionCategory: this.applied.functionCategory,
      keyword: this.applied.keyword,
      startTime: this.applied.startTime,
      endTime: this.applied.endTime,
      assetIds: this.selectedAssetIds
    });
  }

  get rows(): EnergyQueryDeviceRow[] {
    return buildEnergyQueryDeviceRows(this.filteredDevices);
  }

  get pageRows(): EnergyQueryDeviceRow[] {
    return this.rows.slice((this.page - 1) * this.pageSize, this.page * this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.rows.length / this.pageSize));
  }

  get deviceTypes(): string[] {
    return getEnergyQueryDeviceTypes(this.devices);
  }

  get functionCategories(): string[] {
    return getEnergyQueryFunctionCategories(this.devices);
  }

  /** 日期筛选只接受起始日不晚于结束日的范围。 */
  get isDateRangeInvalid(): boolean {
    return Boolean(this.draft.startTime && this.draft.endTime && this.draft.startTime > this.draft.endTime);
  }

  get allPageRowsSelected(): boolean {
    return this.pageRows.length > 0 && this.pageRows.every((row) => this.selectedRowIds.has(row.id));
  }

  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    this.selectedAssetId = rootAssetId;
    this.selectedAssetIds = getEnergyMonitoringSelectedAssetIds(this.assetTree, rootAssetId);
    if (!rootAssetId) {
      for (const device of this.liveSnapshot?.contexts || []) { this.selectedAssetIds.add(device.deviceId); }
    }
    const root = this.assetTree.roots.find((node) => node.id === rootAssetId);
    this.selectedAssetName = root?.name || '全部资产';
    this.resetPage();
  }

  selectAssetNode(node: EnergyAssetNode): void {
    this.selectedAssetId = node.id;
    this.selectedAssetIds = getEnergyMonitoringSelectedAssetIds(this.assetTree, node.id);
    this.selectedAssetName = node.name;
    this.resetPage();
  }

  selectAllAssets(): void {
    this.selectRootAsset(this.selectedRootAssetId);
  }

  toggleAssetNode(node: EnergyAssetNode): void {
    this.expandedAssetIds.has(node.id) ? this.expandedAssetIds.delete(node.id) : this.expandedAssetIds.add(node.id);
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  /** 「查询」按钮：把筛选草稿应用到表格并回到第一页。 */
  search(): void {
    if (this.isDateRangeInvalid) {
      return;
    }
    this.applied = {...this.draft};
    this.resetPage();
  }

  /** 清除名称草稿并立即应用空关键字，保留其他已提交筛选条件。 */
  clearKeyword(): void {
    this.draft = {...this.draft, keyword: ''};
    this.applied = {...this.applied, keyword: ''};
    this.resetPage();
  }

  /** 「重置」按钮：清空草稿与已应用的筛选条件。 */
  reset(): void {
    this.draft = {deviceType: '', functionCategory: '', keyword: '', startTime: '', endTime: ''};
    this.applied = {...this.draft};
    this.activeStatus = '全部';
    this.selectedRowIds = new Set();
    this.resetPage();
  }

  setStatus(status: '全部' | EnergyQueryStatus): void {
    this.activeStatus = status;
    this.resetPage();
  }

  setPage(page: number): void {
    this.page = Math.min(Math.max(1, page), this.totalPages);
  }

  toggleRow(rowId: string): void {
    this.selectedRowIds.has(rowId) ? this.selectedRowIds.delete(rowId) : this.selectedRowIds.add(rowId);
    this.selectedRowIds = new Set(this.selectedRowIds);
  }

  togglePageRows(): void {
    const next = new Set(this.selectedRowIds);
    if (this.allPageRowsSelected) {
      this.pageRows.forEach((row) => next.delete(row.id));
    } else {
      this.pageRows.forEach((row) => next.add(row.id));
    }
    this.selectedRowIds = next;
  }

  /** 打开当前设备的本地信息预览，并将焦点移入对话框。 */
  openReview(row: EnergyQueryDeviceRow, mode: '回顾' | '详情', event: Event): void {
    this.reviewTrigger = event.currentTarget instanceof HTMLElement ? event.currentTarget : null;
    this.reviewDevice = this.devices.find((device) => device.id === row.id) || null;
    this.reviewMode = mode;
    this.loadReview();
    this.cd.detectChanges();
    this.reviewCloseButton?.nativeElement.focus();
  }

  /** 关闭预览后把键盘焦点返回到打开它的设备操作按钮。 */
  closeReview(): void {
    const trigger = this.reviewTrigger;
    this.reviewTrigger = null;
    this.reviewRequest$.next();
    this.reviewDevice = null;
    this.cd.markForCheck();
    if (trigger) {
      requestAnimationFrame(() => trigger.focus());
    }
  }

  /** 对话框仅有关闭操作时，将 Tab 留在关闭按钮并支持 Escape 关闭。 */
  @HostListener('document:keydown', ['$event'])
  handleReviewKeyboard(event: KeyboardEvent): void {
    if (!this.reviewDevice) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeReview();
      return;
    }
    if (event.key === 'Tab') {
      event.preventDefault();
      this.reviewCloseButton?.nativeElement.focus();
    }
  }

  /** 勾选时导出勾选设备，未勾选时导出当前筛选结果（与防雷查询页同一约定）。 */
  exportRows(): void {
    const rows = this.selectedRowIds.size
      ? this.filteredDevices.filter((device) => this.selectedRowIds.has(device.id))
      : this.filteredDevices;
    if (!rows.length) {
      return;
    }
    const blob = buildEnergyQueryCsv(buildEnergyQueryDeviceCsv(rows));
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `用能数据查询-${new Date().getTime()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  /** 回顾只展示实际样本，切换设备或参数时取消旧请求。 */
  loadReview(): void {
    this.reviewRequest$.next();
    this.historyPoints = []; this.detailPoints = []; this.reviewError = false;
    if (!this.reviewDevice) { return; }
    this.reviewLoading = true;
    const id = this.reviewDevice.id;
    const request = this.reviewMode === '回顾' ? this.historyApi.history(id, this.reviewKey)
      : this.historyApi.latest(id, this.measurements.map((item) => item.key));
    request.pipe(takeUntil(this.reviewRequest$), takeUntil(this.destroy$)).subscribe({next: (values) => {
      const definition = this.measurements.find((item) => item.key === this.reviewKey);
      this.historyPoints = (values[this.reviewKey] || []).slice().sort((a, b) => a.ts - b.ts)
        .map((sample) => ({time: new Date(sample.ts).toLocaleString(), value: `${sample.value} ${definition?.unit || ''}`}));
      this.detailPoints = this.measurements.map((item) => ({label: item.label,
        value: values[item.key]?.[0] ? `${values[item.key][0].value} ${item.unit}` : '__'}));
      this.reviewLoading = false; this.cd.markForCheck();
    }, error: () => { this.reviewLoading = false; this.reviewError = true; this.cd.markForCheck(); }});
  }

  reviewSeries(): Array<{time: string; value: string}> { return this.historyPoints; }

  private initialize(roots: unknown[]): void {
    this.assetTree = buildEnergyMonitoringAssetTree(roots as never);
    this.devices = buildEnergyQueryDevices(this.assetTree, this.liveSnapshot?.contexts || [], (id) => this.live.latest(id));
    this.expandedAssetIds = this.assetsLoading ? new Set(this.assetTree.roots.map((root) => root.id))
      : new Set([...this.expandedAssetIds].filter((id) => this.assetTree.nodes.has(id)));
    this.assetsLoading = false;
    if (this.selectedAssetId && this.assetTree.nodes.has(this.selectedAssetId)) {
      this.selectedAssetIds = getEnergyMonitoringSelectedAssetIds(this.assetTree, this.selectedAssetId);
      this.selectedAssetName = this.assetTree.nodes.get(this.selectedAssetId)?.name || '全部资产';
    } else {
      this.selectedAssetIds = new Set(this.devices.map((device) => device.assetId));
      this.selectedAssetId = ''; this.selectedRootAssetId = ''; this.selectedAssetName = '全部资产';
    }
    this.page = Math.min(this.page, this.totalPages);
    this.cd.markForCheck();
  }

  private resetPage(): void {
    this.page = 1;
    this.cd.markForCheck();
  }
}
