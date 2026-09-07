import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { SmartLightningPeriod } from './smart-lightning-dashboard.data';

@Component({
  selector: 'tb-smart-lightning-period-switcher',
  templateUrl: './smart-lightning-period-switcher.component.html',
  styleUrls: ['./smart-lightning-period-switcher.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningPeriodSwitcherComponent {

  @Input() activePeriod: SmartLightningPeriod = SmartLightningPeriod.day;
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
