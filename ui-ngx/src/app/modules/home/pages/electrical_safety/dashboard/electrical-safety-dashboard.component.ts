/** 文件职责：加载电气安全设备和告警数据，并驱动看板各区域展示。 */
import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {auditTime, bufferTime, filter, merge, Subject, Subscription, timer} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import L from 'leaflet';
import {AttributeService} from '@core/http/attribute.service';
import {EntityType} from '@shared/models/entity-type.models';
import {AttributeScope} from '@shared/models/telemetry/telemetry.models';
import {SmartLightningPeriod} from '../../smart-lightning/dashboard/smart-lightning-dashboard.data';
import type {
  SmartLightningMapDevice,
  SmartLightningMapDraftPoint
} from '../../smart-lightning/shared/map/smart-lightning-device-map.models';
import {SmartLightningDeviceMapComponent} from '../../smart-lightning/shared/map/smart-lightning-device-map.component';
import type {DeviceAlarmDisplayColumn, DeviceAlarmDisplayRow} from '@home/pages/deviceAlarms/shared/alarm-list/device-alarm-records.model';
import {
  ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
  hasElectricalSafetyDashboardMetricUpdate,
  parseElectricalSafetyActiveValue,
  formatElectricalSafetyActivityTime as formatElectricalSafetyTimestamp,
  getElectricalSafetyRealtimeMetricDefinitions
} from '../monitoring/electrical-safety-monitoring.data';
import {ElectricalSafetyApiService} from '../electrical-safety-api.service';
import {
  filterSmartLightningMapDevicesWithoutPoints
} from '../../smart-lightning/shared/map/smart-lightning-device-map.data';
import type {
  ElectricalSafetyAssetTree,
  ElectricalSafetyDeviceContext
} from '../electrical-safety-api.service';
import {
  buildElectricalSafetyAlarmChart,
  buildElectricalSafetyDashboardCards,
  buildElectricalSafetyDeviceOverview,
  buildElectricalSafetyFireRiskMeasurements,
  formatElectricalSafetyFireRiskMetricTooltip,
  buildElectricalSafetyMapDevices,
  resolveElectricalSafetyMapCoordinates,
  ELECTRICAL_SAFETY_ALARM_COLUMNS,
  EMPTY_ELECTRICAL_SAFETY_DEVICE_OVERVIEW,
  hasElectricalSafetyFireRiskMetricUpdate,
  isElectricalSafetyFireFunctionCategory,
  mapElectricalSafetyAlarms,
  sumElectricalSafetyDashboardAlarmEvents
} from './electrical-safety-dashboard.data';
import type {
  ElectricalSafetyAlarmChartItem,
  ElectricalSafetyDashboardCategory,
  ElectricalSafetyDashboardCard,
  ElectricalSafetyDeviceOverview,
  ElectricalSafetyFireRiskMetric
} from './electrical-safety-dashboard.data';
import type {AlarmInfo} from '@shared/models/alarm.models';
import type {PageData} from '@shared/models/page/page-data';
import {
  DeviceTelemetryChange,
  ElectricalSafetyRealtimeService
} from '../shared/electrical-safety-realtime.service';

/** 电气安全看板的设备地图、设备统计和告警列表均使用真实模块接口数据。 */
@Component({
  selector: 'tb-electrical-safety-dashboard',
  templateUrl: './electrical-safety-dashboard.component.html',
  styleUrls: ['./electrical-safety-dashboard.component.scss'],
  standalone: false
})
export class ElectricalSafetyDashboardComponent implements OnInit, OnDestroy {
  /** 用户可见的电气安全设备；null 表示接口尚未成功返回。 */
  private deviceContexts: ElectricalSafetyDeviceContext[] | null = null;
  /** 用户可见且裁剪到电气安全设备的资产树；null 表示接口尚未成功返回。 */
  private assetTree: ElectricalSafetyAssetTree | null = null;
  /** 最近告警接口原始分页，等待设备和资产路径加载后再转换成表格行。 */
  private alarmPage: PageData<AlarmInfo> | null = null;
  /** 当前活动告警设备 ID；null 代表未知，空集合代表接口确认没有活动告警。 */
  private activeAlarmDeviceIds: Set<string> | null = null;
  /** 追踪设备和资产树两个独立请求是否完成，以区分地图空数据和加载中。 */
  private deviceContextsLoaded = false;
  private assetTreeLoaded = false;
  /** 每个周期单独追踪请求版本，避免旧响应覆盖同一周期较新的数据。 */
  private readonly alarmStatisticsRequestIds = new Map<SmartLightningPeriod, number>();
  /** 同一周期的普通切换请求只保留一份，周期刷新才允许替换正在执行的请求。 */
  private readonly alarmStatisticsPeriodsLoading = new Set<SmartLightningPeriod>();
  /** 缓存各自然周期的功能分类计数，供不同卡片独立选择周期并复用结果。 */
  private readonly alarmEventCountsByPeriod = new Map<SmartLightningPeriod, ReadonlyMap<string, number>>();
  /** 当前已应用的活动告警快照请求序号，防止较早周期响应覆盖较新快照。 */
  private latestAppliedActiveAlarmSnapshotRequestId = 0;
  /** 跨周期请求统一使用递增序号，保护活动告警设备集合的更新时间顺序。 */
  private alarmStatisticsRequestSequence = 0;
  /** 独立设备、资产树和告警列表请求只允许最新响应覆盖当前页面。 */
  private deviceContextsRequestId = 0;
  private assetTreeRequestId = 0;
  private alarmInformationRequestId = 0;
  private latestAlarmRequestId = 0;
  private readonly destroy$ = new Subject<void>();
  /** 告警数量事件可能早于数据库事务提交；记录初始数量并延迟复读新增告警。 */
  private realtimeAlarmCount: number | null = null;
  /** WebSocket 告警数量首帧只建立基线，不视为页面进入后的告警变化。 */
  private hasRealtimeAlarmCountBaseline = false;
  /** 实体 WebSocket 完整首帧到达前，合并暂存上下文或集合变化事件。 */
  private realtimeSnapshotReady = false;
  private pendingSnapshotReload = false;
  private initialAlarmLoadFinished = false;
  private alarmSettleRefreshSubscription?: Subscription;

