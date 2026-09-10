/** 文件职责：数据监测模块的 smart-lightning-monitoring.component，负责页面状态管理、接口调用与用户交互。 */
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { AlarmService } from '@core/http/alarm.service';
import { forkJoin, map, Subject, takeUntil } from 'rxjs';
import { AlarmInfo, AlarmQueryV2, AlarmSearchStatus } from '@shared/models/alarm.models';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceId } from '@shared/models/id/device-id';
import { MAX_SAFE_PAGE_SIZE, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  buildSmartLightningMonitoringDeviceCards,
  calculateSmartLightningMonitoringAlarmSummary,
  getSmartLightningActiveAlarmDeviceIds,
  getSmartLightningDefaultExpandedAssetIds,
  getSmartLightningMonitoringAssetPaths,
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
import { SmartLightningDeviceContext, SmartLightningMonitoringApiService } from './smart-lightning-monitoring-api.service';

interface MonitoringDeviceContext {
  devices: DeviceInfo[];
  attributesByDeviceId: Map<string, SmartLightningMonitoringDeviceAttributes>;
}

/**
 * 智能防雷数据监测页面组件。
 *
 * <p>左侧共享资产树按资产层级联动，右侧展示能力分析卡、选中节点
 * 统计区（8 项指标）与设备总览卡片网格；设备状态按“活动告警优先、
 * 其次在线、最后离线”判定。</p>
 */
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
  /** 资产树设备节点的状态标记：存在未清除告警时显示红色竖条。 */
  activeAlarmDeviceIds = new Set<string>();
  alarmsLoading = true;
  alarmsLoadError = false;
  readonly analysisCards = SMART_LIGHTNING_ANALYSIS_CARDS;

  private nodeDevices: DeviceInfo[] = [];
  private nodeDeviceAttributes = new Map<string, SmartLightningMonitoringDeviceAttributes>();
  private nodeDeviceAssetPaths = new Map<string, string>();

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly alarmService: AlarmService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAssetTree();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

  /** 展开/折叠资产节点，同步为不可变集合触发变更检测。 */
  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    if (this.expandedAssetIds.has(node.id)) {
      this.expandedAssetIds.delete(node.id);
    } else {
      this.expandedAssetIds.add(node.id);
    }
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  /** 选中资产节点：选中集合包含其全部后代，用于设备总览与统计范围。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.devicePage = 1;
    this.openDescendants(node.id);
    this.cd.markForCheck();
  }

  /** 根资产筛选与设备总览、告警统计使用同一组选中节点。 */
  selectRootAsset(rootAssetId: string, expandDescendants = true): void {
    this.selectedRootAssetId = rootAssetId;
    const roots = getSmartLightningMonitoringVisibleRoots(this.assetTree, rootAssetId);
    this.selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    this.selectedAssetName = rootAssetId && roots[0] ? roots[0].name : '全部根资产';
    if (expandDescendants) {
      roots.forEach((root) => this.openDescendants(root.id));
    }
    this.devicePage = 1;
    this.cd.markForCheck();
  }

  /** 切换设备总览的功能分类页签并回到第一页。 */
  setCategory(category: string): void {
    this.activeCategory = category;
    this.devicePage = 1;
  }

  /** 设备搜索词变化时重置分页。 */
  resetDevicePage(): void {
    this.devicePage = 1;
  }

  /** 设备卡片分页翻页，页码由分页工具函数钳制。 */
  setDevicePage(page: number): void {
    this.devicePage = getSmartLightningMonitoringPage(
      this.visibleMonitoringCards,
      page,
      this.devicePageSize
    ).currentPage;
  }

  /** 加载共享资产树并初始化展开状态与根资产选择，失败时标记错误状态。 */
  private loadAssetTree(): void {
    this.assetsLoading = true;
    this.assetsLoadError = false;
    this.monitoringApi.getTree().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({roots}) => {
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots);
        this.nodeDeviceAssetPaths = getSmartLightningMonitoringAssetPaths(this.assetTree);
        this.expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
        this.selectRootAsset('', false);
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

  /** 后端一次返回设备基础信息与服务器属性，前端只按资产树节点做范围过滤。 */
  private loadNodeDeviceCards(): void {
    this.devicesLoading = true;
    this.devicesLoadError = false;
    this.monitoringApi.getLightningDeviceContexts(MAX_SAFE_PAGE_SIZE, 0).pipe(
      map((pageData) => this.toMonitoringDeviceContext(pageData.data)),
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

  private toMonitoringDeviceContext(contexts: SmartLightningDeviceContext[]): MonitoringDeviceContext {
    const treeDeviceIds = new Set([...this.assetTree.nodes.values()]
      .filter((node) => node.entityType === EntityType.DEVICE)
      .map((node) => node.id));
    const devices: DeviceInfo[] = [];
    const attributesByDeviceId = new Map<string, SmartLightningMonitoringDeviceAttributes>();
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
        functionCategory: context.functionCategory,
        productName: context.productName,
        lastActivityTime: context.lastActivityTime
      });
    }
    return {devices, attributesByDeviceId};
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

  /** 返回今天 0 点的毫秒时间戳，用于“当日报警”统计窗口。 */
  private startOfToday(): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today.getTime();
  }

  /** 活动告警优先于在线状态展示，避免有告警的在线设备仍被误标为“正常”。 */
  private rebuildNodeDeviceCards(): void {
    this.activeAlarmDeviceIds = getSmartLightningActiveAlarmDeviceIds(
      this.nodeAlarms,
      new Set(this.nodeDevices.map((device) => device.id?.id).filter((id): id is string => !!id))
    );
    this.nodeDeviceCards = buildSmartLightningMonitoringDeviceCards(
      this.nodeDevices,
      new Set(this.assetTree.nodes.keys()),
      this.nodeDeviceAttributes,
      this.activeAlarmDeviceIds,
      this.nodeDeviceAssetPaths
    );
  }

  /** 递归展开选中节点的全部后代资产，保证新选中子树完整可见。 */
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
