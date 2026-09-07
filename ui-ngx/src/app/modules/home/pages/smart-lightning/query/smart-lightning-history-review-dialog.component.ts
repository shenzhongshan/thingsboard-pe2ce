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
  filterSmartLightningTelemetryParameters,
  getSmartLightningTelemetryParameterLabel,
  getSmartLightningTelemetryParameterUnit,
  SmartLightningHistoryPoint
} from './smart-lightning-history-review.data';

export interface SmartLightningHistoryReviewDialogData {
  deviceId: string;
  deviceName: string;
  deviceProfileName: string;
}

type HistoryRangePreset = '24h' | '7d' | '30d' | 'custom';
type QuickHistoryRangePreset = Exclude<HistoryRangePreset, 'custom'>;

const HISTORY_RANGE_HOURS: Record<QuickHistoryRangePreset, number> = {
  '24h': 24,
  '7d': 24 * 7,
  '30d': 24 * 30
};

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

  private chart?: ECharts;
  private resizeObserver?: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: SmartLightningHistoryReviewDialogData,
    private readonly dialogRef: MatDialogRef<SmartLightningHistoryReviewDialogComponent>,
    private readonly entityService: EntityService,
    private readonly attributeService: AttributeService,
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
    this.destroy$.next();
    this.destroy$.complete();
    this.resizeObserver?.disconnect();
    this.chart?.dispose();
  }

  get selectedParameterLabel(): string {
    return this.selectedParameter
      ? getSmartLightningTelemetryParameterLabel(this.data.deviceProfileName, this.selectedParameter)
      : '设备参数';
  }

  parameterLabel(key: string): string {
    return getSmartLightningTelemetryParameterLabel(this.data.deviceProfileName, key);
  }

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

  markCustomRange(): void {
    this.rangePreset = 'custom';
  }

  setEndNow(): void {
    this.endTime = this.toLocalDateTime(Date.now());
    this.rangePreset = 'custom';
    this.rangeError = '';
  }

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
    this.historyLoading = true;
    this.historyLoadError = false;
    this.historyLoaded = false;
    this.points = [];
    this.renderChart();
    this.attributeService.getEntityTimeseries(
      new DeviceId(this.data.deviceId),
      [this.selectedParameter],
      startTs,
      endTs,
      1000,
      AggregationType.NONE,
      undefined,
      DataSortOrder.ASC,
      true
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: (timeseries: TimeseriesData) => {
        this.points = buildSmartLightningHistoryPoints(timeseries, this.selectedParameter);
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

  private loadParameterKeys(): void {
    this.keysLoading = true;
    this.keysLoadError = false;
    this.entityService.getEntityKeys(
      new DeviceId(this.data.deviceId),
      '',
      DataKeyType.timeseries
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: (keys) => {
        this.parameterKeys = filterSmartLightningTelemetryParameters(this.data.deviceProfileName, keys)
          .sort((left, right) => left.localeCompare(right));
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

  private renderChart(): void {
    if (!this.chart) {
      return;
    }
    const unit = getSmartLightningTelemetryParameterUnit(this.data.deviceProfileName, this.selectedParameter);
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

  private toLocalDateTime(timestamp: number): string {
    const date = new Date(timestamp);
    const pad = (value: number): string => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T` +
      `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}
