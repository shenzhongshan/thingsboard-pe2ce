/** 文件职责：展示单台电气安全设备的真实资料、遥测趋势和最近告警。 */
import {
  AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef,
  OnDestroy, QueryList, ViewChildren
} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {forkJoin, of, Subject} from 'rxjs';
import {auditTime, catchError, startWith, takeUntil} from 'rxjs/operators';
import {AlarmInfo, AlarmSeverity} from '@shared/models/alarm.models';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningMonitoringAssetPaths,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import {
  SmartLightningDeviceTelemetryChange,
  SmartLightningRealtimeService
} from '@home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service';
import {ElectricalSafetyApiService, ElectricalSafetyDeviceContext} from '../electrical-safety-api.service';
import {
  ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
  parseElectricalSafetyActiveValue
} from './electrical-safety-monitoring.data';
import {ElectricalSafetyDeviceDetailApiService} from './electrical-safety-device-detail-api.service';
import {
  buildElectricalSafetyChartReadKeys,
  buildElectricalSafetyChartSeriesData,
  buildElectricalSafetyYAxisBounds,
  ElectricalSafetyDetailChartDefinition,
  ElectricalSafetyDetailChartSeries,
  filterElectricalSafetyChartDefinitions,
  getElectricalSafetyChartDefinitions,
  getElectricalSafetyChartWindow
} from './electrical-safety-device-detail.charts';
import {
  buildElectricalSafetyDetailMetrics,
  ElectricalSafetyDetailMetric,
  ElectricalSafetyDetailPeriod,
  ElectricalSafetyDetailSample
} from './electrical-safety-device-detail.data';

/** 最近告警表格行，状态表示是否清除，和设备在线状态互不混用。 */
interface ElectricalSafetyDetailAlarmRow {
  item: string;
  severity: string;
  status: string;
  time: string;
}

