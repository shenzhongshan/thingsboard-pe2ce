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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { User } from '@shared/models/user.model';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, isGroupAll } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { forkJoin, Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { CreateEntityGroupFunction } from '@shared/components/entity/entity-group-list.component';
import {
  EntityGroupDialogComponent,
  EntityGroupDialogData
} from '@home/pages/entity-group/entity-group-dialog.component';

export interface UserOwnerGroupsDialogData {
  user: User;
}

@Component({
    selector: 'tb-user-owner-groups-dialog',
    templateUrl: './user-owner-groups-dialog.component.html',
    standalone: false
})
export class UserOwnerGroupsDialogComponent implements OnInit {

  entityType = EntityType;

  form: UntypedFormGroup;

  createGroupFunction: CreateEntityGroupFunction;

  private initialGroupIds: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: UserOwnerGroupsDialogData,
              private dialogRef: MatDialogRef<UserOwnerGroupsDialogComponent, boolean>,
              private entityGroupService: EntityGroupService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
  }

  /** Owner is the tenant or customer the user already belongs to — shown for context only. The
   * backend validator rejects a changed tenantId/customerId/authority on update ("Can't update
   * user authority!" / "...customer id!"), so unlike the Asset/Device equivalents, this dialog
   * never offers to reassign it; only group membership is editable. */
  ngOnInit() {
    const owner: EntityId = this.data.user.customerId && this.data.user.customerId.id !== NULL_UUID ?
      this.data.user.customerId : this.data.user.tenantId;
    this.form = this.fb.group({
      owner: [{value: owner, disabled: true}],
      groupIds: [[]]
    });
    this.createGroupFunction = (groupType, groupName, ownerId) => this.createEntityGroup(groupType, groupName, ownerId);
    this.entityGroupService.getEntityGroupsForEntity(EntityType.USER, this.data.user.id.id).pipe(
      mergeMap(groups => this.entityGroupService.getEntityGroupsByIds(groups.map(g => g.id)))
    ).subscribe(groups => {
      const realGroups = groups.filter(group => !isGroupAll(group));
      this.initialGroupIds = realGroups.map(group => group.id.id);
      this.form.get('groupIds').setValue(this.initialGroupIds, {emitEvent: false});
    });
  }

  /** The group picker is scoped to the owner, so it needs the owner's EntityId. */
  ownerId(): EntityId | null {
    const owner = this.form?.get('owner')?.value as EntityId;
    return owner && owner.entityType ? owner : null;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  private createEntityGroup(groupType: EntityType, groupName: string, ownerId: EntityId): Observable<EntityInfoData | null> {
    const data: EntityGroupDialogData = {groupType, name: groupName};
    if (ownerId && ownerId.entityType === EntityType.CUSTOMER) {
      data.ownerId = ownerId.id;
    }
    return this.dialog.open<EntityGroupDialogComponent, EntityGroupDialogData, EntityGroup | false>(
      EntityGroupDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data
      }).afterClosed().pipe(
      map(created => created ? {id: created.id, name: created.name} as EntityInfoData : null)
    );
  }

  save(): void {
    const selectedGroupIds: string[] = this.form.get('groupIds').value || [];
    const toAdd = selectedGroupIds.filter(id => !this.initialGroupIds.includes(id));
    const toRemove = this.initialGroupIds.filter(id => !selectedGroupIds.includes(id));

    const ops = [
      ...toAdd.map(groupId => this.entityGroupService.addEntitiesToEntityGroup(groupId, [this.data.user.id.id])),
      ...toRemove.map(groupId => this.entityGroupService.removeEntitiesFromEntityGroup(groupId, [this.data.user.id.id]))
    ];
    (ops.length ? forkJoin(ops) : of(null)).subscribe(() => this.dialogRef.close(true));
  }

}
