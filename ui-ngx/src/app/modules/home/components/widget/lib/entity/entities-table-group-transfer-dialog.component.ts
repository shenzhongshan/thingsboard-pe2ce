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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { EntityType } from '@shared/models/entity-type.models';
import { entityGroupTypes } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityId } from '@shared/models/id/entity-id';

export interface EntitiesTableGroupTransferDialogData {
  mode: 'add' | 'remove';
  entities: Array<{id: EntityId; entityName: string}>;
  currentGroupType?: EntityType;
  currentGroupId?: string;
}

@Component({
    selector: 'tb-entities-table-group-transfer-dialog',
    templateUrl: './entities-table-group-transfer-dialog.component.html',
    standalone: false
})
export class EntitiesTableGroupTransferDialogComponent extends DialogComponent<EntitiesTableGroupTransferDialogComponent, boolean> {

  mode = this.data.mode;
  entities = this.data.entities;
  groupableEntityTypes = entityGroupTypes;
  entityType = EntityType;

  transferFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntitiesTableGroupTransferDialogData,
              public dialogRef: MatDialogRef<EntitiesTableGroupTransferDialogComponent, boolean>,
              private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.transferFormGroup = this.fb.group({
      groupType: [{value: this.data.currentGroupType, disabled: this.mode === 'remove'},
        this.mode === 'add' ? [Validators.required] : []],
      targetGroup: [{value: null, disabled: this.mode === 'remove'},
        this.mode === 'add' ? [Validators.required] : []],
      selectedEntityIds: [[], [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  transfer(): void {
    if (this.transferFormGroup.invalid) {
      this.transferFormGroup.markAllAsTouched();
      return;
    }
    this.submitted = true;
    const selectedEntityIds: string[] = this.transferFormGroup.get('selectedEntityIds').value;
    if (this.mode === 'add') {
      const targetGroup = this.transferFormGroup.get('targetGroup').value;
      const targetGroupId = typeof targetGroup === 'string' ? targetGroup : targetGroup?.id?.id;
      this.entityGroupService.addEntitiesToEntityGroup(targetGroupId, selectedEntityIds).subscribe({
        next: () => this.dialogRef.close(true),
        error: () => this.submitted = false
      });
    } else {
      this.entityGroupService.removeEntitiesFromEntityGroup(this.data.currentGroupId, selectedEntityIds).subscribe({
        next: () => this.dialogRef.close(true),
        error: () => this.submitted = false
      });
    }
  }

}
