/** 文件职责：数据查询模块的 smart-lightning-history-review-dialog.component，负责页面状态管理、接口调用与用户交互。 */
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Inject,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {AttributeService} from '@core/http/attribute.service';
import {EntityService} from '@core/http/entity.service';
import {DeviceId} from '@shared/models/id/device-id';
import {DataKeyType, DataSortOrder, TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {AggregationType} from '@shared/models/time/time.models';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import {Subject, takeUntil} from 'rxjs';
import {
  buildSmartLightningHistoryPoints,
  buildSmartLightningWaveformHistoryPage,
  SmartLightningWaveformHistoryRecord,
  filterSmartLightningTelemetryParameters,
  getSmartLightningHistoryYAxisRange,
  getSmartLightningTelemetryParameterLabel,
  getSmartLightningTelemetryParameterUnit,
  SmartLightningHistoryMetricDefinition,
  SmartLightningHistoryPoint
} from './smart-lightning-history-review.data';
import {buildSmartLightningWaveformChartOption, getSmartLightningChartDefinitions,
  SmartLightningChartDefinition} from '../monitoring/smart-lightning-device-detail.charts';
import {SMART_LIGHTNING_WAVEFORM_KEY} from '../shared/telemetry/smart-lightning-measurements';
import {SmartLightningWaveformHistoryApiService} from './smart-lightning-waveform-history-api.service';

export interface SmartLightningHistoryReviewDialogData {
  deviceId: string;
  deviceName: string;
  deviceProfileName: string;
  /** 非防雷页面可传入本模块已确认的测点表；不传时沿用防雷型号定义。 */
  metricDefinitions?: readonly SmartLightningHistoryMetricDefinition[];
}

type HistoryRangePreset = '24h' | '7d' | '30d' | 'custom';
type QuickHistoryRangePreset = Exclude<HistoryRangePreset, 'custom'>;

const HISTORY_RANGE_HOURS: Record<QuickHistoryRangePreset, number> = {
  '24h': 24,
  '7d': 24 * 7,
  '30d': 24 * 30
};

/**
 * 历史遥测“回顾”弹窗组件。
 *
 * <p>单选遥测参数，按起止时间查询数值趋势或逐次触发波形并绘制 ECharts 折线；
 * 不影响看板各图表独立的日/周/月筛选。</p>
 */
@Component({
  selector: 'tb-smart-lightning-history-review-dialog',
  templateUrl: './smart-lightning-history-review-dialog.component.html',
  styleUrls: ['./smart-lightning-history-review-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningHistoryReviewDialogComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('historyChart') historyChart?: ElementRef<HTMLDivElement>;

  parameterKeys: string[] = [];
  selectedParameter = '';
  startTime = this.toLocalDateTime(Date.now() - 24 * 60 * 60 * 1000);
  endTime = this.toLocalDateTime(Date.now());
  rangePreset: HistoryRangePreset = '24h';
  keysLoading = true;
  keysLoadError = false;
  historyLoading = false;
  historyLoadError = false;
  historyLoaded = false;
  rangeError = '';
  points: SmartLightningHistoryPoint[] = [];

  /** 历史波形独立按事件翻页；切换记录仅重画本地采样点。 */
  waveformRecords: SmartLightningWaveformHistoryRecord[] = [];
  selectedWaveform?: SmartLightningWaveformHistoryRecord;
  waveformHasMore = false;
  waveformLoadingMore = false;
  waveformPageError = false;
  appliedRange?: {startTs: number; endTs: number};
  private waveformNextEndTs?: number;
  private readonly waveformPageSize = 100;
  /** 新查询或参数切换时取消旧请求，避免旧结果覆盖当前选择。 */
  private readonly historyRequest$ = new Subject<void>();
  private chart?: ECharts;
  private resizeObserver?: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: SmartLightningHistoryReviewDialogData,
    private readonly dialogRef: MatDialogRef<SmartLightningHistoryReviewDialogComponent>,
    private readonly entityService: EntityService,
    private readonly attributeService: AttributeService,
    private readonly waveformApi: SmartLightningWaveformHistoryApiService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadParameterKeys();
  }

  ngAfterViewInit(): void {
    if (!this.historyChart) {
      return;
    }
    this.chart = echarts.init(this.historyChart.nativeElement, undefined, {renderer: 'svg'});
    this.resizeObserver = new ResizeObserver(() => this.chart?.resize());
    this.resizeObserver.observe(this.historyChart.nativeElement);
    this.renderChart();
  }

  ngOnDestroy(): void {
    this.historyRequest$.next();
    this.historyRequest$.complete();
    this.destroy$.next();
    this.destroy$.complete();
    this.resizeObserver?.disconnect();
    this.chart?.dispose();
  }

  /** 只给防雷 M1AI/L2AI 开放历史波形，外部模块显式测点表不受影响。 */
  get waveformDefinition(): SmartLightningChartDefinition | undefined {
    return this.data.metricDefinitions === undefined
      ? getSmartLightningChartDefinitions(this.data.deviceProfileName).find((definition) => definition.kind === 'waveform')
      : undefined;
  }

  get isWaveform(): boolean {
    return this.selectedParameter === SMART_LIGHTNING_WAVEFORM_KEY && !!this.waveformDefinition;
  }

  /** 参数切换后立即应用当前时间范围，同时取消上一项查询。 */
  onParameterChange(): void {
    this.historyRequest$.next();
    this.historyLoading = false;
    this.waveformLoadingMore = false;
    this.historyLoaded = false;
    this.historyLoadError = false;
    this.points = [];
    this.waveformRecords = [];
    this.selectedWaveform = undefined;
    this.renderChart();
    this.loadHistory();
  }

  get selectedParameterLabel(): string {
    return this.selectedParameter
      ? getSmartLightningTelemetryParameterLabel(
        this.data.deviceProfileName, this.selectedParameter, this.data.metricDefinitions
      )
      : '设备参数';
  }

  /** 遥测参数展示文本：配置过映射的显示中文业务名并保留原始键。 */
  parameterLabel(key: string): string {
    return getSmartLightningTelemetryParameterLabel(this.data.deviceProfileName, key, this.data.metricDefinitions);
  }

  /** 关闭弹窗，断开图表尺寸监听。 */
  close(): void {
    this.dialogRef.close();
  }

  /** 快捷范围只更新本弹窗的草稿时间，不会联动其他组件。 */
  setRangePreset(preset: QuickHistoryRangePreset): void {
    const hours = HISTORY_RANGE_HOURS[preset];
    const endTimestamp = Date.now();
    this.startTime = this.toLocalDateTime(endTimestamp - hours * 60 * 60 * 1000);
    this.endTime = this.toLocalDateTime(endTimestamp);
    this.rangePreset = preset;
    this.rangeError = '';
  }

  /** 手动修改时间后标记为自定义范围，退出快捷区间高亮。 */
  markCustomRange(): void {
    this.rangePreset = 'custom';
  }

  /** 把结束时间快捷设置为当前时刻。 */
  setEndNow(): void {
    this.endTime = this.toLocalDateTime(Date.now());
    this.rangePreset = 'custom';
    this.rangeError = '';
  }

  /** 校验时间范围后调用历史遥测接口，构造折线数据并渲染；含加载/空数据/失败状态。 */
  loadHistory(): void {
    this.rangeError = '';
    const startTs = new Date(this.startTime).getTime();
    const endTs = new Date(this.endTime).getTime();
    if (!this.selectedParameter) {
      return;
    }
    if (!Number.isFinite(startTs) || !Number.isFinite(endTs) || startTs >= endTs) {
      this.rangeError = '结束时间必须晚于开始时间';
      this.cd.markForCheck();
      return;
    }
    this.historyRequest$.next();
    const parameter = this.selectedParameter;
    this.appliedRange = {startTs, endTs};
    this.waveformRecords = [];
    this.selectedWaveform = undefined;
    this.waveformHasMore = false;
    this.waveformLoadingMore = false;
    this.waveformPageError = false;
    this.waveformNextEndTs = undefined;
    this.historyLoading = true;
    this.historyLoadError = false;
    this.historyLoaded = false;
    this.points = [];
    this.renderChart();
    if (this.isWaveform) {
      this.loadWaveformPage(false);
      return;
    }
    this.attributeService.getEntityTimeseries(
      new DeviceId(this.data.deviceId),
      [parameter],
      startTs,
      endTs,
      1000,
      AggregationType.NONE,
      undefined,
      DataSortOrder.ASC,
      true
    ).pipe(takeUntil(this.historyRequest$), takeUntil(this.destroy$)).subscribe({
      next: (timeseries: TimeseriesData) => {
        this.points = buildSmartLightningHistoryPoints(timeseries, parameter);
        this.historyLoading = false;
        this.historyLoaded = true;
        this.renderChart();
        this.cd.markForCheck();
      },
      error: () => {
        this.points = [];
        this.historyLoading = false;
        this.historyLoadError = true;
        this.historyLoaded = true;
        this.renderChart();
        this.cd.markForCheck();
      }
    });
  }

  /** 点击一条上报记录，直接使用完整 WF 采样点绘图，首点固定为 0 μs。 */
  selectWaveform(record: SmartLightningWaveformHistoryRecord): void {
    this.selectedWaveform = record;
    this.renderChart();
    this.cd.markForCheck();
  }

  /** 沿已提交的时间范围加载更早事件，编辑中的筛选时间不会改变分页游标。 */
  loadMoreWaveforms(): void {
    if (this.isWaveform && this.waveformHasMore && !this.waveformLoadingMore && !this.historyLoading) {
      this.loadWaveformPage(true);
    }
  }

  /** 每次最多读取一页原始事件，多读一条判断后续页，失败时保留已加载记录供重试。 */
  private loadWaveformPage(append: boolean): void {
    const definition = this.waveformDefinition;
    const range = this.appliedRange;
    if (!definition || !range) {
      return;
    }
    const endTs = append ? this.waveformNextEndTs : range.endTs;
    if (endTs === undefined || endTs <= range.startTs) {
      this.waveformHasMore = false;
      return;
    }
    this.waveformLoadingMore = append;
    this.waveformPageError = false;
    this.waveformApi.getHistory(this.data.deviceId, range.startTs, endTs, this.waveformPageSize + 1).pipe(
      takeUntil(this.historyRequest$), takeUntil(this.destroy$)
    ).subscribe({
      next: (timeseries) => {
        const page = buildSmartLightningWaveformHistoryPage(timeseries, definition, this.waveformPageSize);
        this.waveformRecords = append ? [...this.waveformRecords, ...page.records] : page.records;
        this.waveformNextEndTs = page.nextEndTs;
        this.waveformHasMore = page.hasMore;
        if (!append) {
          this.selectedWaveform = this.waveformRecords[0];
        }
        this.historyLoading = false;
        this.waveformLoadingMore = false;
        this.historyLoaded = true;
        this.renderChart();
        this.cd.markForCheck();
      },
      error: () => {
        this.historyLoading = false;
        this.waveformLoadingMore = false;
        this.historyLoadError = !append;
        this.waveformPageError = append;
        this.historyLoaded = true;
        this.cd.markForCheck();
      }
    });
  }

  /** 读取设备可用遥测键，并按“数值参数优先”过滤出可回顾的参数列表。 */
  private loadParameterKeys(): void {
    this.keysLoading = true;
    this.keysLoadError = false;
    this.entityService.getEntityKeys(
      new DeviceId(this.data.deviceId),
      '',
      DataKeyType.timeseries
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: (keys) => {
        this.parameterKeys = filterSmartLightningTelemetryParameters(
          this.data.deviceProfileName, keys, this.data.metricDefinitions
        )
          .sort((left, right) => left.localeCompare(right));
        // 尚未触发过的设备也保留历史波形入口，查询后显示真实空状态。
        if (this.waveformDefinition) {
          this.parameterKeys = [SMART_LIGHTNING_WAVEFORM_KEY,
            ...this.parameterKeys.filter((key) => key !== SMART_LIGHTNING_WAVEFORM_KEY)];
        }
        this.selectedParameter = this.parameterKeys[0] || '';
        this.keysLoading = false;
        this.cd.markForCheck();
        if (this.selectedParameter) {
          this.loadHistory();
        }
      },
      error: () => {
        this.parameterKeys = [];
        this.keysLoading = false;
        this.keysLoadError = true;
        this.cd.markForCheck();
      }
    });
  }

  /** 渲染/更新历史折线图，容器尺寸变化时同步重绘。 */
  private renderChart(): void {
    if (!this.chart) {
      return;
    }
    if (this.isWaveform) {
      this.chart.setOption(buildSmartLightningWaveformChartOption(
        this.waveformDefinition!, this.selectedWaveform?.seriesData || []
      ), true);
      return;
    }
    const unit = getSmartLightningTelemetryParameterUnit(
      this.data.deviceProfileName, this.selectedParameter, this.data.metricDefinitions
    );
    const yAxisRange = getSmartLightningHistoryYAxisRange(this.points);
    const option: EChartsOption = {
      animationDuration: 250,
      color: ['#7147ff'],
      tooltip: {trigger: 'axis'},
      legend: {
        top: 8,
        data: this.selectedParameter ? [this.selectedParameterLabel] : [],
        textStyle: {color: '#536b7f', fontSize: 12}
      },
      grid: {top: 58, left: 62, right: 34, bottom: 54},
      xAxis: {
        type: 'time',
        axisLine: {lineStyle: {color: '#bac7d1'}},
        axisTick: {show: false},
        axisLabel: {color: '#657b8e', fontSize: 11},
        splitLine: {show: true, lineStyle: {color: '#e2e8ed'}}
      },
      yAxis: {
        type: 'value',
        min: yAxisRange.min,
        max: yAxisRange.max,
        scale: true,
        name: unit,
        nameTextStyle: {color: '#657b8e'},
        axisLabel: {color: '#657b8e', fontSize: 11},
        splitLine: {lineStyle: {color: '#e2e8ed'}}
      },
      series: [{
        name: this.selectedParameterLabel,
        type: 'line',
        smooth: true,
        showSymbol: this.points.length < 30,
        symbolSize: 6,
        data: this.points,
        lineStyle: {width: 2.5},
        areaStyle: {color: 'rgba(113, 71, 255, .08)'}
      }]
    };
    this.chart.setOption(option, true);
  }

  /** 把时间戳转为 datetime-local 输入框使用的本地时间文本。 */
  private toLocalDateTime(timestamp: number): string {
    const date = new Date(timestamp);
    const pad = (value: number): string => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T` +
      `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }
}
