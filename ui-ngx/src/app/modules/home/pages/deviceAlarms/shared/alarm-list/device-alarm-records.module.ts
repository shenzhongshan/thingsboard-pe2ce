/** 文件职责：导出可复用的告警记录表组件。 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DeviceAlarmRecordsComponent } from './device-alarm-records.component';

/** 供业务看板复用告警记录展示，不绑定告警接口或设备分类。 */
@NgModule({
  declarations: [DeviceAlarmRecordsComponent],
  imports: [CommonModule],
  exports: [DeviceAlarmRecordsComponent]
})
export class DeviceAlarmRecordsModule {}
