import {AfterViewInit, Directive, ElementRef, NgZone, OnDestroy} from '@angular/core';

/** 让看板至少填满系统工作区；全屏或窗口缩放时重算，不干预原有窄屏滚动。 */
@Directive({selector: '[tbDashboardFill]', standalone: true})
export class DashboardFillDirective implements AfterViewInit, OnDestroy {
  private observer?: ResizeObserver;
  private workspace?: HTMLElement;
  private readonly resize = () => this.updateHeight();

  constructor(private readonly element: ElementRef<HTMLElement>, private readonly zone: NgZone) {}

  ngAfterViewInit(): void {
    this.workspace = this.element.nativeElement.closest<HTMLElement>('.tb-main-content') ?? undefined;
    if (!this.workspace) { return; }
    this.zone.runOutsideAngular(() => {
      // 观察实际工作区，兼容顶部工具栏隐藏、浏览器全屏和侧栏布局变化。
      this.observer = new ResizeObserver(this.resize);
      this.observer.observe(this.workspace!);
      window.addEventListener('resize', this.resize);
      document.addEventListener('fullscreenchange', this.resize);
      this.updateHeight();
    });
  }

  /** 用滚动前的页面偏移扣除上方内容，避免滚动时反复增大最小高度。 */
  private updateHeight(): void {
    if (!this.workspace) { return; }
    const page = this.element.nativeElement;
    const offset = page.getBoundingClientRect().top - this.workspace.getBoundingClientRect().top
      - this.workspace.clientTop + this.workspace.scrollTop;
    const height = Math.max(0, this.workspace.clientHeight - Math.max(0, offset));
    page.style.setProperty('--tb-dashboard-min-height', `${height}px`);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    window.removeEventListener('resize', this.resize);
    document.removeEventListener('fullscreenchange', this.resize);
    this.element.nativeElement.style.removeProperty('--tb-dashboard-min-height');
  }
}
