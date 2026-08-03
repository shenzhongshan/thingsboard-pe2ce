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

import { Component, Input, OnChanges } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { EntityGroup, EntityGroupSettings } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';

// Edited in place on the shared draft `entity` — see entity-group-columns-tab.component.ts for why
// there's no separate save button (PE saves 详情/列/设置/操作 together via the drawer's global apply).
@Component({
    selector: 'tb-entity-group-settings-tab',
    templateUrl: './entity-group-settings-tab.component.html',
    styleUrls: ['./entity-group-settings-tab.component.scss'],
    standalone: false
})
export class EntityGroupSettingsTabComponent implements OnChanges {

  @Input() entity: EntityGroup;
  @Input() isEdit: boolean;
  @Input() detailsForm: UntypedFormGroup;

  form: UntypedFormGroup;
  isDeviceGroup = false;

  constructor(private fb: UntypedFormBuilder) {
    this.form = this.buildForm({});
  }

  ngOnChanges() {
    this.isDeviceGroup = this.entity?.type === EntityType.DEVICE;
    this.form = this.buildForm(this.entity?.configuration?.settings || {});
    this.syncFormState();
    // Only fires on user edits (not this construction) — see entity-group-columns-tab.component.ts
    // for why the shared 详情-tab form needs to be marked dirty from here too.
    this.form.valueChanges.subscribe(() => {
      this.syncToEntity();
      this.detailsForm?.markAsDirty();
    });
  }

  // Mirrors entities-table.component.ts's pageSizeOptions generation (originally hardcoded
  // [defaultPageSize, defaultPageSize*2, defaultPageSize*3]) generalized to a configurable step
  // increment and step count — 默认页面大小 picks one of these generated candidates.
  get pageSizeOptions(): number[] {
    const increment = this.form?.get('pageStepIncrement')?.value || 10;
    const count = this.form?.get('pageStepCount')?.value || 3;
    const options: number[] = [];
    for (let i = 1; i <= count; i++) {
      options.push(increment * i);
    }
    return options;
  }

  private buildForm(settings: EntityGroupSettings): UntypedFormGroup {
    return this.fb.group({
      tableTitle: [settings.tableTitle || ''],
      enableSearch: [settings.enableSearch !== false],
      enableAdd: [settings.enableAdd !== false],
      enableDelete: [settings.enableDelete !== false],
      enableSelection: [settings.enableSelection !== false],
      enableGroupTransfer: [settings.enableGroupTransfer !== false],
      rowClickAction: [settings.rowClickAction || 'openDetails'],
      displayPagination: [settings.displayPagination !== false],
      pageStepIncrement: [settings.pageStepIncrement || 10],
      pageStepCount: [settings.pageStepCount || 3],
      defaultPageSize: [settings.defaultPageSize || 10],
      enableCredentialsManagement: [settings.enableCredentialsManagement !== false]
    });
  }

  private syncFormState() {
    if (this.isEdit) {
      this.form.enable({emitEvent: false});
    } else {
      this.form.disable({emitEvent: false});
    }
  }

  private syncToEntity() {
    if (this.entity) {
      this.entity.configuration = {...(this.entity.configuration || {}), settings: this.form.getRawValue()};
    }
  }

}
