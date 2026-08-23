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

// PE's report component model (this port's configuration is now the typed model itself, not a
// free-form CE-shaped JSON blob — see backend ReportTemplate javadoc). ERROR and SPLIT_VIEW exist
// in the model and render correctly, but aren't offered from the "add component" menu: ERROR is
// PE's own internal rendering-failure placeholder, never authored directly, and SPLIT_VIEW's two
// nested child components don't fit this editor's flat per-row form without a materially bigger
// UI. DASHBOARD is likewise modeled but not offered: it needs PE's separate dashboard-screenshot
// service, which this port does not have.
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
  DIVIDER = 'DIVIDER',
  ERROR = 'ERROR',
  SPLIT_VIEW = 'SPLIT_VIEW'
}

// What the "add component" menu actually offers (see the class-level note on ReportComponentType).
export const addableReportComponentTypes: ReportComponentType[] = [
  ReportComponentType.HEADING,
  ReportComponentType.RICH_TEXT,
  ReportComponentType.IMAGE,
  ReportComponentType.DIVIDER,
  ReportComponentType.PAGE_BREAK,
  ReportComponentType.SUB_REPORT,
  ReportComponentType.ENTITY_TABLE,
  ReportComponentType.TIME_SERIES_TABLE,
  ReportComponentType.ALARM_TABLE,
  ReportComponentType.TIME_SERIES_CHART,
  ReportComponentType.LATEST_CHART
];

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
  [ReportComponentType.DIVIDER, 'report.component-type-divider'],
  [ReportComponentType.ERROR, 'report.component-type-error'],
  [ReportComponentType.SPLIT_VIEW, 'report.component-type-split-view']
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
  [ReportComponentType.DIVIDER, 'horizontal_rule'],
  [ReportComponentType.ERROR, 'error_outline'],
  [ReportComponentType.SPLIT_VIEW, 'vertical_split']
]);

// Component types whose configuration is backed by an entity + telemetry key selection.
export const dataDrivenReportComponentTypes: ReportComponentType[] = [
  ReportComponentType.ENTITY_TABLE,
  ReportComponentType.TIME_SERIES_TABLE,
  ReportComponentType.ALARM_TABLE,
  ReportComponentType.TIME_SERIES_CHART,
  ReportComponentType.LATEST_CHART
];

// ENTITY_TABLE/LATEST_CHART fetch a snapshot ("latest value of each key"); TIME_SERIES_TABLE/CHART
// fetch a time range. Both are wire-compatible with a plain `type: 'timeseries'` DataKey (verified
// against the running backend) - the difference is only which DataSource list they land in.
const latestSnapshotComponentTypes: ReportComponentType[] = [
  ReportComponentType.ENTITY_TABLE,
  ReportComponentType.LATEST_CHART
];

// --- PE's typed configuration model (report.configuration.*) -------------------------------

export interface AliasEntityId {
  entityType: string;
  id: string;
}

export interface SingleEntityFilter {
  type: 'singleEntity';
  singleEntity: AliasEntityId;
}

export interface EntityAlias {
  id: string;
  alias: string;
  filter: SingleEntityFilter;
}

export interface DataKey {
  name: string;
  type: 'timeseries' | 'attribute' | 'entityField';
  label?: string;
}

export interface AlarmFilterConfig {
  statusList: string[];
  severityList: string[];
  typeList: string[];
  searchPropagatedAlarms: boolean;
}

export interface DataSource {
  type: 'device' | 'entity';
  deviceId?: string;
  entityAliasId?: string;
  dataKeys?: DataKey[];
  latestDataKeys?: DataKey[];
  alarmFilterConfig?: AlarmFilterConfig;
}

export interface HistoryInterval {
  type: 'INTERVAL';
  interval: number;
}

export interface TimeWindowConfig {
  type: 'HISTORY';
  history: HistoryInterval;
}

export interface ReportComponentConfig {
  type: ReportComponentType;
  title?: string;
  // HEADING, RICH_TEXT
  value?: string;
  // IMAGE - sourceType is always 'image' from this editor (a fixed URL); PE's other option,
  // 'entityKey' (image sourced dynamically from an entity's key at render time), has no UI here.
  sourceType?: 'image' | 'entityKey';
  imageUrl?: string;
  // SUB_REPORT
  templateId?: { entityType: 'REPORT_TEMPLATE'; id: string };
  // ENTITY_TABLE / TIME_SERIES_TABLE / TIME_SERIES_CHART / LATEST_CHART
  dataSources?: DataSource[];
  chartType?: 'BAR' | 'PIE';
  // ALARM_TABLE - note the field is singular (alarmSource), not the dataSources list the other
  // data-driven types use; PE models one alarm table as reading exactly one data source.
  alarmSource?: DataSource;
  timewindow?: TimeWindowConfig;
}

