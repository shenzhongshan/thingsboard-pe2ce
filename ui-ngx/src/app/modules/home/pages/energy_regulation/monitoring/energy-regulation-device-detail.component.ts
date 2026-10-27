import {EnergyVisualFullscreenButtonComponent} from '../shared/energy-visual-fullscreen-button.component';
import {EnergyVisualFullscreenService} from '../shared/energy-visual-fullscreen.service';
/** ESA设备详情：能源权限上下文、最新遥测、分组历史曲线和单设备最近告警。 */
import {AfterViewInit, Component, ElementRef, HostListener, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute} from '@angular/router';
import {A11yModule} from '@angular/cdk/a11y';
import {EMPTY, Subject, catchError, mergeMap, takeUntil, tap} from 'rxjs';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {SharedModule} from '@app/shared/shared.module';
import {AlarmInfo} from '@shared/models/alarm.models';
import {EnergyRegulationDeviceContext} from '../energy-regulation-api.service';
import {EnergyRegulationLiveService} from '../shared/energy-regulation-live.service';
import {ENERGY_METER_MEASUREMENTS} from '../shared/energy-regulation-measurements';
import {TelemetryFitValueDirective} from '../../smart-lightning/shared/telemetry/telemetry-fit-value.directive';
import {SmartLightningAssetTreeModule} from '../../smart-lightning/shared/asset-tree/smart-lightning-asset-tree.module';
import {getSmartLightningAlarmSeverityLabel, getSmartLightningAlarmSeverityTone, getSmartLightningAlarmStatusLabel} from '../../deviceAlarms/shared/smart-lightning-alarm-display';
import {buildEnergyMonitoringAssetTree, EnergyAssetNode, EnergyMonitoringAssetTree} from './energy-regulation-monitoring.data';
import {EnergyRegulationDeviceDetailApiService} from './energy-regulation-device-detail-api.service';
import {buildEnergyDetailChartPoints, ENERGY_DETAIL_CHARTS, ENERGY_METER_SPECIFICATION, ENERGY_METER_USAGE, EnergyDetailChart,
  EnergyDetailPeriod, EnergyDetailSample, formatEnergyDetailValue, getEnergyDetailWindow, mergeEnergyDetailSamples} from './energy-regulation-device-detail.data';

/** 每个历史任务携带版本，切换周期后的旧响应不得覆盖新图。 */
interface EnergyChartRequest {definition: EnergyDetailChart; period: EnergyDetailPeriod; version: number;}

