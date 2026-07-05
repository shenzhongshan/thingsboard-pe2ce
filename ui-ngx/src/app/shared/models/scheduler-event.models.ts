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

export class SchedulerEventId implements HasUUID {
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

// Known scheduler event types (CE port: PE's generateReport/generateDashboardReport dropped).
export enum SchedulerEventType {
  UPDATE_ATTRIBUTES = 'updateAttributes',
  SEND_RPC_REQUEST = 'sendRpcRequest',
  UPDATE_FIRMWARE = 'updateFirmware',
  UPDATE_SOFTWARE = 'updateSoftware'
}

export const schedulerEventTypeTranslations = new Map<string, string>([
  [SchedulerEventType.UPDATE_ATTRIBUTES, 'scheduler.event-type-update-attributes'],
  [SchedulerEventType.SEND_RPC_REQUEST, 'scheduler.event-type-send-rpc-request'],
  [SchedulerEventType.UPDATE_FIRMWARE, 'scheduler.event-type-update-firmware'],
  [SchedulerEventType.UPDATE_SOFTWARE, 'scheduler.event-type-update-software']
]);

// Display names matching the PE UI dropdown.
export const schedulerEventTypeNames = new Map<string, string>([
  [SchedulerEventType.UPDATE_ATTRIBUTES, 'Update Attributes'],
  [SchedulerEventType.SEND_RPC_REQUEST, 'Send RPC Request to Device'],
  [SchedulerEventType.UPDATE_FIRMWARE, 'Update Firmware'],
  [SchedulerEventType.UPDATE_SOFTWARE, 'Update Software']
]);

export enum SchedulerRepeatType {
  DAILY = 'DAILY',
  EVERY_N_DAYS = 'EVERY_N_DAYS',
  WEEKLY = 'WEEKLY',
  EVERY_N_WEEKS = 'EVERY_N_WEEKS',
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY',
  TIMER = 'TIMER'
}

export const schedulerRepeatTypeTranslations = new Map<SchedulerRepeatType, string>([
  [SchedulerRepeatType.DAILY, 'scheduler.repeat-type-daily'],
  [SchedulerRepeatType.EVERY_N_DAYS, 'scheduler.repeat-type-every-n-days'],
  [SchedulerRepeatType.WEEKLY, 'scheduler.repeat-type-weekly'],
  [SchedulerRepeatType.EVERY_N_WEEKS, 'scheduler.repeat-type-every-n-weeks'],
  [SchedulerRepeatType.MONTHLY, 'scheduler.repeat-type-monthly'],
  [SchedulerRepeatType.YEARLY, 'scheduler.repeat-type-yearly'],
  [SchedulerRepeatType.TIMER, 'scheduler.repeat-type-timer']
]);

export enum SchedulerTimeUnit {
  MINUTES = 'MINUTES',
  HOURS = 'HOURS'
}

export const schedulerTimeUnitTranslations = new Map<SchedulerTimeUnit, string>([
  [SchedulerTimeUnit.MINUTES, 'scheduler.time-unit-minutes'],
  [SchedulerTimeUnit.HOURS, 'scheduler.time-unit-hours']
]);

export interface SchedulerRepeat {
  type: SchedulerRepeatType;
  endsOn: number;
  // WEEKLY: 0 (Sunday) .. 6 (Saturday)
  repeatOn?: number[];
  // EVERY_N_DAYS / EVERY_N_WEEKS
  days?: number;
  weeks?: number;
  // TIMER
  repeatInterval?: number;
  timeUnit?: SchedulerTimeUnit;
}

export interface SchedulerEventSchedule {
  timezone: string;
  startTime: number;
  repeat?: SchedulerRepeat;
}

// Configuration payload: what the fired event does.
export interface SchedulerEventConfiguration {
  originatorId?: EntityId;
  msgType?: string;
  msgBody?: any;
  metadata?: {[key: string]: string};
}

export interface SchedulerEventInfo extends BaseData<SchedulerEventId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  originatorId?: EntityId;
  name: string;
  type: string;
  schedule: SchedulerEventSchedule;
  enabled: boolean;
  additionalInfo?: any;
}

export interface SchedulerEventWithCustomerInfo extends SchedulerEventInfo {
  customerTitle?: string;
  customerIsPublic?: boolean;
  // Fire times within the requested range; present only in the calendar (time-filter) query.
  timestamps?: number[];
}

export interface SchedulerEvent extends SchedulerEventInfo {
  configuration: SchedulerEventConfiguration;
}

export function newSchedulerEvent(defaultTimezone: string): SchedulerEvent {
  return {
    name: '',
    type: null,
    enabled: true,
    schedule: {
      timezone: defaultTimezone,
      startTime: new Date().setHours(0, 0, 0, 0) + 24 * 60 * 60 * 1000
    },
    configuration: {
      originatorId: null,
      msgType: null,
      msgBody: {},
      metadata: {}
    }
  };
}
