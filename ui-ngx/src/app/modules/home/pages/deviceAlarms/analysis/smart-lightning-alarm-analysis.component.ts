/** 文件职责：告警分析模块的 smart-lightning-alarm-analysis.component，负责页面状态管理、接口调用与用户交互。 */
import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {auditTime, Subject, takeUntil} from 'rxjs';
import {
  SmartLightningAlarmAnalysisSummary,
  SmartLightningAlarmDeviceCoverage,
  SmartLightningAlarmAnalysisOverview,
  SmartLightningAlarmSeverityTotal,
  SmartLightningAlarmTrendPoint,
  SmartLightningFocusAlarmDevice,
  SmartLightningMonitoringApiService
} from '../../smart-lightning/monitoring/smart-lightning-monitoring-api.service';
import {SmartLightningPeriod} from '../../smart-lightning/dashboard/smart-lightning-dashboard.data';
import {
  ALERT_ANALYSIS_PERIODS,
  AlarmAnalysisPeriod,
  AlarmAnalysisPeriodCard,
  AlarmAnalysisStatusRow,
  AlarmAnalysisSiteRow,
  buildAlarmAnalysisBars,
  calculateAlarmCoveragePercent,
  getAlarmAnalysisRankTone,
  mapAlarmAnalysisSeverityRows,
  mapAlarmAnalysisStatusRows,
  mapAlarmAnalysisSiteRows,
  mapAlarmAnalysisTypeRows
} from './smart-lightning-alarm-analysis.data';
import {SmartLightningRealtimeService} from '../../smart-lightning/shared/realtime/smart-lightning-realtime.service';

/**
 * 告警分析页面组件。
 *
 * <p>顶部周期统计卡、站点排名、告警类型排名、严重程度分布和告警趋势均接入告警分析接口。</p>
 */
