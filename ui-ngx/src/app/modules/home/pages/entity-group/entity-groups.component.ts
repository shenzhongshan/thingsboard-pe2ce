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
  EntityGroupShareDialogComponent,
  EntityGroupShareDialogData
} from '@home/pages/entity-group/entity-group-share-dialog.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { createGroupDetailsConfig } from '@home/pages/entity-group/entity-group-details-config.util';
import {
  AssignEntityGroupToEdgeDialogComponent,
  AssignEntityGroupToEdgeDialogData
} from '@home/dialogs/assign-entity-group-to-edge-dialog.component';

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

  /**
   * Edge 模式：从 edge 详情页进入时（instances/:edgeId/deviceGroups 等），列表只显示
   * **分配给这台 edge** 的分组，动作换成"分配给 edge / 从 edge 取消分配"。
   * 新建/删除/共享/权限属于所有者层面的操作，与 edge 分配无关，edge 模式下隐藏。
   */
  edgeId: string;

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
    // edgeId 在祖先路由段上（':edgeId/deviceGroups' 的子路由是 ''），而 Angular 默认
    // 不向子路由继承 params，所以必须沿路由树往上找，不能只读当前 snapshot.params。
    this.edgeId = this.embedded ? null : this.findRouteParam('edgeId');
    this.groupDetailsConfig = createGroupDetailsConfig(this.entityGroupService, action => this.onGroupAction(action));
    this.reload();
  }

  private findRouteParam(name: string): string {
    let route = this.route.snapshot;
    while (route) {
      if (route.params?.[name]) {
        return route.params[name];
      }
      route = route.parent;
    }
    return null;
  }

  get edgeMode(): boolean {
    return !!this.edgeId;
  }

  get groupTypeTitleKey(): string {
    return 'entity-group.group-type-' + this.groupType.toLowerCase().replace(/_/g, '-');
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
    const groups$ = this.edgeMode
      ? this.entityGroupService.getEdgeEntityGroups(this.edgeId, this.groupType, pageLink)
      : this.entityGroupService.getEntityGroupsByType(this.groupType, pageLink, this.customerId);
    groups$.subscribe((pageData) => {
      this.groups = pageData.data;
      this.totalElements = pageData.totalElements;
    });
  }

  /** 把一个已有分组分配给这台 edge。候选里排除已分配的，避免重复。 */
  assignGroupToEdge($event: Event) {
    $event?.stopPropagation();
    this.dialog.open<AssignEntityGroupToEdgeDialogComponent, AssignEntityGroupToEdgeDialogData, boolean>(
      AssignEntityGroupToEdgeDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          edgeId: this.edgeId,
          groupType: this.groupType,
          excludeGroupIds: this.groups.map(g => g.id.id)
        }
      }).afterClosed().subscribe((assigned) => {
      if (assigned) {
        this.reload();
      }
    });
  }

  /** 从这台 edge 取消分配（不删除分组本身）。确认文案对齐 PE 的同名操作。 */
  unassignGroupFromEdge($event: Event, group: EntityGroup) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('edge.unassign-entity-group-from-edge-title', {entityGroupName: group.name}),
      this.translate.instant('edge.unassign-entity-group-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.entityGroupService.unassignEntityGroupFromEdge(this.edgeId, group.id.id, group.type)
          .subscribe(() => this.reload());
      }
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
        this.entityGroupService.makeEntityGroupPublic(action.entity.id.id).subscribe(() => {
          this.groupDetailsPanel?.reloadEntity();
          this.reload();
        });
        return true;
      case 'makePrivate':
        this.entityGroupService.makeEntityGroupPrivate(action.entity.id.id).subscribe(() => {
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

  shareGroup($event: Event, group: EntityGroup) {
    $event?.stopPropagation();
    this.dialog.open<EntityGroupShareDialogComponent, EntityGroupShareDialogData, boolean>(
      EntityGroupShareDialogComponent, {
        disableClose: true,
        data: { entityGroup: group }
      });
  }

  canBePublic(group: EntityGroup): boolean {
    return !isGroupAll(group) && [
      EntityType.ASSET, EntityType.DEVICE, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.DASHBOARD
    ].includes(group.type);
  }

  isPublicGroup(group: EntityGroup): boolean {
    return group?.additionalInfo?.isPublic === true;
  }

  togglePublic($event: Event, group: EntityGroup, isPublic: boolean) {
    $event?.stopPropagation();
    const request = isPublic
      ? this.entityGroupService.makeEntityGroupPublic(group.id.id)
      : this.entityGroupService.makeEntityGroupPrivate(group.id.id);
    request.subscribe(() => this.reload());
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
