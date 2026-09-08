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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AssetInfo } from '@shared/models/asset.models';
import { AssetService } from '@core/http/asset.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, isGroupAll } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { forkJoin, Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { CreateEntityGroupFunction } from '@shared/components/entity/entity-group-list.component';
import {
  EntityGroupDialogComponent,
  EntityGroupDialogData
} from '@home/pages/entity-group/entity-group-dialog.component';

export interface AssetOwnerGroupsDialogData {
  asset: AssetInfo;
}

@Component({
    selector: 'tb-asset-owner-groups-dialog',
    templateUrl: './asset-owner-groups-dialog.component.html',
    standalone: false
})
export class AssetOwnerGroupsDialogComponent implements OnInit {

  entityType = EntityType;

  form: UntypedFormGroup;

  createGroupFunction: CreateEntityGroupFunction;

  private initialGroupIds: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: AssetOwnerGroupsDialogData,
              protected store: Store<AppState>,
              private dialogRef: MatDialogRef<AssetOwnerGroupsDialogComponent, boolean>,
              private assetService: AssetService,
              private entityGroupService: EntityGroupService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
  }

  ngOnInit() {
    const owner: EntityId = this.data.asset.customerId && this.data.asset.customerId.id !== NULL_UUID ?
      this.data.asset.customerId : null;
    this.form = this.fb.group({
      owner: [owner],
      groupIds: [[]]
    });
    this.createGroupFunction = (groupType, groupName, ownerId) => this.createEntityGroup(groupType, groupName, ownerId);
    this.entityGroupService.getEntityGroupsForEntity(EntityType.ASSET, this.data.asset.id.id).pipe(
      mergeMap(groups => this.entityGroupService.getEntityGroupsByIds(groups.map(g => g.id)))
    ).subscribe(groups => {
      const realGroups = groups.filter(group => !isGroupAll(group));
      this.initialGroupIds = realGroups.map(group => group.id.id);
      this.form.get('groupIds').setValue(this.initialGroupIds, {emitEvent: false});
    });
  }

  /** The group picker is scoped to the selected owner, so it needs the owner's EntityId (or null
   * when the asset is tenant-owned / unassigned). */
  ownerId(): EntityId | null {
    const owner = this.form?.get('owner')?.value as EntityId;
    return owner && owner.entityType ? owner : null;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  /** "所有者" allows picking either the tenant itself (= unassigned / no customer) or a customer;
   * tb-entity-select emits a generic EntityId, so a TENANT selection (or nothing selected) converts
   * to null here. */
  private ownerAsCustomerId(): CustomerId | null {
    const owner = this.form.get('owner').value as EntityId;
    return owner && owner.entityType === EntityType.CUSTOMER ? new CustomerId(owner.id) : null;
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
    const targetCustomerId = this.ownerAsCustomerId();
    const currentCustomerId = this.data.asset.customerId && this.data.asset.customerId.id !== NULL_UUID ?
      this.data.asset.customerId : null;

    const ownerChanged = (targetCustomerId?.id ?? null) !== (currentCustomerId?.id ?? null);
    let ownerChange$: Observable<any> = of(null);
    if (ownerChanged) {
      ownerChange$ = targetCustomerId ?
        this.assetService.assignAssetToCustomer(targetCustomerId.id, this.data.asset.id.id) :
        this.assetService.unassignAssetFromCustomer(this.data.asset.id.id);
    }

    const selectedGroupIds: string[] = this.form.get('groupIds').value || [];
    const toAdd = selectedGroupIds.filter(id => !this.initialGroupIds.includes(id));
    const toRemove = this.initialGroupIds.filter(id => !selectedGroupIds.includes(id));

    ownerChange$.pipe(
      mergeMap(() => {
        const ops = [
          ...toAdd.map(groupId => this.entityGroupService.addEntitiesToEntityGroup(groupId, [this.data.asset.id.id])),
          ...toRemove.map(groupId => this.entityGroupService.removeEntitiesFromEntityGroup(groupId, [this.data.asset.id.id]))
        ];
        return ops.length ? forkJoin(ops) : of(null);
      })
    ).subscribe(() => this.dialogRef.close(true));
  }

}
