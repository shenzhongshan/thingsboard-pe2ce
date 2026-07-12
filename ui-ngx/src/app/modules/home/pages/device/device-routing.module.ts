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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { DevicesTableConfigResolver } from '@modules/home/pages/device/devices-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsComponent } from '@home/pages/entity-group/entity-groups.component';
import {
  DeviceGroupEntitiesTableConfigResolver
} from '@home/pages/entity-group/group-entities-table-config.resolvers';

export const deviceRoutes: Routes = [
  {
    path: 'devices',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      showMainLoadingBar: false,
      useChildrenRoutesForTabs: true,
      breadcrumb: {
        menuId: MenuId.devices
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/entities/devices/all'
        }
      },
      {
        path: 'all',
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          breadcrumb: {
            label: 'entity-group.all-entities',
            icon: 'devices_other'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'device.devices',
              devicesType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'devices_other'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'device.devices',
              devicesType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
          }
        ]
      },
      {
        path: 'groups',
        data: {
          auth: [Authority.TENANT_ADMIN],
          breadcrumb: {
            label: 'entity-group.group-type-device',
            icon: 'workspaces'
          }
        },
        children: [
          {
            path: '',
            component: EntityGroupsComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'entity-group.group-type-device',
              groupType: EntityType.DEVICE
            }
          },
          {
            path: ':entityGroupId',
            data: {
              auth: [Authority.TENANT_ADMIN],
              breadcrumb: {
                label: 'entity-group.group-entities',
                icon: 'view_list'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN],
                  title: 'entity-group.group-type-device',
                  devicesType: 'tenant',
                  groupType: EntityType.DEVICE
                },
                resolve: {
                  entitiesTableConfig: DeviceGroupEntitiesTableConfigResolver
                }
              },
              {
                path: ':entityId',
                component: EntityDetailsPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                    icon: 'devices_other'
                  } as BreadCrumbConfig<EntityDetailsPageComponent>,
                  auth: [Authority.TENANT_ADMIN],
                  title: 'device.devices',
                  devicesType: 'tenant',
                  groupType: EntityType.DEVICE
                },
                resolve: {
                  entitiesTableConfig: DeviceGroupEntitiesTableConfigResolver
                }
              }
            ]
          }
        ]
      },
      {
        path: ':entityId',
        pathMatch: 'full',
        redirectTo: 'all/:entityId'
      }
    ]
  }
];

const routes: Routes = [
  {
    path: 'devices',
    pathMatch: 'full',
    redirectTo: '/entities/devices'
  },
  {
    path: 'devices/:entityId',
    redirectTo: '/entities/devices/all/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DevicesTableConfigResolver,
    DeviceGroupEntitiesTableConfigResolver
  ]
})
export class DeviceRoutingModule { }
