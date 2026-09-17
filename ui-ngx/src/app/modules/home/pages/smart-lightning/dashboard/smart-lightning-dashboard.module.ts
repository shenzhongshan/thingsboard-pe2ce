/** 文件职责：看板模块的 smart-lightning-dashboard.module，声明 Angular 模块、组件与依赖导入。 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '@app/shared/shared.module';
import { SmartLightningDashboardComponent } from './smart-lightning-dashboard.component';
import { SmartLightningDashboardRoutingModule } from './smart-lightning-dashboard-routing.module';
import { SmartLightningPeriodSwitcherModule } from './smart-lightning-period-switcher.module';
import { SmartLightningMonitoringComponent } from '../monitoring/smart-lightning-monitoring.component';
import { SmartLightningDeviceDetailComponent } from '../monitoring/smart-lightning-device-detail.component';
import { SmartLightningDataQueryComponent } from '../query/smart-lightning-data-query.component';
import { SmartLightningHistoryReviewDialogComponent } from '../query/smart-lightning-history-review-dialog.component';
import { SmartLightningRootAssetFilterComponent } from '../smart-lightning-root-asset-filter.component';
import { SmartLightningAssetTreeModule } from '../shared/asset-tree/smart-lightning-asset-tree.module';
import { SmartLightningDeviceMapModule } from '../shared/map/smart-lightning-device-map.module';

@NgModule({
  declarations: [SmartLightningDashboardComponent, SmartLightningMonitoringComponent, SmartLightningDeviceDetailComponent, SmartLightningDataQueryComponent, SmartLightningHistoryReviewDialogComponent],
  imports: [CommonModule, FormsModule, SharedModule, SmartLightningDashboardRoutingModule, SmartLightningPeriodSwitcherModule, SmartLightningRootAssetFilterComponent, SmartLightningAssetTreeModule, SmartLightningDeviceMapModule]
})
export class SmartLightningDashboardModule {}
