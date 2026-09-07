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

@NgModule({
  declarations: [SmartLightningDashboardComponent, SmartLightningPeriodSwitcherComponent, SmartLightningMonitoringComponent, SmartLightningDeviceDetailComponent, SmartLightningDataQueryComponent, SmartLightningHistoryReviewDialogComponent],
  imports: [CommonModule, FormsModule, SharedModule, SmartLightningDashboardRoutingModule, SmartLightningRootAssetFilterComponent]
})
export class SmartLightningDashboardModule {}
