/** 文件职责：统一呈现看板中的报警信息柱状图；数据请求由调用页面负责。 */
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import * as echarts from 'echarts';
import {ECharts, EChartsOption} from 'echarts';
import type {DeviceAlarmInformationChartItem} from './device-alarm-information-chart.model';

/**
 * 防雷与电气安全看板共享的报警信息卡片。
 * 页面传入已按自身权限和产品分类查询、整理后的统计项；本组件只负责卡片外观、图表和通用状态展示。
 */
@Component({
  selector: 'tb-device-alarm-information-chart',
  templateUrl: './device-alarm-information-chart.component.html',
  styleUrls: ['./device-alarm-information-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class DeviceAlarmInformationChartComponent implements AfterViewInit, OnChanges, OnDestroy {

  /** 已由业务页面按原始告警名称整理的统计项；共享卡片不负责告警分类。 */
  @Input() items: readonly DeviceAlarmInformationChartItem[] = [];
  /** 当前模块的统计请求是否正在执行。 */
  @Input() loading = false;
  /** 当前模块的统计请求是否失败。 */
  @Input() loadError = false;
  /** 接口成功但没有任何统计项时显示的业务空状态文案。 */
  @Input() emptyMessage = '暂无报警数据';
  /** 图表容器用于延迟初始化 ECharts，并提供组件内尺寸观察。 */
  @ViewChild('chartHost') private chartHost?: ElementRef<HTMLDivElement>;

  /** 当前共享图表实例；组件销毁时释放底层 SVG 和事件资源。 */
  private chart?: ECharts;
  /** 观察面板尺寸变化，使图表随响应式布局和父级尺寸变化重绘。 */
  private resizeObserver?: ResizeObserver;
  /** 记录视图是否已创建，避免在 Angular 初始化输入阶段访问尚不存在的容器。 */
  private viewInitialized = false;

  /** 首次视图创建后初始化图表，并观察卡片内容区域的尺寸变化。 */
  ngAfterViewInit(): void {
    this.viewInitialized = true;
    const host = this.chartHost?.nativeElement;
    if (!host) {
      return;
    }
    this.chart = echarts.init(host, undefined, {renderer: 'svg'});
    this.resizeObserver = new ResizeObserver(() => this.chart?.resize());
    this.resizeObserver.observe(host);
    this.updateChart();
  }

  /** 页面接口返回新数据或加载状态变化时，更新同一个图表实例。 */
  ngOnChanges(_changes: SimpleChanges): void {
    if (this.viewInitialized) {
      this.updateChart();
    }
  }

  /** 断开尺寸观察并释放图表实例，避免页面切换后残留监听器。 */
  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.resizeObserver = undefined;
    this.chart?.dispose();
    this.chart = undefined;
  }

  /** 使用共享样式重绘告警名称计数图；空数据提示由调用页面提供。 */
  private updateChart(): void {
    if (!this.chart || this.chart.isDisposed()) {
      return;
    }
    this.chart.setOption(this.buildChartOption(), true);
  }

  /** 构建与智能防雷看板原报警信息图一致的坐标轴、提示和柱体配色。 */
  private buildChartOption(): EChartsOption {
    return {
      animationDuration: 380,
      title: {
        // 空数据、加载中和错误提示由模板统一呈现，避免与图表标题重复绘制。
        text: '',
        left: 'center',
        top: 'middle',
        textStyle: {color: '#7890a4', fontSize: 12, fontWeight: 'normal'}
      },
      grid: {top: 20, right: 10, bottom: 25, left: 28},
      tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
      xAxis: {
        type: 'category',
        data: this.items.map((item) => item.label),
        axisLabel: {color: '#7890a4', fontSize: 9, fontStyle: 'normal', rotate: 0, interval: 0, hideOverlap: true},
        axisLine: {lineStyle: {color: '#d7e0e9'}},
        axisTick: {show: false}
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: {lineStyle: {color: '#edf2f6'}},
        axisLabel: {color: '#7890a4', fontSize: 10}
      },
      series: [{
        type: 'bar',
        data: this.items.map((item) => item.count ?? 0),
        barMaxWidth: 28,
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {offset: 0, color: '#22e466'},
            {offset: 1, color: '#073d5c'}
          ])
        }
      }]
    };
  }
}
