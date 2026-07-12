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
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import {
  deleteAtPath,
  flattenTranslation,
  localeMeta,
  LocaleMeta,
  setAtPath,
  TranslationRow,
  TranslationRowFilter
} from '@shared/models/custom-translation.models';

@Component({
  selector: 'tb-custom-translation',
  templateUrl: './custom-translation.component.html',
  styleUrls: ['./custom-translation.component.scss'],
  standalone: false
})
export class CustomTranslationComponent extends PageComponent implements OnInit {

  localeCode: string;
  meta: LocaleMeta;
  mode: 'basic' | 'advanced' = 'basic';
  fullscreen = false;

  advancedForm: UntypedFormGroup;

  allRows: TranslationRow[] = [];
  filteredRows: TranslationRow[] = [];
  textSearch = '';
  activeFilter: TranslationRowFilter = null;

  pageSize = 50;
  page = 0;

  isDirty = false;

  private baseEn: {[key: string]: string} = {};
  private baseTarget: {[key: string]: string} = {};
  private workingValue: any = {};
  private originalSnapshot = '{}';

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private customTranslationService: CustomTranslationService,
              private http: HttpClient,
              private dialogService: DialogService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
    this.advancedForm = this.fb.group({
      value: ['{}']
    });
  }

  ngOnInit() {
    this.localeCode = this.route.snapshot.params.localeCode;
    this.meta = localeMeta(this.localeCode);
    this.load();
    this.advancedForm.get('value').valueChanges.subscribe(() => {
      if (this.mode === 'advanced') {
        this.onAdvancedChange();
      }
    });
  }

  private load() {
    forkJoin({
      en: this.localeCode === 'en_US' ? of({}) :
        this.http.get<any>('/assets/locale/locale.constant-en_US.json').pipe(catchError(() => of({}))),
      target: this.http.get<any>(`/assets/locale/locale.constant-${this.localeCode}.json`).pipe(catchError(() => of({}))),
      own: this.customTranslationService.getCustomTranslation(this.localeCode).pipe(catchError(() => of({})))
    }).subscribe(({en, target, own}) => {
      this.baseEn = flattenTranslation(en);
      this.baseTarget = flattenTranslation(target || {});
      this.workingValue = own ? JSON.parse(JSON.stringify(own)) : {};
      this.originalSnapshot = JSON.stringify(this.workingValue);
      this.syncAdvancedForm();
      this.buildRows();
    });
  }

  private syncAdvancedForm() {
    this.advancedForm.get('value').setValue(JSON.stringify(this.workingValue, null, 2), {emitEvent: false});
  }

  private buildRows() {
    const override = flattenTranslation(this.workingValue);
    const keys = new Set([...Object.keys(this.baseEn), ...Object.keys(override)]);
    this.allRows = Array.from(keys).sort().map(key => ({
      key,
      english: this.baseEn[key] ?? '',
      value: override[key] ?? this.baseTarget[key] ?? '',
      translated: !!this.baseTarget[key],
      customized: Object.prototype.hasOwnProperty.call(override, key),
      added: !Object.prototype.hasOwnProperty.call(this.baseEn, key)
    }));
    this.applyFilter();
  }

  switchMode(mode: 'basic' | 'advanced') {
    if (mode === this.mode) {
      return;
    }
    if (this.mode === 'advanced') {
      if (!this.applyAdvancedJson()) {
        return;
      }
    }
    this.mode = mode;
    if (mode === 'basic') {
      this.buildRows();
    }
  }

  private applyAdvancedJson(): boolean {
    try {
      this.workingValue = JSON.parse(this.advancedForm.get('value').value || '{}');
      return true;
    } catch (e) {
      this.dialogService.alert(
        this.translate.instant('custom-translation.invalid-json-title'),
        this.translate.instant('custom-translation.invalid-json-text')
      );
      return false;
    }
  }

  toggleFilter(filter: TranslationRowFilter) {
    this.activeFilter = this.activeFilter === filter ? null : filter;
    this.applyFilter();
  }

  applyFilter() {
    const search = (this.textSearch || '').toLowerCase();
    this.filteredRows = this.allRows.filter(row => {
      if (this.activeFilter && !row[this.activeFilter]) {
        return false;
      }
      if (search && !row.key.toLowerCase().includes(search) && !row.english.toLowerCase().includes(search)
        && !row.value.toLowerCase().includes(search)) {
        return false;
      }
      return true;
    });
    this.page = 0;
  }

  updateRowValue(row: TranslationRow, value: string) {
    row.value = value;
    row.customized = true;
    setAtPath(this.workingValue, row.key, value);
    this.markDirty();
  }

  revertRow(row: TranslationRow) {
    deleteAtPath(this.workingValue, row.key);
    row.value = this.baseTarget[row.key] ?? '';
    row.customized = false;
    if (row.added) {
      this.allRows = this.allRows.filter(r => r.key !== row.key);
      this.applyFilter();
    }
    this.markDirty();
  }

  addKey() {
    const key = prompt(this.translate.instant('custom-translation.new-key-prompt'));
    if (!key) {
      return;
    }
    if (this.allRows.some(r => r.key === key)) {
      this.dialogService.alert(
        this.translate.instant('custom-translation.duplicate-key-title'),
        this.translate.instant('custom-translation.duplicate-key-text')
      );
      return;
    }
    setAtPath(this.workingValue, key, '');
    this.allRows.push({key, english: '', value: '', translated: false, customized: true, added: true});
    this.allRows.sort((a, b) => a.key.localeCompare(b.key));
    this.applyFilter();
    this.markDirty();
  }

  private markDirty() {
    this.isDirty = JSON.stringify(this.workingValue) !== this.originalSnapshot;
    this.syncAdvancedForm();
  }

  onAdvancedChange() {
    // Actual JSON validity is only enforced at save/mode-switch time; while typing, just flag dirty.
    this.isDirty = true;
  }

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen;
  }

  cancel() {
    this.load();
    this.isDirty = false;
  }

  save() {
    if (this.mode === 'advanced' && !this.applyAdvancedJson()) {
      return;
    }
    this.customTranslationService.saveCustomTranslation(this.localeCode, this.workingValue).subscribe(() => {
      this.originalSnapshot = JSON.stringify(this.workingValue);
      this.isDirty = false;
    });
  }

  goBack() {
    this.router.navigate(['/white-labeling/custom-translation']);
  }

}
