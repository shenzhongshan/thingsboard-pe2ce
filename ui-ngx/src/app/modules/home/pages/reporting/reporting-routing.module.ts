///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { ReportTemplatesComponent } from '@home/pages/reporting/report-template/report-templates.component';
import { ReportScheduleComponent } from '@home/pages/reporting/schedule/report-schedule.component';
import { ReportsComponent } from '@home/pages/reporting/report/reports.component';

const routes: Routes = [
  {
    path: 'reporting',
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.reporting
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/reporting/templates'
        }
      },
      {
        path: 'templates',
        component: ReportTemplatesComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'report.templates',
          breadcrumb: {
            menuId: MenuId.report_templates
          }
        }
      },
      {
        path: 'schedule',
        component: ReportScheduleComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'report.schedule',
          breadcrumb: {
            menuId: MenuId.report_schedule
          }
        }
      },
      {
        path: 'reports',
        component: ReportsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'report.reports',
          breadcrumb: {
            menuId: MenuId.reports
          }
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ReportingRoutingModule { }
