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

import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { SecretService } from '@core/http/secret.service';
import { Secret, SecretType } from '@shared/models/secret.models';

const MAX_TEXT_LENGTH = 2048;
const MAX_FILE_SIZE_BYTES = 512 * 1024;

@Component({
  selector: 'tb-secret-dialog',
  templateUrl: './secret-dialog.component.html',
  standalone: false
})
export class SecretDialogComponent extends DialogComponent<SecretDialogComponent, boolean> implements OnInit {

  secretType = SecretType;
  maxFileSizeBytes = MAX_FILE_SIZE_BYTES;

  secretFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private secretService: SecretService,
              public dialogRef: MatDialogRef<SecretDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.secretFormGroup = this.fb.group({
      type: [SecretType.TEXT, [Validators.required]],
      name: [null, [Validators.required]],
      value: [null, [Validators.required]],
      description: [null, [Validators.maxLength(255)]]
    });
    this.secretFormGroup.get('type').valueChanges.subscribe(() => this.updateValueValidators());
    this.updateValueValidators();
  }

  private updateValueValidators() {
    const valueCtrl = this.secretFormGroup.get('value');
    valueCtrl.setValue(null);
    valueCtrl.setValidators(this.secretFormGroup.get('type').value === SecretType.TEXT ?
      [Validators.required, Validators.maxLength(MAX_TEXT_LENGTH)] : [Validators.required]);
    valueCtrl.updateValueAndValidity();
  }

  close(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    this.secretFormGroup.markAsPristine();
    const secret: Secret = this.secretFormGroup.value;
    this.secretService.saveSecret(secret).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

}
