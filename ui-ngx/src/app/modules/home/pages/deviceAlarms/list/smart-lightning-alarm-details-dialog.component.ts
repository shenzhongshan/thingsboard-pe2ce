/** 文件职责：告警列表模块的 smart-lightning-alarm-details-dialog.component，负责页面状态管理、接口调用与用户交互。 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AlarmService } from '@core/http/alarm.service';
import { DialogService } from '@core/services/dialog.service';
import { AlarmCommentComponent } from '@home/components/alarm/alarm-comment.component';
import { AlarmInfo, AlarmSeverity, AlarmStatus } from '@shared/models/alarm.models';
import {
  getSmartLightningAlarmSeverityLabel,
  getSmartLightningAlarmSeverityTone,
  getSmartLightningAlarmStatusLabel
} from '../shared/smart-lightning-alarm-display';

export interface SmartLightningAlarmDetailsDialogData {
  alarmId: string;
  /** true 时显示派发、活动、确认和清除；普通告警列表保持只读。 */
  actionMode?: boolean;
}

/**
 * 智能防雷告警详情弹窗。
 * 告警处理页使用完整操作模式，普通告警列表使用只读模式。
 */
@Component({
  selector: 'tb-smart-lightning-alarm-details-dialog',
  templateUrl: './smart-lightning-alarm-details-dialog.component.html',
  styleUrls: ['./smart-lightning-alarm-details-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class SmartLightningAlarmDetailsDialogComponent implements OnInit {
  @ViewChild('alarmCommentComponent') private alarmCommentComponent?: AlarmCommentComponent;

  readonly alarmId: string;
  readonly actionMode: boolean;
  alarm?: AlarmInfo;
  loading = true;
  loadError = false;
  actionPending = false;
  actionError = '';
  private updated = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) private readonly data: SmartLightningAlarmDetailsDialogData,
    private readonly dialogRef: MatDialogRef<SmartLightningAlarmDetailsDialogComponent, boolean>,
    private readonly alarmService: AlarmService,
    private readonly dialogService: DialogService,
    private readonly cd: ChangeDetectorRef
  ) {
    this.alarmId = data.alarmId;
    this.actionMode = data.actionMode === true;
  }

  ngOnInit(): void {
    this.loadAlarm();
  }

  /** 重新读取告警，确保派发、确认或清除后状态立即同步到弹窗。 */
  private loadAlarm(refreshActivity = false): void {
    this.loadError = false;
    this.alarmService.getAlarmInfo(this.alarmId, {ignoreLoading: true}).subscribe({
      next: (alarm) => {
        this.alarm = alarm;
        this.loading = false;
        this.actionPending = false;
        this.actionError = '';
        if (refreshActivity) {
          this.alarmCommentComponent?.loadAlarmComments();
        }
        this.cd.markForCheck();
      },
      error: () => {
        this.loadError = !this.alarm;
        this.loading = false;
        this.actionPending = false;
        this.actionError = this.alarm ? '告警状态刷新失败，请稍后重试。' : '';
        this.cd.markForCheck();
      }
    });
  }

  /** 关闭弹窗，并告知列表是否需要重新读取告警。 */
  close(): void {
    this.dialogRef.close(this.updated);
  }

  /** 派发完成后重新读取告警及活动记录。 */
  onReassigned(): void {
    this.updated = true;
    this.actionPending = true;
    this.loadAlarm(true);
  }

  /** 二次确认后，使用 ThingsBoard 告警接口确认当前告警。 */
  acknowledge(): void {
    if (!this.canAcknowledge() || this.actionPending) return;
    this.actionPending = true;
    this.actionError = '';
    this.cd.markForCheck();
    this.dialogService.confirm('确认告警', `确认“${this.alarm?.type || '当前'}”告警后，告警仍会保持活动状态。`, '取消', '确认')
      .subscribe((confirmed) => {
        if (!confirmed) {
          this.actionPending = false;
          this.cd.markForCheck();
          return;
        }
        this.alarmService.ackAlarm(this.alarmId).subscribe({
          next: () => {
            this.updated = true;
            this.loadAlarm(true);
          },
          error: () => {
            this.actionPending = false;
            this.actionError = '告警确认失败，请稍后重试。';
            this.cd.markForCheck();
          }
        });
      });
  }

  /** 二次确认后，使用 ThingsBoard 告警接口清除当前仍处于活动状态的告警。 */
  clear(): void {
    if (!this.canClear() || this.actionPending) return;
    this.actionPending = true;
    this.actionError = '';
    this.cd.markForCheck();
    this.dialogService.confirm('清除告警', `清除“${this.alarm?.type || '当前'}”告警后，该告警将进入已处理状态。`, '取消', '清除')
      .subscribe((confirmed) => {
        if (!confirmed) {
          this.actionPending = false;
          this.cd.markForCheck();
          return;
        }
        this.alarmService.clearAlarm(this.alarmId).subscribe({
          next: () => {
            this.updated = true;
            this.loadAlarm(true);
          },
          error: () => {
            this.actionPending = false;
            this.actionError = '告警清除失败，请稍后重试。';
            this.cd.markForCheck();
          }
        });
      });
  }

  /** 未确认告警可以执行确认，告警是否清除不影响确认操作。 */
  canAcknowledge(): boolean {
    return this.actionMode && (this.alarm?.status === AlarmStatus.ACTIVE_UNACK ||
      this.alarm?.status === AlarmStatus.CLEARED_UNACK);
  }

  /** 只有尚未清除的告警可以执行清除。 */
  canClear(): boolean {
    return this.actionMode && (this.alarm?.status === AlarmStatus.ACTIVE_UNACK ||
      this.alarm?.status === AlarmStatus.ACTIVE_ACK);
  }

  severityLabel(severity?: AlarmSeverity): string {
    return getSmartLightningAlarmSeverityLabel(getSmartLightningAlarmSeverityTone(severity));
  }

  /** 把 ThingsBoard 告警状态枚举转为中文展示文本。 */
  statusLabel(status?: AlarmStatus): string {
    return getSmartLightningAlarmStatusLabel(status);
  }

  /** 受理人展示文本：无受理人为“未派发”，姓名缺失时回退邮箱。 */
  assigneeLabel(alarm?: AlarmInfo): string {
    if (!alarm?.assigneeId) return '未派发';
    const name = `${alarm.assignee?.firstName || ''} ${alarm.assignee?.lastName || ''}`.trim();
    return name || alarm.assignee?.email || '受理人已删除';
  }

  /** 时间格式化（yyyy-MM-dd HH:mm:ss），空值显示 --。 */
  formatTime(timestamp?: number): string {
    if (!timestamp) return '--';
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    }).format(new Date(timestamp)).replace(/\//g, '-');
  }

  /**
   * 详情信息序列化展示。
   *
   * <p>对象详情会剔除网关识别号、回路名称等已从表格移除的字段，
   * 避免详情弹窗展示列表页已不呈现的业务列。</p>
   */
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