  /** 设备总览值在设备上下文或告警设备范围未到达前显示未知占位。 */
  deviceOverview: ElectricalSafetyDeviceOverview = {...EMPTY_ELECTRICAL_SAFETY_DEVICE_OVERVIEW};
  /** 设备上下文初次加载期间的辅助状态。 */
  deviceOverviewLoading = true;
  /** 设备资料请求失败时显示错误而不是把失败呈现为零设备。 */
  deviceOverviewLoadError = false;
  /** 当前真实设备坐标生成的地图点位，没有坐标的设备不会被绘制。 */
  mapDevices: SmartLightningMapDevice[] = [];
  /** 用户手动选点模式只绑定当前用户可见且尚无有效坐标的电气安全设备。 */
  isAddingMapPoint = false;
  /** 用户点击地图后待绑定的坐标草稿；确认保存前不会写入设备属性。 */
  pendingMapPoint: SmartLightningMapDraftPoint | null = null;
  /** 当前绑定面板中选中的设备 ID。 */
  selectedMapDeviceId = '';
  /** 坐标保存中的状态用于阻止重复提交。 */
  mapPointSaving = false;
  /** 坐标写入失败时在绑定面板内展示原因。 */
  mapPointSaveError = '';
  /** 设备上下文首次加载完成前显示地图加载状态。 */
  mapLoading = true;
  /** 设备上下文请求失败时显示地图错误状态；资产树只负责补全点位详情路径。 */
  mapLoadError = false;
  /** 当前设备上下文和活动告警设备集合生成的四类真实设备统计。 */
  monitoringCards: ElectricalSafetyDashboardCard[] = buildElectricalSafetyDashboardCards(null, null);
  /** 火灾卡片中按型号已配置遥测聚合的实时风险指标；未知测点保持 --。 */
  fireRiskMeasurements = buildElectricalSafetyFireRiskMeasurements(null);
  /** 按原始告警名称统计的当前活动告警；名称来自告警记录的 type 字段。 */
  alarmInformationItems: ElectricalSafetyAlarmChartItem[] = [];
  /** 当前活动告警名称请求状态，与周期趋势统计请求相互独立。 */
  alarmInformationLoading = true;
  /** 活动告警名称查询失败时由共享图表显示错误状态。 */
  alarmInformationLoadError = false;
  /** 四张分类卡各自维护日/周/月选择；周期只控制本卡的告警次数。 */
  alarmPeriods: Record<ElectricalSafetyDashboardCategory, SmartLightningPeriod> = {
    fire: SmartLightningPeriod.day,
    'safe-electricity': SmartLightningPeriod.day,
    'power-quality': SmartLightningPeriod.day,
    'arc-fault': SmartLightningPeriod.day
  };
  /** 每张卡的周期统计加载状态独立，失败时不把未知值伪装成零。 */
  alarmStatisticsLoading: Record<ElectricalSafetyDashboardCategory, boolean> = {
    fire: true,
    'safe-electricity': true,
    'power-quality': true,
    'arc-fault': true
  };
  alarmStatisticsLoadError: Record<ElectricalSafetyDashboardCategory, boolean> = {
    fire: false,
    'safe-electricity': false,
    'power-quality': false,
    'arc-fault': false
  };
  /** 最近告警共享表格的真实告警行。 */
  alarmRecords: DeviceAlarmDisplayRow[] = [];
  /** 最近告警记录表沿用电气安全看板的列顺序和宽度。 */
  readonly alarmRecordColumns: readonly DeviceAlarmDisplayColumn[] = ELECTRICAL_SAFETY_ALARM_COLUMNS;
  /** 最近告警分页请求是否正在执行。 */
  alarmsLoading = true;
  /** 最近告警查询失败状态，由共享记录表提供错误提示和重试入口。 */
  alarmsLoadError = false;
  /** 防雷地图绘制组件保持通用，本页面只允许显示电气安全设备。 */
  readonly includeElectricalSafetyDevice = (device: SmartLightningMapDevice): boolean =>
    device.productCategory?.trim().toLowerCase() === ELECTRICAL_SAFETY_PRODUCT_CATEGORY;

