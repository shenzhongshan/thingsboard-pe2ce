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
import { HasUUID } from '@shared/models/id/has-uuid';

// Mirrors org.thingsboard.server.service.solutions.data.values.ValueStrategyDefinitionType /
// ValueStrategyDefinition's @JsonSubTypes — the `type` discriminator solution templates use in
// device_emulators.json. Kept in sync manually since there is no shared schema source.
export enum ValueStrategyType {
  CONSTANT = 'constant',
  NATURAL = 'natural',
  COUNTER = 'counter',
  EVENT = 'event',
  SEQUENCE = 'sequence',
  COMPOSITE = 'composite',
  SCHEDULE = 'schedule',
  INC = 'inc',
  DEC = 'dec'
}

export const valueStrategyTypeTranslations = new Map<ValueStrategyType, string>([
  [ValueStrategyType.CONSTANT, 'device-simulator.strategy.constant'],
  [ValueStrategyType.NATURAL, 'device-simulator.strategy.natural'],
  [ValueStrategyType.COUNTER, 'device-simulator.strategy.counter'],
  [ValueStrategyType.EVENT, 'device-simulator.strategy.event'],
  [ValueStrategyType.SEQUENCE, 'device-simulator.strategy.sequence'],
  [ValueStrategyType.COMPOSITE, 'device-simulator.strategy.composite'],
  [ValueStrategyType.SCHEDULE, 'device-simulator.strategy.schedule'],
  [ValueStrategyType.INC, 'device-simulator.strategy.inc'],
  [ValueStrategyType.DEC, 'device-simulator.strategy.dec']
]);

export interface ConstantValueStrategyDefinition {
  type: ValueStrategyType.CONSTANT;
  value: any;
}

export interface NaturalValueStrategyDefinition {
  type: ValueStrategyType.NATURAL;
  precision: number;
  minStartValue: number;
  maxStartValue: number;
  minLowValue: number;
  maxLowValue: number;
  minHighValue: number;
  maxHighValue: number;
  minIncrement: number;
  maxIncrement: number;
  minDecrement: number;
  maxDecrement: number;
  holidayMultiplier: number;
  workHoursMultiplier: number;
  nightHoursMultiplier: number;
  decrementOnStart: boolean;
}

export interface CounterValueStrategyDefinition {
  type: ValueStrategyType.COUNTER;
  precision: number;
  minStartValue: number;
  maxStartValue: number;
  minEndValue: number;
  maxEndValue: number;
  minIncrement: number;
  maxIncrement: number;
  holidayMultiplier: number;
  workHoursMultiplier: number;
  nightHoursMultiplier: number;
}

export interface EventValueStrategyDefinition {
  type: ValueStrategyType.EVENT;
  normalValue: any;
  anomalyValue: any;
  anomalyChance: number;
}

export interface SequenceValueStrategyDefinition {
  type: ValueStrategyType.SEQUENCE;
  random: boolean;
  // key -> array of values; every key advances through its array in lockstep.
  telemetry: { [key: string]: any[] };
}

export interface CompositeValueStrategyDefinition {
  type: ValueStrategyType.COMPOSITE;
  defaultHours: ValueStrategyDefinition;
  workHours?: ValueStrategyDefinition;
  nightHours?: ValueStrategyDefinition;
  holidayHours?: ValueStrategyDefinition;
}

export interface ValueStrategySchedule {
  startHour: number;
  startMinute: number;
  endHour: number;
  endMinute: number;
  definition: ValueStrategyDefinition;
}

export interface ScheduleValueStrategyDefinition {
  type: ValueStrategyType.SCHEDULE;
  timeZone: string;
  schedule: ValueStrategySchedule[];
  defaultDefinition: ValueStrategyDefinition;
}

export interface IncrementValueStrategyDefinition {
  type: ValueStrategyType.INC;
  precision: number;
  minStartValue: number;
  maxStartValue: number;
  minEndValue: number;
  maxEndValue: number;
  minIncrement: number;
  maxIncrement: number;
}

export interface DecrementValueStrategyDefinition {
  type: ValueStrategyType.DEC;
  precision: number;
  minStartValue: number;
  maxStartValue: number;
  minEndValue: number;
  maxEndValue: number;
  minDecrement: number;
  maxDecrement: number;
}

