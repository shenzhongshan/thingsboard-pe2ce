import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {AlarmService} from '@core/http/alarm.service';
import {AlarmInfo} from '@shared/models/alarm.models';
import {DeviceInfo} from '@shared/models/device.models';
import {EntityType} from '@shared/models/entity-type.models';
import {DeviceId} from '@shared/models/id/device-id';
import {MAX_SAFE_PAGE_SIZE} from '@shared/models/page/page-link';
import {catchError, forkJoin, map, of, Subject, takeUntil} from 'rxjs';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
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

interface QueryDeviceContext {
  devices: DeviceInfo[];
  attributesByDeviceId: Map<string, SmartLightningQueryDeviceAttributes>;
}

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
    private readonly dialog: MatDialog,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAssetTree();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.expandedAssetIds.has(node.id) ? this.expandedAssetIds.delete(node.id) : this.expandedAssetIds.add(node.id);
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.openDescendants(node.id);
    this.selectedRowIds.clear();
    this.page = 1;
    this.cd.markForCheck();
  }

  /** 根资产筛选与节点选择共用同一套递归下属节点逻辑，确保右侧列表同步更新。 */
  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    const roots = getSmartLightningQueryVisibleRoots(this.assetTree, rootAssetId);
    this.selectedAssetIds = new Set(getSmartLightningQueryRootAssetIds(this.assetTree, rootAssetId));
    this.selectedAssetName = rootAssetId && roots[0] ? roots[0].name : '全部根资产';
    roots.forEach((root) => this.openDescendants(root.id));
    this.selectedRowIds.clear();
    this.page = 1;
    this.cd.markForCheck();
  }

  setStatus(status: '全部' | SmartLightningQueryStatus): void {
    this.activeStatus = status;
    this.page = 1;
  }

  query(): void {
    this.selectedRowIds.clear();
    this.page = 1;
  }

  reset(): void {
    this.functionType = '';
    this.productNameFilter = '';
    this.keyword = '';
    this.activeStatus = '全部';
    this.selectedRowIds.clear();
    this.page = 1;
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

  private loadAssetTree(): void {
    this.monitoringApi.getTree().pipe(takeUntil(this.destroy$)).subscribe({
      next: ({roots}) => {
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots);
        this.buildDevicePaths();
        this.assetTree.roots.forEach((node) => this.expandedAssetIds.add(node.id));
        this.expandedAssetIds = new Set(this.expandedAssetIds);
        this.selectRootAsset('');
        this.assetsLoading = false;
        this.loadDevices();
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

  /** 后端批量返回设备上下文，并一次读取当前活动告警用于设备状态标记。 */
  private loadDevices(): void {
    this.devicesLoading = true;
    this.devicesLoadError = false;
    const deviceContexts$ = this.monitoringApi.getLightningDeviceContexts(MAX_SAFE_PAGE_SIZE, 0);
    const activeAlarms$ = this.alarmService.getAllAlarmsV2(createSmartLightningAlarmQuery()).pipe(
      map((pageData) => this.toActiveAlarmContext(pageData.data)),
      // 告警请求失败不应阻断设备查询；此时设备先按正常/离线显示。
      catchError(() => of({deviceIds: new Set<string>(), severityByDeviceId: new Map<string, string>()}))
    );
    forkJoin({devicePage: deviceContexts$, activeAlarms: activeAlarms$}).pipe(
      map(({devicePage, activeAlarms}) => ({
        devices: this.toQueryDeviceContext(devicePage.data),
        activeAlarms
      })),
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({devices, activeAlarms}) => {
        this.devices = devices.devices;
        this.deviceAttributes = devices.attributesByDeviceId;
        this.activeAlarmDeviceIds = activeAlarms.deviceIds;
        this.alarmSeverityByDeviceId = activeAlarms.severityByDeviceId;
        this.devicesLoading = false;
        this.page = 1;
        this.cd.markForCheck();
      },
      error: () => {
        this.devices = [];
        this.deviceAttributes = new Map();
        this.activeAlarmDeviceIds = new Set();
        this.alarmSeverityByDeviceId = new Map();
        this.devicesLoading = false;
        this.devicesLoadError = true;
        this.cd.markForCheck();
      }
    });
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
