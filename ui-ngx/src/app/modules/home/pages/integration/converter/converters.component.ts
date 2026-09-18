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

import { Component, OnInit, Renderer2, ViewChild, ViewContainerRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { MatDrawer } from '@angular/material/sidenav';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ConverterService } from '@core/http/converter.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Converter, converterTypeTranslations } from '@shared/models/converter.models';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import {
  ConverterDialogComponent,
  ConverterDialogData
} from '@home/pages/integration/converter/converter-dialog.component';
import { IntegrationDetailsPanelTarget } from '@home/pages/integration/integration-details-panel.component';
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

  /** 由路由 data.isEdgeTemplate 决定这个页面看的是 edge 模板还是普通实体 —— 同一套组件
   *  服务 /integrations-center 和 /edgeManagement 两条路由，与 PE 一致。 */
  isEdgeTemplate = false;


  @ViewChild('detailsDrawer') detailsDrawer: MatDrawer;
  detailsTarget: IntegrationDetailsPanelTarget = null;
  detailsFocusTab: 'overview' | 'events' = 'overview';

  constructor(protected store: Store<AppState>,
              private converterService: ConverterService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private viewContainerRef: ViewContainerRef,
              private renderer: Renderer2,
              private dialog: MatDialog,
              private route: ActivatedRoute) {
    super(store);
  }

  ngOnInit() {
    this.isEdgeTemplate = !!this.route.snapshot.data.isEdgeTemplate;
    this.reload();
  }

  reload() {
    const pageLink = new PageLink(this.pageSize, this.page, this.textSearch,
      { property: 'createdTime', direction: Direction.DESC });
    this.converterService.getConverters(pageLink, undefined, this.isEdgeTemplate).subscribe((pageData) => {
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

  isDebugActive(converter: Converter): boolean {
    const settings = converter.debugSettings;
    return this.entityDebugSettingsService.isDebugActive(settings?.allEnabledUntil) || !!settings?.failuresEnabled;
  }

  debugTooltip(converter: Converter): string {
    return this.entityDebugSettingsService.getDebugConfigLabel(converter.debugSettings);
  }

  openDebugConfig($event: MouseEvent, converter: Converter) {
    $event?.stopPropagation();
    this.entityDebugSettingsService.viewContainerRef = this.viewContainerRef;
    this.entityDebugSettingsService.renderer = this.renderer;
    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings: converter.debugSettings || {},
      debugConfig: {
        entityType: EntityType.CONVERTER,
        additionalActionConfig: {
          title: this.translate.instant('action.see-debug-events'),
          action: () => this.openDebugEvents(converter)
        }
      },
      onSettingsAppliedFn: (settings: EntityDebugSettings) => this.saveDebugSettings(converter, settings)
    }, $event.target as Element);
  }

  private saveDebugSettings(converter: Converter, debugSettings: EntityDebugSettings) {
    // Re-read first: the list row omits `configuration`, so posting the row itself would wipe the
    // converter's script.
    this.converterService.getConverter(converter.id.id).pipe(
      switchMap((full: Converter) => this.converterService.saveConverter({...full, debugSettings})),
      catchError(() => of(null))
    ).subscribe(() => this.reload());
  }

  openDebugEvents(converter: Converter) {
    this.openDetails(converter.id.id, 'events');
  }

  /** Row click opens the read-oriented side panel; see the matching comment on IntegrationsComponent. */
  openConverterDetails($event: Event, converter: Converter) {
    $event?.stopPropagation();
    this.openDetails(converter.id.id, 'overview');
  }

  private openDetails(converterId: string, focusTab: 'overview' | 'events') {
    this.converterService.getConverter(converterId).subscribe((full) => {
      this.detailsTarget = { kind: 'converter', entity: full };
      this.detailsFocusTab = focusTab;
      this.detailsDrawer.open();
    });
  }

  closeDetails() {
    this.detailsDrawer.close();
  }

  onDetailsEdit() {
    const id = this.detailsTarget?.entity?.id?.id;
    this.detailsDrawer.close();
    if (id) {
      this.openDialog(id);
    }
  }

  onDetailsDelete() {
    const converter = this.detailsTarget?.entity as Converter;
    this.detailsDrawer.close();
    if (converter) {
      this.deleteConverter(null, converter);
    }
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
          // 从模板页导入的落在模板命名空间，否则导进来就看不见了。
          edgeTemplate: this.isEdgeTemplate,
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
        data: { converterId, isEdgeTemplate: this.isEdgeTemplate }
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
