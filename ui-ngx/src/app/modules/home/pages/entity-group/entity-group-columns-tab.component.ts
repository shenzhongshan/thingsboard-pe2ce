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
import { UntypedFormGroup } from '@angular/forms';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {
  defaultEntityGroupColumns,
  EntityGroup,
  EntityGroupColumn,
  entityGroupDefaultFields
} from '@shared/models/entity-group.models';

// Edited in place on the shared draft `entity` — PE saves 详情/列/设置/操作 together via the single
// global edit/apply toggle on the details drawer, not per-tab save buttons. Mutations here write
// straight into entity.configuration so entity-details-panel's saveEntity() picks them up.
@Component({
    selector: 'tb-entity-group-columns-tab',
    templateUrl: './entity-group-columns-tab.component.html',
    standalone: false
})
export class EntityGroupColumnsTabComponent implements OnChanges {

  @Input() entity: EntityGroup;
  @Input() isEdit: boolean;
  @Input() detailsForm: UntypedFormGroup;

  columns: EntityGroupColumn[] = [];

  ngOnChanges() {
    const saved = this.entity?.configuration?.columns;
    this.columns = saved?.length ? [...saved] : defaultEntityGroupColumns(this.entity?.type);
    this.syncToEntity();
  }

  entityFieldOptions(): {key: string; label: string}[] {
    return entityGroupDefaultFields[this.entity?.type] ? [
      {key: 'createdTime', label: 'common.created-time'},
      ...entityGroupDefaultFields[this.entity.type]
    ] : [];
  }

  onTypeChanged(column: EntityGroupColumn) {
    // Reset the value when switching column type — an ENTITY_FIELD key is meaningless for
    // ATTRIBUTE/TIMESERIES and vice versa.
    column.key = '';
    column.label = '';
    this.onFieldChanged();
  }

  onEntityFieldChanged(column: EntityGroupColumn) {
    const field = this.entityFieldOptions().find(f => f.key === column.key);
    column.label = field?.label || '';
    this.onFieldChanged();
  }

  addColumn() {
    this.columns.push({type: 'ENTITY_FIELD', key: '', label: '', sortOrder: 'NONE', hideOnMobile: false});
    this.onFieldChanged();
  }

  removeColumn(index: number) {
    this.columns.splice(index, 1);
    this.onFieldChanged();
  }

  drop(event: CdkDragDrop<EntityGroupColumn[]>) {
    moveItemInArray(this.columns, event.previousIndex, event.currentIndex);
    this.onFieldChanged();
  }

  // Called on every user-driven edit (not on the initial ngOnChanges sync) — marks the shared
  // 详情-tab form dirty so the drawer's single global apply/done button becomes enabled, since
  // PE saves 详情/列/设置/操作 together and this tab's edits don't otherwise touch that form.
  onFieldChanged() {
    this.syncToEntity();
    this.detailsForm?.markAsDirty();
  }

  private syncToEntity() {
    if (this.entity) {
      this.entity.configuration = {...(this.entity.configuration || {}), columns: this.columns};
    }
  }

}
