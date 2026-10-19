/** 文件职责：数据监测模块的 smart-lightning-device-detail.component，负责页面状态管理、接口调用与用户交互。 */
import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, QueryList, ViewChildren} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AttributeService} from '@core/http/attribute.service';
import {DeviceService} from '@core/http/device.service';
import {AlarmService} from '@core/http/alarm.service';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {auditTime, catchError, of, Subject, startWith, switchMap, takeUntil} from 'rxjs';
import {DeviceId} from '@shared/models/id/device-id';
import {AttributeScope} from '@shared/models/telemetry/telemetry.models';
import {AlarmInfo, AlarmQuery, AlarmSearchStatus, AlarmSeverity} from '@shared/models/alarm.models';
import {TimePageLink} from '@shared/models/page/page-link';
import {Direction} from '@shared/models/page/sort-order';
import {buildSmartLightningMonitoringAssetTreeFromRoots, getSmartLightningDefaultExpandedAssetIds, getSmartLightningMonitoringSelectedAssetIds, SmartLightningMonitoringAssetNode, SmartLightningMonitoringAssetTree} from './smart-lightning-monitoring.data';
import {SmartLightningMonitoringApiService} from './smart-lightning-monitoring-api.service';
import {createSmartLightningDeviceDetailState, buildSmartLightningRealtimeMetrics, getSmartLightningRealtimeMetricDefinitions, SmartLightningChartPeriod, SmartLightningDeviceDetailState} from './smart-lightning-device-detail.data';
import {buildSmartLightningChartReadKeys, buildSmartLightningChartSeriesData, buildSmartLightningYAxisBounds, filterSmartLightningChartDefinitions, getSmartLightningChartDefinitions, getSmartLightningChartWindow, SmartLightningChartDefinition, SmartLightningChartSeriesData} from './smart-lightning-device-detail.charts';
import {SmartLightningDeviceDetailApiService} from './smart-lightning-device-detail-api.service';
import {getSmartLightningProductSpecification, SmartLightningProductSpecification} from './smart-lightning-product-specifications';
import {SmartLightningDeviceTelemetryChange, SmartLightningRealtimeService} from '../shared/realtime/smart-lightning-realtime.service';

