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
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DialogService } from '@core/services/dialog.service';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { CustomTranslationRow, flattenTranslation, localeMeta, LOCALE_METAS } from '@shared/models/custom-translation.models';
import { displayedLangs } from '@shared/models/constants';
import {
  CustomTranslationAddDialogComponent,
  CustomTranslationAddDialogData
} from '@home/pages/admin/custom-translation-add-dialog.component';

@Component({
  selector: 'tb-custom-translation-list',
  templateUrl: './custom-translation-list.component.html',
  styleUrls: ['./custom-translation-list.component.scss', './settings-card.scss'],
  standalone: false
})
export class CustomTranslationListComponent extends PageComponent implements OnInit {

  rows: CustomTranslationRow[] = [];
  filteredRows: CustomTranslationRow[] = [];
  searchMode = false;
  textSearch = '';

  private enUsKeyCount = 0;

  constructor(protected store: Store<AppState>,
              private customTranslationService: CustomTranslationService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private http: HttpClient,
              private dialog: MatDialog,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
    this.reload();
  }

  reload() {
    forkJoin({
      customized: this.customTranslationService.getCustomizedLocales(),
      enUs: this.http.get<any>('/assets/locale/locale.constant-en_US.json').pipe(catchError(() => of({})))
    }).subscribe(({customized, enUs}) => {
      this.enUsKeyCount = Object.keys(flattenTranslation(enUs)).length || 1;
      const customizedSet = new Set(customized);
      // Only offer the languages surfaced elsewhere in the UI (displayedLangs). Locales
      // customized at this level but not part of that list (e.g. added via 添加新语言 with an
      // arbitrary code) have no base file to show a progress bar for.
      const visibleMetas = LOCALE_METAS.filter(m => displayedLangs.includes(m.code));
      const extraCodes = customized.filter(code => !visibleMetas.some(m => m.code === code));
      const allMetas = [...visibleMetas, ...extraCodes.map(localeMeta)];
      forkJoin(allMetas.map(meta =>
        this.http.get<any>(`/assets/locale/locale.constant-${meta.code}.json`).pipe(catchError(() => of(null)))
      )).subscribe((localeFiles) => {
        this.rows = allMetas.map((meta, i) => ({
          ...meta,
          progress: this.computeProgress(localeFiles[i]),
          customized: customizedSet.has(meta.code)
        }));
        this.applySearch();
      });
    });
  }

  private computeProgress(localeJson: any): number {
    if (!localeJson) {
      return 0;
    }
    const translatedCount = Object.keys(flattenTranslation(localeJson)).length;
    return Math.min(100, Math.round((translatedCount / this.enUsKeyCount) * 100));
  }

  enterSearchMode() {
    this.searchMode = true;
  }

  exitSearchMode() {
    this.searchMode = false;
    this.textSearch = '';
    this.applySearch();
  }

  applySearch() {
    const search = (this.textSearch || '').toLowerCase();
    this.filteredRows = !search ? this.rows : this.rows.filter(r =>
      r.code.toLowerCase().includes(search) || r.name.toLowerCase().includes(search) || r.country.toLowerCase().includes(search));
  }

  addLanguage() {
    this.dialog.open<CustomTranslationAddDialogComponent, CustomTranslationAddDialogData, string>(
      CustomTranslationAddDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data: {existingCodes: this.rows.filter(r => r.customized).map(r => r.code)}
      }).afterClosed().subscribe((localeCode) => {
      if (localeCode) {
        this.router.navigate(['/white-labeling/custom-translation', localeCode]);
      }
    });
  }

  openEditor($event: Event, row: CustomTranslationRow) {
    $event?.stopPropagation();
    this.router.navigate(['/white-labeling/custom-translation', row.code]);
  }

  downloadTranslation($event: Event, row: CustomTranslationRow) {
    $event?.stopPropagation();
    this.customTranslationService.getCustomTranslation(row.code).subscribe((value) => {
      const blob = new Blob([JSON.stringify(value || {}, null, 2)], {type: 'application/json'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `custom-translation-${row.code}.json`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  deleteTranslation($event: Event, row: CustomTranslationRow) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('custom-translation.delete-title', {locale: row.name}),
      this.translate.instant('custom-translation.delete-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.customTranslationService.deleteCustomTranslation(row.code).subscribe(() => this.reload());
      }
    });
  }

}
