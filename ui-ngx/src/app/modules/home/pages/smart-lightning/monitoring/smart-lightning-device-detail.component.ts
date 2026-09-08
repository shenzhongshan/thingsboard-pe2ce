import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, QueryList, ViewChildren} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AttributeService} from '@core/http/attribute.service';
import {DeviceService} from '@core/http/device.service';
import {AlarmService} from '@core/http/alarm.service';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {catchError, of, Subject, switchMap, takeUntil} from 'rxjs';
import {DeviceId} from '@shared/models/id/device-id';
import {AttributeScope} from '@shared/models/telemetry/telemetry.models';
import {AlarmInfo, AlarmQuery, AlarmSearchStatus, AlarmSeverity} from '@shared/models/alarm.models';
import {TimePageLink} from '@shared/models/page/page-link';
import {Direction} from '@shared/models/page/sort-order';
import {buildSmartLightningMonitoringAssetTreeFromRoots, getSmartLightningMonitoringSelectedAssetIds, SmartLightningMonitoringAssetNode, SmartLightningMonitoringAssetTree} from './smart-lightning-monitoring.data';
import {SmartLightningMonitoringApiService} from './smart-lightning-monitoring-api.service';
import {buildSmartLightningChartTimes, buildSmartLightningDeviceDetailMock, buildSmartLightningRealtimeMetrics, fitSmartLightningMockSeries, getSmartLightningRealtimeMetricDefinitions, SmartLightningChartPeriod, SmartLightningDeviceDetailMock} from './smart-lightning-device-detail.data';

