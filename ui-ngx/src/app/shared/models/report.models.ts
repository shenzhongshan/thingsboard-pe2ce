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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityId } from '@shared/models/id/entity-id';
import { HasUUID } from '@shared/models/id/has-uuid';
import { UserId } from '@shared/models/id/user-id';

export class ReportTemplateId implements HasUUID {
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export class ReportId implements HasUUID {
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export enum TbReportFormat {
  PDF = 'PDF',
  CSV = 'CSV'
}

export enum ReportTemplateType {
  REPORT = 'REPORT',
  SUB_REPORT = 'SUB_REPORT'
}

// CE port: PE's ~14 component types collapse into these 12 (ERROR/SPLIT_VIEW dropped) — see
// backend ReportComponentType javadoc for the JsonNode-configuration rationale.
export enum ReportComponentType {
  HEADING = 'HEADING',
  RICH_TEXT = 'RICH_TEXT',
  ENTITY_TABLE = 'ENTITY_TABLE',
  TIME_SERIES_TABLE = 'TIME_SERIES_TABLE',
  ALARM_TABLE = 'ALARM_TABLE',
  TIME_SERIES_CHART = 'TIME_SERIES_CHART',
  LATEST_CHART = 'LATEST_CHART',
  DASHBOARD = 'DASHBOARD',
  IMAGE = 'IMAGE',
  SUB_REPORT = 'SUB_REPORT',
  PAGE_BREAK = 'PAGE_BREAK',
  DIVIDER = 'DIVIDER'
}

export const reportComponentTypeTranslations = new Map<ReportComponentType, string>([
  [ReportComponentType.HEADING, 'report.component-type-heading'],
  [ReportComponentType.RICH_TEXT, 'report.component-type-rich-text'],
  [ReportComponentType.ENTITY_TABLE, 'report.component-type-entity-table'],
  [ReportComponentType.TIME_SERIES_TABLE, 'report.component-type-time-series-table'],
  [ReportComponentType.ALARM_TABLE, 'report.component-type-alarm-table'],
  [ReportComponentType.TIME_SERIES_CHART, 'report.component-type-time-series-chart'],
  [ReportComponentType.LATEST_CHART, 'report.component-type-latest-chart'],
  [ReportComponentType.DASHBOARD, 'report.component-type-dashboard'],
  [ReportComponentType.IMAGE, 'report.component-type-image'],
  [ReportComponentType.SUB_REPORT, 'report.component-type-sub-report'],
  [ReportComponentType.PAGE_BREAK, 'report.component-type-page-break'],
  [ReportComponentType.DIVIDER, 'report.component-type-divider']
]);

export const reportComponentTypeIcons = new Map<ReportComponentType, string>([
  [ReportComponentType.HEADING, 'title'],
  [ReportComponentType.RICH_TEXT, 'notes'],
  [ReportComponentType.ENTITY_TABLE, 'table_chart'],
  [ReportComponentType.TIME_SERIES_TABLE, 'table_rows'],
  [ReportComponentType.ALARM_TABLE, 'mdi:alert-outline'],
  [ReportComponentType.TIME_SERIES_CHART, 'show_chart'],
  [ReportComponentType.LATEST_CHART, 'bar_chart'],
  [ReportComponentType.DASHBOARD, 'dashboard'],
  [ReportComponentType.IMAGE, 'image'],
  [ReportComponentType.SUB_REPORT, 'description'],
  [ReportComponentType.PAGE_BREAK, 'insert_page_break'],
  [ReportComponentType.DIVIDER, 'horizontal_rule']
]);

// Component types whose configuration is backed by an entity + telemetry key selection.
export const dataDrivenReportComponentTypes: ReportComponentType[] = [
  ReportComponentType.ENTITY_TABLE,
  ReportComponentType.TIME_SERIES_TABLE,
  ReportComponentType.ALARM_TABLE,
  ReportComponentType.TIME_SERIES_CHART,
  ReportComponentType.LATEST_CHART
];

export interface ReportComponentConfig {
  type: ReportComponentType;
  title?: string;
  // HEADING
  text?: string;
  level?: number;
  // RICH_TEXT
  html?: string;
  // IMAGE
  url?: string;
  // data-driven components
  entityId?: EntityId;
  keys?: string[];
  lastMs?: number;
  // LATEST_CHART
  chartType?: 'BAR' | 'PIE';
  // DASHBOARD
  dashboardId?: string;
  widgets?: ReportComponentConfig[];
  // SUB_REPORT
  reportTemplateId?: string;
}

export interface ReportTemplateConfiguration {
  namePattern?: string;
  pageSize?: string;
  pageOrientation?: 'portrait' | 'landscape';
  pageMargin?: string;
  header?: string;
  footer?: string;
  components: ReportComponentConfig[];
}

export interface ReportTemplateInfo extends BaseData<ReportTemplateId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  format: TbReportFormat;
  type: ReportTemplateType;
  description?: string;
  ownerName?: string;
}

export interface ReportTemplate extends ReportTemplateInfo {
  configuration: ReportTemplateConfiguration;
}

export function newReportTemplate(): ReportTemplate {
  return {
    name: '',
    format: TbReportFormat.PDF,
    type: ReportTemplateType.REPORT,
    description: '',
    configuration: {
      namePattern: 'report-${date}',
      pageSize: 'A4',
      pageOrientation: 'portrait',
      pageMargin: '2cm',
      components: []
    }
  } as ReportTemplate;
}

export interface ReportInfo extends BaseData<ReportId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  templateId?: ReportTemplateId;
  format: TbReportFormat;
  name: string;
  userId?: UserId;
  templateName?: string;
  userName?: string;
  customerTitle?: string;
}

// Payload shape for a scheduler event of type 'generateReport' (configuration.msgBody).
export interface ReportConfig {
  reportTemplateId: ReportTemplateId;
  userId?: UserId;
  timezone?: string;
  targets?: string[];
  notificationTemplateId?: string;
}
