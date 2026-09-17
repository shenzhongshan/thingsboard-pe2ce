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
import { auditTime, catchError, forkJoin, map, Observable, of, Subject, Subscription, switchMap, takeUntil, timer } from 'rxjs';
import { AlarmInfo, AlarmStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { MAX_SAFE_PAGE_SIZE } from '@shared/models/page/page-link';
import {
  buildSmartLightningMock,
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
  SmartLightningMock,
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
import { filterSmartLightningMapDevicesWithoutPoints } from './smart-lightning-map.data';
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
import { SmartLightningRealtimeService } from '../shared/realtime/smart-lightning-realtime.service';
import {
  buildSmartLightningExtremaTooltip,
  buildSmartLightningMaximumTooltip,
  formatSmartLightningMetric
} from './smart-lightning-summary.data';
import {SMART_LIGHTNING_UNITS} from '../shared/telemetry/smart-lightning-measurements';
import {getSmartLightningChangedSummaryTargets} from './smart-lightning-realtime-refresh';
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
 * 设备数量、地网汇总、告警和地图点位接入真实接口；部分曲线仍保留演示数据。
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

  data: SmartLightningMock = buildSmartLightningMock(SmartLightningPeriod.day);
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

  /** 点击公共地图中的设备时不改变筛选，只保留统一弹窗交互。 */
  onDeviceMapClick(_device: SmartLightningMapDevice): void {
    // 设备详情仍由地图弹窗负责展示，页面无需重复发起请求。
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

  /** 地网报警率展示格式；接口按告警设备去重计算，空值显示占位符。 */
  formatGroundAlarmRate(value: number | null | undefined): string {
    return value === null || value === undefined || !Number.isFinite(value) ? '--' : `${value.toFixed(2)}%`;
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

  /** SPD 卡片 DIV 中的周期数值只显示一位小数；悬浮详情仍保留原有两位精度。 */
  formatSpdPeriodicValue(value: number | null | undefined, unit: string): string {
    return formatSmartLightningMetric(value, unit, 1);
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
   * 监听共享实时服务的失效通知。
   * WebSocket 只负责通知变化，页面仍复用原有 HTTP 聚合接口读取完整数据，
   * 因此不会改变当前用户权限、资产过滤和统计口径。
   */
  private subscribeToRealtimeChanges(): void {
    this.realtimeService.deviceChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe((changedDeviceIds) => {
      // 先使用当前设备上下文确定受影响分类，再刷新设备总览。
      // 避免一台 SPD 设备上报数据时，同时重算地网和雷电流的整段周期数据。
      const changedTargets = getSmartLightningChangedSummaryTargets(this.mapDevices, changedDeviceIds);
      this.loadDeviceSummary();
      for (const target of changedTargets) {
        if (target === 'ground') {
          this.loadGroundSummary();
        } else if (target === 'spd') {
          this.loadSpdSummary();
        } else {
          this.loadLightningSummary();
        }
      }
    });

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
      // 地网、SPD、雷电卡片包含周期内告警/设备汇总，活动告警变化后同步刷新。
      this.loadGroundSummary();
      this.loadSpdSummary();
      this.loadLightningSummary();
    });

    this.realtimeService.acquire();
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
        const activeValues: unknown[] = [];
        for (const device of pageData.devices.data) {
          activeValues.push(device.active);
        }
        return activeValues;
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (activeValues) => {
        this.deviceSummary = calculateSmartLightningDeviceSummary(activeValues);
        this.devicesLoading = false;
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
    const data = buildSmartLightningMock(this.chartPeriods[chartId]);
    switch (chartId) {
      case 'ground':
        chart.setOption(this.lineOption(data, data.groundSeries, SMART_LIGHTNING_UNITS.resistance));
        break;
      case 'spd':
        chart.setOption(this.lineOption(data, data.spdSeries, SMART_LIGHTNING_UNITS.leakageCurrent));
        break;
      case 'alarm':
        chart.setOption(this.alarmOption());
        break;
      case 'orders':
        chart.setOption(this.orderOption(data));
        break;
      case 'lightning':
        chart.setOption(this.lightningOption(data));
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

  /** 折线图通用配置：平滑曲线 + 统一坐标轴样式。 */
  private lineOption(data: SmartLightningMock, series: SmartLightningMock['spdSeries'], unit: string): EChartsOption {
    return {
      animationDuration: 380,
      animationEasing: 'cubicOut',
      tooltip: {trigger: 'axis', confine: true, borderColor: '#dbe5ef', textStyle: {color: '#183653'}},
      legend: {top: 0, right: 0, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 11, color: '#5f7488'}},
      grid: {top: 34, right: 10, bottom: 24, left: 32, containLabel: true},
      xAxis: {
        type: 'category', boundaryGap: false, data: data.axisLabels,
        axisLine: {lineStyle: {color: '#d7e0e9'}},
        axisLabel: {color: '#7890a4', fontSize: 10}, axisTick: {show: false}
      },
      yAxis: {
        type: 'value', name: unit, nameTextStyle: {color: '#7890a4', fontSize: 10},
        splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}
      },
      series: series.map(item => ({
        name: item.name, type: 'line', smooth: true, showSymbol: false, data: item.data,
        lineStyle: {width: 2, color: item.color},
        areaStyle: {color: item.color, opacity: 0.06},
        emphasis: {focus: 'series'}
      }))
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

  /** 运维工单柱状图配置。 */
  private orderOption(data: SmartLightningMock): EChartsOption {
    return {
      animationDuration: 380,
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
      legend: {top: 0, right: 0, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 11, color: '#5f7488'}},
      grid: {top: 34, right: 8, bottom: 25, left: 28},
      xAxis: {type: 'category', data: data.axisLabels, axisLabel: {color: '#7890a4', fontSize: 10}, axisTick: {show: false}},
      yAxis: {type: 'value', splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: data.workOrderSeries.map(item => ({
        name: item.name, type: 'bar', data: item.data, barMaxWidth: 12,
        itemStyle: {color: item.color, borderRadius: [3, 3, 0, 0]}
      }))
    };
  }

  /** 雷电监测横向条形图配置。 */
  private lightningOption(data: SmartLightningMock): EChartsOption {
    return {
      animationDuration: 380,
      grid: {top: 10, right: 16, bottom: 8, left: 72},
      xAxis: {type: 'value', max: 100, show: false},
      yAxis: {
        type: 'category', inverse: true, data: data.lightningMetrics.map(item => item.name),
        axisTick: {show: false}, axisLine: {show: false}, axisLabel: {color: '#526a80', fontSize: 11}
      },
      series: [{
        type: 'bar', data: data.lightningMetrics.map(item => item.value), barWidth: 18,
        showBackground: true, backgroundStyle: {color: '#edf2f6', borderRadius: 9},
        itemStyle: {borderRadius: 9, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          {offset: 0, color: '#0a4965'}, {offset: 1, color: '#20e66b'}
        ])}, label: {show: true, position: 'right', color: '#567087', fontSize: 10, formatter: '{c}'}
      }]
    };
  }
}
