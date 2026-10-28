/** 文件职责：管理电气安全数据查询页的资产范围、筛选条件、表格选择和分页状态。 */
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {ActivatedRoute} from '@angular/router';
import {auditTime, bufferTime, filter, merge, Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {AlarmInfo} from '@shared/models/alarm.models';
import {EntityType} from '@shared/models/entity-type.models';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningDefaultExpandedAssetIds,
  getSmartLightningMonitoringRootAssetIds,
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import {
  buildElectricalSafetyQueryRows,
  ElectricalSafetyQueryRow,
  ElectricalSafetyQueryStatus,
  filterElectricalSafetyQueryRows
} from './electrical-safety-data-query.data';
import {
  buildElectricalSafetyMonitoringDevices,
  ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
  getElectricalSafetyRealtimeMetricDefinitions,
  parseElectricalSafetyActiveValue
} from '../monitoring/electrical-safety-monitoring.data';
import {ElectricalSafetyApiService, ElectricalSafetyDeviceContext} from '../electrical-safety-api.service';
import {SmartLightningHistoryReviewDialogComponent} from '@home/pages/smart-lightning/query/smart-lightning-history-review-dialog.component';
import {
  SmartLightningDeviceTelemetryChange,
  SmartLightningRealtimeService
} from '@home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service';

/** 请求完成前使用空树，避免首次渲染时把本地演示资产作为真实范围。 */
const EMPTY_ELECTRICAL_SAFETY_ASSET_TREE = buildSmartLightningMonitoringAssetTreeFromRoots([]);

/** 电气安全数据查询页；表格与资产范围均从电气安全模块专用接口加载。 */
@Component({
  selector: 'tb-electrical-safety-data-query',
  templateUrl: './electrical-safety-data-query.component.html',
  styleUrls: ['./electrical-safety-data-query.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ElectricalSafetyDataQueryComponent implements OnInit, OnDestroy {
  /** 左侧资产树只包含当前用户可见的电气安全设备层级。 */
  assetTree = EMPTY_ELECTRICAL_SAFETY_ASSET_TREE;
  readonly statuses: Array<'全部' | ElectricalSafetyQueryStatus> = ['全部', '报警', '离线', '正常'];
  readonly pageSize = 20;
  assetsLoading = true;
  assetsLoadError = false;
  devicesLoading = true;
  devicesLoadError = false;
  alarmsLoading = true;
  alarmsLoadError = false;

  /** 默认选择全部根资产，并展开树的首层，供页面首次浏览设备。 */
  selectedRootAssetId = '';
  selectedTreeNodeId = '';
  selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, ''));
  expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
  selectedAssetName = '全部根资产';
  assetSearch = '';

  /** 查询表单状态在本页独立维护，重置筛选不会清除资产树范围。 */
  productNameFilter = '';
  functionType = '';
  keyword = '';
  activeStatus: '全部' | ElectricalSafetyQueryStatus = '全部';
  page = 1;
  selectedRowIds = new Set<string>();

  /** 树标记、状态筛选与行级告警等级都来自同一批模块活动告警。 */
  activeAlarmDeviceIds = new Set<string>();

  private deviceContexts: ElectricalSafetyDeviceContext[] = [];
  private alarmSeverityByDeviceId = new Map<string, string>();
  private readonly destroy$ = new Subject<void>();
  private assetTreeRequestId = 0;
  private deviceContextsRequestId = 0;
  private alarmRequestId = 0;

  constructor(
    private readonly electricalSafetyApiService: ElectricalSafetyApiService,
    private readonly realtimeService: SmartLightningRealtimeService,
    private readonly dialog: MatDialog,
    private readonly route: ActivatedRoute,
    private readonly changeDetector: ChangeDetectorRef
  ) {}

  /** 分别加载模块设备、分类资产树和活动告警，并订阅防雷共享实时变化流。 */
  ngOnInit(): void {
    // 查询页与防雷页共用实时订阅；只把 active 应用到当前电气安全设备行。
    this.realtimeService.deviceTelemetryChanged$.pipe(
      bufferTime(100),
      filter((changes) => changes.length > 0),
      takeUntil(this.destroy$)
    ).subscribe((changes) => this.applyRealtimeTelemetryChanges(changes));
    this.realtimeService.entitySnapshotReady$.pipe(takeUntil(this.destroy$)).subscribe((ready) => {
      if (ready) {
        this.applyLatestTelemetrySnapshot();
      }
    });
    this.realtimeService.deviceContextIncrementalChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadDeviceContexts();
      }
    });
    merge(this.realtimeService.deviceMembershipChanged$, this.realtimeService.assetMembershipChanged$)
      .pipe(auditTime(600), takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.assetsLoading) {
          this.loadAssetTree(true);
          this.loadDeviceContexts();
        }
      });
    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => this.loadActiveAlarms());

    this.realtimeService.acquire();
    this.selectedRootAssetId = this.route.snapshot.queryParamMap.get('selectedRootAssetId') || '';
    this.selectedTreeNodeId = this.route.snapshot.queryParamMap.get('selectedAssetId') || '';
    this.loadAssetTree();
    this.loadDeviceContexts();
    this.loadActiveAlarms();
  }

  /** 离开页面时结束仍在进行的接口订阅。 */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
  }

  /** 从当前电气安全上下文与告警状态纯转换查询行，保持接口数据和筛选逻辑分离。 */
  get rows(): ElectricalSafetyQueryRow[] {
    const devices = buildElectricalSafetyMonitoringDevices(this.deviceContexts, this.assetTree);
    return buildElectricalSafetyQueryRows(devices, this.activeAlarmDeviceIds, this.alarmSeverityByDeviceId);
  }

  /** 返回资产树当前范围内的设备行，产品分类在数据定义中已限制为 electrical_safety。 */
  get scopedRows(): ElectricalSafetyQueryRow[] {
    return this.rows.filter((row) => this.selectedAssetIds.has(row.assetId));
  }

  /** 按资产范围、状态、产品、功能和关键词筛选当前设备行。 */
  get filteredRows(): ElectricalSafetyQueryRow[] {
    return filterElectricalSafetyQueryRows(this.rows, this.selectedAssetIds, {
      status: this.activeStatus,
      productName: this.productNameFilter,
      functionCategory: this.functionType,
      keyword: this.keyword
    });
  }

  /** 当前页展示行，供序号、全选和表格正文共用。 */
  get pageRows(): ElectricalSafetyQueryRow[] {
    return this.filteredRows.slice((this.page - 1) * this.pageSize, this.page * this.pageSize);
  }

  /** 查询结果至少显示一页，空结果时分页按钮仍保持稳定状态。 */
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize));
  }

  /** 从当前资产范围生成产品名称选项，避免筛选出范围外的设备类型。 */
  get productNames(): string[] {
    return [...new Set(this.scopedRows.map((row) => row.productName))]
      .sort((left, right) => left.localeCompare(right, 'zh-CN'));
  }

  /** 从当前资产范围生成设备功能分类选项。 */
  get functionCategories(): string[] {
    return [...new Set(this.scopedRows.map((row) => row.functionCategory))]
      .sort((left, right) => left.localeCompare(right, 'zh-CN'));
  }

  /** 判断当前页是否已全部勾选。 */
  get allPageRowsSelected(): boolean {
    return this.pageRows.length > 0 && this.pageRows.every((row) => this.selectedRowIds.has(row.id));
  }

  /** 根资产变化后同步资产树选择范围，并清空旧的行选择。 */
  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    this.selectedTreeNodeId = rootAssetId;
    this.selectedAssetIds = this.getRootAssetSelection(rootAssetId);
    const root = this.assetTree.roots.find((node) => node.id === rootAssetId);
    this.selectedAssetName = root?.name || '全部根资产';
    this.clearSelectionAndResetPage();
  }

  /** 选择资产节点及其全部后代，确保表格和筛选下拉项使用同一资产范围。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedTreeNodeId = node.id;
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.openDescendants(node.id);
    this.clearSelectionAndResetPage();
  }

  /** 展开或收起资产分支时替换集合引用，触发 OnPush 页面更新。 */
  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    const nextExpandedIds = new Set(this.expandedAssetIds);
    if (nextExpandedIds.has(node.id)) {
      nextExpandedIds.delete(node.id);
    } else {
      nextExpandedIds.add(node.id);
    }
    this.expandedAssetIds = nextExpandedIds;
  }

  /** 全选当前根资产范围内的节点，不会跨越根资产筛选边界。 */
  selectAllAssets(): void {
    this.selectRootAsset(this.selectedRootAssetId);
  }

  /** 切换结果状态并回到第一页，避免当前页超出筛选结果。 */
  setStatus(status: '全部' | ElectricalSafetyQueryStatus): void {
    this.activeStatus = status;
    this.clearSelectionAndResetPage();
  }

  /** 文本和下拉筛选变化时回到第一页，避免当前页号超出结果范围。 */
  resetPage(): void {
    this.page = 1;
  }

  /** 重置表单和状态页签，保留左侧当前资产范围。 */
  reset(): void {
    this.productNameFilter = '';
    this.functionType = '';
    this.keyword = '';
    this.activeStatus = '全部';
    this.clearSelectionAndResetPage();
  }

  /** 将页码限制在当前结果的有效范围内。 */
  setPage(page: number): void {
    this.page = Math.min(Math.max(1, page), this.totalPages);
  }

  /** 勾选或取消勾选单台设备，并创建新集合触发变更检测。 */
  toggleRow(rowId: string): void {
    const nextSelectedRowIds = new Set(this.selectedRowIds);
    if (nextSelectedRowIds.has(rowId)) {
      nextSelectedRowIds.delete(rowId);
    } else {
      nextSelectedRowIds.add(rowId);
    }
    this.selectedRowIds = nextSelectedRowIds;
  }

  /** 对当前页执行全选或取消全选，不影响其他页已选行。 */
  togglePageRows(): void {
    const nextSelectedRowIds = new Set(this.selectedRowIds);
    for (const row of this.pageRows) {
      if (this.allPageRowsSelected) {
        nextSelectedRowIds.delete(row.id);
      } else {
        nextSelectedRowIds.add(row.id);
      }
    }
    this.selectedRowIds = nextSelectedRowIds;
  }

  /** 导出已勾选设备；未勾选时导出当前筛选结果，文件编码兼容常见表格软件。 */
  exportRows(): void {
    const exportRows = this.selectedRowIds.size
      ? this.filteredRows.filter((row) => this.selectedRowIds.has(row.id))
      : this.filteredRows;
    if (!exportRows.length) {
      return;
    }
    const escapeCsvCell = (value: string): string => `"${value.replace(/"/g, '""')}"`;
    const lines = [
      ['序号', '设备名称', '设备类型', '设备协议', '状态', '产品名称', '所属资产', '最近上报时间'],
      ...exportRows.map((row, index) => [
        String(index + 1), row.deviceName, row.deviceType, row.protocol, row.status,
        row.productName, row.assetPath, row.lastActivityText
      ])
    ];
    const csvContent = `\uFEFF${lines.map((line) => line.map(escapeCsvCell).join(',')).join('\r\n')}`;
    const csvBlob = new Blob([csvContent], {type: 'text/csv;charset=utf-8'});
    const objectUrl = URL.createObjectURL(csvBlob);
    const downloadLink = document.createElement('a');
    downloadLink.href = objectUrl;
    downloadLink.download = `电气安全数据查询-${Date.now()}.csv`;
    downloadLink.click();
    URL.revokeObjectURL(objectUrl);
  }

  /** 打开防雷共享的历史遥测弹窗，并给它传入本型号已核实的测点表。 */
  openHistoryReview(row: ElectricalSafetyQueryRow): void {
    this.dialog.open(SmartLightningHistoryReviewDialogComponent, {
      data: {
        deviceId: row.id,
        deviceName: row.deviceName,
        deviceProfileName: row.deviceType,
        metricDefinitions: getElectricalSafetyRealtimeMetricDefinitions(row.deviceType)
      },
      width: '980px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: 'first-tabbable',
      restoreFocus: true
    });
  }

  /** 分页/范围变化时清理行选择，避免把已离开范围的设备带入导出结果。 */
  private clearSelectionAndResetPage(): void {
    this.selectedRowIds = new Set<string>();
    this.page = 1;
  }

  /** 读取专属资产树；保留用户选择时仅恢复仍存在且仍属于当前根资产的节点。 */
  private loadAssetTree(preserveSelection = false): void {
    const requestId = ++this.assetTreeRequestId;
    const previousExpandedIds = new Set(this.expandedAssetIds);
    const previousRootAssetId = this.selectedRootAssetId;
    const previousNodeId = this.selectedTreeNodeId;
    this.assetsLoading = true;
    this.assetsLoadError = false;
    this.electricalSafetyApiService.getAssetTree().pipe(takeUntil(this.destroy$)).subscribe({
      next: (tree) => {
        if (requestId !== this.assetTreeRequestId) {
          return;
        }
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(tree?.roots || []);
        this.expandedAssetIds = preserveSelection
          ? new Set([...previousExpandedIds].filter((id) => this.assetTree.nodes.has(id)))
          : getSmartLightningDefaultExpandedAssetIds(this.assetTree);
        this.restoreAssetSelection(
          preserveSelection ? previousRootAssetId : this.selectedRootAssetId,
          preserveSelection ? previousNodeId : this.selectedTreeNodeId
        );
        this.assetsLoading = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        if (requestId !== this.assetTreeRequestId) {
          return;
        }
        this.assetTree = EMPTY_ELECTRICAL_SAFETY_ASSET_TREE;
        this.expandedAssetIds = new Set<string>();
        this.selectedRootAssetId = '';
        this.selectedTreeNodeId = '';
        this.selectedAssetName = '全部根资产';
        this.selectedAssetIds = this.getRootAssetSelection('');
        this.assetsLoading = false;
        this.assetsLoadError = true;
        this.changeDetector.markForCheck();
      }
    });
  }

  /** 单独读取当前用户可见的电气安全设备上下文，资产树失败不会清空设备资料。 */
  private loadDeviceContexts(): void {
    const requestId = ++this.deviceContextsRequestId;
    this.devicesLoading = true;
    this.devicesLoadError = false;
    this.electricalSafetyApiService.getDeviceContexts().pipe(takeUntil(this.destroy$)).subscribe({
      next: (contexts) => {
        if (requestId !== this.deviceContextsRequestId) {
          return;
        }
        this.deviceContexts = (contexts || []).filter((context) =>
          context.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY && !!context.deviceId
        );
        this.devicesLoading = false;
        this.applyLatestTelemetrySnapshot();
        if (!this.assetsLoading) {
          this.restoreAssetSelection(this.selectedRootAssetId, this.selectedTreeNodeId);
        } else {
          this.selectedAssetIds = this.getRootAssetSelection(this.selectedRootAssetId);
        }
        this.clearSelectionAndResetPage();
        this.changeDetector.markForCheck();
      },
      error: () => {
        if (requestId !== this.deviceContextsRequestId) {
          return;
        }
        this.deviceContexts = [];
        this.devicesLoading = false;
        this.devicesLoadError = true;
        this.selectedAssetIds = this.getRootAssetSelection(this.selectedRootAssetId);
        this.changeDetector.markForCheck();
      }
    });
  }

  /** 告警事件只刷新活动告警标记和状态，不重复请求设备资料。 */
  private loadActiveAlarms(): void {
    const requestId = ++this.alarmRequestId;
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    this.electricalSafetyApiService.getAllActiveAlarms(ELECTRICAL_SAFETY_PRODUCT_CATEGORY)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (alarms) => {
          if (requestId !== this.alarmRequestId) {
            return;
          }
          const alarmContext = this.toActiveAlarmContext(alarms);
          this.activeAlarmDeviceIds = alarmContext.deviceIds;
          this.alarmSeverityByDeviceId = alarmContext.severityByDeviceId;
          this.alarmsLoading = false;
          this.changeDetector.markForCheck();
        },
        error: () => {
          if (requestId !== this.alarmRequestId) {
            return;
          }
          this.activeAlarmDeviceIds = new Set<string>();
          this.alarmSeverityByDeviceId = new Map<string, string>();
          this.alarmsLoading = false;
          this.alarmsLoadError = true;
          this.changeDetector.markForCheck();
        }
      });
  }

  /** 只把当前活动告警的设备 ID 和最高严重程度提取给查询行展示。 */
  private toActiveAlarmContext(alarms: readonly AlarmInfo[]): {
    deviceIds: Set<string>;
    severityByDeviceId: Map<string, string>;
  } {
    const severityRanks: Record<string, number> = {CRITICAL: 5, MAJOR: 4, MINOR: 3, WARNING: 2, INDETERMINATE: 1};
    const severityLabels: Record<string, string> = {
      CRITICAL: '严重', MAJOR: '重要', MINOR: '次要', WARNING: '警告', INDETERMINATE: '不确定'
    };
    const deviceIds = new Set<string>();
    const severityRanksByDeviceId = new Map<string, number>();
    const severityByDeviceId = new Map<string, string>();
    for (const alarm of alarms) {
      if (alarm.originator?.entityType !== EntityType.DEVICE
        || (alarm.status !== 'ACTIVE_UNACK' && alarm.status !== 'ACTIVE_ACK')) {
        continue;
      }
      const deviceId = alarm.originator.id;
      const severity = String(alarm.severity || 'INDETERMINATE');
      const rank = severityRanks[severity] || severityRanks.INDETERMINATE;
      deviceIds.add(deviceId);
      if ((severityRanksByDeviceId.get(deviceId) || 0) < rank) {
        severityRanksByDeviceId.set(deviceId, rank);
        severityByDeviceId.set(deviceId, severityLabels[severity] || '不确定');
      }
    }
    return {deviceIds, severityByDeviceId};
  }

  /** 从共享 WebSocket 缓存应用设备 active 首帧，弥补 HTTP 请求和订阅建立间隙。 */
  private applyLatestTelemetrySnapshot(): void {
    if (!this.deviceContexts.length) {
      return;
    }
    const activeByDeviceId = new Map<string, boolean>();
    const activityTimeByDeviceId = new Map<string, number>();
    for (const context of this.deviceContexts) {
      const sample = this.realtimeService.getLatestTelemetryValues(context.deviceId).get('active');
      const active = parseElectricalSafetyActiveValue(sample?.value);
      if (active !== undefined) {
        activeByDeviceId.set(context.deviceId, active);
      }
      const lastActivityTime = this.realtimeService.getLatestActivityTime(context.deviceId);
      if (lastActivityTime !== undefined) {
        activityTimeByDeviceId.set(context.deviceId, lastActivityTime);
      }
    }
    this.applyDeviceRealtimeUpdates(activeByDeviceId, activityTimeByDeviceId);
  }

  /** 合并当前设备 active 遥测增量；页面外设备和其他电气测点不会影响查询状态。 */
  private applyRealtimeTelemetryChanges(changes: readonly SmartLightningDeviceTelemetryChange[]): void {
    if (!this.deviceContexts.length || !changes.length) {
      return;
    }
    const knownDeviceIds = new Set(this.deviceContexts.map((context) => context.deviceId));
    const activeByDeviceId = new Map<string, boolean>();
    const activityTimeByDeviceId = new Map<string, number>();
    for (const change of changes) {
      if (!knownDeviceIds.has(change.deviceId)) {
        continue;
      }
      if (change.values.has('active')) {
        const sample = this.realtimeService.getLatestTelemetryValues(change.deviceId).get('active');
        const active = parseElectricalSafetyActiveValue(sample?.value);
        if (active !== undefined) {
          activeByDeviceId.set(change.deviceId, active);
        }
      }
      const lastActivityTime = change.lastActivityTime ?? this.realtimeService.getLatestActivityTime(change.deviceId);
      if (lastActivityTime !== undefined) {
        activityTimeByDeviceId.set(change.deviceId, lastActivityTime);
      }
    }
    this.applyDeviceRealtimeUpdates(activeByDeviceId, activityTimeByDeviceId);
  }

  /** 只替换发生变化的设备上下文，确保在线状态和最近上报时间采用同一实时快照。 */
  private applyDeviceRealtimeUpdates(
    activeByDeviceId: ReadonlyMap<string, boolean>,
    activityTimeByDeviceId: ReadonlyMap<string, number>
  ): void {
    if (!activeByDeviceId.size && !activityTimeByDeviceId.size) {
      return;
    }
    let changed = false;
    this.deviceContexts = this.deviceContexts.map((context) => {
      const hasActiveUpdate = activeByDeviceId.has(context.deviceId);
      const active = activeByDeviceId.get(context.deviceId);
      const activityTime = activityTimeByDeviceId.get(context.deviceId);
      const activityIsNewer = activityTime !== undefined && activityTime > (context.lastActivityTime || 0);
      if ((!hasActiveUpdate || active === context.active) && !activityIsNewer) {
        return context;
      }
      changed = true;
      return {
        ...context,
        ...(hasActiveUpdate ? {active} : {}),
        ...(activityIsNewer ? {lastActivityTime: activityTime} : {})
      };
    });
    if (!changed) {
      return;
    }
    this.changeDetector.markForCheck();
  }

  /** 刷新资产树后恢复有效节点，避免旧 URL 或已移除资产造成空范围。 */
  private restoreAssetSelection(rootAssetId: string, nodeId: string): void {
    const validRootId = this.assetTree.roots.some((root) => root.id === rootAssetId) ? rootAssetId : '';
    const node = this.assetTree.nodes.get(nodeId);
    const rootIds = validRootId ? getSmartLightningMonitoringRootAssetIds(this.assetTree, validRootId) : [];
    if (node?.entityType === EntityType.ASSET && (!validRootId || rootIds.includes(node.id))) {
      this.selectedRootAssetId = validRootId;
      this.selectedTreeNodeId = node.id;
      this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
      this.selectedAssetName = node.name;
      return;
    }
    this.selectRootAsset(validRootId);
  }

  /** 选中资产节点后展开其子树，保持与防雷监测页面相同的浏览体验。 */
  private openDescendants(assetId: string): void {
    const node = this.assetTree.nodes.get(assetId);
    if (!node) {
      return;
    }
    const expanded = new Set(this.expandedAssetIds);
    for (const childId of node.childIds) {
      expanded.add(childId);
      this.expandedAssetIds = expanded;
      this.openDescendants(childId);
    }
  }

  /**
   * 选择所有根资产时纳入所有已授权设备，包括未挂载到资产树的设备。
   * 指定根资产时则严格按真实关系树中的后代设备 ID 限定范围。
   */
  private getRootAssetSelection(rootAssetId: string): Set<string> {
    const ids = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    if (!rootAssetId) {
      this.rows.forEach((row) => {
        if (row.id) {
          ids.add(row.id);
        }
      });
    }
    return ids;
  }
}
