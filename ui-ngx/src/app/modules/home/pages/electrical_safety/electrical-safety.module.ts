/** 文件职责：声明电气安全看板、监测、设备详情和查询页面及共享组件依赖。 */
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {SharedModule} from '@app/shared/shared.module';
import {DeviceAlarmRecordsModule} from '@home/pages/deviceAlarms/shared/alarm-list/device-alarm-records.module';
import {DeviceAlarmInformationChartModule} from '@home/pages/deviceAlarms/shared/alarm-information/device-alarm-information-chart.module';
import {SmartLightningAssetTreeModule} from '@home/pages/smart-lightning/shared/asset-tree/smart-lightning-asset-tree.module';
import {SmartLightningRootAssetFilterComponent} from '@home/pages/smart-lightning/smart-lightning-root-asset-filter.component';
import {SmartLightningPeriodSwitcherModule} from '@home/pages/smart-lightning/dashboard/smart-lightning-period-switcher.module';
import {SmartLightningDeviceMapModule} from '@home/pages/smart-lightning/shared/map/smart-lightning-device-map.module';
import {SmartLightningHistoryReviewDialogModule} from '@home/pages/smart-lightning/query/smart-lightning-history-review-dialog.module';
import {ElectricalSafetyRoutingModule} from './electrical-safety-routing.module';
import {ElectricalSafetyDashboardComponent} from './dashboard/electrical-safety-dashboard.component';
import {ElectricalSafetyMonitoringComponent} from './monitoring/electrical-safety-monitoring.component';
import {ElectricalSafetyDeviceDetailComponent} from './monitoring/electrical-safety-device-detail.component';
import {ElectricalSafetyDataQueryComponent} from './query/electrical-safety-data-query.component';

@NgModule({
  declarations: [
    ElectricalSafetyDashboardComponent,
    ElectricalSafetyMonitoringComponent,
    ElectricalSafetyDeviceDetailComponent,
    ElectricalSafetyDataQueryComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    DeviceAlarmRecordsModule,
    DeviceAlarmInformationChartModule,
    ElectricalSafetyRoutingModule,
    SmartLightningRootAssetFilterComponent,
    SmartLightningAssetTreeModule,
    SmartLightningDeviceMapModule,
    SmartLightningPeriodSwitcherModule,
    SmartLightningHistoryReviewDialogModule
  ]
})
export class ElectricalSafetyModule {}
