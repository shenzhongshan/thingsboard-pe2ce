/** 文件职责：数据监测模块的 smart-lightning-monitoring.component，负责页面状态管理、接口调用与用户交互。 */
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnDestroy,
  OnInit
} from '@angular/core';
import { AlarmService } from '@core/http/alarm.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { auditTime, map, Subject, takeUntil } from 'rxjs';
import { AlarmInfo, AlarmQueryV2, AlarmSearchStatus } from '@shared/models/alarm.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityKeyType } from '@shared/models/query/query.models';
import { DeviceId } from '@shared/models/id/device-id';
import { MAX_SAFE_PAGE_SIZE, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { AlarmCountCmd, EntityDataCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  buildSmartLightningMonitoringDeviceCards,
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
  formatSmartLightningRealtimeValue
} from './smart-lightning-monitoring.data';
import {
  SmartLightningDeviceContext,
  SmartLightningMonitoringApiService,
  SmartLightningRealtimeAnalysis
} from './smart-lightning-monitoring-api.service';
import { SMART_LIGHTNING_UNITS } from '../shared/telemetry/smart-lightning-measurements';

interface MonitoringDeviceContext {
  devices: DeviceInfo[];
  attributesByDeviceId: Map<string, SmartLightningMonitoringDeviceAttributes>;
}

/**
 * 智能防雷数据监测页面组件。
 *
 * <p>左侧共享资产树按资产层级联动，右侧展示同一资产范围的实时分析
 * 与设备总览卡片网格；设备状态按“活动告警优先、其次在线、最后离线”判定。</p>
 */
@Component({
  selector: 'tb-smart-lightning-monitoring',
  templateUrl: './smart-lightning-monitoring.component.html',
  styleUrls: ['./smart-lightning-monitoring.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningMonitoringComponent implements OnInit, OnDestroy {

  /** 页面单位统一引用智能防雷共享定义，避免模板分散硬编码。 */
  readonly units = SMART_LIGHTNING_UNITS;
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
  realtimeAnalysis: SmartLightningRealtimeAnalysis | null = null;
  realtimeAnalysisLoading = true;
  realtimeAnalysisLoadError = false;

  private nodeDevices: DeviceInfo[] = [];
  private nodeDeviceAttributes = new Map<string, SmartLightningMonitoringDeviceAttributes>();
  private nodeDeviceAssetPaths = new Map<string, string>();
  /** 防止快速切换资产时，较早返回的请求覆盖当前选中范围。 */
  private realtimeAnalysisRequestId = 0;
  private selectedRealtimeAssetId?: string;
  private realtimeEntitySubscriber?: TelemetrySubscriber;
  private realtimeAlarmSubscriber?: TelemetrySubscriber;
  /** 多个设备同时上报时合并为一次刷新，避免瞬时重复请求聚合接口。 */
  private readonly realtimeRefresh$ = new Subject<void>();

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly alarmService: AlarmService,
    private readonly telemetryWebsocketService: TelemetryWebsocketService,
    private readonly ngZone: NgZone,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.realtimeRefresh$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadRealtimeAnalysis(this.selectedRealtimeAssetId, true);
      }
    });
    this.subscribeToRealtimeChanges();
    this.loadAssetTree();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeEntitySubscriber?.unsubscribe();
    this.realtimeAlarmSubscriber?.unsubscribe();
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
    this.loadRealtimeAnalysis(node.id);
    this.cd.markForCheck();
  }

  /** 根资产筛选与设备总览、告警统计使用同一组选中节点。 */
  selectRootAsset(rootAssetId: string, expandDescendants = true): void {
    this.selectedRootAssetId = rootAssetId;
    const roots = getSmartLightningMonitoringVisibleRoots(this.assetTree, rootAssetId);
    this.selectedAssetIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    this.selectedAssetName = rootAssetId && roots[0] ? roots[0].name : '全部资产';
    if (expandDescendants) {
      roots.forEach((root) => this.openDescendants(root.id));
    }
    this.devicePage = 1;
    this.loadRealtimeAnalysis(rootAssetId || undefined);
    this.cd.markForCheck();
  }

  /** 资产树一键全选：保留当前根资产筛选范围并选中其全部下属节点。 */
  selectAllAssets(): void {
    this.selectRootAsset(this.selectedRootAssetId);
  }

  /** 实时数值统一显示两位小数；无遥测显示 --，零保留为 0.00。 */
  formatRealtimeValue(value: number | null | undefined, unit = ''): string {
    return formatSmartLightningRealtimeValue(value, unit);
  }

  /** 累计次数为整数，无数据由后端返回零。 */
  formatRealtimeCount(value: number | null | undefined): string {
    return value == null || !Number.isFinite(value) ? '--' : `${Math.round(value)}${this.units.count}`;
  }

  /** 格式化接口毫秒时间戳，供悬浮详情展示来源时间。 */
  formatRealtimeTime(value: number | null | undefined): string {
    if (value == null || !Number.isFinite(value)) {
      return '--';
    }
    const date = new Date(value);
    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
      `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  /** 将 leakageA/B/C 等规范键的后缀转换为悬浮详情中的中文相位。 */
  realtimeParameterLabel(parameter: string | null | undefined): string {
    const suffix = parameter?.trim().slice(-1).toUpperCase();
    return suffix && ['A', 'B', 'C'].includes(suffix) ? `${suffix}相` : (parameter || '--');
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

  /**
   * 根据当前资产重新请求实时分析；后端负责同一范围内的权限、告警和遥测聚合。
   * 请求序号用于丢弃快速切换资产后返回的旧响应。
   */
  private loadRealtimeAnalysis(assetId?: string, preserveCurrentData = false): void {
    this.selectedRealtimeAssetId = assetId;
    const requestId = ++this.realtimeAnalysisRequestId;
    if (!preserveCurrentData) {
      this.realtimeAnalysis = null;
    }
    this.realtimeAnalysisLoading = this.realtimeAnalysis == null;
    this.realtimeAnalysisLoadError = false;
    this.monitoringApi.getRealtimeAnalysis(assetId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (analysis) => {
        if (requestId !== this.realtimeAnalysisRequestId) {
          return;
        }
        this.realtimeAnalysis = analysis;
        this.realtimeAnalysisLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        if (requestId !== this.realtimeAnalysisRequestId) {
          return;
        }
        if (!preserveCurrentData) {
          this.realtimeAnalysis = null;
          this.realtimeAnalysisLoadError = true;
        }
        this.realtimeAnalysisLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  /**
   * 订阅设备在线属性、规范遥测和活动告警数量变化。
   * WebSocket 只负责通知数据失效，最终展示值仍统一由后端聚合接口计算。
   */
  private subscribeToRealtimeChanges(): void {
    const latestValues = [
      {key: 'active', type: EntityKeyType.SERVER_ATTRIBUTE},
      ...[
        'spdStatus', 'groundStatus',
        'temperatureA', 'temperatureB', 'temperatureC', 'temperatureD',
        'lifePercent', 'leakageA', 'leakageB', 'leakageC',
        'groundResistance', 'surgeCount', 'surgePeak', 'surgePolarity',
        'deviceStatus', 'arcFaultCount'
      ].map((key) => ({key, type: EntityKeyType.TIME_SERIES}))
    ];
    const entityCommand = new EntityDataCmd();
    entityCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      // WebSocket 实体订阅使用明确上限，避免把前端通用“安全最大值”传给订阅分页。
      pageLink: {pageSize: 10_000, page: 0},
      latestValues
    };
    entityCommand.latestCmd = {keys: latestValues};
    this.realtimeEntitySubscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    this.realtimeEntitySubscriber.subscriptionCommands.push(entityCommand);
    this.realtimeEntitySubscriber.entityData$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.realtimeRefresh$.next();
    });
    this.realtimeEntitySubscriber.subscribe();

    const alarmCommand = new AlarmCountCmd();
    alarmCommand.query = {
      entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE},
      statusList: [AlarmSearchStatus.ACTIVE]
    };
    this.realtimeAlarmSubscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    this.realtimeAlarmSubscriber.subscriptionCommands.push(alarmCommand);
    this.realtimeAlarmSubscriber.alarmCount$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.realtimeRefresh$.next();
      if (!this.assetsLoading) {
        this.loadNodeAlarms();
      }
    });
    this.realtimeAlarmSubscriber.subscribe();
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

  /** 复用 ThingsBoard 原生告警接口读取活动告警，仅用于设备卡片和资产树告警标记。 */
  private loadNodeAlarms(): void {
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    const sortOrder = {property: 'createdTime', direction: Direction.DESC};
    const activeQuery = new AlarmQueryV2(
      null,
      new TimePageLink(MAX_SAFE_PAGE_SIZE, 0, null, sortOrder),
      {statusList: [AlarmSearchStatus.ACTIVE]}
    );
    this.alarmService.getAllAlarmsV2(activeQuery).pipe(
      map((page) => this.uniqueAlarms(page.data)),
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
