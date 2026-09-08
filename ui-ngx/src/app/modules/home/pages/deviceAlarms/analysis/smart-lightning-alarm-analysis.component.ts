import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {Subject, takeUntil} from 'rxjs';
import {
  SmartLightningAlarmAnalysisSummary,
  SmartLightningMonitoringApiService
} from '../../smart-lightning/monitoring/smart-lightning-monitoring-api.service';
import {
  ALARM_ANALYSIS_SITE_ROWS,
  ALARM_ANALYSIS_TREND,
  ALARM_ANALYSIS_TYPE_ROWS,
  ALERT_ANALYSIS_PERIODS,
  AlarmAnalysisPeriod,
  AlarmAnalysisPeriodCard,
  buildAlarmAnalysisBars,
  getAlarmAnalysisRankTone
} from './smart-lightning-alarm-analysis.data';

@Component({
  selector: 'tb-smart-lightning-alarm-analysis',
  templateUrl: './smart-lightning-alarm-analysis.component.html',
  styleUrls: ['./smart-lightning-alarm-analysis.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmAnalysisComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('rankingChart') rankingChartHost?: ElementRef<HTMLDivElement>;
  @ViewChild('trendChart') trendChartHost?: ElementRef<HTMLDivElement>;

  periods: AlarmAnalysisPeriodCard[] = ALERT_ANALYSIS_PERIODS.map((period) => ({...period}));
  readonly siteRows = ALARM_ANALYSIS_SITE_ROWS;
  readonly trend = ALARM_ANALYSIS_TREND;
  readonly typeBars = buildAlarmAnalysisBars(ALARM_ANALYSIS_TYPE_ROWS);

  activePeriod: AlarmAnalysisPeriod = 'day';
  selectedRankingTab: 'site' | 'type' = 'site';
  summaryLoading = true;
  summaryError = false;

  private rankingChart?: ECharts;
  private trendChart?: ECharts;
  private resizeObserver?: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAlarmAnalysisSummary();
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

  setPeriod(period: AlarmAnalysisPeriod): void {
    this.activePeriod = period;
    this.renderTrendChart();
  }

  selectRankingTab(tab: 'site' | 'type'): void {
    this.selectedRankingTab = tab;
    this.renderRankingChart();
  }

  getAlarmAnalysisRankTone = getAlarmAnalysisRankTone;

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
        this.cd.markForCheck();
      }
    });
  }

  private mergePeriodSummary(period: AlarmAnalysisPeriodCard, summary: SmartLightningAlarmAnalysisSummary): AlarmAnalysisPeriodCard {
    const values = summary[period.key];
    return {
      ...period,
      total: values.currentCount,
      previous: values.previousCount,
      change: this.formatChangePercent(values.changePercent)
    };
  }

  private formatChangePercent(value: number): string {
    if (!Number.isFinite(value)) {
      return '--';
    }
    return `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
  }

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
