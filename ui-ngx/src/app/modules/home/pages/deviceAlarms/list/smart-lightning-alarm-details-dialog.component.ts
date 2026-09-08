import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AlarmService } from '@core/http/alarm.service';
import { AlarmInfo, AlarmSeverity, AlarmStatus } from '@shared/models/alarm.models';

export interface SmartLightningAlarmDetailsDialogData {
  alarmId: string;
}

/**
 * 智能防雷告警的只读详情弹窗。
 * 派发、确认、清除、备注均从列表的独立操作按钮触发，避免详情混入业务操作。
 */
@Component({
  selector: 'tb-smart-lightning-alarm-details-dialog',
  templateUrl: './smart-lightning-alarm-details-dialog.component.html',
  styleUrls: ['./smart-lightning-alarm-details-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmDetailsDialogComponent implements OnInit {
  alarm?: AlarmInfo;
  loading = true;
  loadError = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) private readonly data: SmartLightningAlarmDetailsDialogData,
    private readonly dialogRef: MatDialogRef<SmartLightningAlarmDetailsDialogComponent>,
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

  close(): void {
    this.dialogRef.close();
  }

  severityLabel(severity?: AlarmSeverity): string {
    switch (severity) {
      case AlarmSeverity.CRITICAL: return '严重';
      case AlarmSeverity.MAJOR: return '重要';
      case AlarmSeverity.MINOR: return '次要';
      case AlarmSeverity.WARNING: return '警告';
      default: return '不确定';
    }
  }

  statusLabel(status?: AlarmStatus): string {
    switch (status) {
      case AlarmStatus.ACTIVE_UNACK: return '未确认未清除';
      case AlarmStatus.ACTIVE_ACK: return '已确认未清除';
      case AlarmStatus.CLEARED_UNACK: return '未确认已清除';
      case AlarmStatus.CLEARED_ACK: return '已确认已清除';
      default: return '--';
    }
  }

  assigneeLabel(alarm?: AlarmInfo): string {
    if (!alarm?.assigneeId) return '未派发';
    const name = `${alarm.assignee?.firstName || ''} ${alarm.assignee?.lastName || ''}`.trim();
    return name || alarm.assignee?.email || '受理人已删除';
  }

  formatTime(timestamp?: number): string {
    if (!timestamp) return '--';
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    }).format(new Date(timestamp)).replace(/\//g, '-');
  }

  detailText(details: unknown): string {
    if (details === null || details === undefined || details === '') return '无';
    if (typeof details === 'string') return details;
    if (typeof details === 'object' && !Array.isArray(details)) {
      const visibleDetails = {...details as Record<string, unknown>};
      delete visibleDetails.gatewayIdentifier;
      delete visibleDetails.gatewayId;
      delete visibleDetails.gateway;
      delete visibleDetails.circuitName;
      delete visibleDetails.loopName;
      delete visibleDetails.circuit;
      details = visibleDetails;
    }
    try {
      return JSON.stringify(details, null, 2);
    } catch {
      return String(details);
    }
  }
}
