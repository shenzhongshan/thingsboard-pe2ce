/** 文件职责：看板周期切换器模块，向智能防雷看板和告警分析页提供统一的日/周/月控件。 */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {SmartLightningPeriodSwitcherComponent} from './smart-lightning-period-switcher.component';

@NgModule({
  declarations: [SmartLightningPeriodSwitcherComponent],
  imports: [CommonModule],
  exports: [SmartLightningPeriodSwitcherComponent]
})
export class SmartLightningPeriodSwitcherModule {}
