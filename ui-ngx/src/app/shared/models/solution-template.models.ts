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

import { EntityId } from '@shared/models/id/entity-id';

export enum SolutionTemplateLevel {
  MAKER = 'MAKER',
  PROTOTYPE = 'PROTOTYPE',
  STARTUP = 'STARTUP'
}

export const SolutionTemplateLevelTranslationMap = new Map<SolutionTemplateLevel, string>([
  [SolutionTemplateLevel.MAKER, 'solution-template.level-maker'],
  [SolutionTemplateLevel.PROTOTYPE, 'solution-template.level-prototype'],
  [SolutionTemplateLevel.STARTUP, 'solution-template.level-startup']
]);

export interface SolutionTemplate {
  id: string;
  title: string;
  level: SolutionTemplateLevel;
  installTimeoutMs: number;
  tenantTelemetryKeys: string[];
  tenantAttributeKeys: string[];
}

export interface SolutionTemplateInfo extends SolutionTemplate {
  previewImageUrl: string;
  videoPreviewImageUrl: string;
  previewMp4Url: string;
  previewWebmUrl: string;
}

export interface TenantSolutionTemplateInfo extends SolutionTemplateInfo {
  installed: boolean;
}

export interface SolutionTemplateDetails extends SolutionTemplate {
  imageUrls: string[];
  highlights: string;
  description: string;
}

export interface TenantSolutionTemplateDetails extends SolutionTemplateDetails {
  installed: boolean;
}

export interface TenantSolutionTemplateInstructions {
  dashboardGroupId: EntityId;
  dashboardId: EntityId;
  publicId: EntityId;
  mainDashboardPublic: boolean;
  details: string;
}

export interface SolutionInstallResponse extends TenantSolutionTemplateInstructions {
  success: boolean;
}
