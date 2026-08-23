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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ReportTemplateService } from '@core/http/report-template.service';
import { PageLink } from '@shared/models/page/page-link';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import {
  addableReportComponentTypes,
  buildDataSource,
  dataKeysToKeys,
  DataSource,
  DEFAULT_REPORT_NAME_PATTERN,
  REPORT_NAME_DATE_TOKEN,
  EntityAlias,
  Insets,
  isLatestSnapshotComponent,
  keysToDataKeys,
  pageMarginsToNumber,
  ReportComponentConfig,
  ReportComponentType,
  reportComponentTypeIcons,
  reportComponentTypeTranslations,
  ReportTemplate,
  ReportTemplateInfo,
  ReportTemplateType,
  resolveDataSourceEntity,
  TbReportFormat,
  usesTimeseriesKeys
} from '@shared/models/report.models';

export interface ReportTemplateEditorData {
  reportTemplateId: string;
}

// One day in ms - the only unit lastMs exposed pre-cutover; kept identical so existing habits carry
// over even though ALARM_TABLE's window is now a structured object instead of a bare number.
const DEFAULT_ALARM_WINDOW_MS = 86400000;

const DEFAULT_PAGE_MARGIN = 20;

@Component({
  selector: 'tb-report-template-editor',
  templateUrl: './report-template-editor.component.html',
  styleUrls: ['./report-template-editor.component.scss'],
  standalone: false
})
export class ReportTemplateEditorComponent implements OnInit {

  form: UntypedFormGroup;
  loaded = false;

  ReportComponentType = ReportComponentType;
  TbReportFormat = TbReportFormat;
  ReportTemplateType = ReportTemplateType;
  EntityType = EntityType;
  componentTypes = addableReportComponentTypes;
  componentTypeTranslations = reportComponentTypeTranslations;
  componentTypeIcons = reportComponentTypeIcons;
  dataDrivenTypes = [
    ReportComponentType.ENTITY_TABLE,
    ReportComponentType.TIME_SERIES_TABLE,
    ReportComponentType.ALARM_TABLE,
    ReportComponentType.TIME_SERIES_CHART,
    ReportComponentType.LATEST_CHART
  ];
  pageSizes = ['A4', 'A3', 'LETTER'];
  // Interpolated into the name-pattern hint rather than written into the translated string,
  // whose braces the ICU messageformat compiler would reject.
  nameDateToken = REPORT_NAME_DATE_TOKEN;

  otherTemplates: ReportTemplateInfo[] = [];

