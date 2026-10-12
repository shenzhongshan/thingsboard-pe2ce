/** 文件职责：能源监管总览看板，组织接口数据、演示内容和本地图表生命周期。 */
import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {AlarmQueryV2, AlarmSearchStatus, AlarmSeverity} from '@shared/models/alarm.models';
import {EntityType} from '@shared/models/entity-type.models';
import {MAX_SAFE_PAGE_SIZE, TimePageLink} from '@shared/models/page/page-link';
import {forkJoin, Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {EnergyRegulationApiService} from './energy-regulation-api.service';
import type {EnergyRegulationEnergyConsumption} from './energy-regulation-api.service';
import {
  buildEnergyRegulationDeviceOverview,
  DEMO_ENERGY_MAP_DEVICES,
  DEMO_ENERGY_REGULATION_DASHBOARD_DATA,
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

  /** 模板绑定集中使用本地演示数据，明确不代表实时监测结果。 */
  readonly demoData = DEMO_ENERGY_REGULATION_DASHBOARD_DATA;
  /** 地图当前仍展示演示点位，真实点位接入在后续步骤处理。 */
  readonly mapDevices = DEMO_ENERGY_MAP_DEVICES;
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

  constructor(
    private readonly energyRegulationApiService: EnergyRegulationApiService
  ) {}

  ngOnInit(): void {
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
    this.resizeObserver?.disconnect();
    this.charts.forEach((chart) => chart.dispose());
    this.charts.clear();
  }

  /** 复用共享设备上下文和活动告警查询，只统计 productCategory=energy_regulation 的设备。 */
  private loadDeviceOverview(): void {
    const alarmsQuery = new AlarmQueryV2(null, new TimePageLink(MAX_SAFE_PAGE_SIZE, 0), {
      statusList: [AlarmSearchStatus.ACTIVE],
      severityList: Object.values(AlarmSeverity)
    });
    forkJoin({
      devices: this.energyRegulationApiService.getDeviceContexts(),
      alarms: this.energyRegulationApiService.getAlarms(alarmsQuery)
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({devices, alarms}) => {
        const energyDeviceIds = new Set(devices.map((device) => device.deviceId));
        const alarmedDeviceIds = new Set<string>();
        for (const alarm of alarms.data || []) {
          if (alarm.originator?.entityType === EntityType.DEVICE
              && alarm.originator.id && energyDeviceIds.has(alarm.originator.id)) {
            alarmedDeviceIds.add(alarm.originator.id);
          }
        }
        const overview = buildEnergyRegulationDeviceOverview(devices, alarmedDeviceIds);
        this.deviceSummary = overview.summary;
        this.deviceProfiles = overview.profiles;
        this.deviceOverviewLoading = false;
        this.renderChart('devices');
      },
      error: () => {
        this.deviceOverviewLoading = false;
        this.deviceOverviewError = true;
      }
    });
  }

  /** 读取标准累计电能接口；失败时保留错误状态，不回退到演示用电量。 */
  private loadEnergyConsumption(): void {
    this.energyRegulationApiService.getEnergyConsumption().pipe(takeUntil(this.destroy$)).subscribe({
      next: (response) => {
        this.energyConsumption = response;
        this.energyConsumptionLoading = false;
        this.renderChart('energy');
      },
      error: () => {
        this.energyConsumptionLoading = false;
        this.energyConsumptionError = true;
        this.renderChart('energy');
      }
    });
  }

  /** 按图表类型生成配置；用电趋势来自标准电能接口，告警与碳排仍使用样例。 */
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

  /** 碳排放趋势保留当前样例口径，避免误用用电表底数据。 */
  private carbonChartOption(period: EnergyRegulationPeriod): EChartsOption {
    const series = this.demoData.carbonSeries[period];
    return {
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

  /** 绘制按未处理、已派发及处理中状态堆叠的告警趋势。 */
  private alarmChartOption(period: EnergyRegulationPeriod): EChartsOption {
    const series = this.demoData.alarmSeries[period];
    return {
      animationDuration: 320,
      grid: {top: 28, right: 12, bottom: 28, left: 34},
      legend: {top: 0, right: 0, itemWidth: 8, itemHeight: 8, textStyle: {color: '#6c8092', fontSize: 10}},
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
      xAxis: {type: 'category', data: series.labels, axisLabel: {color: '#7890a4', fontSize: 10}, axisLine: {lineStyle: {color: '#dce5ee'}}, axisTick: {show: false}},
      yAxis: {type: 'value', minInterval: 1, splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: [
        {name: '未处理', type: 'bar', stack: 'total', data: series.pending, itemStyle: {color: '#f3ad2e'}, barMaxWidth: 22},
        {name: '已派发', type: 'bar', stack: 'total', data: series.assigned, itemStyle: {color: '#408bea'}, barMaxWidth: 22},
        {name: '处理中', type: 'bar', stack: 'total', data: series.processing, itemStyle: {color: '#35bb82'}, barMaxWidth: 22}
      ]
    };
  }
}
