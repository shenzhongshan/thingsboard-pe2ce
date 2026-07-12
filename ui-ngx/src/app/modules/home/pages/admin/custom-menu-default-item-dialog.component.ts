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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

export interface CustomMenuDefaultItemDialogData {
  baseName: string;
  icon: string;
  name: string;
  visible: boolean;
}

export interface CustomMenuDefaultItemDialogResult {
  icon: string;
  name: string;
  visible: boolean;
}

// Quick-edit surface for a built-in (DEFAULT) menu item: visible + icon + rename only.
// Item type/link configuration doesn't apply here — that's exclusive to CUSTOM items.
@Component({
  selector: 'tb-custom-menu-default-item-dialog',
  templateUrl: './custom-menu-default-item-dialog.component.html',
  standalone: false
})
export class CustomMenuDefaultItemDialogComponent {

  form: UntypedFormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: CustomMenuDefaultItemDialogData,
              private dialogRef: MatDialogRef<CustomMenuDefaultItemDialogComponent, CustomMenuDefaultItemDialogResult>,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      visible: [data.visible],
      icon: [data.icon],
      name: [data.name]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  apply(): void {
    this.dialogRef.close(this.form.value);
  }
}