/**
 * The date placeholder is `%d{<java SimpleDateFormat>}`, expanded by the backend's
 * ReportUtils.prepareReportName. Anything else is left in the filename verbatim - so this must
 * stay in sync with ReportUtils.DEFAULT_REPORT_NAME_PATTERN rather than being a UI-side invention.
 */
/** The date token itself, shown in the UI hint. Braces are deliberately NOT put in the
 * translated string: ngx-translate runs it through an ICU messageformat parser, which
 * rejects `{yyyy-MM-dd_HH:mm:ss}` as a malformed argument. Passed in as an interpolation
 * parameter instead. */
export const REPORT_NAME_DATE_TOKEN = '%d{yyyy-MM-dd_HH:mm:ss}';

export const DEFAULT_REPORT_NAME_PATTERN = `report-${REPORT_NAME_DATE_TOKEN}`;

export interface Insets {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

export interface ReportTemplateConfiguration {
  format: TbReportFormat;
  namePattern?: string;
  pageSize?: string;
  pageOrientation?: 'PORTRAIT' | 'LANDSCAPE';
  // Asymmetric on read, symmetric-shorthand on write: PE's Insets has a single-int constructor, so
  // posting a bare number applies it to all four sides - but it always serializes back as the full
  // object. Both shapes therefore reach this editor. See pageMarginsToNumber.
  pageMargins?: number | Insets;
  header?: { enabled: boolean; components: ReportComponentConfig[] };
  footer?: { enabled: boolean; components: ReportComponentConfig[] };
  entityAliases?: EntityAlias[];
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
      format: TbReportFormat.PDF,
      namePattern: DEFAULT_REPORT_NAME_PATTERN,
      pageSize: 'A4',
      pageOrientation: 'PORTRAIT',
      pageMargins: 20,
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

// --- helpers shared by the editor: translating the simple "pick one entity" UX into PE's ------
// --- alias-based DataSource shape, and back. Kept here (not in the component) so both the ------
// --- editor and any future header/footer sub-editor can reuse them. ---------------------------

/** True for chart/table components whose key list is timeseries-typed, per the running backend. */
export function usesTimeseriesKeys(type: ReportComponentType): boolean {
  return dataDrivenReportComponentTypes.includes(type) && type !== ReportComponentType.ALARM_TABLE;
}

export function isLatestSnapshotComponent(type: ReportComponentType): boolean {
  return latestSnapshotComponentTypes.includes(type);
}

/**
 * Builds the DataSource for a data-driven component from a plain entity picked in the UI.
 * DEVICE entities use PE's dedicated `device` shortcut (no alias needed - verified against the
 * backend). Every other allowed type (ASSET, ENTITY_VIEW, CUSTOMER) needs an EntityAlias, since
 * DataSource only has a raw id field for the device case; `ensureAlias` creates or reuses one in
 * `entityAliases`, keyed by `aliasId` so editing the same component twice does not accumulate
 * orphaned aliases.
 */
export function buildDataSource(entityId: EntityId, aliasId: string,
                                 ensureAlias: (id: string, target: EntityId) => void): DataSource {
  if (!entityId) {
    return { type: 'device' };
  }
  if (entityId.entityType === 'DEVICE') {
    return { type: 'device', deviceId: entityId.id };
  }
  ensureAlias(aliasId, entityId);
  return { type: 'entity', entityAliasId: aliasId };
}

/** Reverses buildDataSource: resolves a DataSource back to the entity the UI should show as picked. */
export function resolveDataSourceEntity(ds: DataSource, aliases: EntityAlias[]): EntityId {
  if (!ds) {
    return null;
  }
  if (ds.type === 'device') {
    return ds.deviceId ? { entityType: 'DEVICE', id: ds.deviceId } as EntityId : null;
  }
  const alias = (aliases || []).find((a) => a.id === ds.entityAliasId);
  const target = alias?.filter?.singleEntity;
  return target ? { entityType: target.entityType, id: target.id } as EntityId : null;
}

/**
 * Collapses PE's pageMargins to the single number this editor's one margin field can show.
 * Returns null when the four sides differ, since no single number represents them faithfully -
 * callers should leave the field blank and preserve the original object rather than flatten it.
 */
export function pageMarginsToNumber(margins: number | Insets): number {
  if (margins == null) {
    return null;
  }
  if (typeof margins === 'number') {
    return margins;
  }
  const { left, right, top, bottom } = margins;
  return (left === right && right === top && top === bottom) ? left : null;
}

export function keysToDataKeys(keys: string[], type: DataKey['type']): DataKey[] {
  return (keys || []).map((name) => ({ name, type, label: name }));
}

export function dataKeysToKeys(keys: DataKey[]): string[] {
  return (keys || []).map((k) => k.name);
}
