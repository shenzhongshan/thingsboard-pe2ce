import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AlarmService } from '@core/http/alarm.service';
import { AlarmDetailsDialogComponent, AlarmDetailsDialogData } from '@home/components/alarm/alarm-details-dialog.component';
import { AlarmInfo, AlarmQueryV2 } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MAX_SAFE_PAGE_SIZE, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, forkJoin, map, of, Subject, takeUntil } from 'rxjs';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningMonitoringRootAssetIds,
  getSmartLightningMonitoringVisibleRoots,
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '../../smart-lightning/monitoring/smart-lightning-monitoring.data';
import { SmartLightningDeviceContext, SmartLightningMonitoringApiService } from '../../smart-lightning/monitoring/smart-lightning-monitoring-api.service';
import {
  filterSmartLightningAlarmListRows,
  formatSmartLightningAlarmListTime,
  mapSmartLightningAlarmListRow,
  SmartLightningAlarmListContext,
  SmartLightningAlarmListFilters,
  SmartLightningAlarmListRow,
  SmartLightningAlarmListSeverityTab,
  sortSmartLightningAlarmListRows
} from './smart-lightning-alarm-list.data';

@Component({
  selector: 'tb-smart-lightning-alarm-list',
  templateUrl: './smart-lightning-alarm-list.component.html',
  styleUrls: ['./smart-lightning-alarm-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmListComponent implements OnInit, OnDestroy {
  /** 由路由数据区分普通告警列表和告警派发页面。 */
  readonly dispatchMode: boolean;
  readonly pageTitle: string;
  assetTree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  expandedAssetIds = new Set<string>();
  selectedAssetIds = new Set<string>();
  selectedRootAssetId = '';
  selectedAssetName = '全部资产';
  assetSearch = '';
  assetsLoading = true;
  assetsLoadError = false;
  alarmsLoading = true;
  alarmsLoadError = false;
  /** 资产树设备节点的状态标记：存在未清除告警时显示红色竖条。 */
  activeAlarmDeviceIds = new Set<string>();
  readonly pageSize = 20;
  page = 1;
  readonly severityTabs: SmartLightningAlarmListSeverityTab[] = ['全部', '紧急', '一般', '正常'];
  selectedRows = new Set<string>();
  filters: SmartLightningAlarmListFilters = {
    severityTab: '全部', alarmType: '', severity: '', workOrderStatus: '', keyword: '', startTime: '', endTime: ''
  };

  private allRows: SmartLightningAlarmListRow[] = [];
  private readonly deviceIds = new Set<string>();
  private readonly deviceContexts = new Map<string, SmartLightningAlarmListContext>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly alarmService: AlarmService,
    private readonly dialog: MatDialog,
    private readonly cd: ChangeDetectorRef,
    private readonly route: ActivatedRoute
  ) {
    this.dispatchMode = this.route.snapshot.data['dispatchMode'] === true;
    this.pageTitle = this.route.snapshot.data['title'] || '智能防雷报警信息';
  }

  ngOnInit(): void {
    this.loadAssetTree();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredRows(): SmartLightningAlarmListRow[] {
    const selectedDeviceIds = new Set([...this.selectedAssetIds]
      .filter((id) => this.assetTree.nodes.get(id)?.entityType === EntityType.DEVICE));
    const scopedRows = this.allRows.filter((row) => selectedDeviceIds.has(row.originatorId));
    return filterSmartLightningAlarmListRows(scopedRows, this.filters);
  }

  get pageRows(): SmartLightningAlarmListRow[] {
    const rows = sortSmartLightningAlarmListRows(this.filteredRows);
    const start = (this.page - 1) * this.pageSize;
    return rows.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize));
  }

  get alarmTypes(): string[] {
    return this.uniqueValues(this.allRows.map((row) => row.alarmType));
  }

  get assetCount(): number {
    const visibleIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, this.selectedRootAssetId));
    return [...this.assetTree.nodes.values()]
      .filter((node) => node.entityType === EntityType.ASSET && visibleIds.has(node.id)).length;
  }

  get severities(): string[] {
    return ['严重', '重要', '次要', '警告', '不确定'];
  }

  get workOrderStatuses(): string[] {
    return ['未派发', '已派发', '已处理'];
  }

  get allPageRowsSelected(): boolean {
    return this.pageRows.length > 0 && this.pageRows.every((row) => this.selectedRows.has(row.id));
  }

  formatTime(timestamp: number): string {
    return formatSmartLightningAlarmListTime(timestamp);
  }

  workOrderTone(status: string): string {
    if (status === '已处理') return 'handled';
    if (status === '已派发') return 'assigned';
    return 'unassigned';
  }

  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    if (this.expandedAssetIds.has(node.id)) this.expandedAssetIds.delete(node.id);
    else this.expandedAssetIds.add(node.id);
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    // 保留资产树递归得到的全部节点；设备接口返回后，filteredRows 只会取其中的 DEVICE 节点。
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.openDescendants(node.id);
    this.page = 1;
    this.selectedRows.clear();
    this.selectedRows = new Set(this.selectedRows);
    this.cd.markForCheck();
  }

  /** 根资产筛选与告警列表共享同一棵树，切换后只保留对应子树的告警。 */
  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    const roots = getSmartLightningMonitoringVisibleRoots(this.assetTree, rootAssetId);
    this.selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    this.selectedAssetName = rootAssetId && roots[0] ? roots[0].name : '全部根资产';
    roots.forEach((root) => this.openDescendants(root.id));
    this.selectedRows.clear();
    this.selectedRows = new Set(this.selectedRows);
    this.page = 1;
    this.cd.markForCheck();
  }

  setSeverityTab(tab: SmartLightningAlarmListSeverityTab): void {
    this.filters = {...this.filters, severityTab: tab};
    this.page = 1;
  }

  query(): void {
    this.page = 1;
    this.selectedRows.clear();
    this.selectedRows = new Set(this.selectedRows);
  }

  reset(): void {
    this.filters = {severityTab: '全部', alarmType: '', severity: '', workOrderStatus: '', keyword: '', startTime: '', endTime: ''};
    this.query();
  }

  setPage(page: number): void {
    this.page = Math.min(Math.max(1, page), this.totalPages);
  }

  toggleRow(id: string): void {
    if (this.selectedRows.has(id)) this.selectedRows.delete(id);
    else this.selectedRows.add(id);
    this.selectedRows = new Set(this.selectedRows);
  }

  togglePageRows(): void {
    const next = new Set(this.selectedRows);
    if (this.allPageRowsSelected) this.pageRows.forEach((row) => next.delete(row.id));
    else this.pageRows.forEach((row) => next.add(row.id));
    this.selectedRows = next;
  }

  /** 一键处理只确认当前选中的活动告警；已处理行不会再次请求。 */
  acknowledgeSelected(): void {
    const ids = this.pageRows
      .filter((row) => this.selectedRows.has(row.id) && row.workOrderStatus !== '已处理')
      .map((row) => row.id);
    if (!ids.length) return;
    const requests = ids.map((id) => this.alarmService.ackAlarm(id).pipe(catchError(() => of(null))));
    forkJoin(requests).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.selectedRows.clear();
      this.selectedRows = new Set(this.selectedRows);
      this.loadAlarms();
    });
  }

  /** 详情直接打开系统已有告警详情对话框；派发页启用受理人、确认和处理操作。 */
  openAlarmDetails(row: SmartLightningAlarmListRow): void {
    const data: AlarmDetailsDialogData = {
      alarmId: row.id,
      allowAcknowledgment: this.dispatchMode,
      allowClear: this.dispatchMode,
      displayDetails: true,
      allowAssign: this.dispatchMode
    };
    this.dialog.open(AlarmDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data
    }).afterClosed().pipe(takeUntil(this.destroy$)).subscribe((updated) => {
      if (updated) {
        this.loadAlarms();
      }
    });
  }

  exportRows(): void {
    const rows = this.selectedRows.size
      ? this.filteredRows.filter((row) => this.selectedRows.has(row.id))
      : this.filteredRows;
    if (!rows.length) return;
    const escape = (value: string): string => `"${value.replace(/"/g, '""')}"`;
    const lines = [
      ['项目/设备/企业名称', '报警时间', '工单状态', '报警类型', '报警等级', '网关识别号', '设备型号', '仪表地址', '回路名称', '时段设置', '详情描述'],
      ...rows.map((row) => [row.projectDeviceName, this.formatTime(row.alarmTime), row.workOrderStatus, row.alarmType,
        row.severity, row.gatewayIdentifier, row.deviceType, row.meterAddress, row.circuitName, row.timeSetting, row.detailDescription])
    ];
    const blob = new Blob([`\uFEFF${lines.map((line) => line.map(escape).join(',')).join('\r\n')}`], {type: 'text/csv;charset=utf-8'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `告警信息-${Date.now()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  private loadAssetTree(): void {
    this.monitoringApi.getTree().pipe(takeUntil(this.destroy$)).subscribe({
      next: ({roots}) => {
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots);
        this.assetTree.roots.forEach((root) => this.expandedAssetIds.add(root.id));
        this.expandedAssetIds = new Set(this.expandedAssetIds);
        this.selectRootAsset('');
        this.assetsLoading = false;
        this.loadDevicesAndAlarms();
        this.cd.markForCheck();
      },
      error: () => {
        this.assetsLoading = false;
        this.alarmsLoading = false;
        this.assetsLoadError = true;
        this.cd.markForCheck();
      }
    });
  }

  private loadDevicesAndAlarms(): void {
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    this.monitoringApi.getLightningDeviceContexts(MAX_SAFE_PAGE_SIZE, 0).pipe(
      map((pageData) => pageData.data.filter((device) =>
        this.assetTree.nodes.get(device.deviceId)?.entityType === EntityType.DEVICE)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (contexts) => {
        this.deviceIds.clear();
        this.deviceContexts.clear();
        contexts.forEach((context) => {
          this.deviceIds.add(context.deviceId);
          this.deviceContexts.set(context.deviceId, this.toAlarmContext(context));
        });
        this.loadAlarms();
      },
      error: () => {
        this.alarmsLoading = false;
        this.alarmsLoadError = true;
        this.cd.markForCheck();
      }
    });
  }

  private toAlarmContext(context: SmartLightningDeviceContext): SmartLightningAlarmListContext {
    return {
      deviceType: context.deviceProfileName || '--',
      productName: context.productName || '--',
      gatewayIdentifier: context.gatewayIdentifier || '--',
      meterAddress: context.meterAddress || '--',
      circuitName: context.circuitName || '--',
      timeSetting: context.timeSetting || '全时段'
    };
  }

  private loadAlarms(): void {
    const query = new AlarmQueryV2(
      null,
      new TimePageLink(MAX_SAFE_PAGE_SIZE, 0, null, {property: 'createdTime', direction: Direction.DESC}),
      {}
    );
    this.alarmService.getAllAlarmsV2(query).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => {
        const rows = pageData.data
          .filter((alarm) => alarm.originator?.entityType === EntityType.DEVICE && this.deviceIds.has(alarm.originator.id))
          .map((alarm) => mapSmartLightningAlarmListRow(alarm, this.deviceContexts.get(alarm.originator.id)));
        this.allRows = sortSmartLightningAlarmListRows(rows);
        this.activeAlarmDeviceIds = new Set(pageData.data
          .filter((alarm) => (alarm.status === 'ACTIVE_UNACK' || alarm.status === 'ACTIVE_ACK')
            && alarm.originator?.entityType === EntityType.DEVICE && this.deviceIds.has(alarm.originator.id))
          .map((alarm) => alarm.originator.id));
        this.page = 1;
        this.alarmsLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.allRows = [];
        this.activeAlarmDeviceIds = new Set();
        this.alarmsLoading = false;
        this.alarmsLoadError = true;
        this.cd.markForCheck();
      }
    });
  }

  private uniqueValues(values: string[]): string[] {
    return [...new Set(values.filter((value) => value && value !== '--'))]
      .sort((left, right) => left.localeCompare(right, 'zh-CN'));
  }

  private openDescendants(id: string): void {
    const node = this.assetTree.nodes.get(id);
    if (!node) return;
    node.childIds.forEach((childId) => {
      this.expandedAssetIds.add(childId);
      this.openDescendants(childId);
    });
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }
}
