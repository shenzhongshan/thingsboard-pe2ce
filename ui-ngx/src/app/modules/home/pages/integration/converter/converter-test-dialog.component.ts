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
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { ContentType, contentTypesMap } from '@shared/models/constants';

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

  ContentType = ContentType;
  contentTypesMap = contentTypesMap;
  contentTypes = Object.values(ContentType);
  form: UntypedFormGroup;
  isUplink = this.data.type === ConverterType.UPLINK;
  result: TestConverterResult;
  testing = false;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ConverterTestDialogData,
              private dialogRef: MatDialogRef<ConverterTestDialogComponent, string>,
              private converterService: ConverterService,
              private fb: UntypedFormBuilder) {
    // PE's test dialog lets you edit the script right there and "保存" (Save) writes it back into
    // the caller's own form - matches that here via the dialog's close result instead of PE's direct
    // backend write, since this port's wizard-embedded converter isn't a persisted entity yet.
    this.form = this.fb.group(this.isUplink ? {
      payloadContentType: [ContentType.JSON],
      payload: ['{\n  "deviceName": "Device A",\n  "deviceType": "thermostat",\n  "temperature": 22,\n  "humidity": 51\n}', [Validators.required]],
      metadata: [{ integrationName: 'Test integration' }],
      script: [this.data.script],
      output: ['']
    } : {
      msg: ['{\n  "temp": 22\n}', [Validators.required]],
      msgType: ['POST_TELEMETRY_REQUEST', [Validators.required]],
      metadata: [{}],
      integrationMetadata: [{}],
      script: [this.data.script],
      output: ['']
    });
  }

  /** Drives the payload editor's syntax mode; downlink has no picker and is always JSON. */
  get payloadContentType(): ContentType {
    return this.form?.get('payloadContentType')?.value ?? ContentType.JSON;
  }

  get scriptEditorLang(): ScriptLanguage {
    return this.data.scriptLang === ConverterScriptLanguage.JS ? ScriptLanguage.JS : ScriptLanguage.TBEL;
  }

  /** PE ships separate TBEL / JS help docs; pick the one matching this converter's language. */
  get scriptHelpId(): string {
    const base = this.data.scriptLang === ConverterScriptLanguage.JS ? 'converter/' : 'converter/tbel/';
    return base + (this.isUplink ? 'decoder_fn' : 'encoder_fn');
  }

  private parseJson(raw: string): any {
    try {
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }

  runTest(): void {
    // Gate only on the input fields, NOT the whole form. `tb-js-func` attaches its own `jsFunc`
    // validator that fails whenever the script doesn't compile, and its `noValidate` flag only
    // short-circuits *after* a successful compile — so gating on form.valid made the button
    // silently do nothing for any script with a syntax error. PE runs the test regardless and
    // reports the failure in the output panel, which is also the more useful behaviour here.
    const inputCtrls = this.isUplink ? ['payload'] : ['msg', 'msgType'];
    const invalid = inputCtrls.filter((n) => this.form.get(n).invalid);
    if (invalid.length) {
      invalid.forEach((n) => this.form.get(n).markAsTouched());
      return;
    }
    const v = this.form.value;
    this.testing = true;
    this.result = null;
    if (this.isUplink) {
      // The decoder always receives a byte array; the content type only decides how the text in
      // the editor is turned into those bytes. JSON is normalised through a parse (so invalid JSON
      // is caught here rather than server-side), TEXT is sent verbatim, and BINARY is already the
      // Base64 the API expects.
      let payloadBase64: string;
      if (v.payloadContentType === ContentType.BINARY) {
        payloadBase64 = (v.payload || '').trim();
      } else if (v.payloadContentType === ContentType.TEXT) {
        payloadBase64 = btoa(unescape(encodeURIComponent(v.payload ?? '')));
      } else {
        const payloadJson = this.parseJson(v.payload);
        if (payloadJson === null) {
          this.applyResult({ output: '', error: 'Invalid JSON' });
          return;
        }
        payloadBase64 = btoa(unescape(encodeURIComponent(JSON.stringify(payloadJson))));
      }
      this.converterService.testUpLink({
        metadata: v.metadata || {},
        payload: payloadBase64,
        decoder: v.script
      }, this.data.scriptLang).subscribe({
        next: (res) => this.applyResult(res),
        error: () => { this.testing = false; }
      });
    } else {
      this.converterService.testDownLink({
        metadata: v.metadata || {},
        payload: '',
        msg: v.msg,
        msgType: v.msgType,
        integrationMetadata: v.integrationMetadata || {},
        encoder: v.script
      }, this.data.scriptLang).subscribe({
        next: (res) => this.applyResult(res),
        error: () => { this.testing = false; }
      });
    }
  }

  private applyResult(res: TestConverterResult): void {
    this.result = res;
    this.form.get('output').setValue(res.error || res.output);
    this.testing = false;
  }

  save(): void {
    this.dialogRef.close(this.form.get('script').value);
  }

  close(): void {
    this.dialogRef.close();
  }

}
