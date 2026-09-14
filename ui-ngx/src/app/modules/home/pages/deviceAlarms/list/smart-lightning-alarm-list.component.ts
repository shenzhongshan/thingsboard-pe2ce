/** 文件职责：告警列表模块的 smart-lightning-alarm-list.component，负责页面状态管理、接口调用与用户交互。 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AlarmService } from '@core/http/alarm.service';
import { DialogService } from '@core/services/dialog.service';
import { AlarmCommentDialogComponent, AlarmCommentDialogData } from '@home/components/alarm/alarm-comment-dialog.component';
import { AlarmInfo, AlarmQueryV2 } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MAX_SAFE_PAGE_SIZE, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, forkJoin, map, of, Subject, takeUntil } from 'rxjs';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningActiveAlarmDeviceIds,
  getSmartLightningDefaultExpandedAssetIds,
  getSmartLightningMonitoringAssetPaths,
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
import {
  SmartLightningAlarmDetailsDialogComponent,
  SmartLightningAlarmDetailsDialogData
} from './smart-lightning-alarm-details-dialog.component';
import {
  SmartLightningAlarmDispatchDialogComponent,
  SmartLightningAlarmDispatchDialogData
} from './smart-lightning-alarm-dispatch-dialog.component';

@Component({
  selector: 'tb-smart-lightning-alarm-list',
  templateUrl: './smart-lightning-alarm-list.component.html',
  styleUrls: ['./smart-lightning-alarm-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmListComponent implements OnInit, OnDestroy {
  /** 由路由数据区分普通列表、告警派发和告警处理页面。
   *  同一组件在 /deviceAlarms/* 多个路由间导航时会被 Angular 复用，
   *  因此模式不能在构造器读取一次快照，必须订阅路由数据变化。 */
  dispatchMode = false;
  processingMode = false;
  pageTitle = '智能防雷报警信息';
  private routeDataLoaded = false;
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
  /** 未清除告警设备 ID；共享资产树据此标记设备所在资产并向全部上级传播。 */
  activeAlarmDeviceIds = new Set<string>();
  readonly pageSize = 20;
  page = 1;
  readonly severityTabs: SmartLightningAlarmListSeverityTab[] = ['全部', '严重', '重要', '次要', '警告', '不确定'];
  selectedRows = new Set<string>();
  filters: SmartLightningAlarmListFilters = {
    severityTab: '全部', alarmType: '', severity: '', workOrderStatus: '', productName: '', keyword: '', startTime: '', endTime: ''
  };

  private allRows: SmartLightningAlarmListRow[] = [];
  private readonly deviceIds = new Set<string>();
  private readonly deviceContexts = new Map<string, SmartLightningAlarmListContext>();
  /** 设备的资产树绝对地址（根资产到设备逐级 / 分隔），用于仪表地址列展示。 */
  private deviceAssetPaths = new Map<string, string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly alarmService: AlarmService,
    private readonly dialog: MatDialog,
    private readonly dialogService: DialogService,
    private readonly cd: ChangeDetectorRef,
    private readonly route: ActivatedRoute
  ) {
  }

  ngOnInit(): void {
    // 路由数据订阅放在 ngOnInit：组件复用期间路由切换会推送新的 data，
    // 操作列（派发/确认/清除/备注）按最新模式重新渲染。
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe((data) => {
      this.dispatchMode = data['dispatchMode'] === true;
      this.processingMode = data['processingMode'] === true;
      this.pageTitle = data['title'] || '智能防雷报警信息';
      this.cd.markForCheck();
    });
    if (!this.routeDataLoaded) {
      this.routeDataLoaded = true;
      this.loadAssetTree();
    }
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

  /**
   * 返回当前资产范围内的全部产品名称，供告警处理页下拉筛选。
   *
   * 产品选项不能只从未清除告警生成：设备可能暂时没有活动告警，
   * 但仍然需要按该产品查询已确认或已清除的历史告警。因此这里以
   * 全部设备上下文为主，并合并已经加载的告警行作为容错补充。
   */
  get productNames(): string[] {
    const deviceProductNames = [...this.deviceContexts.values()]
      .map((context) => context.productName || '');
    const alarmProductNames = this.allRows.map((row) => row.productName);
    return this.uniqueValues([...deviceProductNames, ...alarmProductNames]);
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
    return ['未派发', '已派发'];
  }

  get allPageRowsSelected(): boolean {
    return this.pageRows.length > 0 && this.pageRows.every((row) => this.selectedRows.has(row.id));
  }

  formatTime(timestamp: number): string {
    return formatSmartLightningAlarmListTime(timestamp);
  }

  workOrderTone(status: string): string {
    if (status === '已派发') return 'assigned';
    return 'unassigned';
  }

  alarmStatusTone(status: string): string {
    switch (status) {
      case '已确认未清除': return 'active-ack';
      case '未确认已清除': return 'cleared-unack';
      case '已确认已清除': return 'cleared-ack';
      default: return 'active-unack';
    }
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
  selectRootAsset(rootAssetId: string, expandDescendants = true): void {
    this.selectedRootAssetId = rootAssetId;
    const roots = getSmartLightningMonitoringVisibleRoots(this.assetTree, rootAssetId);
    this.selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    this.selectedAssetName = rootAssetId && roots[0] ? roots[0].name : '全部根资产';
    if (expandDescendants) {
      roots.forEach((root) => this.openDescendants(root.id));
    }
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
    this.filters = {severityTab: '全部', alarmType: '', severity: '', workOrderStatus: '', productName: '', keyword: '', startTime: '', endTime: ''};
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

  /** 一键处理只确认当前仍可由 ThingsBoard 原生接口确认的告警。 */
  acknowledgeSelected(): void {
    const ids = this.pageRows
      .filter((row) => this.selectedRows.has(row.id) && this.canAcknowledge(row))
      .map((row) => row.id);
    if (!ids.length) return;
    const requests = ids.map((id) => this.alarmService.ackAlarm(id).pipe(catchError(() => of(null))));
    forkJoin(requests).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.selectedRows.clear();
      this.selectedRows = new Set(this.selectedRows);
      this.loadAlarms();
    });
  }

  /**
   * 打开告警详情。
   *
   * <p>告警处理页在本模块的自定义弹窗内整合派发、活动、确认和清除；
   * 普通列表打开同一个弹窗的只读模式。</p>
   */
  openAlarmDetails(row: SmartLightningAlarmListRow): void {
    const data: SmartLightningAlarmDetailsDialogData = {
      alarmId: row.id,
      actionMode: this.processingMode
    };
    this.dialog.open<SmartLightningAlarmDetailsDialogComponent, SmartLightningAlarmDetailsDialogData, boolean>(SmartLightningAlarmDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'smart-lightning-alarm-detail-panel'],
      data
    }).afterClosed().pipe(takeUntil(this.destroy$)).subscribe((updated) => {
      if (updated) this.loadAlarms();
    });
  }

  openAlarmDispatch(row: SmartLightningAlarmListRow): void {
    const data: SmartLightningAlarmDispatchDialogData = {alarmId: row.id};
    this.dialog.open<SmartLightningAlarmDispatchDialogComponent, SmartLightningAlarmDispatchDialogData, boolean>(
      SmartLightningAlarmDispatchDialogComponent,
      {disableClose: true, panelClass: ['tb-dialog'], data}
    ).afterClosed().pipe(takeUntil(this.destroy$)).subscribe((updated) => {
      if (updated) this.loadAlarms();
    });
  }

  acknowledgeAlarm(row: SmartLightningAlarmListRow): void {
    if (!this.canAcknowledge(row)) return;
    this.dialogService.confirm('确认告警', `确认“${row.alarmType}”告警后，告警仍会保持活动状态。`, '取消', '确认')
      .pipe(takeUntil(this.destroy$)).subscribe((confirmed) => {
        if (!confirmed) return;
        this.alarmService.ackAlarm(row.id).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadAlarms());
      });
  }

  clearAlarm(row: SmartLightningAlarmListRow): void {
    if (!this.canClear(row)) return;
    this.dialogService.confirm('清除告警', `清除“${row.alarmType}”告警后，该告警将进入已处理状态。`, '取消', '清除')
      .pipe(takeUntil(this.destroy$)).subscribe((confirmed) => {
        if (!confirmed) return;
        this.alarmService.clearAlarm(row.id).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadAlarms());
      });
  }

  openAlarmComment(row: SmartLightningAlarmListRow): void {
    const data: AlarmCommentDialogData = {alarmId: row.id};
    this.dialog.open<AlarmCommentDialogComponent, AlarmCommentDialogData, void>(AlarmCommentDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data
    });
  }

  canAcknowledge(row: SmartLightningAlarmListRow): boolean {
    return row.status === 'ACTIVE_UNACK' || row.status === 'CLEARED_UNACK';
  }

  canClear(row: SmartLightningAlarmListRow): boolean {
    return row.status === 'ACTIVE_UNACK' || row.status === 'ACTIVE_ACK';
  }

  canDispatch(row: SmartLightningAlarmListRow): boolean {
    return row.workOrderStatus === '未派发' && (row.status === 'ACTIVE_UNACK' || row.status === 'ACTIVE_ACK');
  }

  exportRows(): void {
    const rows = this.selectedRows.size
      ? this.filteredRows.filter((row) => this.selectedRows.has(row.id))
      : this.filteredRows;
    if (!rows.length) return;
    const escape = (value: string): string => `"${value.replace(/"/g, '""')}"`;
    const lines = [
      ['设备名', '报警时间', '告警状态', '工单状态', '报警类型', '报警等级', '设备型号', '仪表地址'],
      ...rows.map((row) => [row.projectDeviceName, this.formatTime(row.alarmTime), row.alarmStatus, row.workOrderStatus, row.alarmType,
        row.severity, row.deviceType, row.meterAddress])
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
        this.deviceAssetPaths = getSmartLightningMonitoringAssetPaths(this.assetTree);
        this.expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
        this.selectRootAsset('', false);
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
      meterAddress: context.meterAddress || '--',
      timeSetting: context.timeSetting || '全时段'
    };
  }

  private loadAlarms(): void {
    // 告警处理页需要同时查询未清除、已确认和已清除记录，空过滤对象表示不限制告警状态。
    const query = new AlarmQueryV2(
      null,
      new TimePageLink(MAX_SAFE_PAGE_SIZE, 0, null, {property: 'createdTime', direction: Direction.DESC}),
      {}
    );
    this.alarmService.getAllAlarmsV2(query).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => {
        const rows = pageData.data
          .filter((alarm) => alarm.originator?.entityType === EntityType.DEVICE && this.deviceIds.has(alarm.originator.id))
          .map((alarm) => mapSmartLightningAlarmListRow(
            alarm,
            this.deviceContexts.get(alarm.originator.id),
            this.deviceAssetPaths.get(alarm.originator.id) || ''
          ));
        this.allRows = sortSmartLightningAlarmListRows(rows);
        this.activeAlarmDeviceIds = getSmartLightningActiveAlarmDeviceIds(pageData.data, this.deviceIds);
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
