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
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { LOCALE_METAS } from '@shared/models/custom-translation.models';

export interface CustomTranslationAddDialogData {
  existingCodes: string[];
}

@Component({
  selector: 'tb-custom-translation-add-dialog',
  templateUrl: './custom-translation-add-dialog.component.html',
  standalone: false
})
export class CustomTranslationAddDialogComponent {

  form: UntypedFormGroup;
  localeOptions = LOCALE_METAS.filter(m => !this.data.existingCodes.includes(m.code));
  uploadedValue: any = null;
  uploadedFileName: string = null;

  constructor(@Inject(MAT_DIALOG_DATA) public data: CustomTranslationAddDialogData,
              private dialogRef: MatDialogRef<CustomTranslationAddDialogComponent, string>,
              protected store: Store<AppState>,
              private customTranslationService: CustomTranslationService,
              public fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      localeCode: ['', [Validators.required, Validators.pattern(/^[a-z]{2}(_[A-Z]{2})?$/)]]
    });
  }

  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) {
      return;
    }
    this.uploadedFileName = file.name;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        this.uploadedValue = JSON.parse(reader.result as string);
      } catch (e) {
        this.uploadedValue = null;
      }
    };
    reader.readAsText(file);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const localeCode = this.form.value.localeCode;
    this.customTranslationService.saveCustomTranslation(localeCode, this.uploadedValue || {})
      .subscribe(() => this.dialogRef.close(localeCode));
  }

}
