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
import {
  AbstractControl,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { EntityService } from '@core/http/entity.service';
import {
  newSchedulerEvent,
  SchedulerEvent,
  SchedulerEventType,
  schedulerEventTypeNames,
  SchedulerRepeat,
  SchedulerRepeatType,
  schedulerRepeatTypeTranslations,
  SchedulerTimeUnit,
  schedulerTimeUnitTranslations
} from '@shared/models/scheduler-event.models';
import { getDefaultTimezone } from '@shared/models/time/time.models';

export interface SchedulerEventDialogData {
  schedulerEventId?: string;
}

@Component({
  selector: 'tb-scheduler-event-dialog',
  templateUrl: './scheduler-event-dialog.component.html',
  styleUrls: ['./scheduler-event-dialog.component.scss'],
  standalone: false
})
export class SchedulerEventDialogComponent implements OnInit {

  form: UntypedFormGroup;

  isEdit = false;
  loaded = false;

  SchedulerEventType = SchedulerEventType;
  SchedulerRepeatType = SchedulerRepeatType;
  SchedulerTimeUnit = SchedulerTimeUnit;
  EntityType = EntityType;
  OtaUpdateType = OtaUpdateType;
  AttributeScope = AttributeScope;

  eventTypes = Object.values(SchedulerEventType) as string[];
  eventTypeNames = schedulerEventTypeNames;
  repeatTypes = Object.values(SchedulerRepeatType);
  repeatTypeTranslations = schedulerRepeatTypeTranslations;
  timeUnits = Object.values(SchedulerTimeUnit);
  timeUnitTranslations = schedulerTimeUnitTranslations;
  weekDays = [0, 1, 2, 3, 4, 5, 6];
  weekDayNames = ['scheduler.sunday', 'scheduler.monday', 'scheduler.tuesday', 'scheduler.wednesday',
    'scheduler.thursday', 'scheduler.friday', 'scheduler.saturday'];

  attributeScopes = [AttributeScope.SERVER_SCOPE, AttributeScope.SHARED_SCOPE];

  targetDeviceProfileId: string = null;

  private schedulerEvent: SchedulerEvent;

  constructor(@Inject(MAT_DIALOG_DATA) public data: SchedulerEventDialogData,
              private dialogRef: MatDialogRef<SchedulerEventDialogComponent, boolean>,
              protected store: Store<AppState>,
              private schedulerEventService: SchedulerEventService,
              private entityService: EntityService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.schedulerEventId;
  }

  ngOnInit() {
    if (this.isEdit) {
      this.schedulerEventService.getSchedulerEvent(this.data.schedulerEventId).subscribe((event) => {
        this.schedulerEvent = event;
        this.buildForm(event);
        this.loaded = true;
      });
    } else {
      this.schedulerEvent = newSchedulerEvent(getDefaultTimezone());
      this.buildForm(this.schedulerEvent);
      this.loaded = true;
    }
  }

  private buildForm(event: SchedulerEvent) {
    const config = event.configuration || {};
    const repeat = event.schedule?.repeat;
    const attributes = config.msgBody && event.type === SchedulerEventType.UPDATE_ATTRIBUTES ?
      Object.entries(config.msgBody).map(([key, value]) => ({key, value: this.valueToString(value)})) : [];
    this.form = this.fb.group({
      name: [event.name, [Validators.required]],
      type: [event.type, [Validators.required]],
      enabled: [event.enabled],
      originatorId: [event.originatorId, [Validators.required]],
      // updateAttributes
      attributesScope: [config.metadata?.scope || AttributeScope.SERVER_SCOPE],
      attributes: this.fb.array(attributes.map((kv) => this.buildAttributeRow(kv.key, kv.value))),
      // sendRpcRequest
      rpcMethod: [event.type === SchedulerEventType.SEND_RPC_REQUEST ? config.msgBody?.method : ''],
      rpcParams: [event.type === SchedulerEventType.SEND_RPC_REQUEST && config.msgBody?.params !== undefined ?
        JSON.stringify(config.msgBody.params) : '{}'],
      rpcOneway: [config.metadata?.oneway === 'true'],
      // updateFirmware / updateSoftware
      otaPackageId: [this.isOtaType(event.type) && config.msgBody?.id ? config.msgBody.id : null],
      // schedule
      timezone: [event.schedule?.timezone, [Validators.required]],
      startTime: [event.schedule?.startTime, [Validators.required]],
      repeatEnabled: [!!repeat],
      repeatType: [repeat?.type || SchedulerRepeatType.DAILY],
      repeatOn: [repeat?.repeatOn || []],
      repeatDays: [repeat?.days || 1],
      repeatWeeks: [repeat?.weeks || 1],
      repeatInterval: [repeat?.repeatInterval || 1],
      repeatTimeUnit: [repeat?.timeUnit || SchedulerTimeUnit.HOURS],
      endsOn: [repeat?.endsOn || (new Date().setHours(0, 0, 0, 0) + 8 * 24 * 60 * 60 * 1000)]
    });
    this.onOriginatorChanged(event.originatorId);
    this.form.get('originatorId').valueChanges.subscribe((originatorId) => this.onOriginatorChanged(originatorId));
    this.form.get('type').valueChanges.subscribe(() => {
      this.form.get('originatorId').patchValue(null, {emitEvent: false});
      this.form.get('otaPackageId').patchValue(null, {emitEvent: false});
      this.targetDeviceProfileId = null;
    });
  }

  private valueToString(value: any): string {
    return typeof value === 'object' ? JSON.stringify(value) : String(value);
  }

  private buildAttributeRow(key: string, value: string): UntypedFormGroup {
    return this.fb.group({
      key: [key, [Validators.required]],
      value: [value, [Validators.required]]
    });
  }

  get attributesArray(): UntypedFormArray {
    return this.form.get('attributes') as UntypedFormArray;
  }

  addAttribute() {
    this.attributesArray.push(this.buildAttributeRow('', ''));
    this.form.markAsDirty();
  }

  removeAttribute(index: number) {
    this.attributesArray.removeAt(index);
    this.form.markAsDirty();
  }

  get eventType(): string {
    return this.form?.get('type')?.value;
  }

  isOtaType(type: string): boolean {
    return type === SchedulerEventType.UPDATE_FIRMWARE || type === SchedulerEventType.UPDATE_SOFTWARE;
  }

  get allowedOriginatorEntityTypes(): EntityType[] {
    switch (this.eventType) {
      case SchedulerEventType.SEND_RPC_REQUEST:
        return [EntityType.DEVICE];
      case SchedulerEventType.UPDATE_FIRMWARE:
      case SchedulerEventType.UPDATE_SOFTWARE:
        return [EntityType.DEVICE, EntityType.DEVICE_PROFILE];
      default:
        return [EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.CUSTOMER];
    }
  }

  // OTA package list must be limited to the target's device profile.
  private onOriginatorChanged(originatorId: EntityId) {
    if (!originatorId || !this.isOtaType(this.eventType)) {
      this.targetDeviceProfileId = null;
      return;
    }
    if (originatorId.entityType === EntityType.DEVICE_PROFILE) {
      this.targetDeviceProfileId = originatorId.id;
    } else if (originatorId.entityType === EntityType.DEVICE) {
      this.entityService.getEntity(EntityType.DEVICE, originatorId.id, {ignoreLoading: true}).subscribe((device: any) => {
        this.targetDeviceProfileId = device?.deviceProfileId?.id || null;
      });
    }
  }

  weekDayToggled(day: number, checked: boolean) {
    const repeatOn: number[] = [...(this.form.get('repeatOn').value || [])];
    const idx = repeatOn.indexOf(day);
    if (checked && idx < 0) {
      repeatOn.push(day);
    } else if (!checked && idx >= 0) {
      repeatOn.splice(idx, 1);
    }
    this.form.get('repeatOn').patchValue(repeatOn);
    this.form.markAsDirty();
  }

  isWeekDayChecked(day: number): boolean {
    return (this.form.get('repeatOn').value || []).includes(day);
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
    if (v.type === SchedulerEventType.UPDATE_ATTRIBUTES && !this.attributesArray.length) {
      return;
    }
    if (this.isOtaType(v.type) && !v.otaPackageId) {
      this.form.get('otaPackageId').setErrors({required: true});
      this.form.get('otaPackageId').markAsTouched();
      return;
    }
    const event: SchedulerEvent = {
      ...(this.schedulerEvent || {} as SchedulerEvent),
      name: v.name,
      type: v.type,
      enabled: v.enabled,
      originatorId: v.originatorId,
      schedule: {
        timezone: v.timezone,
        startTime: v.startTime
      },
      configuration: this.buildConfiguration(v)
    };
    if (v.repeatEnabled) {
      event.schedule.repeat = this.buildRepeat(v);
    }
    this.schedulerEventService.saveSchedulerEvent(event).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

  private buildConfiguration(v: any) {
    switch (v.type) {
      case SchedulerEventType.UPDATE_ATTRIBUTES: {
        const msgBody: {[key: string]: any} = {};
        for (const row of v.attributes) {
          msgBody[row.key] = this.parseValue(row.value);
        }
        return {
          originatorId: v.originatorId,
          msgType: 'POST_ATTRIBUTES_REQUEST',
          msgBody,
          metadata: {scope: v.attributesScope}
        };
      }
      case SchedulerEventType.SEND_RPC_REQUEST: {
        let params: any = {};
        try {
          params = JSON.parse(v.rpcParams);
        } catch (e) {
          params = v.rpcParams;
        }
        return {
          originatorId: v.originatorId,
          msgType: 'RPC_CALL_FROM_SERVER_TO_DEVICE',
          msgBody: {method: v.rpcMethod, params, timeout: 60000},
          metadata: {oneway: String(v.rpcOneway), persistent: 'false'}
        };
      }
      default:
        // updateFirmware / updateSoftware: msgBody = OtaPackageId
        return {
          originatorId: v.originatorId,
          msgBody: {entityType: EntityType.OTA_PACKAGE, id: v.otaPackageId}
        };
    }
  }

  private parseValue(value: string): any {
    if (value === 'true') {
      return true;
    }
    if (value === 'false') {
      return false;
    }
    const num = Number(value);
    if (!isNaN(num) && value.trim() !== '') {
      return num;
    }
    try {
      return JSON.parse(value);
    } catch (e) {
      return value;
    }
  }

  private buildRepeat(v: any): SchedulerRepeat {
    const repeat: SchedulerRepeat = {
      type: v.repeatType,
      endsOn: v.endsOn
    };
    switch (v.repeatType) {
      case SchedulerRepeatType.WEEKLY:
        repeat.repeatOn = v.repeatOn;
        break;
      case SchedulerRepeatType.EVERY_N_DAYS:
        repeat.days = v.repeatDays;
        break;
      case SchedulerRepeatType.EVERY_N_WEEKS:
        repeat.weeks = v.repeatWeeks;
        break;
      case SchedulerRepeatType.TIMER:
        repeat.repeatInterval = v.repeatInterval;
        repeat.timeUnit = v.repeatTimeUnit;
        break;
    }
    return repeat;
  }

}