/** 资产树复用真实接口；下方监测、控制和告警区域在遥测接入前均为明确标记的模拟数据。 */
@Component({selector: 'tb-smart-lightning-device-detail', templateUrl: './smart-lightning-device-detail.component.html', styleUrls: ['./smart-lightning-device-detail.component.scss'], changeDetection: ChangeDetectionStrategy.OnPush, standalone: false})
export class SmartLightningDeviceDetailComponent implements AfterViewInit, OnDestroy {
  @ViewChildren('chartHost') chartHosts!: QueryList<ElementRef<HTMLDivElement>>;
  assetTree: SmartLightningMonitoringAssetTree = {roots: [], nodes: new Map()};
  expandedAssetIds = new Set<string>(); selectedAssetIds = new Set<string>(); selectedAssetName = '未选择资产';
  assetSearch = ''; assetsLoading = true; assetsLoadError = false;
  detail: SmartLightningDeviceDetailMock = buildSmartLightningDeviceDetailMock();
  productName = '未配置产品名称';
  deviceName = '--';
  instrumentAddress = '--';
  alarmsLoading = false;
  alarmsLoadError = false;
  /** 资产树设备节点的状态标记：当前设备存在未处理告警时显示红色竖条。 */
  get activeAlarmDeviceIds(): Set<string> {
    const hasUnhandledAlarm = this.detail.alarms.some((alarm) => alarm.status === '未处理');
    return hasUnhandledAlarm && this.requestedDeviceId ? new Set([this.requestedDeviceId]) : new Set<string>();
  }
  get selectableAssetIds(): ReadonlySet<string> | null {
    return this.requestedDeviceId ? new Set([this.requestedDeviceId]) : null;
  }
  returnUrl = '/smart-lightning/monitoring';
  returnLabel = '返回';
  readonly chartPeriodOptions: ReadonlyArray<{value: SmartLightningChartPeriod; label: string}> = [
    {value: 'day', label: '日'}, {value: 'week', label: '周'}, {value: 'month', label: '月'}
  ];
  selectedChartPeriods: Record<string, SmartLightningChartPeriod> = {
    phaseCurrent: 'day', phaseVoltage: 'day', residualCurrent: 'day', temperature: 'day'
  };
  private requestedDeviceId?: string; private readonly charts = new Map<string, ECharts>(); private readonly destroy$ = new Subject<void>(); private resizeObserver?: ResizeObserver;
  constructor(private readonly monitoringApi: SmartLightningMonitoringApiService, private readonly attributeService: AttributeService, private readonly deviceService: DeviceService, private readonly alarmService: AlarmService, private readonly route: ActivatedRoute, private readonly router: Router, private readonly cd: ChangeDetectorRef) {}
  ngAfterViewInit(): void {
    this.returnUrl = this.resolveReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
    this.route.paramMap.pipe(
      switchMap((params) => {
        this.requestedDeviceId = params.get('deviceId') || undefined;
        this.detail = buildSmartLightningDeviceDetailMock(this.requestedDeviceId);
        this.productName = '未配置产品名称';
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
    this.renderCharts(); this.loadAssetTree(); this.resizeObserver = new ResizeObserver(() => this.charts.forEach((chart) => chart.resize())); this.chartHosts.forEach((host) => this.resizeObserver?.observe(host.nativeElement));
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); this.resizeObserver?.disconnect(); this.charts.forEach((chart) => chart.dispose()); }
  /** 根据来源页返回；未携带来源参数时默认回到数据监测。 */
  goBack(event: MouseEvent): void {
    event.preventDefault();
    this.router.navigateByUrl(this.returnUrl);
  }
  private resolveReturnUrl(value: string | null): string {
    return value === '/smart-lightning/query' ? value : '/smart-lightning/monitoring';
  }
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
  selectAssetNode(node: SmartLightningMonitoringAssetNode): void {
    if (!this.isNodeSelectable(node)) { return; }
    this.selectedAssetIds = this.requestedDeviceId ? new Set([node.id]) : new Set(getSmartLightningMonitoringSelectedAssetIds(this.assetTree.nodes, node.id));
    this.selectedAssetName = node.name; this.deviceName = node.name; this.instrumentAddress = this.getAssetPath(node.id).join('/') || '--'; this.openDescendants(node.id); this.cd.markForCheck();
  }
  private loadAssetTree(): void { this.monitoringApi.getTree().pipe(takeUntil(this.destroy$)).subscribe({next: ({roots}) => { this.assetTree = buildSmartLightningMonitoringAssetTreeFromRoots(roots); const initial = this.requestedDeviceId ? this.assetTree.nodes.get(this.requestedDeviceId) : this.assetTree.roots[0]; if (initial) { this.openPathTo(initial.id); this.selectAssetNode(initial); } this.assetsLoading = false; this.cd.markForCheck(); }, error: () => { this.assetsLoadError = true; this.assetsLoading = false; this.cd.markForCheck(); }}); }
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
   * 任意一项缺失时由 buildSmartLightningRealtimeMetrics 保留对应模拟值，而不是清空整组卡片。
   */
  private loadRealtimeMetrics(deviceId?: string): void {
    if (!deviceId) { return; }
    this.deviceService.getDeviceInfo(deviceId).pipe(
      switchMap((device) => {
        const definitions = getSmartLightningRealtimeMetricDefinitions(device.deviceProfileName);
        if (!definitions.length) { return of({deviceProfileName: device.deviceProfileName, latest: {}}); }
        // 规则链可能已经输出规范键，也可能仍保存设备原始键；两类键都查询，映射层按规范键优先取值。
        const readKeys = Array.from(new Set(definitions.flatMap((definition) => [definition.key, ...(definition.sourceKeys || [])])));
        return this.attributeService.getEntityTimeseriesLatest(new DeviceId(deviceId), readKeys).pipe(
          catchError(() => of({})),
          switchMap((latest) => of({deviceProfileName: device.deviceProfileName, latest}))
        );
      }),
      catchError(() => of({deviceProfileName: undefined, latest: {}})),
      takeUntil(this.destroy$)
    ).subscribe(({deviceProfileName, latest}) => {
      const latestValues = new Map<string, unknown>();
      Object.entries(latest).forEach(([key, values]) => {
        const firstValue = Array.isArray(values) ? values[0]?.value : undefined;
        if (firstValue !== undefined) { latestValues.set(key, firstValue); }
      });
      this.detail = {...this.detail, metrics: buildSmartLightningRealtimeMetrics(deviceProfileName, latestValues, this.detail.metrics)};
      this.cd.markForCheck();
    });
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
  private openDescendants(id: string): void { const node = this.assetTree.nodes.get(id); if (!node) { return; } node.childIds.forEach((childId) => { this.expandedAssetIds.add(childId); this.openDescendants(childId); }); this.expandedAssetIds = new Set(this.expandedAssetIds); }
  setChartPeriod(id: string, period: SmartLightningChartPeriod): void {
    this.selectedChartPeriods = {...this.selectedChartPeriods, [id]: period};
    const chart = this.charts.get(id);
    if (chart) { chart.setOption(this.createChartOption(id), true); }
  }
  private renderCharts(): void { this.chartHosts.forEach((host) => { const id = host.nativeElement.dataset.chart; if (!id) { return; } const chart = echarts.init(host.nativeElement, undefined, {renderer: 'svg'}); this.charts.set(id, chart); chart.setOption(this.createChartOption(id)); }); }
  private createChartOption(id: string): EChartsOption {
    const period = this.selectedChartPeriods[id] || 'day';
    const times = buildSmartLightningChartTimes(period);
    const one = (name: string, data: number[], color: string) => ({name, type: 'line' as const, smooth: true, data, symbol: 'none', lineStyle: {width: 2.5, color}, areaStyle: {color: 'rgba(25,92,232,.05)'}});
    const temperature = [one('温度1', [40, 47, 38, 43, 37, 48], '#ff4d55'), one('温度2', [20, 25, 18, 32, 42, 18], '#814ce3'), one('温度3', [26, 33, 17, 23, 15, 35], '#11a94d')];
    const settings: Record<string, {unit: string; series: any[]}> = {phaseCurrent: {unit: 'mA', series: [one('剩余电流', [39, 32, 40, 36, 42, 29], '#ff5656')]}, residualCurrent: {unit: 'mA', series: [one('剩余电流', [40, 31, 40, 35, 42, 29], '#ff5656')]}, phaseVoltage: {unit: '℃', series: temperature}, temperature: {unit: '℃', series: temperature}};
    const setting = settings[id];
    const series = setting.series.map((item) => ({...item, data: fitSmartLightningMockSeries(item.data as number[], times.length)}));
    return {animation: false, tooltip: {trigger: 'axis'}, legend: {top: 4, right: 8, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 10, color: '#61778b'}}, grid: {top: 42, left: 40, right: 16, bottom: 38}, xAxis: {type: 'category', boundaryGap: false, data: times, axisTick: {show: false}, axisLine: {lineStyle: {color: '#cfdae3'}}, axisLabel: {fontSize: 9, color: '#71859a', hideOverlap: true}}, yAxis: {type: 'value', name: setting.unit, nameTextStyle: {fontSize: 10, color: '#71859a'}, axisLabel: {fontSize: 9, color: '#71859a'}, splitLine: {lineStyle: {color: '#eef2f5'}}}, series};
  }
}
