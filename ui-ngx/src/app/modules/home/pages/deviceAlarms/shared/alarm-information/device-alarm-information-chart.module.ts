/** 文件职责：导出供各业务看板复用的报警信息图表卡片。 */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {DeviceAlarmInformationChartComponent} from './device-alarm-information-chart.component';

/** 共享图表只负责呈现输入数据，接口请求由防雷、电气安全等调用页面各自管理。 */
@NgModule({
  declarations: [DeviceAlarmInformationChartComponent],
  imports: [CommonModule],
  exports: [DeviceAlarmInformationChartComponent]
})
export class DeviceAlarmInformationChartModule {}
