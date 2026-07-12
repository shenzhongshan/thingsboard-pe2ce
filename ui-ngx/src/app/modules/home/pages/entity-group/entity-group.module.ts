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
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { EntityGroupsComponent } from '@home/pages/entity-group/entity-groups.component';
import { EntityGroupDialogComponent } from '@home/pages/entity-group/entity-group-dialog.component';
import { AddGroupEntitiesDialogComponent } from '@home/pages/entity-group/add-group-entities-dialog.component';
import { GroupPermissionsDialogComponent } from '@home/pages/entity-group/group-permissions-dialog.component';
import { RolesComponent } from '@home/pages/entity-group/roles.component';
import { RoleDialogComponent } from '@home/pages/entity-group/role-dialog.component';
import { CustomerHierarchyComponent } from '@home/pages/entity-group/customer-hierarchy.component';

@NgModule({
  declarations: [
    EntityGroupsComponent,
    EntityGroupDialogComponent,
    AddGroupEntitiesDialogComponent,
    GroupPermissionsDialogComponent,
    RolesComponent,
    RoleDialogComponent,
    CustomerHierarchyComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ],
  exports: [
    EntityGroupsComponent,
    RolesComponent,
    CustomerHierarchyComponent
  ]
})
export class EntityGroupModule { }
