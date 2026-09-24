/** 文件职责：数据查询模块的 smart-lightning-data-query.component，负责页面状态管理、接口调用与用户交互。 */
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {AlarmService} from '@core/http/alarm.service';
import {AlarmInfo} from '@shared/models/alarm.models';
import {DeviceInfo} from '@shared/models/device.models';
import {EntityType} from '@shared/models/entity-type.models';
import {DeviceId} from '@shared/models/id/device-id';
import {MAX_SAFE_PAGE_SIZE} from '@shared/models/page/page-link';
import {auditTime, bufferTime, catchError, filter, merge, map, of, Subject, takeUntil} from 'rxjs';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningDefaultExpandedAssetIds,
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '../monitoring/smart-lightning-monitoring.data';
import {SmartLightningDeviceContext, SmartLightningMonitoringApiService} from '../monitoring/smart-lightning-monitoring-api.service';
import {createSmartLightningAlarmQuery} from '../dashboard/smart-lightning-dashboard.data';
import {
  buildSmartLightningQueryRows,
  filterSmartLightningQueryRows,
  getSmartLightningQueryRootAssetIds,
  getSmartLightningQueryVisibleRoots,
  SmartLightningQueryDeviceAttributes,
  SmartLightningQueryRow,
  SmartLightningQueryStatus
} from './smart-lightning-data-query.data';
import {SmartLightningHistoryReviewDialogComponent} from './smart-lightning-history-review-dialog.component';
import {
  SmartLightningDeviceTelemetryChange,
  SmartLightningRealtimeService
} from '../shared/realtime/smart-lightning-realtime.service';

interface QueryDeviceContext {
  devices: DeviceInfo[];
  attributesByDeviceId: Map<string, SmartLightningQueryDeviceAttributes>;
}

/**
 * 智能防雷数据查询页面组件。
 *
 * <p>左侧共享资产树联动筛选，右侧按产品名称/功能/关键字查询设备表格，
 * 支持状态三态筛选、CSV 导出与历史遥测回顾弹窗。</p>
 */
