/** 文件职责：声明公共设备地图组件，供智能防雷看板和自定义平台首页复用。 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SmartLightningDeviceMapComponent } from './smart-lightning-device-map.component';

@NgModule({
  declarations: [SmartLightningDeviceMapComponent],
  imports: [CommonModule],
  exports: [SmartLightningDeviceMapComponent]
})
export class SmartLightningDeviceMapModule {}
