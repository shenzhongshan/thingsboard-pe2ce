/** 文件职责：用电气安全专属接口和共享实时订阅驱动防雷风格的数据监测页面。 */
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {auditTime, bufferTime, forkJoin, Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {EntityType} from '@shared/models/entity-type.models';
import {
  buildElectricalSafetyFireRiskMeasurements,
  formatElectricalSafetyFireRiskMetricTooltip
} from '../dashboard/electrical-safety-dashboard.data';
import type {ElectricalSafetyFireRiskMetric} from '../dashboard/electrical-safety-dashboard.data';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningDefaultExpandedAssetIds,
  getSmartLightningMonitoringRootAssetIds,
  getSmartLightningMonitoringSelectedAssetIds,
  getSmartLightningMonitoringPage,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import {
  SmartLightningDeviceTelemetryChange,
  SmartLightningRealtimeService
} from '@home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service';
import {
  buildElectricalSafetyMonitoringScopeSummary,
  buildElectricalSafetyMonitoringDevices,
  buildElectricalSafetyMonitoringSummaryCards,
  buildElectricalSafetyArcFaultSummaryMetrics,
  ELECTRICAL_SAFETY_MONITORING_CATEGORIES,
  ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
  isElectricalSafetyMonitoringSummaryDevice,
  ElectricalSafetyMonitoringCategoryKey,
  ElectricalSafetyMonitoringDevice,
  ElectricalSafetyMonitoringMetric,
  ElectricalSafetyMonitoringSummaryCategoryKey,
  ElectricalSafetyMonitoringTelemetryValue,
  filterElectricalSafetyMonitoringDevices,
  parseElectricalSafetyActiveValue,
} from './electrical-safety-monitoring.data';
import {ElectricalSafetyApiService, ElectricalSafetyDeviceContext} from '../electrical-safety-api.service';

/** 空资产树用于首次接口响应前稳定渲染共享资产树组件。 */
const EMPTY_ELECTRICAL_SAFETY_ASSET_TREE: SmartLightningMonitoringAssetTree =
  buildSmartLightningMonitoringAssetTreeFromRoots([]);

/** 电气安全监测页面沿用智能防雷的数据监测布局，数据由本模块接口提供。 */
@Component({
  selector: 'tb-electrical-safety-monitoring',
  templateUrl: './electrical-safety-monitoring.component.html',
  styleUrls: ['./electrical-safety-monitoring.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ElectricalSafetyMonitoringComponent implements OnInit, OnDestroy {
  /** 与防雷页面共用同一资产树结构；树内容已由电气安全接口按分类及权限过滤。 */
  assetTree = EMPTY_ELECTRICAL_SAFETY_ASSET_TREE;
  readonly categoryOptions = ELECTRICAL_SAFETY_MONITORING_CATEGORIES;
  readonly devicePageSize = 10;
  devices: ElectricalSafetyMonitoringDevice[] = [];
  activeAlarmDeviceIds = new Set<string>();

  /** 设备/资产接口和告警接口分别维护状态，告警接口失败不隐藏设备列表。 */
  assetsLoading = true;
  assetsLoadError = false;
  devicesLoading = true;
  devicesLoadError = false;
  alarmsLoading = true;
  alarmsLoadError = false;

  /** 当前资产范围和设备列表控件状态。 */
  selectedRootAssetId = '';
  selectedTreeNodeId = '';
  selectedAssetIds = new Set<string>();
  expandedAssetIds = new Set<string>();
  selectedAssetName = '全部资产';
  assetSearch = '';
  activeCategory: ElectricalSafetyMonitoringCategoryKey = 'all';
  deviceSearch = '';
  devicePage = 1;

  private deviceContexts: ElectricalSafetyDeviceContext[] = [];
  private readonly telemetryByDeviceId = new Map<
    string,
    ReadonlyMap<string, ElectricalSafetyMonitoringTelemetryValue>
  >();
  /** 火灾卡片只汇总当前选中资产范围内已配置测点的最新遥测。 */
  private fireRiskMeasurements = buildElectricalSafetyFireRiskMeasurements(null);
  private readonly membershipRefresh$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  private monitoringRequestId = 0;
  private alarmRequestId = 0;

  constructor(
    private readonly electricalSafetyApiService: ElectricalSafetyApiService,
    private readonly realtimeService: SmartLightningRealtimeService,
    private readonly route: ActivatedRoute,
    private readonly changeDetector: ChangeDetectorRef
  ) {}

  /** 先接入共享 WebSocket，再读取本模块设备与资产快照，减少首屏订阅间隙。 */
  ngOnInit(): void {
    this.realtimeService.deviceTelemetryChanged$.pipe(
      bufferTime(100),
      takeUntil(this.destroy$)
    ).subscribe((changes) => {
      if (changes.length) {
        this.applyRealtimeTelemetryChanges(changes);
      }
    });
    this.realtimeService.entitySnapshotReady$.pipe(
      takeUntil(this.destroy$)
    ).subscribe((ready) => {
      if (ready) {
        this.applyRealtimeSnapshot();
      }
    });
    this.realtimeService.deviceContextIncrementalChanged$.pipe(
      auditTime(500),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadMonitoringData(true);
      }
    });
    this.realtimeService.deviceMembershipChanged$.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.membershipRefresh$.next());
    this.realtimeService.assetMembershipChanged$.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.membershipRefresh$.next());
    this.membershipRefresh$.pipe(
      auditTime(500),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.assetsLoading) {
        this.loadMonitoringData(true);
      }
    });
    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => this.loadActiveAlarmDevices());

    this.realtimeService.acquire();
    // 从详情页返回时恢复根资产和选中节点，仍由新资产树重新校验有效范围。
    this.selectedRootAssetId = this.route.snapshot.queryParamMap.get('selectedRootAssetId') || '';
    this.selectedTreeNodeId = this.route.snapshot.queryParamMap.get('selectedAssetId') || '';
    this.loadMonitoringData(true);
    this.loadActiveAlarmDevices();
  }

  /** 页面销毁时释放本页 WebSocket 引用，并终止所有订阅。 */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
  }

  /** 分类页签与防雷页面一样只展示当前设备列表中实际存在的类别。 */
  get categories(): ReadonlyArray<{key: ElectricalSafetyMonitoringCategoryKey; label: string}> {
    const presentCategories = new Set(this.devices.map((device) => device.categoryKey));
    return this.categoryOptions.filter((category) =>
      category.key === 'all' || presentCategories.has(category.key as Exclude<ElectricalSafetyMonitoringCategoryKey, 'all'>)
    );
  }

  /** 顶部能力卡片沿用防雷页面的信息密度；火灾风险指标来自当前资产范围的实时遥测。 */
  get summaryCards() {
    const scopedDeviceIds = new Set(this.scopedDevices.map((device) => device.id));
    const scopedContexts = this.deviceContexts.filter((context) => scopedDeviceIds.has(context.deviceId));
    const fireMetrics: ElectricalSafetyMonitoringMetric[] = [
      this.buildFireRiskSummaryMetric('最大剩余电流', this.fireRiskMeasurements.residualCurrentMax),
      this.buildFireRiskSummaryMetric('最高温度', this.fireRiskMeasurements.highestTemperature)
    ];
    const arcFaultMetrics = buildElectricalSafetyArcFaultSummaryMetrics(scopedContexts, this.telemetryByDeviceId);
    return buildElectricalSafetyMonitoringSummaryCards().map((card) => {
      if (card.key === 'fire') {
        return {...card, metrics: fireMetrics};
      }
      if (card.key === 'arc-fault') {
        return {...card, metrics: arcFaultMetrics};
      }
      return card;
    });
  }

  /** 当前选中资产的全部设备；页面汇总、筛选、卡片和告警标记统一用此范围。 */
  get scopedDevices(): ElectricalSafetyMonitoringDevice[] {
    return this.devices.filter((device) => this.selectedAssetIds.has(device.assetId));
  }

  /** 当前资产范围的设备状态统计；只有活动告警查询成功后才返回告警设备数。 */
  get scopeSummary() {
    if (this.devicesLoading || this.devicesLoadError) {
      return {
        totalDevices: null,
        normalDevices: null,
        onlineDevices: null,
        alarmDevices: null,
        offlineDevices: null,
        todayAlarmCount: null,
        unprocessedAlarmCount: null,
        dispatchedAlarmCount: null,
        processedAlarmCount: null
      };
    }
    const alarmIds = this.alarmsLoading || this.alarmsLoadError ? null : this.activeAlarmDeviceIds;
    return buildElectricalSafetyMonitoringScopeSummary(this.scopedDevices, alarmIds);
  }

  /** 页面状态只按已确认的设备在线状态和活动告警计算，不推断测点健康等级。 */
  get scopeStatus(): string {
    return this.statusForDevices(this.scopedDevices);
  }

  /** 设备页签按资产、功能分类和搜索内容筛选；搜索变化时回到第一页。 */
  get filteredDevices(): ElectricalSafetyMonitoringDevice[] {
    return filterElectricalSafetyMonitoringDevices(
      this.devices,
      this.selectedAssetIds,
      this.activeCategory,
      this.deviceSearch
    ).sort((left, right) => {
      const statusOrder = this.deviceStatusOrder(left) - this.deviceStatusOrder(right);
      return statusOrder || left.name.localeCompare(right.name);
    });
  }

  /** 使用防雷页面相同的分页口径，结果变化后将页码钳制在有效范围内。 */
  get devicePagination() {
    return getSmartLightningMonitoringPage(this.filteredDevices, this.devicePage, this.devicePageSize);
  }

  /** 当前页设备卡片数据。 */
  get pagedDevices(): ElectricalSafetyMonitoringDevice[] {
    return this.devicePagination.items;
  }

  /** 根资产筛选同步影响资产树选择和设备列表数据范围。 */
  selectRootAsset(rootAssetId: string, expandDescendants = true): void {
    this.selectedRootAssetId = rootAssetId;
    this.selectedTreeNodeId = '';
    this.selectedAssetIds = this.getRootAssetSelection(rootAssetId);
    const root = this.assetTree.roots.find((node) => node.id === rootAssetId);
    this.selectedAssetName = root?.name || '全部资产';
    this.refreshFireRiskMeasurements();
    if (expandDescendants && root) {
      const expanded = new Set(this.expandedAssetIds);
      expanded.add(root.id);
      this.expandedAssetIds = expanded;
    }
    this.resetDevicePage();
    this.changeDetector.markForCheck();
  }

  /** 选中资产时，将其完整后代纳入页面统计与设备列表。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    this.selectedTreeNodeId = node.id;
    this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name;
    this.refreshFireRiskMeasurements();
    this.openDescendants(node.id);
    this.resetDevicePage();
    this.changeDetector.markForCheck();
  }

  /** 展开或收起资产分支时替换集合引用，通知 OnPush 共享树更新。 */
  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    const expandedIds = new Set(this.expandedAssetIds);
    if (expandedIds.has(node.id)) {
      expandedIds.delete(node.id);
    } else {
      expandedIds.add(node.id);
    }
    this.expandedAssetIds = expandedIds;
  }

  /** 全选保留当前公司筛选，并重新选中该范围下的所有资产节点。 */
  selectAllAssets(): void {
    this.selectedTreeNodeId = '';
    this.selectedAssetIds = this.getRootAssetSelection(this.selectedRootAssetId);
    const root = this.assetTree.roots.find((node) => node.id === this.selectedRootAssetId);
    this.selectedAssetName = root?.name || '全部资产';
    this.refreshFireRiskMeasurements();
    this.resetDevicePage();
  }

  /** 切换功能分类后回到第一页。 */
  selectCategory(category: ElectricalSafetyMonitoringCategoryKey): void {
    this.activeCategory = category;
    this.resetDevicePage();
  }

  /** 搜索框变化和清除操作共用此方法重置分页位置。 */
  resetDevicePage(): void {
    this.devicePage = 1;
  }

  /** 设备卡片状态优先显示活动告警，其次显示在线/离线状态。 */
  deviceStatus(device: ElectricalSafetyMonitoringDevice): string {
    if (this.activeAlarmDeviceIds.has(device.id)) {
      return '报警';
    }
    return device.connection === 'online' ? '正常' : '离线';
  }

  /** 分类别显示真实设备数、在线数、离线数和活动告警设备数。 */
  categorySummary(category: ElectricalSafetyMonitoringSummaryCategoryKey) {
    if (this.devicesLoading || this.devicesLoadError) {
      return {total: null, online: null, offline: null, alarms: null};
    }
    const categoryDevices = this.scopedDevices.filter((device) =>
      isElectricalSafetyMonitoringSummaryDevice(device, category));
    const alarmCount = this.alarmsLoading || this.alarmsLoadError
      ? null
      : categoryDevices.filter((device) => this.activeAlarmDeviceIds.has(device.id)).length;
    return {
      total: categoryDevices.length,
      online: categoryDevices.filter((device) => device.connection === 'online').length,
      offline: categoryDevices.filter((device) => device.connection === 'offline').length,
      alarms: alarmCount
    };
  }

  /** 分类别状态只反映设备连通性及真实活动告警，不冒充电气指标诊断。 */
  categoryStatus(category: ElectricalSafetyMonitoringSummaryCategoryKey): string {
    return this.statusForDevices(this.scopedDevices.filter((device) =>
      isElectricalSafetyMonitoringSummaryDevice(device, category)));
  }

  /** 翻页时复用防雷页的分页纯函数，筛选后不会进入无数据页。 */
  setDevicePage(page: number): void {
    this.devicePage = getSmartLightningMonitoringPage(
      this.filteredDevices,
      page,
      this.devicePageSize
    ).currentPage;
  }

  /** 并行请求电气安全专属设备上下文和资产树；刷新时尽量保留当前选择。 */
  private loadMonitoringData(preserveCurrentSelection = false): void {
    const requestId = ++this.monitoringRequestId;
    const previousRootAssetId = this.selectedRootAssetId;
    const previousTreeNodeId = this.selectedTreeNodeId;
    this.assetsLoading = true;
    this.devicesLoading = true;
    this.assetsLoadError = false;
    this.devicesLoadError = false;
    forkJoin({
      contexts: this.electricalSafetyApiService.getDeviceContexts(),
      tree: this.electricalSafetyApiService.getAssetTree()
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({contexts, tree}) => {
        if (requestId !== this.monitoringRequestId) {
          return;
        }
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(tree?.roots || []);
        this.deviceContexts = (contexts || []).filter((context) =>
          context.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY && !!context.deviceId
        );
        const currentDeviceIds = new Set(this.deviceContexts.map((context) => context.deviceId));
        for (const deviceId of this.telemetryByDeviceId.keys()) {
          if (!currentDeviceIds.has(deviceId)) {
            this.telemetryByDeviceId.delete(deviceId);
          }
        }
        this.expandedAssetIds = getSmartLightningDefaultExpandedAssetIds(this.assetTree);
        this.restoreAssetSelection(
          preserveCurrentSelection ? previousRootAssetId : '',
          preserveCurrentSelection ? previousTreeNodeId : ''
        );
        this.applyRealtimeSnapshot();
        this.refreshDevices();
        this.assetsLoading = false;
        this.devicesLoading = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        if (requestId !== this.monitoringRequestId) {
          return;
        }
        this.assetTree = EMPTY_ELECTRICAL_SAFETY_ASSET_TREE;
        this.deviceContexts = [];
        this.devices = [];
        this.fireRiskMeasurements = buildElectricalSafetyFireRiskMeasurements(null);
        this.selectedAssetIds = new Set<string>();
        this.selectedRootAssetId = '';
        this.selectedTreeNodeId = '';
        this.selectedAssetName = '全部资产';
        this.assetsLoading = false;
        this.devicesLoading = false;
        this.assetsLoadError = true;
        this.devicesLoadError = true;
        this.changeDetector.markForCheck();
      }
    });
  }

  /** 查询当前用户可见的电气安全活动告警设备 ID，供树标记与页面状态使用。 */
  private loadActiveAlarmDevices(): void {
    const requestId = ++this.alarmRequestId;
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    this.changeDetector.markForCheck();
    this.electricalSafetyApiService.getDashboardAlarmStatistics(ELECTRICAL_SAFETY_PRODUCT_CATEGORY, 'day')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (statistics) => {
          if (requestId !== this.alarmRequestId) {
            return;
          }
          this.activeAlarmDeviceIds = new Set(statistics?.activeAlarmDeviceIds || []);
          this.alarmsLoading = false;
          this.changeDetector.markForCheck();
        },
        error: () => {
          if (requestId !== this.alarmRequestId) {
            return;
          }
          this.alarmsLoading = false;
          this.alarmsLoadError = true;
          this.changeDetector.markForCheck();
        }
      });
  }

  /** 根据模块专属资产树恢复根筛选和所选资产；无效旧节点时安全回退到有效公司。 */
  private restoreAssetSelection(rootAssetId: string, treeNodeId: string): void {
    const validRootId = this.assetTree.roots.some((root) => root.id === rootAssetId) ? rootAssetId : '';
    const node = this.assetTree.nodes.get(treeNodeId);
    const rootIds = validRootId ? getSmartLightningMonitoringRootAssetIds(this.assetTree, validRootId) : [];
    if (node?.entityType === EntityType.ASSET && (!validRootId || rootIds.includes(node.id))) {
      this.selectedRootAssetId = validRootId;
      this.selectedTreeNodeId = node.id;
      this.selectedAssetIds = new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
      this.selectedAssetName = node.name;
      return;
    }
    this.selectRootAsset(validRootId, false);
  }

  /** 从最新 WebSocket 缓存读取页面设备快照，补齐 HTTP 请求期间到达的状态和测点。 */
  private applyRealtimeSnapshot(): void {
    const nextContexts: ElectricalSafetyDeviceContext[] = [];
    for (const context of this.deviceContexts) {
      const cachedValues = this.realtimeService.getLatestTelemetryValues(context.deviceId);
      if (cachedValues.size) {
        const values = new Map(this.telemetryByDeviceId.get(context.deviceId) || []);
        cachedValues.forEach((sample, key) => {
          const current = values.get(key);
          if (!current || sample.ts > current.ts || (sample.ts === current.ts && sample.value !== current.value)) {
            values.set(key, sample);
          }
        });
        this.telemetryByDeviceId.set(context.deviceId, values);
      }
      const active = parseElectricalSafetyActiveValue(cachedValues.get('active')?.value);
      const latestActivityTime = this.realtimeService.getLatestActivityTime(context.deviceId);
      nextContexts.push({
        ...context,
        active: active === undefined ? context.active : active,
        lastActivityTime: latestActivityTime == null
          ? context.lastActivityTime
          : Math.max(context.lastActivityTime || 0, latestActivityTime)
      });
    }
    this.deviceContexts = nextContexts;
    this.refreshDevices();
  }

  /** 合并共享 WebSocket 的设备增量；只修改电气安全接口返回的设备。 */
  private applyRealtimeTelemetryChanges(changes: SmartLightningDeviceTelemetryChange[]): void {
    const knownDeviceIds = new Set(this.deviceContexts.map((context) => context.deviceId));
    const changesByDeviceId = new Map<string, SmartLightningDeviceTelemetryChange>();
    for (const change of changes) {
      if (!knownDeviceIds.has(change.deviceId)) {
        continue;
      }
      const previous = changesByDeviceId.get(change.deviceId);
      if (!previous) {
        changesByDeviceId.set(change.deviceId, change);
        continue;
      }
      const mergedValues = new Map(previous.values);
      change.values.forEach((sample, key) => {
        const current = mergedValues.get(key);
        if (!current || sample.ts > current.ts || (sample.ts === current.ts && sample.value !== current.value)) {
          mergedValues.set(key, sample);
        }
      });
      changesByDeviceId.set(change.deviceId, {
        deviceId: change.deviceId,
        values: mergedValues,
        lastActivityTime: change.lastActivityTime == null
          ? previous.lastActivityTime
          : Math.max(previous.lastActivityTime || 0, change.lastActivityTime)
      });
    }
    let hasRelevantChange = false;
    const contextsByDeviceId = new Map<string, ElectricalSafetyDeviceContext>();
    for (const context of this.deviceContexts) {
      contextsByDeviceId.set(context.deviceId, context);
    }
    for (const change of changesByDeviceId.values()) {
      const values = new Map(this.telemetryByDeviceId.get(change.deviceId) || []);
      change.values.forEach((sample, key) => {
        const current = values.get(key);
        if (!current || sample.ts > current.ts || (sample.ts === current.ts && sample.value !== current.value)) {
          values.set(key, sample);
          hasRelevantChange = true;
        }
      });
      this.telemetryByDeviceId.set(change.deviceId, values);
      const context = contextsByDeviceId.get(change.deviceId);
      if (context && change.lastActivityTime != null &&
        change.lastActivityTime > (context.lastActivityTime || 0)) {
        hasRelevantChange = true;
      }
    }
    if (!hasRelevantChange) {
      return;
    }

    this.deviceContexts = this.deviceContexts.map((context) => {
      const change = changesByDeviceId.get(context.deviceId);
      if (!change) {
        return context;
      }
      const active = parseElectricalSafetyActiveValue(change.values.get('active')?.value);
      const latestActivityTime = change.lastActivityTime ?? this.realtimeService.getLatestActivityTime(context.deviceId);
      return {
        ...context,
        active: active === undefined ? context.active : active,
        lastActivityTime: latestActivityTime == null
          ? context.lastActivityTime
          : Math.max(context.lastActivityTime || 0, latestActivityTime)
      };
    });
    this.refreshDevices();
    this.changeDetector.markForCheck();
  }

  /** 统一调用纯映射函数重建设备卡片，确保 HTTP 与 WebSocket 数据显示口径一致。 */
  private refreshDevices(): void {
    this.devices = buildElectricalSafetyMonitoringDevices(
      this.deviceContexts,
      this.assetTree,
      this.telemetryByDeviceId
    );
    this.refreshFireRiskMeasurements();
    if (!this.categories.some((category) => category.key === this.activeCategory)) {
      this.activeCategory = 'all';
    }
    this.setDevicePage(this.devicePage);
  }

  /** 只取当前资产范围内的火灾测点，避免切换资产后仍展示其他区域的最大值。 */
  private refreshFireRiskMeasurements(): void {
    const scopedDeviceIds = new Set(this.scopedDevices.map((device) => device.id));
    const scopedContexts = this.deviceContexts.filter((context) => scopedDeviceIds.has(context.deviceId));
    this.fireRiskMeasurements = buildElectricalSafetyFireRiskMeasurements(
      scopedContexts,
      this.telemetryByDeviceId
    );
  }

  /** 把型号测点最大值映射为摘要卡指标；来源只放进悬浮提示，不常驻显示。 */
  private buildFireRiskSummaryMetric(
    label: string,
    measurement: ElectricalSafetyFireRiskMetric
  ): ElectricalSafetyMonitoringMetric {
    return {
      label,
      value: measurement.value ?? '--',
      unit: measurement.unit || undefined,
      // 没有项目确认的阈值时只呈现遥测值，不把有效读数误标成风险状态。
      tone: 'neutral',
      title: measurement.value === null
        ? undefined
        : formatElectricalSafetyFireRiskMetricTooltip(label, measurement)
    };
  }

  /** 对当前设备范围应用统一的状态优先级，不根据设备名称或未知测点猜测状态。 */
  private statusForDevices(devices: readonly ElectricalSafetyMonitoringDevice[]): string {
    if (!devices.length || this.alarmsLoading || this.alarmsLoadError) {
      return '数据不足';
    }
    if (devices.some((device) => this.activeAlarmDeviceIds.has(device.id))) {
      return '异常';
    }
    if (devices.some((device) => device.connection === 'offline')) {
      return '需关注';
    }
    return '正常';
  }

  /** 用于列表排序的状态序号，与卡片状态显示保持同一优先级。 */
  private deviceStatusOrder(device: ElectricalSafetyMonitoringDevice): number {
    if (this.activeAlarmDeviceIds.has(device.id)) {
      return 0;
    }
    return device.connection === 'offline' ? 1 : 2;
  }

  /** 根资产筛选返回全部子节点；“全部资产”额外纳入未挂载在树内的可见设备。 */
  private getRootAssetSelection(rootAssetId: string): Set<string> {
    const selectedIds = new Set(getSmartLightningMonitoringRootAssetIds(this.assetTree, rootAssetId));
    if (!rootAssetId) {
      this.deviceContexts.forEach((context) => selectedIds.add(context.deviceId));
    }
    return selectedIds;
  }

  /** 选中资产后展开下级节点，与防雷资产树保持同样的浏览方式。 */
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
}
