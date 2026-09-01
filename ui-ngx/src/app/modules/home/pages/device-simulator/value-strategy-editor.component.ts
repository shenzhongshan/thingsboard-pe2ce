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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  defaultValueStrategy,
  ValueStrategyDefinition,
  ValueStrategySchedule,
  ValueStrategyType,
  valueStrategyTypeTranslations
} from '@shared/models/device-simulator.models';

/**
 * Recursively edits one ValueStrategyDefinition. `composite`/`schedule` strategies nest further
 * ValueStrategyDefinitions, so this component renders itself again for those slots rather than
 * hand-coding a fixed nesting depth.
 */
@Component({
  selector: 'tb-value-strategy-editor',
  templateUrl: './value-strategy-editor.component.html',
  styleUrls: ['./value-strategy-editor.component.scss'],
  standalone: false
})
export class ValueStrategyEditorComponent {

  // Typed `any` rather than the ValueStrategyDefinition union: this component dispatches on
  // strategy.type at runtime (*ngSwitch below), and Angular's template type-narrowing does not
  // discriminate a union from an enum-valued property the way plain TypeScript control flow does.
  @Input()
  strategy: any;

  @Output()
  strategyChange = new EventEmitter<ValueStrategyDefinition>();

  @Input()
  label = '';

  ValueStrategyType = ValueStrategyType;
  strategyTypes = Object.values(ValueStrategyType);
  valueStrategyTypeTranslations = valueStrategyTypeTranslations;

  onTypeChange(type: ValueStrategyType): void {
    this.strategy = defaultValueStrategy(type);
    this.emit();
  }

  emit(): void {
    this.strategyChange.emit(this.strategy);
  }

  addScheduleWindow(): void {
    const s = this.strategy as any;
    const window: ValueStrategySchedule = {
      startHour: 0, startMinute: 0, endHour: 0, endMinute: 0,
      definition: defaultValueStrategy(ValueStrategyType.CONSTANT)
    };
    s.schedule = [...(s.schedule || []), window];
    this.emit();
  }

  removeScheduleWindow(index: number): void {
    const s = this.strategy as any;
    s.schedule.splice(index, 1);
    this.emit();
  }

  sequenceKeys(): string[] {
    const s = this.strategy as any;
    return s.telemetry ? Object.keys(s.telemetry) : [];
  }

  get sequenceTelemetryJson(): string {
    const s = this.strategy as any;
    return JSON.stringify(s.telemetry || {}, null, 2);
  }

  set sequenceTelemetryJson(value: string) {
    try {
      const parsed = JSON.parse(value);
      (this.strategy as any).telemetry = parsed;
      this.emit();
    } catch (e) {
      // leave the invalid text as-is; the user is still typing
    }
  }

}
