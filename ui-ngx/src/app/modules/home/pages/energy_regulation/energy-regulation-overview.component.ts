import {EnergyVisualFullscreenButtonComponent} from './shared/energy-visual-fullscreen-button.component';
/** 文件职责：加载用能概况根资产筛选项，并维护概况图表及交互状态。 */
import {Subject, forkJoin, takeUntil, auditTime} from 'rxjs';
import {EnergyRegulationLiveService} from './shared/energy-regulation-live.service';
import {CommonModule} from '@angular/common';
import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {SharedModule} from '@app/shared/shared.module';
import {EnergyRegulationApiService, EnergyRegulationRootAsset} from './energy-regulation-api.service';
import {EnergyRegulationPeriod} from './energy-regulation-dashboard.data';
import {
  buildEnergyOverviewSnapshot,
  formatEnergyOverviewChartValue,
  EnergyOverviewSnapshot
} from './energy-regulation-overview.data';

@Component({
  selector: 'tb-energy-regulation-overview',
  templateUrl: './energy-regulation-overview.component.html',
  styleUrls: ['./energy-regulation-overview.component.scss'],
  standalone: true,
  imports: [EnergyVisualFullscreenButtonComponent, CommonModule, FormsModule, SharedModule]
})
export class EnergyRegulationOverviewComponent implements OnInit, AfterViewInit, OnDestroy {
  /** 图表 DOM 建立后再初始化实例，切换页面时统一释放。 */
  @ViewChild('trendHost') trendHost?: ElementRef<HTMLDivElement>;
  @ViewChild('powerHost') powerHost?: ElementRef<HTMLDivElement>;

  /** 下拉选项来自当前用户有权查看的资产树根节点。 */
  rootAssets: EnergyRegulationRootAsset[] = [];
  rootAssetsLoading = true;
  rootAssetsLoadError = false;
  readonly periods: Array<{key: EnergyRegulationPeriod; label: string}> = [
    {key: 'day', label: '当日'}, {key: 'week', label: '当周'}, {key: 'month', label: '当月'}
  ];
  selectedRootAssetId = '';
  private appliedRootAssetId = '';
  selectedPeriod: EnergyRegulationPeriod = 'day';
  snapshot: EnergyOverviewSnapshot = buildEnergyOverviewSnapshot();

  loading = true;
  loadError = false;
  private readonly destroy$ = new Subject<void>();
  private readonly statsRequest$ = new Subject<void>();
  private trendChart?: ECharts;
  private powerChart?: ECharts;
  private resizeObserver?: ResizeObserver;

  constructor(private readonly energyRegulationApiService: EnergyRegulationApiService, private readonly live: EnergyRegulationLiveService) {}

  /** 页面启动时从能源监管接口读取根资产选项，不在前端硬编码企业名称。 */
  ngOnInit(): void {
    this.live.acquire();
    this.live.telemetryChanged$.pipe(auditTime(5000), takeUntil(this.destroy$)).subscribe(() => this.loadStatistics());
    this.applyFilters();
    this.energyRegulationApiService.getRootAssets().pipe(takeUntil(this.destroy$)).subscribe({
      next: (roots) => {
        this.rootAssets = roots || [];
        this.rootAssetsLoading = false;
      },
      error: () => {
        this.rootAssetsLoadError = true;
        this.rootAssetsLoading = false;
      }
    });
  }

  ngAfterViewInit(): void {
    if (!this.trendHost || !this.powerHost) {
      return;
    }
    this.trendChart = echarts.init(this.trendHost.nativeElement, undefined, {renderer: 'svg'});
    this.powerChart = echarts.init(this.powerHost.nativeElement, undefined, {renderer: 'svg'});
    this.renderCharts();
    this.resizeObserver = new ResizeObserver(() => {
      this.trendChart?.resize();
      this.powerChart?.resize();
    });
    this.resizeObserver.observe(this.trendHost.nativeElement);
    this.resizeObserver.observe(this.powerHost.nativeElement);
  }

  /** 服务端按公司子树聚合；取消上一请求，防止旧公司数据覆盖当前选择。 */
  applyFilters(): void {
    this.appliedRootAssetId = this.selectedRootAssetId;
    this.loadStatistics();
  }

  /** 实时刷新使用已提交的公司条件，不提前应用下拉框中的草稿。 */
  private loadStatistics(): void {
    this.statsRequest$.next();
    this.loading = true;
    this.loadError = false;
    forkJoin({energy: this.energyRegulationApiService.getEnergyConsumption(this.appliedRootAssetId),
      power: this.energyRegulationApiService.getPowerTrend(this.appliedRootAssetId)})
      .pipe(takeUntil(this.statsRequest$), takeUntil(this.destroy$)).subscribe({next: ({energy, power}) => {
        this.snapshot = buildEnergyOverviewSnapshot(energy, power); this.loading = false; this.renderCharts();
      }, error: () => {this.snapshot = buildEnergyOverviewSnapshot(); this.loading = false; this.loadError = true; this.renderCharts();}});
  }

