import {ChangeDetectionStrategy, Component, Input, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from '@app/shared/shared.module';
import {EnergyVisualFullscreenService} from './energy-visual-fullscreen.service';

/** 统一图标、键盘说明和进入/退出状态；业务页面只传需要放大的卡片节点。 */
@Component({
  selector: 'tb-energy-visual-fullscreen-button',
  standalone: true,
  imports: [CommonModule, SharedModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<button type="button" class="visual-fullscreen-button" (click)="toggle($event)"
    [attr.aria-pressed]="(fullscreen.activeTarget$ | async) === target"
    [attr.aria-label]="(fullscreen.activeTarget$ | async) === target ? '退出全屏' : '全屏显示'"
    [attr.title]="(fullscreen.activeTarget$ | async) === target ? '退出全屏' : '全屏显示'">
      <tb-icon>{{(fullscreen.activeTarget$ | async) === target ? 'fullscreen_exit' : 'fullscreen'}}</tb-icon>
    </button>`,
  styles: [`:host {display:inline-flex;flex:0 0 auto;align-items:center;}
    .visual-fullscreen-button {display:inline-grid;place-items:center;width:24px;height:24px;padding:0;border:1px solid #195ce8;border-radius:4px;color:#195ce8;background:#fff;cursor:pointer;}
    .visual-fullscreen-button tb-icon {width:16px;height:16px;font-size:16px;line-height:16px;}
    .visual-fullscreen-button:hover {color:#fff;background:#195ce8;}
    .visual-fullscreen-button:focus-visible {outline:2px solid #195ce8;outline-offset:2px;}`]
})
export class EnergyVisualFullscreenButtonComponent implements OnDestroy {
  @Input({required: true}) target!: HTMLElement;
  constructor(public readonly fullscreen: EnergyVisualFullscreenService) {}
  toggle(event: Event): void { this.fullscreen.toggle(this.target, event.currentTarget as HTMLElement); }
  /** 关闭弹窗或切换页面时，只有当前放大卡片的按钮会恢复它。 */
  ngOnDestroy(): void { if (this.fullscreen.activeTarget === this.target) { this.fullscreen.exit(); } }
}
