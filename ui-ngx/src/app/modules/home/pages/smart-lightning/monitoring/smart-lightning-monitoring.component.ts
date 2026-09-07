import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { AttributeService } from '@core/http/attribute.service';
import { AlarmService } from '@core/http/alarm.service';
import { DeviceService } from '@core/http/device.service';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { catchError, forkJoin, map, of, Subject, switchMap, take, takeUntil } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { Authority } from '@shared/models/authority.enum';
import { AlarmInfo, AlarmQueryV2, AlarmSearchStatus } from '@shared/models/alarm.models';
import { DeviceInfo, DeviceInfoQuery } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { CustomerId } from '@shared/models/id/customer-id';
import { MAX_SAFE_PAGE_SIZE, PageLink, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  buildSmartLightningMonitoringDeviceCards,
  calculateSmartLightningMonitoringAlarmSummary,
  getSmartLightningMonitoringPage,
  getSmartLightningMonitoringRootAssetIds,
  getSmartLightningMonitoringVisibleRoots,
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree,
  SmartLightningMonitoringDeviceAttributes,
  SmartLightningMonitoringDeviceCard,
  SmartLightningMonitoringAlarmRecord,
  SmartLightningMonitoringAlarmSummary,
  SMART_LIGHTNING_ANALYSIS_CARDS
} from './smart-lightning-monitoring.data';
import { SmartLightningMonitoringApiService } from './smart-lightning-monitoring-api.service';

interface MonitoringDeviceContext {
  devices: DeviceInfo[];
  attributesByDeviceId: Map<string, SmartLightningMonitoringDeviceAttributes>;
}