@Component({
  selector: 'tb-energy-regulation-device-detail',
  standalone: true,
  imports: [EnergyVisualFullscreenButtonComponent, CommonModule, SharedModule, A11yModule, TelemetryFitValueDirective, SmartLightningAssetTreeModule],
  templateUrl: './energy-regulation-device-detail.component.html',
  styleUrls: ['./energy-regulation-device-detail.component.scss']
})
export class EnergyRegulationDeviceDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChildren('chartHost') chartHosts!: QueryList<ElementRef<HTMLDivElement>>;
  readonly measurements = ENERGY_METER_MEASUREMENTS;
  readonly specification = ENERGY_METER_SPECIFICATION;
  readonly usage = ENERGY_METER_USAGE;
  readonly allChartDefinitions = ENERGY_DETAIL_CHARTS;
  readonly chartDefinitions = ENERGY_DETAIL_CHARTS.slice(0, 4);
  readonly chartPeriodOptions: ReadonlyArray<{value: EnergyDetailPeriod; label: string}> = [
    {value: 'day', label: '日'}, {value: 'week', label: '周'}, {value: 'month', label: '月'}
  ];
  readonly selectedChartPeriods: Record<string, EnergyDetailPeriod> = {};
  readonly chartLoading: Record<string, boolean> = {};
  readonly chartErrors: Record<string, boolean> = {};
  assetTree: EnergyMonitoringAssetTree = buildEnergyMonitoringAssetTree([]);
  selectedAssetIds = new Set<string>();
  expandedAssetIds = new Set<string>();
  selectableAssetIds = new Set<string>();
  activeAlarmDeviceIds = new Set<string>();
  assetSearch = '';
  device?: EnergyRegulationDeviceContext;
  deviceLocation = '__';
  loading = true;
  loadError = false;
  telemetryLoading = false;
  telemetryError = false;
  reportTime: number | null = null;
  alarms: AlarmInfo[] = [];
  alarmsLoading = false;
  alarmsLoadError = false;
  showAllChartsDialog = false;
  private readonly samples = new Map<string, EnergyDetailSample>();
  private readonly historyValues: Record<string, Record<string, EnergyDetailSample[]>> = {};
  private readonly chartVersions: Record<string, number> = {};
  private readonly charts = new Map<string, ECharts>();
  private readonly historyRequests$ = new Subject<EnergyChartRequest>();
  private readonly alarmRequest$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  private readonly deviceId: string;
  private resizeObserver?: ResizeObserver;
  private dialogTrigger?: HTMLElement;

  constructor(private readonly route: ActivatedRoute, private readonly live: EnergyRegulationLiveService,
              private readonly api: EnergyRegulationDeviceDetailApiService, private readonly fullscreen: EnergyVisualFullscreenService) {
    this.deviceId = this.route.snapshot.paramMap.get('deviceId') || '';
    for (const chart of this.allChartDefinitions) { this.selectedChartPeriods[chart.id] = 'day'; }
  }

  /** 历史查询最多4个并发；先核对当前能源设备权限，再读取单设备遥测和告警。 */
  ngOnInit(): void {
    this.historyRequests$.pipe(mergeMap((request) => {
      const {definition, period, version} = request;
      if (!this.device || version !== this.chartVersions[definition.id]) { return EMPTY; }
      const window = getEnergyDetailWindow(period);
      return this.api.getHistory(this.deviceId, definition.keys, window.startTs, window.endTs).pipe(tap((values) => {
        if (this.device && version === this.chartVersions[definition.id]) {
          // 合并请求期间到达的实时采样，不能让历史响应把实时曲线退回旧值。
          const realtime = this.historyValues[definition.id] || {};
          const merged: Record<string, EnergyDetailSample[]> = {};
          for (const key of definition.keys) {
            merged[key] = buildEnergyDetailChartPoints([...(values[key] || []), ...(realtime[key] || [])])
              .filter(([ts]) => ts >= window.startTs).slice(-1000).map(([ts, value]) => ({ts, value}));
          }
          this.historyValues[definition.id] = merged;
          this.chartLoading[definition.id] = false;
          this.renderCharts();
        }
      }), catchError(() => {
        if (version === this.chartVersions[definition.id]) {
          this.chartLoading[definition.id] = false;
          this.chartErrors[definition.id] = true;
          this.renderCharts();
        }
        return EMPTY;
      }));
    }, 4), takeUntil(this.destroy$)).subscribe();
    this.live.acquire();
    this.live.snapshot$.pipe(takeUntil(this.destroy$)).subscribe((snapshot) => {
      this.loading = snapshot.loading;
      if (snapshot.loading) { return; }
      const previous = this.device;
      this.device = snapshot.contexts.find((item) => item.deviceId === this.deviceId);
      this.loadError = snapshot.error || !this.device;
      this.assetTree = buildEnergyMonitoringAssetTree(snapshot.roots);
      this.activeAlarmDeviceIds = new Set(snapshot.alarms.filter((alarm) => !alarm.cleared).map((alarm) => alarm.originator.id));
      if (!this.device) {
        this.samples.clear();
        this.alarms = [];
        this.fullscreen.exit();
        this.disposeCharts();
        return;
      }
      this.initializeAssetLocation();
      this.mergeRealtime();
      if (!previous) {
        this.readLatest();
        for (const definition of this.chartDefinitions) { this.requestChart(definition); }
      }
      this.readAlarms();
    });
    this.live.telemetryChanged$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.device) { this.mergeRealtime(); }
    });
  }

  /** 图表只在容器存在时初始化；弹窗切换后释放旧实例，避免挤压或重复初始化。 */
  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => this.onResize());
    this.chartHosts.changes.pipe(takeUntil(this.destroy$)).subscribe(() => this.synchronizeChartHosts());
    this.synchronizeChartHosts();
  }

  /** 与实时数据独立取在线状态，不把状态时间计入业务遥测更新时间。 */
  get deviceActive(): boolean {
    const value = this.live.latest(this.deviceId).get('active')?.value;
    return value === undefined ? this.device?.active === true : ['true', '1'].includes(String(value).toLowerCase());
  }

  /** 统一输出状态文本、数值和单位，供共享字体自适应指令测量。 */
  measurementValue(key: string, status: boolean, unit: string): string {
    return formatEnergyDetailValue(this.samples.get(key)?.value, status, unit);
  }

  /** 悬浮保留规范键和实际采样时间；状态异常用与其他详情页一致的红色格子。 */
  measurementTitle(key: string): string {
    const sample = this.samples.get(key);
    return `${key} · ${sample ? new Date(sample.ts).toLocaleString() : '暂无采样'}`;
  }

  /** 只把已定义状态测点的1标红，不自行推断电压等数值的告警阈值。 */
  isAbnormal(key: string, status: boolean): boolean {
    return status && String(this.samples.get(key)?.value) === '1';
  }

  /** 详情树只定位当前设备，允许展开/收起资产层级，不更换设备上下文。 */
  toggleAssetNode(node: EnergyAssetNode): void {
    const expanded = new Set(this.expandedAssetIds);
    if (expanded.has(node.id)) { expanded.delete(node.id); } else { expanded.add(node.id); }
    this.expandedAssetIds = expanded;
  }

  /** 各图独立更新周期，不重查其他图表。 */
  setChartPeriod(id: string, period: EnergyDetailPeriod): void {
    if (this.selectedChartPeriods[id] === period) { return; }
    this.selectedChartPeriods[id] = period;
    delete this.historyValues[id];
    const definition = this.allChartDefinitions.find((chart) => chart.id === id);
    if (definition) { this.requestChart(definition); }
  }

  /** 更多参数采用相同两列图卡弹窗，并保留各图已经选择的周期。 */
  openAllChartsDialog(event: Event): void {
    this.dialogTrigger = event.currentTarget as HTMLElement;
    this.fullscreen.exit();
    this.disposeCharts();
    this.showAllChartsDialog = true;
    for (const definition of this.allChartDefinitions) {
      if (!this.historyValues[definition.id] && !this.chartLoading[definition.id]) { this.requestChart(definition); }
    }
  }

  /** 关闭时清理放大和图表实例，并把键盘焦点交还触发按钮。 */
  closeAllChartsDialog(): void {
    this.fullscreen.exit();
    this.disposeCharts();
    this.showAllChartsDialog = false;
    setTimeout(() => this.dialogTrigger?.focus());
  }

  /** 图表继续使用原有尺寸监听，放大边界由能源共享控制器维护。 */
  @HostListener('window:resize')
  onResize(): void {
    this.charts.forEach((chart) => chart.resize());
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscape(event: Event): void {
    if (this.fullscreen.activeTarget) { this.fullscreen.exit(); this.onResize(); event.preventDefault(); }
    else if (this.showAllChartsDialog) { this.closeAllChartsDialog(); event.preventDefault(); }
  }

  /** 显示平台原告警名称、严重度和完整确认/清除状态，不自定义告警类型。 */
  severityLabel(alarm: AlarmInfo): string { return getSmartLightningAlarmSeverityLabel(getSmartLightningAlarmSeverityTone(alarm.severity)); }
  statusLabel(alarm: AlarmInfo): string { return getSmartLightningAlarmStatusLabel(alarm.status); }

  /** 资料中的固定参数只用于已确认的ESA型号；不猜测其他型号产品说明。 */
  get isEsa(): boolean { return this.device?.deviceProfileName?.toUpperCase() === 'E1A2'; }

  /** 根据权限树追溯设备绝对资产路径，遇到环形关系立即终止。 */
  private initializeAssetLocation(): void {
    const parents = new Map<string, string>();
    this.assetTree.nodes.forEach((node) => node.childIds.forEach((id) => parents.set(id, node.id)));
    const names: string[] = [];
    const expanded = new Set(this.expandedAssetIds);
    const visited = new Set<string>();
    let id: string | undefined = this.deviceId;
    while (id && !visited.has(id)) {
      visited.add(id);
      const node = this.assetTree.nodes.get(id);
      if (node) { names.unshift(node.name); }
      id = parents.get(id);
      if (id) { expanded.add(id); }
    }
    this.deviceLocation = names.join('/') || '__';
    this.expandedAssetIds = expanded;
    this.selectedAssetIds = new Set([this.deviceId]);
    this.selectableAssetIds = new Set([this.deviceId]);
  }

  /** 最新值补读也适用于离线设备，响应按采样时间与实时缓存合并。 */
  private readLatest(): void {
    this.telemetryLoading = true;
    this.telemetryError = false;
    this.api.getLatest(this.deviceId, this.measurements.map((item) => item.key)).pipe(takeUntil(this.destroy$)).subscribe({next: (values) => {
      if (this.device) { mergeEnergyDetailSamples(this.samples, values); this.updateReportTime(); }
      this.telemetryLoading = false;
    }, error: () => { this.telemetryLoading = false; this.telemetryError = true; }});
  }

  /** 最新流更新数值和已加载曲线；不对每条WebSocket消息再发历史HTTP请求。 */
  private mergeRealtime(): void {
    const values = this.live.latest(this.deviceId);
    const latest: Record<string, EnergyDetailSample[]> = {};
    for (const definition of this.measurements) {
      const sample = values.get(definition.key);
      if (sample) { latest[definition.key] = [sample]; }
    }
    mergeEnergyDetailSamples(this.samples, latest);
    for (const definition of this.allChartDefinitions) {
      const history = this.historyValues[definition.id];
      if (!history) { continue; }
      const window = getEnergyDetailWindow(this.selectedChartPeriods[definition.id]);
      for (const key of definition.keys) {
        history[key] = buildEnergyDetailChartPoints([...(history[key] || []), ...(latest[key] || [])])
          .filter(([ts]) => ts >= window.startTs && ts <= window.endTs).slice(-1000).map(([ts, value]) => ({ts, value}));
      }
    }
    this.updateReportTime();
    this.renderCharts();
  }

  /** 只使用业务测点最大采样时间，不能用进入页面的时间代替。 */
  private updateReportTime(): void {
    const timestamps = Array.from(this.samples.values(), (sample) => sample.ts);
    this.reportTime = timestamps.length ? Math.max(...timestamps) : null;
  }

  /** 权限校验后只读取当前设备最近十条，包括已清除告警；新快照取消旧请求。 */
  private readAlarms(): void {
    this.alarmRequest$.next();
    this.alarmsLoading = !this.alarms.length;
    this.alarmsLoadError = false;
    this.api.getDeviceAlarms(this.deviceId).pipe(takeUntil(this.alarmRequest$), takeUntil(this.destroy$)).subscribe({next: (page) => {
      if (this.device) { this.alarms = page.data; }
      this.alarmsLoading = false;
    }, error: () => { this.alarmsLoading = false; this.alarmsLoadError = true; }});
  }

  /** 新查询提升图表版本号，过期请求在并发队列与响应边界均会被丢弃。 */
  private requestChart(definition: EnergyDetailChart): void {
    const version = (this.chartVersions[definition.id] || 0) + 1;
    this.chartVersions[definition.id] = version;
    this.chartLoading[definition.id] = true;
    this.chartErrors[definition.id] = false;
    this.renderCharts();
    this.historyRequests$.next({definition, version, period: this.selectedChartPeriods[definition.id]});
  }

  /** 视图切换后只为真实存在的容器创建实例，销毁离开视图的实例。 */
  private synchronizeChartHosts(): void {
    this.resizeObserver?.disconnect();
    const visible = new Set<string>();
    this.chartHosts.forEach((host) => {
      const id = host.nativeElement.dataset.chart;
      if (!id) { return; }
      visible.add(id);
      if (!this.charts.has(id)) { this.charts.set(id, echarts.init(host.nativeElement, undefined, {renderer: 'svg'})); }
      this.resizeObserver?.observe(host.nativeElement);
    });
    this.charts.forEach((chart, id) => { if (!visible.has(id)) { chart.dispose(); this.charts.delete(id); } });
    this.renderCharts();
  }

  /** 原始时间轴、测点中文图例及单位提示与前两个模块一致，空数据不画零线。 */
  private renderCharts(): void {
    this.charts.forEach((chart, id) => {
      const definition = this.allChartDefinitions.find((item) => item.id === id)!;
      const window = getEnergyDetailWindow(this.selectedChartPeriods[id]);
      const values = this.historyValues[id] || {};
      const series = definition.keys.map((key) => {
        const metric = this.measurements.find((item) => item.key === key)!;
        return {name: metric.label, type: 'line' as const, smooth: true, symbol: 'none',
          data: buildEnergyDetailChartPoints(values[key] || []), lineStyle: {width: 2.2},
          tooltip: {valueFormatter: (value: unknown) => formatEnergyDetailValue(Array.isArray(value) ? value[1] : value, false, metric.unit)}};
      });
      const points = series.flatMap((item) => item.data);
      const unit = this.measurements.find((item) => item.key === definition.keys[0])?.unit || '';
      const hint = this.chartLoading[id] ? '正在读取…' : this.chartErrors[id] ? '历史数据读取失败' : points.length ? '' : '__';
      const option: EChartsOption = {animation: false, color: ['#ff5656', '#814ce3', '#11a94d', '#195ce8'],
        tooltip: {trigger: 'axis'}, legend: {top: 4, right: 8, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 10, color: '#61778b'}},
        grid: {top: 52, left: 48, right: 16, bottom: 36},
        graphic: hint ? [{type: 'text', left: 'center', top: 'middle', style: {text: hint, fill: '#718599'}}] : [],
        xAxis: {type: 'time', min: window.startTs, max: window.endTs, axisLabel: {fontSize: 9, hideOverlap: true}, axisTick: {show: false}},
        yAxis: {type: 'value', name: unit, scale: true, axisLabel: {fontSize: 9, formatter: (value: number) => String(value)},
          splitLine: {lineStyle: {color: '#e5ebf1'}}}, series};
      chart.setOption(option, true);
    });
  }





  /** 销毁全部ECharts实例；容器重新出现后再初始化。 */
  private disposeCharts(): void { this.charts.forEach((chart) => chart.dispose()); this.charts.clear(); }

  /** 清理图表、放大样式和本页订阅，不影响其他页面使用同一模块实时服务。 */
  ngOnDestroy(): void {
    this.fullscreen.exit();
    this.destroy$.next(); this.destroy$.complete();
    this.alarmRequest$.complete(); this.historyRequests$.complete();
    this.resizeObserver?.disconnect(); this.disposeCharts(); this.live.release();
  }
}
