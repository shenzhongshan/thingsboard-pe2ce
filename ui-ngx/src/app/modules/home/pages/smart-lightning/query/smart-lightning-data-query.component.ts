import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {select, Store} from '@ngrx/store';
import {AttributeService} from '@core/http/attribute.service';
import {DeviceService} from '@core/http/device.service';
import {selectAuthUser} from '@core/auth/auth.selectors';
import {AppState} from '@core/core.state';
import {Authority} from '@shared/models/authority.enum';
import {DeviceInfo, DeviceInfoQuery} from '@shared/models/device.models';
import {EntityType} from '@shared/models/entity-type.models';
import {CustomerId} from '@shared/models/id/customer-id';
import {MAX_SAFE_PAGE_SIZE, PageLink} from '@shared/models/page/page-link';
import {AttributeScope} from '@shared/models/telemetry/telemetry.models';
import {catchError, forkJoin, map, of, Subject, switchMap, take, takeUntil} from 'rxjs';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '../monitoring/smart-lightning-monitoring.data';
import {SmartLightningMonitoringApiService} from '../monitoring/smart-lightning-monitoring-api.service';
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
  readonly statuses: Array<'全部' | SmartLightningQueryStatus> = ['全部', '在线', '离线', '未连接'];

  private devices: DeviceInfo[] = [];
  private deviceAttributes = new Map<string, SmartLightningQueryDeviceAttributes>();
  private readonly devicePaths = new Map<string, string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly deviceService: DeviceService,
    private readonly attributeService: AttributeService,
    private readonly store: Store<AppState>,
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

  get visibleAssetNodes(): Array<{node: SmartLightningMonitoringAssetNode; depth: number}> {
    const output: Array<{node: SmartLightningMonitoringAssetNode; depth: number}> = [];
    const query = this.assetSearch.trim().toLowerCase();
    const includes = (node: SmartLightningMonitoringAssetNode): boolean =>
      node.name.toLowerCase().includes(query) || node.childIds.some((id) => {
        const child = this.assetTree.nodes.get(id);
        return child ? includes(child) : false;
      });
    const append = (node: SmartLightningMonitoringAssetNode, depth: number): void => {
      if (query && !includes(node)) {
        return;
      }
      output.push({node, depth});
      if (query || this.expandedAssetIds.has(node.id)) {
        node.childIds.forEach((id) => {
          const child = this.assetTree.nodes.get(id);
          if (child) {
            append(child, depth + 1);
          }
        });
      }
    };
    getSmartLightningQueryVisibleRoots(this.assetTree, this.selectedRootAssetId)
      .forEach((node) => append(node, 0));
    return output;
  }

  get rows(): SmartLightningQueryRow[] {
    const selectedDeviceIds = new Set([...this.selectedAssetIds]
      .filter((id) => this.assetTree.nodes.get(id)?.entityType === EntityType.DEVICE));
    return buildSmartLightningQueryRows(
      this.devices.map((device) => ({
        id: device.id.id,
        name: device.name,
        label: device.label,
        deviceProfileName: device.deviceProfileName || device.type || '--',
        protocol: device.deviceData?.transportConfiguration?.type,
        active: device.active
      })),
      selectedDeviceIds,
      this.devicePaths,
      this.deviceAttributes
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

  toggleAssetNode(event: Event, node: SmartLightningMonitoringAssetNode): void {
    event.stopPropagation();
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

  /**
   * 读取当前账号可见设备，再读取页面需要的服务器属性；单台属性失败不会阻断整张表。
   * 未连接状态严格由 lastActivityTime 为空判定。
   */
  private loadDevices(): void {
    this.devicesLoading = true;
    this.devicesLoadError = false;
    this.store.pipe(
      select(selectAuthUser),
      take(1),
      switchMap((authUser) => {
        const filter = authUser.authority === Authority.CUSTOMER_USER
          ? {customerId: new CustomerId(authUser.customerId)}
          : {};
        return this.deviceService.getDeviceInfosByQuery(new DeviceInfoQuery(new PageLink(MAX_SAFE_PAGE_SIZE), filter));
      }),
      switchMap((pageData) => {
        const treeDeviceIds = new Set([...this.assetTree.nodes.values()]
          .filter((node) => node.entityType === EntityType.DEVICE)
          .map((node) => node.id));
        const devices = pageData.data.filter((device) => treeDeviceIds.has(device.id.id));
        if (!devices.length) {
          return of<QueryDeviceContext>({devices: [], attributesByDeviceId: new Map()});
        }
        return forkJoin(devices.map((device) => this.attributeService.getEntityAttributes(
          device.id,
          AttributeScope.SERVER_SCOPE,
          ['active', 'functionCategory', 'productName', 'lastActivityTime', 'deviceProtocol']
        ).pipe(
          map((attributes) => [device.id.id, Object.fromEntries(
            attributes.map((attribute) => [attribute.key, attribute.value])
          ) as SmartLightningQueryDeviceAttributes] as const),
          catchError(() => of([device.id.id, {}] as const))
        ))).pipe(map((entries) => ({
          devices,
          attributesByDeviceId: new Map<string, SmartLightningQueryDeviceAttributes>(entries)
        })));
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({devices, attributesByDeviceId}) => {
        this.devices = devices;
        this.deviceAttributes = attributesByDeviceId;
        this.devicesLoading = false;
        this.page = 1;
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
