/** 能源数据查询：与防雷、电气安全共用查询交互格式，业务范围只消费能源模块接口。 */
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, RouterModule} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {Subject, takeUntil} from 'rxjs';
import {SharedModule} from '@app/shared/shared.module';
import {EnergyRegulationLiveService, EnergyLiveSnapshot} from '../shared/energy-regulation-live.service';
import {ENERGY_METER_MEASUREMENTS} from '../shared/energy-regulation-measurements';
import {SmartLightningRootAssetFilterComponent} from '../../smart-lightning/smart-lightning-root-asset-filter.component';
import {SmartLightningAssetTreeModule} from '../../smart-lightning/shared/asset-tree/smart-lightning-asset-tree.module';
import {SmartLightningHistoryReviewDialogComponent} from '../../smart-lightning/query/smart-lightning-history-review-dialog.component';
import {SmartLightningHistoryReviewDialogModule} from '../../smart-lightning/query/smart-lightning-history-review-dialog.module';
import {getSmartLightningAlarmSeverityLabel, getSmartLightningAlarmSeverityTone} from '../../deviceAlarms/shared/smart-lightning-alarm-display';
import {buildEnergyMonitoringAssetTree, EnergyAssetNode, getEnergyDefaultExpandedAssetIds,
  getEnergyMonitoringSelectedAssetIds} from '../monitoring/energy-regulation-monitoring.data';
import {buildEnergyQueryCsv, buildEnergyQueryDeviceCsv, buildEnergyQueryDeviceRows, buildEnergyQueryDevices,
  EnergyQueryDevice, EnergyQueryDeviceRow, EnergyQueryStatus, filterEnergyQueryDevices} from './energy-regulation-data-query.data';

