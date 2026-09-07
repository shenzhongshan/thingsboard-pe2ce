import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import { AlarmService } from '@core/http/alarm.service';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import * as echarts from 'echarts';
import { ECharts, EChartsOption } from 'echarts';
import L from 'leaflet';
import { catchError, forkJoin, map, Observable, of, Subject, switchMap, takeUntil, tap } from 'rxjs';
import { AlarmInfo, AlarmSearchStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope, AlarmCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { MAX_SAFE_PAGE_SIZE } from '@shared/models/page/page-link';
import {
  buildSmartLightningMock,
  aggregateSmartLightningActiveAlarms,
  calculateSmartLightningDeviceSummary,
  compareSmartLightningAlarms,
  createSmartLightningAlarmQuery,
  defaultSmartLightningChartPeriods,
  setSmartLightningChartPeriod,
  SmartLightningChartId,
  SmartLightningChartPeriods,
  SmartLightningAlarmRow,
  SmartLightningAlarmTypeTotal,
  SmartLightningDeviceSummary,
  SMART_LIGHTNING_ALARM_DISPLAY_LIMIT,
  SmartLightningMock,
  SmartLightningPeriod,
  formatSmartLightningAlarm,
  getSmartLightningParentDirectory,
  getSmartLightningProductName,
  getSmartLightningAlarmChartMessage,
  shouldReloadSmartLightningAlarms,
} from './smart-lightning-dashboard.data';
import { smartLightningTianDiTuMapConfig } from './smart-lightning-map.config';
import { SmartLightningMonitoringApiService } from '../monitoring/smart-lightning-monitoring-api.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';

interface StationMarker {
  name: string;
  status: 'normal' | 'warning' | 'critical';
  position: L.LatLngExpression;
  detail: string;
}

@Component({
  selector: 'tb-smart-lightning-dashboard',
  templateUrl: './smart-lightning-dashboard.component.html',
  styleUrls: ['./smart-lightning-dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningDashboardComponent implements AfterViewInit, OnDestroy {

  readonly markers: StationMarker[] = [
    {name: '防雷监测器', status: 'normal', position: [22.54, 114.05], detail: '状态正常 · 3 车道配电箱'},
    {name: '配电箱 A', status: 'critical', position: [22.48, 114.02], detail: '接地异常 · 请优先处理'},
    {name: '发电厂', status: 'warning', position: [22.51, 114.11], detail: 'SPD 脱扣 · 等待派单'}
  ];

  @ViewChild('mapHost') mapHost!: ElementRef<HTMLDivElement>;
  @ViewChildren('chartHost') chartHosts!: QueryList<ElementRef<HTMLDivElement>>;

  data: SmartLightningMock = buildSmartLightningMock(SmartLightningPeriod.day);
  chartPeriods: SmartLightningChartPeriods = {...defaultSmartLightningChartPeriods};
  alarms: SmartLightningAlarmRow[] = [];
  alarmsLoading = true;
  alarmsLoadError = false;
  deviceSummary: SmartLightningDeviceSummary = calculateSmartLightningDeviceSummary([]);
  devicesLoading = true;
  devicesLoadError = false;
  activeAlarmCount: number | null = null;
  activeAlarmTypeTotals: SmartLightningAlarmTypeTotal[] = [];

  private readonly charts = new Map<string, ECharts>();
  private readonly destroy$ = new Subject<void>();
  private map?: L.Map;
  private resizeObserver?: ResizeObserver;
  private alarmRealtimeSubscriber?: TelemetrySubscriber;
  private realtimeAlarmCount: number | null = null;
  private initialAlarmLoadFinished = false;

  constructor(
    private readonly cd: ChangeDetectorRef,
    private readonly alarmService: AlarmService,
    private readonly attributeService: AttributeService,
    private readonly deviceService: DeviceService,
    private readonly entityRelationService: EntityRelationService,
    private readonly monitoringApi: SmartLightningMonitoringApiService,
    private readonly telemetryWebsocketService: TelemetryWebsocketService,
    private readonly ngZone: NgZone
  ) {}

  ngAfterViewInit(): void {
    this.initializeMap();
    this.renderCharts();
    this.loadLatestAlarms();
    this.subscribeToAlarmCount();
    this.loadDeviceSummary();
    this.resizeObserver = new ResizeObserver(() => this.charts.forEach(chart => chart.resize()));
    this.chartHosts.forEach(host => this.resizeObserver.observe(host.nativeElement));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.alarmRealtimeSubscriber?.unsubscribe();
    this.resizeObserver?.disconnect();
    this.charts.forEach(chart => chart.dispose());
    this.charts.clear();
    this.map?.remove();
  }

  setChartPeriod(chart: SmartLightningChartId, period: SmartLightningPeriod): void {
    if (period === this.chartPeriods[chart]) {
      return;
    }
    this.chartPeriods = setSmartLightningChartPeriod(this.chartPeriods, chart, period);
    this.renderChart(chart);
    this.cd.markForCheck();
  }

  loadLatestAlarms(): void {
    this.alarmsLoading = true;
    this.alarmsLoadError = false;
    this.alarmService.getAllAlarmsV2(createSmartLightningAlarmQuery()).pipe(
      tap((pageData) => {
        this.activeAlarmCount = pageData.totalElements;
        this.activeAlarmTypeTotals = aggregateSmartLightningActiveAlarms(pageData.data);
      }),
      map((pageData) => pageData.data.map((alarm) => ({info: alarm, row: formatSmartLightningAlarm(alarm)}))
        .sort((left, right) => compareSmartLightningAlarms(left.row, right.row))
        .slice(0, SMART_LIGHTNING_ALARM_DISPLAY_LIMIT)
        .map(({info}) => info)),
      switchMap((alarms) => this.resolveAlarmRows(alarms)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (alarms) => {
        this.alarms = alarms;
        this.alarmsLoading = false;
        this.initialAlarmLoadFinished = true;
        this.renderChart('alarm');
        this.cd.markForCheck();
        if (this.realtimeAlarmCount !== null && this.realtimeAlarmCount !== this.activeAlarmCount) {
          this.loadLatestAlarms();
        }
      },
      error: () => {
        this.alarms = [];
        this.activeAlarmCount = null;
        this.activeAlarmTypeTotals = [];
        this.alarmsLoadError = true;
        this.alarmsLoading = false;
        this.initialAlarmLoadFinished = true;
        this.renderChart('alarm');
        this.cd.markForCheck();
      }
    });
  }

  /**
   * Subscribe to the server-side active-alarm count. The count update is used
   * as a lightweight invalidation signal; the existing REST loader remains the
   * single source of truth for rows and type aggregation.
   */
  private subscribeToAlarmCount(): void {
    const command = new AlarmCountCmd();
    command.query = {
      entityFilter: {
        type: AliasFilterType.entityType,
        entityType: EntityType.DEVICE
      },
      statusList: [AlarmSearchStatus.ACTIVE]
    };

    const subscriber = new TelemetrySubscriber(this.telemetryWebsocketService, this.ngZone);
    subscriber.subscriptionCommands.push(command);
    this.alarmRealtimeSubscriber = subscriber;
    subscriber.alarmCount$.pipe(takeUntil(this.destroy$)).subscribe((update) => {
      const previousCount = this.realtimeAlarmCount;
      this.realtimeAlarmCount = update.count;
      if (this.initialAlarmLoadFinished && shouldReloadSmartLightningAlarms(previousCount, update.count)) {
        this.loadLatestAlarms();
      }
    });
    subscriber.subscribe();
  }

  private loadDeviceSummary(): void {
    this.devicesLoading = true;
    this.devicesLoadError = false;
    this.monitoringApi.getLightningDevices(MAX_SAFE_PAGE_SIZE, 0).pipe(
      map((pageData) => {
        const activeValues: unknown[] = [];
        for (const device of pageData.data) {
          activeValues.push(device.active);
        }
        return activeValues;
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (activeValues) => {
        this.deviceSummary = calculateSmartLightningDeviceSummary(activeValues);
        this.devicesLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.devicesLoadError = true;
        this.devicesLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  private resolveAlarmRows(alarms: AlarmInfo[]): Observable<SmartLightningAlarmRow[]> {
    if (!alarms.length) {
      return of([]);
    }
    return forkJoin(alarms.map((alarm) => this.resolveAlarmRow(alarm)));
  }

  private resolveAlarmRow(alarm: AlarmInfo): Observable<SmartLightningAlarmRow> {
    if (alarm.originator?.entityType !== EntityType.DEVICE) {
      return of(formatSmartLightningAlarm(alarm));
    }
    const deviceId = alarm.originator.id;
    return forkJoin({
      deviceProfileName: this.deviceService.getDeviceInfo(deviceId).pipe(
        map((device) => device.deviceProfileName),
        catchError(() => of('--'))
      ),
      parentDirectory: this.entityRelationService.findInfoByTo(alarm.originator).pipe(
        map(getSmartLightningParentDirectory),
        catchError(() => of('--'))
      ),
      productName: this.attributeService.getEntityAttributes(
        alarm.originator,
        AttributeScope.SERVER_SCOPE,
        ['productName']
      ).pipe(
        map(getSmartLightningProductName),
        catchError(() => of('--'))
      )
    }).pipe(map((context) => formatSmartLightningAlarm(alarm, context)));
  }

  private initializeMap(): void {
    const {center, zoom, token} = smartLightningTianDiTuMapConfig;
    this.map = L.map(this.mapHost.nativeElement, {attributionControl: true, zoomControl: true}).setView(center, zoom);
    const tileOptions: L.TileLayerOptions = {
      subdomains: '01234567',
      maxNativeZoom: 18,
      maxZoom: 21,
      attribution: '&copy; 天地图 Tianditu'
    };
    L.tileLayer(
      'https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + token,
      tileOptions
    ).addTo(this.map);
    L.tileLayer(
      'https://t{s}.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + token,
      tileOptions
    ).addTo(this.map);

    this.markers.forEach((marker) => {
      L.circleMarker(marker.position, {
        radius: 10,
        color: '#ffffff',
        weight: 3,
        fillColor: this.markerColor(marker.status),
        fillOpacity: 1
      }).bindPopup(`<strong>${marker.name}</strong><br>${marker.detail}`).addTo(this.map!);
    });
  }

  private renderCharts(): void {
    if (!this.chartHosts) {
      return;
    }
    this.chartHosts.forEach((host) => {
      const chartId = host.nativeElement.dataset.chart;
      if (!this.isChartId(chartId)) {
        return;
      }
      this.renderChart(chartId, host.nativeElement);
    });
  }

  private renderChart(chartId: SmartLightningChartId, chartHost?: HTMLDivElement): void {
    const host = chartHost ?? this.chartHosts.find(item => item.nativeElement.dataset.chart === chartId)?.nativeElement;
    if (!host) {
      return;
    }
    const chart = this.getChart(chartId, host);
    const data = buildSmartLightningMock(this.chartPeriods[chartId]);
    switch (chartId) {
      case 'ground':
        chart.setOption(this.lineOption(data, data.groundSeries, 'Ω'));
        break;
      case 'spd':
        chart.setOption(this.lineOption(data, data.spdSeries, 'mA'));
        break;
      case 'alarm':
        chart.setOption(this.alarmOption());
        break;
      case 'orders':
        chart.setOption(this.orderOption(data));
        break;
      case 'lightning':
        chart.setOption(this.lightningOption(data));
        break;
    }
  }

  private isChartId(value: string | undefined): value is SmartLightningChartId {
    return value === 'ground' || value === 'spd' || value === 'alarm' || value === 'orders' || value === 'lightning';
  }

  private markerColor(status: StationMarker['status']): string {
    switch (status) {
      case 'normal': return '#2b9fe7';
      case 'warning': return '#f59d1b';
      case 'critical': return '#ef4444';
    }
  }

  private getChart(id: string, host: HTMLDivElement): ECharts {
    let chart = this.charts.get(id);
    if (!chart || chart.isDisposed()) {
      chart = echarts.init(host, undefined, {renderer: 'svg'});
      this.charts.set(id, chart);
    }
    return chart;
  }

  private lineOption(data: SmartLightningMock, series: SmartLightningMock['spdSeries'], unit: string): EChartsOption {
    return {
      animationDuration: 380,
      animationEasing: 'cubicOut',
      tooltip: {trigger: 'axis', confine: true, borderColor: '#dbe5ef', textStyle: {color: '#183653'}},
      legend: {top: 0, right: 0, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 11, color: '#5f7488'}},
      grid: {top: 34, right: 10, bottom: 24, left: 32, containLabel: true},
      xAxis: {
        type: 'category', boundaryGap: false, data: data.axisLabels,
        axisLine: {lineStyle: {color: '#d7e0e9'}},
        axisLabel: {color: '#7890a4', fontSize: 10}, axisTick: {show: false}
      },
      yAxis: {
        type: 'value', name: unit, nameTextStyle: {color: '#7890a4', fontSize: 10},
        splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}
      },
      series: series.map(item => ({
        name: item.name, type: 'line', smooth: true, showSymbol: false, data: item.data,
        lineStyle: {width: 2, color: item.color},
        areaStyle: {color: item.color, opacity: 0.06},
        emphasis: {focus: 'series'}
      }))
    };
  }

  private alarmOption(): EChartsOption {
    const alarmTypeTotals = this.activeAlarmTypeTotals;
    const emptyStateText = getSmartLightningAlarmChartMessage(
      this.alarmsLoading,
      this.alarmsLoadError,
      alarmTypeTotals
    );
    return {
      animationDuration: 380,
      title: {text: emptyStateText || '', left: 'center', top: 'middle', textStyle: {color: '#7890a4', fontSize: 12, fontWeight: 'normal'}},
      grid: {top: 20, right: 10, bottom: 25, left: 28},
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
      xAxis: {
        type: 'category', data: alarmTypeTotals.map((item) => item.type),
        axisLabel: {color: '#7890a4', fontSize: 10, rotate: 20, interval: 0}, axisLine: {lineStyle: {color: '#d7e0e9'}}, axisTick: {show: false}
      },
      yAxis: {type: 'value', minInterval: 1, splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: [{
        type: 'bar', data: alarmTypeTotals.map((item) => item.count), barMaxWidth: 28,
        itemStyle: {borderRadius: [5, 5, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: '#22e466'}, {offset: 1, color: '#073d5c'}
        ])}
      }]
    };
  }

  private orderOption(data: SmartLightningMock): EChartsOption {
    return {
      animationDuration: 380,
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
      legend: {top: 0, right: 0, itemWidth: 8, itemHeight: 8, textStyle: {fontSize: 11, color: '#5f7488'}},
      grid: {top: 34, right: 8, bottom: 25, left: 28},
      xAxis: {type: 'category', data: data.axisLabels, axisLabel: {color: '#7890a4', fontSize: 10}, axisTick: {show: false}},
      yAxis: {type: 'value', splitLine: {lineStyle: {color: '#edf2f6'}}, axisLabel: {color: '#7890a4', fontSize: 10}},
      series: data.workOrderSeries.map(item => ({
        name: item.name, type: 'bar', data: item.data, barMaxWidth: 12,
        itemStyle: {color: item.color, borderRadius: [3, 3, 0, 0]}
      }))
    };
  }

  private lightningOption(data: SmartLightningMock): EChartsOption {
    return {
      animationDuration: 380,
      grid: {top: 10, right: 16, bottom: 8, left: 72},
      xAxis: {type: 'value', max: 100, show: false},
      yAxis: {
        type: 'category', inverse: true, data: data.lightningMetrics.map(item => item.name),
        axisTick: {show: false}, axisLine: {show: false}, axisLabel: {color: '#526a80', fontSize: 11}
      },
      series: [{
        type: 'bar', data: data.lightningMetrics.map(item => item.value), barWidth: 18,
        showBackground: true, backgroundStyle: {color: '#edf2f6', borderRadius: 9},
        itemStyle: {borderRadius: 9, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          {offset: 0, color: '#0a4965'}, {offset: 1, color: '#20e66b'}
        ])}, label: {show: true, position: 'right', color: '#567087', fontSize: 10, formatter: '{c}'}
      }]
    };
  }
}
