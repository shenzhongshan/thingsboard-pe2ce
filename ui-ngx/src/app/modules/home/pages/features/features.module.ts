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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { FeaturesRoutingModule } from '@home/pages/features/features-routing.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { SchedulerEventsComponent } from '@home/pages/features/scheduler/scheduler-events.component';
import { SchedulerEventDialogComponent } from '@home/pages/features/scheduler/scheduler-event-dialog.component';
import {
  AttributeKeyValueTableComponent
} from '@home/pages/features/scheduler/attribute-key-value-table.component';
import { TargetSelectComponent } from '@home/pages/features/scheduler/target-select.component';
import { TaskManagerComponent } from '@home/pages/features/tasks/task-manager.component';

@NgModule({
  declarations: [
    SchedulerEventsComponent,
    SchedulerEventDialogComponent,
    AttributeKeyValueTableComponent,
    TargetSelectComponent,
    TaskManagerComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    // tb-target-select renders tb-device-profile-autocomplete, which lives in the home components module.
    HomeComponentsModule,
    FeaturesRoutingModule
  ],
  exports: [
    SchedulerEventDialogComponent
  ]
})
export class FeaturesModule { }