  /** 防雷共享地图实例负责渲染；本页面仅控制选点流程和坐标持久化。 */
  @ViewChild('deviceMap') deviceMap?: SmartLightningDeviceMapComponent;

  /** 可供地图手动绑定的设备：已具有 GPS 或服务器坐标的设备均不重复出现。 */
  get availableMapDevices(): ElectricalSafetyDeviceContext[] {
    const candidates = (this.deviceContexts || []).filter((context) =>
      context.productCategory?.trim().toLowerCase() === ELECTRICAL_SAFETY_PRODUCT_CATEGORY);
    const boundDeviceIds = new Set(candidates
      .filter((context) => this.resolveDeviceMapCoordinates(context) !== null)
      .map((context) => context.deviceId));
    return filterSmartLightningMapDevicesWithoutPoints(candidates, boundDeviceIds);
  }

  /** 地图空状态文案区分无设备和设备尚未配置有效坐标两种情况。 */
  get mapEmptyMessage(): string {
    if (!this.deviceContexts?.length) {
      return '当前没有电气安全设备';
    }
    if (this.availableMapDevices.length) {
      return '设备已加载，但尚无有效坐标；可点击“添加点位”为设备绑定位置';
    }
    return '当前没有可展示的电气安全设备点位';
  }

  /** 当前地图接口返回的电气安全设备数量，供标题显示定位覆盖情况。 */
  get mapDeviceContextCount(): number {
    return this.deviceContexts?.length ?? 0;
  }

  constructor(
    private readonly cd: ChangeDetectorRef,
    private readonly attributeService: AttributeService,
    private readonly electricalSafetyApiService: ElectricalSafetyApiService,
    private readonly realtimeService: ElectricalSafetyRealtimeService
  ) {}

  /** 并行加载设备、资产树、周期统计、当前活动告警名称及最近告警记录。 */
  ngOnInit(): void {
    this.subscribeToRealtimeChanges();
    this.loadDeviceContexts();
    this.loadAssetTree();
    this.loadDashboardAlarmStatisticsForSelectedPeriods();
    this.loadAlarmInformation();
    this.loadLatestAlarms();
  }

