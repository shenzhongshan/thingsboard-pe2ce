/** 文件职责：注册电气安全看板、数据监测和数据查询路由及页面权限元数据。 */
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Authority} from '@shared/models/authority.enum';
import {ElectricalSafetyDashboardComponent} from './dashboard/electrical-safety-dashboard.component';
import {ElectricalSafetyMonitoringComponent} from './monitoring/electrical-safety-monitoring.component';
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
    component: ElectricalSafetyMonitoringComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '数据监测',
      breadcrumb: {label: '数据监测', icon: 'electrical_services'}
    }
  },
  {
    path: 'electrical-safety/query',
    component: ElectricalSafetyDataQueryComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '电气安全数据查询',
      breadcrumb: {label: '数据查询', icon: 'search'}
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ElectricalSafetyRoutingModule {}
