/** 文件职责：设备日志自定义页面路由，不修改 ThingsBoard 原生设备路由。 */
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { LogComponent } from './log.component';

const routes: Routes = [
  {
    path: 'log',
    component: LogComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: '设备日志',
      breadcrumb: {label: '设备日志', icon: 'history'}
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LogRoutingModule {}