@Component({
  selector: 'tb-smart-lightning-alarm-analysis',
  templateUrl: './smart-lightning-alarm-analysis.component.html',
  styleUrls: ['./smart-lightning-alarm-analysis.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmAnalysisComponent implements OnInit, AfterViewInit, OnDestroy {
  /** 告警趋势折线图容器。 */
  @ViewChild('trendChart') trendChartHost?: ElementRef<HTMLDivElement>;

  /** 周期统计卡配置；接口返回后填充真实统计值，接口完成前显示 0/-- 占位。 */
  periods: AlarmAnalysisPeriodCard[] = ALERT_ANALYSIS_PERIODS.map((period) => ({...period}));
  /** 当前日/周/月周期下全部设备直属父资产的告警排名行。 */
  siteRows: AlarmAnalysisSiteRow[] = [];
  /** 按告警数量降序整理的重点站点，仅用于分析页展示，不改变原始站点排序。 */
  riskSiteRows: AlarmAnalysisSiteRow[] = [];
  /** 后端按日/月/年返回的趋势时间桶。 */
  trendByPeriod: Record<AlarmAnalysisPeriod, SmartLightningAlarmTrendPoint[]> = {
    day: [],
    month: [],
    year: []
  };
  /** 当前仍未清除且严重等级为 CRITICAL 的重点设备。 */
  focusDevices: SmartLightningFocusAlarmDevice[] = [];
  /** 按告警类型聚合的数据；左侧排行切换到类型时使用。 */
  typeBars = buildAlarmAnalysisBars([]);
  /** 按严重程度聚合的数据；接口返回后映射为中文展示名称。 */
  severityRows = mapAlarmAnalysisSeverityRows([]);
  /** 按确认/清除状态聚合的当前周期告警记录。 */
  statusRows: AlarmAnalysisStatusRow[] = [];
  /** 当前周期的设备告警覆盖率统计。 */
  deviceCoverage: SmartLightningAlarmDeviceCoverage = {
    totalDevices: 0,
    alarmedDevices: 0,
    activeAlarmDevices: 0
  };

  /** 趋势图独立的日/月/年周期。 */
  trendPeriod: AlarmAnalysisPeriod = 'day';
  /** 站点排名独立的日/周/月周期，不与趋势图周期联动。 */
  sitePeriod: SmartLightningPeriod = SmartLightningPeriod.day;
  /** 告警类型排名独立的日/周/月周期。 */
  typePeriod: SmartLightningPeriod = SmartLightningPeriod.day;
  /** 严重程度分布独立的日/周/月周期。 */
  severityPeriod: SmartLightningPeriod = SmartLightningPeriod.day;
  /** 确认/清除状态分布独立的日/周/月周期。 */
  statusPeriod: SmartLightningPeriod = SmartLightningPeriod.day;
  /** 设备告警覆盖率独立的日/周/月周期。 */
  coveragePeriod: SmartLightningPeriod = SmartLightningPeriod.day;
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
  /** 类型排名加载状态。 */
  typeLoading = true;
  /** 类型排名加载失败状态。 */
  typeError = false;
  /** 严重程度分布加载状态。 */
  severityLoading = true;
  /** 严重程度分布加载失败状态。 */
  severityError = false;
  /** 状态分布组件加载状态。 */
  statusLoading = true;
  statusError = false;
  /** 设备覆盖率组件加载状态。 */
  coverageLoading = true;
  coverageError = false;

  private trendChart?: ECharts;
  /** 监听图表容器尺寸变化，容器伸缩时同步重绘。 */
  private resizeObserver?: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly realtimeService: SmartLightningRealtimeService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAlarmAnalysisSummary();
    this.loadSiteAlarmRanking();
    this.loadAlarmTypeRanking();
    this.loadAlarmSeverityRanking();
    this.loadAlarmAnalysisOverview(this.statusPeriod, true, false);
    this.loadAlarmAnalysisOverview(this.coveragePeriod, false, true);
    // 告警分析的数据全部来自告警聚合接口，只在告警事件发生时刷新。
    // 普通遥测变化不会改变告警统计，禁止因此重复执行整页历史聚合查询。
    this.realtimeService.alarmChanged$.pipe(
      auditTime(600),
      takeUntil(this.destroy$)
    ).subscribe(() => this.reloadAnalysisData());
    this.realtimeService.acquire();
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
    this.realtimeService.release();
    this.resizeObserver?.disconnect();
    this.trendChart?.dispose();
  }

  /** 按页面当前各自周期刷新全部分析区块，不改变用户当前选择的周期。 */
  private reloadAnalysisData(): void {
    this.loadAlarmAnalysisSummary();
    this.loadSiteAlarmRanking();
    this.loadAlarmTypeRanking();
    this.loadAlarmSeverityRanking();
    this.loadAlarmAnalysisOverview(this.statusPeriod, true, false);
    this.loadAlarmAnalysisOverview(this.coveragePeriod, false, true);
  }

  /** 切换趋势图周期，只重绘趋势组件，不影响其他周期选择。 */
  setTrendPeriod(period: AlarmAnalysisPeriod): void {
    this.trendPeriod = period;
    this.renderTrendChart();
  }

  /** 切换站点排名周期，同时刷新左侧排行和右侧周期分析模块。 */
  setSitePeriod(period: SmartLightningPeriod): void {
    if (period === this.sitePeriod) {
      return;
    }
    this.sitePeriod = period;
    this.loadSiteAlarmRanking();
  }

  /** 切换告警类型排名周期，只刷新类型排行，不影响其他组件。 */
  setTypePeriod(period: SmartLightningPeriod): void {
    if (period === this.typePeriod) {
      return;
    }
    this.typePeriod = period;
    this.loadAlarmTypeRanking();
  }

  /** 切换严重程度分布周期，只刷新严重程度组件。 */
  setSeverityPeriod(period: SmartLightningPeriod): void {
    if (period === this.severityPeriod) {
      return;
    }
    this.severityPeriod = period;
    this.loadAlarmSeverityRanking();
  }

  /** 切换状态分布周期，只刷新告警状态组件。 */
  setStatusPeriod(period: SmartLightningPeriod): void {
    if (period === this.statusPeriod) {
      return;
    }
    this.statusPeriod = period;
    this.loadAlarmAnalysisOverview(this.statusPeriod, true, false);
  }

  /** 切换设备覆盖率周期，只刷新覆盖率组件。 */
  setCoveragePeriod(period: SmartLightningPeriod): void {
    if (period === this.coveragePeriod) {
      return;
    }
    this.coveragePeriod = period;
    this.loadAlarmAnalysisOverview(this.coveragePeriod, false, true);
  }

  /** 切换左侧排行数据源（站点/类型）。 */
  selectRankingTab(tab: 'site' | 'type'): void {
    this.selectedRankingTab = tab;
  }

  getAlarmAnalysisRankTone = getAlarmAnalysisRankTone;

  /** 将后端返回的毫秒持续时长转换为紧凑中文文案。 */
  formatAlarmDuration(durationMs: number): string {
    const totalMinutes = Math.max(0, Math.floor(durationMs / 60_000));
    const days = Math.floor(totalMinutes / (24 * 60));
    const hours = Math.floor((totalMinutes % (24 * 60)) / 60);
    const minutes = totalMinutes % 60;
    if (days > 0) {
      return `${days}天${hours}小时${minutes}分`;
    }
    if (hours > 0) {
      return `${hours}小时${minutes}分`;
    }
    return `${minutes}分`;
  }

  /** 拉取日/月/年告警统计；接口失败时保持 0/-- 占位并标记错误状态。 */
  private loadAlarmAnalysisSummary(): void {
    this.summaryLoading = true;
    this.summaryError = false;
    this.monitoringApi.getLightningAlarmAnalysisSummary().pipe(takeUntil(this.destroy$)).subscribe({
      next: (summary) => {
        this.periods = this.periods.map((period) => this.mergePeriodSummary(period, summary));
        this.trendByPeriod = {
          day: summary.dayTrend || [],
          month: summary.monthTrend || [],
          year: summary.yearTrend || []
        };
        this.focusDevices = summary.focusDevices || [];
        this.summaryLoading = false;
        this.renderTrendChart();
        this.cd.markForCheck();
      },
      error: () => {
        this.summaryLoading = false;
        this.summaryError = true;
        this.periods = this.periods.map((period) => ({...period, total: 0, previous: 0, change: '--'}));
        this.trendByPeriod = {day: [], month: [], year: []};
        this.focusDevices = [];
        this.renderTrendChart();
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
        this.riskSiteRows = this.buildRiskSiteRows(this.siteRows);
        this.siteLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.siteRows = [];
        this.riskSiteRows = [];
        this.siteLoading = false;
        this.siteError = true;
        this.cd.markForCheck();
      }
    });
  }

  /** 从已返回的站点统计中提取告警数最高的三个重点站点。 */
  private buildRiskSiteRows(rows: readonly AlarmAnalysisSiteRow[]): AlarmAnalysisSiteRow[] {
    const sorted = [...rows];
    sorted.sort((left, right) => right.count - left.count
      || right.handledRate - left.handledRate
      || left.name.localeCompare(right.name, 'zh-CN'));
    return sorted.slice(0, 3);
  }

  /**
   * 统计重点设备所属站点数量，并按直属父资产名称去重。
   *
   * <p>重点设备由后端按“严重且未清除”告警筛选，站点统计必须基于这些设备的直属父资产，
   * 不能使用站点排行榜的行数，也不能让同一站点下的多台设备重复计数。</p>
   */
  get alarmSiteCount(): number {
    const siteNames = new Set<string>();
    for (const device of this.focusDevices) {
      const siteName = device.parentNodeName?.trim();
      if (siteName) {
        siteNames.add(siteName);
      }
    }
    return siteNames.size;
  }

  /** 当前站点统计中的最高处理率，仅用于展示分析摘要。 */
  get maxHandledRate(): number {
    let max = 0;
    for (const row of this.siteRows) {
      max = Math.max(max, row.handledRate);
    }
    return max;
  }

  /** 当前站点统计中的最低处理率，仅用于展示分析摘要。 */
  get minHandledRate(): number {
    if (!this.siteRows.length) {
      return 0;
    }
    let min = this.siteRows[0].handledRate;
    for (const row of this.siteRows) {
      min = Math.min(min, row.handledRate);
    }
    return min;
  }

  /** 查询当前周期内防雷设备的告警类型排行。 */
  private loadAlarmTypeRanking(): void {
    this.typeLoading = true;
    this.typeError = false;
    this.monitoringApi.getAlarmTypeRanking(this.typePeriod).pipe(takeUntil(this.destroy$)).subscribe({
      next: (items) => {
        this.typeBars = buildAlarmAnalysisBars(mapAlarmAnalysisTypeRows(items));
        this.typeLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.typeBars = [];
        this.typeLoading = false;
        this.typeError = true;
        this.cd.markForCheck();
      }
    });
  }

  /**
   * 查询告警状态分布和设备覆盖率。
   *
   * <p>两个组件复用同一个后端接口，但通过 updateStatus/updateCoverage 分别更新自己的状态，
   * 因此切换其中一个组件的日/周/月不会重置另一个组件。</p>
   */
  private loadAlarmAnalysisOverview(
    period: SmartLightningPeriod,
    updateStatus: boolean,
    updateCoverage: boolean
  ): void {
    if (updateStatus) {
      this.statusLoading = true;
      this.statusError = false;
    }
    if (updateCoverage) {
      this.coverageLoading = true;
      this.coverageError = false;
    }
    this.monitoringApi.getAlarmAnalysisOverview(period).pipe(takeUntil(this.destroy$)).subscribe({
      next: (overview: SmartLightningAlarmAnalysisOverview) => {
        if (updateStatus) {
          this.statusRows = mapAlarmAnalysisStatusRows(overview.statusDistribution || []);
          this.statusLoading = false;
        }
        if (updateCoverage) {
          this.deviceCoverage = overview.deviceCoverage || {
            totalDevices: 0,
            alarmedDevices: 0,
            activeAlarmDevices: 0
          };
          this.coverageLoading = false;
        }
        this.cd.markForCheck();
      },
      error: () => {
        if (updateStatus) {
          this.statusRows = [];
          this.statusLoading = false;
          this.statusError = true;
        }
        if (updateCoverage) {
          this.deviceCoverage = {totalDevices: 0, alarmedDevices: 0, activeAlarmDevices: 0};
          this.coverageLoading = false;
          this.coverageError = true;
        }
        this.cd.markForCheck();
      }
    });
  }

  /** 计算当前周期发生过告警的防雷设备占防雷设备总数的百分比。 */
  get alarmCoveragePercent(): number {
    return calculateAlarmCoveragePercent(
      this.deviceCoverage.alarmedDevices,
      this.deviceCoverage.totalDevices
    );
  }

  /** 查询当前周期内防雷设备按告警严重程度聚合的数量。 */
  private loadAlarmSeverityRanking(): void {
    this.severityLoading = true;
    this.severityError = false;
    this.monitoringApi.getAlarmSeverityRanking(this.severityPeriod).pipe(takeUntil(this.destroy$)).subscribe({
      next: (items: SmartLightningAlarmSeverityTotal[]) => {
        this.severityRows = mapAlarmAnalysisSeverityRows(items);
        this.severityLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.severityRows = [];
        this.severityLoading = false;
        this.severityError = true;
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

  /** 环比百分比展示文本：正数带 + 号，保留接口返回的小数位，非法值显示 --。 */
  private formatChangePercent(value: number): string {
    if (!Number.isFinite(value)) {
      return '--';
    }
    return `${value > 0 ? '+' : ''}${value}%`;
  }

  /** 初始化两张 ECharts 实例并注册容器尺寸监听。 */
  private initCharts(): void {
    if (this.trendChartHost?.nativeElement) {
      this.trendChart = echarts.init(this.trendChartHost.nativeElement, undefined, {renderer: 'svg'});
    }
    this.resizeObserver = new ResizeObserver(() => {
      this.trendChart?.resize();
    });
    if (this.trendChartHost?.nativeElement) {
      this.resizeObserver.observe(this.trendChartHost.nativeElement);
    }
    this.renderTrendChart();
  }

  /** 渲染告警趋势折线图；横轴和数量均来自当前周期的后端时间桶。 */
  private renderTrendChart(): void {
    if (!this.trendChart) {
      return;
    }
    const trend = this.trendByPeriod[this.trendPeriod];
    this.trendChart.setOption({
      grid: {left: 42, right: 26, top: 20, bottom: 32},
      tooltip: {trigger: 'axis', formatter: (params: any) => `${params[0].axisValue}<br/>告警数量：${params[0].value}`},
      xAxis: {type: 'category', data: trend.map((point) => point.label), boundaryGap: false, axisLine: {lineStyle: {color: '#bbc6cf'}}, axisLabel: {color: '#72879a'}},
      yAxis: {type: 'value', min: 0, splitLine: {lineStyle: {color: '#e4e9ef'}}, axisLabel: {color: '#72879a'}},
      series: [{
        name: '告警数量',
        type: 'line',
        data: trend.map((point) => point.count),
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: {width: 3, color: '#195ce8'},
        itemStyle: {color: '#17e96c', borderColor: '#195ce8', borderWidth: 2},
        areaStyle: {color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: 'rgba(25, 92, 232, .24)'},
          {offset: 1, color: 'rgba(25, 92, 232, .03)'}
        ])}
      }]
    } as EChartsOption, true);
  }
}
