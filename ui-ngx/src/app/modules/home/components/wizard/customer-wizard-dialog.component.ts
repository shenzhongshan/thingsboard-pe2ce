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

import { Component, Inject, Optional, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { Customer } from '@shared/models/customer.model';
import { MatStepper } from '@angular/material/stepper';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { CustomerService } from '@core/http/customer.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { CustomMenuInfo } from '@shared/models/custom-menu.models';
import { deepTrim } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { CustomerId } from '@shared/models/id/customer-id';
import {
  EntityGroupDialogComponent,
  EntityGroupDialogData
} from '@home/pages/entity-group/entity-group-dialog.component';
import { EntityGroup, isGroupAll } from '@shared/models/entity-group.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TenantId } from '@shared/models/id/tenant-id';

export interface CustomerWizardDialogData {
  // When set, the group's owner and id pre-fill the owner/groups fields and the whole form
  // renders as a single flat page (no stepper) — matches PE's "add customer from within a
  // group's member view" flow, as opposed to the two-step wizard used from customer.all.
  presetGroup?: EntityGroup;
}

@Component({
    selector: 'tb-customer-wizard',
    templateUrl: './customer-wizard-dialog.component.html',
    standalone: false
})
export class CustomerWizardDialogComponent extends DialogComponent<CustomerWizardDialogComponent, Customer> {

  @ViewChild('addCustomerWizardStepper') stepper: MatStepper;

  entityType = EntityType;

  flatMode: boolean;

  detailsFormGroup: FormGroup;
  ownerGroupsFormGroup: FormGroup;

  customMenus: CustomMenuInfo[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<CustomerWizardDialogComponent, Customer>,
              private customerService: CustomerService,
              private entityGroupService: EntityGroupService,
              private customMenuService: CustomMenuService,
              private dialog: MatDialog,
              private fb: FormBuilder,
              @Optional() @Inject(MAT_DIALOG_DATA) private data: CustomerWizardDialogData | null) {
    super(store, router, dialogRef);

    this.flatMode = !!this.data?.presetGroup;

    this.detailsFormGroup = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255)]],
      description: [''],
      allowWhiteLabeling: [true],
      customMenuId: [null],
      country: [''],
      city: [''],
      state: [''],
      zip: [''],
      address: [''],
      address2: [''],
      phone: [''],
      email: ['', Validators.email]
    });

    // A group's ownerId is null for plain tenant-owned groups (only set for customer-owned
    // sub-groups) — default to the current tenant so the owner field pre-fills the same way
    // PE's does, instead of showing blank.
    const presetOwner: EntityId = (this.data?.presetGroup?.ownerId as EntityId)
      ?? (this.data?.presetGroup ? new TenantId(getCurrentAuthUser(this.store).tenantId) : null);
    this.ownerGroupsFormGroup = this.fb.group({
      parentCustomerId: [presetOwner],
      // "All" groups have automatic membership — nothing to explicitly add to, so only preset
      // a real, named group.
      groupIds: [this.data?.presetGroup && !isGroupAll(this.data.presetGroup) ? [this.data.presetGroup.id.id] : []]
    });

    this.customMenuService.getCustomMenuInfos(new PageLink(100, 0)).subscribe(page => this.customMenus = page.data);
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
            const groupIds: string[] = this.ownerGroupsFormGroup.get('groupIds').value || [];
            this.ownerGroupsFormGroup.get('groupIds').setValue([...groupIds, page.data[0].id.id]);
          }
        });
      }
    });
  }

  /** "所有者" allows picking either the tenant itself (= no parent customer) or another
   * customer (= sub-customer); tb-entity-select emits a generic EntityId, so a TENANT
   * selection (or nothing selected) converts to null here. */
  private ownerAsParentCustomerId(): CustomerId | null {
    const owner = this.ownerGroupsFormGroup.get('parentCustomerId').value as EntityId;
    return owner && owner.entityType === EntityType.CUSTOMER ? new CustomerId(owner.id) : null;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.stepper.previous();
  }

  nextStep(): void {
    if (this.detailsFormGroup.invalid) {
      this.detailsFormGroup.markAllAsTouched();
      return;
    }
    this.stepper.next();
  }

  add(): void {
    if (this.detailsFormGroup.invalid) {
      this.detailsFormGroup.markAllAsTouched();
      if (this.stepper) {
        this.stepper.selectedIndex = 0;
      }
      return;
    }
    const v = this.detailsFormGroup.getRawValue();
    const customer: Customer = {
      tenantId: null,
      title: v.title,
      country: v.country,
      city: v.city,
      state: v.state,
      zip: v.zip,
      address: v.address,
      address2: v.address2,
      phone: v.phone,
      email: v.email,
      customMenuId: v.customMenuId,
      parentCustomerId: this.ownerAsParentCustomerId(),
      additionalInfo: {
        description: v.description,
        allowWhiteLabeling: v.allowWhiteLabeling
      }
    };
    this.customerService.saveCustomer(deepTrim(customer)).pipe(
      mergeMap(saved => {
        const groupIds: string[] = this.ownerGroupsFormGroup.get('groupIds').value || [];
        if (!groupIds.length) {
          return of(saved);
        }
        return forkJoin(groupIds.map(groupId => this.entityGroupService.addEntitiesToEntityGroup(groupId, [saved.id.id])))
          .pipe(map(() => saved));
      })
    ).subscribe(saved => this.dialogRef.close(saved));
  }

}
