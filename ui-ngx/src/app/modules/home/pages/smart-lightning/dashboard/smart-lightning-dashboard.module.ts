/** 文件职责：看板模块的 smart-lightning-dashboard.module，声明 Angular 模块、组件与依赖导入。 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '@app/shared/shared.module';
import { SmartLightningDashboardComponent } from './smart-lightning-dashboard.component';
import { SmartLightningDashboardRoutingModule } from './smart-lightning-dashboard-routing.module';
import { SmartLightningPeriodSwitcherComponent } from './smart-lightning-period-switcher.component';
import { SmartLightningMonitoringComponent } from '../monitoring/smart-lightning-monitoring.component';
import { SmartLightningDeviceDetailComponent } from '../monitoring/smart-lightning-device-detail.component';
import { SmartLightningDataQueryComponent } from '../query/smart-lightning-data-query.component';
import { SmartLightningHistoryReviewDialogComponent } from '../query/smart-lightning-history-review-dialog.component';
import { SmartLightningRootAssetFilterComponent } from '../smart-lightning-root-asset-filter.component';
import { SmartLightningAssetTreeModule } from '../shared/asset-tree/smart-lightning-asset-tree.module';

@NgModule({
  declarations: [SmartLightningDashboardComponent, SmartLightningPeriodSwitcherComponent, SmartLightningMonitoringComponent, SmartLightningDeviceDetailComponent, SmartLightningDataQueryComponent, SmartLightningHistoryReviewDialogComponent],
  imports: [CommonModule, FormsModule, SharedModule, SmartLightningDashboardRoutingModule, SmartLightningRootAssetFilterComponent, SmartLightningAssetTreeModule]
})
export class SmartLightningDashboardModule {}
