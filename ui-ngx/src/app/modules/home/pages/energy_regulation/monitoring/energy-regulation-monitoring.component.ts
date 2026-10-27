import {EnergyVisualFullscreenButtonComponent} from '../shared/energy-visual-fullscreen-button.component';
/** 文件职责：组织用能监测资产范围、指标周期、设备筛选和图表展示。 */
import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, RouterModule} from '@angular/router';
import {ECharts, EChartsOption} from 'echarts';
import * as echarts from 'echarts';
import {Subject, forkJoin, auditTime} from 'rxjs';
import {EnergyRegulationLiveService, EnergyLiveSnapshot} from '../shared/energy-regulation-live.service';
import {EnergyRegulationEnergyConsumption} from '../energy-regulation-api.service';
import {AlarmInfo} from '@shared/models/alarm.models';
import {takeUntil} from 'rxjs/operators';
import {EnergyRegulationApiService} from '../energy-regulation-api.service';
import type {EnergyRegulationAssetTreeNode} from '../energy-regulation-api.service';
import type {EnergyMonitoringCategory, EnergyMonitoringDevice, EnergyMonitoringPeriod} from './energy-regulation-monitoring.data';
import {
  buildEnergyMonitoringAssetSummary,
  buildEnergyMonitoringAssetTree,
  buildEnergyMonitoringDevices,
  ENERGY_MONITORING_CATEGORIES,
  ENERGY_MONITORING_KPIS,
  ENERGY_MONITORING_PERIODS,
  ENERGY_MONITORING_QUALITY,
  ENERGY_MONITORING_TRENDS,
  filterEnergyMonitoringDevices,
  getEnergyMonitoringSelectedAssetIds
} from './energy-regulation-monitoring.data';
import {getEnergyDefaultExpandedAssetIds, getEnergyDescendantIds, EnergyAssetNode} from './energy-regulation-monitoring.data';
import {SmartLightningRootAssetFilterComponent} from '@home/pages/smart-lightning/smart-lightning-root-asset-filter.component';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {SharedModule} from '@app/shared/shared.module';
import {SmartLightningAssetTreeModule} from '@home/pages/smart-lightning/shared/asset-tree/smart-lightning-asset-tree.module';

@Component({
  selector: 'tb-energy-regulation-monitoring',
  templateUrl: './energy-regulation-monitoring.component.html',
  styleUrls: ['./energy-regulation-monitoring.component.scss'],
  standalone: true,
  imports: [EnergyVisualFullscreenButtonComponent, CommonModule, FormsModule, RouterModule, SharedModule, SmartLightningRootAssetFilterComponent, SmartLightningAssetTreeModule]
})
export class EnergyRegulationMonitoringComponent implements OnInit, AfterViewInit, OnDestroy {
  /** ECharts 容器在视图创建后初始化，并随容器尺寸改变重新布局。 */
  @ViewChild('trendHost') trendHost?: ElementRef<HTMLDivElement>;

  /** 公司筛选和下方资产树都取自后端按当前用户权限返回的真实关系树。 */
  assetTree = buildEnergyMonitoringAssetTree([]);
  assetsLoading = true;
  assetsLoadError = false;
  statisticsLoadError = false;
  /** 设备总览独立区分加载和失败状态，展示口径与防雷、电气安全一致。 */
  devicesLoading = true;
  devicesLoadError = false;
  selectedRootAssetId = '';
  selectedAssetId = '';
  selectedAssetName = '全部资产';
  selectedAssetIds = new Set<string>();
  expandedAssetIds = new Set<string>();
  assetSearch = '';

  /** 三个周期切换器互不联动。 */
  readonly periods = ENERGY_MONITORING_PERIODS;
  readonly categories = ENERGY_MONITORING_CATEGORIES;
  readonly ENERGY_MONITORING_KPIS = structuredClone(ENERGY_MONITORING_KPIS);
  readonly ENERGY_MONITORING_QUALITY = structuredClone(ENERGY_MONITORING_QUALITY);
  readonly math = Math;
  metricsPeriod: EnergyMonitoringPeriod = 'day';
  trendPeriod: EnergyMonitoringPeriod = 'day';
  qualityPeriod: EnergyMonitoringPeriod = 'day';
  selectedCategory: EnergyMonitoringCategory = 'all';
  deviceSearch = '';
  page = 1;
  readonly pageSize = 20;
  devices: EnergyMonitoringDevice[] = [];