@Component({
  selector: 'tb-smart-lightning-data-query',
  templateUrl: './smart-lightning-data-query.component.html',
  styleUrls: ['./smart-lightning-data-query.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningDataQueryComponent implements OnInit, OnDestroy {

  assetTree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  expandedAssetIds = new Set<string>();
  selectedAssetIds = new Set<string>();
  selectedRootAssetId = '';
  private selectedTreeNodeId = '';
  private selectionSource: 'root' | 'node' = 'root';
  selectedRowIds = new Set<string>();
  assetSearch = '';
  selectedAssetName = '全部资产';
  assetsLoading = true;
  assetsLoadError = false;
  devicesLoading = true;
  devicesLoadError = false;

  functionType = '';
  productNameFilter = '';
  keyword = '';
  activeStatus: '全部' | SmartLightningQueryStatus = '全部';
  page = 1;
  readonly pageSize = 20;
  readonly statuses: Array<'全部' | SmartLightningQueryStatus> = ['全部', '报警', '离线', '正常'];

  private devices: DeviceInfo[] = [];
  private deviceAttributes = new Map<string, SmartLightningQueryDeviceAttributes>();
  /** 传给共享资产树：选中的告警设备显示红色竖条。 */
  activeAlarmDeviceIds = new Set<string>();
  private alarmSeverityByDeviceId = new Map<string, string>();
  private readonly devicePaths = new Map<string, string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly alarmService: AlarmService,
    private readonly realtimeService: SmartLightningRealtimeService,
    private readonly dialog: MatDialog,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // 只将 active 与最近活跃时间的遥测增量应用到当前设备行，不重查整张设备表。
    this.realtimeService.deviceTelemetryChanged$.pipe(
      bufferTime(100),
      filter((changes) => changes.length > 0),
      takeUntil(this.destroy$)
    ).subscribe((changes) => this.applyQueryRealtimeChanges(changes));
    this.realtimeService.entitySnapshotReady$.pipe(
      filter((ready) => ready),
      takeUntil(this.destroy$)
    ).subscribe(() => this.applyLatestTelemetrySnapshot());

    // 设备名称、产品标签、协议等上下文变化时才重新读取设备列表。
    this.realtimeService.deviceContextIncrementalChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadDevices();
      }
    });
    // 集合成员变化时更新资产树、设备表与活动告警标记，并保留当前树选择。
    merge(
      this.realtimeService.deviceMembershipChanged$,
      this.realtimeService.assetMembershipChanged$
    ).pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadAssetTree(true);
      }
    });
    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadActiveAlarms();
      }
    });
    this.realtimeService.acquire();
    this.loadAssetTree();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
  }

  get rows(): SmartLightningQueryRow[] {
    const selectedDeviceIds = new Set([...this.selectedAssetIds]
      .filter((id) => this.assetTree.nodes.get(id)?.entityType === EntityType.DEVICE));
    return buildSmartLightningQueryRows(
      this.devices.map((device) => {
        const attributes = this.deviceAttributes.get(device.id.id);
        return {
          id: device.id.id,
          name: device.name,
          label: device.label,
          deviceProfileName: device.deviceProfileName || device.type || '--',
          protocol: typeof attributes?.deviceProtocol === 'string'
            ? attributes.deviceProtocol
            : device.deviceData?.transportConfiguration?.type,
          active: device.active
        };
      }),
      selectedDeviceIds,
      this.devicePaths,
      this.deviceAttributes,
      this.activeAlarmDeviceIds,
      this.alarmSeverityByDeviceId
    );
  }

  get filteredRows(): SmartLightningQueryRow[] {
    return filterSmartLightningQueryRows(this.rows, {
      status: this.activeStatus,
      productName: this.productNameFilter,
      functionCategory: this.functionType,
      keyword: this.keyword
    });
  }

  get pageRows(): SmartLightningQueryRow[] {
    return this.filteredRows.slice((this.page - 1) * this.pageSize, this.page * this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredRows.length / this.pageSize));
  }

  get productNames(): string[] {
    return [...new Set(this.rows.map((row) => row.productName).filter((value) => value !== '未配置产品名称'))]
      .sort((left, right) => left.localeCompare(right, 'zh-CN'));
  }

  get functionCategories(): string[] {
    return [...new Set(this.rows.map((row) => row.functionCategory).filter((value) => value !== '未分类'))]
      .sort((left, right) => left.localeCompare(right, 'zh-CN'));
  }

  get allPageRowsSelected(): boolean {
    return this.pageRows.length > 0 && this.pageRows.every((row) => this.selectedRowIds.has(row.id));
  }

  /** 展开/折叠资产节点，同步为不可变集合触发变更检测。 */
  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.expandedAssetIds.has(node.id) ? this.expandedAssetIds.delete(node.id) : this.expandedAssetIds.add(node.id);
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  /** 选中资产节点：选中集合包含其全部后代，作为设备表格的数据范围。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedTreeNodeId = node.id;
    this.selectionSource = 'node';
    this.selectedAssetName = node.name;
    this.openDescendants(node.id);
    this.selectedRowIds.clear();
    this.page = 1;
    this.cd.markForCheck();
  }

  /** 根资产筛选与节点选择共用同一套递归下属节点逻辑，确保右侧列表同步更新。 */
  selectRootAsset(rootAssetId: string, expandDescendants = true): void {
    this.selectedRootAssetId = rootAssetId;
    this.selectedTreeNodeId = rootAssetId;
    this.selectionSource = 'root';
    const roots = getSmartLightningQueryVisibleRoots(this.assetTree, rootAssetId);
    this.selectedAssetIds = new Set(getSmartLightningQueryRootAssetIds(this.assetTree, rootAssetId));
    this.selectedAssetName = rootAssetId && roots[0] ? roots[0].name : '全部根资产';
    if (expandDescendants) {
      roots.forEach((root) => this.openDescendants(root.id));
    }
    this.selectedRowIds.clear();
    this.page = 1;
    this.cd.markForCheck();
  }

  /** 资产树一键全选：保留当前根资产筛选范围并选中其全部下属节点。 */
  selectAllAssets(): void {
    this.selectRootAsset(this.selectedRootAssetId);
  }

  /** 切换状态页签（全部/在线/离线/未连接）并回到第一页。 */
  setStatus(status: '全部' | SmartLightningQueryStatus): void {
    this.activeStatus = status;
    this.page = 1;
  }

  /** 重置全部筛选条件并恢复默认状态页签。 */
  reset(): void {
    this.functionType = '';
    this.productNameFilter = '';
    this.keyword = '';
    this.activeStatus = '全部';
    this.selectedRowIds.clear();
    this.page = 1;
  }

  /** 表格翻页，页码钳制在有效范围内。 */
  setPage(page: number): void {
    this.page = Math.min(Math.max(1, page), this.totalPages);
  }

  /** 勾选/取消勾选单行设备，同步为不可变集合触发变更检测。 */
  toggleRow(rowId: string): void {
    this.selectedRowIds.has(rowId) ? this.selectedRowIds.delete(rowId) : this.selectedRowIds.add(rowId);
    this.selectedRowIds = new Set(this.selectedRowIds);
  }

  /** 全选/取消勾选当前页的全部设备行。 */
  togglePageRows(): void {
    const next = new Set(this.selectedRowIds);
    if (this.allPageRowsSelected) {
      this.pageRows.forEach((row) => next.delete(row.id));
    } else {
      this.pageRows.forEach((row) => next.add(row.id));
    }
    this.selectedRowIds = next;
  }

  /** 打开历史遥测回顾弹窗，携带设备与档案上下文。 */
  openHistoryReview(row: SmartLightningQueryRow): void {
    this.dialog.open(SmartLightningHistoryReviewDialogComponent, {
      data: {
        deviceId: row.id,
        deviceName: row.deviceName,
        deviceProfileName: row.deviceType
      },
      width: '980px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: 'first-tabbable',
      restoreFocus: true
    });
  }

  /** 复刻旧系统导出按钮：勾选时导出勾选设备，未勾选时导出当前筛选结果。 */
  exportRows(): void {
    const rows = this.selectedRowIds.size
      ? this.filteredRows.filter((row) => this.selectedRowIds.has(row.id))
      : this.filteredRows;
    if (!rows.length) {
      return;
    }
    const escape = (value: string): string => `"${value.replace(/"/g, '""')}"`;
    const lines = [
      ['序号', '设备名称', '设备类型', '设备协议', '状态', '产品名称', '所属资产', '最近活跃时间'],
      ...rows.map((row, index) => [
        String(index + 1), row.deviceName, row.deviceType, row.protocol, row.status,
        row.productName, row.assetPath, row.lastActivityText
      ])
    ];
    const blob = new Blob([`\uFEFF${lines.map((line) => line.map(escape).join(',')).join('\r\n')}`], {type: 'text/csv;charset=utf-8'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `设备查询-${new Date().getTime()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  /** 加载共享资产树并初始化展开状态与根资产选择，失败时标记错误状态。 */
  private loadAssetTree(preserveSelection = false): void {
    const previousExpandedIds = new Set(this.expandedAssetIds);
    const previousSelectedTreeNodeId = this.selectedTreeNodeId;
    const previousSelectionSource = this.selectionSource;
    this.assetsLoading = true;
    this.monitoringApi.getTree().pipe(takeUntil(this.destroy$)).subscribe({
      next: ({roots}) => {
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots);
        this.buildDevicePaths();
        this.expandedAssetIds = preserveSelection
          ? new Set([...previousExpandedIds].filter((id) => this.assetTree.nodes.has(id)))
          : getSmartLightningDefaultExpandedAssetIds(this.assetTree);
        const previousSelectedNode = this.assetTree.nodes.get(previousSelectedTreeNodeId);
        if (preserveSelection && previousSelectionSource === 'node' && previousSelectedNode) {
          this.selectAssetNode(previousSelectedNode);
        } else if (preserveSelection && previousSelectionSource === 'root' &&
          (!previousSelectedTreeNodeId || this.assetTree.roots.some((root) => root.id === previousSelectedTreeNodeId))) {
          this.selectRootAsset(previousSelectedTreeNodeId, false);
        } else {
          this.selectRootAsset('', false);
        }
        this.assetsLoading = false;
        this.loadDevices();
        this.loadActiveAlarms();
        this.cd.markForCheck();
      },
      error: () => {
        this.assetsLoadError = true;
        this.assetsLoading = false;
        this.devicesLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  /** 后端批量读取设备上下文；活动告警由独立查询及告警事件订阅维护。 */
  private loadDevices(): void {
    this.devicesLoading = true;
    this.devicesLoadError = false;
    this.monitoringApi.getLightningDeviceContexts(MAX_SAFE_PAGE_SIZE, 0).pipe(
      map((devicePage) => this.toQueryDeviceContext(devicePage.data)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (devices) => {
        this.devices = devices.devices;
        this.deviceAttributes = devices.attributesByDeviceId;
        this.devicesLoading = false;
        this.page = 1;
        // 若遥测首帧先于设备 HTTP 响应到达，从共享缓存补齐 active 和最近活跃时间。
        this.applyLatestTelemetrySnapshot();
        this.cd.markForCheck();
      },
      error: () => {
        this.devices = [];
        this.deviceAttributes = new Map();
        this.devicesLoading = false;
        this.devicesLoadError = true;
        this.cd.markForCheck();
      }
    });
  }

  /** 活动告警变化只刷新告警标记，不连带重查设备上下文。 */
  private loadActiveAlarms(): void {
    this.alarmService.getAllAlarmsV2(createSmartLightningAlarmQuery()).pipe(
      map((pageData) => this.toActiveAlarmContext(pageData.data)),
      // 告警请求失败不应阻断设备查询；此时设备先按正常/离线显示。
      catchError(() => of({deviceIds: new Set<string>(), severityByDeviceId: new Map<string, string>()})),
      takeUntil(this.destroy$)
    ).subscribe(({deviceIds, severityByDeviceId}) => {
      this.activeAlarmDeviceIds = deviceIds;
      this.alarmSeverityByDeviceId = severityByDeviceId;
      this.cd.markForCheck();
    });
  }

  /** 直接把实时 active 与 lastActivityTime 应用到当前资产树范围内的设备上下文。 */
  private applyQueryRealtimeChanges(changes: readonly SmartLightningDeviceTelemetryChange[]): void {
    if (!this.devices.length || !changes.length) {
      return;
    }
    const visibleDeviceIds = new Set(this.devices.map((device) => device.id.id));
    const activeUpdates = new Map<string, boolean>();
    const activityUpdates = new Map<string, number>();
    for (const change of changes) {
      if (!visibleDeviceIds.has(change.deviceId)) {
        continue;
      }
      const latestValues = this.realtimeService.getLatestTelemetryValues(change.deviceId);
      const activeValue = String(
        change.values.has('active') ? latestValues.get('active')?.value ?? '' : ''
      ).trim().toLowerCase();
      if (activeValue === 'true' || activeValue === '1') {
        activeUpdates.set(change.deviceId, true);
      } else if (activeValue === 'false' || activeValue === '0') {
        activeUpdates.set(change.deviceId, false);
      }
      const activityTime = change.lastActivityTime == null
        ? undefined
        : this.realtimeService.getLatestActivityTime(change.deviceId);
      if (activityTime != null && Number.isFinite(activityTime) && activityTime > 0) {
        activityUpdates.set(change.deviceId, activityTime);
      }
    }
    if (!activeUpdates.size && !activityUpdates.size) {
      return;
    }

    if (activeUpdates.size) {
      this.devices = this.devices.map((device) => activeUpdates.has(device.id.id)
        ? {...device, active: activeUpdates.get(device.id.id)} as DeviceInfo
        : device);
    }
    const nextAttributes = new Map(this.deviceAttributes);
    for (const deviceId of new Set([...activeUpdates.keys(), ...activityUpdates.keys()])) {
      const current = nextAttributes.get(deviceId) || {};
      nextAttributes.set(deviceId, {
        ...current,
        ...(activeUpdates.has(deviceId) ? {active: activeUpdates.get(deviceId)} : {}),
        ...(activityUpdates.has(deviceId) ? {lastActivityTime: activityUpdates.get(deviceId)} : {})
      });
    }
    this.deviceAttributes = nextAttributes;
    this.cd.markForCheck();
  }

  /** 设备列表 HTTP 加载较慢时，使用共享 WebSocket 缓存补齐已经到达的首帧。 */
  private applyLatestTelemetrySnapshot(): void {
    const changes = this.devices.map((device) => ({
      deviceId: device.id.id,
      values: this.realtimeService.getLatestTelemetryValues(device.id.id),
      lastActivityTime: this.realtimeService.getLatestActivityTime(device.id.id)
    }));
    this.applyQueryRealtimeChanges(changes);
  }

  private toActiveAlarmContext(alarms: AlarmInfo[]): {
    deviceIds: Set<string>;
    severityByDeviceId: Map<string, string>;
  } {
    const deviceIds = new Set<string>();
    const severityByDeviceId = new Map<string, string>();
    const severityRank: Record<string, number> = {
      CRITICAL: 5,
      MAJOR: 4,
      MINOR: 3,
      WARNING: 2,
      INDETERMINATE: 1
    };
    const severityLabels: Record<string, string> = {
      CRITICAL: '紧急',
      MAJOR: '重要',
      MINOR: '次要',
      WARNING: '警告',
      INDETERMINATE: '不确定'
    };
    const severityRanksByDeviceId = new Map<string, number>();
    for (const alarm of alarms) {
      if (alarm.originator?.entityType !== EntityType.DEVICE ||
          (alarm.status !== 'ACTIVE_UNACK' && alarm.status !== 'ACTIVE_ACK')) {
        continue;
      }
      const deviceId = alarm.originator.id;
      const severity = String(alarm.severity || 'INDETERMINATE');
      const rank = severityRank[severity] || 1;
      deviceIds.add(deviceId);
      if ((severityRanksByDeviceId.get(deviceId) || 0) < rank) {
        severityRanksByDeviceId.set(deviceId, rank);
        severityByDeviceId.set(deviceId, severityLabels[severity] || '不确定');
      }
    }
    return {deviceIds, severityByDeviceId};
  }

  private toQueryDeviceContext(contexts: SmartLightningDeviceContext[]): QueryDeviceContext {
    const treeDeviceIds = new Set([...this.assetTree.nodes.values()]
      .filter((node) => node.entityType === EntityType.DEVICE)
      .map((node) => node.id));
    const devices: DeviceInfo[] = [];
    const attributesByDeviceId = new Map<string, SmartLightningQueryDeviceAttributes>();
    for (const context of contexts) {
      if (!treeDeviceIds.has(context.deviceId)) {
        continue;
      }
      // DeviceInfo 是接口而不是可实例化类；这里组装页面所需的最小设备对象。
      const device = {
        id: new DeviceId(context.deviceId),
        name: context.deviceName || context.deviceId,
        label: context.label || '',
        type: context.deviceProfileName || '--',
        deviceProfileName: context.deviceProfileName || '--',
        active: context.active === true,
        customerTitle: '',
        customerIsPublic: false,
        groups: ''
      } as DeviceInfo;
      devices.push(device);
      attributesByDeviceId.set(context.deviceId, {
        active: context.active,
        productName: context.productName,
        functionCategory: context.functionCategory,
        lastActivityTime: context.lastActivityTime,
        deviceProtocol: context.deviceProtocol
      });
    }
    return {devices, attributesByDeviceId};
  }

  /** 递归展开选中节点的全部后代资产。 */
  private openDescendants(id: string): void {
    const node = this.assetTree.nodes.get(id);
    if (!node) {
      return;
    }
    node.childIds.forEach((childId) => {
      this.expandedAssetIds.add(childId);
      this.openDescendants(childId);
    });
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  /** 计算每台设备在资产树中的绝对地址，用于表格悬浮展示。 */
  private buildDevicePaths(): void {
    this.devicePaths.clear();
    const visit = (node: SmartLightningMonitoringAssetNode, parentPath: string): void => {
      const path = parentPath ? `${parentPath}/${node.name}` : node.name;
      if (node.entityType === EntityType.DEVICE) {
        this.devicePaths.set(node.id, path);
      }
      node.childIds.forEach((childId) => {
        const child = this.assetTree.nodes.get(childId);
        if (child) {
          visit(child, path);
        }
      });
    };
    this.assetTree.roots.forEach((root) => visit(root, ''));
  }

}
