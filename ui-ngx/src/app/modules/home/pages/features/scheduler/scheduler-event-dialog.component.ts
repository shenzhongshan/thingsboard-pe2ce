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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatTabGroup } from '@angular/material/tabs';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { EntityService } from '@core/http/entity.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { ReportTemplateType } from '@shared/models/report.models';
import { NotificationTarget, NotificationType } from '@shared/models/notification.models';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { EntityGroup } from '@shared/models/entity-group.models';
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
import { attributeKeyValueValidator } from '@home/pages/features/scheduler/attribute-key-value-table.component';

export interface SchedulerEventDialogData {
  schedulerEventId?: string;
}

/** PE's default RPC metadata — `timeout` feeds the backend's `expirationTime` for persistent RPCs. */
const DEFAULT_RPC_TIMEOUT = 5000;

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

  /** Type of the group currently selected in the target picker — drives the attribute-scope select. */
  currentGroupType: EntityType = null;

  /** OTA package list is limited to the target's device profile, or to the target device group. */
  targetDeviceProfileId: string = null;
  targetDeviceGroupId: string = null;
  targetDeviceGroupAll = false;

  ReportTemplateType = ReportTemplateType;
  NotificationType = NotificationType;

  @ViewChild('tabGroup') tabGroup: MatTabGroup;

  private schedulerEvent: SchedulerEvent;

  constructor(@Inject(MAT_DIALOG_DATA) public data: SchedulerEventDialogData,
              private dialogRef: MatDialogRef<SchedulerEventDialogComponent, boolean>,
              protected store: Store<AppState>,
              private schedulerEventService: SchedulerEventService,
              private entityService: EntityService,
              private entityGroupService: EntityGroupService,
              private translate: TranslateService,
              private dialog: MatDialog,
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
    const isUpdateAttributes = event.type === SchedulerEventType.UPDATE_ATTRIBUTES;
    const scope = config.metadata?.scope || AttributeScope.SERVER_SCOPE;
    const attributes = isUpdateAttributes && config.msgBody ? config.msgBody : {};
    this.form = this.fb.group({
      name: [event.name, [Validators.required]],
      type: [event.type, [Validators.required]],
      enabled: [event.enabled],
      originatorId: [event.originatorId],
      // updateAttributes — PE keeps one map per scope so switching scope doesn't carry values over.
      attributesScope: [scope],
      serverAttributes: [scope === AttributeScope.SERVER_SCOPE ? attributes : {}],
      sharedAttributes: [scope === AttributeScope.SHARED_SCOPE ? attributes : {}],
      // sendRpcRequest
      rpcMethod: [event.type === SchedulerEventType.SEND_RPC_REQUEST ? config.msgBody?.method : null],
      rpcParams: [event.type === SchedulerEventType.SEND_RPC_REQUEST && config.msgBody?.params !== undefined ?
        config.msgBody.params : {}],
      rpcTimeout: [this.toNumber(config.metadata?.timeout, DEFAULT_RPC_TIMEOUT)],
      rpcOneway: [this.toBoolean(config.metadata?.oneway, true)],
      rpcPersistent: [this.toBoolean(config.metadata?.persistent, false)],
      // updateFirmware / updateSoftware
      otaPackageId: [this.isOtaType(event.type) && config.msgBody?.id ? config.msgBody : null],
      // generateReport
      reportTemplateId: [event.type === SchedulerEventType.GENERATE_REPORT ? config.msgBody?.reportTemplateId?.id : null],
      reportUserId: [event.type === SchedulerEventType.GENERATE_REPORT && config.msgBody?.userId?.id ?
        {entityType: EntityType.USER, id: config.msgBody.userId.id} as EntityId :
        {entityType: EntityType.USER, id: getCurrentAuthUser(this.store).userId} as EntityId],
      reportTargets: [event.type === SchedulerEventType.GENERATE_REPORT ? (config.msgBody?.targets || []) : []],
      // tb-template-autocomplete's model is a full EntityId, not a bare id string.
      reportNotificationTemplateId: [event.type === SchedulerEventType.GENERATE_REPORT ?
        (config.msgBody?.notificationTemplateId || null) : null],
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
    this.updateConditionalValidators();
    this.onOriginatorChanged(event.originatorId);
    this.form.get('originatorId').valueChanges.subscribe((originatorId) => this.onOriginatorChanged(originatorId));
    this.form.get('type').valueChanges.subscribe(() => {
      this.form.patchValue({
        originatorId: null,
        otaPackageId: null,
        serverAttributes: {},
        sharedAttributes: {},
        attributesScope: AttributeScope.SERVER_SCOPE
      }, {emitEvent: false});
      this.currentGroupType = null;
      this.targetDeviceProfileId = null;
      this.targetDeviceGroupId = null;
      this.targetDeviceGroupAll = false;
      this.updateConditionalValidators();
    });
    // PE clears both maps on scope change so values never carry across scopes.
    this.form.get('attributesScope').valueChanges.subscribe(() => {
      this.form.patchValue({serverAttributes: {}, sharedAttributes: {}}, {emitEvent: false});
      this.updateConditionalValidators();
    });
  }

  onCurrentGroupType(groupType: EntityType) {
    this.currentGroupType = groupType;
    if (!this.sharedScopeSupported && this.form.get('attributesScope').value !== AttributeScope.SERVER_SCOPE) {
      this.form.get('attributesScope').patchValue(AttributeScope.SERVER_SCOPE);
    }
  }

  /** Shared attributes only exist on devices — PE hides the scope select for every other target. */
  get sharedScopeSupported(): boolean {
    const originatorId: EntityId = this.form?.get('originatorId')?.value;
    if (!originatorId) {
      return false;
    }
    return originatorId.entityType === EntityType.DEVICE ||
      (originatorId.entityType === EntityType.ENTITY_GROUP && this.currentGroupType === EntityType.DEVICE);
  }

  get targetRequired(): boolean {
    return this.eventType && this.eventType !== SchedulerEventType.GENERATE_REPORT;
  }

  createNotificationTarget() {
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData, NotificationTarget>(
      RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {isAdd: true}
    }).afterClosed().subscribe((target) => {
      if (target) {
        this.form.get('reportTargets').patchValue([...(this.form.get('reportTargets').value || []), target.id.id]);
        this.form.markAsDirty();
      }
    });
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
      case SchedulerEventType.UPDATE_FIRMWARE:
      case SchedulerEventType.UPDATE_SOFTWARE:
        return [EntityType.DEVICE];
      default:
        // PE's Update Attributes entity-type list, minus SCHEDULER_EVENT/BLOB_ENTITY (no CE equivalent).
        return [
          EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.TENANT,
          EntityType.CUSTOMER, EntityType.DASHBOARD, EntityType.USER, EntityType.CONVERTER,
          EntityType.INTEGRATION, EntityType.REPORT_TEMPLATE, EntityType.REPORT, EntityType.ROLE
        ];
    }
  }

  /**
   * Rebuilds the validators of every type-specific control from scratch.
   *
   * This must run on each event-type change: Angular does NOT strip validators contributed by a
   * directive (`required`, or a component providing NG_VALIDATORS such as `tb-json-object-edit`)
   * when a structural block destroys its host, so a control left behind by the previous event type
   * would otherwise stay invalid forever — an invisible, un-saveable form. `setValidators` replaces
   * the whole composed validator, and any control still on screen has its directive re-register
   * right after this runs.
   */
  private updateConditionalValidators() {
    const type = this.eventType;
    const isReport = type === SchedulerEventType.GENERATE_REPORT;
    const isRpc = type === SchedulerEventType.SEND_RPC_REQUEST;
    const isAttributes = type === SchedulerEventType.UPDATE_ATTRIBUTES;
    const scope = this.form.get('attributesScope').value;
    const apply = (name: string, validators: any[]) => {
      const control = this.form.get(name);
      control.setValidators(validators);
      control.updateValueAndValidity({emitEvent: false});
    };
    apply('originatorId', this.targetRequired ? [Validators.required] : []);
    apply('rpcMethod', isRpc ? [Validators.required, Validators.pattern(/^\S+$/)] : []);
    apply('rpcTimeout', isRpc ? [Validators.min(0)] : []);
    apply('rpcParams', []);
    apply('otaPackageId', []);
    apply('reportTemplateId', isReport ? [Validators.required] : []);
    apply('reportUserId', isReport ? [Validators.required] : []);
    apply('serverAttributes', [attributeKeyValueValidator(isAttributes && scope === AttributeScope.SERVER_SCOPE)]);
    apply('sharedAttributes', [attributeKeyValueValidator(isAttributes && scope === AttributeScope.SHARED_SCOPE)]);
  }

  /** Narrow the OTA package list to whatever the target implies: a profile, or a device group. */
  private onOriginatorChanged(originatorId: EntityId) {
    this.targetDeviceProfileId = null;
    this.targetDeviceGroupId = null;
    this.targetDeviceGroupAll = false;
    if (!originatorId || !this.isOtaType(this.eventType)) {
      return;
    }
    if (originatorId.entityType === EntityType.DEVICE_PROFILE) {
      this.targetDeviceProfileId = originatorId.id;
    } else if (originatorId.entityType === EntityType.ENTITY_GROUP) {
      this.targetDeviceGroupId = originatorId.id;
      this.entityGroupService.getEntityGroup(originatorId.id, {ignoreLoading: true, ignoreErrors: true})
        .subscribe((group: EntityGroup) => {
          this.targetDeviceGroupAll = group?.name === 'All';
        });
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

  private showValidationError(translationKey: string, tabIndex: number) {
    if (this.tabGroup) {
      this.tabGroup.selectedIndex = tabIndex;
    }
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(translationKey),
      type: 'error',
      duration: 3000
    }));
  }

  save(): void {
    if (!this.form.valid) {
      this.form.markAllAsTouched();
      const scheduleTabInvalid = this.form.get('timezone').invalid || this.form.get('startTime').invalid;
      if (scheduleTabInvalid) {
        this.showValidationError('scheduler.schedule-incomplete', 1);
      } else if (this.form.get('originatorId').invalid) {
        this.showValidationError('scheduler.target-required', 0);
      } else if (this.attributesInvalid) {
        this.showValidationError(this.attributesMissing ? 'scheduler.attributes-required' :
          'scheduler.attribute-row-incomplete', 0);
      } else {
        this.showValidationError('scheduler.validation-error', 0);
      }
      return;
    }
    const v = this.form.value;
    if (this.isOtaType(v.type) && !v.otaPackageId) {
      this.form.get('otaPackageId').setErrors({required: true});
      this.form.get('otaPackageId').markAsTouched();
      this.showValidationError('scheduler.ota-package-required', 0);
      return;
    }
    if (v.type === SchedulerEventType.GENERATE_REPORT && !v.reportTemplateId) {
      this.form.get('reportTemplateId').setErrors({required: true});
      this.form.get('reportTemplateId').markAsTouched();
      this.showValidationError('report.report-template-required', 0);
      return;
    }
    if (v.type === SchedulerEventType.GENERATE_REPORT && !v.reportUserId?.id) {
      this.form.get('reportUserId').setErrors({required: true});
      this.form.get('reportUserId').markAsTouched();
      this.showValidationError('scheduler.report-user-required', 0);
      return;
    }
    const originatorId: EntityId = v.type === SchedulerEventType.GENERATE_REPORT ? null : v.originatorId;
    const event: SchedulerEvent = {
      ...(this.schedulerEvent || {} as SchedulerEvent),
      name: v.name,
      type: v.type,
      enabled: v.enabled,
      originatorId,
      schedule: {
        timezone: v.timezone,
        startTime: v.startTime
      },
      configuration: this.buildConfiguration(v, originatorId)
    };
    if (v.repeatEnabled) {
      event.schedule.repeat = this.buildRepeat(v);
    }
    this.schedulerEventService.saveSchedulerEvent(event).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

  private get attributesInvalid(): boolean {
    return this.eventType === SchedulerEventType.UPDATE_ATTRIBUTES &&
      (this.form.get('serverAttributes').invalid || this.form.get('sharedAttributes').invalid);
  }

  private get attributesMissing(): boolean {
    return !!(this.form.get('serverAttributes').hasError('required') ||
      this.form.get('sharedAttributes').hasError('required'));
  }

  private buildConfiguration(v: any, originatorId: EntityId) {
    switch (v.type) {
      case SchedulerEventType.UPDATE_ATTRIBUTES:
        return {
          originatorId,
          msgType: 'POST_ATTRIBUTES_REQUEST',
          msgBody: v.attributesScope === AttributeScope.SHARED_SCOPE ? v.sharedAttributes : v.serverAttributes,
          metadata: {scope: v.attributesScope}
        };
      case SchedulerEventType.SEND_RPC_REQUEST:
        return {
          originatorId,
          msgType: 'RPC_CALL_FROM_SERVER_TO_DEVICE',
          msgBody: {method: v.rpcMethod, params: v.rpcParams},
          metadata: {oneway: v.rpcOneway, timeout: v.rpcTimeout, persistent: v.rpcPersistent}
        };
      case SchedulerEventType.GENERATE_REPORT:
        return {
          msgBody: {
            reportTemplateId: {id: v.reportTemplateId, entityType: EntityType.REPORT_TEMPLATE},
            userId: {id: v.reportUserId.id, entityType: EntityType.USER},
            timezone: v.timezone,
            targets: v.reportTargets || [],
            notificationTemplateId: v.reportNotificationTemplateId || null
          }
        };
      default:
        // updateFirmware / updateSoftware: msgBody = OtaPackageId
        return {
          originatorId,
          msgType: v.type === SchedulerEventType.UPDATE_FIRMWARE ? 'FIRMWARE_UPDATED' : 'SOFTWARE_UPDATED',
          msgBody: v.otaPackageId
        };
    }
  }

  private toBoolean(value: any, defaultValue: boolean): boolean {
    if (value === undefined || value === null) {
      return defaultValue;
    }
    return typeof value === 'boolean' ? value : value === 'true';
  }

  private toNumber(value: any, defaultValue: number): number {
    const num = Number(value);
    return value === undefined || value === null || isNaN(num) ? defaultValue : num;
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