  private liveSnapshot?: EnergyLiveSnapshot;
  private consumption: EnergyRegulationEnergyConsumption | null = null;
  private statsRequest$ = new Subject<void>();
  private trendChart?: ECharts;
  private resizeObserver?: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly energyRegulationApiService: EnergyRegulationApiService, private readonly live: EnergyRegulationLiveService, private readonly route: ActivatedRoute) {}

  /** 载入后端当前用户可见资产树，并映射真实设备上下文与规范遥测。 */
  ngOnInit(): void {
    // 仅恢复本模块详情链接携带的范围；资产加载后再校验节点是否仍然可见。
    this.selectedRootAssetId = this.route.snapshot.queryParamMap.get('returnRootAssetId') || '';
    this.selectedAssetId = this.route.snapshot.queryParamMap.get('returnAssetId') || '';
    this.live.acquire();
    this.live.snapshot$.pipe(takeUntil(this.destroy$)).subscribe((snapshot) => {
      this.liveSnapshot = snapshot;
      this.assetsLoadError = snapshot.error;
      this.devicesLoading = snapshot.loading;
      this.devicesLoadError = snapshot.error;
      if (!snapshot.loading) { this.initializeAssetScope(snapshot.roots); this.refreshDevices(); }
    });
    this.live.telemetryChanged$.pipe(takeUntil(this.destroy$)).subscribe(() => this.refreshDevices());
    this.live.telemetryChanged$.pipe(auditTime(5000), takeUntil(this.destroy$)).subscribe(() => this.loadStatistics());
  }

  /** 视图创建后创建能耗趋势图，并观察页面宽度变化。 */
  ngAfterViewInit(): void {
    if (!this.trendHost) {
      return;
    }
    this.trendChart = echarts.init(this.trendHost.nativeElement, undefined, {renderer: 'svg'});
    this.renderTrend();
    this.resizeObserver = new ResizeObserver(() => this.trendChart?.resize());
    this.resizeObserver.observe(this.trendHost.nativeElement);
  }

  /** 资产树选择公司根节点后，同步根筛选、统计和设备范围。 */
  selectRootAsset(rootAssetId: string): void {
    this.selectedRootAssetId = rootAssetId;
    const root = this.assetTree.roots.find((node) => node.id === rootAssetId);
    this.selectedAssetId = rootAssetId;
    this.selectedAssetName = root?.name || '全部资产';
    this.selectedAssetIds = getEnergyMonitoringSelectedAssetIds(this.assetTree, rootAssetId);
    if (!rootAssetId) {
      for (const device of this.liveSnapshot?.contexts || []) { this.selectedAssetIds.add(device.deviceId); }
    }
    this.expandedAssetIds = new Set(getEnergyDefaultExpandedAssetIds(this.assetTree));
    this.resetDevicePage();
    this.refreshDevices();
    this.loadStatistics();
  }

  /** 选择任一资产节点后，汇总该资产及其全部下属楼层设备。 */
  selectAssetNode(node: EnergyAssetNode): void {
    this.selectedAssetId = node.id;
    this.selectedAssetName = node.name;
    this.selectedAssetIds = new Set(getEnergyDescendantIds(this.assetTree.nodes, node.id));
    this.resetDevicePage();
    this.refreshDevices();
    this.loadStatistics();
  }

  /** 切换资产节点展开状态时创建新集合，触发共享树组件变更检测。 */
  toggleAssetNode(node: EnergyAssetNode): void {
    if (this.expandedAssetIds.has(node.id)) {
      this.expandedAssetIds.delete(node.id);
    } else {
      this.expandedAssetIds.add(node.id);
    }
    this.expandedAssetIds = new Set(this.expandedAssetIds);
  }

  /** 全选当前根资产筛选范围，并将统计范围恢复为根节点子树。 */
  selectAllAssets(): void {
    this.selectRootAsset(this.selectedRootAssetId);
  }

  /** 查询按钮按当前资产、分类和关键词刷新分页位置。 */
  applyFilters(): void {
    this.page = 1;
  }

  /** 清除根资产、资产树选择、分类和设备关键词。 */
  resetFilters(): void {
    this.selectedRootAssetId = '';
    this.selectedCategory = 'all';
    this.assetSearch = '';
    this.deviceSearch = '';
    this.selectRootAsset('');
  }

  /** 分类变化后回到第一页，避免当前页超出过滤结果。 */
  selectCategory(category: EnergyMonitoringCategory): void {
    this.selectedCategory = category;
    this.resetDevicePage();
  }

  /** 每个面板自行切换日、周或月，并只更新对应图表或指标。 */
  selectPeriod(panel: 'metrics' | 'trend' | 'quality', period: EnergyMonitoringPeriod): void {
    if (panel === 'metrics') {
      this.metricsPeriod = period;
    } else if (panel === 'trend') {
      this.trendPeriod = period;
      this.renderTrend();
    } else {
      this.qualityPeriod = period;
    }
  }

  /** 根据已选资产子树汇总真实设备在线和告警状态。 */
  get assetSummary() {
    const summary = buildEnergyMonitoringAssetSummary(this.scopedDevices);
    const ids = new Set(this.scopedDevices.map((device) => device.id));
    const alarms = (this.liveSnapshot?.alarms || []).filter((alarm) => ids.has(alarm.originator.id));
    return {...summary, alarmTotal: alarms.length, unprocessedAlarms: alarms.filter((alarm) => !alarm.cleared).length,
      processedAlarms: alarms.filter((alarm) => alarm.cleared).length};
  }

  /** 获取当前资产树范围内的设备，不受分类和搜索影响。 */
  get scopedDevices(): EnergyMonitoringDevice[] {
    return this.devices.filter((device) => this.selectedAssetIds.has(device.assetId));
  }

  /** 获取当前资产、分类与名称搜索条件下的设备集合。 */
  get filteredDevices(): EnergyMonitoringDevice[] {
    return filterEnergyMonitoringDevices(
      this.devices, this.selectedAssetIds, this.selectedCategory, this.deviceSearch);
  }

  /** 当前页设备卡片或表格行。 */
  get pagedDevices(): EnergyMonitoringDevice[] {
    const start = (this.page - 1) * this.pageSize;
    return this.filteredDevices.slice(start, start + this.pageSize);
  }

  /** 当前筛选后的总页数至少为一页，空结果时仍保留分页控件稳定布局。 */
  get pageCount(): number {
    return Math.max(1, Math.ceil(this.filteredDevices.length / this.pageSize));
  }

  /** 返回资产节点全路径，用于设备卡片定位信息。 */
  assetPath(assetId: string): string {
    const names: string[] = [];
    let current = this.assetTree.flatNodes.find((node) => node.id === assetId);
    while (current) {
      names.unshift(current.name);
      const parentId = current.parentId;
      current = parentId
        ? this.assetTree.flatNodes.find((node) => node.id === parentId)
        : undefined;
    }
    return names.join(' / ') || '--';
  }

  /** 切换上一页或下一页，并将页码限制在有效区间。 */
  movePage(offset: number): void {
    this.page = Math.min(this.pageCount, Math.max(1, this.page + offset));
  }

  /** 页面退出时清理观察器、图表和待处理订阅。 */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.live.release();
    this.statsRequest$.complete();
    this.resizeObserver?.disconnect();
    this.trendChart?.dispose();
  }

  /** 用真实权限树建立选择范围，设备使用后端真实实体ID。 */
  private initializeAssetScope(roots: EnergyRegulationAssetTreeNode[]): void {
    const initial = this.assetsLoading;
    const expanded = this.expandedAssetIds;
    const page = this.page;
    this.assetTree = buildEnergyMonitoringAssetTree(roots);
    this.refreshDevices();
    this.expandedAssetIds = new Set(getEnergyDefaultExpandedAssetIds(this.assetTree));
    this.assetsLoading = false;
    if (this.selectedAssetId && this.assetTree.nodes.has(this.selectedAssetId)) {
      this.selectedAssetName = this.assetTree.nodes.get(this.selectedAssetId)?.name || '全部资产';
      this.selectedAssetIds = getEnergyMonitoringSelectedAssetIds(this.assetTree, this.selectedAssetId);
      this.loadStatistics();
    } else { this.selectRootAsset(this.selectedRootAssetId); }
    if (!initial) {
      this.expandedAssetIds = new Set([...expanded].filter((id) => this.assetTree.nodes.has(id)));
      this.page = Math.min(page, this.pageCount);
    }
  }

  /** 在线状态和告警状态独立计算；累计读数只消费规范键。 */
  private refreshDevices(): void {
    const contexts = this.liveSnapshot?.contexts || [];
    const alarms = new Set((this.liveSnapshot?.alarms || []).filter((alarm) => !alarm.cleared).map((alarm) => alarm.originator.id));
    this.devices = buildEnergyMonitoringDevices(this.assetTree, contexts, (id) => this.live.latest(id), alarms);
    const scope = this.scopedDevices;
    for (const period of this.periods) {
      const quality = this.ENERGY_MONITORING_QUALITY[period.key];
      quality.pointCount = scope.filter((device) => device.voltage !== null).length;
      const ids = new Set(scope.map((device) => device.id));
      const start = new Date(); start.setHours(0, 0, 0, 0);
      if (period.key === 'week') { start.setDate(start.getDate() - (start.getDay() + 6) % 7); }
      if (period.key === 'month') { start.setDate(1); }
      quality.abnormalCount = (this.liveSnapshot?.alarms || []).filter((alarm) => ids.has(alarm.originator.id) && alarm.createdTime >= start.getTime()).length;
    }
  }

  /** 周期用电和趋势复用能源后端；无额定负载/质量规则的指标保持未知。 */
  private loadStatistics(): void {
    this.statsRequest$.next();
    this.statisticsLoadError = false;
    forkJoin({energy: this.energyRegulationApiService.getEnergyConsumption(this.selectedAssetId),
      power: this.energyRegulationApiService.getPowerTrend(this.selectedAssetId)})
      .pipe(takeUntil(this.statsRequest$), takeUntil(this.destroy$)).subscribe({next: ({energy, power}) => {
        this.consumption = energy;
        for (const period of this.periods) {
          this.ENERGY_MONITORING_KPIS[period.key].totalEnergyKwh = energy.energyConsumptionByPeriod[period.key].valueKwh;
          this.ENERGY_MONITORING_KPIS[period.key].peakPowerKw = power.periodPeaks[period.key];
        }
        this.renderTrend();
      }, error: () => { this.statisticsLoadError = true; this.consumption = null;
        for (const period of this.periods) {this.ENERGY_MONITORING_KPIS[period.key].totalEnergyKwh = null; this.ENERGY_MONITORING_KPIS[period.key].peakPowerKw = null;}
        this.renderTrend();
      }});
  }

  /** 设备列表筛选变更后回到第一页，供模板搜索输入事件调用。 */
  resetDevicePage(): void {
    this.page = 1;
  }

  /** 更新用能趋势 ECharts 实例，不重复创建图表。 */
  private renderTrend(): void {
    if (!this.trendChart || this.trendChart.isDisposed()) {
      return;
    }
    const trend = this.consumption?.energySeries[this.trendPeriod] || {labels: [], values: []};
    const option: EChartsOption = {
      animationDuration: 220,
      graphic: trend.values.some((value) => value !== null) ? [] : [{type: 'text', left: 'center', top: 'middle', style: {text: '__', fill: '#71869a'}}],
      grid: {left: 42, right: 16, top: 20, bottom: 34},
      tooltip: {trigger: 'axis', valueFormatter: (value: unknown) => `${value} kWh`},
      xAxis: {type: 'category', data: trend.labels, boundaryGap: false, axisTick: {show: false},
        axisLine: {lineStyle: {color: '#cbd6e0'}}, axisLabel: {color: '#71869a', fontSize: 10, hideOverlap: true}},
      yAxis: {type: 'value', min: 0, name: 'kWh', nameTextStyle: {color: '#71869a', fontSize: 10},
        axisLabel: {color: '#71869a', fontSize: 10}, splitLine: {lineStyle: {color: '#e5ebf1'}}},
      series: [{type: 'line', name: '用电量', data: trend.values, smooth: true, symbol: 'circle', symbolSize: 4,
        lineStyle: {width: 2, color: '#2878df'}, itemStyle: {color: '#2878df'},
        areaStyle: {color: 'rgba(40, 120, 223, .08)'}}]
    };
    this.trendChart.setOption(option, true);
  }
}
