/** 文件职责：声明电气安全看板、数据监测和数据查询页面及其共享组件依赖。 */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {SharedModule} from '@app/shared/shared.module';
import {DeviceAlarmRecordsModule} from '@home/pages/deviceAlarms/shared/alarm-list/device-alarm-records.module';
import {SmartLightningAssetTreeModule} from '@home/pages/smart-lightning/shared/asset-tree/smart-lightning-asset-tree.module';
import {SmartLightningRootAssetFilterComponent} from '@home/pages/smart-lightning/smart-lightning-root-asset-filter.component';
import {SmartLightningPeriodSwitcherModule} from '@home/pages/smart-lightning/dashboard/smart-lightning-period-switcher.module';
import {SmartLightningDeviceMapModule} from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.module';
import {ElectricalSafetyRoutingModule} from './electrical-safety-routing.module';
import {ElectricalSafetyDashboardComponent} from './dashboard/electrical-safety-dashboard.component';
import {ElectricalSafetyMonitoringComponent} from './monitoring/electrical-safety-monitoring.component';
import {ElectricalSafetyDataQueryComponent} from './query/electrical-safety-data-query.component';

@NgModule({
  declarations: [
    ElectricalSafetyDashboardComponent,
    ElectricalSafetyMonitoringComponent,
    ElectricalSafetyDataQueryComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    DeviceAlarmRecordsModule,
    ElectricalSafetyRoutingModule,
    SmartLightningRootAssetFilterComponent,
    SmartLightningAssetTreeModule,
    SmartLightningDeviceMapModule,
    SmartLightningPeriodSwitcherModule
  ]
})
export class ElectricalSafetyModule {}