  private template: ReportTemplate;
  // Aliases carried over untouched from the loaded template, plus any this editing session
  // creates. Keyed by the same id buildComponentRow hands out per row (see aliasIdForRow), so
  // saving twice reuses the row's existing alias instead of accumulating new ones.
  private entityAliases: EntityAlias[] = [];
  // Kept so asymmetric margins, which the single margin field cannot express, survive a save that
  // never touched them. See resolvePageMargins.
  private originalPageMargins: number | Insets;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ReportTemplateEditorData,
              private dialogRef: MatDialogRef<ReportTemplateEditorComponent, boolean>,
              private reportTemplateService: ReportTemplateService,
              private fb: UntypedFormBuilder) {
  }

  ngOnInit() {
    this.reportTemplateService.getReportTemplate(this.data.reportTemplateId).subscribe((template) => {
      this.template = template;
      this.entityAliases = [...(template.configuration?.entityAliases || [])];
      this.buildForm(template);
      this.loaded = true;
      if ((template.configuration?.components || []).some((c) => c.type === ReportComponentType.SUB_REPORT)) {
        this.loadOtherTemplates();
      }
    });
  }

  private buildForm(template: ReportTemplate) {
    const cfg = template.configuration || { format: TbReportFormat.PDF, components: [] };
    this.originalPageMargins = cfg.pageMargins;
    this.form = this.fb.group({
      name: [template.name, [Validators.required]],
      description: [template.description || ''],
      namePattern: [cfg.namePattern || DEFAULT_REPORT_NAME_PATTERN],
      pageSize: [cfg.pageSize || 'A4'],
      pageOrientation: [(cfg.pageOrientation || 'PORTRAIT').toLowerCase()],
      // null (blank field) when the template carries asymmetric margins - see resolvePageMargins.
      pageMargin: [cfg.pageMargins != null ? pageMarginsToNumber(cfg.pageMargins) : DEFAULT_PAGE_MARGIN],
      header: [this.headerFooterToText(cfg.header)],
      footer: [this.headerFooterToText(cfg.footer)],
      components: this.fb.array((cfg.components || [])
        // ERROR is PE's internal render-failure placeholder, never authored - and DASHBOARD needs
        // PE's screenshot service, which this port doesn't have (see report.models.ts). Neither
        // has an edit UI; dropping them on load means re-saving an existing template that has one
        // silently removes it, which is preferable to editing it into a broken shape.
        .filter((c) => c.type !== ReportComponentType.ERROR && c.type !== ReportComponentType.DASHBOARD)
        .map((c, i) => this.buildComponentRow(c, `row-${i}`)))
    });
  }

  private buildComponentRow(c: ReportComponentConfig, rowId: string): UntypedFormGroup {
    const ds: DataSource = c.type === ReportComponentType.ALARM_TABLE ? c.alarmSource : c.dataSources?.[0];
    const keys = c.type === ReportComponentType.ALARM_TABLE ? ds?.dataKeys : (ds?.dataKeys || ds?.latestDataKeys);
    return this.fb.group({
      rowId: [rowId],
      type: [c.type],
      title: [c.title || ''],
      // HEADING / RICH_TEXT share the same underlying PE field ("value"); kept as two form
      // controls only so the two pieces of UI (a single-line heading vs. a textarea) stay simple.
      text: [c.type === ReportComponentType.HEADING ? (c.value || '') : ''],
      level: [1],
      html: [c.type === ReportComponentType.RICH_TEXT ? (c.value || '') : ''],
      url: [c.imageUrl || ''],
      entityId: [resolveDataSourceEntity(ds, this.entityAliases)],
      keys: [dataKeysToKeys(keys)],
      lastMs: [c.timewindow?.history?.interval ?? DEFAULT_ALARM_WINDOW_MS],
      chartType: [c.chartType || 'BAR'],
      reportTemplateId: [c.templateId?.id || null]
    });
  }

  get componentsArray(): UntypedFormArray {
    return this.form.get('components') as UntypedFormArray;
  }

  addComponent(type: ReportComponentType) {
    this.componentsArray.push(this.buildComponentRow({ type } as ReportComponentConfig, this.newRowId()));
    if (type === ReportComponentType.SUB_REPORT) {
      this.loadOtherTemplates();
    }
  }

  removeComponent(i: number) {
    this.componentsArray.removeAt(i);
  }

  moveComponent(i: number, delta: number) {
    const arr = this.componentsArray;
    const target = i + delta;
    if (target < 0 || target >= arr.length) {
      return;
    }
    const row = arr.at(i);
    arr.removeAt(i);
    arr.insert(target, row);
  }

  isDataDriven(type: ReportComponentType): boolean {
    return this.dataDrivenTypes.includes(type);
  }

  private newRowId(): string {
    return 'row-' + Math.random().toString(36).slice(2);
  }

  private loadOtherTemplates() {
    if (this.otherTemplates.length) {
      return;
    }
    this.reportTemplateService.getReportTemplateInfos(new PageLink(100)).subscribe((page) => {
      this.otherTemplates = page.data.filter((t) => t.id.id !== this.data.reportTemplateId);
    });
  }

  typeName(type: ReportComponentType): string {
    return this.componentTypeTranslations.get(type);
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
    // toComponentConfig populates aliases into this.entityAliases as a side effect (via
    // buildDataSource's ensureAlias callback) - it must run for every row before the array is
    // read below.
    this.entityAliases = [];
    const components: ReportComponentConfig[] = (v.components as any[]).map((row) => this.toComponentConfig(row));
    const updated: ReportTemplate = {
      ...this.template,
      name: v.name,
      description: v.description,
      configuration: {
        format: this.template.format || TbReportFormat.PDF,
        namePattern: v.namePattern,
        pageSize: v.pageSize,
        pageOrientation: (v.pageOrientation || 'portrait').toUpperCase() as 'PORTRAIT' | 'LANDSCAPE',
        pageMargins: this.resolvePageMargins(v.pageMargin),
        header: this.textToHeaderFooter(v.header),
        footer: this.textToHeaderFooter(v.footer),
        entityAliases: this.entityAliases.length ? this.entityAliases : undefined,
        components
      }
    };
    this.reportTemplateService.saveReportTemplate(updated).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

  private toComponentConfig(row: any): ReportComponentConfig {
    const c: ReportComponentConfig = { type: row.type };
    const ensureAlias = (id: string, target: EntityId) => {
      const existing = this.entityAliases.find((a) => a.id === id);
      const alias: EntityAlias = {
        id, alias: id,
        filter: { type: 'singleEntity', singleEntity: { entityType: target.entityType, id: target.id } }
      };
      if (existing) {
        Object.assign(existing, alias);
      } else {
        this.entityAliases.push(alias);
      }
    };
    switch (row.type) {
      case ReportComponentType.HEADING:
        c.value = row.text;
        break;
      case ReportComponentType.RICH_TEXT:
        c.value = row.html;
        break;
      case ReportComponentType.IMAGE:
        c.sourceType = 'image';
        c.imageUrl = row.url;
        break;
      case ReportComponentType.DIVIDER:
      case ReportComponentType.PAGE_BREAK:
        break;
      case ReportComponentType.SUB_REPORT:
        c.templateId = row.reportTemplateId ? { entityType: 'REPORT_TEMPLATE', id: row.reportTemplateId } : undefined;
        break;
      case ReportComponentType.ALARM_TABLE: {
        c.title = row.title;
        const ds = buildDataSource(row.entityId, row.rowId, ensureAlias);
        // 'entityField' matches the shape verified against the running backend for alarm tables;
        // unlike the other data-driven types, this has not been confirmed with 'timeseries' keys.
        ds.dataKeys = keysToDataKeys(row.keys, 'entityField');
        ds.alarmFilterConfig = { statusList: [], severityList: [], typeList: [], searchPropagatedAlarms: false };
        c.alarmSource = ds;
        c.timewindow = { type: 'HISTORY', history: { type: 'INTERVAL', interval: row.lastMs } };
        break;
      }
      default: {
        // ENTITY_TABLE, TIME_SERIES_TABLE, TIME_SERIES_CHART, LATEST_CHART
        c.title = row.title;
        const ds = buildDataSource(row.entityId, row.rowId, ensureAlias);
        const dataKeys = keysToDataKeys(row.keys, usesTimeseriesKeys(row.type) ? 'timeseries' : 'attribute');
        if (isLatestSnapshotComponent(row.type)) {
          ds.latestDataKeys = dataKeys;
        } else {
          ds.dataKeys = dataKeys;
        }
        c.dataSources = [ds];
        if (row.type === ReportComponentType.LATEST_CHART) {
          c.chartType = row.chartType;
        }
      }
    }
    return c;
  }

  /**
   * A bare number is the normal case: PE expands it to all four sides. The exception is a template
   * whose four margins differ - it loaded with a blank field, and writing the field's value back
   * would silently flatten the other three sides, so the original object is kept unless the user
   * actually typed something.
   */
  private resolvePageMargins(formValue: number): number | Insets {
    if (formValue == null && this.originalPageMargins != null) {
      return this.originalPageMargins;
    }
    return formValue;
  }

  private headerFooterToText(hf: ReportTemplate['configuration']['header']): string {
    const first = hf?.components?.[0];
    return first?.value || '';
  }

  private textToHeaderFooter(text: string): ReportTemplate['configuration']['header'] {
    if (!text) {
      return { enabled: false, components: [] };
    }
    return { enabled: true, components: [{ type: ReportComponentType.RICH_TEXT, value: text }] };
  }

}
