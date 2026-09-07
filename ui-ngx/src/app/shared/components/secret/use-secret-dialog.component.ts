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
import { SecretService } from '@core/http/secret.service';
import { Secret, SecretType } from '@shared/models/secret.models';
import { secretPlaceholder } from '@shared/models/secret.models';

export interface UseSecretDialogData {
  /** TEXT for password-like values, TEXT_FILE for certificate/key file contents. */
  secretType: SecretType;
}

/**
 * PE's "使用密钥" dialog: pick an existing secret from the vault, or create one inline. Returns the
 * `${secret:<name>;type:<type>}` placeholder string, which is what gets persisted in the entity
 * configuration — the value itself never travels through the config, and is only substituted
 * server-side by SecretConfigurationService when the connection is actually opened.
 */
@Component({
  selector: 'tb-use-secret-dialog',
  templateUrl: './use-secret-dialog.component.html',
  styleUrls: ['./use-secret-dialog.component.scss'],
  standalone: false
})
export class UseSecretDialogComponent implements OnInit {

  /** 'existing' = PE's 使用存储 tab, 'new' = 新建. */
  mode: 'existing' | 'new' = 'existing';
  existingForm: UntypedFormGroup;
  newForm: UntypedFormGroup;
  secretNames: string[] = [];
  loading = true;
  saving = false;
  hideValue = true;
  /** PE's cap for an uploaded secret file. */
  readonly maxFileSizeByte = 512 * 1024;

  constructor(@Inject(MAT_DIALOG_DATA) public data: UseSecretDialogData,
              private dialogRef: MatDialogRef<UseSecretDialogComponent, string>,
              protected store: Store<AppState>,
              private secretService: SecretService,
              private fb: UntypedFormBuilder) {
  }

  ngOnInit() {
    this.existingForm = this.fb.group({ name: [null, [Validators.required]] });
    this.newForm = this.fb.group({
      name: ['', [Validators.required]],
      value: ['', this.isFileSecret ? [Validators.required] : [Validators.required, Validators.maxLength(2048)]],
      description: ['', [Validators.maxLength(255)]]
    });
    this.secretService.getSecretNames({ ignoreErrors: true }).subscribe({
      next: (names) => {
        this.secretNames = names || [];
        this.loading = false;
        // Nothing stored yet — land the user on the tab they can actually use.
        if (!this.secretNames.length) {
          this.mode = 'new';
        }
      },
      error: () => {
        this.secretNames = [];
        this.loading = false;
        this.mode = 'new';
      }
    });
  }

  get secretType(): SecretType {
    return this.data.secretType;
  }

  /** PE swaps the whole "create" tab for a file uploader when the secret holds file content. */
  get isFileSecret(): boolean {
    return this.secretType === SecretType.TEXT_FILE;
  }

  cancel() {
    this.dialogRef.close();
  }

  apply() {
    if (this.mode === 'existing') {
      if (this.existingForm.invalid) {
        this.existingForm.markAllAsTouched();
        return;
      }
      this.dialogRef.close(secretPlaceholder(this.existingForm.get('name').value, this.secretType));
      return;
    }
    if (this.newForm.invalid) {
      this.newForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    const v = this.newForm.value;
    const secret: Secret = {
      name: v.name,
      type: this.secretType,
      value: v.value,
      description: v.description || undefined
    } as Secret;
    this.secretService.saveSecret(secret).subscribe({
      next: (saved) => this.dialogRef.close(secretPlaceholder(saved.name, this.secretType)),
      error: () => { this.saving = false; }
    });
  }

}
