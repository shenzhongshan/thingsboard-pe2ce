import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AlarmService } from '@core/http/alarm.service';
import { AlarmInfo } from '@shared/models/alarm.models';

export interface SmartLightningAlarmDispatchDialogData {
  alarmId: string;
}

/** 仅负责派发：受理人选择和原生 assign 接口复用 tb-alarm-assignee。 */
@Component({
  selector: 'tb-smart-lightning-alarm-dispatch-dialog',
  templateUrl: './smart-lightning-alarm-dispatch-dialog.component.html',
  styleUrls: ['./smart-lightning-alarm-dispatch-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmDispatchDialogComponent implements OnInit {
  alarm?: AlarmInfo;
  loading = true;
  loadError = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) private readonly data: SmartLightningAlarmDispatchDialogData,
    private readonly dialogRef: MatDialogRef<SmartLightningAlarmDispatchDialogComponent, boolean>,
    private readonly alarmService: AlarmService,
    private readonly cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.alarmService.getAlarmInfo(this.data.alarmId, {ignoreLoading: true}).subscribe({
      next: (alarm) => {
        this.alarm = alarm;
        this.loading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.loadError = true;
        this.loading = false;
        this.cd.markForCheck();
      }
    });
  }

  close(updated = false): void {
    this.dialogRef.close(updated);
  }
}
