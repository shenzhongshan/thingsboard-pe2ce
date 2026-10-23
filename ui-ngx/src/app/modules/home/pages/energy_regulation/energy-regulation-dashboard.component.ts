/** 文件职责：能源监管总览看板，组织设备接口、真实用能与告警数据和本地图表生命周期。 */
import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {AlarmQueryV2, AlarmSearchStatus, AlarmSeverity} from '@shared/models/alarm.models';
import {EntityType} from '@shared/models/entity-type.models';
import {MAX_SAFE_PAGE_SIZE, TimePageLink} from '@shared/models/page/page-link';
import {auditTime, bufferTime, catchError, filter, forkJoin, merge, of, Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {EnergyRegulationApiService} from './energy-regulation-api.service';
import type {
  EnergyRegulationAssetTreeNode,
  EnergyRegulationDeviceContext,
  EnergyRegulationEnergyConsumption
} from './energy-regulation-api.service';
import type {SmartLightningMapDevice} from '../smart-lightning/shared/map/smart-lightning-device-map.models';
import {resolveSmartLightningMapCoordinates} from '../smart-lightning/shared/map/smart-lightning-device-map.data';
import {
  SmartLightningDeviceTelemetryChange,
  SmartLightningRealtimeService
} from '../smart-lightning/shared/realtime/smart-lightning-realtime.service';
import {
  buildEnergyRegulationDeviceOverview,
  EnergyRegulationDeviceProfileCount,
  EnergyRegulationDeviceSummary,
  EnergyRegulationChartId,
  EnergyRegulationPeriod,
  EnergyRegulationPeriods,
  INITIAL_ENERGY_REGULATION_PERIODS,
  getLiveEnergyConsumptionForPeriod,
  setEnergyRegulationPeriod
} from './energy-regulation-dashboard.data';

@Component({
  selector: 'tb-energy-regulation-dashboard',
  templateUrl: './energy-regulation-dashboard.component.html',
  styleUrls: ['./energy-regulation-dashboard.component.scss'],
  standalone: false
})
export class EnergyRegulationDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  /** 图表宿主在模板建立后由 ECharts 生命周期统一管理。 */
  @ViewChildren('chartHost') chartHosts!: QueryList<ElementRef<HTMLDivElement>>;

  /** 能源监管接口返回的实际告警，用于列表和独立周期统计。 */
  alarms: import('@shared/models/alarm.models').AlarmInfo[] = [];
  /** 能源监管地图仅显示现有设备上下文中具有有效遥测或服务器属性坐标的设备。 */
  mapDevices: SmartLightningMapDevice[] = [];
  /** 地图坐标和设备信息沿用能源监管模块已有接口，不依赖防雷或电气安全数据。 */
  private energyDeviceContexts: EnergyRegulationDeviceContext[] = [];
  /** 当前活动告警设备集合，只用于地图点位的告警状态标记。 */
  private alarmedDeviceIds = new Set<string>();
  /** 设备 ID 到完整资产树路径的索引，供地图弹窗展示设备归属。 */
  private readonly mapAssetPaths = new Map<string, string>();
  /** 地图坐标请求失败时清空点位，避免把旧点位误当成当前设备结果。 */
  mapLoadError = false;
  deviceSummary: EnergyRegulationDeviceSummary = {total: 0, online: 0, offline: 0, alarming: 0};
  deviceProfiles: EnergyRegulationDeviceProfileCount[] = [];
  deviceOverviewLoading = true;
  deviceOverviewError = false;
  energyConsumption: EnergyRegulationEnergyConsumption | null = null;
  energyConsumptionLoading = true;
  energyConsumptionError = false;
  readonly periodOptions: EnergyRegulationPeriod[] = ['day', 'week', 'month'];
  readonly math = Math;
  periods: EnergyRegulationPeriods = {...INITIAL_ENERGY_REGULATION_PERIODS};
  private readonly charts = new Map<string, ECharts>();
  private readonly destroy$ = new Subject<void>();
  private resizeObserver?: ResizeObserver;
  /** 只接受最近一次看板快照请求，避免设备新增后旧响应覆盖新点位。 */
  private deviceOverviewRequestId = 0;
  private energyRequestId = 0;

  constructor(
    private readonly energyRegulationApiService: EnergyRegulationApiService,
    private readonly realtimeService: SmartLightningRealtimeService
  ) {}

  ngOnInit(): void {
    this.subscribeToMapTelemetry();
    this.realtimeService.acquire();
    this.loadDeviceOverview();
    this.loadEnergyConsumption();
  }

  /** 周期只影响当前图表，图表之间不共享筛选状态。 */
  selectPeriod(chartId: EnergyRegulationChartId, period: EnergyRegulationPeriod): void {
    this.periods = setEnergyRegulationPeriod(this.periods, chartId, period);
    if (chartId !== 'consumption') {
      this.renderChart(chartId);
    }
  }

  /** 用能卡的数据由独立周期选择器读取，不影响趋势图周期。 */
  get selectedConsumption() {
    return this.energyConsumption
      ? getLiveEnergyConsumptionForPeriod(this.energyConsumption, this.periods.consumption)
      : null;
  }

  /** 今日真实用电量；没有有效表底差时由页面显示为横线。 */
  get todayConsumptionKwh(): number | null {
    return this.energyConsumption?.energyConsumptionByPeriod.day.valueKwh ?? null;
  }

  /** 当前用户可读电表累计表底之和。 */
  get totalConsumptionKwh(): number | null {
    return this.energyConsumption?.totalKwh ?? null;
  }

  /** 当前用能卡周期值，缺少边界读数时返回 null。 */
  get selectedConsumptionValueKwh(): number | null {
    return this.selectedConsumption?.valueKwh ?? null;
  }

  /** 当前用能卡对比值，缺少上一周期数据时返回 null。 */
  get selectedPreviousConsumptionKwh(): number | null {
    return this.selectedConsumption?.previousValueKwh ?? null;
  }

  /** 当前周期变化率；只有两期数据完整且上一期非零时才展示。 */
  get selectedConsumptionChangePercent(): number | null {
    return this.selectedConsumption?.changePercent ?? null;
  }

  /** 趋势图缺少有效读数时只显示占位符，避免把缺数显示成零值。 */
  get energyChartMessage(): string {
    if (this.energyConsumptionLoading) {
      return '…';
    }
    if (this.energyConsumptionError) {
      return '--';
    }
    if (!this.energyConsumption || this.energyConsumption.meterCount === 0) {
      return '--';
    }
    const values = this.energyConsumption.energySeries[this.periods.energy]?.values || [];
    return values.some((value) => value !== null) ? '' : '--';
  }

  /** 当日告警条数与活动重点告警分开统计，已清除不等同于已确认。 */
  get alarmSummary() {
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const alarms = this.alarms.filter((alarm) => alarm.createdTime >= today.getTime());
    const cleared = alarms.filter((alarm) => alarm.cleared).length;
    return {todayTotal: alarms.length, todayProcessed: cleared,
      urgentActive: this.alarms.filter((alarm) => !alarm.cleared && ['CRITICAL','MAJOR'].includes(alarm.severity)).length,
      resolutionRate: alarms.length ? Math.round(cleared / alarms.length * 100) : null};
  }

  /** 实际告警记录使用本模块设备与资产路径补充显示，不生成演示事件。 */
  get alarmSamples() {
    const devices = new Map(this.energyDeviceContexts.map((device) => [device.deviceId, device]));
    const severity: Record<string, string> = {CRITICAL: '严重', MAJOR: '重要', MINOR: '次要', WARNING: '警告', INDETERMINATE: '不确定'};
    return this.alarms.slice().sort((a, b) => b.createdTime - a.createdTime).slice(0, 10).map((alarm) => ({
      deviceName: devices.get(alarm.originator.id)?.deviceName || alarm.originatorName || '__',
      productName: devices.get(alarm.originator.id)?.productName || '__', location: this.mapAssetPaths.get(alarm.originator.id) || '__',
      severity: severity[alarm.severity] || alarm.severity, message: alarm.type,
      occurredAt: new Date(alarm.createdTime).toLocaleString()}));
  }

  /** 每张趋势图独立按上海自然日、周、月分桶，三组互斥状态相加等于事件条数。 */
  private realAlarmSeries(period: EnergyRegulationPeriod) {
    const start = new Date(); start.setHours(0, 0, 0, 0);
    if (period === 'week') { start.setDate(start.getDate() - (start.getDay() + 6) % 7); }
    if (period === 'month') { start.setDate(1); }
    const count = period === 'day' ? 24 : period === 'week' ? 7 : new Date(start.getFullYear(), start.getMonth() + 1, 0).getDate();
    const result = {labels: Array.from({length: count}, (_, i) => period === 'day' ? `${i}:00` : `${i + 1}日`),
      pending: Array(count).fill(0) as number[], assigned: Array(count).fill(0) as number[], processing: Array(count).fill(0) as number[]};
    for (const alarm of this.alarms) {
      const index = Math.floor((alarm.createdTime - start.getTime()) / (period === 'day' ? 3_600_000 : 86_400_000));
      if (index < 0 || index >= count) { continue; }
      const target = alarm.cleared ? result.processing : alarm.acknowledged ? result.assigned : result.pending;
      target[index]++;
    }
    return result;
  }

  /** 为仅展示在线、离线状态的环形图及辅助技术提供摘要。 */
  deviceDonutLabel(): string {
    if (this.deviceOverviewLoading) {
      return '能源监管设备概况加载中';
    }
    if (this.deviceOverviewError) {
      return '能源监管设备概况加载失败';
    }
    const summary = this.deviceSummary;
    return `设备总数 ${summary.total} 台；设备状态占比：在线 ${summary.online} 台，离线 ${summary.offline} 台`;
  }

  /** 将周期枚举转换成界面中文文案。 */
  periodLabel(period: EnergyRegulationPeriod): string {
    return period === 'day' ? '日' : period === 'week' ? '周' : '月';
  }

  /** 将告警级别映射为样式类，不修改告警数据。 */
  severityClass(severity: string): string {
    return severity === '严重' ? 'critical' : severity === '紧急' ? 'urgent' : 'general';
  }

  ngAfterViewInit(): void {
    this.chartHosts.forEach((host) => {
      const chartId = host.nativeElement.dataset.chart;
      if (chartId) {
        this.getChart(chartId, host.nativeElement);
        this.renderChart(chartId);
      }
    });
    this.resizeObserver = new ResizeObserver(() => this.charts.forEach((chart) => chart.resize()));
    this.chartHosts.forEach((host) => this.resizeObserver?.observe(host.nativeElement));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtimeService.release();
    this.resizeObserver?.disconnect();
    this.charts.forEach((chart) => chart.dispose());
    this.charts.clear();
  }

  /** 复用能源监管模块现有设备、资产树和告警接口；无设备时地图保持空白。 */
  private loadDeviceOverview(): void {
    const requestId = ++this.deviceOverviewRequestId;
    const alarmsQuery = new AlarmQueryV2(null, new TimePageLink(MAX_SAFE_PAGE_SIZE, 0), {
      statusList: [AlarmSearchStatus.ACTIVE],
      severityList: Object.values(AlarmSeverity)
    });
    forkJoin({
      devices: this.energyRegulationApiService.getDeviceContexts(),
      alarms: this.energyRegulationApiService.getAllAlarms(),
      // 资产树只为地图弹窗提供归属路径；树请求失败不影响设备统计和坐标点位。
      assetTree: this.energyRegulationApiService.getAssetTree().pipe(catchError(() => of(null)))
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({devices, alarms, assetTree}) => {
        if (requestId !== this.deviceOverviewRequestId) {
          return;
        }
        const energyDeviceIds = new Set(devices.map((device) => device.deviceId));
        const alarmedDeviceIds = new Set<string>();
        this.deviceOverviewError = false;
        this.alarms = alarms;
        for (const alarm of alarms) {
          if (!alarm.cleared && alarm.originator?.entityType === EntityType.DEVICE
              && alarm.originator.id && energyDeviceIds.has(alarm.originator.id)) {
            alarmedDeviceIds.add(alarm.originator.id);
          }
        }
        this.energyDeviceContexts = devices;
        this.alarmedDeviceIds = alarmedDeviceIds;
        this.mapAssetPaths.clear();
        if (assetTree) {
          buildEnergyRegulationAssetPathIndex(assetTree.roots, this.mapAssetPaths);
        }
        this.mapLoadError = false;
        this.refreshMapDevices();
        const overview = buildEnergyRegulationDeviceOverview(devices, alarmedDeviceIds);
        this.deviceSummary = overview.summary;
        this.deviceProfiles = overview.profiles;
        this.deviceOverviewLoading = false;
        this.renderChart('devices');
        this.renderChart('alarms');
      },
      error: () => {
        if (requestId !== this.deviceOverviewRequestId) {
          return;
        }
        this.alarms = [];
        this.energyDeviceContexts = [];
        this.renderChart('alarms');
        this.mapDevices = [];
        this.mapLoadError = true;
        this.deviceOverviewLoading = false;
        this.deviceOverviewError = true;
      }
    });
  }

  /** 复用防雷共享 WebSocket 缓存：GPS 坐标和在线状态更新只刷新当前能源设备地图。 */
  private subscribeToMapTelemetry(): void {
    this.realtimeService.deviceTelemetryChanged$.pipe(
      bufferTime(100),
      filter((changes) => changes.length > 0),
      takeUntil(this.destroy$)
    ).subscribe((changes) => {
      const energyDeviceIds = new Set(this.energyDeviceContexts.map((device) => device.deviceId));
      const mapStateChanged = changes.some((change) => energyDeviceIds.has(change.deviceId) && (
        change.values.has('active') || change.values.has('Latitude') || change.values.has('Longitude')
      ));
      if (mapStateChanged) {
        this.refreshMapDevices();
        const contexts = this.energyDeviceContexts.map((device) => ({...device,
          active: parseEnergyRegulationActiveValue(this.realtimeService.getLatestTelemetryValue(device.deviceId, 'active')?.value) ?? device.active}));
        const overview = buildEnergyRegulationDeviceOverview(contexts, this.alarmedDeviceIds);
        this.deviceSummary = overview.summary; this.renderChart('devices');
      }
    });

    // 能源业务统计在模块内刷新；共享服务只负责传递变更，不承担能源计算。
    this.realtimeService.deviceTelemetryChanged$.pipe(
      filter((change) => this.energyDeviceContexts.some((device) => device.deviceId === change.deviceId)),
      auditTime(5000), takeUntil(this.destroy$))
      .subscribe(() => this.loadEnergyConsumption());
    // 新设备、资产归属或服务器坐标变化后，重新读取模块现有快照接口以发现新增能源设备。
    merge(
      this.realtimeService.deviceMembershipChanged$,
      this.realtimeService.deviceContextIncrementalChanged$,
      this.realtimeService.assetMembershipChanged$,
      this.realtimeService.alarmChanged$
    ).pipe(
      auditTime(400),
      takeUntil(this.destroy$)
    ).subscribe(() => this.loadDeviceOverview());
  }

  /** 遥测坐标优先，完整遥测坐标无效时回退服务器属性；无坐标设备不创建地图标记。 */
  private refreshMapDevices(): void {
    this.mapDevices = [];
    for (const context of this.energyDeviceContexts) {
      const coordinates = resolveSmartLightningMapCoordinates(
        context.latitude,
        context.longitude,
        this.realtimeService.getLatestTelemetryValue(context.deviceId, 'Latitude')?.value,
        this.realtimeService.getLatestTelemetryValue(context.deviceId, 'Longitude')?.value
      );
      if (!coordinates) {
        continue;
      }
      const activeValue = this.realtimeService.getLatestTelemetryValue(context.deviceId, 'active')?.value;
      this.mapDevices.push({
        id: context.deviceId,
        name: context.deviceName,
        productName: context.productName || '--',
        profileName: context.deviceProfileName || '--',
        productCategory: context.productCategory,
        active: parseEnergyRegulationActiveValue(activeValue) ?? (context.active === true),
        latitude: coordinates.latitude,
        longitude: coordinates.longitude,
        assetPath: this.mapAssetPaths.get(context.deviceId) || '--',
        alarmNames: this.alarmedDeviceIds.has(context.deviceId) ? ['存在未清除告警'] : []
      });
    }
  }

  /** 读取标准累计电能接口；失败时保留错误状态，不回退到演示用电量。 */
  private loadEnergyConsumption(): void {
    const request = ++this.energyRequestId;
    this.energyRegulationApiService.getEnergyConsumption().pipe(takeUntil(this.destroy$)).subscribe({
      next: (response) => {
        if (request !== this.energyRequestId) { return; }
        this.energyConsumptionError = false;
        this.energyConsumption = response;
        this.energyConsumptionLoading = false;
        this.renderChart('energy');
      },
      error: () => {
        if (request !== this.energyRequestId) { return; }
        this.energyConsumption = null;
        this.energyConsumptionLoading = false;
        this.energyConsumptionError = true;
        this.renderChart('energy');
      }
    });
  }

  /** 按图表类型生成真实数据配置；未配置排放系数时碳排图保持空。 */
  private renderChart(chartId: string): void {
    const chart = this.charts.get(chartId);
    if (!chart || chart.isDisposed()) {
      return;
    }
    const option = chartId === 'devices'
      ? this.deviceChartOption()
      : chartId === 'alarms'
        ? this.alarmChartOption(this.periods.alarms)
        : chartId === 'energy'
          ? this.energyChartOption(this.periods.energy)
          : this.carbonChartOption(this.periods.carbon);
    chart.setOption(option, true);
  }

  /** 获取或初始化指定容器的 SVG 图表实例，避免重复绑定同一 DOM。 */
  private getChart(id: string, host: HTMLDivElement): ECharts {
    let chart = this.charts.get(id);
    if (!chart || chart.isDisposed()) {
      chart = echarts.init(host, undefined, {renderer: 'svg'});
      this.charts.set(id, chart);
    }
    return chart;
  }

  /** 绘制真实用电趋势，缺失的时间桶保留为 null 以显示数据间断。 */
  private energyChartOption(period: EnergyRegulationPeriod): EChartsOption {
    const series = this.energyConsumption?.energySeries[period];
    return {
      animationDuration: 320,
      grid: {top: 18, right: 16, bottom: 30, left: 44},
      tooltip: {trigger: 'axis', valueFormatter: (value: unknown) => value === null || value === undefined ? '--' : `${value} kWh`},
      xAxis: {type: 'category', data: series?.labels ?? [], boundaryGap: false, axisLabel: {color: '#7890a4', fontSize: 10}, axisLine: {lineStyle: {color: '#dce5ee'}}, axisTick: {show: false}},
      yAxis: {type: 'value', min: 0, splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: [{type: 'line', data: series?.values ?? [], smooth: true, connectNulls: false, symbol: 'circle', symbolSize: 5,
        lineStyle: {width: 2, color: '#3989f7'}, itemStyle: {color: '#3989f7', borderColor: '#fff', borderWidth: 1},
        areaStyle: {color: 'rgba(57, 137, 247, 0.12)'}}]
    };
  }

  /** 缺少排放系数时返回空序列，不把用电量伪装为碳排放。 */
  private carbonChartOption(period: EnergyRegulationPeriod): EChartsOption {
    const series = {labels: [] as string[], values: [] as number[]};
    return {
      graphic: [{type: 'text', left: 'center', top: 'middle', style: {text: '__', fill: '#71869a'}}],
      animationDuration: 320,
      grid: {top: 18, right: 16, bottom: 30, left: 44},
      tooltip: {trigger: 'axis', valueFormatter: (value: unknown) => `${value ?? '--'} kgCO₂`},
      xAxis: {type: 'category', data: series.labels, boundaryGap: true, axisLabel: {color: '#7890a4', fontSize: 10}, axisLine: {lineStyle: {color: '#dce5ee'}}, axisTick: {show: false}},
      yAxis: {type: 'value', min: 0, splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: [{type: 'bar', data: series.values, itemStyle: {color: '#8a55df'}, barMaxWidth: 24}]
    };
  }

  /** 仅以在线和离线设备数绘制状态环形图。 */
  private deviceChartOption(): EChartsOption {
    const summary = this.deviceSummary;
    const statusData: {name: string; value: number; itemStyle: {color: string}}[] = [];
    if (summary.online > 0) {
      statusData.push({name: '在线', value: summary.online, itemStyle: {color: '#35b878'}});
    }
    if (summary.offline > 0) {
      statusData.push({name: '离线', value: summary.offline, itemStyle: {color: '#9aa7b3'}});
    }
    return {
      animationDuration: 420,
      tooltip: {
        trigger: 'item',
        renderMode: 'richText',
        formatter: '{b}：{c} 台（{d}%）'
      },
      series: [{
        type: 'pie',
        radius: ['67%', '89%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        label: {show: false},
        labelLine: {show: false},
        itemStyle: {borderColor: '#fff', borderWidth: 3, borderRadius: 3},
        emphasis: {scale: true, scaleSize: 5},
        data: statusData
      }]
    };
  }

  /** 按未确认、已确认和已清除三类互斥状态绘制实际告警趋势。 */
  private alarmChartOption(period: EnergyRegulationPeriod): EChartsOption {
    const series = this.realAlarmSeries(period);
    return {
      animationDuration: 320,
      grid: {top: 28, right: 12, bottom: 28, left: 34},
      legend: {top: 0, right: 0, itemWidth: 8, itemHeight: 8, textStyle: {color: '#6c8092', fontSize: 10}},
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
      xAxis: {type: 'category', data: series.labels, axisLabel: {color: '#7890a4', fontSize: 10}, axisLine: {lineStyle: {color: '#dce5ee'}}, axisTick: {show: false}},
      yAxis: {type: 'value', minInterval: 1, splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: [
        {name: '未确认', type: 'bar', stack: 'total', data: series.pending, itemStyle: {color: '#f3ad2e'}, barMaxWidth: 22},
        {name: '已确认', type: 'bar', stack: 'total', data: series.assigned, itemStyle: {color: '#408bea'}, barMaxWidth: 22},
        {name: '已清除', type: 'bar', stack: 'total', data: series.processing, itemStyle: {color: '#35bb82'}, barMaxWidth: 22}
      ]
    };
  }
}

/** 递归建立能源监管设备及资产 ID 到完整树路径的映射。 */
function buildEnergyRegulationAssetPathIndex(
  roots: readonly EnergyRegulationAssetTreeNode[],
  paths: Map<string, string>
): void {
  const visit = (node: EnergyRegulationAssetTreeNode, parentNames: readonly string[]): void => {
    const currentNames = [...parentNames, node.name?.trim() || node.id];
    paths.set(node.id, currentNames.join('/'));
    for (const child of node.children || []) {
      visit(child, currentNames);
    }
  };
  for (const root of roots) {
    visit(root, []);
  }
}

/** 把共享 active 遥测转换成明确的在线状态；未知编码保留上下文接口的状态。 */
function parseEnergyRegulationActiveValue(value: string | undefined): boolean | undefined {
  const normalizedValue = String(value ?? '').trim().toLowerCase();
  if (normalizedValue === 'true' || normalizedValue === '1') {
    return true;
  }
  if (normalizedValue === 'false' || normalizedValue === '0') {
    return false;
  }
  return undefined;
}
