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
import {
  AbstractControl,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { EntityService } from '@core/http/entity.service';
import { ReportTemplateService } from '@core/http/report-template.service';
import { NotificationService } from '@core/http/notification.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { ReportTemplateInfo } from '@shared/models/report.models';
import { NotificationTarget, NotificationTemplate, NotificationType } from '@shared/models/notification.models';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import {
  TemplateNotificationDialogComponent,
  TemplateNotificationDialogData
} from '@home/pages/notification/template/template-notification-dialog.component';
import { EntityGroup, entityGroupTypes } from '@shared/models/entity-group.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, switchMap } from 'rxjs/operators';
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

export enum TargetMode {
  SINGLE = 'SINGLE',
  GROUP = 'GROUP',
  OWNER = 'OWNER'
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

  TargetMode = TargetMode;
  entityTypeTranslations = entityTypeTranslations;

  targetDeviceProfileId: string = null;

  filteredEntityGroups: Observable<EntityGroup[]>;
  filteredOwners: Observable<EntityInfoData[]>;

  reportTemplates: ReportTemplateInfo[] = [];
  notificationTargets: NotificationTarget[] = [];
  notificationTemplates: NotificationTemplate[] = [];

  @ViewChild('tabGroup') tabGroup: MatTabGroup;

  private schedulerEvent: SchedulerEvent;

  constructor(@Inject(MAT_DIALOG_DATA) public data: SchedulerEventDialogData,
              private dialogRef: MatDialogRef<SchedulerEventDialogComponent, boolean>,
              protected store: Store<AppState>,
              private schedulerEventService: SchedulerEventService,
              private entityService: EntityService,
              private reportTemplateService: ReportTemplateService,
              private notificationService: NotificationService,
              private entityGroupService: EntityGroupService,
              private translate: TranslateService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.schedulerEventId;
  }

  ngOnInit() {
    this.reportTemplateService.getReportTemplateInfos(new PageLink(100)).subscribe((page) => {
      this.reportTemplates = page.data;
    });
    // CE reports notify via NotificationType.GENERAL (PE's dedicated REPORT_GENERATED type has no CE equivalent).
    this.notificationService.getNotificationTargets(new PageLink(100), NotificationType.GENERAL).subscribe((page) => {
      this.notificationTargets = page.data;
    });
    this.notificationService.getNotificationTemplates(new PageLink(100), NotificationType.GENERAL).subscribe((page) => {
      this.notificationTemplates = page.data;
    });
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
    const isGroupTarget = event.originatorId?.entityType === EntityType.ENTITY_GROUP;
    this.form = this.fb.group({
      name: [event.name, [Validators.required]],
      type: [event.type, [Validators.required]],
      enabled: [event.enabled],
      originatorId: [isGroupTarget ? null : event.originatorId],
      targetMode: [isGroupTarget ? TargetMode.GROUP : TargetMode.SINGLE],
      targetEntityType: [null],
      targetEntityGroup: [null],
      targetOwner: [null],
      // updateAttributes
      attributesScope: [config.metadata?.scope || AttributeScope.SERVER_SCOPE],
      attributes: this.fb.array(attributes.map((kv) => this.buildAttributeRow(kv.key, kv.value))),
      // sendRpcRequest
      rpcMethod: [event.type === SchedulerEventType.SEND_RPC_REQUEST ? config.msgBody?.method : ''],
      rpcParams: [event.type === SchedulerEventType.SEND_RPC_REQUEST && config.msgBody?.params !== undefined ?
        JSON.stringify(config.msgBody.params) : '{}'],
      rpcOneway: [config.metadata?.oneway === 'true'],
      rpcPersistent: [config.metadata?.persistent === 'true'],
      // updateFirmware / updateSoftware
      otaPackageId: [this.isOtaType(event.type) && config.msgBody?.id ? config.msgBody.id : null],
      // generateReport
      reportTemplateId: [event.type === SchedulerEventType.GENERATE_REPORT ? config.msgBody?.reportTemplateId?.id : null],
      reportUserId: [event.type === SchedulerEventType.GENERATE_REPORT && config.msgBody?.userId?.id ?
        {entityType: EntityType.USER, id: config.msgBody.userId.id} as EntityId :
        {entityType: EntityType.USER, id: getCurrentAuthUser(this.store).userId} as EntityId],
      reportTargets: [event.type === SchedulerEventType.GENERATE_REPORT ? (config.msgBody?.targets || []) : []],
      reportNotificationTemplateId: [event.type === SchedulerEventType.GENERATE_REPORT ?
        config.msgBody?.notificationTemplateId?.id : null],
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
      this.form.get('targetMode').patchValue(TargetMode.SINGLE, {emitEvent: false});
      this.form.get('targetEntityType').patchValue(null, {emitEvent: false});
      this.form.get('targetEntityGroup').patchValue(null, {emitEvent: false});
      this.form.get('targetOwner').patchValue(null, {emitEvent: false});
      this.targetDeviceProfileId = null;
    });
    this.form.get('targetMode').valueChanges.subscribe((mode) => {
      this.form.get('targetEntityGroup').patchValue(null, {emitEvent: false});
      this.form.get('targetOwner').patchValue(null, {emitEvent: false});
      // tb-entity-select's `required` directive doesn't unregister its validator when @if destroys
      // the element on mode switch, leaving originatorId permanently (invisibly) invalid — clear explicitly.
      const originatorCtrl = this.form.get('originatorId');
      if (mode !== TargetMode.SINGLE) {
        originatorCtrl.patchValue(null, {emitEvent: false});
      }
      originatorCtrl.clearValidators();
      originatorCtrl.updateValueAndValidity({emitEvent: false});
    });
    this.form.get('targetEntityType').valueChanges.subscribe(() => {
      this.form.get('targetEntityGroup').patchValue(null, {emitEvent: false});
    });
    this.setupTargetAutocompletes();
    if (isGroupTarget) {
      this.entityGroupService.getEntityGroup(event.originatorId.id, {ignoreErrors: true}).subscribe((group) => {
        this.form.get('targetEntityType').patchValue(group.type, {emitEvent: false});
        this.form.get('targetEntityGroup').patchValue(group, {emitEvent: false});
      });
    }
  }

  private setupTargetAutocompletes() {
    this.filteredEntityGroups = this.form.get('targetEntityGroup').valueChanges.pipe(
      map(value => typeof value === 'string' ? value : value?.name || ''),
      debounceTime(150),
      switchMap(name => {
        const groupType = this.form.get('targetEntityType').value;
        if (!groupType) {
          return of([]);
        }
        return this.entityGroupService.getEntityGroupsByType(groupType, new PageLink(50, 0, name), null,
          {ignoreLoading: true, ignoreErrors: true}).pipe(
          map(page => page.data),
          catchError(() => of([]))
        );
      })
    );
    this.filteredOwners = this.form.get('targetOwner').valueChanges.pipe(
      map(value => typeof value === 'string' ? value : value?.name || ''),
      debounceTime(150),
      switchMap(name => this.entityGroupService.getOwners(new PageLink(50, 0, name),
        {ignoreLoading: true, ignoreErrors: true}).pipe(
        map(page => page.data),
        catchError(() => of([]))
      ))
    );
  }

  displayEntityGroupFn(group?: EntityGroup): string | undefined {
    return group?.name;
  }

  displayOwnerFn(owner?: EntityInfoData): string | undefined {
    return owner?.name;
  }

  get groupableEntityTypes(): EntityType[] {
    return this.allowedOriginatorEntityTypes.filter(t => entityGroupTypes.includes(t));
  }

  get supportsGroupTargeting(): boolean {
    return this.eventType === SchedulerEventType.UPDATE_ATTRIBUTES || this.eventType === SchedulerEventType.SEND_RPC_REQUEST;
  }

  createNotificationTarget() {
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData, NotificationTarget>(
      RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {isAdd: true}
    }).afterClosed().subscribe((target) => {
      if (target) {
        this.notificationTargets = [...this.notificationTargets, target];
        this.form.get('reportTargets').patchValue([...(this.form.get('reportTargets').value || []), target.id.id]);
        this.form.markAsDirty();
      }
    });
  }

  createNotificationTemplate() {
    this.dialog.open<TemplateNotificationDialogComponent, TemplateNotificationDialogData, NotificationTemplate>(
      TemplateNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {isAdd: true, predefinedType: NotificationType.GENERAL}
    }).afterClosed().subscribe((template) => {
      if (template) {
        this.notificationTemplates = [...this.notificationTemplates, template];
        this.form.get('reportNotificationTemplateId').patchValue(template.id.id);
        this.form.markAsDirty();
      }
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
        // PE's Update Attributes entity-type list, minus SCHEDULER_EVENT/BLOB_ENTITY (no CE equivalent).
        return [
          EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.TENANT,
          EntityType.CUSTOMER, EntityType.DASHBOARD, EntityType.USER, EntityType.CONVERTER,
          EntityType.INTEGRATION, EntityType.REPORT_TEMPLATE, EntityType.REPORT, EntityType.ROLE
        ];
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
      const attributesRowInvalid = this.form.get('type').value === SchedulerEventType.UPDATE_ATTRIBUTES && this.attributesArray.invalid;
      if (scheduleTabInvalid) {
        this.showValidationError('scheduler.schedule-incomplete', 1);
      } else if (attributesRowInvalid) {
        this.showValidationError('scheduler.attribute-row-incomplete', 0);
      } else {
        this.showValidationError('scheduler.validation-error', 0);
      }
      return;
    }
    const v = this.form.value;
    if (v.type === SchedulerEventType.UPDATE_ATTRIBUTES && !this.attributesArray.length) {
      this.showValidationError('scheduler.attributes-required', 0);
      return;
    }
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
    if (this.supportsGroupTargeting) {
      if (v.targetMode === TargetMode.SINGLE && !v.originatorId) {
        this.form.get('originatorId').setErrors({required: true});
        this.form.get('originatorId').markAsTouched();
        this.showValidationError('scheduler.target-required', 0);
        return;
      }
      if (v.targetMode === TargetMode.GROUP && (!v.targetEntityType || !v.targetEntityGroup)) {
        this.showValidationError('scheduler.target-entity-group-required', 0);
        return;
      }
      if (v.targetMode === TargetMode.OWNER && (!v.targetEntityType || !v.targetOwner)) {
        this.showValidationError('scheduler.target-owner-required', 0);
        return;
      }
    } else if (v.type !== SchedulerEventType.GENERATE_REPORT && !v.originatorId) {
      this.form.get('originatorId').setErrors({required: true});
      this.form.get('originatorId').markAsTouched();
      this.showValidationError('scheduler.target-required', 0);
      return;
    }
    this.resolveOriginatorId(v).subscribe((originatorId) => {
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
    });
  }

  // Group/owner targeting resolves to a concrete EntityGroupId originator (PE parity: the rule chain's
  // entity-group fan-out sub-flow expands it to member entities at fire time — no backend change needed here).
  private resolveOriginatorId(v: any): Observable<EntityId> {
    if (!this.supportsGroupTargeting || v.targetMode === TargetMode.SINGLE) {
      return of(v.originatorId);
    }
    if (v.targetMode === TargetMode.GROUP) {
      const group: EntityGroup = v.targetEntityGroup;
      return of({id: group.id.id, entityType: EntityType.ENTITY_GROUP} as EntityId);
    }
    const owner: EntityInfoData = v.targetOwner;
    const ownerId = owner.id.entityType === EntityType.CUSTOMER ? owner.id.id : undefined;
    return this.entityGroupService.getAllGroupId(v.targetEntityType, ownerId).pipe(
      map(groupId => ({id: groupId, entityType: EntityType.ENTITY_GROUP} as EntityId))
    );
  }

  private buildConfiguration(v: any, originatorId: EntityId) {
    switch (v.type) {
      case SchedulerEventType.UPDATE_ATTRIBUTES: {
        const msgBody: {[key: string]: any} = {};
        for (const row of v.attributes) {
          msgBody[row.key] = this.parseValue(row.value);
        }
        return {
          originatorId,
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
          originatorId,
          msgType: 'RPC_CALL_FROM_SERVER_TO_DEVICE',
          msgBody: {method: v.rpcMethod, params, timeout: 60000},
          metadata: {oneway: String(v.rpcOneway), persistent: String(v.rpcPersistent)}
        };
      }
      case SchedulerEventType.GENERATE_REPORT:
        return {
          msgBody: {
            reportTemplateId: {id: v.reportTemplateId, entityType: EntityType.REPORT_TEMPLATE},
            userId: {id: v.reportUserId.id, entityType: EntityType.USER},
            timezone: v.timezone,
            targets: v.reportTargets || [],
            notificationTemplateId: v.reportNotificationTemplateId ?
              {id: v.reportNotificationTemplateId, entityType: EntityType.NOTIFICATION_TEMPLATE} : null
          }
        };
      default:
        // updateFirmware / updateSoftware: msgBody = OtaPackageId
        return {
          originatorId,
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