/** 页面仅允许进入当前用户可见且 productCategory 为 electrical_safety 的设备。 */
@Component({
  selector: 'tb-electrical-safety-device-detail',
  templateUrl: './electrical-safety-device-detail.component.html',
  styleUrls: ['./electrical-safety-device-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ElectricalSafetyDeviceDetailComponent implements AfterViewInit, OnDestroy {
  @ViewChildren('chartHost') chartHosts!: QueryList<ElementRef<HTMLDivElement>>;

  /** 资产树和设备资料都来自电气安全模块接口，不借用防雷的设备查询结果。 */
  assetTree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  expandedAssetIds = new Set<string>();
  selectedAssetIds = new Set<string>();
  assetSearch = '';
  assetsLoading = true;
  assetsLoadError = false;
  deviceLoading = true;
  deviceLoadError = false;
  deviceName = '--';
  productName = '--';
  deviceModel = '--';
  deviceProtocol = '--';
  deviceLocation = '--';
  deviceActive = false;
  lastActivityTime = '--';

  /** 缺失测点和读取失败分别呈现，避免把请求失败当成设备没有数据。 */
  metrics: ElectricalSafetyDetailMetric[] = [];
  telemetryLoading = false;
  telemetryLoadError = false;
  realtimeUpdatedAt = '--';
  alarms: ElectricalSafetyDetailAlarmRow[] = [];
  alarmsLoading = false;
  alarmsLoadError = false;
  returnUrl = '/electrical-safety/monitor';
  readonly chartPeriodOptions: ReadonlyArray<{value: ElectricalSafetyDetailPeriod; label: string}> = [
    {value: 'day', label: '日'}, {value: 'week', label: '周'}, {value: 'month', label: '月'}
  ];
  chartDefinitions: ElectricalSafetyDetailChartDefinition[] = [];
  allChartDefinitions: ElectricalSafetyDetailChartDefinition[] = [];
  chartSeriesData: Record<string, ElectricalSafetyDetailChartSeries[]> = {};
  selectedChartPeriods: Record<string, ElectricalSafetyDetailPeriod> = {};
  chartErrorIds = new Set<string>();
  showAllChartsDialog = false;
  fullscreenChartCard?: HTMLElement;

  /** 详情定位模式只允许选中当前设备，资产树仍可展开和搜索。 */
  get selectableAssetIds(): ReadonlySet<string> | null {
    return this.deviceId ? new Set([this.deviceId]) : null;
  }

  /** 告警树标记仅用于当前设备，依据平台告警的 cleared 字段判断。 */
  get activeAlarmDeviceIds(): ReadonlySet<string> {
    return this.deviceId && this.alarms.some((alarm) => alarm.status === '未清除')
      ? new Set([this.deviceId]) : new Set<string>();
  }

  get returnLabel(): string {
    return this.returnUrl === '/electrical-safety/query' ? '返回数据查询' : '返回数据监测';
  }

  private deviceId?: string;
  private availableKeys: string[] = [];
  private readonly latestValues = new Map<string, ElectricalSafetyDetailSample>();
  private readonly charts = new Map<string, ECharts>();
  private readonly destroy$ = new Subject<void>();
  private resizeObserver?: ResizeObserver;
  private requestId = 0;
  private readonly onWindowResize = (): void => {
    if (this.fullscreenChartCard) {
      this.updateFullscreenBounds(this.fullscreenChartCard);
    }
    this.charts.forEach((chart) => chart.resize());
  };

  constructor(
    private readonly api: ElectricalSafetyApiService,
    private readonly detailApi: ElectricalSafetyDeviceDetailApiService,
    private readonly realtimeService: SmartLightningRealtimeService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly changeDetector: ChangeDetectorRef
  ) {}

  /** 页面建立时先订阅共享 WebSocket，再按路由设备 ID 加载模块数据。 */
  ngAfterViewInit(): void {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') === '/electrical-safety/query'
      ? '/electrical-safety/query' : '/electrical-safety/monitor';
    this.realtimeService.deviceTelemetryChanged$.pipe(takeUntil(this.destroy$)).subscribe((change) => {
      if (change.deviceId === this.deviceId && !this.deviceLoadError) {
        this.applyRealtimeChange(change);
      }
    });
    this.realtimeService.alarmChanged$.pipe(auditTime(600), takeUntil(this.destroy$)).subscribe(() => {
      if (this.deviceId && !this.deviceLoading && !this.deviceLoadError) {
        this.loadAlarms(this.deviceId, this.requestId);
      }
    });
    this.realtimeService.acquire();
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.loadDevice(params.get('deviceId') || '');
    });
    this.resizeObserver = typeof ResizeObserver !== 'undefined'
      ? new ResizeObserver(() => this.charts.forEach((chart) => chart.resize())) : undefined;
    this.chartHosts.changes.pipe(startWith(this.chartHosts), takeUntil(this.destroy$)).subscribe(() => {
      this.chartHosts.forEach((host) => this.resizeObserver?.observe(host.nativeElement));
      setTimeout(() => this.renderCharts());
    });
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.onWindowResize);
    }
  }

  /** 离开详情时释放共享订阅、窗口监听和图表实例。 */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
    this.exitChartFullscreen();
    this.resizeObserver?.disconnect();
    this.disposeCharts();
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.onWindowResize);
    }
  }

  /** 只接受电气安全监测页或查询页的返回地址，阻止任意跳转。 */
  goBack(event: MouseEvent): void {
    event.preventDefault();
    if (this.returnUrl === '/electrical-safety/query') {
      this.router.navigateByUrl(this.returnUrl);
      return;
    }
    const queryParams: Record<string, string> = {};
    const selectedAssetId = this.route.snapshot.queryParamMap.get('returnAssetId');
    const selectedRootAssetId = this.route.snapshot.queryParamMap.get('returnRootAssetId');
    if (selectedAssetId) {
      queryParams['selectedAssetId'] = selectedAssetId;
    }
    if (selectedRootAssetId) {
      queryParams['selectedRootAssetId'] = selectedRootAssetId;
    }
    this.router.navigate([this.returnUrl], {queryParams});
  }

  /** 详情页资产树仅作设备位置定位，不切换到另一台设备。 */
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    if (node.id === this.deviceId) {
      this.selectedAssetIds = new Set([node.id]);
    }
  }

  /** 展开状态使用新集合引用，确保 OnPush 模板正确更新。 */
  toggleAssetNode(node: SmartLightningMonitoringAssetNode): void {
    const next = new Set(this.expandedAssetIds);
    if (next.has(node.id)) {
      next.delete(node.id);
    } else {
      next.add(node.id);
    }
    this.expandedAssetIds = next;
  }

  /** 先用模块接口核对可见性和分类，再访问该设备的原生遥测、告警。 */
  private loadDevice(deviceId: string): void {
    const requestId = ++this.requestId;
    this.resetDeviceState();
    this.deviceId = deviceId || undefined;
    if (!deviceId) {
      this.deviceLoading = false;
      this.deviceLoadError = true;
      this.assetsLoading = false;
      this.changeDetector.markForCheck();
      return;
    }
    forkJoin({
      contexts: this.api.getDeviceContexts(),
      tree: this.api.getAssetTree().pipe(catchError(() => of(null)))
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({contexts, tree}) => {
        if (requestId !== this.requestId) {
          return;
        }
        const context = (contexts || []).find((item) => item.deviceId === deviceId
          && item.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY);
        if (!context) {
          this.deviceLoading = false;
          this.deviceLoadError = true;
          this.assetsLoading = false;
          this.changeDetector.markForCheck();
          return;
        }
        this.applyDeviceContext(context);
        this.assetsLoadError = tree === null;
        this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(tree?.roots || []);
        this.locateDeviceInTree(deviceId);
        this.assetsLoading = false;
        this.deviceLoading = false;
        this.changeDetector.markForCheck();
        this.loadTelemetry(deviceId, requestId);
        this.loadAlarms(deviceId, requestId);
      },
      error: () => {
        if (requestId !== this.requestId) {
          return;
        }
        this.assetsLoading = false;
        this.deviceLoading = false;
        this.deviceLoadError = true;
        this.changeDetector.markForCheck();
      }
    });
  }

  /** 清空上台设备的快照和图表，避免路由切换期间短暂显示错误设备数据。 */
  private resetDeviceState(): void {
    this.exitChartFullscreen();
    this.disposeCharts();
    this.assetTree = {roots: [], nodes: new Map()};
    this.expandedAssetIds = new Set();
    this.selectedAssetIds = new Set();
    this.assetsLoading = true;
    this.assetsLoadError = false;
    this.deviceLoading = true;
    this.deviceLoadError = false;
    this.deviceName = '--';
    this.productName = '--';
    this.deviceModel = '--';
    this.deviceProtocol = '--';
    this.deviceLocation = '--';
    this.deviceActive = false;
    this.lastActivityTime = '--';
    this.availableKeys = [];
    this.latestValues.clear();
    this.metrics = [];
    this.telemetryLoading = false;
    this.telemetryLoadError = false;
    this.realtimeUpdatedAt = '--';
    this.alarms = [];
    this.alarmsLoading = false;
    this.alarmsLoadError = false;
    this.chartDefinitions = [];
    this.allChartDefinitions = [];
    this.chartSeriesData = {};
    this.selectedChartPeriods = {};
    this.chartErrorIds = new Set();
    this.showAllChartsDialog = false;
  }

  /** 从设备上下文读取已授权的基础字段，时间仍保持后端毫秒时间戳语义。 */
  private applyDeviceContext(context: ElectricalSafetyDeviceContext): void {
    this.deviceName = context.deviceName || '--';
    this.productName = context.productName?.trim() || '--';
    this.deviceModel = context.deviceProfileName?.trim() || '--';
    this.deviceProtocol = context.deviceProtocol?.trim() || '--';
    this.deviceActive = context.active === true;
    this.lastActivityTime = context.lastActivityTime && Number.isFinite(context.lastActivityTime)
      ? new Date(context.lastActivityTime).toLocaleString('zh-CN', {hour12: false}) : '--';
  }

  /** 展开当前设备的全部上级资产；设备没有资产关系时仍可查看其详情。 */
  private locateDeviceInTree(deviceId: string): void {
    const path = getSmartLightningMonitoringAssetPaths(this.assetTree).get(deviceId);
    this.deviceLocation = path || '--';
    if (!this.assetTree.nodes.has(deviceId)) {
      return;
    }
    this.selectedAssetIds = new Set([deviceId]);
    const expanded = new Set<string>();
    const visit = (node: SmartLightningMonitoringAssetNode): boolean => {
      if (node.id === deviceId) {
        return true;
      }
      for (const childId of node.childIds) {
        const child = this.assetTree.nodes.get(childId);
        if (child && visit(child)) {
          expanded.add(node.id);
          return true;
        }
      }
      return false;
    };
    this.assetTree.roots.some(visit);
    this.expandedAssetIds = expanded;
  }

  /** 先读取真实遥测键，再一次查询这些键的最新值；共享订阅缓存可补齐并发更新。 */
  private loadTelemetry(deviceId: string, requestId: number): void {
    this.telemetryLoading = true;
    this.detailApi.getTimeseriesKeys(deviceId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (keys) => {
        if (requestId !== this.requestId) {
          return;
        }
        this.availableKeys = (keys || []).filter((key) => !!key);
        this.configureCharts(this.deviceModel, this.availableKeys);
        this.realtimeService.getLatestTelemetryValues(deviceId).forEach((sample, key) => {
          this.mergeLatestValue(key, sample.ts, sample.value);
        });
        const cachedActive = parseElectricalSafetyActiveValue(
          this.realtimeService.getLatestTelemetryValues(deviceId).get('active')?.value
        );
        if (cachedActive !== undefined) {
          this.deviceActive = cachedActive;
        }
        const cachedActivityTime = this.realtimeService.getLatestActivityTime(deviceId);
        if (cachedActivityTime != null && Number.isFinite(cachedActivityTime)) {
          this.lastActivityTime = new Date(cachedActivityTime).toLocaleString('zh-CN', {hour12: false});
        }
        if (!this.availableKeys.length) {
          this.refreshTelemetryPresentation();
          this.telemetryLoading = false;
          this.changeDetector.markForCheck();
          return;
        }
        this.detailApi.getLatest(deviceId, this.availableKeys).pipe(takeUntil(this.destroy$)).subscribe({
          next: (latest) => {
            if (requestId !== this.requestId) {
              return;
            }
            for (const [key, values] of Object.entries(latest || {})) {
              const sample = Array.isArray(values) ? values[0] : undefined;
              if (sample?.value !== undefined) {
                this.mergeLatestValue(key, Number(sample.ts), String(sample.value));
              }
            }
            this.refreshTelemetryPresentation();
            this.telemetryLoading = false;
            this.changeDetector.markForCheck();
            for (const definition of this.chartDefinitions) {
              this.loadChartData(deviceId, definition, 'day', requestId);
            }
          },
          error: () => {
            if (requestId === this.requestId) {
              this.telemetryLoading = false;
              this.telemetryLoadError = true;
              this.changeDetector.markForCheck();
            }
          }
        });
      },
      error: () => {
        if (requestId === this.requestId) {
          this.telemetryLoading = false;
          this.telemetryLoadError = true;
          this.changeDetector.markForCheck();
        }
      }
    });
  }

  /** 只接受时间较新的值，避免首次 HTTP 响应覆盖先到的 WebSocket 更新。 */
  private mergeLatestValue(key: string, ts: number, value: string): void {
    // 平台为缺失测点返回的“当前时间 + null”是占位数据，不代表设备真实上报。
    // 同时过滤 HTTP 字符串化后的空值，保留有效的 0/false，防止重新进入详情时产生虚假的新时间。
    if (value == null || !String(value).trim() || /^(null|undefined)$/i.test(String(value).trim())) {
      return;
    }
    if (!Number.isFinite(ts) || ts <= 0) {
      return;
    }
    const previous = this.latestValues.get(key);
    if (!previous || ts >= previous.ts) {
      this.latestValues.set(key, {ts, value});
    }
  }

  /** 更新测点卡片；最新时间只取实际展示测点，已排除的平台状态和系统时间键不参与。 */
  private refreshTelemetryPresentation(): void {
    this.metrics = buildElectricalSafetyDetailMetrics(this.availableKeys, this.latestValues, this.deviceModel);
    let latestTimestamp = 0;
    for (const metric of this.metrics) {
      const timestamp = this.latestValues.get(metric.key)?.ts;
      if (timestamp !== undefined && Number.isFinite(timestamp) && timestamp > latestTimestamp) {
        latestTimestamp = timestamp;
      }
    }
    this.realtimeUpdatedAt = latestTimestamp > 0
      ? new Date(latestTimestamp).toLocaleString('zh-CN', {hour12: false}) : '--';
  }

  /** 按设备型号取得趋势职责，再用设备真实时序键过滤并选出首屏优先图表。 */
  private configureCharts(deviceProfileName: string, availableKeys: readonly string[]): void {
    const definitions = getElectricalSafetyChartDefinitions(deviceProfileName);
    this.allChartDefinitions = definitions;
    this.chartDefinitions = filterElectricalSafetyChartDefinitions(definitions, availableKeys, 4);
    for (const definition of definitions) {
      this.selectedChartPeriods[definition.id] ??= 'day';
    }
  }

  /** 当前设备的 WebSocket 更新直接作用于实时卡片及当前图表周期。 */
  private applyRealtimeChange(change: SmartLightningDeviceTelemetryChange): void {
    change.values.forEach((sample, key) => this.mergeLatestValue(key, sample.ts, sample.value));
    const activeValue = change.values.get('active')?.value;
    if (activeValue !== undefined) {
      const active = parseElectricalSafetyActiveValue(activeValue);
      if (active !== undefined) {
        this.deviceActive = active;
      }
    }
    if (change.lastActivityTime) {
      this.lastActivityTime = new Date(change.lastActivityTime).toLocaleString('zh-CN', {hour12: false});
    }
    this.refreshTelemetryPresentation();
    for (const definition of this.allChartDefinitions) {
      const period = this.selectedChartPeriods[definition.id] || 'day';
      const window = getElectricalSafetyChartWindow(period);
      const current = (this.chartSeriesData[definition.id] || []).map((item) => ({
        definition: item.definition,
        points: [...item.points]
      }));
      let changed = false;
      for (const seriesDefinition of definition.series) {
        const sample = change.values.get(seriesDefinition.key);
        const value = Number(sample?.value);
        if (!sample || !Number.isFinite(value) || sample.ts < window.startTs || sample.ts > window.endTs + 60_000) {
          continue;
        }
        let previous = current.find((item) => item.definition.key === seriesDefinition.key);
        if (!previous) {
          previous = {definition: seriesDefinition, points: []};
          current.push(previous);
        }
        const points = [...(previous?.points || []).filter((point) => point[0] !== sample.ts), [sample.ts, value] as [number, number]]
          .sort((left, right) => left[0] - right[0]);
        previous.points = points;
        changed = true;
      }
      if (changed) {
        this.chartSeriesData = {...this.chartSeriesData, [definition.id]: current};
      }
    }
    this.changeDetector.markForCheck();
    setTimeout(() => this.renderCharts());
  }

  /** 按当前设备读取最近告警，事件名称使用 ThingsBoard 的 alarm.type 原值。 */
  private loadAlarms(deviceId: string, requestId: number): void {
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    this.detailApi.getDeviceAlarms(deviceId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        if (requestId !== this.requestId) {
          return;
        }
        const severityLabels: Record<AlarmSeverity, string> = {
          [AlarmSeverity.CRITICAL]: '严重', [AlarmSeverity.MAJOR]: '重要',
          [AlarmSeverity.MINOR]: '次要', [AlarmSeverity.WARNING]: '警告',
          [AlarmSeverity.INDETERMINATE]: '不确定'
        };
        this.alarms = (page.data || []).map((alarm: AlarmInfo) => ({
          item: alarm.type || '--',
          severity: severityLabels[alarm.severity] || '不确定',
          status: alarm.cleared ? '已清除' : '未清除',
          time: alarm.startTs ? new Date(alarm.startTs).toLocaleString('zh-CN', {hour12: false}) : '--'
        }));
        this.alarmsLoading = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        if (requestId === this.requestId) {
          this.alarmsLoading = false;
          this.alarmsLoadError = true;
          this.changeDetector.markForCheck();
        }
      }
    });
  }

  /** 单张趋势图独立维护周期；只有用户展开更多参数时才读取其余曲线。 */
  setChartPeriod(id: string, period: ElectricalSafetyDetailPeriod): void {
    this.selectedChartPeriods = {...this.selectedChartPeriods, [id]: period};
    const definition = this.allChartDefinitions.find((item) => item.id === id);
    if (definition && this.deviceId) {
      this.loadChartData(this.deviceId, definition, period, this.requestId);
    }
  }

  /** 以平台历史数据填充指定图表，错误图卡单独显示失败状态。 */
  private loadChartData(
    deviceId: string, definition: ElectricalSafetyDetailChartDefinition,
    period: ElectricalSafetyDetailPeriod, requestId: number
  ): void {
    const window = getElectricalSafetyChartWindow(period);
    const keys = buildElectricalSafetyChartReadKeys([definition]);
    this.detailApi.getHistory(deviceId, keys, window.startTs, window.endTs)
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: (timeseries) => {
          if (requestId !== this.requestId || this.selectedChartPeriods[definition.id] !== period) {
            return;
          }
          const currentWindow = getElectricalSafetyChartWindow(period);
          const historySeries = buildElectricalSafetyChartSeriesData(definition, timeseries);
          this.chartSeriesData = {
            ...this.chartSeriesData,
            [definition.id]: this.mergeLatestRealtimePoints(definition, historySeries, currentWindow)
          };
          const nextErrors = new Set(this.chartErrorIds);
          nextErrors.delete(definition.id);
          this.chartErrorIds = nextErrors;
          this.changeDetector.markForCheck();
          setTimeout(() => this.renderCharts());
        },
        error: () => {
          if (requestId === this.requestId && this.selectedChartPeriods[definition.id] === period) {
            this.chartErrorIds = new Set([...this.chartErrorIds, definition.id]);
            this.changeDetector.markForCheck();
            setTimeout(() => this.renderCharts());
          }
        }
      });
  }

  /** 历史请求可能晚于 WebSocket 帧返回；合并请求期间缓存的最新点，避免历史结果覆盖实时曲线。 */
  private mergeLatestRealtimePoints(
    definition: ElectricalSafetyDetailChartDefinition,
    historySeries: ElectricalSafetyDetailChartSeries[],
    window: {startTs: number; endTs: number}
  ): ElectricalSafetyDetailChartSeries[] {
    const seriesByKey = new Map<string, ElectricalSafetyDetailChartSeries>();
    for (const series of historySeries) {
      seriesByKey.set(series.definition.key, {definition: series.definition, points: [...series.points]});
    }
    for (const seriesDefinition of definition.series) {
      const latest = this.latestValues.get(seriesDefinition.key);
      const timestamp = Number(latest?.ts);
      const value = Number(latest?.value);
      if (!latest || !Number.isFinite(timestamp) || !Number.isFinite(value) ||
        timestamp < window.startTs || timestamp > window.endTs + 60_000) {
        continue;
      }
      let series = seriesByKey.get(seriesDefinition.key);
      if (!series) {
        series = {definition: seriesDefinition, points: []};
        seriesByKey.set(seriesDefinition.key, series);
      }
      series.points = [...series.points.filter((point) => point[0] !== timestamp), [timestamp, value] as [number, number]]
        .sort((left, right) => left[0] - right[0]);
    }
    return definition.series
      .map((seriesDefinition) => seriesByKey.get(seriesDefinition.key))
      .filter((series): series is ElectricalSafetyDetailChartSeries => !!series);
  }

  /** 弹窗沿用防雷详情的完整参数图卡布局，首次打开再读取其余历史曲线。 */
  openAllChartsDialog(): void {
    if (this.allChartDefinitions.length <= this.chartDefinitions.length) {
      return;
    }
    this.exitChartFullscreen();
    this.disposeCharts();
    this.showAllChartsDialog = true;
    if (this.deviceId) {
      for (const definition of this.allChartDefinitions.slice(this.chartDefinitions.length)) {
        if (!this.chartSeriesData[definition.id] && !this.chartErrorIds.has(definition.id)) {
          this.loadChartData(this.deviceId, definition, this.selectedChartPeriods[definition.id], this.requestId);
        }
      }
    }
    this.changeDetector.markForCheck();
    setTimeout(() => this.renderCharts());
  }

  /** 关闭弹窗时清理图表 DOM 引用并恢复页面中的四张图卡。 */
  closeAllChartsDialog(): void {
    this.exitChartFullscreen();
    this.disposeCharts();
    this.showAllChartsDialog = false;
    this.changeDetector.markForCheck();
    setTimeout(() => this.renderCharts());
  }

  /** 每张图独立放大到应用主内容区域，并保留顶部工具栏和左侧导航。 */
  toggleChartFullscreen(card: HTMLElement): void {
    if (this.fullscreenChartCard === card) {
      this.exitChartFullscreen();
      return;
    }
    this.exitChartFullscreen();
    this.fullscreenChartCard = card;
    this.updateFullscreenBounds(card);
    this.changeDetector.markForCheck();
    setTimeout(() => this.charts.forEach((chart) => chart.resize()));
  }

  /** 退出放大时删除动态边界变量，避免影响下一张图或路由。 */
  private exitChartFullscreen(): void {
    if (!this.fullscreenChartCard) {
      return;
    }
    for (const name of ['top', 'left', 'width', 'height']) {
      this.fullscreenChartCard.style.removeProperty(`--chart-fullscreen-${name}`);
    }
    this.fullscreenChartCard = undefined;
  }

  /** 使用主内容容器实际视口矩形确定放大位置，不写死导航尺寸。 */
  private updateFullscreenBounds(card: HTMLElement): void {
    const bounds = card.closest('.tb-main-content')?.getBoundingClientRect();
    if (!bounds) {
      return;
    }
    card.style.setProperty('--chart-fullscreen-top', `${Math.max(0, bounds.top)}px`);
    card.style.setProperty('--chart-fullscreen-left', `${Math.max(0, bounds.left)}px`);
    card.style.setProperty('--chart-fullscreen-width', `${Math.max(0, bounds.width)}px`);
    card.style.setProperty('--chart-fullscreen-height', `${Math.max(0, bounds.height)}px`);
  }

  /** 图表节点重建或数据变化后更新 ECharts，未显示的实例及时销毁。 */
  private renderCharts(): void {
    const definitions = this.showAllChartsDialog ? this.allChartDefinitions : this.chartDefinitions;
    const visible = new Set(definitions.map((definition) => definition.id));
    this.charts.forEach((chart, id) => {
      if (!visible.has(id)) {
        chart.dispose();
        this.charts.delete(id);
      }
    });
    this.chartHosts?.forEach((host) => {
      const id = host.nativeElement.dataset.chart;
      if (!id || !visible.has(id)) {
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

  /** 历史点使用原生时间轴，空值与接口失败分别给出明确图内提示。 */
  private createChartOption(id: string): EChartsOption {
    const definition = this.allChartDefinitions.find((item) => item.id === id);
    const period = this.selectedChartPeriods[id] || 'day';
    const window = getElectricalSafetyChartWindow(period);
    const chartData = this.chartSeriesData[id] || [];
    const isDayChart = period === 'day';
    const dataTimestamps = chartData.flatMap((item) => item.points.map((point) => point[0]));
    const firstDataTimestamp = dataTimestamps.length
      ? Math.min(...dataTimestamps)
      : window.startTs;
    const xAxisStart = isDayChart
      ? Math.max(window.startTs, Math.min(firstDataTimestamp, window.endTs))
      : window.startTs;
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
    const yAxisBounds = buildElectricalSafetyYAxisBounds(chartData);
    return {
      animation: false,
      tooltip: {trigger: 'axis'},
      legend: {top: 4, right: 8, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 10, color: '#61778b'}},
      grid: {top: 42, left: 40, right: 16, bottom: 38},
      xAxis: {
        type: 'time',
        min: xAxisStart,
        max: window.endTs,
        minInterval: isDayChart ? 60 * 60 * 1000 : undefined,
        maxInterval: isDayChart ? 60 * 60 * 1000 : undefined,
        axisTick: {show: false},
        axisLine: {lineStyle: {color: '#cfdae3'}},
        axisLabel: {
          fontSize: 9,
          color: '#71859a',
          hideOverlap: true,
          formatter: isDayChart ? ((value: number) => {
            const time = new Date(value);
            return time.getMinutes() === 0 && time.getSeconds() === 0
              ? `${String(time.getHours()).padStart(2, '0')}:00`
              : '';
          }) : undefined
        }
      },
      yAxis: {
        type: 'value', ...(yAxisBounds || {}), scale: true, name: definition?.series[0]?.unit || '',
        nameTextStyle: {fontSize: 10, color: '#71859a'},
        axisLabel: {fontSize: 9, color: '#71859a'},
        splitLine: {lineStyle: {color: '#eef2f5'}}
      },
      series,
      ...(series.length ? {} : {graphic: {
        type: 'text', left: 'center', top: 'middle',
        style: {text: this.chartErrorIds.has(id) ? '趋势读取失败' : '暂无遥测数据', fill: '#8a9aaa', fontSize: 12}
      }})
    };
  }

  /** 销毁旧图表，防止弹窗切换后复用已被 Angular 移除的 DOM。 */
  private disposeCharts(): void {
    this.charts.forEach((chart) => chart.dispose());
    this.charts.clear();
  }
}