  /** 清除根资产选择并恢复概况展示。 */
  resetFilters(): void {
    this.selectedRootAssetId = '';
    this.applyFilters();
  }

  /** 时间按钮只改变能耗趋势周期，不影响固定的环比和日功率曲线。 */
  selectPeriod(period: EnergyRegulationPeriod): void {
    this.selectedPeriod = period;
    this.renderTrend();
  }

  ngOnDestroy(): void {
    this.destroy$.next(); this.destroy$.complete(); this.statsRequest$.complete(); this.live.release();
    this.resizeObserver?.disconnect();
    this.trendChart?.dispose();
    this.powerChart?.dispose();
  }

  /** 按当前筛选结果更新两张图，不重复创建 ECharts 实例。 */
  private renderCharts(): void {
    this.renderTrend();
    this.renderPower();
  }

  /** 用能趋势按日、周、月切换横轴和数值。 */
  private renderTrend(): void {
    if (!this.trendChart || this.trendChart.isDisposed()) {
      return;
    }
    const series = this.snapshot.energySeries[this.selectedPeriod];
    const option: EChartsOption = {
      animationDuration: 250,
      // null和undefined均为缺数；仅有限数值（包括0）允许取消空图占位。
      graphic: series.values.some((value) => typeof value === 'number' && Number.isFinite(value))
        ? [] : [{type: 'text', left: 'center', top: 'middle', style: {text: '__', fill: '#71869a'}}],
      grid: {left: 52, right: 20, top: 28, bottom: 48},
      tooltip: {trigger: 'axis', valueFormatter: (value: unknown) => formatEnergyOverviewChartValue(value, 'kWh')},
      xAxis: {type: 'category', data: series.labels, boundaryGap: false, axisTick: {show: false},
        axisLine: {lineStyle: {color: '#d8e0e8'}}, axisLabel: {color: '#71869a', fontSize: 11, hideOverlap: true}},
      yAxis: {type: 'value', min: 0, name: 'kWh', nameTextStyle: {color: '#71869a', fontSize: 11},
        axisLabel: {color: '#71869a', fontSize: 11}, splitLine: {lineStyle: {color: '#e4eaf0'}}},
      series: [{type: 'line', name: '用电量', data: series.values, smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: {width: 3, color: '#195ce8'}, itemStyle: {color: '#195ce8'},
        areaStyle: {color: 'rgba(25, 92, 232, .10)'}}]
    };
    this.trendChart.setOption(option, true);
  }

  /** 日功率曲线同时展示今日和昨日小时平均总功率，峰值卡使用同一组真实数据。 */
  private renderPower(): void {
    if (!this.powerChart || this.powerChart.isDisposed()) {
      return;
    }
    const option: EChartsOption = {
      animationDuration: 250,
      graphic: [...this.snapshot.todayPower, ...this.snapshot.yesterdayPower]
        .some((value) => typeof value === 'number' && Number.isFinite(value))
        ? [] : [{type: 'text', left: 'center', top: 'middle', style: {text: '__', fill: '#71869a'}}],
      grid: {left: 52, right: 20, top: 24, bottom: 58},
      legend: {bottom: 4, itemWidth: 12, itemHeight: 8, textStyle: {color: '#53687c', fontSize: 11}},
      tooltip: {trigger: 'axis', valueFormatter: (value: unknown) => formatEnergyOverviewChartValue(value, 'kW')},
      xAxis: {type: 'category', data: this.snapshot.powerLabels, boundaryGap: false, axisTick: {show: false},
        axisLine: {lineStyle: {color: '#d8e0e8'}}, axisLabel: {color: '#71869a', fontSize: 11, hideOverlap: true}},
      yAxis: {type: 'value', min: 0, name: 'kW', nameTextStyle: {color: '#71869a', fontSize: 11},
        axisLabel: {color: '#71869a', fontSize: 11}, splitLine: {lineStyle: {color: '#e4eaf0'}}},
      series: [
        {type: 'line', name: '今日', data: this.snapshot.todayPower, smooth: true, symbol: 'circle', symbolSize: 5,
          lineStyle: {width: 2, color: '#195ce8'}, itemStyle: {color: '#195ce8'}},
        {type: 'line', name: '昨日', data: this.snapshot.yesterdayPower, smooth: true, symbol: 'circle', symbolSize: 5,
          lineStyle: {width: 2, color: '#16a65a'}, itemStyle: {color: '#16a65a'}}
      ]
    };
    this.powerChart.setOption(option, true);
  }
}
