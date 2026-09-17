/** 文件职责：自定义平台首页的独立路由；不覆盖 ThingsBoard 原生 /home。 */
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { HomeOverviewComponent } from './home-overview.component';

const routes: Routes = [{
  path: 'platform-home',
  component: HomeOverviewComponent,
  data: {
    auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    title: '平台首页',
    breadcrumb: {label: '平台首页', icon: 'dashboard'}
  }
}];

@NgModule({imports: [RouterModule.forChild(routes)], exports: [RouterModule]})
export class HomeOverviewRoutingModule {}
