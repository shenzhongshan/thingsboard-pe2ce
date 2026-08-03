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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroup, EntityGroupId, isGroupAll } from '@shared/models/entity-group.models';
import {
  EntityGroupDialogComponent,
  EntityGroupDialogData
} from '@home/pages/entity-group/entity-group-dialog.component';
import {
  GroupPermissionsDialogComponent,
  GroupPermissionsDialogData
} from '@home/pages/entity-group/group-permissions-dialog.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { createGroupDetailsConfig } from '@home/pages/entity-group/entity-group-details-config.util';

@Component({
  selector: 'tb-entity-groups',
  templateUrl: './entity-groups.component.html',
  styleUrls: ['./entity-groups.component.scss'],
  standalone: false
})
export class EntityGroupsComponent extends PageComponent implements OnInit, OnChanges {

  // Embedded mode (customer hierarchy): type/owner come from inputs, open is emitted to the host.
  @Input() embedded = false;
  @Input('groupType') inputGroupType: EntityType;
  @Input() customerId: string;
  @Output() groupOpen = new EventEmitter<EntityGroup>();

  groupType: EntityType;
  groups: EntityGroup[] = [];
  pageSize = 10;
  page = 0;
  totalElements = 0;
  textSearch: string = null;
  searchMode = false;

  EntityType = EntityType;
  isGroupAll = isGroupAll;

  groupDetailsConfig: EntityTableConfig<EntityGroup>;
  groupDetailsOpen = false;
  editingGroupId: EntityGroupId;

  @ViewChild('groupDetailsPanel') groupDetailsPanel: EntityDetailsPanelComponent;

  constructor(protected store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private route: ActivatedRoute,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
    this.groupType = this.inputGroupType ?? this.route.snapshot.data.groupType;
    this.groupDetailsConfig = createGroupDetailsConfig(this.entityGroupService, action => this.onGroupAction(action));
    this.reload();
  }

  ngOnChanges(changes: SimpleChanges) {
    const typeChange = changes.inputGroupType;
    const customerChange = changes.customerId;
    if ((typeChange && !typeChange.firstChange) || (customerChange && !customerChange.firstChange)) {
      this.setContext(this.inputGroupType, this.customerId);
    }
  }

  setContext(groupType: EntityType, customerId: string) {
    this.groupType = groupType;
    this.customerId = customerId;
    this.page = 0;
    this.textSearch = null;
    this.searchMode = false;
    this.reload();
  }

  reload() {
    const pageLink = new PageLink(this.pageSize, this.page, this.textSearch,
      { property: 'createdTime', direction: Direction.DESC });
    this.entityGroupService.getEntityGroupsByType(this.groupType, pageLink, this.customerId).subscribe((pageData) => {
      this.groups = pageData.data;
      this.totalElements = pageData.totalElements;
    });
  }

  pageChanged(event: {pageIndex: number; pageSize: number}) {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload();
  }

  enterSearchMode() { this.searchMode = true; }

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

  addGroup() {
    this.openDialog(null);
  }

  editGroup($event: Event, group: EntityGroup) {
    $event?.stopPropagation();
    if (isGroupAll(group)) {
      return;
    }
    this.editingGroupId = group.id;
    this.groupDetailsOpen = true;
  }

  onGroupDetailsClosed() {
    this.groupDetailsOpen = false;
    this.reload();
  }

  private onGroupAction(action: EntityAction<EntityGroup>): boolean {
    switch (action.action) {
      case 'open':
        this.groupDetailsOpen = false;
        this.openGroup(null, action.entity);
        return true;
      case 'delete':
        this.deleteGroup(null, action.entity);
        this.groupDetailsOpen = false;
        return true;
      case 'makePublic':
        this.entityGroupService.makeEntityGroupPublic(action.entity.id.id, true).subscribe(() => {
          this.groupDetailsPanel?.reloadEntity();
          this.reload();
        });
        return true;
      case 'makePrivate':
        this.entityGroupService.makeEntityGroupPublic(action.entity.id.id, false).subscribe(() => {
          this.groupDetailsPanel?.reloadEntity();
          this.reload();
        });
        return true;
      default:
        return false;
    }
  }

  private openDialog(entityGroupId: string) {
    this.dialog.open<EntityGroupDialogComponent, EntityGroupDialogData, boolean>(
      EntityGroupDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { entityGroupId, groupType: this.groupType, ownerId: this.customerId }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
      }
    });
  }

  openGroup($event: Event, group: EntityGroup) {
    $event?.stopPropagation();
    if (this.embedded) {
      this.groupOpen.emit(group);
    } else {
      this.router.navigate([group.id.id], {relativeTo: this.route});
    }
  }

  openPermissions($event: Event, group: EntityGroup) {
    $event?.stopPropagation();
    this.dialog.open<GroupPermissionsDialogComponent, GroupPermissionsDialogData>(
      GroupPermissionsDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { userGroup: group }
      });
  }

  isPublic(group: EntityGroup): boolean {
    return group.additionalInfo?.isPublic === true;
  }

  togglePublic($event: Event, group: EntityGroup) {
    $event?.stopPropagation();
    const makePublic = !this.isPublic(group);
    this.dialogService.confirm(
      this.translate.instant(makePublic ? 'entity-group.make-public-title' : 'entity-group.make-private-title',
        {entityGroupName: group.name}),
      this.translate.instant(makePublic ? 'entity-group.make-public-text' : 'entity-group.make-private-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.entityGroupService.makeEntityGroupPublic(group.id.id, makePublic).subscribe(() => this.reload());
      }
    });
  }

  deleteGroup($event: Event, group: EntityGroup) {
    $event?.stopPropagation();
    if (isGroupAll(group)) {
      return;
    }
    this.dialogService.confirm(
      this.translate.instant('entity-group.delete-group-title', {entityGroupName: group.name}),
      this.translate.instant('entity-group.delete-group-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.entityGroupService.deleteEntityGroup(group.id.id).subscribe(() => this.reload());
      }
    });
  }

}