@Component({
  selector: 'tb-energy-regulation-data-query',
  templateUrl: './energy-regulation-data-query.component.html',
  styleUrls: ['./energy-regulation-data-query.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, SharedModule, SmartLightningRootAssetFilterComponent,
    SmartLightningAssetTreeModule, SmartLightningHistoryReviewDialogModule]
})
export class EnergyRegulationDataQueryComponent implements OnInit, OnDestroy {
  assetTree = buildEnergyMonitoringAssetTree([]);
  assetsLoading = true;
  assetsLoadError = false;
  devicesLoading = true;
  devicesLoadError = false;
  selectedRootAssetId = '';
  selectedTreeNodeId = '';
  selectedAssetName = '全部根资产';
  selectedAssetIds = new Set<string>();
  expandedAssetIds = new Set<string>();
  activeAlarmDeviceIds = new Set<string>();
  assetSearch = '';
  productNameFilter = '';
  functionType = '';
  keyword = '';
  activeStatus: '全部' | EnergyQueryStatus = '全部';
  readonly statuses: Array<'全部' | EnergyQueryStatus> = ['全部', '报警', '离线', '正常'];
  readonly pageSize = 20;
  page = 1;
  selectedRowIds = new Set<string>();
  private devices: EnergyQueryDevice[] = [];
  private snapshot?: EnergyLiveSnapshot;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly live: EnergyRegulationLiveService, private readonly dialog: MatDialog,
              private readonly route: ActivatedRoute, private readonly cd: ChangeDetectorRef) {}

  /** 保留能源模块自己的实时服务，不重新订阅其他产品分类。 */
  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.selectedRootAssetId = params.get('selectedRootAssetId') || params.get('returnRootAssetId') || '';
    this.selectedTreeNodeId = params.get('selectedAssetId') || params.get('returnAssetId') || '';
    this.live.acquire();
    this.live.snapshot$.pipe(takeUntil(this.destroy$)).subscribe((snapshot) => {
      const initial = this.assetsLoading;
      this.snapshot = snapshot;
      this.assetsLoading = this.devicesLoading = snapshot.loading;
      this.assetsLoadError = this.devicesLoadError = snapshot.error;
      if (!snapshot.loading) {
        this.assetTree = buildEnergyMonitoringAssetTree(snapshot.roots);
        this.expandedAssetIds = initial ? getEnergyDefaultExpandedAssetIds(this.assetTree)
          : new Set([...this.expandedAssetIds].filter((id) => this.assetTree.nodes.has(id)));
        this.restoreAssetScope();
        this.rebuildDevices();
      }
      this.cd.markForCheck();
    });
    this.live.telemetryChanged$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.rebuildDevices(); this.cd.markForCheck();
    });
  }

  /** 产品与功能下拉只来自当前选中资产范围，不能借用范围外设备。 */
  get productNames(): string[] { return [...new Set(this.scopedDevices.map((item) => item.productName))].sort((a, b) => a.localeCompare(b, 'zh-CN')); }
  get functionCategories(): string[] { return [...new Set(this.scopedDevices.map((item) => item.functionCategory))].sort((a, b) => a.localeCompare(b, 'zh-CN')); }
  private get scopedDevices(): EnergyQueryDevice[] { return this.devices.filter((item) => this.selectedAssetIds.has(item.id)); }
  private get filteredDevices(): EnergyQueryDevice[] {
    return filterEnergyQueryDevices(this.devices, {status: this.activeStatus, productName: this.productNameFilter,
      functionCategory: this.functionType, keyword: this.keyword, assetIds: this.selectedAssetIds});
  }
  /** 所有导出、分页和勾选都基于同一份筛选结果。 */
  get filteredRows(): EnergyQueryDeviceRow[] { return buildEnergyQueryDeviceRows(this.filteredDevices); }
  get pageRows(): EnergyQueryDeviceRow[] { return this.filteredRows.slice((this.page - 1) * this.pageSize, this.page * this.pageSize); }
  get totalPages(): number { return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize)); }
  get allPageRowsSelected(): boolean { return this.pageRows.length > 0 && this.pageRows.every((row) => this.selectedRowIds.has(row.id)); }

  /** 切换根资产后恢复到该根的完整子树，过滤条件保留，清空旧行选择。 */
  selectRootAsset(id: string): void {
    this.selectedRootAssetId = id; this.selectedTreeNodeId = id;
    this.restoreAssetScope(); this.resetPage();
  }
  selectAssetNode(node: EnergyAssetNode): void {
    this.selectedTreeNodeId = node.id; this.restoreAssetScope(); this.resetPage();
  }
  selectAllAssets(): void { this.selectRootAsset(this.selectedRootAssetId); }
  toggleAssetNode(node: EnergyAssetNode): void {
    const expanded = new Set(this.expandedAssetIds);
    if (expanded.has(node.id)) { expanded.delete(node.id); } else { expanded.add(node.id); }
    this.expandedAssetIds = expanded;
  }
  /** 重置表单不清除资产范围，与前两个查询模块保持一致。 */
  reset(): void {
    this.productNameFilter = ''; this.functionType = ''; this.keyword = ''; this.activeStatus = '全部'; this.resetPage();
  }
  setStatus(status: '全部' | EnergyQueryStatus): void { this.activeStatus = status; this.resetPage(); }
  resetPage(): void { this.page = 1; this.selectedRowIds = new Set(); this.cd.markForCheck(); }
  setPage(page: number): void { this.page = Math.min(Math.max(1, page), this.totalPages); }
  toggleRow(id: string): void {
    const selected = new Set(this.selectedRowIds);
    if (selected.has(id)) { selected.delete(id); } else { selected.add(id); }
    this.selectedRowIds = selected;
  }
  togglePageRows(): void {
    const selected = new Set(this.selectedRowIds);
    const remove = this.allPageRowsSelected;
    for (const row of this.pageRows) { if (remove) { selected.delete(row.id); } else { selected.add(row.id); } }
    this.selectedRowIds = selected;
  }

  /** 复用通用回顾弹窗，显式传入能源34个规范测点，避免回退到防雷型号映射。 */
  openHistoryReview(row: EnergyQueryDeviceRow): void {
    this.dialog.open(SmartLightningHistoryReviewDialogComponent, {
      data: {deviceId: row.id, deviceName: row.deviceName, deviceProfileName: row.deviceType,
        metricDefinitions: ENERGY_METER_MEASUREMENTS.map((item) => ({key: item.key, label: item.label, unit: item.unit,
          valueType: item.status ? 'status' : 'telemetry', chartable: !item.status}))},
      width: '980px', maxWidth: '96vw', maxHeight: '92vh', autoFocus: 'first-tabbable', restoreFocus: true
    });
  }

  /** 勾选时导出当前筛选内的勾选设备，否则导出全部筛选结果。 */
  exportRows(): void {
    const rows = this.selectedRowIds.size ? this.filteredDevices.filter((item) => this.selectedRowIds.has(item.id)) : this.filteredDevices;
    if (!rows.length) { return; }
    const url = URL.createObjectURL(buildEnergyQueryCsv(buildEnergyQueryDeviceCsv(rows)));
    const link = document.createElement('a'); link.href = url; link.download = `用能数据查询-${Date.now()}.csv`; link.click(); URL.revokeObjectURL(url);
  }

  /** 根资产变化或权限刷新后校验旧选择；全部根资产包含可见的未挂载设备。 */
  private restoreAssetScope(): void {
    if (!this.assetTree.roots.some((node) => node.id === this.selectedRootAssetId)) { this.selectedRootAssetId = ''; }
    const scope = getEnergyMonitoringSelectedAssetIds(this.assetTree, this.selectedRootAssetId);
    if (!this.selectedRootAssetId) { for (const context of this.snapshot?.contexts || []) { scope.add(context.deviceId); } }
    if (!scope.has(this.selectedTreeNodeId)) { this.selectedTreeNodeId = this.selectedRootAssetId; }
    this.selectedAssetIds = this.selectedTreeNodeId ? getEnergyMonitoringSelectedAssetIds(this.assetTree, this.selectedTreeNodeId) : scope;
    this.selectedAssetName = this.assetTree.nodes.get(this.selectedTreeNodeId)?.name || '全部根资产';
  }

  /** 活动告警优先级与行状态共用同一映射；实时更新后夹紧分页并移除范围外勾选。 */
  private rebuildDevices(): void {
    const priorities = ['CRITICAL', 'MAJOR', 'MINOR', 'WARNING', 'INDETERMINATE'];
    const severities = new Map<string, string>();
    for (const alarm of this.snapshot?.alarms || []) {
      if (alarm.cleared) { continue; }
      const old = severities.get(alarm.originator.id);
      if (!old || priorities.indexOf(alarm.severity) < priorities.indexOf(old)) { severities.set(alarm.originator.id, alarm.severity); }
    }
    this.activeAlarmDeviceIds = new Set(severities.keys());
    const labels = new Map<string, string>();
    for (const [id, severity] of severities) {
      labels.set(id, getSmartLightningAlarmSeverityLabel(getSmartLightningAlarmSeverityTone(severity)));
    }
    this.devices = buildEnergyQueryDevices(this.assetTree, this.snapshot?.contexts || [], (id) => this.live.latest(id), labels);
    this.page = Math.min(this.page, this.totalPages);
    const visible = new Set(this.filteredRows.map((row) => row.id));
    this.selectedRowIds = new Set([...this.selectedRowIds].filter((id) => visible.has(id)));
  }

  /** 销毁只释放本页引用，其他能源页面继续共享原WebSocket连接。 */
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); this.live.release(); }
}
