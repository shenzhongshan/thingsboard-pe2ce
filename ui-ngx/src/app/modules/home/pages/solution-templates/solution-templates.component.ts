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

import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { SolutionService } from '@core/http/solution.service';
import { TenantSolutionTemplateInfo } from '@shared/models/solution-template.models';

@Component({
    selector: 'tb-solution-templates',
    templateUrl: './solution-templates.component.html',
    styleUrls: ['./solution-templates.component.scss'],
    standalone: false
})
export class SolutionTemplatesComponent extends PageComponent {

  solutionTemplates: TenantSolutionTemplateInfo[];

  constructor(protected store: Store<AppState>,
              private solutionService: SolutionService,
              private route: ActivatedRoute) {
    super(store);
    this.solutionTemplates = this.route.snapshot.data.solutionTemplates;
  }

  reload(): void {
    this.solutionService.getSolutionTemplateInfos().subscribe(solutionTemplates => {
      this.solutionTemplates = solutionTemplates;
    });
  }

}
