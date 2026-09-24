/** 文件职责：看板模块的 smart-lightning-dashboard.component，负责页面状态管理、接口调用与用户交互。 */
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import { AlarmService } from '@core/http/alarm.service';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import * as echarts from 'echarts';
import { ECharts, EChartsOption } from 'echarts';
import L from 'leaflet';
import { auditTime, bufferTime, catchError, filter, forkJoin, map, merge, Observable, of, Subject, Subscription, switchMap, takeUntil, timer } from 'rxjs';
import { AlarmInfo, AlarmStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { MAX_SAFE_PAGE_SIZE } from '@shared/models/page/page-link';
import {
  calculateSmartLightningDeviceSummary,
  compareSmartLightningAlarms,
  createSmartLightningAlarmQuery,
  defaultSmartLightningChartPeriods,
  setSmartLightningChartPeriod,
  SmartLightningChartId,
  SmartLightningChartPeriods,
  SmartLightningAlarmRow,
  SmartLightningAlarmTypeTotal,
  SmartLightningDeviceSummary,
  SMART_LIGHTNING_ALARM_DISPLAY_LIMIT,
  SmartLightningPeriod,
  formatSmartLightningAlarm,
  formatSmartLightningResistance,
  getSmartLightningParentDirectory,
  getSmartLightningProductName,
  getSmartLightningAlarmChartMessage,
  shouldReloadSmartLightningAlarms,
  shouldScheduleSmartLightningAlarmSettleRefresh,
  isLatestSmartLightningAlarmRequest,
} from './smart-lightning-dashboard.data';
import { filterSmartLightningMapDevicesWithoutPoints } from '../shared/map/smart-lightning-device-map.data';
import {
  SmartLightningGroundMonitoringSummary,
  SmartLightningSpdMonitoringSummary,
  SmartLightningCurrentMonitoringSummary,
  SmartLightningMonitoringApiService,
  SmartLightningDeviceContext
} from '../monitoring/smart-lightning-monitoring-api.service';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  getSmartLightningMonitoringAssetPaths
} from '../monitoring/smart-lightning-monitoring.data';
import {
  SmartLightningDeviceTelemetryChange,
  SmartLightningRealtimeService
} from '../shared/realtime/smart-lightning-realtime.service';
import {
  buildSmartLightningExtremaTooltip,
  buildSmartLightningMaximumTooltip,
  formatSmartLightningMetric,
  getSmartLightningPhaseLabel
} from './smart-lightning-summary.data';
import {SMART_LIGHTNING_UNITS} from '../shared/telemetry/smart-lightning-measurements';
import {
  doesSmartLightningPeakSampleInvalidateExtrema,
  doesSmartLightningSampleInvalidateExtrema,
  getSmartLightningPeriodDuration,
  SmartLightningPeriodicExtremaSample,
  updateSmartLightningPeakExtrema,
  updateSmartLightningPeriodicExtrema
} from './smart-lightning-dashboard-realtime.data';
import {formatSmartLightningPolarityLabel} from '../monitoring/smart-lightning-device-detail.data';
import { SmartLightningDeviceMapComponent } from '../shared/map/smart-lightning-device-map.component';
import { SmartLightningMapDevice } from '../shared/map/smart-lightning-device-map.models';

interface SmartLightningPendingMapPoint {
  latitude: number;
  longitude: number;
}

/**
 * 智能防雷看板页面组件。
 *
 * <p>监控大屏：设备汇总、地图点位、地网/SPD/雷电/告警等图表与告警记录表。
 * 设备数量、地网汇总、告警和地图点位接入真实接口；尚未接入历史数据的图表显示空态。
 * 每张图表拥有独立的日/周/月周期状态，切换互不影响。</p>
 */
