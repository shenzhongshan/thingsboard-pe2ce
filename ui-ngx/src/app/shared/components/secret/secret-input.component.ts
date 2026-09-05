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

import { Component, forwardRef, Input, ViewChild, ElementRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { SecretType, isSecretPlaceholder, secretPlaceholderName } from '@shared/models/secret.models';
import {
  UseSecretDialogComponent,
  UseSecretDialogData
} from '@shared/components/secret/use-secret-dialog.component';

/**
 * PE's `tb-secret-key-input` / `tb-secret-file-input`: a credential field whose value is either
 * typed in literally, or replaced by a `${secret:<name>;type:<type>}` reference into the secret
 * vault. The control's value is that string either way, so hosts need no special handling — the
 * server substitutes the real value at connection time (SecretConfigurationService).
 *
 * `mode='password'` renders PE's single-line field with the eye + key suffix buttons.
 * `mode='file'` renders PE's two-button block (浏览文件 / 使用密钥存储) used for PEM certificates,
 * where a browsed file's text content becomes the literal value.
 */
@Component({
  selector: 'tb-secret-input',
  templateUrl: './secret-input.component.html',
  styleUrls: ['./secret-input.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SecretInputComponent),
    multi: true
  }],
  standalone: false
})
export class SecretInputComponent implements ControlValueAccessor {

  @Input() mode: 'password' | 'file' = 'password';
  @Input() label: string;
  @Input() required = false;
  /** Vault entry type to pick/create. Defaults follow the mode. */
  @Input() secretType: SecretType;

  @ViewChild('fileInput') fileInput: ElementRef<HTMLInputElement>;

  value: string = '';
  disabled = false;
  hide = true;
  /** Name of the browsed file, shown instead of dumping a PEM blob into the UI. */
  fileName: string = null;

  private propagateChange: (v: any) => void = () => {};

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog) {
  }

  writeValue(value: string): void {
    this.value = value || '';
    this.fileName = null;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  get effectiveSecretType(): SecretType {
    return this.secretType || (this.mode === 'file' ? SecretType.TEXT_FILE : SecretType.TEXT);
  }

  /** True when the value references the vault rather than holding the credential inline. */
  get usesSecret(): boolean {
    return isSecretPlaceholder(this.value);
  }

  get secretName(): string {
    return secretPlaceholderName(this.value);
  }

  onInput(value: string) {
    this.value = value;
    this.propagateChange(value);
  }

  openSecretDialog() {
    this.dialog.open<UseSecretDialogComponent, UseSecretDialogData, string>(UseSecretDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog-gt-xs'],
      data: { secretType: this.effectiveSecretType }
    }).afterClosed().subscribe((placeholder) => {
      if (placeholder) {
        this.fileName = null;
        this.value = placeholder;
        this.propagateChange(placeholder);
      }
    });
  }

  browseFile() {
    this.fileInput?.nativeElement.click();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      this.fileName = file.name;
      this.value = String(reader.result || '');
      this.propagateChange(this.value);
    };
    reader.readAsText(file);
    // Allow re-selecting the same file; without this a second pick of the same path fires nothing.
    input.value = '';
  }

  clear() {
    this.fileName = null;
    this.value = '';
    this.propagateChange('');
  }

}
