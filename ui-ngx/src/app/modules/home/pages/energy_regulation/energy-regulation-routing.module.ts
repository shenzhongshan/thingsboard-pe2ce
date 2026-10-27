/** 文件职责：能源监管看板、用能概况、用能监测和用能数据查询页面的路由入口。 */
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Authority} from '@shared/models/authority.enum';
import {EnergyRegulationDashboardComponent} from './energy-regulation-dashboard.component';
import {EnergyRegulationOverviewComponent} from './energy-regulation-overview.component';
import {EnergyRegulationMonitoringComponent} from './monitoring/energy-regulation-monitoring.component';
import {EnergyRegulationDataQueryComponent} from './query/energy-regulation-data-query.component';
import {EnergyRegulationDeviceDetailComponent} from './monitoring/energy-regulation-device-detail.component';

const routes: Routes = [
  {
    path: 'energy-regulation/dashboard',
    component: EnergyRegulationDashboardComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '能源监管看板',
      breadcrumb: {label: '能源监管看板', icon: 'energy'}
    }
  },
  {
    path: 'energy-regulation/overview',
    component: EnergyRegulationOverviewComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '用能概况',
      breadcrumb: {label: '用能概况', icon: 'energy'}
    }
  },
  {
    // 与防雷、电气安全保持父子路由，详情页继承监测入口的面包屑。
    path: 'energy-regulation/monitoring',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {label: '用能监测', icon: 'energy'}
    },
    children: [
      {
        path: '',
        component: EnergyRegulationMonitoringComponent,
        data: {auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER], title: '用能监测'}
      },
      {
        path: 'device/:deviceId',
        component: EnergyRegulationDeviceDetailComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '能源设备详情',
          breadcrumb: {label: '设备详情', icon: 'devices'}
        }
      }
    ]
  },
  {
    // 查询详情也保留来源面包屑，两个入口复用能源设备详情组件。
    path: 'energy-regulation/query',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {label: '用能数据查询', icon: 'search'}
    },
    children: [
      {path: '', component: EnergyRegulationDataQueryComponent,
        data: {auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER], title: '用能数据查询'}},
      {path: 'device/:deviceId', component: EnergyRegulationDeviceDetailComponent,
        data: {auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER], title: '能源设备详情',
          breadcrumb: {label: '设备详情', icon: 'devices'}}}
    ]
  }
];

@NgModule({imports: [RouterModule.forChild(routes)], exports: [RouterModule]})
export class EnergyRegulationRoutingModule {}
