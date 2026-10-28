/** 文件职责：自定义平台首页模块，声明页面组件并复用项目通用 UI。 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SharedModule } from '@app/shared/shared.module';
import { HomeOverviewComponent } from './home-overview.component';
import { HomeOverviewRoutingModule } from './home-overview-routing.module';
import { SmartLightningDeviceMapModule } from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.module';

@NgModule({
  declarations: [HomeOverviewComponent],
  imports: [CommonModule, SharedModule, HomeOverviewRoutingModule, SmartLightningDeviceMapModule]
})
export class HomeOverviewModule {}
