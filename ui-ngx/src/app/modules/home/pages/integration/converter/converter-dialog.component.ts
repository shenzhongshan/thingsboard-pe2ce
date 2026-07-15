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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { ConverterService } from '@core/http/converter.service';
import {
  Converter,
  ConverterScriptLanguage,
  ConverterType,
  DEFAULT_JS_DECODER,
  DEFAULT_JS_ENCODER,
  DEFAULT_TBEL_DECODER,
  DEFAULT_TBEL_ENCODER,
  newConverter
} from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { ConverterTestDialogComponent } from '@home/pages/integration/converter/converter-test-dialog.component';
import { EntityType } from '@shared/models/entity-type.models';

export interface ConverterDialogData {
  converterId?: string;
}

@Component({
  selector: 'tb-converter-dialog',
  templateUrl: './converter-dialog.component.html',
  styleUrls: ['./converter-dialog.component.scss'],
  standalone: false
})
export class ConverterDialogComponent implements OnInit {

  form: UntypedFormGroup;
  isEdit = false;
  loaded = false;

  EntityType = EntityType;
  ConverterType = ConverterType;
  ScriptLanguage = ScriptLanguage;
  ConverterScriptLanguage = ConverterScriptLanguage;
  integrationTypes = ['', ...Object.values(IntegrationType)];

  private converter: Converter;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ConverterDialogData,
              private dialogRef: MatDialogRef<ConverterDialogComponent, boolean>,
              protected store: Store<AppState>,
              private converterService: ConverterService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.converterId;
  }

  ngOnInit() {
    if (this.isEdit) {
      this.converterService.getConverter(this.data.converterId).subscribe((converter) => {
        this.converter = converter;
        this.buildForm(converter);
        this.loaded = true;
      });
    } else {
      this.converter = newConverter();
      this.buildForm(this.converter);
      this.loaded = true;
    }
  }

  private buildForm(converter: Converter) {
    const cfg = converter.configuration || {} as any;
    const isUplink = converter.type === ConverterType.UPLINK;
    this.form = this.fb.group({
      name: [converter.name, [Validators.required]],
      type: [converter.type],
      integrationType: [converter.integrationType || ''],
      debugSettings: [converter.debugSettings || { failuresEnabled: true, allEnabled: false, allEnabledUntil: 0 }],
      scriptLang: [cfg.scriptLang || ConverterScriptLanguage.TBEL],
      script: [isUplink ? (cfg.decoder || DEFAULT_TBEL_DECODER) : (cfg.encoder || DEFAULT_TBEL_ENCODER),
        [Validators.required]],
      updateOnlyKeys: [cfg.updateOnlyKeys || []],
      description: [converter.additionalInfo?.description || '']
    });
    // Reset the script template when switching type/lang on a fresh converter.
    if (!this.isEdit) {
      this.form.get('type').valueChanges.subscribe(() => this.resetScriptTemplate());
      this.form.get('scriptLang').valueChanges.subscribe(() => this.resetScriptTemplate());
    }
  }

  private resetScriptTemplate() {
    const isUplink = this.form.get('type').value === ConverterType.UPLINK;
    const isTbel = this.form.get('scriptLang').value === ConverterScriptLanguage.TBEL;
    let template: string;
    if (isUplink) {
      template = isTbel ? DEFAULT_TBEL_DECODER : DEFAULT_JS_DECODER;
    } else {
      template = isTbel ? DEFAULT_TBEL_ENCODER : DEFAULT_JS_ENCODER;
    }
    this.form.get('script').patchValue(template);
  }

  get isUplink(): boolean {
    return this.form?.get('type')?.value === ConverterType.UPLINK;
  }

  get scriptEditorLang(): ScriptLanguage {
    return this.form.get('scriptLang').value === ConverterScriptLanguage.JS ? ScriptLanguage.JS : ScriptLanguage.TBEL;
  }

  testFunction(): void {
    this.dialog.open(ConverterTestDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog'],
      data: {
        type: this.form.get('type').value,
        script: this.form.get('script').value,
        scriptLang: this.form.get('scriptLang').value
      }
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
    const configuration = this.isUplink
      ? { scriptLang: v.scriptLang, decoder: v.script, updateOnlyKeys: v.updateOnlyKeys || [] }
      : { scriptLang: v.scriptLang, encoder: v.script, updateOnlyKeys: v.updateOnlyKeys || [] };
    const converter: Converter = {
      ...(this.converter || {} as Converter),
      name: v.name,
      type: v.type,
      integrationType: v.integrationType || null,
      debugSettings: v.debugSettings,
      configuration,
      additionalInfo: { ...(this.converter?.additionalInfo || {}), description: v.description }
    };
    this.converterService.saveConverter(converter).subscribe(() => this.dialogRef.close(true));
  }

}
