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
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EdgesTableConfigResolver } from '@home/pages/edge/edges-table-config.resolver';
import { AssetsTableConfigResolver } from '@home/pages/asset/assets-table-config.resolver';
import { DevicesTableConfigResolver } from '@home/pages/device/devices-table-config.resolver';
import { EntityViewsTableConfigResolver } from '@home/pages/entity-view/entity-views-table-config.resolver';
import { DashboardsTableConfigResolver } from '@home/pages/dashboard/dashboards-table-config.resolver';
import { RuleChainsTableConfigResolver } from '@home/pages/rulechain/rulechains-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { dashboardBreadcumbLabelFunction, DashboardResolver } from '@home/pages/dashboard/dashboard-routing.module';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { RuleChainType } from '@shared/models/rule-chain.models';
import {
  importRuleChainBreadcumbLabelFunction,
  RuleChainMetaDataResolver,
  ruleChainBreadcumbLabelFunction,
  RuleChainImportGuard,
  RuleChainResolver,
  RuleNodeComponentsResolver,
  TooltipsterResolver
} from '@home/pages/rulechain/rulechain-routing.module';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { MenuId } from '@core/services/menu.models';
import { IntegrationsComponent } from '@home/pages/integration/integration/integrations.component';
import { ConvertersComponent } from '@home/pages/integration/converter/converters.component';
import {
  IntegrationEntityDetailsPageComponent
} from '@home/pages/integration/integration-entity-details-page.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsComponent } from '@home/pages/entity-group/entity-groups.component';
import { GroupMembersPageComponent } from '@home/pages/entity-group/group-members-page.component';
import { EdgeGroupEntitiesTableConfigResolver } from '@home/pages/entity-group/group-entities-table-config.resolvers';
import { groupMembersPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';

const edgeIntegrationEntityBreadcrumbLabelFunction: BreadCrumbLabelFunction<IntegrationEntityDetailsPageComponent>
  = ((route, translate, component) => component.entityName);

const routes: Routes = [
  {
    path: 'edgeManagement',
    data: {
      breadcrumb: {
        menuId: MenuId.edge_management
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/edgeManagement/instances'
        }
      },
      {
        path: 'instances',
        data: {
          breadcrumb: {
            menuId: MenuId.edges
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.edge-instances',
              edgesType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'router'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.edge-instances',
              edgesType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          },
          {
            path: ':edgeId/assets',
            data: {
              breadcrumb: {
                label: 'edge.assets',
                icon: 'domain'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'edge.assets',
                  assetsType: 'edge'
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
                  title: 'edge.assets',
                  assetsType: 'edge'
                },
                resolve: {
                  entitiesTableConfig: AssetsTableConfigResolver
                }
              }
            ]
          },
          {
            path: ':edgeId/devices',
            data: {
              breadcrumb: {
                label: 'edge.devices',
                icon: 'devices_other'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'edge.devices',
                  devicesType: 'edge'
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
                  title: 'edge.devices',
                  devicesType: 'edge'
                },
                resolve: {
                  entitiesTableConfig: DevicesTableConfigResolver
                }
              }
            ]
          },
          {
            path: ':edgeId/entityViews',
            data: {
              breadcrumb: {
                label: 'edge.entity-views',
                icon: 'view_quilt'
              },
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'edge.entity-views',
                  entityViewsType: 'edge'
                },
                resolve: {
                  entitiesTableConfig: EntityViewsTableConfigResolver
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
                  title: 'edge.entity-views',
                  entityViewsType: 'edge'
                },
                resolve: {
                  entitiesTableConfig: EntityViewsTableConfigResolver
                }
              }
            ]
          },
          {
            path: ':edgeId/dashboards',
            data: {
              breadcrumb: {
                label: 'edge.dashboards',
                icon: 'dashboard'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  dashboardsType: 'edge'
                },
                resolve: {
                  entitiesTableConfig: DashboardsTableConfigResolver
                },
              },
              {
                path: ':dashboardId',
                component: DashboardPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: dashboardBreadcumbLabelFunction,
                    icon: 'dashboard'
                  } as BreadCrumbConfig<DashboardPageComponent>,
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'edge.dashboard',
                  widgetEditMode: false
                },
                resolve: {
                  dashboard: DashboardResolver
                }
              }
            ]
          },
          {
            path: ':edgeId/ruleChains',
            data: {
              breadcrumb: {
                label: 'edge.rulechains',
                icon: 'settings_ethernet'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN],
                  title: 'edge.rulechains',
                  ruleChainsType: 'edge'
                },
                resolve: {
                  entitiesTableConfig: RuleChainsTableConfigResolver
                }
              },
              {
                path: ':ruleChainId',
                component: RuleChainPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: ruleChainBreadcumbLabelFunction,
                    icon: 'settings_ethernet'
                  } as BreadCrumbConfig<RuleChainPageComponent>,
                  auth: [Authority.TENANT_ADMIN],
                  title: 'rulechain.edge-rulechain',
                  import: false,
                  ruleChainType: RuleChainType.EDGE
                },
                loadChildren: () => import('../rulechain/rulechain-page.module').then(m => m.RuleChainPageModule),
                resolve: {
                  ruleChain: RuleChainResolver,
                  ruleChainMetaData: RuleChainMetaDataResolver,
                  ruleNodeComponents: RuleNodeComponentsResolver,
                  tooltipster: TooltipsterResolver
                }
              }
            ]
          }
        ]
      },
      {
        // Edge 实例本身可以分组（PE 4.3.1.1 的 groupTypes 含 EDGE）。
        // 结构镜像 asset-routing 的 groups 分支。注意每个叶子路由都要带 auth ——
        // AuthGuard 只看最深一层 activated route 的 data.auth，父路由上的不生效。
        path: 'groups',
        data: {
          auth: [Authority.TENANT_ADMIN],
          breadcrumb: {
            label: 'entity-group.group-type-edge',
            icon: 'workspaces'
          }
        },
        children: [
          {
            path: '',
            component: EntityGroupsComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'entity-group.group-type-edge',
              groupType: EntityType.EDGE
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
                  title: 'entity-group.group-type-edge',
                  edgesType: 'tenant',
                  groupType: EntityType.EDGE
                },
                resolve: {
                  entitiesTableConfig: EdgeGroupEntitiesTableConfigResolver
                }
              },
              {
                path: ':entityId',
                component: EntityDetailsPageComponent,
                canDeactivate: [ConfirmOnExitGuard],
                data: {
                  breadcrumb: {
                    labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                    icon: 'router'
                  } as BreadCrumbConfig<EntityDetailsPageComponent>,
                  auth: [Authority.TENANT_ADMIN],
                  title: 'edge.edge-instances',
                  edgesType: 'tenant',
                  groupType: EntityType.EDGE
                },
                resolve: {
                  entitiesTableConfig: EdgeGroupEntitiesTableConfigResolver
                }
              }
            ]
          }
        ]
      },
      {
        path: 'ruleChains',
        data: {
          breadcrumb: {
            menuId: MenuId.rulechain_templates
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'edge.rulechain-templates',
              ruleChainsType: 'edges'
            },
            resolve: {
              entitiesTableConfig: RuleChainsTableConfigResolver
            }
          },
          {
            path: ':ruleChainId',
            component: RuleChainPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: ruleChainBreadcumbLabelFunction,
                icon: 'settings_ethernet'
              } as BreadCrumbConfig<RuleChainPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'rulechain.edge-rulechain',
              import: false,
              ruleChainType: RuleChainType.EDGE
            },
            loadChildren: () => import('../rulechain/rulechain-page.module').then(m => m.RuleChainPageModule),
            resolve: {
              ruleChain: RuleChainResolver,
              ruleChainMetaData: RuleChainMetaDataResolver,
              ruleNodeComponents: RuleNodeComponentsResolver,
              tooltipster: TooltipsterResolver
            }
          },
          {
            path: 'ruleChain/import',
            component: RuleChainPageComponent,
            canActivate: [RuleChainImportGuard],
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: importRuleChainBreadcumbLabelFunction,
                icon: 'settings_ethernet'
              } as BreadCrumbConfig<RuleChainPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'rulechain.edge-rulechain',
              import: true,
              ruleChainType: RuleChainType.EDGE
            },
            loadChildren: () => import('../rulechain/rulechain-page.module').then(m => m.RuleChainPageModule),
            resolve: {
              ruleNodeComponents: RuleNodeComponentsResolver,
              tooltipster: TooltipsterResolver
            }
          }
        ]
      },
      // 集成 / 转换器模板。复用集成中心那套组件，靠 data.isEdgeTemplate 切换命名空间。
      // auth 必须写在叶子路由上：CE 的 AuthGuard 只看最深一层 activated route 的 data.auth，
      // 写在这一层的分组节点上不会生效。
      {
        path: 'integrations',
        data: {
          breadcrumb: {
            menuId: MenuId.integration_templates
          }
        },
        children: [
          {
            path: '',
            component: IntegrationsComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'edge.edge-integration-templates',
              isEdgeTemplate: true
            }
          },
          {
            path: ':integrationId',
            component: IntegrationEntityDetailsPageComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'edge.edge-integration-templates',
              kind: 'integration',
              isEdgeTemplate: true,
              breadcrumb: {
                labelFunction: edgeIntegrationEntityBreadcrumbLabelFunction,
                icon: 'input'
              } as BreadCrumbConfig<IntegrationEntityDetailsPageComponent>
            }
          }
        ]
      },
      {
        path: 'converters',
        data: {
          breadcrumb: {
            menuId: MenuId.converter_templates
          }
        },
        children: [
          {
            path: '',
            component: ConvertersComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'edge.edge-converter-templates',
              isEdgeTemplate: true
            }
          },
          {
            path: ':converterId',
            component: IntegrationEntityDetailsPageComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'edge.edge-converter-templates',
              kind: 'converter',
              isEdgeTemplate: true,
              breadcrumb: {
                labelFunction: edgeIntegrationEntityBreadcrumbLabelFunction,
                icon: 'transform'
              } as BreadCrumbConfig<IntegrationEntityDetailsPageComponent>
            }
          }
        ]
      }
    ]
  },
  {
    path: 'edgeInstances',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/instances'
  },
  {
    path: 'edgeInstances/:entityId',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/instances/:entityId'
  },
  {
    path: 'edgeInstances/:edgeId/assets',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/instances/:edgeId/assets'
  },
  {
    path: 'edgeInstances/:edgeId/assets/:entityId',
    redirectTo: '/edgeManagement/instances/:edgeId/assets/:entityId'
  },
  {
    path: 'edgeInstances/:edgeId/devices',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/instances/:edgeId/devices'
  },
  {
    path: 'edgeInstances/:edgeId/devices/:entityId',
    redirectTo: '/edgeManagement/instances/:edgeId/devices/:entityId'
  },
  {
    path: 'edgeInstances/:edgeId/entityViews',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/instances/:edgeId/entityViews'
  },
  {
    path: 'edgeInstances/:edgeId/entityViews/:entityId',
    redirectTo: '/edgeManagement/instances/:edgeId/entityViews/:entityId'
  },
  {
    path: 'edgeInstances/:edgeId/dashboards',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/instances/:edgeId/dashboards'
  },
  {
    path: 'edgeInstances/:edgeId/dashboards/:dashboardId',
    redirectTo: '/edgeManagement/instances/:edgeId/dashboards/:dashboardId'
  },
  {
    path: 'edgeInstances/:edgeId/ruleChains',
    pathMatch: 'full',
    redirectTo: '/edgeManagement/instances/:edgeId/ruleChains'
  },
  {
    path: 'edgeInstances/:edgeId/ruleChains/:ruleChainId',
    redirectTo: '/edgeManagement/instances/:edgeId/ruleChains/:ruleChainId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EdgesTableConfigResolver,
    EdgeGroupEntitiesTableConfigResolver
  ]
})
export class EdgeRoutingModule {
}
