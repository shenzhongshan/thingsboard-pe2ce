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
import { AssetsTableConfigResolver } from './assets-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import {
  entityDetailsPageBreadcrumbLabelFunction,
  groupMembersPageBreadcrumbLabelFunction
} from '@home/pages/home-pages.models';
import { MenuId } from '@core/services/menu.models';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsComponent } from '@home/pages/entity-group/entity-groups.component';
import { GroupMembersPageComponent } from '@home/pages/entity-group/group-members-page.component';
import {
  AssetGroupEntitiesTableConfigResolver
} from '@home/pages/entity-group/group-entities-table-config.resolvers';

export const assetRoutes: Routes = [
  {
    path: 'assets',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      showMainLoadingBar: false,
      useChildrenRoutesForTabs: true,
      breadcrumb: {
        menuId: MenuId.assets
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/entities/assets/all'
        }
      },
      {
        path: 'all',
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          breadcrumb: {
            label: 'entity-group.all-entities',
            icon: 'domain'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'asset.assets',
              assetsType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: AssetsTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'domain'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'asset.assets',
              assetsType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: AssetsTableConfigResolver
            }
          }
        ]
      },
      {
        path: 'groups',
        data: {
          auth: [Authority.TENANT_ADMIN],
          breadcrumb: {
            label: 'entity-group.group-type-asset',
            icon: 'workspaces'
          }
        },
        children: [
          {
            path: '',
            component: EntityGroupsComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'entity-group.group-type-asset',
              groupType: EntityType.ASSET
            }
          },
          {
            path: ':entityGroupId',
            data: {
              auth: [Authority.TENANT_ADMIN],
              breadcrumb: {
                labelFunction: groupMembersPageBreadcrumbLabelFunction,
                icon: 'view_list'
              } as BreadCrumbConfig<GroupMembersPageComponent>
            },
            children: [
              {
                path: '',
                component: GroupMembersPageComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN],
                  title: 'entity-group.group-type-asset',
                  assetsType: 'tenant',
                  groupType: EntityType.ASSET
                },
                resolve: {
                  entitiesTableConfig: AssetGroupEntitiesTableConfigResolver
                }
              },
              {
                path: ':entityId',
                component: EntityDetailsPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                    icon: 'domain'
                  } as BreadCrumbConfig<EntityDetailsPageComponent>,
                  auth: [Authority.TENANT_ADMIN],
                  title: 'asset.assets',
                  assetsType: 'tenant',
                  groupType: EntityType.ASSET
                },
                resolve: {
                  entitiesTableConfig: AssetGroupEntitiesTableConfigResolver
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
    path: 'assets',
    pathMatch: 'full',
    redirectTo: '/entities/assets'
  },
  {
    path: 'assets/:entityId',
    redirectTo: '/entities/assets/all/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AssetsTableConfigResolver,
    AssetGroupEntitiesTableConfigResolver
  ]
})
export class AssetRoutingModule { }
