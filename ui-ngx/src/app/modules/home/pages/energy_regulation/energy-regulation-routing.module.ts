/** 文件职责：能源监管看板、用能概况、用能监测和用能数据查询页面的路由入口。 */
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Authority} from '@shared/models/authority.enum';
import {EnergyRegulationDashboardComponent} from './energy-regulation-dashboard.component';
import {EnergyRegulationOverviewComponent} from './energy-regulation-overview.component';
import {EnergyRegulationMonitoringComponent} from './monitoring/energy-regulation-monitoring.component';
import {EnergyRegulationDataQueryComponent} from './query/energy-regulation-data-query.component';

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
    path: 'energy-regulation/monitoring',
    component: EnergyRegulationMonitoringComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '用能监测',
      breadcrumb: {label: '用能监测', icon: 'energy'}
    }
  },
  {
    path: 'energy-regulation/query',
    component: EnergyRegulationDataQueryComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '用能数据查询',
      breadcrumb: {label: '用能数据查询', icon: 'energy'}
    }
  }
];

@NgModule({imports: [RouterModule.forChild(routes)], exports: [RouterModule]})
export class EnergyRegulationRoutingModule {}
