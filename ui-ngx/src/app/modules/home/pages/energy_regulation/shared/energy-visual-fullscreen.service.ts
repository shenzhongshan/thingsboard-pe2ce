import {Injectable, OnDestroy} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {getEnergyFullscreenBounds} from './energy-visual-fullscreen.data';

/** 能源图像组件共用工作区放大控制；同一时刻只放大一张卡片，切页时完整恢复样式。 */
@Injectable({providedIn: 'root'})
export class EnergyVisualFullscreenService implements OnDestroy {
  private readonly activeSubject = new BehaviorSubject<HTMLElement | null>(null);
  readonly activeTarget$ = this.activeSubject.asObservable();
  private readonly previousStyles = new Map<string, {value: string; priority: string}>();
  private trigger?: HTMLElement;
  private resizeFrame?: number;
  private workspaceObserver?: ResizeObserver;
  private readonly onResize = () => this.updateBounds();
  private readonly onEscape = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && this.activeTarget) {
      event.preventDefault();
      // 先退出当前放大，避免同一次Escape继续关闭“更多参数”弹窗。
      event.stopImmediatePropagation();
      this.exit();
    }
  };

  get activeTarget(): HTMLElement | null { return this.activeSubject.value; }

  /** 卡片原节点原地放大，不销毁图表或地图，因此数据订阅和图例状态保持不变。 */
  toggle(target: HTMLElement, trigger?: HTMLElement): void {
    if (this.activeTarget === target) { this.exit(); return; }
    const workspace = document.querySelector('.tb-main-content');
    const bounds = workspace ? getEnergyFullscreenBounds(workspace.getBoundingClientRect(), window.innerWidth, window.innerHeight) : null;
    if (!bounds) { return; }
    this.exit();
    this.trigger = trigger;
    const styles: Record<string, string> = {
      position: 'fixed', 'z-index': '1100', display: 'flex', 'flex-direction': 'column',
      'min-width': '0', 'min-height': '0', 'max-width': 'none', 'max-height': 'none',
      margin: '0', 'box-sizing': 'border-box', overflow: 'hidden', background: '#fff',
      top: `${bounds.top}px`, left: `${bounds.left}px`, width: `${bounds.width}px`, height: `${bounds.height}px`
    };
    for (const [property, value] of Object.entries(styles)) {
      this.previousStyles.set(property, {value: target.style.getPropertyValue(property), priority: target.style.getPropertyPriority(property)});
      target.style.setProperty(property, value);
    }
    target.classList.add('energy-visual-expanded');
    this.activeSubject.next(target);
    // 侧栏展开或工作区高度变化未必产生window.resize，因此同时观察内容区。
    if (typeof ResizeObserver !== 'undefined') {
      this.workspaceObserver = new ResizeObserver(() => this.updateBounds());
      this.workspaceObserver.observe(workspace!);
    }
    window.addEventListener('resize', this.onResize);
    document.addEventListener('keydown', this.onEscape, true);
    this.notifyLayout();
  }

  /** 恢复进入前的内联样式，不覆盖卡片其他运行时属性。 */
  exit(): void {
    const target = this.activeTarget;
    if (!target) { return; }
    this.workspaceObserver?.disconnect();
    this.workspaceObserver = undefined;
    window.removeEventListener('resize', this.onResize);
    document.removeEventListener('keydown', this.onEscape, true);
    for (const [property, previous] of this.previousStyles) {
      if (previous.value) { target.style.setProperty(property, previous.value, previous.priority); }
      else { target.style.removeProperty(property); }
    }
    this.previousStyles.clear();
    target.classList.remove('energy-visual-expanded');
    this.activeSubject.next(null);
    if (this.trigger?.isConnected) { this.trigger.focus({preventScroll: true}); }
    this.trigger = undefined;
    this.notifyLayout();
  }

  /** 窗口和工作区尺寸变化后重新计算可视矩形，不采用固定偏移量。 */
  private updateBounds(): void {
    const workspace = document.querySelector('.tb-main-content');
    if (!this.activeTarget || !workspace) { return; }
    const bounds = getEnergyFullscreenBounds(workspace.getBoundingClientRect(), window.innerWidth, window.innerHeight);
    if (!bounds) { this.exit(); return; }
    for (const property of ['top', 'left', 'width', 'height'] as const) {
      this.activeTarget.style.setProperty(property, `${bounds[property]}px`);
    }
  }

  /** 图表的ResizeObserver及Leaflet原生窗口监听共同响应，避免地图放大后留下空白。 */
  private notifyLayout(): void {
    if (this.resizeFrame !== undefined) { cancelAnimationFrame(this.resizeFrame); }
    this.resizeFrame = requestAnimationFrame(() => {
      this.resizeFrame = undefined;
      window.dispatchEvent(new Event('resize'));
    });
  }

  ngOnDestroy(): void {
    this.exit();
    if (this.resizeFrame !== undefined) { cancelAnimationFrame(this.resizeFrame); }
    this.activeSubject.complete();
  }
}
