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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { DashboardsTableConfigResolver } from './dashboards-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { mergeMap, Observable, of } from 'rxjs';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { catchError, map } from 'rxjs/operators';
import { UserSettingsService } from '@core/http/user-settings.service';
import { UserDashboardAction } from '@shared/models/user-settings.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { MenuId } from '@core/services/menu.models';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsComponent } from '@home/pages/entity-group/entity-groups.component';
import { GroupMembersPageComponent } from '@home/pages/entity-group/group-members-page.component';
import {
  DashboardGroupEntitiesTableConfigResolver
} from '@home/pages/entity-group/group-entities-table-config.resolvers';
import { groupMembersPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';

@Injectable()
export class DashboardResolver  {

  constructor(private store: Store<AppState>,
              private dashboardService: DashboardService,
              private userSettingService: UserSettingsService,
              private dashboardUtils: DashboardUtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Dashboard> {
    const dashboardId = route.params.dashboardId;
    return this.dashboardService.getDashboard(dashboardId).pipe(
      mergeMap((dashboard) =>
        (getCurrentAuthUser(this.store).isPublic ? of(null) :
          this.userSettingService.reportUserDashboardAction(dashboardId, UserDashboardAction.VISIT,
            {ignoreLoading: true, ignoreErrors: true})).pipe(
          catchError(() => of(dashboard)),
          map(() => dashboard)
        )),
      map((dashboard) => this.dashboardUtils.validateAndUpdateDashboard(dashboard))
    );
  }
}

export const dashboardBreadcumbLabelFunction: BreadCrumbLabelFunction<DashboardPageComponent>
  = ((route, translate, component) => component.dashboard.title);

const routes: Routes = [
  {
    path: 'dashboards',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      showMainLoadingBar: false,
      useChildrenRoutesForTabs: true,
      breadcrumb: {
        menuId: MenuId.dashboards
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/dashboards/all'
        }
      },
      {
        path: 'all',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'dashboard.dashboards',
          dashboardsType: 'tenant',
          breadcrumb: {
            label: 'entity-group.all-entities',
            icon: 'dashboard'
          }
        },
        resolve: {
          entitiesTableConfig: DashboardsTableConfigResolver
        }
      },
      {
        path: 'groups',
        data: {
          auth: [Authority.TENANT_ADMIN],
          breadcrumb: {
            label: 'entity-group.group-type-dashboard',
            icon: 'workspaces'
          }
        },
        children: [
          {
            path: '',
            component: EntityGroupsComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'entity-group.group-type-dashboard',
              groupType: EntityType.DASHBOARD
            }
          },
          {
            path: ':entityGroupId',
            component: GroupMembersPageComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'entity-group.group-type-dashboard',
              dashboardsType: 'tenant',
              groupType: EntityType.DASHBOARD,
              breadcrumb: {
                labelFunction: groupMembersPageBreadcrumbLabelFunction,
                icon: 'view_list'
              } as BreadCrumbConfig<GroupMembersPageComponent>
            },
            resolve: {
              entitiesTableConfig: DashboardGroupEntitiesTableConfigResolver
            }
          }
        ]
      },
      {
        // Dashboard view stays at /dashboards/<id> — absolute links all over the app point here.
        path: ':dashboardId',
        component: DashboardPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          hideTabs: true,
          breadcrumb: {
            labelFunction: dashboardBreadcumbLabelFunction,
            icon: 'dashboard'
          } as BreadCrumbConfig<DashboardPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'dashboard.dashboard',
          widgetEditMode: false
        },
        resolve: {
          dashboard: DashboardResolver
        }
      }
    ]
  }
];

// @dynamic
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DashboardsTableConfigResolver,
    DashboardResolver,
    DashboardGroupEntitiesTableConfigResolver
  ]
})
export class DashboardRoutingModule { }
