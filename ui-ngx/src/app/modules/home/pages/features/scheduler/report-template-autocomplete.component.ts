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

import { Component, ElementRef, Input, OnInit, ViewChild, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, switchMap, tap } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ReportTemplateService } from '@core/http/report-template.service';
import { ReportTemplateInfo, ReportTemplateType } from '@shared/models/report.models';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';

/**
 * Autocomplete over report templates. PE parity: `tb-report-template-autocomplete` — a search input,
 * not the `<mat-select>` CE previously used, matching PE's Generate Report form.
 */
@Component({
  selector: 'tb-report-template-autocomplete',
  templateUrl: './report-template-autocomplete.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ReportTemplateAutocompleteComponent),
      multi: true
    }
  ],
  standalone: false
})
export class ReportTemplateAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectTemplateFormGroup: UntypedFormGroup;

  modelValue: string | null = null;

  @Input()
  type: ReportTemplateType = ReportTemplateType.REPORT;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @ViewChild('templateInput', {static: true}) templateInput: ElementRef<HTMLInputElement>;

  filteredTemplates: Observable<Array<ReportTemplateInfo>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_v: any) => { };

  constructor(private reportTemplateService: ReportTemplateService,
              private fb: UntypedFormBuilder) {
    this.selectTemplateFormGroup = this.fb.group({
      template: [null]
    });
  }

  ngOnInit() {
    this.filteredTemplates = this.selectTemplateFormGroup.get('template').valueChanges.pipe(
      debounceTime(150),
      tap((value: ReportTemplateInfo | string) => {
        const modelValue = (typeof value === 'string' || !value) ? null : value.id.id;
        this.updateView(modelValue);
        if (value === null) {
          this.clear();
        }
      }),
      map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
      switchMap(name => this.fetchTemplates(name))
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.selectTemplateFormGroup.disable({emitEvent: false});
    } else {
      this.selectTemplateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value ?? null;
    if (!value) {
      this.selectTemplateFormGroup.get('template').patchValue('', {emitEvent: false});
      this.dirty = true;
      return;
    }
    this.reportTemplateService.getReportTemplate(value, {ignoreLoading: true, ignoreErrors: true}).subscribe({
      next: (template) => this.selectTemplateFormGroup.get('template').patchValue(template, {emitEvent: false}),
      error: () => this.selectTemplateFormGroup.get('template').patchValue('', {emitEvent: false})
    });
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectTemplateFormGroup.get('template').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear() {
    this.selectTemplateFormGroup.get('template').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.templateInput.nativeElement.blur();
      this.templateInput.nativeElement.focus();
    }, 0);
  }

  displayTemplateFn(template?: ReportTemplateInfo): string | undefined {
    return template ? template.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return !!(text && text.length > 0);
  }

  private updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private fetchTemplates(searchText?: string): Observable<Array<ReportTemplateInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {property: 'name', direction: Direction.ASC});
    return this.reportTemplateService.getReportTemplateInfos(pageLink, null, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<ReportTemplateInfo>())),
      map(pageData => pageData.data.filter(t => t.type === this.type))
    );
  }
}
