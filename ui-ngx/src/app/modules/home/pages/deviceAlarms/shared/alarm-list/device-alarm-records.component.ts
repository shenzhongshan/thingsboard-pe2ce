/** 文件职责：共用告警记录表组件，只负责呈现告警行和加载状态。 */
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import type { DeviceAlarmDisplayColumn, DeviceAlarmDisplayField, DeviceAlarmDisplayRow } from './device-alarm-records.model';

/** 在业务页面中呈现已格式化的告警记录；告警查询与模块筛选由父页面负责。 */
@Component({
  selector: 'tb-device-alarm-records',
  templateUrl: './device-alarm-records.component.html',
  styleUrls: ['./device-alarm-records.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class DeviceAlarmRecordsComponent {

  /** 已完成设备信息补全和本地化的告警行。 */
  @Input() alarms: DeviceAlarmDisplayRow[] = [];
  /** 是否正在读取告警列表。 */
  @Input() loading = false;
  /** 最近一次告警请求是否失败。 */
  @Input() loadError = false;
  /** 页面列配置；空数组时使用智能防雷看板现有列布局。 */
  @Input() columns: readonly DeviceAlarmDisplayColumn[] = [];
  /** 表格的辅助技术名称，默认沿用智能防雷告警记录名称。 */
  @Input() tableLabel = '活动告警记录';
  /** 请求失败时通知父页面重新加载。 */
  @Output() retry = new EventEmitter<void>();

  /** 读取列配置对应的文本字段；触发时间由模板的日期管道格式化。 */
  getDisplayValue(alarm: DeviceAlarmDisplayRow, field: DeviceAlarmDisplayField): string {
    switch (field) {
      case 'source': return alarm.source;
      case 'secondaryLocation': return alarm.secondaryLocation;
      case 'deviceType': return alarm.deviceType;
      case 'deviceName': return alarm.deviceName;
      case 'type': return alarm.type;
      case 'severity': return alarm.severity;
      case 'status': return alarm.status;
      case 'startTs': return String(alarm.startTs);
      default: return '--';
    }
  }
}
