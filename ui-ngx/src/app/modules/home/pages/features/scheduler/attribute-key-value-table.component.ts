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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { coerceBoolean } from '@shared/decorators/coercion';

/**
 * Rejects an attribute map that is empty (when `required`) or that has a blank key / null value.
 * PE parity: `attributeKeyValueValidator`.
 */
export function attributeKeyValueValidator(required: boolean): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: {[key: string]: any} = control.value;
    const keys = value ? Object.keys(value) : [];
    const missing = required && !keys.length;
    const invalid = keys.some(key => !key || !key.length || value[key] === null);
    if (!missing && !invalid) {
      return null;
    }
    const errors: ValidationErrors = {};
    if (missing) {
      errors.required = true;
    }
    if (invalid) {
      errors.invalid = true;
    }
    return errors;
  };
}

/**
 * Key/typed-value table backing one attribute scope of a scheduler event. PE parity:
 * `tb-attribute-key-value-table` — the value column is a `tb-value-input`, so a value keeps its JSON
 * type (string / number / boolean / object) instead of being guessed from its text at save time.
 */
@Component({
  selector: 'tb-attribute-key-value-table',
  templateUrl: './attribute-key-value-table.component.html',
  styleUrls: ['./attribute-key-value-table.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AttributeKeyValueTableComponent),
      multi: true
    }
  ],
  standalone: false
})
export class AttributeKeyValueTableComponent extends PageComponent implements ControlValueAccessor, OnInit {

  kvListFormGroup: UntypedFormGroup;

  @Input()
  disabled: boolean;

  @Input()
  titleText: string;

  @Input()
  requiredPrompt: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  private propagateChange: (value: any) => void = () => { };

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit() {
    this.kvListFormGroup = this.fb.group({
      keyVals: this.fb.array([])
    });
    this.kvListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  keyValsFormArray(): UntypedFormArray {
    return this.kvListFormGroup.get('keyVals') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.kvListFormGroup.disable({emitEvent: false});
    } else {
      this.kvListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: {[key: string]: any} | null): void {
    const rows: UntypedFormGroup[] = [];
    if (value) {
      for (const key of Object.keys(value)) {
        rows.push(this.fb.group({
          key: [key, [Validators.required]],
          value: [value[key], [Validators.required]]
        }));
      }
    }
    this.kvListFormGroup.setControl('keyVals', this.fb.array(rows), {emitEvent: false});
  }

  addKeyVal() {
    this.keyValsFormArray().push(this.fb.group({
      key: ['', [Validators.required]],
      value: ['', [Validators.required]]
    }));
  }

  removeKeyVal(index: number) {
    this.keyValsFormArray().removeAt(index);
  }

  private updateModel() {
    const kvList: {key: string; value: any}[] = this.keyValsFormArray().value;
    const value: {[key: string]: any} = {};
    kvList.forEach(kv => value[kv.key.trim()] = kv.value);
    this.propagateChange(value);
  }
}
