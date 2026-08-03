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
import { entityGroupActionSources, EntityGroupAction } from '@shared/models/entity-group.models';

export interface EntityGroupActionDialogData {
  action?: EntityGroupAction;
}

@Component({
    selector: 'tb-entity-group-action-dialog',
    templateUrl: './entity-group-action-dialog.component.html',
    standalone: false
})
export class EntityGroupActionDialogComponent {

  isEdit: boolean;
  form: UntypedFormGroup;
  actionSources = entityGroupActionSources;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EntityGroupActionDialogData,
              private dialogRef: MatDialogRef<EntityGroupActionDialogComponent, EntityGroupAction>,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.action;
    const action = data.action;
    this.form = this.fb.group({
      source: [action?.source || 'row', [Validators.required]],
      name: [action?.name || '', [Validators.required]],
      icon: [action?.icon || 'flash_on', [Validators.required]],
      type: [action?.type || 'custom', [Validators.required]],
      dashboardId: [action?.dashboardId || null],
      dashboardState: [action?.dashboardState || ''],
      customFunction: [action?.customFunction || '']
    });
  }

  save(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close({...(this.data.action || {}), ...this.form.value});
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

}
