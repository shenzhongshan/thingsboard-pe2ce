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

import { Component, Inject, Injector, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormBuilder,
  FormGroupDirective,
  NgForm,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { EntityType, EntityTypeResource, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { EntityComponent } from './entity.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { CustomerId } from '@shared/models/id/customer-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { OwnerAndGroups } from './owner-and-groups.component';

@Component({
    selector: 'tb-add-entity-dialog',
    templateUrl: './add-entity-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: AddEntityDialogComponent }],
    styleUrls: ['./add-entity-dialog.component.scss'],
    standalone: false
})
export class AddEntityDialogComponent extends
  DialogComponent<AddEntityDialogComponent, BaseData<HasId>> implements OnInit, ErrorStateMatcher {

  entityComponent: EntityComponent<BaseData<HasId>>;
  detailsForm: UntypedFormGroup;

  entitiesTableConfig: EntityTableConfig<BaseData<HasId>>;
  translations: EntityTypeTranslation;
  resources: EntityTypeResource<BaseData<HasId>>;
  entity: BaseData<EntityId>;

  entityType: EntityType;
  ownerAndGroups = false;
  ownerAndGroupsFormGroup: UntypedFormGroup;
  initialOwnerId: EntityId;

  submitted = false;

  @ViewChild('entityDetailsForm', {static: true}) entityDetailsFormAnchor: TbAnchorComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityDialogData<BaseData<HasId>>,
              public dialogRef: MatDialogRef<AddEntityDialogComponent, BaseData<HasId>>,
              private injector: Injector,
              private entityGroupService: EntityGroupService,
              private fb: FormBuilder,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.entitiesTableConfig = this.data.entitiesTableConfig;
    this.translations = this.entitiesTableConfig.entityTranslations;
    this.resources = this.entitiesTableConfig.entityResources;
    this.entityType = this.entitiesTableConfig.entityType;
    this.entity = {};
    this.ownerAndGroups = this.entitiesTableConfig.addDialogOwnerAndGroups;
    if (this.ownerAndGroups) {
      const authUser = getCurrentAuthUser(this.store);
      // The table can be scoped to a customer (a customer's entity list) — that customer is then
      // the owner being added under; otherwise it is the current user's own owner.
      this.initialOwnerId = this.entitiesTableConfig.addDialogOwnerId
        ?? (authUser.authority === Authority.CUSTOMER_USER
          ? new CustomerId(authUser.customerId)
          : new TenantId(authUser.tenantId));
      this.ownerAndGroupsFormGroup = this.fb.group({
        ownerAndGroups: [{owner: this.initialOwnerId, groups: []} as OwnerAndGroups, [Validators.required]]
      });
    }
    const viewContainerRef = this.entityDetailsFormAnchor.viewContainerRef;
    viewContainerRef.clear();
    const injector: Injector = Injector.create(
      {
        providers: [
          {
            provide: 'entity',
            useValue: this.entity
          },
          {
            provide: 'entitiesTableConfig',
            useValue: this.entitiesTableConfig
          }
        ],
        parent: this.injector
      }
    );
    const componentRef = viewContainerRef.createComponent(this.entitiesTableConfig.entityComponent, {index: 0, injector});
    this.entityComponent = componentRef.instance;
    this.entityComponent.isEdit = true;
    this.detailsForm = this.entityComponent.entityForm;
  }

  helpLinkId(): string {
    if (this.resources.helpLinkIdForEntity && this.entityComponent.entityForm) {
      return this.resources.helpLinkIdForEntity(this.entityComponent.entityForm.getRawValue());
    } else {
      return this.resources.helpLinkId;
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    if (!this.detailsForm.valid || (this.ownerAndGroups && this.ownerAndGroupsFormGroup.invalid)) {
      return;
    }
    this.entity = {...this.entity, ...this.entityComponent.entityFormValue()};
    const groupIds = this.ownerAndGroups ? this.applyOwner() : [];
    this.entitiesTableConfig.saveEntity(this.entity).pipe(
      mergeMap(saved => this.addToGroups(saved, groupIds))
    ).subscribe(entity => this.dialogRef.close(entity));
  }

  /**
   * Folds the picked owner into the entity being saved and returns the picked group ids. A customer
   * owner becomes the entity's customer assignment (its parent customer, when adding a customer);
   * a tenant owner means "no customer", which is the default state anyway.
   */
  private applyOwner(): string[] {
    const value: OwnerAndGroups = this.ownerAndGroupsFormGroup.get('ownerAndGroups').value;
    const owner = value?.owner as EntityInfoData;
    const ownerId = owner?.name !== undefined ? owner.id : value?.owner as EntityId;
    if (ownerId?.entityType === EntityType.CUSTOMER) {
      const customerId = new CustomerId(ownerId.id);
      if (this.entityType === EntityType.CUSTOMER) {
        (this.entity as any).parentCustomerId = customerId;
      } else {
        (this.entity as any).customerId = customerId;
      }
    }
    return (value?.groups || []).map(group => group.id.id);
  }

  private addToGroups(entity: BaseData<HasId>, groupIds: string[]): Observable<BaseData<HasId>> {
    if (!groupIds.length) {
      return of(entity);
    }
    return forkJoin(groupIds.map(groupId =>
      this.entityGroupService.addEntitiesToEntityGroup(groupId, [entity.id.id]))
    ).pipe(map(() => entity));
  }
}
