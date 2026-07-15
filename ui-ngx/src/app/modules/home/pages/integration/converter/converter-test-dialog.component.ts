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
import { ConverterService, TestConverterResult } from '@core/http/converter.service';
import { ConverterScriptLanguage, ConverterType } from '@shared/models/converter.models';

export interface ConverterTestDialogData {
  type: ConverterType;
  script: string;
  scriptLang: ConverterScriptLanguage;
}

@Component({
  selector: 'tb-converter-test-dialog',
  templateUrl: './converter-test-dialog.component.html',
  styleUrls: ['./converter-test-dialog.component.scss'],
  standalone: false
})
export class ConverterTestDialogComponent {

  form: UntypedFormGroup;
  isUplink = this.data.type === ConverterType.UPLINK;
  result: TestConverterResult;
  testing = false;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ConverterTestDialogData,
              private dialogRef: MatDialogRef<ConverterTestDialogComponent, void>,
              private converterService: ConverterService,
              private fb: UntypedFormBuilder) {
    this.form = this.fb.group(this.isUplink ? {
      payload: ['{\n  "deviceName": "Device A",\n  "deviceType": "thermostat",\n  "temperature": 22,\n  "humidity": 51\n}', [Validators.required]],
      metadata: ['{\n  "integrationName": "Test integration"\n}', [Validators.required]]
    } : {
      msg: ['{\n  "temp": 22\n}', [Validators.required]],
      msgType: ['POST_TELEMETRY_REQUEST', [Validators.required]],
      metadata: ['{}', [Validators.required]],
      integrationMetadata: ['{}', [Validators.required]]
    });
  }

  private parseJson(raw: string): any {
    try {
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }

  runTest(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    this.testing = true;
    this.result = null;
    if (this.isUplink) {
      const payloadJson = this.parseJson(v.payload);
      const metadata = this.parseJson(v.metadata);
      if (payloadJson === null || metadata === null) {
        this.result = { output: '', error: 'Invalid JSON' };
        this.testing = false;
        return;
      }
      const payloadBase64 = btoa(unescape(encodeURIComponent(JSON.stringify(payloadJson))));
      this.converterService.testUpLink({
        metadata,
        payload: payloadBase64,
        decoder: this.data.script
      }, this.data.scriptLang).subscribe({
        next: (res) => { this.result = res; this.testing = false; },
        error: () => { this.testing = false; }
      });
    } else {
      const metadata = this.parseJson(v.metadata);
      const integrationMetadata = this.parseJson(v.integrationMetadata);
      if (metadata === null || integrationMetadata === null) {
        this.result = { output: '', error: 'Invalid JSON' };
        this.testing = false;
        return;
      }
      this.converterService.testDownLink({
        metadata,
        payload: '',
        msg: v.msg,
        msgType: v.msgType,
        integrationMetadata,
        encoder: this.data.script
      }, this.data.scriptLang).subscribe({
        next: (res) => { this.result = res; this.testing = false; },
        error: () => { this.testing = false; }
      });
    }
  }

  close(): void {
    this.dialogRef.close();
  }

}