/** 资产树和遥测均使用真实接口；图表只展示型号定义且设备实际存在的数值测点。 */
@Component({selector: 'tb-smart-lightning-device-detail', templateUrl: './smart-lightning-device-detail.component.html', styleUrls: ['./smart-lightning-device-detail.component.scss'], changeDetection: ChangeDetectionStrategy.OnPush, standalone: false})
export class SmartLightningDeviceDetailComponent implements AfterViewInit, OnDestroy {
  @ViewChildren('chartHost') chartHosts!: QueryList<ElementRef<HTMLDivElement>>;
  assetTree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  expandedAssetIds = new Set<string>(); selectedAssetIds = new Set<string>(); selectedAssetName = '未选择资产';
  assetSearch = ''; assetsLoading = true; assetsLoadError = false;
  detail: SmartLightningDeviceDetailState = createSmartLightningDeviceDetailState();
  productName = '未配置产品名称';
  /** 当前设备配置对应的静态产品参数；没有资料的型号保持为空。 */
  productSpecification?: SmartLightningProductSpecification;
  deviceName = '--';
  instrumentAddress = '--';
  /** 当前实时数据中最新一条遥测的时间；不再使用固定演示时间。 */
  realtimeUpdatedAt = '--';
  /** 首次请求完成前展示加载状态，不把默认占位测点误呈现为设备实际数据。 */
  realtimeLoading = true;
  /** 原生接口失败必须明确提示，不能通过空对象伪装成设备没有遥测。 */
  realtimeLoadError = false;
  alarmsLoading = false;
  alarmsLoadError = false;
  /** 资产树告警状态：共享树同时标记当前告警设备及其直属上级资产。 */
  get activeAlarmDeviceIds(): Set<string> {
    const hasUnhandledAlarm = this.detail.alarms.some((alarm) => alarm.status === '未处理');
    return hasUnhandledAlarm && this.requestedDeviceId ? new Set([this.requestedDeviceId]) : new Set<string>();
  }
  /** 详情定位模式下的可选中节点白名单：仅当前设备；非定位模式返回 null 表示全部可选。 */
  get selectableAssetIds(): ReadonlySet<string> | null {
    return this.requestedDeviceId ? new Set([this.requestedDeviceId]) : null;
  }
  returnUrl = '/smart-lightning/monitoring';
  returnLabel = '返回';
  readonly chartPeriodOptions: ReadonlyArray<{value: SmartLightningChartPeriod; label: string}> = [
    {value: 'day', label: '日'}, {value: 'week', label: '周'}, {value: 'month', label: '月'}
  ];
  chartDefinitions: SmartLightningChartDefinition[] = [];
  /** 当前型号完整职责图卡；主页面只展示有数据的前四张，更多参数弹窗展示全部定义。 */
  allChartDefinitions: SmartLightningChartDefinition[] = [];
  chartSeriesData: Record<string, SmartLightningChartSeriesData[]> = {};
  chartsLoading = false;
  showAllChartsDialog = false;
  selectedChartPeriods: Record<string, SmartLightningChartPeriod> = {};
  /** 当前处于浏览器全屏的图卡元素；通过元素而不是图卡 id 区分主页面和弹窗中的同名图卡。 */
  fullscreenChartCard?: HTMLElement;
  /** 当前设备已收到的最新遥测快照；按时间戳合并，防止旧 HTTP 响应覆盖新 WebSocket 数据。 */
  private readonly latestRealtimeValues = new Map<string, {ts: number; value: string}>();
  private currentDeviceProfileName?: string;
  /** 浏览器窗口变化时重新测量工作区，保证放大图卡仍避开顶部和左侧导航。 */
  private readonly onWindowResize = (): void => {
    if (this.fullscreenChartCard) {
      this.updateFullscreenBounds(this.fullscreenChartCard);
      setTimeout(() => this.charts.forEach((chart) => chart.resize()));
    }
  };
  private requestedDeviceId?: string; private readonly charts = new Map<string, ECharts>(); private readonly destroy$ = new Subject<void>(); private resizeObserver?: ResizeObserver;
  constructor(private readonly monitoringApi: SmartLightningMonitoringApiService, private readonly detailApi: SmartLightningDeviceDetailApiService, private readonly attributeService: AttributeService, private readonly deviceService: DeviceService, private readonly alarmService: AlarmService, private readonly realtimeService: SmartLightningRealtimeService, private readonly route: ActivatedRoute, private readonly router: Router, private readonly cd: ChangeDetectorRef) {}
  ngAfterViewInit(): void {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.onWindowResize);
    }
    // 直接消费 WebSocket 帧中的当前设备遥测，不再依赖二次 HTTP 查询才能刷新页面。
    this.realtimeService.deviceTelemetryChanged$.pipe(
      takeUntil(this.destroy$)
    ).subscribe((change) => {
      if (this.requestedDeviceId === change.deviceId) {
        this.applyRealtimeTelemetry(change);
      }
    });
    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this.requestedDeviceId) {
        this.loadDeviceAlarms(this.requestedDeviceId);
      }
    });
    this.realtimeService.acquire();
    this.returnUrl = this.resolveReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
    this.route.paramMap.pipe(
      switchMap((params) => {
        // 切换设备详情时先恢复普通布局，避免上一台设备的放大状态残留。
        this.exitChartFullscreen();
        this.requestedDeviceId = params.get('deviceId') || undefined;
        this.detail = createSmartLightningDeviceDetailState(this.requestedDeviceId);
        this.productName = '未配置产品名称';
        this.productSpecification = undefined;
        this.realtimeUpdatedAt = '--';
        this.realtimeLoading = true;
        this.realtimeLoadError = false;
        this.currentDeviceProfileName = undefined;
        this.latestRealtimeValues.clear();
        this.resetCharts();
        this.loadRealtimeMetrics(this.requestedDeviceId);
        this.loadDeviceAlarms(this.requestedDeviceId);
        return this.requestedDeviceId ? this.attributeService.getEntityAttributes(
          new DeviceId(this.requestedDeviceId), AttributeScope.SERVER_SCOPE, ['productName']
        ).pipe(catchError(() => of([]))) : of([]);
      }),
      takeUntil(this.destroy$)
    ).subscribe((attributes) => {
      const value = attributes.find((attribute) => attribute.key === 'productName')?.value;
      this.productName = typeof value === 'string' && value.trim() ? value.trim() : '未配置产品名称';
      this.cd.markForCheck();
    });
    this.loadAssetTree();
    this.resizeObserver = new ResizeObserver(() => this.charts.forEach((chart) => chart.resize()));
    this.chartHosts.changes.pipe(startWith(this.chartHosts), takeUntil(this.destroy$)).subscribe((hosts: QueryList<ElementRef<HTMLDivElement>>) => {
      hosts.forEach((host) => this.resizeObserver?.observe(host.nativeElement));
      setTimeout(() => this.renderCharts());
    });
  }
  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.onWindowResize);
    }
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
    this.resizeObserver?.disconnect();
    this.charts.forEach((chart) => chart.dispose());
  }

  /**
   * 将单张图表放大到应用内容工作区。
   * 工作区边界取自 .tb-main-content，因此左侧导航和顶部工具栏始终保留可见。
   */
  toggleChartFullscreen(card: HTMLElement): void {
    if (this.fullscreenChartCard === card) {
      this.exitChartFullscreen();
      return;
    }
    this.exitChartFullscreen();
    this.fullscreenChartCard = card;
    this.updateFullscreenBounds(card);
    this.cd.markForCheck();
    setTimeout(() => this.charts.forEach((chart) => chart.resize()));
  }

  /** 清理放大状态和动态尺寸，供按钮及弹窗切换前调用。 */
  private exitChartFullscreen(): void {
    if (!this.fullscreenChartCard) {
      return;
    }
    this.fullscreenChartCard.style.removeProperty('--chart-fullscreen-top');
    this.fullscreenChartCard.style.removeProperty('--chart-fullscreen-left');
    this.fullscreenChartCard.style.removeProperty('--chart-fullscreen-width');
    this.fullscreenChartCard.style.removeProperty('--chart-fullscreen-height');
    this.fullscreenChartCard = undefined;
    this.cd.markForCheck();
  }

  /** 根据主内容容器的视口矩形设置放大图卡边界，自动适配导航栏宽度和工具栏高度。 */
  private updateFullscreenBounds(card: HTMLElement): void {
    const content = card.closest('.tb-main-content') as HTMLElement | null;
    const bounds = content?.getBoundingClientRect();
    if (!bounds) {
      return;
    }
    card.style.setProperty('--chart-fullscreen-top', `${Math.max(0, bounds.top)}px`);
    card.style.setProperty('--chart-fullscreen-left', `${Math.max(0, bounds.left)}px`);
    card.style.setProperty('--chart-fullscreen-width', `${Math.max(0, bounds.width)}px`);
    card.style.setProperty('--chart-fullscreen-height', `${Math.max(0, bounds.height)}px`);
  }
  /** 根据来源页返回；未携带来源参数时默认回到数据监测。 */
  goBack(event: MouseEvent): void {
    event.preventDefault();
    if (this.returnUrl === '/smart-lightning/monitoring') {
      const queryParams: Record<string, string> = {};
      const selectedAssetId = this.route.snapshot.queryParamMap.get('returnAssetId');
      const selectedRootAssetId = this.route.snapshot.queryParamMap.get('returnRootAssetId');
      if (selectedAssetId) { queryParams['selectedAssetId'] = selectedAssetId; }
      if (selectedRootAssetId) { queryParams['selectedRootAssetId'] = selectedRootAssetId; }
      this.router.navigate(['/smart-lightning/monitoring'], {queryParams});
      return;
    }
    this.router.navigateByUrl(this.returnUrl);
  }
  /** 校验 returnUrl 是否为受支持的来源页，防止跳转到任意地址。 */
  private resolveReturnUrl(value: string | null): string {
    return value === '/smart-lightning/query' ? value : '/smart-lightning/monitoring';
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
  /** 详情页由设备卡带入 deviceId 后，资产树只作为定位视图，不能再切换到其他节点。 */
  isNodeSelectable(node: SmartLightningMonitoringAssetNode): boolean { return !this.requestedDeviceId || node.id === this.requestedDeviceId; }
  /** 选中节点：详情模式只选中单个节点并同步设备名称与仪表地址。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode, expandDescendants = true): void {
    if (!this.isNodeSelectable(node)) { return; }
    this.selectedAssetIds = this.requestedDeviceId ? new Set([node.id]) : new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name; this.deviceName = node.name; this.instrumentAddress = this.getAssetPath(node.id).join('/') || '--';
    if (expandDescendants) { this.openDescendants(node.id); }
    this.cd.markForCheck();
  }
  /** 加载资产树后定位到当前设备并展开其上级路径，失败时标记错误状态。 */
  private loadAssetTree(): void { this.monitoringApi.getTree().pipe(takeUntil(this.destroy$)).subscribe({next: ({roots}) => { this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots); const initial = this.requestedDeviceId ? this.assetTree.nodes.get(this.requestedDeviceId) : this.assetTree.roots[0]; if (initial) { this.expandedAssetIds = this.requestedDeviceId ? new Set<string>() : getSmartLightningDefaultExpandedAssetIds(this.assetTree); if (this.requestedDeviceId) { this.openPathTo(initial.id); } this.selectAssetNode(initial, !!this.requestedDeviceId); } this.assetsLoading = false; this.cd.markForCheck(); }, error: () => { this.assetsLoadError = true; this.assetsLoading = false; this.cd.markForCheck(); }}); }
  /** 从根节点递归回溯到目标设备，只展开该路径上的资产，避免用户进入详情时看到整棵树展开。 */
  private openPathTo(targetId: string): void {
    const visit = (node: SmartLightningMonitoringAssetNode): boolean => {
      if (node.id === targetId) { return true; }
      for (const childId of node.childIds) {
        const child = this.assetTree.nodes.get(childId);
        if (child && visit(child)) { this.expandedAssetIds.add(node.id); return true; }
      }
      return false;
    };
    this.assetTree.roots.some(visit);
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }
  /** 返回从根资产到当前设备的名称路径，用于“仪表地址”的层级展示。 */
  /** 返回目标节点从根到自身的层级名称路径，用于仪表地址展示。 */
  private getAssetPath(targetId: string): string[] {
    const visit = (node: SmartLightningMonitoringAssetNode, ancestors: string[]): string[] | undefined => {
      const path = [...ancestors, node.name];
      if (node.id === targetId) { return path; }
      for (const childId of node.childIds) {
        const child = this.assetTree.nodes.get(childId);
        const result = child ? visit(child, path) : undefined;
        if (result) { return result; }
      }
      return undefined;
    };
    for (const root of this.assetTree.roots) {
      const result = visit(root, []);
      if (result) { return result; }
    }
    return [];
  }
  /**
   * 先读取设备所属配置，再按该配置定义的 key 批量读取最新遥测。
   * 任意一项缺失时由 buildSmartLightningRealtimeMetrics 显示占位符，不使用模拟数值。
   */
  /** 按设备档案读取实时遥测指标；没有上报的测点继续展示占位符。 */
  private loadRealtimeMetrics(deviceId?: string, configureCharts = true): void {
    if (!deviceId) { return; }
    this.realtimeLoading = true;
    this.realtimeLoadError = false;
    this.deviceService.getDeviceInfo(deviceId).pipe(
      switchMap((device) => {
        const definitions = getSmartLightningRealtimeMetricDefinitions(device.deviceProfileName);
        if (!definitions.length) {
          return of({deviceProfileName: device.deviceProfileName, latest: {}, loadError: false});
        }
        // 只查询规则链输出的规范键，避免新旧键并存造成统计口径不一致。
        const readKeys = Array.from(new Set(definitions.map((definition) => definition.key)));
        return this.attributeService.getEntityTimeseriesLatest(new DeviceId(deviceId), readKeys).pipe(
          switchMap((latest) => of({deviceProfileName: device.deviceProfileName, latest, loadError: false})),
          catchError(() => of({deviceProfileName: device.deviceProfileName, latest: {}, loadError: true}))
        );
      }),
      catchError(() => of({deviceProfileName: undefined, latest: {}, loadError: true})),
      takeUntil(this.destroy$)
    ).subscribe(({deviceProfileName, latest, loadError}) => {
      // 切换设备后，上一台设备的延迟响应不能覆盖当前型号和测点时间。
      if (deviceId !== this.requestedDeviceId) {
        return;
      }
      this.currentDeviceProfileName = deviceProfileName;
      this.realtimeLoading = false;
      this.realtimeLoadError = loadError;
      if (configureCharts) {
        this.configureCharts(this.requestedDeviceId, deviceProfileName);
      }
      Object.entries(latest).forEach(([key, values]) => {
        const firstValue = Array.isArray(values) ? values[0] : undefined;
        if (firstValue?.value !== undefined) {
          this.mergeRealtimeValue(key, Number(firstValue.ts), String(firstValue.value));
        }
      });
      // 共享订阅首帧可能早于详情加载；按原始时间合并缓存，避免依赖下一条推送才能补齐。
      this.realtimeService.getLatestTelemetryValues(deviceId).forEach((sample, key) => {
        this.mergeRealtimeValue(key, sample.ts, sample.value);
      });
      this.updateRealtimePresentation();
      this.productSpecification = getSmartLightningProductSpecification(deviceProfileName);
      this.cd.markForCheck();
    });
  }

  /** 只重试当前设备的实时读取，避免重置已经选好的趋势图周期。 */
  retryRealtimeMetrics(): void {
    this.loadRealtimeMetrics(this.requestedDeviceId, false);
  }

  /** 将当前设备的 WebSocket 遥测直接合并进卡片、时间和趋势图。 */
  private applyRealtimeTelemetry(change: SmartLightningDeviceTelemetryChange): void {
    change.values.forEach((item, key) => this.mergeRealtimeValue(key, item.ts, item.value));
    this.updateRealtimePresentation();
    this.appendRealtimeChartPoints(change);
    this.cd.markForCheck();
  }

  /** 仅接受时间更新或相同的值，防止并发 HTTP 响应把 WebSocket 新值覆盖成旧值。 */
  private mergeRealtimeValue(key: string, timestamp: number, value: string): void {
    // 查询缺失测点时平台可能返回“当前时间 + null”，不能让占位记录覆盖真实值或推新上报时间。
    // HTTP 已将值转成字符串，因此同时排除原生空值、空文本及文本化的 null/undefined；0 和 false 保留。
    if (value == null || !String(value).trim() || /^(null|undefined)$/i.test(String(value).trim())) {
      return;
    }
    if (!Number.isFinite(timestamp) || timestamp <= 0) { return; }
    const current = this.latestRealtimeValues.get(key);
    if (!current || timestamp >= current.ts) {
      this.latestRealtimeValues.set(key, {ts: timestamp, value});
    }
  }

  /** 根据快照更新卡片；时间只取当前型号展示测点，平台在线状态和活跃时间不参与。 */
  private updateRealtimePresentation(): void {
    const rawValues = new Map<string, unknown>();
    this.latestRealtimeValues.forEach((item, key) => rawValues.set(key, item.value));
    this.detail = {
      ...this.detail,
      metrics: buildSmartLightningRealtimeMetrics(this.currentDeviceProfileName, rawValues, this.detail.metrics)
    };
    let latestTimestamp = 0;
    for (const definition of getSmartLightningRealtimeMetricDefinitions(this.currentDeviceProfileName)) {
      const timestamp = this.latestRealtimeValues.get(definition.key)?.ts;
      if (timestamp !== undefined && Number.isFinite(timestamp) && timestamp > latestTimestamp) {
        latestTimestamp = timestamp;
      }
    }
    this.realtimeUpdatedAt = latestTimestamp > 0
      ? new Date(latestTimestamp).toLocaleString('zh-CN', {hour12: false})
      : '--';
  }

  /** 当前版本按实体告警查询的原生路由是 /api/alarm/{entityType}/{entityId}。 */
  private loadDeviceAlarms(deviceId?: string): void {
    if (!deviceId) { return; }
    this.alarmsLoading = true; this.alarmsLoadError = false;
    this.detail = {...this.detail, alarms: []};
    const query = new AlarmQuery(
      new DeviceId(deviceId),
      new TimePageLink(10, 0, null, {property: 'createdTime', direction: Direction.DESC}),
      AlarmSearchStatus.ANY,
      null,
      false
    );
    this.alarmService.getAlarms(query).pipe(takeUntil(this.destroy$)).subscribe({
      next: (pageData) => {
        this.detail = {...this.detail, alarms: this.sortDeviceAlarms(pageData.data).map((alarm) => this.toDeviceAlarmRow(alarm))};
        this.alarmsLoading = false; this.cd.markForCheck();
      },
      error: () => { this.alarmsLoading = false; this.alarmsLoadError = true; this.cd.markForCheck(); }
    });
  }
  /** 把告警实体转为页面报警表的行结构（中文等级与处理状态）。 */
  private toDeviceAlarmRow(alarm: AlarmInfo): {item: string; severity: string; status: string; time: string} {
    const severityLabels: Record<AlarmSeverity, string> = {
      [AlarmSeverity.CRITICAL]: '严重', [AlarmSeverity.MAJOR]: '重要', [AlarmSeverity.MINOR]: '次要',
      [AlarmSeverity.WARNING]: '警告', [AlarmSeverity.INDETERMINATE]: '不确定'
    };
    return {
      item: alarm.type || '--',
      severity: severityLabels[alarm.severity] || alarm.severity || '不确定',
      status: alarm.cleared ? '已处理' : '未处理',
      time: alarm.startTs ? new Date(alarm.startTs).toLocaleString('zh-CN', {hour12: false}) : '--'
    };
  }
  /** 未清除告警优先且按严重程度排序；已清除告警只按发生时间从新到旧排序。 */
  private sortDeviceAlarms(alarms: AlarmInfo[]): AlarmInfo[] {
    const severityRank: Record<AlarmSeverity, number> = {
      [AlarmSeverity.CRITICAL]: 5, [AlarmSeverity.MAJOR]: 4, [AlarmSeverity.MINOR]: 3,
      [AlarmSeverity.WARNING]: 2, [AlarmSeverity.INDETERMINATE]: 1
    };
    return [...alarms].sort((left, right) => {
      const handledOrder = Number(left.cleared) - Number(right.cleared);
      if (handledOrder) { return handledOrder; }
      if (left.cleared && right.cleared) { return right.startTs - left.startTs; }
      return (severityRank[right.severity] || 0) - (severityRank[left.severity] || 0) || right.startTs - left.startTs;
    });
  }
  /** 递归展开选中节点的全部后代资产，保证新选中子树完整可见。 */
  private openDescendants(id: string): void { const node = this.assetTree.nodes.get(id); if (!node) { return; } node.childIds.forEach((childId) => { this.expandedAssetIds.add(childId); this.openDescendants(childId); }); this.expandedAssetIds = new Set(this.expandedAssetIds); }
  /** 清理上一个设备的图表实例和请求状态，避免切换设备后沿用旧型号曲线。 */
  private resetCharts(): void {
    this.chartDefinitions = [];
    this.allChartDefinitions = [];
    this.chartSeriesData = {};
    this.selectedChartPeriods = {};
    this.chartsLoading = false;
    this.showAllChartsDialog = false;
    this.disposeCharts();
  }

  /** 销毁当前视图中的 ECharts，切换主视图/弹窗时避免复用已移除的 DOM。 */
  private disposeCharts(): void {
    this.charts.forEach((chart) => chart.dispose());
    this.charts.clear();
  }

  /** 读取型号职责映射，再按设备实际存在的遥测键决定最终显示哪些图表。 */
  private configureCharts(deviceId: string | undefined, deviceProfileName: string | undefined): void {
    this.resetCharts();
    const definitions = getSmartLightningChartDefinitions(deviceProfileName);
    if (!deviceId || !definitions.length) {
      this.cd.markForCheck();
      return;
    }
    this.chartsLoading = true;
    this.detailApi.getTimeseriesKeys(deviceId).pipe(
      catchError(() => of([])),
      takeUntil(this.destroy$)
    ).subscribe((availableKeys) => {
      const availableDefinitions = filterSmartLightningChartDefinitions(definitions, availableKeys, definitions.length);
      // 弹窗保留型号的完整职责定义，即使其中某个参数暂时没有历史值，也要给出明确的空状态。
      this.allChartDefinitions = definitions.map((definition) => ({...definition, series: [...definition.series]}));
      this.chartDefinitions = availableDefinitions.slice(0, 4);
      this.allChartDefinitions.forEach((definition) => {
        this.selectedChartPeriods[definition.id] = 'day';
      });
      this.chartsLoading = false;
      this.cd.markForCheck();
      this.allChartDefinitions.forEach((definition) => this.loadChartData(deviceId, definition, 'day'));
    });
  }

  /** 打开全部参数趋势弹窗；只有存在第五张及以上图卡时才允许打开。 */
  openAllChartsDialog(): void {
    if (this.allChartDefinitions.length <= this.chartDefinitions.length) {
      return;
    }
    this.exitChartFullscreen();
    this.disposeCharts();
    this.showAllChartsDialog = true;
    this.cd.markForCheck();
    setTimeout(() => this.renderCharts());
  }

  /** 关闭全部参数趋势弹窗并恢复主页面的四张优先图卡。 */
  closeAllChartsDialog(): void {
    this.exitChartFullscreen();
    this.disposeCharts();
    this.showAllChartsDialog = false;
    this.cd.markForCheck();
    setTimeout(() => this.renderCharts());
  }

  /** 按单个图卡的周期读取历史数据；日/周/月互不影响。 */
  private loadChartData(deviceId: string, definition: SmartLightningChartDefinition, period: SmartLightningChartPeriod): void {
    const keys = buildSmartLightningChartReadKeys([definition]);
    if (!keys.length) {
      return;
    }
    const window = getSmartLightningChartWindow(period);
    this.detailApi.getHistory(deviceId, keys, window.startTs, window.endTs).pipe(
      catchError(() => of({})),
      takeUntil(this.destroy$)
    ).subscribe((timeseries) => {
      this.chartSeriesData = {...this.chartSeriesData, [definition.id]: buildSmartLightningChartSeriesData(definition, timeseries)};
      this.cd.markForCheck();
      setTimeout(() => this.renderCharts());
    });
  }

  /** 把 WebSocket 最新数值直接追加到每张图当前周期内，避免等待切换周期后重新查询。 */
  private appendRealtimeChartPoints(change: SmartLightningDeviceTelemetryChange): void {
    let changed = false;
    const nextChartData = {...this.chartSeriesData};
    this.allChartDefinitions.forEach((definition) => {
      const period = this.selectedChartPeriods[definition.id] || 'day';
      const window = getSmartLightningChartWindow(period);
      const currentSeries = (nextChartData[definition.id] || []).map((item) => ({
        definition: item.definition,
        points: [...item.points]
      }));
      let definitionChanged = false;
      definition.series.forEach((seriesDefinition) => {
        const latest = change.values.get(seriesDefinition.key);
        const value = Number(latest?.value);
        if (!latest || !Number.isFinite(value) || latest.ts < window.startTs || latest.ts > window.endTs + 60_000) {
          return;
        }
        let seriesData = currentSeries.find((item) => item.definition.key === seriesDefinition.key);
        if (!seriesData) {
          seriesData = {definition: seriesDefinition, points: []};
          currentSeries.push(seriesData);
        }
        seriesData.points = seriesData.points
          .filter((point) => point[0] >= window.startTs && point[0] !== latest.ts)
          .concat([[latest.ts, value]])
          .sort((left, right) => left[0] - right[0]);
        definitionChanged = true;
      });
      if (definitionChanged) {
        nextChartData[definition.id] = currentSeries;
        changed = true;
      }
    });
    if (changed) {
      this.chartSeriesData = nextChartData;
      setTimeout(() => this.renderCharts());
    }
  }

  /** 切换指定趋势图的统计周期，只影响该图表自身。 */
  setChartPeriod(id: string, period: SmartLightningChartPeriod): void {
    this.selectedChartPeriods = {...this.selectedChartPeriods, [id]: period};
    if (this.requestedDeviceId) {
      const definition = this.allChartDefinitions.find((item) => item.id === id);
      if (definition) {
        this.loadChartData(this.requestedDeviceId, definition, period);
      }
    }
    const chart = this.charts.get(id);
    if (chart) { chart.setOption(this.createChartOption(id), true); }
  }
  /** 根据当前动态图卡集合创建或更新 ECharts 实例。 */
  private renderCharts(): void {
    const visibleDefinitions = this.showAllChartsDialog ? this.allChartDefinitions : this.chartDefinitions;
    const visibleIds = new Set(visibleDefinitions.map((definition) => definition.id));
    this.charts.forEach((chart, id) => {
      if (!visibleIds.has(id)) {
        chart.dispose();
        this.charts.delete(id);
      }
    });
    this.chartHosts.forEach((host) => {
      const id = host.nativeElement.dataset.chart;
      if (!id || !visibleIds.has(id)) {
        return;
      }
      let chart = this.charts.get(id);
      if (!chart) {
        chart = echarts.init(host.nativeElement, undefined, {renderer: 'svg'});
        this.charts.set(id, chart);
      }
      chart.setOption(this.createChartOption(id), true);
    });
  }
  /** 按真实历史遥测点生成 ECharts 配置；没有点时只显示空状态提示。 */
  private createChartOption(id: string): EChartsOption {
    const definition = this.chartDefinitions.find((item) => item.id === id);
    const chartData = this.chartSeriesData[id] || [];
    const period = this.selectedChartPeriods[id] || 'day';
    const chartWindow = getSmartLightningChartWindow(period);
    const isDayChart = period === 'day';
    // 日图只从当前图中最早的遥测点开始展示；没有数据时仍回退到当天 0 点。
    // 查询窗口依旧从 0 点开始，因此不会因为调整显示范围而漏掉当天数据。
    const dataTimestamps = chartData.flatMap((item) => item.points.map((point) => point[0]));
    const firstDataTimestamp = dataTimestamps.length
      ? Math.min(...dataTimestamps)
      : chartWindow.startTs;
    const xAxisStart = isDayChart
      ? Math.max(chartWindow.startTs, Math.min(firstDataTimestamp, chartWindow.endTs))
      : chartWindow.startTs;
    const colors = ['#ff5656', '#814ce3', '#11a94d', '#195ce8'];
    const series = chartData.map((item, index) => ({
      name: item.definition.label,
      type: 'line' as const,
      smooth: true,
      data: item.points,
      symbol: 'none',
      lineStyle: {width: 2.2, color: colors[index % colors.length]},
      areaStyle: {color: 'rgba(25,92,232,.04)'}
    }));
    const yAxisBounds = buildSmartLightningYAxisBounds(chartData);
    return {
      animation: false,
      tooltip: {trigger: 'axis'},
      legend: {top: 4, right: 8, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 10, color: '#61778b'}},
      grid: {top: 42, left: 40, right: 16, bottom: 38},
      xAxis: {
        type: 'time',
        min: xAxisStart,
        max: chartWindow.endTs,
        minInterval: isDayChart ? 60 * 60 * 1000 : undefined,
        maxInterval: isDayChart ? 60 * 60 * 1000 : undefined,
        axisTick: {show: false},
        axisLine: {lineStyle: {color: '#cfdae3'}},
        axisLabel: {
          fontSize: 9,
          color: '#71859a',
          hideOverlap: true,
          // 日图只显示整点，避免出现 10:30、11:45 等零散刻度。
          formatter: isDayChart ? ((value: number) => {
            const time = new Date(value);
            return time.getMinutes() === 0 && time.getSeconds() === 0
              ? `${String(time.getHours()).padStart(2, '0')}:00`
              : '';
          }) : undefined
        }
      },
      yAxis: {type: 'value', ...(yAxisBounds || {}), scale: true, name: definition?.series[0]?.unit || '', nameTextStyle: {fontSize: 10, color: '#71859a'}, axisLabel: {fontSize: 9, color: '#71859a'}, splitLine: {lineStyle: {color: '#eef2f5'}}},
      series,
      ...(series.length ? {} : {graphic: {type: 'text', left: 'center', top: 'middle', style: {text: '暂无遥测数据', fill: '#8a9aaa', fontSize: 12}}})
    };
  }
}
