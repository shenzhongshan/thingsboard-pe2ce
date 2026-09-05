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
import { TranslateService } from '@ngx-translate/core';
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
import { IntegrationType, integrationTypeInfoMap } from '@shared/models/integration.models';
import {
  ConverterTestDialogComponent,
  ConverterTestDialogData
} from '@home/pages/integration/converter/converter-test-dialog.component';
import { EntityType } from '@shared/models/entity-type.models';

export interface ConverterDialogData {
  converterId?: string;
  /** Preset + lock the direction when opened from an integration's converter field (PE does the
   *  same: the type toggle and integration type are fixed to the slot you launched it from). */
  presetType?: ConverterType;
  presetIntegrationType?: IntegrationType;
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
  // PE renders this field with the same rich icon+description autocomplete the integration wizard
  // uses (its shared tb-integration-type-select), with an empty value meaning "all types" — shown
  // via the 全部 placeholder rather than an explicit row in the list.
  integrationTypes = Object.values(IntegrationType);
  integrationTypeInfoMap = integrationTypeInfoMap;

  private converter: Converter;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ConverterDialogData,
              private dialogRef: MatDialogRef<ConverterDialogComponent, Converter | boolean>,
              protected store: Store<AppState>,
              private converterService: ConverterService,
              private translate: TranslateService,
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
      if (this.data.presetType) {
        this.converter.type = this.data.presetType;
      }
      if (this.data.presetIntegrationType) {
        this.converter.integrationType = this.data.presetIntegrationType;
      }
      this.buildForm(this.converter);
      if (this.presetsLocked) {
        // PE renders these two as fixed (disabled) when launched from an integration's slot.
        this.form.get('type').disable({ emitEvent: false });
        this.form.get('integrationType').disable({ emitEvent: false });
      }
      this.loaded = true;
    }
  }

  /** True when opened from an integration's converter field, which fixes direction + integration type. */
  get presetsLocked(): boolean {
    return !this.isEdit && !!this.data.presetType;
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

  /**
   * PE ships two variants of each help doc and swaps them with the script language: the TBEL one
   * links to the TBEL guide and its examples omit the helper functions TBEL provides natively,
   * while the JS one spells those helpers out. Mirrors PE's own `converter/` vs `converter/tbel/`
   * asset paths.
   */
  get scriptHelpId(): string {
    const tbel = this.form?.get('scriptLang')?.getRawValue() !== ConverterScriptLanguage.JS;
    const base = tbel ? 'converter/tbel/' : 'converter/';
    return base + (this.isUplink ? 'decoder_fn' : 'encoder_fn');
  }

  /** Shows the translated name for a picked type; empty stays empty so the 全部 placeholder shows. */
  displayIntegrationTypeFn = (type: IntegrationType): string => {
    if (!type) {
      return '';
    }
    const info = this.integrationTypeInfoMap.get(type);
    return info ? this.translate.instant(info.name) : '';
  };

  get filteredIntegrationTypes(): IntegrationType[] {
    const raw = this.form?.get('integrationType')?.value;
    if (!raw || this.integrationTypes.includes(raw)) {
      return this.integrationTypes;
    }
    const query = String(raw).toLowerCase();
    return this.integrationTypes.filter((type) =>
      this.translate.instant(this.integrationTypeInfoMap.get(type).name).toLowerCase().includes(query));
  }

  clearIntegrationType(event: Event) {
    event.stopPropagation();
    this.form.get('integrationType').setValue('');
  }

  get isUplink(): boolean {
    // getRawValue-equivalent: `type` is disabled when preset-locked, so read the control directly.
    return this.form?.get('type')?.getRawValue() === ConverterType.UPLINK;
  }

  get scriptEditorLang(): ScriptLanguage {
    return this.form.get('scriptLang').value === ConverterScriptLanguage.JS ? ScriptLanguage.JS : ScriptLanguage.TBEL;
  }

  testFunction(): void {
    this.dialog.open<ConverterTestDialogComponent, ConverterTestDialogData, string>(ConverterTestDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
      data: {
        type: this.form.get('type').value,
        script: this.form.get('script').value,
        scriptLang: this.form.get('scriptLang').value
      }
    }).afterClosed().subscribe((savedScript) => {
      if (savedScript !== undefined) {
        this.form.get('script').setValue(savedScript);
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
    // getRawValue(), not value: type/integrationType are disabled when preset-locked, and
    // disabled controls are omitted from .value.
    const v = this.form.getRawValue();
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
    // Return the saved entity so an integration form that launched this dialog can bind the new
    // converter straight into its own field; existing callers just check truthiness.
    this.converterService.saveConverter(converter).subscribe((saved) => this.dialogRef.close(saved));
  }

}
