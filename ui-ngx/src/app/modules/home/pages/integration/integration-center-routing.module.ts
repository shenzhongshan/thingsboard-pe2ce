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
import { IntegrationsComponent } from '@home/pages/integration/integration/integrations.component';
import { ConvertersComponent } from '@home/pages/integration/converter/converters.component';

const routes: Routes = [
  {
    path: 'integrations-center',
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.integration_center
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/integrations-center/integrations'
        }
      },
      {
        path: 'integrations',
        component: IntegrationsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'integration.integrations',
          breadcrumb: {
            menuId: MenuId.integrations
          }
        }
      },
      {
        path: 'converters',
        component: ConvertersComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'converter.data-converters',
          breadcrumb: {
            menuId: MenuId.converters
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
export class IntegrationCenterRoutingModule { }
