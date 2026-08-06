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
import { CustomerInfo } from '@shared/models/customer.model';
import { CustomerService } from '@core/http/customer.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { isGroupAll } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { PageLink } from '@shared/models/page/page-link';
import { forkJoin, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { CustomerId } from '@shared/models/id/customer-id';
import {
  EntityGroupDialogComponent,
  EntityGroupDialogData
} from '@home/pages/entity-group/entity-group-dialog.component';

export interface CustomerOwnerGroupsDialogData {
  customer: CustomerInfo;
}

@Component({
    selector: 'tb-customer-owner-groups-dialog',
    templateUrl: './customer-owner-groups-dialog.component.html',
    standalone: false
})
export class CustomerOwnerGroupsDialogComponent implements OnInit {

  entityType = EntityType;

  form: UntypedFormGroup;

  private initialGroupIds: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: CustomerOwnerGroupsDialogData,
              protected store: Store<AppState>,
              private dialogRef: MatDialogRef<CustomerOwnerGroupsDialogComponent, boolean>,
              private customerService: CustomerService,
              private entityGroupService: EntityGroupService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
  }

  ngOnInit() {
    const owner: EntityId = this.data.customer.parentCustomerId?.id ?
      this.data.customer.parentCustomerId : null;
    this.form = this.fb.group({
      parentCustomerId: [owner],
      groupIds: [[]]
    });
    this.entityGroupService.getEntityGroupsForEntity(EntityType.CUSTOMER, this.data.customer.id.id).pipe(
      mergeMap(groups => this.entityGroupService.getEntityGroupsByIds(groups.map(g => g.id)))
    ).subscribe(groups => {
      const realGroups = groups.filter(group => !isGroupAll(group));
      this.initialGroupIds = realGroups.map(group => group.id.id);
      this.form.get('groupIds').setValue(this.initialGroupIds, {emitEvent: false});
    });
  }

  createNewGroup($event: string): void {
    this.dialog.open<EntityGroupDialogComponent, EntityGroupDialogData, boolean>(EntityGroupDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog'],
      data: {groupType: EntityType.CUSTOMER}
    }).afterClosed().subscribe(created => {
      if (created) {
        this.entityGroupService.getEntityGroupsByType(EntityType.CUSTOMER, new PageLink(1, 0, $event)).subscribe(page => {
          if (page.data.length) {
            const groupIds: string[] = this.form.get('groupIds').value || [];
            this.form.get('groupIds').setValue([...groupIds, page.data[0].id.id]);
          }
        });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  /** "所有者" allows picking either the tenant itself (= no parent customer) or another
   * customer (= sub-customer); tb-entity-select emits a generic EntityId, so a TENANT
   * selection (or nothing selected) converts to null here. */
  private ownerAsParentCustomerId(): CustomerId | null {
    const owner = this.form.get('parentCustomerId').value as EntityId;
    return owner && owner.entityType === EntityType.CUSTOMER ? new CustomerId(owner.id) : null;
  }

  save(): void {
    const customer: CustomerInfo = {
      ...this.data.customer,
      parentCustomerId: this.ownerAsParentCustomerId()
    };
    const selectedGroupIds: string[] = this.form.get('groupIds').value || [];
    const toAdd = selectedGroupIds.filter(id => !this.initialGroupIds.includes(id));
    const toRemove = this.initialGroupIds.filter(id => !selectedGroupIds.includes(id));
    this.customerService.saveCustomer(customer).pipe(
      mergeMap(() => {
        const ops = [
          ...toAdd.map(groupId => this.entityGroupService.addEntitiesToEntityGroup(groupId, [this.data.customer.id.id])),
          ...toRemove.map(groupId => this.entityGroupService.removeEntitiesFromEntityGroup(groupId, [this.data.customer.id.id]))
        ];
        return ops.length ? forkJoin(ops) : of(null);
      })
    ).subscribe(() => this.dialogRef.close(true));
  }

}
