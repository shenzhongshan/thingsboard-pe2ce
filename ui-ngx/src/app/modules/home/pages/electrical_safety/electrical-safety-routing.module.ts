/** 文件职责：注册电气安全看板、数据监测、设备详情和数据查询路由。 */
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Authority} from '@shared/models/authority.enum';
import {ElectricalSafetyDashboardComponent} from './dashboard/electrical-safety-dashboard.component';
import {ElectricalSafetyMonitoringComponent} from './monitoring/electrical-safety-monitoring.component';
import {ElectricalSafetyDeviceDetailComponent} from './monitoring/electrical-safety-device-detail.component';
import {ElectricalSafetyDataQueryComponent} from './query/electrical-safety-data-query.component';

const routes: Routes = [
  {
    path: 'electrical-safety/dashboard',
    component: ElectricalSafetyDashboardComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '电气安全看板',
      breadcrumb: {label: '电气安全看板', icon: 'electrical_services'}
    }
  },
  {
    path: 'electrical-safety/monitor',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {label: '数据监测', icon: 'electrical_services'}
    },
    children: [
      {
        path: '',
        component: ElectricalSafetyMonitoringComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '电气安全数据监测'
        }
      },
      {
        path: 'device/:deviceId',
        component: ElectricalSafetyDeviceDetailComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '设备详情',
          breadcrumb: {label: '设备详情', icon: 'devices'}
        }
      }
    ]
  },
  {
    // 提供与防雷一致的独立详情别名；监测和查询仍通过各自的嵌套路由进入详情。
    path: 'electrical-safety/device/:deviceId',
    component: ElectricalSafetyDeviceDetailComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '设备详情',
      breadcrumb: {label: '设备详情', icon: 'electrical_services'}
    }
  },
  {
    path: 'electrical-safety/query',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {label: '数据查询', icon: 'search'}
    },
    children: [
      {
        path: '',
        component: ElectricalSafetyDataQueryComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: '电气安全数据查询'
        }
      },
      {
        path: 'device/:deviceId',
        component: ElectricalSafetyDeviceDetailComponent,
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
export class ElectricalSafetyRoutingModule {}
