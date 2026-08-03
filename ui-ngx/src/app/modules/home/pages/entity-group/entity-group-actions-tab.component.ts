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
import { MatDialog } from '@angular/material/dialog';
import { EntityGroup, EntityGroupAction } from '@shared/models/entity-group.models';
import {
  EntityGroupActionDialogComponent,
  EntityGroupActionDialogData
} from '@home/pages/entity-group/entity-group-action-dialog.component';

// Edited in place on the shared draft `entity` — see entity-group-columns-tab.component.ts for why
// there's no separate save button (PE saves 详情/列/设置/操作 together via the drawer's global apply).
@Component({
    selector: 'tb-entity-group-actions-tab',
    templateUrl: './entity-group-actions-tab.component.html',
    standalone: false
})
export class EntityGroupActionsTabComponent implements OnChanges {

  @Input() entity: EntityGroup;
  @Input() isEdit: boolean;
  @Input() detailsForm: UntypedFormGroup;

  actions: EntityGroupAction[] = [];
  filteredActions: EntityGroupAction[] = [];
  searchMode = false;
  textSearch = '';

  constructor(private dialog: MatDialog) {
  }

  ngOnChanges() {
    this.actions = [...(this.entity?.configuration?.actions || [])];
    this.applyFilter();
  }

  enterSearchMode() {
    this.searchMode = true;
  }

  exitSearchMode() {
    this.searchMode = false;
    this.textSearch = '';
    this.applyFilter();
  }

  searchChanged() {
    this.applyFilter();
  }

  private applyFilter() {
    const search = this.textSearch?.toLowerCase();
    this.filteredActions = !search ? this.actions : this.actions.filter(a => a.name?.toLowerCase().includes(search));
  }

  addAction() {
    if (!this.isEdit) {
      return;
    }
    this.dialog.open<EntityGroupActionDialogComponent, EntityGroupActionDialogData, EntityGroupAction>(
      EntityGroupActionDialogComponent, {disableClose: true, data: {}}
    ).afterClosed().subscribe((action) => {
      if (action) {
        this.actions.push(action);
        this.applyFilter();
        this.syncToEntity();
      }
    });
  }

  editAction($event: Event, action: EntityGroupAction, index: number) {
    $event.stopPropagation();
    if (!this.isEdit) {
      return;
    }
    this.dialog.open<EntityGroupActionDialogComponent, EntityGroupActionDialogData, EntityGroupAction>(
      EntityGroupActionDialogComponent, {disableClose: true, data: {action}}
    ).afterClosed().subscribe((updated) => {
      if (updated) {
        this.actions[index] = updated;
        this.applyFilter();
        this.syncToEntity();
      }
    });
  }

  removeAction($event: Event, index: number) {
    $event.stopPropagation();
    this.actions.splice(index, 1);
    this.applyFilter();
    this.syncToEntity();
  }

  private syncToEntity() {
    if (this.entity) {
      this.entity.configuration = {...(this.entity.configuration || {}), actions: this.actions};
    }
    this.detailsForm?.markAsDirty();
  }

}
