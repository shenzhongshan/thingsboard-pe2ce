import {AfterViewInit, Directive, ElementRef, Input, OnChanges, OnDestroy} from '@angular/core';

/** 按实时数值格的实际可用宽度缩小字号；两个业务模块共用；数值或容器变化时重新测量，保持数值和单位同一行。 */
@Directive({selector: '[tbTelemetryFitValue]', standalone: true})
export class TelemetryFitValueDirective implements AfterViewInit, OnChanges, OnDestroy {
  /** 完整展示文本同时作为更新信号，保证 WebSocket 更新后重新适配。 */
  @Input() tbTelemetryFitValue = '';
  private observer?: ResizeObserver;
  private frame?: number;
  private ready = false;

  constructor(private readonly element: ElementRef<HTMLElement>) {}

  /** 观察外层格子，避免字号自身变化反复触发尺寸回调。 */
  ngAfterViewInit(): void {
    this.ready = true;
    this.observer = new ResizeObserver(() => this.scheduleFit());
    this.observer.observe(this.element.nativeElement.parentElement!);
    this.scheduleFit();
    document.fonts.ready.then(() => this.scheduleFit());
  }

  /** 在 Angular 写入最新文本后合并测量，避免每条遥测重复同步布局。 */
  ngOnChanges(): void {
    this.scheduleFit();
  }

  /** 销毁时释放尺寸观察和待执行动画帧。 */
  ngOnDestroy(): void {
    this.ready = false;
    this.observer?.disconnect();
    if (this.frame !== undefined) {
      cancelAnimationFrame(this.frame);
    }
  }

  private scheduleFit(): void {
    if (!this.ready || this.frame !== undefined) {
      return;
    }
    this.frame = requestAnimationFrame(() => {
      this.frame = undefined;
      this.fitValue();
    });
  }

  /** 从当前断点的设计字号开始二分缩小，文本变短时也能恢复原字号。 */
  private fitValue(): void {
    const host = this.element.nativeElement;
    host.style.fontSize = '';
    if (!host.clientWidth || host.scrollWidth <= host.clientWidth) {
      return;
    }
    let minimum = 1;
    let maximum = parseFloat(getComputedStyle(host).fontSize);
    while (maximum - minimum > 0.25) {
      const size = (minimum + maximum) / 2;
      host.style.fontSize = `${size}px`;
      if (host.scrollWidth > host.clientWidth) {
        maximum = size;
      } else {
        minimum = size;
      }
    }
    host.style.fontSize = `${minimum}px`;
  }
}
