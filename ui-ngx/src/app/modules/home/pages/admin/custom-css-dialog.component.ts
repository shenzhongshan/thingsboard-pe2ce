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

export interface CustomCssDialogData {
  customCss: string;
  readonly: boolean;
}

@Component({
  selector: 'tb-custom-css-dialog',
  templateUrl: './custom-css-dialog.component.html',
  standalone: false
})
export class CustomCssDialogComponent {

  customCss: string;

  constructor(@Inject(MAT_DIALOG_DATA) public data: CustomCssDialogData,
              private dialogRef: MatDialogRef<CustomCssDialogComponent, string>) {
    this.customCss = data.customCss || '';
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.customCss ?? '');
  }
}
