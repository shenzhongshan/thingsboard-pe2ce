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

import { Component, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { MatIconButton } from '@angular/material/button';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import {
  SchedulerEventWithCustomerInfo,
  schedulerEventTypeNames
} from '@shared/models/scheduler-event.models';
import {
  SchedulerEventDialogComponent,
  SchedulerEventDialogData
} from '@home/pages/features/scheduler/scheduler-event-dialog.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { VersionControlComponent } from '@home/components/vc/version-control.component';

interface CalendarCell {
  date: Date;
  day: number;
  otherMonth: boolean;
  isToday: boolean;
  events: CalendarEventChip[];
}

interface CalendarEventChip {
  event: SchedulerEventWithCustomerInfo;
  time: string;
}

@Component({
  selector: 'tb-scheduler-events',
  templateUrl: './scheduler-events.component.html',
  styleUrls: ['./scheduler-events.component.scss'],
  standalone: false
})
export class SchedulerEventsComponent extends PageComponent implements OnInit {

  mode: 'list' | 'calendar' = 'list';

  // list state
  events: SchedulerEventWithCustomerInfo[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  // calendar state
  calendarDate = new Date();
  calendarWeeks: CalendarCell[][] = [];
  weekDays: string[] = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  schedulerEventTypeNames = schedulerEventTypeNames;

  constructor(protected store: Store<AppState>,
              private schedulerEventService: SchedulerEventService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
    super(store);
  }

  ngOnInit() {
    this.reload();
  }

  setMode(mode: 'list' | 'calendar') {
    if (this.mode !== mode) {
      this.mode = mode;
      this.reload();
    }
  }

  reload() {
    if (this.mode === 'list') {
      this.loadList();
    } else {
      this.loadCalendar();
    }
  }

  enterSearchMode() {
    this.searchMode = true;
  }

  exitSearchMode() {
    this.searchMode = false;
    if (this.textSearch) {
      this.textSearch = null;
      this.reload();
    }
  }

  searchChanged() {
    this.page = 0;
    this.reload();
  }

  private loadList() {
    const pageLink = new PageLink(this.pageSize, this.page, this.textSearch,
      { property: 'createdTime', direction: Direction.DESC });
    this.schedulerEventService.getSchedulerEvents(pageLink).subscribe((pageData) => {
      this.events = pageData.data;
      this.totalElements = pageData.totalElements;
    });
  }

  pageChanged(event: {pageIndex: number; pageSize: number}) {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadList();
  }

  eventTypeName(event: SchedulerEventWithCustomerInfo): string {
    return schedulerEventTypeNames.get(event.type) || event.type;
  }

  scheduleText(event: SchedulerEventWithCustomerInfo): string {
    const start = new Date(event.schedule.startTime);
    let text = start.toLocaleString(undefined,
      {year: 'numeric', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit'});
    if (event.schedule.repeat) {
      text += ' (' + this.translate.instant('scheduler.repeat') + ')';
    }
    return text;
  }

  addEvent() {
    this.openDialog(null);
  }

  editEvent($event: Event, event: SchedulerEventWithCustomerInfo) {
    $event?.stopPropagation();
    this.openDialog(event.id.id);
  }

  private openDialog(schedulerEventId: string) {
    this.dialog.open<SchedulerEventDialogComponent, SchedulerEventDialogData, boolean>(
      SchedulerEventDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { schedulerEventId }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  toggleEnabled($event: Event, event: SchedulerEventWithCustomerInfo, enabled: boolean) {
    $event?.stopPropagation();
    this.schedulerEventService.enableSchedulerEvent(event.id.id, enabled).subscribe(() => {
      event.enabled = enabled;
    });
  }

  toggleVersionControl($event: Event, event: SchedulerEventWithCustomerInfo, versionControlButton: MatIconButton) {
    $event?.stopPropagation();
    const trigger = versionControlButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
      return;
    }
    const versionControlPopover = this.popoverService.displayPopover({
      trigger,
      renderer: this.renderer,
      hostView: this.viewContainerRef,
      componentType: VersionControlComponent,
      preferredPlacement: 'leftTop',
      hideOnClickOutside: true,
      context: {
        detailsMode: true,
        active: true,
        singleEntityMode: true,
        externalEntityId: event.externalId || event.id,
        entityId: event.id,
        entityName: event.name
      },
      isModal: true
    });
    versionControlPopover.tbComponentRef.instance.popoverComponent = versionControlPopover;
    versionControlPopover.tbComponentRef.instance.versionRestored.subscribe(() => {
      this.reload();
    });
  }

  deleteEvent($event: Event, event: SchedulerEventWithCustomerInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('scheduler.delete-scheduler-event-title', {schedulerEventName: event.name}),
      this.translate.instant('scheduler.delete-scheduler-event-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.schedulerEventService.deleteSchedulerEvent(event.id.id).subscribe(() => {
          this.reload();
        });
      }
    });
  }

  // --- calendar (month view) ---

  get calendarTitle(): string {
    return this.calendarDate.toLocaleString(undefined, {month: 'long', year: 'numeric'});
  }

  calendarToday() {
    this.calendarDate = new Date();
    this.loadCalendar();
  }

  calendarPrev() {
    this.calendarDate = new Date(this.calendarDate.getFullYear(), this.calendarDate.getMonth() - 1, 1);
    this.loadCalendar();
  }

  calendarNext() {
    this.calendarDate = new Date(this.calendarDate.getFullYear(), this.calendarDate.getMonth() + 1, 1);
    this.loadCalendar();
  }

  private loadCalendar() {
    const year = this.calendarDate.getFullYear();
    const month = this.calendarDate.getMonth();
    const firstOfMonth = new Date(year, month, 1);
    const gridStart = new Date(year, month, 1 - firstOfMonth.getDay());
    const lastOfMonth = new Date(year, month + 1, 0);
    const gridEnd = new Date(year, month + 1, 0 + (6 - lastOfMonth.getDay()));
    gridEnd.setHours(23, 59, 59, 999);
    this.schedulerEventService.getSchedulerEventsByTime(gridStart.getTime(), gridEnd.getTime())
      .subscribe((events) => {
        this.buildCalendar(gridStart, gridEnd, month, events);
      });
  }

  private buildCalendar(gridStart: Date, gridEnd: Date, month: number, events: SchedulerEventWithCustomerInfo[]) {
    const eventsByDay = new Map<string, CalendarEventChip[]>();
    for (const event of events) {
      const timestamps = event.timestamps?.length ? event.timestamps : [event.schedule.startTime];
      for (const ts of timestamps) {
        if (ts < gridStart.getTime() || ts > gridEnd.getTime()) {
          continue;
        }
        const d = new Date(ts);
        const key = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
        const chips = eventsByDay.get(key) || [];
        chips.push({
          event,
          time: d.toLocaleTimeString(undefined, {hour: 'numeric', minute: '2-digit'})
        });
        eventsByDay.set(key, chips);
      }
    }
    const today = new Date();
    const weeks: CalendarCell[][] = [];
    const cursor = new Date(gridStart);
    while (cursor <= gridEnd) {
      const week: CalendarCell[] = [];
      for (let i = 0; i < 7; i++) {
        const key = `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`;
        week.push({
          date: new Date(cursor),
          day: cursor.getDate(),
          otherMonth: cursor.getMonth() !== month,
          isToday: cursor.getFullYear() === today.getFullYear()
            && cursor.getMonth() === today.getMonth() && cursor.getDate() === today.getDate(),
          events: eventsByDay.get(key) || []
        });
        cursor.setDate(cursor.getDate() + 1);
      }
      weeks.push(week);
    }
    this.calendarWeeks = weeks;
  }

}
