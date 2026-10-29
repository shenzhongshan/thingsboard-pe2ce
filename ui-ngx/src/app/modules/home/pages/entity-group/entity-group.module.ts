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
import { GroupPermissionDialogComponent } from '@home/pages/entity-group/group-permission-dialog.component';
import { RoleComponent } from '@home/pages/entity-group/role.component';
import { CustomerHierarchyComponent } from '@home/pages/entity-group/customer-hierarchy.component';
import { GroupMembersPageComponent } from '@home/pages/entity-group/group-members-page.component';
import { GroupEntityTableHeaderComponent } from '@home/pages/entity-group/group-entity-table-header.component';
import { EntityGroupComponent } from '@home/pages/entity-group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/pages/entity-group/entity-group-tabs.component';
import { EntityGroupColumnsTabComponent } from '@home/pages/entity-group/entity-group-columns-tab.component';
import { EntityGroupSettingsTabComponent } from '@home/pages/entity-group/entity-group-settings-tab.component';
import { EntityGroupActionsTabComponent } from '@home/pages/entity-group/entity-group-actions-tab.component';
import { EntityGroupPermissionsTabComponent } from '@home/pages/entity-group/entity-group-permissions-tab.component';
import { EntityGroupRolesTabComponent } from '@home/pages/entity-group/entity-group-roles-tab.component';
import { EntityGroupPermissionDialogComponent } from '@home/pages/entity-group/entity-group-permission-dialog.component';
import { EntityGroupActionDialogComponent } from '@home/pages/entity-group/entity-group-action-dialog.component';
import { EntityGroupShareDialogComponent } from '@home/pages/entity-group/entity-group-share-dialog.component';
import { RoleViewDialogComponent } from '@home/pages/entity-group/role-view-dialog.component';

@NgModule({
  declarations: [
    EntityGroupsComponent,
    EntityGroupDialogComponent,
    AddGroupEntitiesDialogComponent,
    GroupPermissionDialogComponent,
    RoleComponent,
    CustomerHierarchyComponent,
    GroupMembersPageComponent,
    GroupEntityTableHeaderComponent,
    EntityGroupComponent,
    EntityGroupTabsComponent,
    EntityGroupColumnsTabComponent,
    EntityGroupSettingsTabComponent,
    EntityGroupActionsTabComponent,
    EntityGroupPermissionsTabComponent,
    EntityGroupRolesTabComponent,
    EntityGroupPermissionDialogComponent,
    EntityGroupActionDialogComponent,
    EntityGroupShareDialogComponent,
    RoleViewDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ],
  exports: [
    EntityGroupsComponent,
    CustomerHierarchyComponent,
    GroupMembersPageComponent
  ]
})
export class EntityGroupModule { }
