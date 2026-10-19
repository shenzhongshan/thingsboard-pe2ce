/** 文件职责：数据监测模块的 smart-lightning-monitoring.component，负责页面状态管理、接口调用与用户交互。 */
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {AssetTreeSelectionStateService} from '../shared/asset-tree/asset-tree-selection-state.service';
import { auditTime, bufferTime, map, Subject, takeUntil } from 'rxjs';
import { AlarmInfo, AlarmQueryV2, AlarmSearchStatus } from '@shared/models/alarm.models';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceId } from '@shared/models/id/device-id';
import { MAX_SAFE_PAGE_SIZE, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
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
  SmartLightningMonitoringRealtimeMetric,
  SmartLightningMonitoringTelemetryValue,
  SmartLightningMonitoringAlarmRecord,
  formatSmartLightningRealtimeValue
} from './smart-lightning-monitoring.data';
import {buildSmartLightningMonitoringRealtimeAnalysis} from './smart-lightning-monitoring-realtime.data';
import {formatSmartLightningPolarityLabel} from './smart-lightning-device-detail.data';
import {
  SmartLightningDeviceContext,
  SmartLightningMonitoringApiService,
  SmartLightningRealtimeAnalysis
} from './smart-lightning-monitoring-api.service';
import {
  SmartLightningDeviceTelemetryChange,
  SmartLightningRealtimeService
} from '../shared/realtime/smart-lightning-realtime.service';
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
  /** 当前手动选中的资产节点，用于进入设备详情后恢复原资产树选择。 */
  selectedTreeNodeId = '';
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
  /** 当前资产树内设备的 ID 集合，用于只刷新页面可见设备的卡片遥测。 */
  private nodeDeviceIds = new Set<string>();
  /** 当前树内设备的最新遥测快照；只缓存卡片所需设备，不重复保存页面外的数据。 */
  private nodeDeviceTelemetryById = new Map<string, ReadonlyMap<string, SmartLightningMonitoringTelemetryValue>>();
  /** 防止快速切换资产时，较早返回的请求覆盖当前选中范围。 */
  private realtimeAnalysisRequestId = 0;
  private selectedRealtimeAssetId?: string;
  /** 设备/资产成员变化合并后重载资产上下文，保留当前树选择。 */
  private readonly membershipRefresh$ = new Subject<void>();
  /** 记录汇总 HTTP 快照的资产范围，避免范围切换时复用旧范围的电弧上报时间。 */
  private realtimeAnalysisAssetId?: string;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly realtimeService: SmartLightningRealtimeService,
    private readonly route: ActivatedRoute,
    private readonly assetSelectionState: AssetTreeSelectionStateService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // 复用全局 WebSocket 遥测增量；批量合并短时间内的多设备上报，避免逐条重建卡片。
    this.realtimeService.deviceTelemetryChanged$.pipe(
      bufferTime(100),
      takeUntil(this.destroy$)
    ).subscribe((changes) => {
      if (changes.length) {
        this.applyRealtimeTelemetryChanges(changes);
      }
    });
    // 完整首帧到达后，才允许用本地 WebSocket 缓存重算整页实时统计。
    this.realtimeService.entitySnapshotReady$.pipe(
      takeUntil(this.destroy$)
    ).subscribe((ready) => {
      if (ready) {
        this.rebuildRealtimeAnalysisFromSnapshot();
      }
    });
    // 属性或名称变化影响设备分类/展示信息，但普通遥测不会进入这个事件流。
    this.realtimeService.deviceContextIncrementalChanged$.pipe(
      auditTime(500),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadNodeDeviceCards();
      }
    });
    this.realtimeService.deviceMembershipChanged$.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.membershipRefresh$.next());
    this.realtimeService.assetMembershipChanged$.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.membershipRefresh$.next());
    this.membershipRefresh$.pipe(auditTime(500), takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.assetsLoading) {
          this.loadAssetTree(true);
        }
      });
    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadNodeAlarms();
      }
    });
    this.realtimeService.acquire();
    this.loadAssetTree();
  }

  ngOnDestroy(): void {
    // 顶部导航不携带详情页的 returnAssetId；离开时独立保存最近一次有效树状态。
    if (this.assetTree.nodes.size && !this.assetsLoadError) {
      this.assetSelectionState.save('/smart-lightning/monitoring', {
        rootAssetId: this.selectedRootAssetId,
        selectedNodeId: this.selectedTreeNodeId,
        expandedIds: [...this.expandedAssetIds]
      });
    }
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
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
    this.selectedTreeNodeId = node.id;
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
    this.selectedTreeNodeId = '';
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

  /** 实时数值保留接口返回的小数位；无遥测显示 --。 */
  formatRealtimeValue(value: number | null | undefined, unit = ''): string {
    return formatSmartLightningRealtimeValue(value, unit);
  }

  /** 累计次数保留接口返回值，无数据由后端返回零。 */
  formatRealtimeCount(value: number | null | undefined): string {
    return value == null || !Number.isFinite(value) ? '--' : `${value}${this.units.count}`;
  }

  /** 复用雷电流测点的极性映射，仅转换悬浮提示文案，不改变接口值。 */
  formatRealtimePolarity(value: unknown): string {
    return formatSmartLightningPolarityLabel(value);
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

  /** 仅识别完整标准漏流键 LeakCurrA/B/C，避免对任意键名末尾进行相位猜测。 */
  realtimeParameterLabel(parameter: string | null | undefined): string {
    const phaseMatch = parameter?.trim().match(/^LeakCurr([ABC])$/i);
    return phaseMatch ? `${phaseMatch[1].toUpperCase()}相` : (parameter || '--');
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
  private loadAssetTree(preserveCurrentSelection = false): void {
    const previousSelectedNodeId = this.selectedTreeNodeId;
    const previousRootAssetId = this.selectedRootAssetId;
    this.assetsLoading = true;
    this.assetsLoadError = false;
    this.monitoringApi.getTree().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ({roots}) => {
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots);
        this.nodeDeviceAssetPaths = getSmartLightningMonitoringAssetPaths(this.assetTree);
        this.prepareNodeDeviceTelemetry();
        this.expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
        if (preserveCurrentSelection) {
          this.restoreCurrentAssetSelection(previousRootAssetId, previousSelectedNodeId);
        } else {
          this.restoreAssetSelection();
        }
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

  /** WebSocket 检测到实体数量变化后重载树，并优先恢复变化前的节点/根资产选择。 */
  private restoreCurrentAssetSelection(rootId: string, nodeId: string): void {
    const validRootId = this.assetTree.roots.some((root) => root.id === rootId) ? rootId : '';
    const selectedNode = this.assetTree.nodes.get(nodeId);
    const rootNodeIds = validRootId ? getSmartLightningMonitoringRootAssetIds(this.assetTree, validRootId) : [];
    if (selectedNode && selectedNode.entityType === EntityType.ASSET &&
        (!validRootId || rootNodeIds.includes(selectedNode.id))) {
      this.selectedRootAssetId = validRootId;
      this.openPathTo(selectedNode.id);
      this.selectAssetNode(selectedNode);
      return;
    }
    this.selectRootAsset(validRootId, false);
  }

  /**
   * 页面从设备详情返回时，根据 URL 中保存的节点和根资产筛选恢复选择。
   * 节点不存在或与根资产筛选不匹配时，安全回退到有效根节点或全部资产。
   */
  private restoreAssetSelection(): void {
    const params = this.route.snapshot.queryParamMap;
    const explicitSelection = params.has('selectedRootAssetId') || params.has('selectedAssetId');
    const remembered = explicitSelection ? undefined : this.assetSelectionState.get('/smart-lightning/monitoring');
    // 明确的链接参数优先；顶部导航没有参数时才恢复本页面最近的选择。
    const requestedRootId = explicitSelection
      ? params.get('selectedRootAssetId') || '' : remembered?.rootAssetId || '';
    const rootAssetId = this.assetTree.roots.some((root) => root.id === requestedRootId)
      ? requestedRootId
      : '';
    const requestedNodeId = explicitSelection
      ? params.get('selectedAssetId') || '' : remembered?.selectedNodeId || '';
    const selectedNode = this.assetTree.nodes.get(requestedNodeId);
    const rootNodeIds = rootAssetId
      ? getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId)
      : [];
    const nodeMatchesRoot = selectedNode && (!rootAssetId || rootNodeIds.includes(selectedNode.id));

    if (selectedNode && nodeMatchesRoot) {
      this.selectedRootAssetId = rootAssetId;
      this.expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
      this.openPathTo(selectedNode.id);
      this.selectAssetNode(selectedNode);
    } else {
      this.selectRootAsset(rootAssetId, false);
    }
    // 展开状态同样以最新树为准，已删除或不可见的节点不会被重新带回页面。
    if (remembered) {
      this.expandedAssetIds = new Set(remembered.expandedIds.filter((id) => this.assetTree.nodes.has(id)));
      if (this.selectedTreeNodeId) {
        this.openPathTo(this.selectedTreeNodeId);
      }
    }
  }

  /** 展开选中节点的祖先路径，确保返回后所选节点在资产树中可见。 */
  private openPathTo(targetId: string): void {
    const visit = (node: SmartLightningMonitoringAssetNode): boolean => {
      if (node.id === targetId) {
        return true;
      }
      for (const childId of node.childIds) {
        const child = this.assetTree.nodes.get(childId);
        if (child && visit(child)) {
          this.expandedAssetIds.add(node.id);
          return true;
        }
      }
      return false;
    };
    this.assetTree.roots.some(visit);
    this.expandedAssetIds = new Set(this.expandedAssetIds);
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
        this.rebuildRealtimeAnalysisFromSnapshot();
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
        this.realtimeAnalysisAssetId = assetId;
        this.realtimeAnalysisLoading = false;
        this.rebuildRealtimeAnalysisFromSnapshot();
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

  private toMonitoringDeviceContext(contexts: SmartLightningDeviceContext[]): MonitoringDeviceContext {
    const treeDeviceIds = new Set([...this.assetTree.nodes.values()]
      .filter((node) => node.entityType === EntityType.DEVICE)
      .map((node) => node.id));
    const devices: DeviceInfo[] = [];
    const attributesByDeviceId = new Map<string, SmartLightningMonitoringDeviceAttributes>();
    for (const context of contexts) {
      const productCategory = context.productCategory?.trim();
      const functionCategory = context.functionCategory?.trim();
      // 设备上下文可能包含旧分类记录；以服务器属性为准，并确保已迁出的电弧设备不再进入防雷列表。
      if ((productCategory && productCategory !== 'lightning') || functionCategory === 'arc_fault') {
        continue;
      }
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
      const realtimeValues = this.realtimeService.getLatestTelemetryValues(context.deviceId);
      const activeTelemetry = realtimeValues.get('active');
      attributesByDeviceId.set(context.deviceId, {
        active: activeTelemetry?.value ?? context.active,
        functionCategory: context.functionCategory,
        productName: context.productName,
        lastActivityTime: this.realtimeService.getLatestActivityTime(context.deviceId) ??
          context.lastActivityTime
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
    this.monitoringApi.getLightningAlarms(activeQuery).pipe(
      map((page) => this.uniqueAlarms(page.data)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (alarms) => {
        this.nodeAlarms = alarms;
        this.rebuildNodeDeviceCards();
        this.rebuildRealtimeAnalysisFromSnapshot();
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
      this.nodeDeviceAssetPaths,
      this.nodeDeviceTelemetryById
    );
  }

  /** 资产树加载后只为树内设备取共享 WebSocket 的当前快照。 */
  private prepareNodeDeviceTelemetry(): void {
    this.nodeDeviceIds = new Set([...this.assetTree.nodes.values()]
      .filter((node) => node.entityType === EntityType.DEVICE)
      .map((node) => node.id));
    this.nodeDeviceTelemetryById.clear();
    this.nodeDeviceIds.forEach((deviceId) => {
      const values = this.realtimeService.getLatestTelemetryValues(deviceId);
      if (values.size) {
        this.nodeDeviceTelemetryById.set(deviceId, values);
      }
    });
  }

  /** 合并树内设备的新遥测，只在上报时间更新时重绘相关卡片。 */
  private applyRealtimeTelemetryChanges(changes: readonly SmartLightningDeviceTelemetryChange[]): void {
    let hasVisibleChange = false;
    let hasRealtimeAnalysisChange = false;
    changes.forEach((change) => {
      if (!this.nodeDeviceIds.has(change.deviceId)) {
        return;
      }
      const merged = new Map(this.nodeDeviceTelemetryById.get(change.deviceId) || []);
      let telemetryChanged = false;
      change.values.forEach((incoming, key) => {
        const current = merged.get(key);
        if (!current || incoming.ts > current.ts ||
          (incoming.ts === current.ts && incoming.value !== current.value)) {
          merged.set(key, incoming);
          telemetryChanged = true;
          // 最近活跃时间只展示在设备卡片上，不参与汇总；其他已订阅键可能影响实时板块。
          if (key !== 'lastActivityTime') {
            hasRealtimeAnalysisChange = true;
          }
          if (key === 'active') {
            const attributes = this.nodeDeviceAttributes.get(change.deviceId);
            this.nodeDeviceAttributes.set(change.deviceId, {
              ...attributes,
              active: incoming.value
            });
          }
        }
      });
      if (telemetryChanged) {
        this.nodeDeviceTelemetryById.set(change.deviceId, merged);
        hasVisibleChange = true;
      }
      if (change.lastActivityTime != null) {
        const attributes = this.nodeDeviceAttributes.get(change.deviceId);
        const currentActivityTime = Number(attributes?.lastActivityTime);
        if (!Number.isFinite(currentActivityTime) || change.lastActivityTime > currentActivityTime) {
          this.nodeDeviceAttributes.set(change.deviceId, {
            ...attributes,
            lastActivityTime: change.lastActivityTime
          });
          hasVisibleChange = true;
        }
      }
    });
    if (hasVisibleChange) {
      this.rebuildNodeDeviceCards();
      this.cd.markForCheck();
    }
    if (hasRealtimeAnalysisChange) {
      this.rebuildRealtimeAnalysisFromSnapshot();
    }
  }

  /** 只用当前已选子树、已加载设备和活动告警重算实时区，不请求后端汇总接口。 */
  private rebuildRealtimeAnalysisFromSnapshot(): void {
    if (!this.realtimeService.isEntitySnapshotReady || this.assetsLoading || this.devicesLoading) {
      return;
    }
    const snapshots = this.nodeDevices
      .filter((device) => {
        const deviceId = device.id?.id;
        return !!deviceId && this.selectedAssetIds.has(deviceId);
      })
      .map((device) => {
        const deviceId = device.id.id;
        const values = this.nodeDeviceTelemetryById.get(deviceId) || new Map<string, SmartLightningMonitoringTelemetryValue>();
        const activeValue = values.get('active')?.value ?? this.nodeDeviceAttributes.get(deviceId)?.active;
        const functionCategory = this.nodeDeviceAttributes.get(deviceId)?.functionCategory;
        return {
          deviceId,
          deviceName: device.name || deviceId,
          deviceProfileName: device.deviceProfileName,
          productName: typeof this.nodeDeviceAttributes.get(deviceId)?.productName === 'string'
            ? String(this.nodeDeviceAttributes.get(deviceId)?.productName) : undefined,
          functionCategory: typeof functionCategory === 'string' ? functionCategory : '',
          active: activeValue,
          values
        };
      });
    const scopedAlarmIds = new Set([...this.activeAlarmDeviceIds].filter((deviceId) => this.selectedAssetIds.has(deviceId)));
    const fallbackLatestArcReportTime = this.realtimeAnalysisAssetId === this.selectedRealtimeAssetId
      ? this.realtimeAnalysis?.arcFault.latestReportTime ?? null
      : null;
    this.realtimeAnalysis = buildSmartLightningMonitoringRealtimeAnalysis(
      snapshots,
      scopedAlarmIds,
      fallbackLatestArcReportTime
    );
    this.realtimeAnalysisLoading = false;
    this.realtimeAnalysisLoadError = false;
    this.cd.markForCheck();
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
