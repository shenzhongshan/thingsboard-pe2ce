/** 文件职责：告警分析模块的 smart-lightning-alarm-analysis.component，负责页面状态管理、接口调用与用户交互。 */
import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {Subject, takeUntil} from 'rxjs';
import {
  SmartLightningAlarmAnalysisSummary,
  SmartLightningMonitoringApiService
} from '../../smart-lightning/monitoring/smart-lightning-monitoring-api.service';
import {SmartLightningPeriod} from '../../smart-lightning/dashboard/smart-lightning-dashboard.data';
import {
  ALARM_ANALYSIS_TREND,
  ALARM_ANALYSIS_TYPE_ROWS,
  ALERT_ANALYSIS_PERIODS,
  AlarmAnalysisPeriod,
  AlarmAnalysisPeriodCard,
  AlarmAnalysisSiteRow,
  buildAlarmAnalysisBars,
  getAlarmAnalysisRankTone,
  mapAlarmAnalysisSiteRows
} from './smart-lightning-alarm-analysis.data';

/**
 * 告警分析页面组件。
 *
 * <p>顶部周期统计卡和设备直属父资产站点排名接入真实接口；类型排名与趋势图暂保留演示数据。</p>
 */
@Component({
  selector: 'tb-smart-lightning-alarm-analysis',
  templateUrl: './smart-lightning-alarm-analysis.component.html',
  styleUrls: ['./smart-lightning-alarm-analysis.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmAnalysisComponent implements OnInit, AfterViewInit, OnDestroy {
  /** 排名榜横向条形图容器。 */
  @ViewChild('rankingChart') rankingChartHost?: ElementRef<HTMLDivElement>;
  /** 趋势柱状图容器。 */
  @ViewChild('trendChart') trendChartHost?: ElementRef<HTMLDivElement>;

  /** 周期统计卡配置；接口返回后填充真实统计值，接口完成前显示 0/-- 占位。 */
  periods: AlarmAnalysisPeriodCard[] = ALERT_ANALYSIS_PERIODS.map((period) => ({...period}));
  /** 当前日/周/月周期下全部设备直属父资产的告警排名行。 */
  siteRows: AlarmAnalysisSiteRow[] = [];
  /** 告警趋势演示数据。 */
  readonly trend = ALARM_ANALYSIS_TREND;
  /** 按告警类型聚合的条形数据（演示数据）。 */
  readonly typeBars = buildAlarmAnalysisBars(ALARM_ANALYSIS_TYPE_ROWS);

  /** 趋势图当前统计周期（日/月/年）。 */
  activePeriod: AlarmAnalysisPeriod = 'day';
  /** 站点排名独立的日/周/月周期，不与趋势图周期联动。 */
  sitePeriod: SmartLightningPeriod = SmartLightningPeriod.day;
  /** 排名榜当前页签：site=资产排名，type=类型排名。 */
  selectedRankingTab: 'site' | 'type' = 'site';
  /** 顶部统计加载中标记。 */
  summaryLoading = true;
  /** 顶部统计加载失败标记。 */
  summaryError = false;
  /** 站点排名加载状态。 */
  siteLoading = true;
  /** 站点排名加载失败状态。 */
  siteError = false;

  private rankingChart?: ECharts;
  private trendChart?: ECharts;
  /** 监听图表容器尺寸变化，容器伸缩时同步重绘。 */
  private resizeObserver?: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAlarmAnalysisSummary();
    this.loadSiteAlarmRanking();
  }

  ngAfterViewInit(): void {
    // 等待 Angular 完成首帧布局后初始化 ECharts，避免容器宽度为 0。
    queueMicrotask(() => {
      this.initCharts();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.resizeObserver?.disconnect();
    this.rankingChart?.dispose();
    this.trendChart?.dispose();
  }

  /** 切换趋势图统计周期并重绘。 */
  setPeriod(period: AlarmAnalysisPeriod): void {
    this.activePeriod = period;
    this.renderTrendChart();
  }

  /** 切换站点排名周期，仅刷新站点表格和站点排行榜柱状图。 */
  setSitePeriod(period: SmartLightningPeriod): void {
    if (period === this.sitePeriod) {
      return;
    }
    this.sitePeriod = period;
    this.loadSiteAlarmRanking();
  }

  /** 切换排名榜数据源（资产/类型）并重绘条形图。 */
  selectRankingTab(tab: 'site' | 'type'): void {
    this.selectedRankingTab = tab;
    this.renderRankingChart();
  }

  getAlarmAnalysisRankTone = getAlarmAnalysisRankTone;

  /** 拉取日/月/年告警统计；接口失败时保持 0/-- 占位并标记错误状态。 */
  private loadAlarmAnalysisSummary(): void {
    this.summaryLoading = true;
    this.summaryError = false;
    this.monitoringApi.getLightningAlarmAnalysisSummary().pipe(takeUntil(this.destroy$)).subscribe({
      next: (summary) => {
        this.periods = this.periods.map((period) => this.mergePeriodSummary(period, summary));
        this.summaryLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.summaryLoading = false;
        this.summaryError = true;
        this.periods = this.periods.map((period) => ({...period, total: 0, previous: 0, change: '--'}));
        this.cd.markForCheck();
      }
    });
  }

  /** 查询当前用户可见树中所有设备直属父资产的告警统计。 */
  private loadSiteAlarmRanking(): void {
    this.siteLoading = true;
    this.siteError = false;
    this.monitoringApi.getLeafAssetAlarmCounts(this.sitePeriod).pipe(takeUntil(this.destroy$)).subscribe({
      next: (items) => {
        this.siteRows = mapAlarmAnalysisSiteRows(items);
        this.siteLoading = false;
        this.renderRankingChart();
        this.cd.markForCheck();
      },
      error: () => {
        this.siteRows = [];
        this.siteLoading = false;
        this.siteError = true;
        this.renderRankingChart();
        this.cd.markForCheck();
      }
    });
  }

  /** 把接口返回的某个周期统计合并进对应的统计卡配置。 */
  private mergePeriodSummary(period: AlarmAnalysisPeriodCard, summary: SmartLightningAlarmAnalysisSummary): AlarmAnalysisPeriodCard {
    const values = summary[period.key];
    return {
      ...period,
      total: values.currentCount,
      previous: values.previousCount,
      change: this.formatChangePercent(values.changePercent)
    };
  }

  /** 环比百分比展示文本：正数带 + 号，非法值显示 --。 */
  private formatChangePercent(value: number): string {
    if (!Number.isFinite(value)) {
      return '--';
    }
    return `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
  }

  /** 初始化两张 ECharts 实例并注册容器尺寸监听。 */
  private initCharts(): void {
    if (this.rankingChartHost?.nativeElement) {
      this.rankingChart = echarts.init(this.rankingChartHost.nativeElement, undefined, {renderer: 'svg'});
    }
    if (this.trendChartHost?.nativeElement) {
      this.trendChart = echarts.init(this.trendChartHost.nativeElement, undefined, {renderer: 'svg'});
    }
    this.resizeObserver = new ResizeObserver(() => {
      this.rankingChart?.resize();
      this.trendChart?.resize();
    });
    if (this.rankingChartHost?.nativeElement) {
      this.resizeObserver.observe(this.rankingChartHost.nativeElement);
    }
    if (this.trendChartHost?.nativeElement) {
      this.resizeObserver.observe(this.trendChartHost.nativeElement);
    }
    this.renderRankingChart();
    this.renderTrendChart();
  }

  /** 渲染排名榜横向条形图，按当前页签在资产排名与类型排名间切换。 */
  private renderRankingChart(): void {
    if (!this.rankingChart) {
      return;
    }
    const bars = this.selectedRankingTab === 'site'
      ? buildAlarmAnalysisBars(this.siteRows.map((row) => ({name: row.name, count: row.count})))
      : this.typeBars;
    this.rankingChart.setOption({
      grid: {left: 112, right: 28, top: 14, bottom: 24},
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}, formatter: (params: any) => `${params[0].name}<br/>告警数：${params[0].value}`},
      xAxis: {type: 'value', min: 0, splitLine: {lineStyle: {color: '#e4e9ef'}}, axisLabel: {color: '#72879a'}},
      yAxis: {type: 'category', inverse: true, data: bars.map((bar) => bar.name), axisLabel: {color: '#3d566b', width: 92, overflow: 'truncate'}},
      series: [{
        type: 'bar',
        data: bars.map((bar) => bar.count),
        barMaxWidth: 18,
        showBackground: true,
        backgroundStyle: {color: '#eef2f5', borderRadius: 9},
        itemStyle: {borderRadius: [0, 9, 9, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          {offset: 0, color: '#087d38'},
          {offset: 1, color: '#17e96c'}
        ])},
        label: {show: true, position: 'right', color: '#3d566b', fontSize: 12}
      }]
    } as EChartsOption, true);
  }

  /** 渲染趋势柱状图；演示数据按周期倍率放大，保证不同周期视觉上有区分。 */
  private renderTrendChart(): void {
    if (!this.trendChart) {
      return;
    }
    const multiplier = this.activePeriod === 'day' ? 1 : this.activePeriod === 'month' ? 2 : 3;
    this.trendChart.setOption({
      grid: {left: 42, right: 26, top: 20, bottom: 32},
      tooltip: {trigger: 'axis', formatter: (params: any) => `${params[0].axisValue}<br/>告警数量：${params[0].value}`},
      xAxis: {type: 'category', data: this.trend.labels, boundaryGap: true, axisLine: {lineStyle: {color: '#bbc6cf'}}, axisLabel: {color: '#72879a'}},
      yAxis: {type: 'value', min: 0, splitLine: {lineStyle: {color: '#e4e9ef'}}, axisLabel: {color: '#72879a'}},
      series: [{
        name: '告警数量',
        type: 'bar',
        data: this.trend.values.map((value) => value * multiplier),
        barMaxWidth: 34,
        itemStyle: {borderRadius: [8, 8, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: '#17e96c'},
          {offset: 1, color: '#083b78'}
        ])}
      }]
    } as EChartsOption, true);
  }
}