export type ValueStrategyDefinition =
  ConstantValueStrategyDefinition |
  NaturalValueStrategyDefinition |
  CounterValueStrategyDefinition |
  EventValueStrategyDefinition |
  SequenceValueStrategyDefinition |
  CompositeValueStrategyDefinition |
  ScheduleValueStrategyDefinition |
  IncrementValueStrategyDefinition |
  DecrementValueStrategyDefinition;

export interface TelemetryProfile {
  key: string;
  valueStrategy: ValueStrategyDefinition;
}

// Mirrors org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition. `clazz` and
// `extendz` are omitted on purpose: user-authored simulators always resolve to the plain
// key-by-key generator, they cannot name a custom emulator class or inherit from another definition.
export interface EmulatorDefinition {
  publishPeriodInDays: number;
  publishFrequencyInSeconds: number;
  publishPauseInMillis: number;
  activityPeriodInMillis: number;
  telemetryProfiles: TelemetryProfile[];
}

export enum SimulatorRunMode {
  HISTORICAL = 'HISTORICAL',
  LIVE = 'LIVE'
}

export interface DeviceSimulator {
  id?: HasUUID;
  createdTime?: number;
  tenantId?: EntityId;
  deviceId: EntityId;
  name: string;
  definition: EmulatorDefinition;
}

export interface DeviceSimulatorInfo {
  id: HasUUID;
  createdTime: number;
  name: string;
  deviceId: EntityId;
  deviceName: string;
  definition: EmulatorDefinition;
  running: boolean;
  mode: SimulatorRunMode;
  startedTime: number;
  publishedCount: number;
  lastError: string;
}

export function defaultEmulatorDefinition(): EmulatorDefinition {
  return {
    publishPeriodInDays: 7,
    publishFrequencyInSeconds: 3600,
    publishPauseInMillis: 1,
    activityPeriodInMillis: 0,
    telemetryProfiles: []
  };
}

export function defaultValueStrategy(type: ValueStrategyType): ValueStrategyDefinition {
  switch (type) {
    case ValueStrategyType.CONSTANT:
      return { type, value: 0 };
    case ValueStrategyType.NATURAL:
      return {
        type, precision: 2,
        minStartValue: 20, maxStartValue: 25,
        minLowValue: 15, maxLowValue: 15,
        minHighValue: 30, maxHighValue: 30,
        minIncrement: 0.1, maxIncrement: 0.5,
        minDecrement: 0.1, maxDecrement: 0.5,
        holidayMultiplier: 1, workHoursMultiplier: 1, nightHoursMultiplier: 1,
        decrementOnStart: false
      };
    case ValueStrategyType.COUNTER:
      return {
        type, precision: 2,
        minStartValue: 0, maxStartValue: 0,
        minEndValue: 1000000, maxEndValue: 1000000,
        minIncrement: 1, maxIncrement: 5,
        holidayMultiplier: 1, workHoursMultiplier: 1, nightHoursMultiplier: 1
      };
    case ValueStrategyType.EVENT:
      return { type, normalValue: false, anomalyValue: true, anomalyChance: 1000 };
    case ValueStrategyType.SEQUENCE:
      return { type, random: false, telemetry: {} };
    case ValueStrategyType.COMPOSITE:
      return { type, defaultHours: defaultValueStrategy(ValueStrategyType.CONSTANT) };
    case ValueStrategyType.SCHEDULE:
      return {
        type, timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone, schedule: [],
        defaultDefinition: defaultValueStrategy(ValueStrategyType.CONSTANT)
      };
    case ValueStrategyType.INC:
      return {
        type, precision: 2,
        minStartValue: 0, maxStartValue: 0,
        minEndValue: 100, maxEndValue: 100,
        minIncrement: 1, maxIncrement: 5
      };
    case ValueStrategyType.DEC:
      return {
        type, precision: 2,
        minStartValue: 100, maxStartValue: 100,
        minEndValue: 0, maxEndValue: 0,
        minDecrement: 1, maxDecrement: 5
      };
  }
}
