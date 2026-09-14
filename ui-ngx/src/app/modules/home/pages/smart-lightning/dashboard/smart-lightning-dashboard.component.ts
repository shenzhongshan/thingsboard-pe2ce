/** 文件职责：看板模块的 smart-lightning-dashboard.component，负责页面状态管理、接口调用与用户交互。 */
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  NgZone,
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
import { catchError, forkJoin, map, Observable, of, Subject, switchMap, takeUntil } from 'rxjs';
import { AlarmInfo, AlarmSearchStatus, AlarmStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope, AlarmCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
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
} from './smart-lightning-dashboard.data';
import { smartLightningTianDiTuMapConfig } from './smart-lightning-map.config';
import {
  buildSmartLightningMapPoints,
  filterSmartLightningMapDevicesWithoutPoints,
  SmartLightningMapPoint
} from './smart-lightning-map.data';
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
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  buildSmartLightningExtremaTooltip,
  buildSmartLightningMaximumTooltip,
  formatSmartLightningMetric
} from './smart-lightning-summary.data';
import {SMART_LIGHTNING_UNITS} from '../shared/telemetry/smart-lightning-measurements';

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

  @ViewChild('mapHost') mapHost!: ElementRef<HTMLDivElement>;
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
  /** 已从服务器属性加载并绘制的持久化点位，key 为设备 ID。 */
  private readonly persistedMapPoints = new Map<string, L.Marker>();
  private readonly destroy$ = new Subject<void>();
  /** 当前页面的 Leaflet 地图实例。 */
  private map?: L.Map;
  /** 用户点击地图后产生的临时点位，确认绑定前不会持久化。 */
  private draftMapPoint?: L.CircleMarker;
  /** 当前选点模式注册的地图点击监听器，退出模式时必须主动解除。 */
  private mapPointClickHandler?: (event: L.LeafletMouseEvent) => void;
  private resizeObserver?: ResizeObserver;
  private alarmRealtimeSubscriber?: TelemetrySubscriber;
  private realtimeAlarmCount: number | null = null;
  private initialAlarmLoadFinished = false;

  /** 控制地图是否处于用户选点模式；坐标需在绑定设备后才会持久化。 */
  isAddingMapPoint = false;
  /** 当前用户可见的防雷设备，供地图点位绑定下拉框使用。 */
  mapDevices: SmartLightningDeviceContext[] = [];
  /**
   * 地图点位绑定下拉框的候选设备。
   *
   * 过滤依据是已经绘制到地图上的设备点位 ID；已绑定设备不会再次出现在下拉框中，
   * 避免同一设备被重复绑定到多个点位。
   */
  get availableMapDevices(): SmartLightningDeviceContext[] {
    return filterSmartLightningMapDevicesWithoutPoints(
      this.mapDevices,
      new Set(this.persistedMapPoints.keys())
    );
  }
  /** 设备 ID 到根资产路径的索引，供地图弹窗显示绝对路径。 */
  private mapAssetPaths = new Map<string, string>();
  /** 当前未清除告警按设备聚合的名称，供地图弹窗和标记状态使用。 */
  private activeAlarmNames = new Map<string, string[]>();
  /** 告警接口失败时，弹窗显示“暂不可用”，避免把接口失败误判成无告警。 */
  private mapAlarmLoadFailed = false;
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
    private readonly telemetryWebsocketService: TelemetryWebsocketService,
    private readonly ngZone: NgZone
  ) {}

  ngAfterViewInit(): void {
    this.initializeMap();
    this.renderCharts();
    this.loadLatestAlarms();
    this.loadAlarmSummary();
    this.subscribeToAlarmCount();
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
    this.alarmRealtimeSubscriber?.unsubscribe();
    this.cancelMapPointMode();
    this.resizeObserver?.disconnect();
    this.charts.forEach(chart => chart.dispose());
    this.charts.clear();
    this.map?.remove();
  }

  /** 恢复地图上的添加点位入口，用户点击地图后生成一个临时点位。 */
  toggleMapPointMode(): void {
    if (!this.map) {
      return;
    }
    if (this.isAddingMapPoint) {
      this.cancelMapPointMode();
      this.cd.markForCheck();
      return;
    }
    this.isAddingMapPoint = true;
    this.map.getContainer().style.cursor = 'crosshair';
    this.mapPointClickHandler = (event: L.LeafletMouseEvent) => {
      this.cancelMapPointMode();
      this.draftMapPoint?.remove();
      this.pendingMapPoint = {
        latitude: event.latlng.lat,
        longitude: event.latlng.lng
      };
      this.selectedMapDeviceId = '';
      this.mapPointSaveError = '';
      this.draftMapPoint = L.circleMarker(event.latlng, {
        radius: 8,
        color: '#ffffff',
        weight: 3,
        fillColor: '#195ce8',
        fillOpacity: 1
      }).bindPopup('请选择设备并确认绑定').addTo(this.map!);
      this.cd.markForCheck();
    };
    this.map.on('click', this.mapPointClickHandler);
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
    if (!selectedDevice || this.persistedMapPoints.has(selectedDevice.deviceId)) {
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
        this.draftMapPoint?.remove();
        this.draftMapPoint = undefined;
        // 清理临时图层后重新生成正式图层，保证当前页面和重新进入页面表现一致。
        this.renderPersistedMapPoints(this.mapDevices);
        this.persistedMapPoints.get(this.selectedMapDeviceId)?.openPopup();
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
    this.draftMapPoint?.remove();
    this.draftMapPoint = undefined;
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
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    this.mapAlarmLoadFailed = false;
    this.alarmService.getAllAlarmsV2(createSmartLightningAlarmQuery()).pipe(
      map((pageData): AlarmInfo[] => {
        const sortedAlarms = pageData.data.map((alarm) => ({info: alarm, row: formatSmartLightningAlarm(alarm)}))
          .sort((left, right) => compareSmartLightningAlarms(left.row, right.row))
        // 地图告警状态必须基于全部当前告警，不能受表格只显示 10 条的限制。
        this.updateMapAlarmDetails(sortedAlarms.map(({info}) => info));
        return sortedAlarms.slice(0, SMART_LIGHTNING_ALARM_DISPLAY_LIMIT).map(({info}) => info);
      }),
      switchMap((alarms) => this.resolveAlarmRows(alarms)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (alarms) => {
        this.alarms = alarms;
        this.alarmsLoading = false;
        this.initialAlarmLoadFinished = true;
        this.renderChart('alarm');
        // 告警接口和设备接口是并行请求，告警先返回时也要刷新已经绘制的点位弹窗。
        this.renderPersistedMapPoints(this.mapDevices);
        this.cd.markForCheck();
        if (this.realtimeAlarmCount !== null && this.realtimeAlarmCount !== this.activeAlarmCount) {
          this.loadLatestAlarms();
        }
      },
      error: () => {
        this.alarms = [];
        this.activeAlarmNames.clear();
        this.mapAlarmLoadFailed = true;
        this.activeAlarmCount = null;
        this.activeAlarmTypeTotals = [];
        this.alarmsLoadError = true;
        this.alarmsLoading = false;
        this.initialAlarmLoadFinished = true;
        this.renderChart('alarm');
        this.renderPersistedMapPoints(this.mapDevices);
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
   * 订阅服务端活动告警计数。计数更新仅作为轻量失效信号；
   * 告警行和类型聚合仍以现有 REST 加载器为唯一数据来源。
   */
  private subscribeToAlarmCount(): void {
    const command = new AlarmCountCmd();
    command.query = {
      entityFilter: {
        type: AliasFilterType.entityType,
        entityType: EntityType.DEVICE
      },
      statusList: [AlarmSearchStatus.ACTIVE]
    };

    const subscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    subscriber.subscriptionCommands.push(command);
    this.alarmRealtimeSubscriber = subscriber;
    subscriber.alarmCount$.pipe(takeUntil(this.destroy$)).subscribe((update) => {
      const previousCount = this.realtimeAlarmCount;
      this.realtimeAlarmCount = update.count;
      if (this.initialAlarmLoadFinished && shouldReloadSmartLightningAlarms(previousCount, update.count)) {
        this.loadLatestAlarms();
        this.loadAlarmSummary();
      }
    });
    subscriber.subscribe();
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
        this.renderPersistedMapPoints(pageData.devices.data);
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

  /** 初始化天地图底图；设备点位在设备上下文接口返回后单独绘制。 */
  private initializeMap(): void {
    const {center, zoom, token} = smartLightningTianDiTuMapConfig;
    this.map = L.map(this.mapHost.nativeElement, {attributionControl: true, zoomControl: true}).setView(center, zoom);
    const tileOptions: L.TileLayerOptions = {
      subdomains: '01234567',
      maxNativeZoom: 18,
      maxZoom: 21,
      attribution: '&copy; 天地图 Tianditu'
    };
    L.tileLayer(
      'https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + token,
      tileOptions
    ).addTo(this.map);
    L.tileLayer(
      'https://t{s}.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + token,
      tileOptions
    ).addTo(this.map);
  }

  /**
   * 根据后端批量返回的服务器属性绘制设备点位。
   * 每次刷新先清理旧图层，避免设备坐标更新后出现重复标记。
   */
  private renderPersistedMapPoints(devices: SmartLightningDeviceContext[]): void {
    this.persistedMapPoints.forEach((marker) => marker.remove());
    this.persistedMapPoints.clear();
    if (!this.map) {
      return;
    }
    const points: SmartLightningMapPoint[] = buildSmartLightningMapPoints(devices);
    const devicesById = new Map<string, SmartLightningDeviceContext>();
    for (const device of devices) {
      devicesById.set(device.deviceId, device);
    }
    for (const point of points) {
      const device = devicesById.get(point.deviceId);
      const status = this.getMapDeviceStatus(device);
      // 每台设备只维护一个正式点位，设备重新选点后旧图层会在方法开头被清除。
      const marker = L.marker([point.latitude, point.longitude], {
        icon: this.buildMapMarkerIcon(device)
      }).bindPopup(
        this.buildMapPopupContent(point, device),
        {className: `smart-lightning-map-popup-container smart-lightning-map-popup-${status}`}
      ).addTo(this.map);
      // 直接覆盖 Leaflet 运行时生成的外层节点，确保整个弹窗面板而非仅内容文字区域变色。
      marker.on('popupopen', () => this.applyMapPopupBackground(marker, status));
      this.persistedMapPoints.set(point.deviceId, marker);
    }
  }

  /**
   * 生成地图点位详情弹窗：设备名称、产品名称、绝对资产路径和告警情况。
   * 所有文本均经过 HTML 转义，避免设备或资产名称中的特殊字符破坏弹窗结构。
   */
  private buildMapPopupContent(point: SmartLightningMapPoint, device?: SmartLightningDeviceContext): string {
    const productName = point.productName || '未配置';
    const assetPath = this.mapAssetPaths.get(point.deviceId) || point.deviceName;
    const alarmNames = this.activeAlarmNames.get(point.deviceId) || [];
    const status = this.getMapDeviceStatus(device);
    const statusColor = this.getMapStatusColor(status);
    const popupBackground = this.getMapPopupBackground(status);
    const onlineText = device?.active === true ? '在线' : '离线';
    const alarmText = this.mapAlarmLoadFailed
      ? '暂无法获取'
      : alarmNames.length > 0 ? alarmNames.join('/') : '无告警';
    const alarmStyle = alarmNames.length > 0 && !this.mapAlarmLoadFailed
      ? ' style="color:#d32f2f;font-weight:600"'
      : '';
    return `<div class="smart-lightning-map-popup" style="min-width:220px;padding:10px 12px;line-height:1.8;background:${popupBackground}">` +
      `<strong>设备名称：${this.escapeMapPopupText(point.deviceName)}</strong><br>` +
      `产品名称：${this.escapeMapPopupText(productName)}<br>` +
      `绝对路径：${this.escapeMapPopupText(assetPath)}<br>` +
      `<span style="color:${statusColor};font-weight:600">在线状态：${onlineText}</span><br>` +
      `<span${alarmStyle}>告警情况：${this.escapeMapPopupText(alarmText)}</span><br>` +
      `纬度：${point.latitude.toFixed(6)}<br>经度：${point.longitude.toFixed(6)}` +
      `</div>`;
  }

  /** 将当前未清除告警按设备 ID 聚合告警名称，避免地图弹窗逐设备发起告警请求。 */
  private updateMapAlarmDetails(alarms: AlarmInfo[]): void {
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

  /**
   * 根据设备当前状态创建水滴形地图标记。
   * 状态优先级固定为：有未清除告警（红）> 离线（灰）> 在线（绿）。
   */
  private buildMapMarkerIcon(device?: SmartLightningDeviceContext): L.DivIcon {
    const color = this.getMapStatusColor(this.getMapDeviceStatus(device));
    const svg = `<svg width="30" height="38" viewBox="0 0 30 38" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">` +
      `<path d="M15 37C13.6 35.1 3 22.4 3 14.3C3 7.9 8.4 2.5 15 2.5S27 7.9 27 14.3C27 22.4 16.4 35.1 15 37Z" fill="${color}" stroke="#ffffff" stroke-width="2"/>` +
      `<circle cx="15" cy="14" r="5" fill="#ffffff" fill-opacity=".9"/>` +
      `</svg>`;
    return L.divIcon({
      className: 'smart-lightning-map-pin',
      html: svg,
      iconSize: [30, 38],
      iconAnchor: [15, 37],
      popupAnchor: [0, -34]
    });
  }

  /** 返回地图设备状态；告警优先于在线和离线状态。 */
  private getMapDeviceStatus(device?: SmartLightningDeviceContext): 'alarm' | 'offline' | 'online' {
    const alarmNames = device ? this.activeAlarmNames.get(device.deviceId) || [] : [];
    if (alarmNames.length > 0) {
      return 'alarm';
    }
    return device?.active === true ? 'online' : 'offline';
  }

  /** 地图标记和弹窗共用同一套状态颜色，避免两处颜色判断不一致。 */
  private getMapStatusColor(status: 'alarm' | 'offline' | 'online'): string {
    if (status === 'alarm') {
      return '#e53935';
    }
    if (status === 'offline') {
      return '#8b95a5';
    }
    return '#16a34a';
  }

  /** 返回弹窗面板使用的浅色背景；状态颜色由整个 Leaflet 外层统一覆盖。 */
  private getMapPopupBackground(status: 'alarm' | 'offline' | 'online'): string {
    if (status === 'alarm') {
      return '#fee2e2';
    }
    if (status === 'offline') {
      return '#e5e7eb';
    }
    return '#dcfce7';
  }

  /**
   * Leaflet 打开弹窗后才会创建外层 DOM，因此在 popupopen 事件中设置三层背景：
   * 外层面板、内容容器和底部箭头，避免默认白色样式覆盖状态色。
   */
  private applyMapPopupBackground(
    marker: L.Marker,
    status: 'alarm' | 'offline' | 'online'
  ): void {
    const popupElement = marker.getPopup()?.getElement();
    if (!popupElement) {
      return;
    }
    const background = this.getMapPopupBackground(status);
    const wrapper = popupElement.querySelector('.leaflet-popup-content-wrapper') as HTMLElement | null;
    const content = popupElement.querySelector('.leaflet-popup-content') as HTMLElement | null;
    const tip = popupElement.querySelector('.leaflet-popup-tip') as HTMLElement | null;
    if (wrapper) {
      wrapper.style.setProperty('background-color', background, 'important');
    }
    if (content) {
      content.style.setProperty('background-color', background, 'important');
    }
    if (tip) {
      tip.style.setProperty('background-color', background, 'important');
    }
  }

  /** 地图弹窗使用文本内容，避免设备名称中的特殊字符被当作 HTML 解析。 */
  private escapeMapPopupText(value: string): string {
    return value.replace(/[&<>"']/g, (character) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[character] || character));
  }

  /** 退出选点模式并解除地图点击监听，避免重复绑定造成一次点击生成多个点位。 */
  private cancelMapPointMode(): void {
    if (this.map && this.mapPointClickHandler) {
      this.map.off('click', this.mapPointClickHandler);
    }
    this.mapPointClickHandler = undefined;
    this.isAddingMapPoint = false;
    if (this.map) {
      this.map.getContainer().style.cursor = '';
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
