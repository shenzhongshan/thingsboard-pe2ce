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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { BaseData, HasId } from '@shared/models/base-data';

@Component({
    selector: 'tb-group-entity-table-header',
    templateUrl: './group-entity-table-header.component.html',
    styleUrls: ['./group-entity-table-header.component.scss'],
    standalone: false
})
export class GroupEntityTableHeaderComponent extends EntityTableHeaderComponent<BaseData<HasId>> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  get groupTableTitle(): string {
    return this.entitiesTableConfig.componentsData?.groupTableTitle;
  }

  openGroupDetails() {
    this.entitiesTableConfig.componentsData?.openGroupDetails?.();
  }

}