@Component({
  selector: 'tb-smart-lightning-monitoring',
  templateUrl: './smart-lightning-monitoring.component.html',
  styleUrls: ['./smart-lightning-monitoring.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningMonitoringComponent implements OnInit, OnDestroy {

  assetTree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  expandedAssetIds = new Set<string>();
  selectedAssetIds = new Set<string>();
  selectedRootAssetId = '';
  selectedAssetName = '全部资产';
  assetSearch = '';
  assetsLoading = true;
  assetsLoadError = false;
  activeCategory = '全部';
  deviceSearch = '';
  readonly devicePageSize = 10;
  devicePage = 1;
  nodeDeviceCards: SmartLightningMonitoringDeviceCard[] = [];
  devicesLoading = true;
  devicesLoadError = false;
  nodeAlarms: SmartLightningMonitoringAlarmRecord[] = [];
  alarmsLoading = true;
  alarmsLoadError = false;
  readonly analysisCards = SMART_LIGHTNING_ANALYSIS_CARDS;

  private nodeDevices: DeviceInfo[] = [];
  private nodeDeviceAttributes = new Map<string, SmartLightningMonitoringDeviceAttributes>();

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly attributeService: AttributeService,
    private readonly alarmService: AlarmService,
    private readonly deviceService: DeviceService,
    private readonly store: Store<AppState>,
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
    const result: Array<{node: SmartLightningMonitoringAssetNode; depth: number}> = [];
    const query = this.assetSearch.trim().toLowerCase();
    const hasVisibleDescendant = (node: SmartLightningMonitoringAssetNode): boolean =>
      node.name.toLowerCase().includes(query) || node.childIds.some((id) => {
        const child = this.assetTree.nodes.get(id);
        return child ? hasVisibleDescendant(child) : false;
      });
    const append = (node: SmartLightningMonitoringAssetNode, depth: number) => {
      if (query && !hasVisibleDescendant(node)) {
        return;
      }
      result.push({node, depth});
      const shouldShowChildren = query || this.expandedAssetIds.has(node.id);
      if (shouldShowChildren) {
        node.childIds.forEach((id) => {
          const child = this.assetTree.nodes.get(id);
          if (child) {
            append(child, depth + 1);
          }
        });
      }
    };
    getSmartLightningMonitoringVisibleRoots(this.assetTree, this.selectedRootAssetId)
      .forEach((node) => append(node, 0));
    return result;
  }

  get categories(): string[] {
    return ['全部', ...new Set(this.nodeDeviceCards.map((card) => card.category))];
  }

  get selectedNodeDeviceCards(): SmartLightningMonitoringDeviceCard[] {
    return this.nodeDeviceCards.filter((card) => this.selectedAssetIds.has(card.id));
  }

  get visibleMonitoringCards(): SmartLightningMonitoringDeviceCard[] {
    const search = this.deviceSearch.trim().toLowerCase();
    return this.selectedNodeDeviceCards.filter((card) =>
      (this.activeCategory === '全部' || card.category === this.activeCategory) &&
      (!search || `${card.name} ${card.category}`.toLowerCase().includes(search))
    );
  }

  get devicePagination() {
    return getSmartLightningMonitoringPage(this.visibleMonitoringCards, this.devicePage, this.devicePageSize);
  }

  get pagedMonitoringCards(): SmartLightningMonitoringDeviceCard[] {
    return this.devicePagination.items;
  }

  get selectedOnlineDeviceCount(): number {
    return this.selectedNodeDeviceCards.filter((card) => card.status === '正常').length;
  }

  get selectedOfflineDeviceCount(): number {
    return this.selectedNodeDeviceCards.filter((card) => card.status === '离线').length;
  }

  get selectedAlarmSummary(): SmartLightningMonitoringAlarmSummary {
    return calculateSmartLightningMonitoringAlarmSummary(
      this.nodeAlarms,
      new Set(this.selectedNodeDeviceCards.map((card) => card.id)),
      this.startOfToday()
    );
  }

  toggleAssetNode(event: Event, node: SmartLightningMonitoringAssetNode): void {
    event.stopPropagation();
    if (this.expandedAssetIds.has(node.id)) {
      this.expandedAssetIds.delete(node.id);
    } else {
      this.expandedAssetIds.add(node.id);
    }
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.devicePage = 1;
    this.openDescendants(node.id);
    this.cd.markForCheck();
  }

  /** 根资产筛选与设备总览、告警统计使用同一组选中节点。 */
  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    const roots = getSmartLightningMonitoringVisibleRoots(this.assetTree, rootAssetId);
    this.selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    this.selectedAssetName = rootAssetId && roots[0] ? roots[0].name : '全部根资产';
    roots.forEach((root) => this.openDescendants(root.id));
    this.devicePage = 1;
    this.cd.markForCheck();
  }

  setCategory(category: string): void {
    this.activeCategory = category;
    this.devicePage = 1;
  }

  resetDevicePage(): void {
    this.devicePage = 1;
  }

  setDevicePage(page: number): void {
    this.devicePage = getSmartLightningMonitoringPage(
      this.visibleMonitoringCards,
      page,
      this.devicePageSize
    ).currentPage;
  }

  private loadAssetTree(): void {
    this.assetsLoading = true;
    this.assetsLoadError = false;
    this.monitoringApi.getTree().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({roots}) => {
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots);
        this.assetTree.roots.forEach((node) => this.expandedAssetIds.add(node.id));
        this.expandedAssetIds = new Set(this.expandedAssetIds);
        this.selectRootAsset('');
        this.loadNodeDeviceCards();
        this.loadNodeAlarms();
        this.assetsLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.assetsLoadError = true;
        this.assetsLoading = false;
        this.devicesLoading = false;
        this.alarmsLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  /**
   * 先取得当前登录用户可见的设备，再以资产树中的设备节点为准筛选；
   * 这样既不会显示树外设备，也不会因为当前选中资产变化而重复请求服务器属性。
   */
  private loadNodeDeviceCards(): void {
    this.devicesLoading = true;
    this.devicesLoadError = false;
    this.store.pipe(
      select(selectAuthUser),
      take(1),
      switchMap((authUser) => {
        const filter = authUser.authority === Authority.CUSTOMER_USER
          ? {customerId: new CustomerId(authUser.customerId)}
          : {};
        return this.deviceService.getDeviceInfosByQuery(new DeviceInfoQuery(
          new PageLink(MAX_SAFE_PAGE_SIZE),
          filter
        ));
      }),
      switchMap((pageData) => {
        const treeDeviceIds = new Set([...this.assetTree.nodes.values()]
          .filter((node) => node.entityType === EntityType.DEVICE)
          .map((node) => node.id));
        const devices = pageData.data.filter((device) => treeDeviceIds.has(device.id.id));
        if (!devices.length) {
          return of<MonitoringDeviceContext>({devices: [], attributesByDeviceId: new Map()});
        }
        return forkJoin(devices.map((device) => this.attributeService.getEntityAttributes(
          device.id,
          AttributeScope.SERVER_SCOPE,
          ['active', 'functionCategory', 'productName', 'lastActivityTime']
        ).pipe(
          map((attributes) => [device.id.id, {
            active: attributes.find((attribute) => attribute.key === 'active')?.value,
            functionCategory: attributes.find((attribute) => attribute.key === 'functionCategory')?.value,
            productName: attributes.find((attribute) => attribute.key === 'productName')?.value,
            lastActivityTime: attributes.find((attribute) => attribute.key === 'lastActivityTime')?.value
          }] as const),
          // 单个设备属性读取失败时保留卡片，并以离线/未分类展示，不影响其他设备。
          catchError(() => of([device.id.id, {}] as const))
        ))).pipe(map((entries) => ({
          devices,
          attributesByDeviceId: new Map<string, SmartLightningMonitoringDeviceAttributes>(entries)
        })));
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({devices, attributesByDeviceId}) => {
        this.nodeDevices = devices;
        this.nodeDeviceAttributes = attributesByDeviceId;
        this.rebuildNodeDeviceCards();
        if (!this.categories.includes(this.activeCategory)) {
          this.activeCategory = '全部';
        }
        this.devicesLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.nodeDevices = [];
        this.nodeDeviceAttributes = new Map();
        this.nodeDeviceCards = [];
        this.devicesLoadError = true;
        this.devicesLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  /**
   * 复用 ThingsBoard 原生告警接口：一条查询当前活动告警，一条查询今日告警。
   * 两条结果合并后，在 selectedAlarmSummary 中按当前选中资产子树过滤。
   */
  private loadNodeAlarms(): void {
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    const startOfToday = this.startOfToday();
    const sortOrder = {property: 'createdTime', direction: Direction.DESC};
    const activeQuery = new AlarmQueryV2(
      null,
      new TimePageLink(MAX_SAFE_PAGE_SIZE, 0, null, sortOrder),
      {statusList: [AlarmSearchStatus.ACTIVE]}
    );
    const todayQuery = new AlarmQueryV2(
      null,
      new TimePageLink(MAX_SAFE_PAGE_SIZE, 0, null, sortOrder, startOfToday, Date.now()),
      {}
    );
    forkJoin({
      active: this.alarmService.getAllAlarmsV2(activeQuery),
      today: this.alarmService.getAllAlarmsV2(todayQuery)
    }).pipe(
      map(({active, today}) => this.uniqueAlarms([...active.data, ...today.data])),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (alarms) => {
        this.nodeAlarms = alarms;
        this.rebuildNodeDeviceCards();
        this.alarmsLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.nodeAlarms = [];
        this.rebuildNodeDeviceCards();
        this.alarmsLoadError = true;
        this.alarmsLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  private uniqueAlarms(alarms: AlarmInfo[]): AlarmInfo[] {
    const unique = new Map<string, AlarmInfo>();
    alarms.forEach((alarm) => {
      const fallbackKey = `${alarm.originator?.id || ''}:${alarm.startTs}:${alarm.type || ''}`;
      unique.set(alarm.id?.id || fallbackKey, alarm);
    });
    return [...unique.values()];
  }

  private startOfToday(): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today.getTime();
  }

  /** 活动告警优先于在线状态展示，避免有告警的在线设备仍被误标为“正常”。 */
  private rebuildNodeDeviceCards(): void {
    const activeAlarmDeviceIds = new Set(this.nodeAlarms
      .filter((alarm) => alarm.status === 'ACTIVE_UNACK' || alarm.status === 'ACTIVE_ACK')
      .map((alarm) => alarm.originator?.id)
      .filter((id): id is string => !!id));
    this.nodeDeviceCards = buildSmartLightningMonitoringDeviceCards(
      this.nodeDevices,
      new Set(this.assetTree.nodes.keys()),
      this.nodeDeviceAttributes,
      activeAlarmDeviceIds
    );
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

}
