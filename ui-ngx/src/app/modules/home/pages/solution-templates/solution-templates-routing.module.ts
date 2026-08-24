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
import { Observable } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { SolutionService } from '@core/http/solution.service';
import { TenantSolutionTemplateDetails, TenantSolutionTemplateInfo } from '@shared/models/solution-template.models';
import { SolutionTemplatesComponent } from '@home/pages/solution-templates/solution-templates.component';
import {
  SolutionTemplateDetailsComponent
} from '@home/pages/solution-templates/solution-template-details.component';

@Injectable()
export class SolutionTemplatesResolver {

  constructor(private solutionService: SolutionService) {
  }

  resolve(): Observable<TenantSolutionTemplateInfo[]> {
    return this.solutionService.getSolutionTemplateInfos();
  }

}

@Injectable()
export class SolutionTemplateDetailsResolver {

  constructor(private solutionService: SolutionService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<TenantSolutionTemplateDetails> {
    return this.solutionService.getSolutionTemplateDetails(route.params.solutionTemplateId);
  }

}

export const solutionTemplateDetailsBreadcrumbLabelFunction: BreadCrumbLabelFunction<SolutionTemplateDetailsComponent> =
  ((route, translate, component) => component.solutionTemplateDetails.title);

const routes: Routes = [
  {
    path: 'solutionTemplates',
    data: {
      breadcrumb: {
        menuId: MenuId.solution_templates
      }
    },
    children: [
      {
        path: '',
        component: SolutionTemplatesComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'solution-template.solution-templates'
        },
        resolve: {
          solutionTemplates: SolutionTemplatesResolver
        }
      },
      {
        path: ':solutionTemplateId',
        component: SolutionTemplateDetailsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'solution-template.solution-template',
          breadcrumb: {
            labelFunction: solutionTemplateDetailsBreadcrumbLabelFunction,
            icon: 'apps'
          } as BreadCrumbConfig<SolutionTemplateDetailsComponent>
        },
        resolve: {
          solutionTemplateDetails: SolutionTemplateDetailsResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    SolutionTemplatesResolver,
    SolutionTemplateDetailsResolver
  ]
})
export class SolutionTemplatesRoutingModule { }
