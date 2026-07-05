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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupService } from '@core/http/entity-group.service';

export interface AddGroupEntitiesDialogData {
  entityGroupId: string;
  groupType: EntityType;
}

@Component({
  selector: 'tb-add-group-entities-dialog',
  templateUrl: './add-group-entities-dialog.component.html',
  standalone: false
})
export class AddGroupEntitiesDialogComponent {

  form: UntypedFormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: AddGroupEntitiesDialogData,
              private dialogRef: MatDialogRef<AddGroupEntitiesDialogComponent, boolean>,
              protected store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      entityIds: [[], [Validators.required]]
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  add(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    this.entityGroupService.addEntitiesToEntityGroup(this.data.entityGroupId, this.form.value.entityIds)
      .subscribe(() => this.dialogRef.close(true));
  }

}
