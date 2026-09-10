/** 文件职责：看板模块的 smart-lightning-dashboard-routing.module，声明智能防雷看板的页面路由与子路由。 */
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
      // 使用 TB 原生面包屑，颜色跟随当前主题。
      breadcrumb: {label: '智能防雷看板', icon: 'dashboard'}
    }
  },
  {
    path: 'smart-lightning/monitoring',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {label: '数据监测', icon: 'monitor_heart'}
    },
    children: [
      {
        path: '',
        component: SmartLightningMonitoringComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '智能防雷数据监测'
        }
      },
      {
        // 数据监测是真实页面，进入设备详情后显示“数据监测 > 设备详情”。
        path: 'device/:deviceId',
        component: SmartLightningDeviceDetailComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '设备详情',
          breadcrumb: {label: '设备详情', icon: 'devices'}
        }
      }
    ]
  },
  {
    path: 'smart-lightning/device/:deviceId',
    component: SmartLightningDeviceDetailComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '设备详情',
      breadcrumb: {label: '设备详情', icon: 'devices'}
    }
  },
  {
    path: 'smart-lightning/query',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {label: '数据查询', icon: 'search'}
    },
    children: [
      {
        path: '',
        component: SmartLightningDataQueryComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '智能防雷数据查询'
        }
      },
      {
        // 数据查询是真实页面，详情页保留来源导航和左侧菜单选中状态。
        path: 'device/:deviceId',
        component: SmartLightningDeviceDetailComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '设备详情',
          breadcrumb: {label: '设备详情', icon: 'devices'}
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SmartLightningDashboardRoutingModule {}
