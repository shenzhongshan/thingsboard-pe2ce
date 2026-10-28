/** 文件职责：看板模块的 smart-lightning-period-switcher.component，负责页面状态管理、接口调用与用户交互。 */
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { SmartLightningPeriod } from './smart-lightning-dashboard.data';

/** 日/周/月三段周期切换按钮组件；每个实例独立维护选中状态，互不同步。 */
@Component({
  selector: 'tb-smart-lightning-period-switcher',
  templateUrl: './smart-lightning-period-switcher.component.html',
  styleUrls: ['./smart-lightning-period-switcher.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningPeriodSwitcherComponent {

  @Input() activePeriod: SmartLightningPeriod = SmartLightningPeriod.day;
  /** 提供当前周期控件的具体统计对象；默认值保持防雷页面原有无障碍标签。 */
  @Input() ariaLabel = '图表时间范围';
  @Output() readonly periodChange = new EventEmitter<SmartLightningPeriod>();

  readonly periods = [
    {value: SmartLightningPeriod.day, label: '日'},
    {value: SmartLightningPeriod.week, label: '周'},
    {value: SmartLightningPeriod.month, label: '月'}
  ];

  select(period: SmartLightningPeriod): void {
    if (period !== this.activePeriod) {
      this.periodChange.emit(period);
    }
  }
}