  /** 页面销毁时终止仍在进行的 HTTP 和周期刷新订阅。 */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.alarmSettleRefreshSubscription?.unsubscribe();
    this.realtimeService.release();
  }

  /** 切换地图选点模式；重新点击按钮可退出，不写入任何设备数据。 */
  toggleMapPointMode(): void {
    if (!this.deviceMap || !this.availableMapDevices.length) {
      return;
    }
    if (this.isAddingMapPoint) {
      this.isAddingMapPoint = false;
      this.cd.markForCheck();
      return;
    }
    this.isAddingMapPoint = true;
    this.cd.markForCheck();
  }

  /** 地图选点模式下接收 Leaflet 坐标，随后由用户选择设备确认写入。 */
  onMapClick(point: L.LatLng): void {
    this.isAddingMapPoint = false;
    this.pendingMapPoint = {latitude: point.lat, longitude: point.lng};
    this.selectedMapDeviceId = '';
    this.mapPointSaveError = '';
    this.cd.markForCheck();
  }

  /** 记录绑定面板中当前选择的电气安全设备。 */
  selectMapDevice(event: Event): void {
    this.selectedMapDeviceId = (event.target as HTMLSelectElement).value;
    this.mapPointSaveError = '';
  }

  /** 把手动选定点位保存到设备 SERVER_SCOPE，作为 GPS 遥测缺失时的回退位置。 */
  saveMapPointBinding(): void {
    if (!this.pendingMapPoint || !this.selectedMapDeviceId || this.mapPointSaving) {
      return;
    }
    const selectedDevice = this.deviceContexts?.find((device) => device.deviceId === this.selectedMapDeviceId);
    // 提交前重新检查实时缓存，避免 GPS 在打开绑定面板后到达并被手动坐标覆盖。
    if (!selectedDevice || this.resolveDeviceMapCoordinates(selectedDevice) !== null) {
      this.mapPointSaveError = '该设备已具有有效地图坐标，请重新选择其他设备。';
      this.selectedMapDeviceId = '';
      this.cd.markForCheck();
      return;
    }

    const deviceId = selectedDevice.deviceId;
    const point = this.pendingMapPoint;
    this.mapPointSaving = true;
    this.mapPointSaveError = '';
    this.attributeService.saveEntityAttributes(
      {entityType: EntityType.DEVICE, id: deviceId},
      AttributeScope.SERVER_SCOPE,
      [
        {key: 'latitude', value: point.latitude},
        {key: 'longitude', value: point.longitude}
      ]
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        if (this.deviceContexts) {
          this.deviceContexts = this.deviceContexts.map((device) => device.deviceId === deviceId
            ? {...device, latitude: point.latitude, longitude: point.longitude}
            : device);
        }
        this.refreshMapDevices();
        this.pendingMapPoint = null;
        this.selectedMapDeviceId = '';
        this.mapPointSaving = false;
        this.cd.detectChanges();
        this.deviceMap?.openDevicePopup(deviceId);
      },
      error: () => {
        this.mapPointSaving = false;
        this.mapPointSaveError = '设备坐标保存失败，请检查当前账号的设备属性写入权限。';
        this.cd.markForCheck();
      }
    });
  }

  /** 取消坐标草稿并关闭绑定面板，不修改设备已有属性。 */
  cancelMapPointBinding(): void {
    this.pendingMapPoint = null;
    this.selectedMapDeviceId = '';
    this.mapPointSaveError = '';
    this.cd.markForCheck();
  }

  /** 使用稳定类别键追踪看板卡片，避免接口刷新时重复创建 DOM。 */
  trackByCategory(_index: number, card: ElectricalSafetyDashboardCard): string {
    return card.key;
  }

  /** 统一使用电气安全监测页的本地时间格式展示电弧最近上报时间。 */
  formatElectricalSafetyActivityTime(timestamp: number | null): string {
    return formatElectricalSafetyTimestamp(timestamp);
  }

  /** 生成原生 title 提示，展开显示指标值及完整来源信息。 */
  formatFireRiskMetricTooltip(label: string, metric: ElectricalSafetyFireRiskMetric): string {
    return formatElectricalSafetyFireRiskMetricTooltip(label, metric);
  }

  /** 将单张卡片所选的告警周期转换为本地化标题。 */
  getAlarmPeriodLabel(category: ElectricalSafetyDashboardCategory): string {
    const period = this.alarmPeriods[category];
    if (period === SmartLightningPeriod.week) {
      return '本周';
    }
    if (period === SmartLightningPeriod.month) {
      return '本月';
    }
    return '今日';
  }

  /** 独立切换指定类别的告警统计周期，其他卡片及实时测点不受影响。 */
  selectAlarmPeriod(category: ElectricalSafetyDashboardCategory, period: SmartLightningPeriod): void {
    if (period === this.alarmPeriods[category]) {
      return;
    }
    this.alarmPeriods = {...this.alarmPeriods, [category]: period};
    this.alarmStatisticsLoadError[category] = false;
    this.alarmStatisticsLoading[category] = !this.alarmEventCountsByPeriod.has(period);
    this.refreshDeviceDerivedData();
    if (this.alarmEventCountsByPeriod.has(period)) {
      this.alarmStatisticsLoading[category] = false;
      return;
    }
    this.loadDashboardAlarmStatistics(period);
  }

  /** 请求失败时重新加载最近告警列表。 */
  retryLatestAlarms(): void {
    this.loadLatestAlarms();
  }

  /** 从电气安全模块接口读取已按当前用户权限过滤的设备上下文。 */
  private loadDeviceContexts(): void {
    const requestId = ++this.deviceContextsRequestId;
    this.electricalSafetyApiService.getDeviceContexts().pipe(takeUntil(this.destroy$)).subscribe({
      next: (contexts) => {
        if (requestId !== this.deviceContextsRequestId) {
          return;
        }
        this.deviceContexts = contexts || [];
        this.deviceContextsLoaded = true;
        this.deviceOverviewLoading = false;
        this.deviceOverviewLoadError = false;
        this.applyLatestTelemetrySnapshot();
        this.refreshDeviceDerivedData();
        this.refreshAlarmRecords();
      },
      error: () => {
        if (requestId !== this.deviceContextsRequestId) {
          return;
        }
        this.deviceContexts = null;
        this.deviceContextsLoaded = true;
        this.deviceOverviewLoading = false;
        this.deviceOverviewLoadError = true;
        this.refreshDeviceDerivedData();
        this.refreshAlarmRecords();
      }
    });
  }

  /** 从电气安全模块资产树读取权限裁剪后的设备绝对路径。 */
  private loadAssetTree(): void {
    const requestId = ++this.assetTreeRequestId;
    this.electricalSafetyApiService.getAssetTree().pipe(takeUntil(this.destroy$)).subscribe({
      next: (tree) => {
        if (requestId !== this.assetTreeRequestId) {
          return;
        }
        this.assetTree = tree || {roots: []};
        this.assetTreeLoaded = true;
        this.refreshDeviceDerivedData();
        this.refreshAlarmRecords();
      },
      error: () => {
        if (requestId !== this.assetTreeRequestId) {
          return;
        }
        this.assetTree = null;
        this.assetTreeLoaded = true;
        this.refreshDeviceDerivedData();
        this.refreshAlarmRecords();
      }
    });
  }

  /** 为当前卡片所选的每个不同周期各请求一次，并复用响应中的全部功能分类计数。 */
  private loadDashboardAlarmStatisticsForSelectedPeriods(refresh = false): void {
    const periods = new Set<SmartLightningPeriod>(Object.values(this.alarmPeriods));
    for (const period of periods) {
      this.loadDashboardAlarmStatistics(period, refresh);
    }
  }

  /** 根据自然周期读取活动告警设备范围和该周期的分类事件数。 */
  private loadDashboardAlarmStatistics(period: SmartLightningPeriod, refresh = false): void {
    if (this.alarmEventCountsByPeriod.has(period) && !refresh) {
      this.setAlarmStatisticsState(period, false, false);
      return;
    }
    if (this.alarmStatisticsPeriodsLoading.has(period) && !refresh) {
      this.setAlarmStatisticsState(period, true, false);
      return;
    }
    const requestId = ++this.alarmStatisticsRequestSequence;
    this.alarmStatisticsRequestIds.set(period, requestId);
    this.alarmStatisticsPeriodsLoading.add(period);
    this.setAlarmStatisticsState(period, true, false);
    this.electricalSafetyApiService.getDashboardAlarmStatistics(
      ELECTRICAL_SAFETY_PRODUCT_CATEGORY, period).pipe(takeUntil(this.destroy$)).subscribe({
      next: (statistics) => {
        if (requestId !== this.alarmStatisticsRequestIds.get(period)) {
          return;
        }
        this.alarmStatisticsPeriodsLoading.delete(period);
        // 活动设备集合不随周期变化；仅让最新发起的成功响应更新这份实时快照。
        if (requestId > this.latestAppliedActiveAlarmSnapshotRequestId) {
          this.activeAlarmDeviceIds = new Set(statistics.activeAlarmDeviceIds || []);
          this.latestAppliedActiveAlarmSnapshotRequestId = requestId;
        }
        this.alarmEventCountsByPeriod.set(period, new Map(
          (statistics.functionCategoryCounts || []).map((count) =>
            [count.functionCategory, count.alarmCount] as const)
        ));
        this.setAlarmStatisticsState(period, false, false);
        this.refreshDeviceDerivedData();
      },
      error: () => {
        if (requestId !== this.alarmStatisticsRequestIds.get(period)) {
          return;
        }
        this.alarmStatisticsPeriodsLoading.delete(period);
        // 统计重读失败时保留其他周期和活动设备快照，仅标记仍选择此周期的卡片出错。
        this.setAlarmStatisticsState(period, false, true);
        this.refreshDeviceDerivedData();
      }
    });
  }

  /** 只更新当前仍选中指定周期的卡片，切换走的卡片不受旧请求反馈影响。 */
  private setAlarmStatisticsState(
    period: SmartLightningPeriod,
    loading: boolean,
    loadError: boolean
  ): void {
    for (const category of Object.keys(this.alarmPeriods) as ElectricalSafetyDashboardCategory[]) {
      if (this.alarmPeriods[category] === period) {
        this.alarmStatisticsLoading[category] = loading;
        this.alarmStatisticsLoadError[category] = loadError;
      }
    }
  }

  /** 请求本模块现有的活动告警分页接口，并按真实告警名称生成共享图表数据。 */
  private loadAlarmInformation(): void {
    const requestId = ++this.alarmInformationRequestId;
    const hasPreviousResult = this.initialAlarmLoadFinished && !this.alarmInformationLoadError;
    this.alarmInformationLoading = !hasPreviousResult;
    this.alarmInformationLoadError = false;
    this.electricalSafetyApiService.getAllActiveAlarms(
      ELECTRICAL_SAFETY_PRODUCT_CATEGORY).pipe(takeUntil(this.destroy$)).subscribe({
      next: (alarms) => {
        if (requestId !== this.alarmInformationRequestId) {
          return;
        }
        this.alarmInformationItems = buildElectricalSafetyAlarmChart(alarms);
        this.realtimeAlarmCount = alarms.length;
        this.initialAlarmLoadFinished = true;
        this.alarmInformationLoading = false;
      },
      error: () => {
        if (requestId !== this.alarmInformationRequestId) {
          return;
        }
        this.initialAlarmLoadFinished = true;
        this.alarmInformationLoading = false;
        // 后台刷新失败时保留上次成功结果，只有没有旧结果时才显示加载错误。
        this.alarmInformationLoadError = !hasPreviousResult;
      }
    });
  }

  /** 查询最近告警，分类过滤由电气安全模块后端在分页前执行。 */
  private loadLatestAlarms(): void {
    const requestId = ++this.latestAlarmRequestId;
    const hasPreviousPage = this.alarmPage !== null;
    this.alarmsLoading = !hasPreviousPage;
    this.alarmsLoadError = false;
    this.electricalSafetyApiService.getAlarms(
      ELECTRICAL_SAFETY_PRODUCT_CATEGORY, 8, 0).pipe(takeUntil(this.destroy$)).subscribe({
      next: (alarmPage) => {
        if (requestId !== this.latestAlarmRequestId) {
          return;
        }
        this.alarmPage = alarmPage;
        this.alarmsLoading = false;
        this.alarmsLoadError = false;
        this.refreshAlarmRecords();
      },
      error: () => {
        if (requestId !== this.latestAlarmRequestId) {
          return;
        }
        this.alarmsLoading = false;
        // 后台复读失败时继续展示已有行，只有首屏失败时才显示错误状态。
        this.alarmsLoadError = !hasPreviousPage;
      }
    });
  }

  /** 设备上下文和告警响应到达后刷新总览、类别卡片和地图标记。 */
  private refreshDeviceDerivedData(): void {
    this.deviceOverview = buildElectricalSafetyDeviceOverview(
      this.deviceContexts, this.activeAlarmDeviceIds);
    const alarmEventCounts = new Map<ElectricalSafetyDashboardCategory, number | null>();
    for (const category of Object.keys(this.alarmPeriods) as ElectricalSafetyDashboardCategory[]) {
      const functionCategoryCounts = this.alarmEventCountsByPeriod.get(this.alarmPeriods[category]) || null;
      alarmEventCounts.set(category, sumElectricalSafetyDashboardAlarmEvents(functionCategoryCounts, category));
    }
    this.monitoringCards = buildElectricalSafetyDashboardCards(
      this.deviceContexts, this.activeAlarmDeviceIds, alarmEventCounts, this.getDashboardTelemetrySnapshot());
    this.refreshFireRiskMeasurements();
    this.refreshMapDevices();
  }

  /** 使用已加载的电气安全上下文和共享遥测缓存重建有效地图点位。 */
  private refreshMapDevices(): void {
    if (!this.deviceContextsLoaded) {
      return;
    }
    this.mapLoading = false;
    // 资产树只补全弹窗路径；资产树查询失败不能隐藏已经有坐标的设备点位。
    this.mapLoadError = this.deviceContexts === null;
    if (this.mapLoadError || this.deviceContexts === null) {
      this.mapDevices = [];
      return;
    }
    const contextsWithResolvedCoordinates = this.deviceContexts.map((context) => {
      const coordinates = resolveElectricalSafetyMapCoordinates(
        context.latitude,
        context.longitude,
        this.realtimeService.getLatestTelemetryValue(context.deviceId, 'Latitude')?.value,
        this.realtimeService.getLatestTelemetryValue(context.deviceId, 'Longitude')?.value
      );
      return {
        ...context,
        latitude: coordinates?.latitude ?? null,
        longitude: coordinates?.longitude ?? null
      };
    });
    this.mapDevices = buildElectricalSafetyMapDevices(
      contextsWithResolvedCoordinates, this.assetTree?.roots || [], this.activeAlarmDeviceIds);
  }

  /** 从现有共享 WebSocket 缓存挑选看板所需最新值，不为各类别重复建立订阅。 */
  private getDashboardTelemetrySnapshot(): ReadonlyMap<
    string,
    ReadonlyMap<string, {ts: number; value: string}>
  > {
    const telemetryByDeviceId = new Map<string, ReadonlyMap<string, {ts: number; value: string}>>();
    for (const context of this.deviceContexts || []) {
      if (context.productCategory !== ELECTRICAL_SAFETY_PRODUCT_CATEGORY) {
        continue;
      }
      const definitions = getElectricalSafetyRealtimeMetricDefinitions(context.deviceProfileName);
      if (context.functionCategory?.trim() === 'arc_fault') {
        telemetryByDeviceId.set(context.deviceId, this.realtimeService.getLatestTelemetryValues(context.deviceId));
        continue;
      }
      const dashboardDefinitions = definitions.filter((definition) => definition.dashboardMetric);
      if (dashboardDefinitions.length === 0) {
        continue;
      }
      const latestValues = this.realtimeService.getLatestTelemetryValues(context.deviceId);
      const selectedValues = new Map<string, {ts: number; value: string}>();
      for (const definition of dashboardDefinitions) {
        const sample = latestValues.get(definition.key);
        if (sample) {
          selectedValues.set(definition.key, sample);
        }
      }
      telemetryByDeviceId.set(context.deviceId, selectedValues);
    }
    return telemetryByDeviceId;
  }

  /** GPS 遥测完整有效时优先使用，否则回退到 SERVER_SCOPE 经纬度属性。 */
  private resolveDeviceMapCoordinates(device: ElectricalSafetyDeviceContext) {
    return resolveElectricalSafetyMapCoordinates(
      device.latitude,
      device.longitude,
      this.realtimeService.getLatestTelemetryValue(device.deviceId, 'Latitude')?.value,
      this.realtimeService.getLatestTelemetryValue(device.deviceId, 'Longitude')?.value
    );
  }

  /** 在告警、设备和资产路径任一请求完成后重新补全共享表格行。 */
  private refreshAlarmRecords(): void {
    if (this.alarmPage === null) {
      this.alarmRecords = [];
      return;
    }
    this.alarmRecords = mapElectricalSafetyAlarms(
      this.alarmPage, this.deviceContexts, this.assetTree?.roots || []);
  }

  /** 按防雷看板的共享订阅生命周期，连接实时遥测、上下文、集合和告警变化。 */
  private subscribeToRealtimeChanges(): void {
    this.realtimeSnapshotReady = this.realtimeService.isEntitySnapshotReady;
    this.realtimeService.deviceTelemetryChanged$.pipe(
      bufferTime(100),
      filter((changes) => changes.length > 0),
      takeUntil(this.destroy$)
    ).subscribe((changes) => this.applyDashboardTelemetryChanges(changes));

    // WebSocket 首帧可能早于设备 HTTP 响应；加载完成后从服务缓存补齐最新状态。
    this.realtimeService.entitySnapshotReady$.pipe(
      takeUntil(this.destroy$)
    ).subscribe((ready) => {
      this.realtimeSnapshotReady = ready;
      if (ready) {
        this.applyLatestTelemetrySnapshot();
        if (this.pendingSnapshotReload) {
          this.pendingSnapshotReload = false;
          this.reloadDashboardSnapshots();
        }
      }
    });

    // 合并首帧期间的上下文和集合变化，等待快照就绪后最多复读一次完整看板数据。
    merge(
      this.realtimeService.deviceContextIncrementalChanged$,
      this.realtimeService.deviceMembershipChanged$,
      this.realtimeService.assetMembershipChanged$
    ).pipe(
      auditTime(500),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (!this.realtimeSnapshotReady) {
        this.pendingSnapshotReload = true;
        return;
      }
      this.reloadDashboardSnapshots();
    });

    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe((change) => {
      if (change.source === 'data') {
        if (!this.initialAlarmLoadFinished) {
          return;
        }
      } else {
        const count = change.count ?? 0;
        const previousCount = this.realtimeAlarmCount;
        this.realtimeAlarmCount = count;
        if (!this.hasRealtimeAlarmCountBaseline) {
          this.hasRealtimeAlarmCountBaseline = true;
          // 如果 WebSocket 首帧晚于 HTTP 且口径不同，只安排一次延迟校准，不重置当前页面数据。
          if (this.initialAlarmLoadFinished && previousCount !== null && previousCount !== count) {
            this.scheduleAlarmSettleRefresh();
          }
          return;
        }
        if (!this.initialAlarmLoadFinished || previousCount === count) {
          return;
        }
        if (previousCount !== null && count > previousCount) {
          this.scheduleAlarmSettleRefresh();
        }
      }
      this.loadLatestAlarms();
      this.loadAlarmInformation();
      this.loadDashboardAlarmStatisticsForSelectedPeriods(true);
    });

    this.realtimeService.acquire();
  }

  /** 设备属性、分类或可见实体集合改变后，刷新看板现有接口快照。 */
  private reloadDashboardSnapshots(): void {
    this.loadDeviceContexts();
    this.loadAssetTree();
    this.loadDashboardAlarmStatisticsForSelectedPeriods(true);
    this.loadAlarmInformation();
    this.loadLatestAlarms();
  }

  /** 新告警计数到达时稍后重读一次，覆盖服务端告警明细尚未提交的短暂时间窗。 */
  private scheduleAlarmSettleRefresh(): void {
    this.alarmSettleRefreshSubscription?.unsubscribe();
    this.alarmSettleRefreshSubscription = timer(800).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.alarmSettleRefreshSubscription = undefined;
      this.loadLatestAlarms();
      this.loadAlarmInformation();
      this.loadDashboardAlarmStatisticsForSelectedPeriods(true);
    });
  }

  /** 使用共享缓存中的 active 与最近活跃时间增量刷新设备卡片和地图标记。 */
  private applyDashboardTelemetryChanges(changes: readonly DeviceTelemetryChange[]): void {
    if (!this.deviceContexts?.length || !changes.length) {
      return;
    }
    const contextsByDeviceId = new Map<string, ElectricalSafetyDeviceContext>(
      this.deviceContexts.map((context) => [context.deviceId, context])
    );
    const activeByDeviceId = new Map<string, boolean>();
    const activityTimeByDeviceId = new Map<string, number>();
    let fireRiskMetricsChanged = false;
    let dashboardMetricsChanged = false;
    let arcFaultMetricsChanged = false;
    let mapCoordinatesChanged = false;
    for (const change of changes) {
      const context = contextsByDeviceId.get(change.deviceId);
      if (!context) {
        continue;
      }
      if (change.values.has('Latitude') || change.values.has('Longitude')) {
        mapCoordinatesChanged = true;
      }
      if (hasElectricalSafetyFireRiskMetricUpdate(context.deviceProfileName, change.values)) {
        fireRiskMetricsChanged = true;
      }
      if (hasElectricalSafetyDashboardMetricUpdate(context.deviceProfileName, change.values)) {
        dashboardMetricsChanged = true;
      }
      if (context.functionCategory?.trim() === 'arc_fault' && change.values.size > 0) {
        arcFaultMetricsChanged = true;
      }
      if (change.values.has('active')) {
        const activeSample = this.realtimeService.getLatestTelemetryValues(change.deviceId).get('active');
        const active = parseElectricalSafetyActiveValue(activeSample?.value);
        if (active !== undefined) {
          activeByDeviceId.set(change.deviceId, active);
        }
      }
      const lastActivityTime = change.lastActivityTime ?? this.realtimeService.getLatestActivityTime(change.deviceId);
      if (lastActivityTime !== undefined) {
        activityTimeByDeviceId.set(change.deviceId, lastActivityTime);
      }
    }
    this.applyDeviceTelemetrySnapshot(activeByDeviceId, activityTimeByDeviceId);
    if (mapCoordinatesChanged) {
      this.refreshMapDevices();
    }
    if (fireRiskMetricsChanged || dashboardMetricsChanged || arcFaultMetricsChanged) {
      this.refreshDeviceDerivedData();
      this.cd.markForCheck();
    }
  }

  /** 初始实体快照或上下文请求稍后返回时，从共享实时缓存补应用最新设备值。 */
  private applyLatestTelemetrySnapshot(): void {
    if (!this.deviceContexts?.length) {
      return;
    }
    const activeByDeviceId = new Map<string, boolean>();
    const activityTimeByDeviceId = new Map<string, number>();
    for (const context of this.deviceContexts) {
      const activeSample = this.realtimeService.getLatestTelemetryValues(context.deviceId).get('active');
      const active = parseElectricalSafetyActiveValue(activeSample?.value);
      if (active !== undefined) {
        activeByDeviceId.set(context.deviceId, active);
      }
      const lastActivityTime = this.realtimeService.getLatestActivityTime(context.deviceId);
      if (lastActivityTime !== undefined) {
        activityTimeByDeviceId.set(context.deviceId, lastActivityTime);
      }
    }
    this.applyDeviceTelemetrySnapshot(activeByDeviceId, activityTimeByDeviceId);
    // 首帧快照可能在设备上下文之后到达；一次重建同步补齐指标、风险值和地图坐标。
    this.refreshDeviceDerivedData();
  }

  /** 从共享 WebSocket 缓存读取火灾类别设备的最新测点，不重复建立实时订阅。 */
  private refreshFireRiskMeasurements(): void {
    if (this.deviceContexts === null) {
      this.fireRiskMeasurements = buildElectricalSafetyFireRiskMeasurements(null);
      return;
    }
    const telemetryByDeviceId = new Map<string, ReadonlyMap<string, {ts: number; value: string}>>();
    for (const context of this.deviceContexts) {
      if (!isElectricalSafetyFireFunctionCategory(context.functionCategory)) {
        continue;
      }
      telemetryByDeviceId.set(context.deviceId, this.realtimeService.getLatestTelemetryValues(context.deviceId));
    }
    this.fireRiskMeasurements = buildElectricalSafetyFireRiskMeasurements(
      this.deviceContexts, telemetryByDeviceId);
  }

  /** 比较快照后只替换发生变化的设备上下文，避免重复刷新地图和统计区域。 */
  private applyDeviceTelemetrySnapshot(
    activeByDeviceId: ReadonlyMap<string, boolean>,
    activityTimeByDeviceId: ReadonlyMap<string, number>
  ): void {
    if (!this.deviceContexts?.length || (!activeByDeviceId.size && !activityTimeByDeviceId.size)) {
      return;
    }
    let changed = false;
    this.deviceContexts = this.deviceContexts.map((context) => {
      const hasActiveUpdate = activeByDeviceId.has(context.deviceId);
      const active = activeByDeviceId.get(context.deviceId);
      const activityTime = activityTimeByDeviceId.get(context.deviceId);
      const hasNewActivityTime = activityTime !== undefined && activityTime > (context.lastActivityTime || 0);
      if ((!hasActiveUpdate || active === context.active) && !hasNewActivityTime) {
        return context;
      }
      changed = true;
      return {
        ...context,
        ...(hasActiveUpdate ? {active} : {}),
        ...(hasNewActivityTime ? {lastActivityTime: activityTime} : {})
      };
    });
    if (changed) {
      this.refreshDeviceDerivedData();
    }
  }
}
