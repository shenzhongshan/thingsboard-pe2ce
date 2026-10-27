/**
 * 文件职责：自定义平台首页的页面控制器。
 * 该页面独立于 ThingsBoard 原生 /home，仅汇总当前用户可见的设备、资产、告警和地图点位。
 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { AssetService } from '@core/http/asset.service';
import { AlarmService } from '@core/http/alarm.service';
import { PageLink, MAX_SAFE_PAGE_SIZE } from '@shared/models/page/page-link';
import { AlarmQueryV2, AlarmSearchStatus, AlarmSeverity } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { forkJoin, of, Subject, timer } from 'rxjs';
import { auditTime, catchError, switchMap, takeUntil } from 'rxjs/operators';
import { SmartLightningMonitoringApiService } from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring-api.service';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningMonitoringAssetPaths
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import { ProductRealtimeService } from '@shared/realtime/product-realtime.service';
import {resolveSmartLightningMapCoordinates} from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.data';
import { HomeOverviewApiService, HomeOverviewDeviceContext } from './home-overview-api.service';
import {
  buildCategoryDistribution,
  buildDonutSegments,
  buildFunctionDistribution,
  categoryLabel,
  createEmptyHomeOverviewStats,
  buildDonutSegmentInfo,
  findDonutSegmentAtPoint,
  formatCoordinate,
  HOME_OVERVIEW_CATEGORY_OPTIONS,
  HOME_OVERVIEW_DISTRIBUTION_DONUT_INNER_RADIUS,
  HOME_OVERVIEW_DONUT_OUTER_RADIUS,
  HOME_OVERVIEW_DONUT_VIEWBOX_SIZE,
  HOME_OVERVIEW_STATUS_DONUT_INNER_RADIUS,
  HomeOverviewCategoryOption,
  HomeOverviewDonutSegment,
  HomeOverviewDonutSegmentInfo,
  HomeOverviewDevice,
  HomeOverviewDistributionItem,
  HomeOverviewStats,
  normalizeCoordinate,
  normalizeHomeOverviewFunctionCategory,
  normalizeCategory,
  applyHomeOverviewActiveUpdates,
  parseHomeOverviewActiveValue,
} from './home-overview.data';

interface HomeOverviewStatusChart {
  id: string;
  label: string;
  remainingLabel: string;
  value: number;
  color: string;
}

@Component({
  selector: 'tb-home-overview',
  standalone: false,
  templateUrl: './home-overview.component.html',
  styleUrls: ['./home-overview.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeOverviewComponent implements OnInit, OnDestroy {
  readonly stats: HomeOverviewStats = createEmptyHomeOverviewStats();
  readonly categoryOptions: HomeOverviewCategoryOption[] = HOME_OVERVIEW_CATEGORY_OPTIONS;
  devices: HomeOverviewDevice[] = [];
  visibleDevices: HomeOverviewDevice[] = [];
  categoryDistribution: HomeOverviewDistributionItem[] = [];
  functionDistribution: HomeOverviewDistributionItem[] = [];
  selectedCategory: string | null = null;
  selectedFunctionCategory: string | null = null;
  hoveredDonutSliceId: string | null = null;
  private hoveredDonutChartId: string | null = null;
  loading = true;
  errorMessage = '';
  private readonly destroy$ = new Subject<void>();
  private readonly deviceRefresh$ = new Subject<void>();
  private readonly assetRefresh$ = new Subject<void>();
  private readonly alarmRefresh$ = new Subject<void>();
  private readonly activeTelemetryRefresh$ = new Subject<void>();
  private readonly alarmSettleRefresh$ = new Subject<void>();
  private readonly pendingActiveDeviceIds = new Set<string>();
  /** 暂存已有设备坐标遥测变化，合并后从共享缓存读取完整经纬度对。 */
  private readonly pendingMapCoordinateDeviceIds = new Set<string>();
  private alarmNamesByDevice = new Map<string, string[]>();
  /** 设备 ID 到当前用户可见资产树完整路径的索引，供地图详情弹窗展示。 */
  deviceAssetPaths = new Map<string, string>();

  constructor(
    private readonly store: Store<AppState>,
    private readonly assetService: AssetService,
    private readonly alarmService: AlarmService,
    private readonly homeOverviewApiService: HomeOverviewApiService,
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly cd: ChangeDetectorRef,
    private readonly realtimeService: ProductRealtimeService
  ) {}

  ngOnInit(): void {
    this.loadOverview();
    this.subscribeToRealtimeChanges();
    this.realtimeService.acquire();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
  }

  /** 当前筛选层级下显示的环形图数据。 */
  get activeDistribution(): HomeOverviewDistributionItem[] {
    return this.selectedCategory ? this.functionDistribution : this.categoryDistribution;
  }

  /** 环形图百分比的分母：一级分类时为全部设备，二级分类时为选中一级分类设备。 */
  get activeDistributionTotal(): number {
    return this.selectedCategory
      ? this.devices.filter((device) => normalizeCategory(device.productCategory) === this.selectedCategory).length
      : this.devices.length;
  }

  get selectedCategoryLabel(): string {
    return this.selectedCategory ? categoryLabel(this.selectedCategory) : '全部设备';
  }

  get selectedFunctionLabel(): string {
    return this.functionDistribution.find((item) => item.key === this.selectedFunctionCategory)?.name || '';
  }

  /** 返回一级分类的设备数量，供筛选按钮展示。 */
  categoryCount(category: string): number {
    return this.categoryDistribution.find((item) => item.key === category)?.value || 0;
  }

  get statusCharts(): HomeOverviewStatusChart[] {
    return [
      {id: 'online', label: '在线设备', remainingLabel: '离线设备', value: this.stats.onlineTotal, color: '#32b866'},
      {id: 'offline', label: '离线设备', remainingLabel: '在线设备', value: this.stats.offlineTotal, color: '#ff4a57'},
      {id: 'unconfigured', label: '未配置坐标', remainingLabel: '已配置坐标', value: this.stats.unconfiguredTotal, color: '#f5b31b'}
    ];
  }

  distributionDonutSegments(): HomeOverviewDonutSegment[] {
    return buildDonutSegments(
      this.activeDistribution.map((item) => ({
        id: `distribution:${item.key}`,
        label: item.name,
        value: item.value,
        color: item.color
      })),
      this.activeDistributionTotal,
      HOME_OVERVIEW_DONUT_OUTER_RADIUS,
      HOME_OVERVIEW_DISTRIBUTION_DONUT_INNER_RADIUS
    );
  }

  statusDonutSegments(chart: HomeOverviewStatusChart): HomeOverviewDonutSegment[] {
    const total = this.devices.length;
    const value = Math.max(0, Math.min(chart.value, total));
    return buildDonutSegments([
      {id: `status:${chart.id}:value`, label: chart.label, value, color: chart.color},
      {id: `status:${chart.id}:remaining`, label: chart.remainingLabel, value: total - value, color: '#e6edf3'}
    ], total, HOME_OVERVIEW_DONUT_OUTER_RADIUS, HOME_OVERVIEW_STATUS_DONUT_INNER_RADIUS);
  }

  onDistributionDonutMove(event: MouseEvent): void {
    this.updateHoveredDonutSlice(
      event,
      'distribution',
      this.distributionDonutSegments(),
      HOME_OVERVIEW_DISTRIBUTION_DONUT_INNER_RADIUS
    );
  }

  onStatusDonutMove(event: MouseEvent, chart: HomeOverviewStatusChart): void {
    this.updateHoveredDonutSlice(
      event,
      'status:' + chart.id,
      this.statusDonutSegments(chart),
      HOME_OVERVIEW_STATUS_DONUT_INNER_RADIUS
    );
  }

  /** 键盘聚焦分类图例时同步突出扇区并显示相同的信息。 */
  focusDistributionItem(item: HomeOverviewDistributionItem): void {
    this.setHoveredDonutSlice('distribution:' + item.key, 'distribution');
  }

  /** 键盘聚焦状态图时显示该状态扇区的数据。 */
  focusStatusDonut(chart: HomeOverviewStatusChart): void {
    this.setHoveredDonutSlice('status:' + chart.id + ':value', 'status:' + chart.id);
  }

  clearHoveredDonutSlice(chartId?: string): void {
    if (chartId && this.hoveredDonutChartId !== chartId) {
      return;
    }
    if (this.hoveredDonutSliceId !== null || this.hoveredDonutChartId !== null) {
      this.hoveredDonutSliceId = null;
      this.hoveredDonutChartId = null;
      this.cd.markForCheck();
    }
  }

  /** 返回主分类环形图当前扇区的信息；比例按当前一级/二级统计范围计算。 */
  distributionDonutCenterInfo(): HomeOverviewDonutSegmentInfo | null {
    if (this.hoveredDonutChartId !== 'distribution' || !this.hoveredDonutSliceId) {
      return null;
    }
    const segment = this.distributionDonutSegments().find((item) => item.id === this.hoveredDonutSliceId);
    return buildDonutSegmentInfo(segment, this.activeDistributionTotal);
  }

  /** 返回指定状态环当前扇区的信息；灰色扇区使用对应的互补状态名称。 */
  statusDonutCenterInfo(chart: HomeOverviewStatusChart): HomeOverviewDonutSegmentInfo | null {
    const chartId = 'status:' + chart.id;
    if (this.hoveredDonutChartId !== chartId || !this.hoveredDonutSliceId) {
      return null;
    }
    const segment = this.statusDonutSegments(chart).find((item) => item.id === this.hoveredDonutSliceId);
    return buildDonutSegmentInfo(segment, this.devices.length);
  }

  /** 为屏幕阅读器提供分类图的当前扇区名称、设备数和比例。 */
  distributionDonutAriaLabel(): string {
    const info = this.distributionDonutCenterInfo();
    return info
      ? `${info.label}，${info.value}台，占当前统计范围${info.percentage.toFixed(1)}%`
      : '设备分类占比；具体分类名称和设备数见分类列表';
  }

  /** 为屏幕阅读器同时描述状态环的着色扇区和灰色互补扇区。 */
  statusDonutAriaLabel(chart: HomeOverviewStatusChart): string {
    const total = this.devices.length;
    if (!total) {
      return chart.label + '，暂无设备数据';
    }
    const currentPercentage = chart.value * 100 / total;
    const remainingCount = total - Math.min(chart.value, total);
    const remainingPercentage = remainingCount * 100 / total;
    return `${chart.label}：${chart.value}台，占${currentPercentage.toFixed(1)}%；` +
      `${chart.remainingLabel}：${remainingCount}台，占${remainingPercentage.toFixed(1)}%`;
  }

  private setHoveredDonutSlice(sliceId: string, chartId: string): void {
    if (this.hoveredDonutSliceId !== sliceId || this.hoveredDonutChartId !== chartId) {
      this.hoveredDonutSliceId = sliceId;
      this.hoveredDonutChartId = chartId;
      this.cd.markForCheck();
    }
  }

  private updateHoveredDonutSlice(
    event: MouseEvent,
    chartId: string,
    segments: HomeOverviewDonutSegment[],
    innerRadius: number
  ): void {
    const chart = event.currentTarget as SVGSVGElement;
    const bounds = chart.getBoundingClientRect();
    if (!bounds.width || !bounds.height) {
      this.clearHoveredDonutSlice(chartId);
      return;
    }

    const x = (event.clientX - bounds.left) * HOME_OVERVIEW_DONUT_VIEWBOX_SIZE / bounds.width;
    const y = (event.clientY - bounds.top) * HOME_OVERVIEW_DONUT_VIEWBOX_SIZE / bounds.height;
    const activeSliceId = this.hoveredDonutChartId === chartId ? this.hoveredDonutSliceId : undefined;
    const nextSliceId = findDonutSegmentAtPoint(
      x,
      y,
      segments,
      innerRadius,
      HOME_OVERVIEW_DONUT_OUTER_RADIUS,
      activeSliceId
    );
    if (this.hoveredDonutSliceId !== nextSliceId || this.hoveredDonutChartId !== (nextSliceId ? chartId : null)) {
      this.hoveredDonutSliceId = nextSliceId;
      this.hoveredDonutChartId = nextSliceId ? chartId : null;
      this.cd.markForCheck();
    }
  }

  formatCoordinate(value: number | undefined): string {
    return formatCoordinate(value);
  }

  /** 选择一级分类，并刷新二级统计和地图点位。 */
  selectCategory(category: string): void {
    // 再次点击当前一级标签时恢复三类默认视图。
    if (this.selectedCategory === category) {
      this.clearCategoryFilter();
      return;
    }
    this.clearHoveredDonutSlice();
    this.selectedCategory = category;
    this.selectedFunctionCategory = null;
    this.functionDistribution = buildFunctionDistribution(this.devices, category);
    this.refreshVisibleDevices();
  }

  /** 选择二级功能分类，进一步过滤地图设备。 */
  selectFunctionCategory(functionCategory: string): void {
    this.clearHoveredDonutSlice();
    this.selectedFunctionCategory = this.selectedFunctionCategory === functionCategory ? null : functionCategory;
    this.refreshVisibleDevices();
  }

  /** 清除一级、二级分类筛选，恢复全量设备地图。 */
  clearCategoryFilter(): void {
    this.clearHoveredDonutSlice();
    this.selectedCategory = null;
    this.selectedFunctionCategory = null;
    this.functionDistribution = [];
    this.refreshVisibleDevices();
  }

  private loadOverview(): void {
    const user = getCurrentAuthUser(this.store);
    const pageLink = new PageLink(MAX_SAFE_PAGE_SIZE, 0);
    const assets$ = user?.authority === Authority.CUSTOMER_USER && user.customerId
      ? this.assetService.getCustomerAssetInfos(user.customerId, pageLink)
      : this.assetService.getTenantAssetInfos(pageLink);
    const alarms$ = this.alarmService.getAllAlarmsV2(
      new AlarmQueryV2(null, new TimePageLink(MAX_SAFE_PAGE_SIZE, 0), {
        statusList: [AlarmSearchStatus.ACTIVE], severityList: Object.values(AlarmSeverity)
      })
    );

    // 三类数据独立重取；新通知到达时 switchMap 取消旧请求，避免旧快照覆盖新状态。
    this.deviceRefresh$.pipe(
      auditTime(150),
      switchMap(() => forkJoin({
        deviceContexts: this.homeOverviewApiService.getVisibleDeviceContexts().pipe(catchError(() => of(null))),
        // 复用按当前用户权限返回的全产品资产树，路径索引不参与首页分类筛选。
        assetTree: this.monitoringApi.getTree().pipe(catchError(() => of(null)))
      })),
      takeUntil(this.destroy$)
    ).subscribe(({deviceContexts, assetTree}) => {
      if (assetTree) {
        const tree = buildSmartLightningMonitoringAssetTreeFromRoots(assetTree.roots);
        this.deviceAssetPaths = getSmartLightningMonitoringAssetPaths(tree);
      }
      if (deviceContexts) {
        this.loadDeviceAttributes(deviceContexts);
      } else {
        this.loading = false;
        this.pendingActiveDeviceIds.clear();
        this.cd.markForCheck();
      }
    });

    this.assetRefresh$.pipe(
      auditTime(150),
      switchMap(() => assets$.pipe(catchError(() => of(null)))),
      takeUntil(this.destroy$)
    ).subscribe((assets) => {
      if (assets) {
        this.stats.assetTotal = assets.totalElements || 0;
        this.cd.markForCheck();
      }
    });

    this.alarmRefresh$.pipe(
      auditTime(150),
      switchMap(() => alarms$.pipe(catchError(() => of(null)))),
      takeUntil(this.destroy$)
    ).subscribe((alarms) => {
      if (!alarms) {
        return;
      }
      const alarmNames = new Map<string, string[]>();
      for (const alarm of alarms.data || []) {
        if (alarm.originator?.entityType !== EntityType.DEVICE) {
          continue;
        }
        const names = alarmNames.get(alarm.originator.id) || [];
        if (alarm.type && !names.includes(alarm.type)) {
          names.push(alarm.type);
        }
        alarmNames.set(alarm.originator.id, names);
      }
      this.alarmNamesByDevice = alarmNames;
      this.devices = this.devices.map((device) => ({
        ...device,
        alarmNames: alarmNames.get(device.id) || []
      }));
      this.stats.alarmDeviceTotal = this.devices.filter((device) => device.alarmNames.length > 0).length;
      this.refreshVisibleDevices();
    });

    this.deviceRefresh$.next();
    this.assetRefresh$.next();
    this.alarmRefresh$.next();
  }

  /** 按实时事件的业务含义分流：首页实时状态本地更新，权限敏感的集合/上下文仍调用原接口。 */
  private subscribeToRealtimeChanges(): void {
    // active 是首页已加载设备的实时状态；只处理当前页面范围内的设备，不为每次遥测重查整组上下文。
    this.realtimeService.deviceTelemetryChanged$.pipe(takeUntil(this.destroy$))
      .subscribe((change) => {
        if (change.values.has('active')) {
          this.pendingActiveDeviceIds.add(change.deviceId);
          this.activeTelemetryRefresh$.next();
        }
        if (change.values.has('Latitude') || change.values.has('Longitude')) {
          this.pendingMapCoordinateDeviceIds.add(change.deviceId);
          this.activeTelemetryRefresh$.next();
        }
      });

    this.activeTelemetryRefresh$.pipe(auditTime(100), takeUntil(this.destroy$))
      .subscribe(() => this.applyPendingActiveTelemetry());

    // 属性、设备名称或归属等上下文变化仍由原接口重取；初始快照已被页面首次加载覆盖，不重复请求。
    this.realtimeService.deviceContextIncrementalChanged$.pipe(auditTime(300), takeUntil(this.destroy$))
      .subscribe(() => this.deviceRefresh$.next());

    // 设备集合变化影响首页设备总量和地图范围；资产集合变化只影响资产总量。
    this.realtimeService.deviceMembershipChanged$.pipe(auditTime(300), takeUntil(this.destroy$))
      .subscribe(() => this.deviceRefresh$.next());

    this.realtimeService.assetMembershipChanged$.pipe(auditTime(300), takeUntil(this.destroy$))
      .subscribe(() => {
        this.assetRefresh$.next();
        // 资产集合变化可能影响完整路径，因此同步刷新可见资产树。
        this.deviceRefresh$.next();
      });

    // 数量变化后的补刷使用 switchMap 合并短时间内的连续计数事件，只保留最后一次 800ms 校准。
    this.alarmSettleRefresh$.pipe(
      switchMap(() => timer(800)),
      takeUntil(this.destroy$)
    ).subscribe(() => this.alarmRefresh$.next());

    this.realtimeService.alarmChanged$.pipe(takeUntil(this.destroy$)).subscribe((change) => {
      this.alarmRefresh$.next();
      if (change.source === 'count') {
        // 新增告警的数量推送可能先于明细提交；再读取一次避免首页停留在旧数量。
        this.alarmSettleRefresh$.next();
      }
    });
  }

  /**
   * 合并 active 和坐标推送，并从共享缓存读取最新值，避免使用乱序到达的旧帧。
   * 只应用首屏 HTTP 已确认可见的设备；权限范围、设备分类和设备总数仍以原接口为准。
   */
  private applyPendingActiveTelemetry(): void {
    if (!this.pendingActiveDeviceIds.size && !this.pendingMapCoordinateDeviceIds.size) {
      return;
    }

    const loadedDeviceIds = new Set(this.devices.map((device) => device.id));
    const activeByDeviceId = new Map<string, boolean>();
    for (const deviceId of Array.from(this.pendingActiveDeviceIds)) {
      if (!loadedDeviceIds.has(deviceId)) {
        // 首次设备上下文仍在加载时暂存事件，待完整权限范围返回后再决定是否应用。
        if (!this.loading) {
          this.pendingActiveDeviceIds.delete(deviceId);
        }
        continue;
      }

      const latest = this.realtimeService.getLatestTelemetryValues(deviceId).get('active');
      const active = latest ? parseHomeOverviewActiveValue(latest.value) : undefined;
      if (active !== undefined) {
        activeByDeviceId.set(deviceId, active);
      }
      this.pendingActiveDeviceIds.delete(deviceId);
    }

    let mapCoordinatesChanged = false;
    for (const deviceId of Array.from(this.pendingMapCoordinateDeviceIds)) {
      if (!loadedDeviceIds.has(deviceId)) {
        // 设备上下文仍在加载时保留事件，等 HTTP 响应确认当前用户的可见范围。
        if (!this.loading) {
          this.pendingMapCoordinateDeviceIds.delete(deviceId);
        }
        continue;
      }
      mapCoordinatesChanged = true;
      this.pendingMapCoordinateDeviceIds.delete(deviceId);
    }

    const updatedDevices = applyHomeOverviewActiveUpdates(this.devices, activeByDeviceId);
    const activeStateChanged = updatedDevices !== this.devices;
    if (!activeStateChanged && !mapCoordinatesChanged) {
      return;
    }

    if (activeStateChanged) {
      this.devices = updatedDevices;
      this.stats.onlineTotal = this.devices.filter((device) => device.active).length;
      this.stats.offlineTotal = this.devices.length - this.stats.onlineTotal;
    }
    this.refreshVisibleDevices();
  }

  /** 将后端已经完成权限和属性聚合的上下文转换为首页地图模型。 */
  private loadDeviceAttributes(deviceContexts: HomeOverviewDeviceContext[]): void {
    this.stats.deviceTotal = deviceContexts.length;
    this.devices = deviceContexts.map((device) => ({
      id: device.deviceId,
      name: device.deviceName,
      productName: device.productName || '未配置产品名称',
      profileName: device.deviceProfileName || '未分类设备',
      productCategory: device.productCategory,
      functionCategory: device.functionCategory,
      active: !!device.active,
      latitude: normalizeCoordinate(device.latitude),
      longitude: normalizeCoordinate(device.longitude),
      assetPath: '--',
      alarmNames: this.alarmNamesByDevice.get(device.deviceId) || []
    }));
    this.stats.onlineTotal = this.devices.filter((item) => item.active).length;
    this.stats.offlineTotal = this.devices.length - this.stats.onlineTotal;
    this.stats.alarmDeviceTotal = this.devices.filter((item) => item.alarmNames.length > 0).length;
    this.categoryDistribution = buildCategoryDistribution(this.devices);
    this.functionDistribution = this.selectedCategory
      ? buildFunctionDistribution(this.devices, this.selectedCategory) : [];
    if (this.selectedFunctionCategory &&
        !this.functionDistribution.some((item) => item.key === this.selectedFunctionCategory)) {
      this.selectedFunctionCategory = null;
    }
    this.refreshVisibleDevices();
    this.loading = false;
    this.applyPendingActiveTelemetry();
    this.cd.markForCheck();
  }
  /** 根据当前分类筛选刷新地图输入，顶部全局统计不受影响。 */
  private refreshVisibleDevices(): void {
    const visibleDevices = this.devices.filter((device) => {
      const categoryMatched = !this.selectedCategory || normalizeCategory(device.productCategory) === this.selectedCategory;
      const functionMatched = !this.selectedFunctionCategory
        || normalizeHomeOverviewFunctionCategory(device.productCategory, device.functionCategory) === this.selectedFunctionCategory;
      return categoryMatched && functionMatched;
    });
    this.visibleDevices = visibleDevices.map((device) => {
      const coordinates = resolveSmartLightningMapCoordinates(
        device.latitude,
        device.longitude,
        this.realtimeService.getLatestTelemetryValue(device.id, 'Latitude')?.value,
        this.realtimeService.getLatestTelemetryValue(device.id, 'Longitude')?.value
      );
      return {
        ...device,
        latitude: coordinates?.latitude,
        longitude: coordinates?.longitude
      };
    });
    // “未配置坐标”按实际地图点位口径统计，已有 GPS 遥测坐标的设备不再计入。
    this.stats.unconfiguredTotal = this.devices.filter((device) => resolveSmartLightningMapCoordinates(
      device.latitude,
      device.longitude,
      this.realtimeService.getLatestTelemetryValue(device.id, 'Latitude')?.value,
      this.realtimeService.getLatestTelemetryValue(device.id, 'Longitude')?.value
    ) === null).length;
    this.cd.markForCheck();
  }
}
