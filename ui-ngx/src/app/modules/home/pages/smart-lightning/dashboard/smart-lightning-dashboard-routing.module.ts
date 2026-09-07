import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { SmartLightningDashboardComponent } from './smart-lightning-dashboard.component';
import { SmartLightningMonitoringComponent } from '../monitoring/smart-lightning-monitoring.component';
import { SmartLightningDeviceDetailComponent } from '../monitoring/smart-lightning-device-detail.component';
import { SmartLightningDataQueryComponent } from '../query/smart-lightning-data-query.component';

const routes: Routes = [
  {
    path: 'smart-lightning-dashboard',
    component: SmartLightningDashboardComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '智能防雷看板',
      breadcrumb: {skip: true}
    }
  },
  {
    path: 'smart-lightning/monitoring',
    component: SmartLightningMonitoringComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '智能防雷数据监测',
      breadcrumb: {skip: true}
    }
  },
  {
    path: 'smart-lightning/device/:deviceId',
    component: SmartLightningDeviceDetailComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '设备详情',
      breadcrumb: {skip: true}
    }
  },
  {
    // 详情路由保留来源页前缀，使左侧 routerLinkActive 能继续高亮对应菜单。
    path: 'smart-lightning/monitoring/device/:deviceId',
    component: SmartLightningDeviceDetailComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '设备详情',
      breadcrumb: {skip: true}
    }
  },
  {
    // 数据查询详情同样使用 query 前缀，避免进入详情后丢失“数据查询”菜单选中状态。
    path: 'smart-lightning/query/device/:deviceId',
    component: SmartLightningDeviceDetailComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '设备详情',
      breadcrumb: {skip: true}
    }
  },
  {
    path: 'smart-lightning/query',
    component: SmartLightningDataQueryComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '智能防雷数据查询',
      breadcrumb: {skip: true}
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SmartLightningDashboardRoutingModule {}