@Component({
  selector: 'tb-smart-lightning-dashboard',
  templateUrl: './smart-lightning-dashboard.component.html',
  styleUrls: ['./smart-lightning-dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningDashboardComponent implements AfterViewInit, OnDestroy {

  /** 将统一单位表暴露给模板，避免模板内重复维护温度、电压和漏流单位。 */
  readonly SMART_LIGHTNING_UNITS = SMART_LIGHTNING_UNITS;

  @ViewChild('deviceMap') deviceMap?: SmartLightningDeviceMapComponent;
  @ViewChildren('chartHost') chartHosts!: QueryList<ElementRef<HTMLDivElement>>;

  chartPeriods: SmartLightningChartPeriods = {...defaultSmartLightningChartPeriods};
  alarms: SmartLightningAlarmRow[] = [];
  alarmsLoading = true;
  alarmsLoadError = false;
  alarmSummaryLoading = true;
  alarmSummaryLoadError = false;
  deviceSummary: SmartLightningDeviceSummary = calculateSmartLightningDeviceSummary([]);
  devicesLoading = true;
  devicesLoadError = false;
  groundAlarmPeriod = SmartLightningPeriod.day;
  groundSummary: SmartLightningGroundMonitoringSummary | null = null;
  groundLoading = true;
  groundLoadError = false;
  spdSummary: SmartLightningSpdMonitoringSummary | null = null;
  spdLoading = true;
  spdLoadError = false;
  lightningSummary: SmartLightningCurrentMonitoringSummary | null = null;
  lightningLoading = true;
  lightningLoadError = false;
  activeAlarmCount: number | null = null;
  activeAlarmTypeTotals: SmartLightningAlarmTypeTotal[] = [];

  private readonly charts = new Map<string, ECharts>();
  private readonly destroy$ = new Subject<void>();
  private resizeObserver?: ResizeObserver;
  private realtimeAlarmCount: number | null = null;
  private initialAlarmLoadFinished = false;
  /** 告警列表请求序号；只允许最后一次请求更新页面状态。 */
  private alarmLoadRequestId = 0;
  /** 告警创建通知可能早于后端落库，保存一次有界的延迟复读订阅。 */
  private alarmSettleRefreshSubscription?: Subscription;
  /** 最值在滚动周期边界滑出时，只安排一次现有接口校准，不做轮询。 */
  private readonly extremaRefreshSubscriptions = new Map<'ground' | 'spd' | 'lightning', Subscription>();

  /** 控制地图是否处于用户选点模式；坐标需在绑定设备后才会持久化。 */
  isAddingMapPoint = false;
  /** 当前用户可见的防雷设备，供地图点位绑定下拉框使用。 */
  mapDevices: SmartLightningDeviceContext[] = [];
  /** 将防雷设备上下文转换为公共地图模型；分类边界由本页面决定。 */
  get mapDevicesForMap(): SmartLightningMapDevice[] {
    return this.mapDevices.map((device) => ({
      id: device.deviceId,
      name: device.deviceName,
      productName: device.productName || '--',
      profileName: device.deviceProfileName || '--',
      // 接口已经按 productCategory=lightning 过滤；缺少回传字段时按该接口契约回退，避免旧服务版本误隐藏点位。
      productCategory: device.productCategory || 'lightning',
      active: device.active === true,
      latitude: device.latitude ?? undefined,
      longitude: device.longitude ?? undefined,
      assetPath: this.mapAssetPaths.get(device.deviceId) || '--',
      alarmNames: this.activeAlarmNames.get(device.deviceId) || []
    }));
  }
  /** 智能防雷页面只显示 productCategory=lightning 的设备；公共组件不承担该业务筛选。 */
  readonly includeSmartLightningMapDevice = (device: SmartLightningMapDevice): boolean =>
    device.productCategory?.trim().toLowerCase() === 'lightning';
  /**
   * 地图点位绑定下拉框的候选设备。
   *
   * 过滤依据是已经绘制到地图上的设备点位 ID；已绑定设备不会再次出现在下拉框中，
   * 避免同一设备被重复绑定到多个点位。
   */
  get availableMapDevices(): SmartLightningDeviceContext[] {
    const mappedDeviceIds = new Set(this.mapDevices
      .filter((device) => device.latitude != null && device.longitude != null)
      .map((device) => device.deviceId));
    return filterSmartLightningMapDevicesWithoutPoints(
      this.mapDevices,
      mappedDeviceIds
    );
  }
  /** 设备 ID 到根资产路径的索引，供公共地图组件弹窗显示绝对路径。 */
  mapAssetPaths = new Map<string, string>();
  /** 当前未清除告警按设备聚合的名称，供公共地图组件渲染状态和弹窗。 */
  activeAlarmNames = new Map<string, string[]>();
  /** 已选地图坐标；只有确认绑定后才会写入设备服务器属性。 */
  pendingMapPoint: SmartLightningPendingMapPoint | null = null;
  /** 绑定面板中选中的设备 ID。 */
  selectedMapDeviceId = '';
  /** 防止用户连续点击导致重复提交。 */
  mapPointSaving = false;
  /** 保存失败时在绑定面板内显示的错误信息。 */
  mapPointSaveError = '';

  constructor(
    private readonly cd: ChangeDetectorRef,
    private readonly alarmService: AlarmService,
    private readonly attributeService: AttributeService,
    private readonly deviceService: DeviceService,
    private readonly entityRelationService: EntityRelationService,
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly realtimeService: SmartLightningRealtimeService
  ) {}

  ngAfterViewInit(): void {
    this.renderCharts();
    this.loadLatestAlarms();
    this.loadAlarmSummary();
    this.subscribeToRealtimeChanges();
    this.loadDeviceSummary();
    this.loadGroundSummary();
    this.loadSpdSummary();
    this.loadLightningSummary();
    this.resizeObserver = new ResizeObserver(() => this.charts.forEach(chart => chart.resize()));
    this.chartHosts.forEach(host => this.resizeObserver.observe(host.nativeElement));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.alarmSettleRefreshSubscription?.unsubscribe();
    this.extremaRefreshSubscriptions.forEach((subscription) => subscription.unsubscribe());
    this.extremaRefreshSubscriptions.clear();
    this.realtimeService.release();
    this.resizeObserver?.disconnect();
    this.charts.forEach(chart => chart.dispose());
    this.charts.clear();
  }

  /** 恢复地图上的添加点位入口，用户点击地图后生成一个临时点位。 */
  toggleMapPointMode(): void {
    if (!this.deviceMap) {
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

  /** 公共地图组件回传用户点击的坐标，页面随后负责选择设备并保存属性。 */
  onMapClick(point: L.LatLng): void {
    this.isAddingMapPoint = false;
    this.pendingMapPoint = {latitude: point.lat, longitude: point.lng};
    this.selectedMapDeviceId = '';
    this.mapPointSaveError = '';
    this.cd.markForCheck();
  }



  /** 记录用户在绑定面板中选择的设备。 */
  selectMapDevice(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedMapDeviceId = select.value;
    this.mapPointSaveError = '';
  }

  /** 将点位坐标写入设备 SERVER_SCOPE，绑定关系由设备属性持久化。 */
  saveMapPointBinding(): void {
    if (!this.pendingMapPoint || !this.selectedMapDeviceId || this.mapPointSaving) {
      return;
    }
    // 下拉框会过滤已绑定设备，但提交前仍再次校验，防止并发刷新或旧页面状态造成重复绑定。
    const selectedDevice = this.mapDevices.find((device) => device.deviceId === this.selectedMapDeviceId);
    if (!selectedDevice || (selectedDevice.latitude != null && selectedDevice.longitude != null)) {
      this.mapPointSaveError = '该设备已经绑定地图点位，请重新选择未绑定设备。';
      this.selectedMapDeviceId = '';
      this.cd.markForCheck();
      return;
    }
    this.mapPointSaving = true;
    this.mapPointSaveError = '';
    // AttributeService 最终调用设备属性接口，保存内容不进入遥测表。
    this.attributeService.saveEntityAttributes(
      {entityType: EntityType.DEVICE, id: this.selectedMapDeviceId},
      AttributeScope.SERVER_SCOPE,
      [
        {key: 'latitude', value: this.pendingMapPoint.latitude},
        {key: 'longitude', value: this.pendingMapPoint.longitude}
      ]
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        const device = this.mapDevices.find(item => item.deviceId === this.selectedMapDeviceId);
        if (device) {
          // 保存成功后同步本地上下文，立即复用持久化点位渲染逻辑，无需刷新页面。
          device.latitude = this.pendingMapPoint.latitude;
          device.longitude = this.pendingMapPoint.longitude;
        }
        // 清理临时图层后由公共组件重新生成正式图层，保证当前页面和重新进入页面表现一致。
        this.deviceMap?.refresh();
        this.deviceMap?.openDevicePopup(this.selectedMapDeviceId);
        this.pendingMapPoint = null;
        this.selectedMapDeviceId = '';
        this.mapPointSaving = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.mapPointSaving = false;
        this.mapPointSaveError = '设备属性保存失败，请检查当前账号的设备写入权限。';
        this.cd.markForCheck();
      }
    });
  }

  /** 取消当前坐标的设备绑定，同时移除尚未保存的临时点位。 */
  cancelMapPointBinding(): void {
    this.pendingMapPoint = null;
    this.selectedMapDeviceId = '';
    this.mapPointSaveError = '';
    this.cd.markForCheck();
  }

  /** 切换指定图表的统计周期，只影响该图表自身。 */
  setChartPeriod(chart: SmartLightningChartId, period: SmartLightningPeriod): void {
    if (period === this.chartPeriods[chart]) {
      return;
    }
    this.chartPeriods = setSmartLightningChartPeriod(this.chartPeriods, chart, period);
    if (chart === 'spd') {
      this.loadSpdSummary();
    } else if (chart === 'lightning') {
      this.loadLightningSummary();
    } else {
      this.renderChart(chart);
    }
    this.cd.markForCheck();
  }

  /** 地网模块的日/周/月只影响按告警触发时间统计的告警数。 */
  setGroundAlarmPeriod(period: SmartLightningPeriod): void {
    if (period === this.groundAlarmPeriod) {
      return;
    }
    this.groundAlarmPeriod = period;
    this.loadGroundSummary();
  }

  /** 地网阻值展示格式化，空值显示占位符。 */
  formatGroundResistance(value: number | null | undefined): string {
    return formatSmartLightningResistance(value);
  }

  /** 地网报警率展示格式；接口按告警设备去重计算，前端不截断接口返回的小数位。 */
  formatGroundAlarmRate(value: number | null | undefined): string {
    return value === null || value === undefined || !Number.isFinite(value) ? '--' : `${value}%`;
  }

  /** 生成地网本周期告警数字的悬浮详情；告警类型按数量降序展示。 */
  formatGroundAlarmTooltip(summary: SmartLightningGroundMonitoringSummary | null): string {
    if (!summary) {
      return '告警类型统计\n暂无数据';
    }
    const totals = summary.alarmTypeTotals || [];
    if (!totals.length) {
      return `告警类型统计\n周期内暂无告警（合计：${summary.alarmCount}次）`;
    }
    const lines = totals.map((item, index) => `${index + 1}. ${item.type}：${item.count}次`);
    return `告警类型统计（合计：${summary.alarmCount}次）\n${lines.join('\n')}`;
  }

  /** 生成地网阻值最值的悬浮详情，显示设备、参数和采集时间。 */
  formatGroundExtremaTooltip(
    label: string,
    value: number | null | undefined,
    deviceName: string | null | undefined,
    parameter: string | null | undefined,
    timestamp: number | null | undefined
  ): string {
    return buildSmartLightningExtremaTooltip(
      label,
      value,
      SMART_LIGHTNING_UNITS.resistance,
      deviceName,
      parameter,
      timestamp
    );
  }

  /** 生成 SPD 本周期告警数字的悬浮详情；后端已按告警类型数量降序返回。 */
  formatSpdAlarmTypeTooltip(summary: SmartLightningSpdMonitoringSummary | null): string {
    if (!summary) {
      return '告警类型统计\n暂无数据';
    }
    const totals = summary.alarmTypeTotals || [];
    if (!totals.length) {
      return `告警类型统计\n周期内暂无告警（合计：${summary.alarmCount}）`;
    }
    const lines = totals.map((item, index) => `${index + 1}. ${item.type}：${item.count}`);
    return `告警类型统计（合计：${summary.alarmCount}）\n${lines.join('\n')}`;
  }

  /** SPD 卡片周期数值保留接口返回的原始小数位；单位只负责展示，不做精度处理。 */
  formatSpdPeriodicValue(value: number | null | undefined, unit: string): string {
    return formatSmartLightningMetric(value, unit);
  }

  /** 生成 SPD 周期最值的原生悬浮提示文本。 */
  formatSpdExtremaTooltip(
    label: string,
    value: number | null | undefined,
    unit: string,
    deviceName: string | null | undefined,
    parameter: string | null | undefined,
    timestamp: number | null | undefined
  ): string {
    return buildSmartLightningExtremaTooltip(label, value, unit, deviceName, parameter, timestamp);
  }

  /** 雷电最大峰值展示格式化，空值显示占位符。 */
  formatLightningPeak(value: number | null | undefined): string {
    return formatSmartLightningMetric(value, SMART_LIGHTNING_UNITS.lightningCurrent);
  }

  /** 生成雷电流本周期告警数字的悬浮详情；后端已按告警类型数量降序返回。 */
  formatLightningAlarmTooltip(summary: SmartLightningCurrentMonitoringSummary | null): string {
    if (!summary) {
      return '告警类型统计\n暂无数据';
    }
    const totals = summary.alarmTypeTotals || [];
    if (!totals.length) {
      return `告警类型统计\n周期内暂无告警（合计：${summary.alarmCount}次）`;
    }
    const lines = totals.map((item, index) => `${index + 1}. ${item.type}：${item.count}次`);
    return `告警类型统计（合计：${summary.alarmCount}次）\n${lines.join('\n')}`;
  }

  /** 生成雷电最大峰值的原生悬浮提示文本。 */
  formatLightningMaximumTooltip(summary: SmartLightningCurrentMonitoringSummary): string {
    return buildSmartLightningMaximumTooltip(
      '最大峰值',
      summary.maxPeakCurrent,
      SMART_LIGHTNING_UNITS.lightningCurrent,
      summary.maxPeakDeviceName,
      '极性',
      summary.maxPeakPolarity,
      summary.maxPeakTime
    );
  }

  /** 生成雷电最小峰值的原生悬浮提示文本。 */
  formatLightningMinimumTooltip(summary: SmartLightningCurrentMonitoringSummary): string {
    return buildSmartLightningMaximumTooltip(
      '最小峰值',
      summary.minPeakCurrent,
      SMART_LIGHTNING_UNITS.lightningCurrent,
      summary.minPeakDeviceName,
      '极性',
      summary.minPeakPolarity,
      summary.minPeakTime
    );
  }

  /** 拉取最新告警列表：加载行上下文后写入告警表并刷新地图弹窗告警信息。 */
  loadLatestAlarms(): void {
    const requestId = ++this.alarmLoadRequestId;
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    this.alarmService.getAllAlarmsV2(createSmartLightningAlarmQuery()).pipe(
      map((pageData): AlarmInfo[] => {
        const sortedAlarms = pageData.data.map((alarm) => ({info: alarm, row: formatSmartLightningAlarm(alarm)}))
          .sort((left, right) => compareSmartLightningAlarms(left.row, right.row))
        // 地图告警状态必须基于全部当前告警，不能受表格只显示 10 条的限制。
        this.updateActiveAlarmNames(sortedAlarms.map(({info}) => info));
        return sortedAlarms.slice(0, SMART_LIGHTNING_ALARM_DISPLAY_LIMIT).map(({info}) => info);
      }),
      switchMap((alarms) => this.resolveAlarmRows(alarms)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (alarms) => {
        // WebSocket 变化可能在请求完成前再次触发刷新，旧响应不能覆盖新列表。
        if (!isLatestSmartLightningAlarmRequest(requestId, this.alarmLoadRequestId)) {
          return;
        }
        this.alarms = alarms;
        this.alarmsLoading = false;
        this.initialAlarmLoadFinished = true;
        this.renderChart('alarm');
        // 告警接口和设备接口是并行请求，告警先返回时也要刷新已经绘制的点位弹窗。
        this.deviceMap?.refresh();
        this.cd.markForCheck();
      },
      error: () => {
        // 旧请求失败时也不能清空新请求正在加载的结果。
        if (!isLatestSmartLightningAlarmRequest(requestId, this.alarmLoadRequestId)) {
          return;
        }
        this.alarms = [];
        this.activeAlarmNames.clear();
        this.activeAlarmCount = null;
        this.activeAlarmTypeTotals = [];
        this.alarmsLoadError = true;
        this.alarmsLoading = false;
        this.initialAlarmLoadFinished = true;
        this.renderChart('alarm');
        this.deviceMap?.refresh();
        this.cd.markForCheck();
      }
    });
  }

  /** 告警统计由后端按当前用户可见防雷设备统一聚合，页面不再自行遍历告警记录。 */
  private loadAlarmSummary(): void {
    this.alarmSummaryLoading = true;
    this.alarmSummaryLoadError = false;
    this.monitoringApi.getLightningAlarmSummary().pipe(takeUntil(this.destroy$)).subscribe({
      next: (summary) => {
        this.activeAlarmCount = summary.activeCount;
        this.activeAlarmTypeTotals = summary.typeTotals;
        this.alarmSummaryLoading = false;
        this.renderChart('alarm');
        this.cd.markForCheck();
      },
      error: () => {
        this.activeAlarmCount = null;
        this.activeAlarmTypeTotals = [];
        this.alarmSummaryLoading = false;
        this.alarmSummaryLoadError = true;
        this.renderChart('alarm');
        this.cd.markForCheck();
      }
    });
  }

  /**
   * 订阅看板依赖的实时事件。
   * 周期最值由当前可见设备的 WebSocket 增量更新；HTTP 继续提供首屏/周期基线、
   * 设备上下文变化后的校准，以及告警统计和窗口边界校准。
   */
  private subscribeToRealtimeChanges(): void {
    this.realtimeService.deviceTelemetryChanged$.pipe(
      bufferTime(100),
      filter((changes) => changes.length > 0),
      takeUntil(this.destroy$)
    ).subscribe((changes) => {
      this.applyDashboardTelemetryChanges(changes);
    });

    // 完整首帧可能先于设备上下文 HTTP 到达；上下文加载完成后再从共享缓存补应用一次。
    this.realtimeService.entitySnapshotReady$.pipe(
      filter((ready) => ready),
      takeUntil(this.destroy$)
    ).subscribe(() => this.applyLatestTelemetrySnapshot());

    // 只在设备业务属性/实体字段真正增量变化时重载上下文和服务端聚合。
    this.realtimeService.deviceContextIncrementalChanged$.pipe(
      auditTime(500),
      takeUntil(this.destroy$)
    ).subscribe(() => this.reloadDashboardSnapshots());

    // 设备或资产集合数量发生变化时重新读取权限范围内的完整集合与聚合结果。
    merge(
      this.realtimeService.deviceMembershipChanged$,
      this.realtimeService.assetMembershipChanged$
    ).pipe(
      auditTime(500),
      takeUntil(this.destroy$)
    ).subscribe(() => this.reloadDashboardSnapshots());

    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe((change) => {
      // 告警字段（确认、派发、清除、备注）变化时数量可能不变，也必须刷新列表和统计。
      if (change.source === 'data') {
        if (!this.initialAlarmLoadFinished) {
          return;
        }
      } else {
        const count = change.count ?? 0;
        const previousCount = this.realtimeAlarmCount;
        this.realtimeAlarmCount = count;
        if (!this.initialAlarmLoadFinished || !shouldReloadSmartLightningAlarms(previousCount, count)) {
          return;
        }
        // 新增告警的数量通知可能早于告警明细事务提交；立即刷新用于快速响应，
        // 仅对数量增加再安排一次延迟复读，避免读到落库前的旧列表。
        if (shouldScheduleSmartLightningAlarmSettleRefresh(previousCount, count)) {
          this.scheduleAlarmSettleRefresh();
        }
      }
      this.loadLatestAlarms();
      this.loadAlarmSummary();
      // 分类卡片还包含周期告警数，告警变化后用原接口重新校准这些告警字段。
      this.loadGroundSummary();
      this.loadSpdSummary();
      this.loadLightningSummary();
    });

    this.realtimeService.acquire();
  }

  /** 设备业务上下文或可见设备范围变化后，沿用原接口刷新授权范围内的数据。 */
  private reloadDashboardSnapshots(): void {
    this.loadDeviceSummary();
    this.loadGroundSummary();
    this.loadSpdSummary();
    this.loadLightningSummary();
  }

  /**
   * 在告警创建通知后最多延迟复读一次列表。
   * 新事件到达时取消旧计时器，防止多个计时器叠加请求。
   */
  private scheduleAlarmSettleRefresh(): void {
    this.alarmSettleRefreshSubscription?.unsubscribe();
    this.alarmSettleRefreshSubscription = timer(800).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.alarmSettleRefreshSubscription = undefined;
      this.loadLatestAlarms();
    });
  }

  /** 批量读取设备与 active 属性，计算顶部设备在线汇总并绘制地图点位。 */
  private loadDeviceSummary(): void {
    this.devicesLoading = true;
    this.devicesLoadError = false;
    forkJoin({
      devices: this.monitoringApi.getLightningDeviceContexts(MAX_SAFE_PAGE_SIZE, 0),
      // 资产树只请求一次，用于把设备 ID 映射成根资产到设备的完整路径。
      tree: this.monitoringApi.getTree().pipe(catchError(() => of({roots: []})))
    }).pipe(
      map((pageData) => {
        // 同一次批量查询同时提供设备状态统计和地图绑定候选，避免再次逐台请求设备。
        this.mapDevices = pageData.devices.data;
        const assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(pageData.tree.roots);
        this.mapAssetPaths = getSmartLightningMonitoringAssetPaths(assetTree);
        // 设备上下文已经包含 SERVER_SCOPE 经纬度，读取完成后立即恢复持久化点位。
        this.deviceMap?.refresh();
        return pageData.devices.data;
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (devices) => {
        this.deviceSummary = calculateSmartLightningDeviceSummary(devices.map((device) =>
          this.realtimeService.getLatestTelemetryValues(device.deviceId).get('active')?.value ?? device.active
        ));
        this.devicesLoading = false;
        // HTTP 与 WS 并发时，接口响应之后再应用最新缓存，避免首屏请求覆盖刚到达的实时值。
        this.applyLatestTelemetrySnapshot();
        this.cd.markForCheck();
      },
      error: () => {
        this.devicesLoadError = true;
        this.devicesLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  /** 读取地网模块的单个聚合接口，避免浏览器逐台读取属性、遥测和告警。 */
  loadGroundSummary(): void {
    this.groundLoading = true;
    this.groundLoadError = false;
    this.monitoringApi.getGroundMonitoringSummary(this.groundAlarmPeriod).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (summary) => {
        this.groundSummary = summary;
        this.groundLoading = false;
        this.scheduleGroundExtremaRefresh();
        this.cd.markForCheck();
      },
      error: () => {
        this.groundSummary = null;
        this.groundLoadError = true;
        this.groundLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  /** 读取 SPD 监测聚合接口并更新卡片与图表。 */
  loadSpdSummary(): void {
    this.spdLoading = true;
    this.spdLoadError = false;
    this.monitoringApi.getSpdMonitoringSummary(this.chartPeriods.spd).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (summary) => {
        this.spdSummary = summary;
        this.spdLoading = false;
        this.scheduleSpdExtremaRefresh();
        this.cd.markForCheck();
      },
      error: () => {
        this.spdSummary = null;
        this.spdLoading = false;
        this.spdLoadError = true;
        this.cd.markForCheck();
      }
    });
  }

  /** 读取雷电流监测聚合接口并更新卡片与图表。 */
  loadLightningSummary(): void {
    this.lightningLoading = true;
    this.lightningLoadError = false;
    this.monitoringApi.getLightningCurrentMonitoringSummary(this.chartPeriods.lightning).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (summary) => {
        this.lightningSummary = summary;
        this.lightningLoading = false;
        this.scheduleLightningExtremaRefresh();
        this.cd.markForCheck();
      },
      error: () => {
        this.lightningSummary = null;
        this.lightningLoading = false;
        this.lightningLoadError = true;
        this.cd.markForCheck();
      }
    });
  }

  /**
   * 把当前已加载的可见设备及本次 WebSocket 遥测增量合并后更新周期极值。
   * 未在当前设备上下文中的实体不会参与，权限/产品分类边界仍由原接口提供。
   */
  private applyDashboardTelemetryChanges(changes: readonly SmartLightningDeviceTelemetryChange[]): void {
    if (!this.mapDevices.length || !changes.length) {
      return;
    }

    const devicesById = new Map(this.mapDevices.map((device) => [device.deviceId, device]));
    const activeUpdates = new Map<string, boolean>();
    const groundSamples: SmartLightningPeriodicExtremaSample[] = [];
    const temperatureSamples: SmartLightningPeriodicExtremaSample[] = [];
    const leakageSamples: SmartLightningPeriodicExtremaSample[] = [];
    const voltageSamples: SmartLightningPeriodicExtremaSample[] = [];
    const peakSamples: Array<SmartLightningPeriodicExtremaSample & {polarity?: string | null}> = [];
    const temperatureKeys = new Set(['Temp1', 'Temp2', 'Temp3', 'Temp4', 'AmbTemp', 'SpdTemp']);
    const leakageKeys = new Set(['LeakCurrA', 'LeakCurrB', 'LeakCurrC']);
    const voltageKeys = new Set(['PhAVolt', 'PhBVolt', 'PhCVolt']);

    for (const change of changes) {
      const device = devicesById.get(change.deviceId);
      if (!device) {
        continue;
      }
      const latestValues = this.realtimeService.getLatestTelemetryValues(device.deviceId);
      const active = this.parseActiveTelemetry(
        change.values.has('active') ? latestValues.get('active')?.value : undefined
      );
      if (active !== undefined) {
        activeUpdates.set(device.deviceId, active);
      }

      const functionCategory = String(device.functionCategory || '').trim().toLowerCase();
      for (const [key, incoming] of change.values) {
        // 服务层可能收到乱序帧；始终采用共享缓存中时间戳最新的该键。
        const telemetry = latestValues.get(key) || incoming;
        if (functionCategory === 'ground_resistance' && key === 'GndGridRes') {
          const sample = this.createPeriodicSample(device.deviceName, key, telemetry);
          if (sample) {
            groundSamples.push(sample);
          }
        } else if (functionCategory === 'spd_monitor') {
          const sample = this.createPeriodicSample(device.deviceName, key, telemetry);
          if (!sample) {
            continue;
          }
          if (temperatureKeys.has(key)) {
            temperatureSamples.push(sample);
          } else if (leakageKeys.has(key)) {
            leakageSamples.push(sample);
          } else if (voltageKeys.has(key)) {
            voltageSamples.push(sample);
          }
        }
      }

      if (functionCategory === 'lightning_current' &&
        (change.values.has('PeakValue') || change.values.has('Polarity'))) {
        // 峰值和极性可能分别到达；仅时间戳一致时才将极性绑定到该峰值，避免误配来源。
        const peak = change.values.get('PeakValue') || latestValues.get('PeakValue');
        const polarity = latestValues.get('Polarity');
        if (peak && (change.values.has('PeakValue') || change.values.has('Polarity'))) {
          const sample = this.createPeriodicSample(device.deviceName, 'PeakValue', peak);
          if (sample) {
            peakSamples.push({
              ...sample,
              polarity: polarity?.ts === peak.ts ? this.normalizeDashboardPolarity(polarity.value) : null
            });
          }
        }
      }
    }

    if (activeUpdates.size) {
      this.mapDevices = this.mapDevices.map((device) => activeUpdates.has(device.deviceId)
        ? {...device, active: activeUpdates.get(device.deviceId)}
        : device);
      this.deviceSummary = calculateSmartLightningDeviceSummary(this.mapDevices.map((device) => device.active));
    }

    let changed = activeUpdates.size > 0;
    if (groundSamples.length && this.groundSummary) {
      const current = this.groundSummary.resistanceExtrema;
      if (doesSmartLightningSampleInvalidateExtrema(
        current, groundSamples, false, this.groundAlarmPeriod, Date.now()
      )) {
        this.loadGroundSummary();
      } else {
        const extrema = updateSmartLightningPeriodicExtrema(
          current, groundSamples, this.groundAlarmPeriod, Date.now()
        );
        if (extrema !== current) {
          this.groundSummary = {
            ...this.groundSummary,
            maxResistance: extrema.maximum,
            minResistance: extrema.minimum,
            resistanceExtrema: extrema
          };
          this.scheduleGroundExtremaRefresh();
          changed = true;
        }
      }
    }

    if ((temperatureSamples.length || leakageSamples.length || voltageSamples.length) && this.spdSummary) {
      const temperature = this.spdSummary.temperatureExtrema;
      const leakage = this.spdSummary.leakageExtrema;
      const voltage = this.spdSummary.voltageExtrema;
      const sourceWasOverwritten =
        doesSmartLightningSampleInvalidateExtrema(temperature, temperatureSamples, false, this.chartPeriods.spd, Date.now()) ||
        doesSmartLightningSampleInvalidateExtrema(leakage, leakageSamples, true, this.chartPeriods.spd, Date.now()) ||
        doesSmartLightningSampleInvalidateExtrema(voltage, voltageSamples, false, this.chartPeriods.spd, Date.now());
      if (sourceWasOverwritten) {
        this.loadSpdSummary();
      } else {
        const nextTemperature = updateSmartLightningPeriodicExtrema(
          temperature, temperatureSamples, this.chartPeriods.spd, Date.now()
        );
        const nextLeakage = updateSmartLightningPeriodicExtrema(
          leakage, leakageSamples, this.chartPeriods.spd, Date.now(), true
        );
        const nextVoltage = updateSmartLightningPeriodicExtrema(
          voltage, voltageSamples, this.chartPeriods.spd, Date.now()
        );
        if (nextTemperature !== temperature || nextLeakage !== leakage || nextVoltage !== voltage) {
          this.spdSummary = {
            ...this.spdSummary,
            temperatureExtrema: nextTemperature,
            leakageExtrema: nextLeakage,
            voltageExtrema: nextVoltage,
            maxLeakage: nextLeakage.maximum,
            maxLeakageDeviceName: nextLeakage.maximumDeviceName,
            maxLeakageKey: nextLeakage.maximumParameter,
            maxLeakagePhase: getSmartLightningPhaseLabel(nextLeakage.maximumParameter),
            maxLeakageTime: nextLeakage.maximumTime
          };
          this.scheduleSpdExtremaRefresh();
          changed = true;
        }
      }
    }

    if (peakSamples.length && this.lightningSummary) {
      if (doesSmartLightningPeakSampleInvalidateExtrema(
        this.lightningSummary, peakSamples, this.chartPeriods.lightning, Date.now()
      )) {
        this.loadLightningSummary();
      } else {
        const summary = updateSmartLightningPeakExtrema(
          this.lightningSummary, peakSamples, this.chartPeriods.lightning, Date.now()
        );
        if (summary !== this.lightningSummary) {
          this.lightningSummary = summary;
          this.scheduleLightningExtremaRefresh();
          changed = true;
        }
      }
    }

    if (changed) {
      this.cd.markForCheck();
    }
  }

  /** 完整首帧或设备上下文响应较晚时，从共享实时缓存补应用当前设备快照。 */
  private applyLatestTelemetrySnapshot(): void {
    const changes = this.mapDevices.map((device) => ({
      deviceId: device.deviceId,
      values: this.realtimeService.getLatestTelemetryValues(device.deviceId)
    }));
    this.applyDashboardTelemetryChanges(changes);
  }

  /** 从原始 WebSocket 字符串解析有效数值，不进行小数截断或单位换算。 */
  private createPeriodicSample(
    deviceName: string,
    parameter: string,
    telemetry: {readonly ts: number; readonly value: string} | undefined
  ): SmartLightningPeriodicExtremaSample | null {
    if (!telemetry) {
      return null;
    }
    const value = Number(String(telemetry.value ?? '').trim());
    return Number.isFinite(value)
      ? {value, deviceName, parameter, timestamp: telemetry.ts}
      : null;
  }

  /** 统一解析设备 active 遥测，只接受明确的在线/离线编码。 */
  private parseActiveTelemetry(value: string | undefined): boolean | undefined {
    const normalized = String(value ?? '').trim().toLowerCase();
    if (normalized === 'true' || normalized === '1') {
      return true;
    }
    if (normalized === 'false' || normalized === '0') {
      return false;
    }
    return undefined;
  }

  /** 看板周期最值接口返回“正/负极性”，增量也统一为相同中文标签。 */
  private normalizeDashboardPolarity(value: string): string {
    const normalized = String(value ?? '').trim();
    const numeric = Number(normalized);
    if (Number.isFinite(numeric) && numeric === 1) {
      return '正极性';
    }
    if (Number.isFinite(numeric) && numeric === 0) {
      return '负极性';
    }
    const label = formatSmartLightningPolarityLabel(normalized);
    if (label === '正极') {
      return '正极性';
    }
    if (label === '负极') {
      return '负极性';
    }
    return label;
  }

  /** 根据最值源时间计算滚动窗口过期时刻，到期后使用原聚合接口重新校准一次。 */
  private scheduleExtremaRefresh(
    target: 'ground' | 'spd' | 'lightning',
    period: SmartLightningPeriod,
    timestamps: Array<number | null | undefined>
  ): void {
    this.extremaRefreshSubscriptions.get(target)?.unsubscribe();
    this.extremaRefreshSubscriptions.delete(target);
    const duration = getSmartLightningPeriodDuration(period);
    const sourceTimes = timestamps.filter((timestamp): timestamp is number =>
      timestamp != null && Number.isFinite(timestamp)
    );
    if (duration == null || !sourceTimes.length) {
      return;
    }

    const refreshAt = Math.min(...sourceTimes.map((timestamp) => timestamp + duration + 1));
    const delay = Math.max(0, Math.min(refreshAt - Date.now(), 2_147_000_000));
    const subscription = timer(delay).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.extremaRefreshSubscriptions.delete(target);
      if (Date.now() < refreshAt) {
        // 月周期超过浏览器单次定时器上限时，分段等待到真实过期时间。
        this.scheduleExtremaRefresh(target, period, sourceTimes);
        return;
      }
      if (target === 'ground') {
        this.loadGroundSummary();
      } else if (target === 'spd') {
        this.loadSpdSummary();
      } else {
        this.loadLightningSummary();
      }
    });
    this.extremaRefreshSubscriptions.set(target, subscription);
  }

  /** 为地网周期阻值最值安排一次性滚动窗口校准。 */
  private scheduleGroundExtremaRefresh(): void {
    const extrema = this.groundSummary?.resistanceExtrema;
    this.scheduleExtremaRefresh('ground', this.groundAlarmPeriod, [
      extrema?.maximumTime, extrema?.minimumTime
    ]);
  }

  /** 为 SPD 温度、漏电流、电压的周期最值安排一次性滚动窗口校准。 */
  private scheduleSpdExtremaRefresh(): void {
    const summary = this.spdSummary;
    this.scheduleExtremaRefresh('spd', this.chartPeriods.spd, [
      summary?.temperatureExtrema.maximumTime, summary?.temperatureExtrema.minimumTime,
      summary?.leakageExtrema.maximumTime, summary?.leakageExtrema.minimumTime,
      summary?.voltageExtrema.maximumTime, summary?.voltageExtrema.minimumTime
    ]);
  }

  /** 为雷电流周期峰值最值安排一次性滚动窗口校准。 */
  private scheduleLightningExtremaRefresh(): void {
    this.scheduleExtremaRefresh('lightning', this.chartPeriods.lightning, [
      this.lightningSummary?.maxPeakTime, this.lightningSummary?.minPeakTime
    ]);
  }

  private resolveAlarmRows(alarms: AlarmInfo[]): Observable<SmartLightningAlarmRow[]> {
    if (!alarms.length) {
      return of([]);
    }
    return forkJoin(alarms.map((alarm) => this.resolveAlarmRow(alarm)));
  }

  /** 补全单条告警的展示上下文（产品名、父级目录），返回可渲染的告警行。 */
  private resolveAlarmRow(alarm: AlarmInfo): Observable<SmartLightningAlarmRow> {
    if (alarm.originator?.entityType !== EntityType.DEVICE) {
      return of(formatSmartLightningAlarm(alarm));
    }
    const deviceId = alarm.originator.id;
    return forkJoin({
      deviceProfileName: this.deviceService.getDeviceInfo(deviceId).pipe(
        map((device) => device.deviceProfileName),
        catchError(() => of('--'))
      ),
      parentDirectory: this.entityRelationService.findInfoByTo(alarm.originator).pipe(
        map(getSmartLightningParentDirectory),
        catchError(() => of('--'))
      ),
      productName: this.attributeService.getEntityAttributes(
        alarm.originator,
        AttributeScope.SERVER_SCOPE,
        ['productName']
      ).pipe(
        map(getSmartLightningProductName),
        catchError(() => of('--'))
      )
    }).pipe(map((context) => formatSmartLightningAlarm(alarm, context)));
  }

  /** 将当前未清除告警按设备 ID 聚合告警名称，避免地图弹窗逐设备发起告警请求。 */
  private updateActiveAlarmNames(alarms: AlarmInfo[]): void {
    this.activeAlarmNames.clear();
    for (const alarm of alarms) {
      if (alarm.originator?.entityType !== EntityType.DEVICE ||
        (alarm.status !== AlarmStatus.ACTIVE_UNACK && alarm.status !== AlarmStatus.ACTIVE_ACK)) {
        continue;
      }
      const deviceId = alarm.originator.id;
      const names = this.activeAlarmNames.get(deviceId) || [];
      const alarmName = alarm.type?.trim() || '未命名告警';
      if (!names.includes(alarmName)) {
        names.push(alarmName);
      }
      this.activeAlarmNames.set(deviceId, names);
    }
  }

  /** 依次重绘所有 ECharts 图表面板。 */
  private renderCharts(): void {
    if (!this.chartHosts) {
      return;
    }
    this.chartHosts.forEach((host) => {
      const chartId = host.nativeElement.dataset.chart;
      if (!this.isChartId(chartId)) {
        return;
      }
      this.renderChart(chartId, host.nativeElement);
    });
  }

  /** 按图表 ID 渲染单个图表，未注册容器时跳过。 */
  private renderChart(chartId: SmartLightningChartId, chartHost?: HTMLDivElement): void {
    const host = chartHost ?? this.chartHosts.find(item => item.nativeElement.dataset.chart === chartId)?.nativeElement;
    if (!host) {
      return;
    }
    const chart = this.getChart(chartId, host);
    switch (chartId) {
      case 'ground':
        chart.setOption(this.emptyChartOption(), true);
        break;
      case 'spd':
        chart.setOption(this.emptyChartOption(), true);
        break;
      case 'alarm':
        chart.setOption(this.alarmOption(), true);
        break;
      case 'orders':
        chart.setOption(this.emptyChartOption(), true);
        break;
      case 'lightning':
        chart.setOption(this.emptyChartOption(), true);
        break;
    }
  }

  private isChartId(value: string | undefined): value is SmartLightningChartId {
    return value === 'ground' || value === 'spd' || value === 'alarm' || value === 'orders' || value === 'lightning';
  }

  /** 获取或懒初始化指定容器的 ECharts 实例（SVG 渲染器）。 */
  private getChart(id: string, host: HTMLDivElement): ECharts {
    let chart = this.charts.get(id);
    if (!chart || chart.isDisposed()) {
      chart = echarts.init(host, undefined, {renderer: 'svg'});
      this.charts.set(id, chart);
    }
    return chart;
  }

  /** 未接入真实趋势数据时显示空态，避免把开发阶段样例值误认为设备数据。 */
  private emptyChartOption(): EChartsOption {
    return {
      title: {text: '暂无数据', left: 'center', top: 'middle', textStyle: {color: '#7890a4', fontSize: 12, fontWeight: 'normal'}},
      xAxis: {type: 'category', show: false, data: []},
      yAxis: {type: 'value', show: false},
      series: []
    };
  }

  /** 报警信息柱状图配置。 */
  private alarmOption(): EChartsOption {
    const alarmTypeTotals = this.activeAlarmTypeTotals;
    const emptyStateText = getSmartLightningAlarmChartMessage(
      this.alarmSummaryLoading,
      this.alarmSummaryLoadError,
      alarmTypeTotals
    );
    return {
      animationDuration: 380,
      title: {text: emptyStateText || '', left: 'center', top: 'middle', textStyle: {color: '#7890a4', fontSize: 12, fontWeight: 'normal'}},
      grid: {top: 20, right: 10, bottom: 25, left: 28},
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
      xAxis: {
        type: 'category', data: alarmTypeTotals.map((item) => item.type),
        axisLabel: {color: '#7890a4', fontSize: 10, rotate: 20, interval: 0}, axisLine: {lineStyle: {color: '#d7e0e9'}}, axisTick: {show: false}
      },
      yAxis: {type: 'value', minInterval: 1, splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: [{
        type: 'bar', data: alarmTypeTotals.map((item) => item.count), barMaxWidth: 28,
        itemStyle: {borderRadius: [5, 5, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: '#22e466'}, {offset: 1, color: '#073d5c'}
        ])}
      }]
    };
  }

}
