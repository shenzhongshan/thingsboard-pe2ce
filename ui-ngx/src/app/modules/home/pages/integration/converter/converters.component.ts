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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ConverterService } from '@core/http/converter.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Converter, converterTypeTranslations } from '@shared/models/converter.models';
import {
  ConverterDialogComponent,
  ConverterDialogData
} from '@home/pages/integration/converter/converter-dialog.component';
import { ImportDialogComponent, ImportDialogData } from '@shared/import-export/import-dialog.component';

@Component({
  selector: 'tb-converters',
  templateUrl: './converters.component.html',
  styleUrls: ['./converters.component.scss'],
  standalone: false
})
export class ConvertersComponent extends PageComponent implements OnInit {

  converters: Converter[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  converterTypeTranslations = converterTypeTranslations;

  constructor(protected store: Store<AppState>,
              private converterService: ConverterService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit() {
    this.reload();
  }

  reload() {
    const pageLink = new PageLink(this.pageSize, this.page, this.textSearch,
      { property: 'createdTime', direction: Direction.DESC });
    this.converterService.getConverters(pageLink).subscribe((pageData) => {
      this.converters = pageData.data;
      this.totalElements = pageData.totalElements;
    });
  }

  pageChanged(event: {pageIndex: number; pageSize: number}) {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload();
  }

  enterSearchMode() {
    this.searchMode = true;
  }

  exitSearchMode() {
    this.searchMode = false;
    if (this.textSearch) {
      this.textSearch = null;
      this.reload();
    }
  }

  searchChanged() {
    this.page = 0;
    this.reload();
  }

  addConverter() {
    this.openDialog(null);
  }

  importConverter() {
    this.dialog.open<ImportDialogComponent, ImportDialogData, any>(ImportDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        importTitle: 'converter.import-converter',
        importFileLabel: 'converter.converter-file'
      }
    }).afterClosed().subscribe((imported) => {
      if (imported) {
        const converter: Converter = {
          name: imported.name,
          type: imported.type,
          integrationType: imported.integrationType,
          debugSettings: imported.debugSettings || { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 },
          configuration: imported.configuration || {},
          additionalInfo: imported.additionalInfo || {}
        } as Converter;
        this.converterService.saveConverter(converter).subscribe(() => this.reload());
      }
    });
  }

  editConverter($event: Event, converter: Converter) {
    $event?.stopPropagation();
    this.openDialog(converter.id.id);
  }

  private openDialog(converterId: string) {
    this.dialog.open<ConverterDialogComponent, ConverterDialogData, boolean>(
      ConverterDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { converterId }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  exportConverter($event: Event, converter: Converter) {
    $event?.stopPropagation();
    this.converterService.getConverter(converter.id.id).subscribe((full) => {
      const exportData = {
        name: full.name,
        type: full.type,
        integrationType: full.integrationType,
        debugSettings: full.debugSettings,
        configuration: full.configuration,
        additionalInfo: full.additionalInfo
      };
      const blob = new Blob([JSON.stringify(exportData, null, 2)], {type: 'application/json'});
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${full.name}.json`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  deleteConverter($event: Event, converter: Converter) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('converter.delete-converter-title', {converterName: converter.name}),
      this.translate.instant('converter.delete-converter-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.converterService.deleteConverter(converter.id.id).subscribe(() => this.reload());
      }
    });
  }

}
