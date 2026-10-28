/** 文件职责：能源监管前端模块，注册看板、用能概况、监测、数据查询路由及共享依赖。 */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {SharedModule} from '@app/shared/shared.module';
import {SmartLightningDeviceMapModule} from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.module';
import {EnergyRegulationDashboardComponent} from './energy-regulation-dashboard.component';
import {EnergyRegulationRoutingModule} from './energy-regulation-routing.module';

@NgModule({
  declarations: [EnergyRegulationDashboardComponent],
  imports: [CommonModule, FormsModule, SharedModule, EnergyRegulationRoutingModule, SmartLightningDeviceMapModule]
})
export class EnergyRegulationModule {}
