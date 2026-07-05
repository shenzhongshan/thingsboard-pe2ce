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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup } from '@shared/models/entity-group.models';

export interface EntityGroupDialogData {
  entityGroupId?: string;
  groupType: EntityType;
}

@Component({
  selector: 'tb-entity-group-dialog',
  templateUrl: './entity-group-dialog.component.html',
  standalone: false
})
export class EntityGroupDialogComponent implements OnInit {

  form: UntypedFormGroup;
  isEdit = false;
  loaded = false;

  private entityGroup: EntityGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EntityGroupDialogData,
              private dialogRef: MatDialogRef<EntityGroupDialogComponent, boolean>,
              protected store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.entityGroupId;
  }

  ngOnInit() {
    if (this.isEdit) {
      this.entityGroupService.getEntityGroup(this.data.entityGroupId).subscribe((group) => {
        this.entityGroup = group;
        this.buildForm(group);
        this.loaded = true;
      });
    } else {
      this.entityGroup = {type: this.data.groupType, name: '', additionalInfo: {}};
      this.buildForm(this.entityGroup);
      this.loaded = true;
    }
  }

  private buildForm(group: EntityGroup) {
    this.form = this.fb.group({
      name: [group.name, [Validators.required]],
      description: [group.additionalInfo?.description || '']
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const group: EntityGroup = {
      ...this.entityGroup,
      name: v.name,
      additionalInfo: { ...(this.entityGroup.additionalInfo || {}), description: v.description }
    };
    this.entityGroupService.saveEntityGroup(group).subscribe(() => this.dialogRef.close(true));
  }

}
