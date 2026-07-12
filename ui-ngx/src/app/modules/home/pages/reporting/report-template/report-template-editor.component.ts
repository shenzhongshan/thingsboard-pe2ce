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
import {
  dataDrivenReportComponentTypes,
  ReportComponentConfig,
  ReportComponentType,
  reportComponentTypeIcons,
  reportComponentTypeTranslations,
  ReportTemplate,
  ReportTemplateInfo,
  ReportTemplateType,
  TbReportFormat
} from '@shared/models/report.models';

export interface ReportTemplateEditorData {
  reportTemplateId: string;
}

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
  componentTypes = Object.values(ReportComponentType);
  componentTypeTranslations = reportComponentTypeTranslations;
  componentTypeIcons = reportComponentTypeIcons;
  dataDrivenTypes = dataDrivenReportComponentTypes;
  pageSizes = ['A4', 'A3', 'LETTER'];

  otherTemplates: ReportTemplateInfo[] = [];

  private template: ReportTemplate;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ReportTemplateEditorData,
              private dialogRef: MatDialogRef<ReportTemplateEditorComponent, boolean>,
              private reportTemplateService: ReportTemplateService,
              private fb: UntypedFormBuilder) {
  }

  ngOnInit() {
    this.reportTemplateService.getReportTemplate(this.data.reportTemplateId).subscribe((template) => {
      this.template = template;
      this.buildForm(template);
      this.loaded = true;
      if ((template.configuration?.components || []).some((c) => c.type === ReportComponentType.SUB_REPORT)) {
        this.loadOtherTemplates();
      }
    });
  }

  private buildForm(template: ReportTemplate) {
    const cfg = template.configuration || {components: []};
    this.form = this.fb.group({
      name: [template.name, [Validators.required]],
      description: [template.description || ''],
      namePattern: [cfg.namePattern || 'report-${date}'],
      pageSize: [cfg.pageSize || 'A4'],
      pageOrientation: [cfg.pageOrientation || 'portrait'],
      pageMargin: [cfg.pageMargin || '2cm'],
      header: [cfg.header || ''],
      footer: [cfg.footer || ''],
      components: this.fb.array((cfg.components || []).map((c) => this.buildComponentRow(c)))
    });
  }

  private buildComponentRow(c: ReportComponentConfig): UntypedFormGroup {
    return this.fb.group({
      type: [c.type],
      title: [c.title || ''],
      text: [c.text || ''],
      level: [c.level || 1],
      html: [c.html || ''],
      url: [c.url || ''],
      entityId: [c.entityId || null],
      keys: [c.keys || []],
      lastMs: [c.lastMs || 86400000],
      chartType: [c.chartType || 'BAR'],
      dashboardId: [c.dashboardId ? {id: c.dashboardId, entityType: EntityType.DASHBOARD} : null],
      widgets: this.fb.array((c.widgets || []).map((w) => this.buildComponentRow(w))),
      reportTemplateId: [c.reportTemplateId || null]
    });
  }

  get componentsArray(): UntypedFormArray {
    return this.form.get('components') as UntypedFormArray;
  }

  widgetsArray(componentRow: AbstractControl): UntypedFormArray {
    return componentRow.get('widgets') as UntypedFormArray;
  }

  addComponent(type: ReportComponentType) {
    this.componentsArray.push(this.buildComponentRow({type} as ReportComponentConfig));
    if (type === ReportComponentType.SUB_REPORT) {
      this.loadOtherTemplates();
    }
  }

  addWidget(componentRow: AbstractControl, type: ReportComponentType) {
    this.widgetsArray(componentRow).push(this.buildComponentRow({type} as ReportComponentConfig));
  }

  removeComponent(i: number) {
    this.componentsArray.removeAt(i);
  }

  removeWidget(componentRow: AbstractControl, i: number) {
    this.widgetsArray(componentRow).removeAt(i);
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
    const components: ReportComponentConfig[] = (v.components as any[]).map((row) => this.toComponentConfig(row));
    const updated: ReportTemplate = {
      ...this.template,
      name: v.name,
      description: v.description,
      configuration: {
        namePattern: v.namePattern,
        pageSize: v.pageSize,
        pageOrientation: v.pageOrientation,
        pageMargin: v.pageMargin,
        header: v.header,
        footer: v.footer,
        components
      }
    };
    this.reportTemplateService.saveReportTemplate(updated).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

  private toComponentConfig(row: any): ReportComponentConfig {
    const c: ReportComponentConfig = {type: row.type};
    switch (row.type) {
      case ReportComponentType.HEADING:
        c.text = row.text;
        c.level = row.level;
        break;
      case ReportComponentType.RICH_TEXT:
        c.html = row.html;
        break;
      case ReportComponentType.IMAGE:
        c.url = row.url;
        break;
      case ReportComponentType.DIVIDER:
      case ReportComponentType.PAGE_BREAK:
        break;
      case ReportComponentType.SUB_REPORT:
        c.reportTemplateId = row.reportTemplateId;
        break;
      case ReportComponentType.DASHBOARD:
        c.dashboardId = row.dashboardId?.id || null;
        c.widgets = ((row.widgets || []) as any[]).map((w) => this.toComponentConfig(w));
        break;
      default:
        c.title = row.title;
        c.entityId = row.entityId;
        c.keys = row.keys || [];
        c.lastMs = row.lastMs;
        if (row.type === ReportComponentType.LATEST_CHART) {
          c.chartType = row.chartType;
        }
    }
    return c;
  }

}
