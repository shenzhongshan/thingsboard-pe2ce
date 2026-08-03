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
import { UsersTableConfigResolver } from '@modules/home/pages/user/users-table-config.resolver';
import { Authority } from '@shared/models/authority.enum';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import {
  entityDetailsPageBreadcrumbLabelFunction,
  groupMembersPageBreadcrumbLabelFunction
} from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsComponent } from '@home/pages/entity-group/entity-groups.component';
import { GroupMembersPageComponent } from '@home/pages/entity-group/group-members-page.component';
import {
  AllUsersTableConfigResolver,
  UserGroupEntitiesTableConfigResolver
} from '@home/pages/entity-group/group-entities-table-config.resolvers';

const routes: Routes = [
  {
    path: 'users',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      showMainLoadingBar: false,
      useChildrenRoutesForTabs: true,
      breadcrumb: {
        menuId: MenuId.users
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/users/all'
        }
      },
      {
        path: 'all',
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          breadcrumb: {
            label: 'entity-group.all-entities',
            icon: 'account_circle'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'user.users'
            },
            resolve: {
              entitiesTableConfig: AllUsersTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'account_circle'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'user.user'
            },
            resolve: {
              entitiesTableConfig: AllUsersTableConfigResolver
            }
          }
        ]
      },
      {
        path: 'groups',
        data: {
          auth: [Authority.TENANT_ADMIN],
          breadcrumb: {
            label: 'entity-group.group-type-user',
            icon: 'workspaces'
          }
        },
        children: [
          {
            path: '',
            component: EntityGroupsComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'entity-group.group-type-user',
              groupType: EntityType.USER
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
                  title: 'entity-group.group-type-user',
                  groupType: EntityType.USER
                },
                resolve: {
                  entitiesTableConfig: UserGroupEntitiesTableConfigResolver
                }
              },
              {
                path: ':entityId',
                component: EntityDetailsPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                    icon: 'account_circle'
                  } as BreadCrumbConfig<EntityDetailsPageComponent>,
                  auth: [Authority.TENANT_ADMIN],
                  title: 'user.user',
                  groupType: EntityType.USER
                },
                resolve: {
                  entitiesTableConfig: UserGroupEntitiesTableConfigResolver
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

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UsersTableConfigResolver,
    AllUsersTableConfigResolver,
    UserGroupEntitiesTableConfigResolver
  ]
})
export class UserRoutingModule { }
