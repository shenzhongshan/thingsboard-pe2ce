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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { UserComponent } from '@modules/home/pages/user/user.component';
import { Authority } from '@shared/models/authority.enum';
import { ActivationLinkInfo, ActivationMethod, activationMethodTranslations, User } from '@shared/models/user.model';
import { CustomerId } from '@shared/models/id/customer-id';
import { UserService } from '@core/http/user.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { forkJoin, Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@modules/home/pages/user/activation-link-dialog.component';
import { TenantId } from '@app/shared/models/id/tenant-id';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { MediaBreakpoints } from '@shared/models/constants';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { OwnerAndGroups } from '@home/components/entity/owner-and-groups.component';

export interface AddUserDialogData {
  tenantId: string;
  /** Initial owner's customer id, or NULL_UUID for a tenant-level (TENANT_ADMIN) user. Step 2 lets
   * the operator change this before saving — authority is derived from whichever owner is picked,
   * mirroring PE's add-user wizard. */
  customerId: string;
}

/**
 * Two-step wizard, PE parity: ① 用户详情 → ② 所有者和分组 (optional). A SYS_ADMIN adding a tenant
 * admin has no owner to choose (it is always that tenant), so for them step 2 is dropped entirely
 * and the dialog collapses to a plain form — same as PE's `no-stepper` mode.
 */
@Component({
    selector: 'tb-add-user-dialog',
    templateUrl: './add-user-dialog.component.html',
    styleUrls: ['./add-user-dialog.component.scss'],
    standalone: false
})
export class AddUserDialogComponent extends DialogComponent<AddUserDialogComponent, User> implements OnInit {

  detailsForm: UntypedFormGroup;
  ownerAndGroupsFormGroup: UntypedFormGroup;
  initialOwnerId: EntityId;
  entityType = EntityType.USER;
  user: User;

  ownerAndGroupsEnabled: boolean;

  stepperOrientation: Observable<StepperOrientation>;
  stepperLabelPosition: Observable<'bottom' | 'end'>;
  selectedIndex = 0;
  showNext = true;

  activationMethods = Object.keys(ActivationMethod);
  activationMethodEnum = ActivationMethod;

  activationMethodTranslations = activationMethodTranslations;

  activationMethod = ActivationMethod.DISPLAY_ACTIVATION_LINK;

  @ViewChild(UserComponent, {static: true}) userComponent: UserComponent;
  @ViewChild('addUserWizardStepper', {static: true}) addUserWizardStepper: MatStepper;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddUserDialogData,
              public dialogRef: MatDialogRef<AddUserDialogComponent, User>,
              private userService: UserService,
              private entityGroupService: EntityGroupService,
              private breakpointObserver: BreakpointObserver,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.user = {} as User;
    this.userComponent.isEdit = true;
    this.userComponent.entity = this.user;
    this.detailsForm = this.userComponent.entityForm;

    this.ownerAndGroupsEnabled = getCurrentAuthUser(this.store).authority !== Authority.SYS_ADMIN;
    this.initialOwnerId = this.data.customerId && this.data.customerId !== NULL_UUID
      ? new CustomerId(this.data.customerId)
      : new TenantId(this.data.tenantId);
    this.ownerAndGroupsFormGroup = this.fb.group({
      ownerAndGroups: [{owner: this.initialOwnerId, groups: []} as OwnerAndGroups, [Validators.required]]
    });

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));
    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'end' : 'bottom'));
    this.showNext = this.ownerAndGroupsEnabled;
  }

  get maxStepperIndex(): number {
    return this.addUserWizardStepper?._steps?.length - 1;
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return 'user.user-details';
      case 1:
        return 'entity-group.owner-and-groups';
    }
    return '';
  }

  previousStep(): void {
    this.addUserWizardStepper.previous();
  }

  nextStep(): void {
    this.addUserWizardStepper.next();
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    this.showNext = this.selectedIndex !== this.maxStepperIndex;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (!this.detailsForm.valid || (this.ownerAndGroupsEnabled && this.ownerAndGroupsFormGroup.invalid)) {
      // Surface the offending step rather than silently refusing to submit.
      if (!this.detailsForm.valid) {
        this.detailsForm.markAllAsTouched();
        this.addUserWizardStepper.selectedIndex = 0;
      } else {
        this.ownerAndGroupsFormGroup.markAllAsTouched();
        this.addUserWizardStepper.selectedIndex = 1;
      }
      return;
    }
    this.user = {...this.user, ...this.userComponent.entityFormValue()};
    const groupIds = this.applyOwner();
    if (!this.user.additionalInfo.lang) {
      delete this.user.additionalInfo.lang;
    }
    if (!this.user.additionalInfo.unitSystem) {
      delete this.user.additionalInfo.unitSystem;
    }
    const sendActivationEmail = this.activationMethod === ActivationMethod.SEND_ACTIVATION_MAIL;
    this.userService.saveUser(this.user, sendActivationEmail).pipe(
      mergeMap(saved => this.addToGroups(saved, groupIds))
    ).subscribe(
      (user) => {
        if (this.activationMethod === ActivationMethod.DISPLAY_ACTIVATION_LINK) {
          this.userService.getActivationLinkInfo(user.id.id).subscribe(
            (activationLink) => {
              this.displayActivationLink(activationLink).subscribe(
                () => {
                  this.dialogRef.close(user);
                }
              );
            }
          );
        } else {
          this.dialogRef.close(user);
        }
      }
    );
  }

  /** The chosen owner also decides authority: a customer owner makes this a CUSTOMER_USER, the
   * tenant itself (or the SYS_ADMIN case, where step 2 is absent) makes it a TENANT_ADMIN.
   * Returns the picked group ids for the post-creation group assignment. */
  private applyOwner(): string[] {
    this.user.tenantId = new TenantId(this.data.tenantId);
    if (!this.ownerAndGroupsEnabled) {
      this.user.authority = Authority.TENANT_ADMIN;
      this.user.customerId = new CustomerId(NULL_UUID);
      return [];
    }
    const value: OwnerAndGroups = this.ownerAndGroupsFormGroup.get('ownerAndGroups').value;
    const owner = value?.owner as EntityInfoData;
    const ownerId = owner?.name !== undefined ? owner.id : value?.owner as EntityId;
    if (ownerId?.entityType === EntityType.CUSTOMER) {
      this.user.authority = Authority.CUSTOMER_USER;
      this.user.customerId = new CustomerId(ownerId.id);
    } else {
      this.user.authority = Authority.TENANT_ADMIN;
      this.user.customerId = new CustomerId(NULL_UUID);
    }
    return (value?.groups || []).map(group => group.id.id);
  }

  private addToGroups(user: User, groupIds: string[]): Observable<User> {
    if (!groupIds.length) {
      return of(user);
    }
    return forkJoin(groupIds.map(groupId =>
      this.entityGroupService.addEntitiesToEntityGroup(groupId, [user.id.id]))
    ).pipe(map(() => user));
  }

  displayActivationLink(activationLinkInfo: ActivationLinkInfo): Observable<void> {
    return this.dialog.open<ActivationLinkDialogComponent, ActivationLinkDialogData,
      void>(ActivationLinkDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        activationLinkInfo
      }
    }).afterClosed();
  }
}
