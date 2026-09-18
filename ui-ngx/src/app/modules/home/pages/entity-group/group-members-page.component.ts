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

import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDrawer } from '@angular/material/sidenav';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityGroup, EntityGroupId } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { GroupEntityTableHeaderComponent } from '@home/pages/entity-group/group-entity-table-header.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { createGroupDetailsConfig } from '@home/pages/entity-group/entity-group-details-config.util';

@Component({
    selector: 'tb-group-members-page',
    templateUrl: './group-members-page.component.html',
    standalone: false
})
export class GroupMembersPageComponent extends PageComponent implements OnInit {

  config: EntityTableConfig<BaseData<HasId>>;
  groupDetailsConfig: EntityTableConfig<EntityGroup>;
  groupId: EntityGroupId;
  groupDetailsOpen = false;

  @ViewChild('groupDetailsPanel') groupDetailsPanel: EntityDetailsPanelComponent;
  @ViewChild('groupDrawer') groupDrawer: MatDrawer;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private entityGroupService: EntityGroupService,
              private dialogService: DialogService,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit() {
    this.config = this.route.snapshot.data.entitiesTableConfig;
    this.groupId = new EntityGroupId(this.route.snapshot.params.entityGroupId);

    this.config.headerComponent = GroupEntityTableHeaderComponent;
    this.config.componentsData = this.config.componentsData || {};
    // The plain table title ("qq: 设备") is now rendered by the header component instead, next to
    // the edit pencil that opens the group's own details drawer (matches PE's layout).
    this.config.componentsData.groupTableTitle = this.config.tableTitle;
    this.config.componentsData.openGroupDetails = () => {
      this.groupDetailsOpen = true;
    };
    this.config.tableTitle = '';

    this.groupDetailsConfig = createGroupDetailsConfig(this.entityGroupService, action => this.onGroupAction(action), false);
  }

  onGroupDetailsUpdated(group: EntityGroup) {
    // Reflect a rename from the 详情 tab back into the member table's header.
    const typeLabel = this.config.componentsData.groupTableTitle.split(': ').slice(1).join(': ');
    this.config.componentsData.groupTableTitle = typeLabel ? `${group.name}: ${typeLabel}` : group.name;
  }

  private onGroupAction(action: EntityAction<EntityGroup>): boolean {
    switch (action.action) {
      case 'open':
        this.groupDrawer?.close();
        return true;
      case 'delete':
        this.deleteGroup(action.entity);
        return true;
      case 'makePublic':
        this.togglePublic(action.entity, true);
        return true;
      case 'makePrivate':
        this.togglePublic(action.entity, false);
        return true;
      default:
        return false;
    }
  }

  private deleteGroup(group: EntityGroup) {
    this.dialogService.confirm(
      this.translate.instant('entity-group.delete-group-title', {entityGroupName: group.name}),
      this.translate.instant('entity-group.delete-group-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.entityGroupService.deleteEntityGroup(group.id.id).subscribe(() => {
          this.groupDrawer?.close();
          this.config.onBackNavigation();
        });
      }
    });
  }

  private togglePublic(group: EntityGroup, isPublic: boolean) {
    const request = isPublic
      ? this.entityGroupService.makeEntityGroupPublic(group.id.id)
      : this.entityGroupService.makeEntityGroupPrivate(group.id.id);
    request.subscribe(() => {
      this.groupDetailsPanel?.reloadEntity();
    });
  }

}
