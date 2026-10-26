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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { GROUP_ALL_NAME, GROUP_CUSTOMER_ADMINS_NAME, GROUP_CUSTOMER_USERS_NAME } from '@shared/models/entity-group.models';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import {
  cmAllowedAssigneeTypes,
  cmAllowedScopes,
  CMAssigneeType,
  cmAssigneeTypeTranslation,
  CMScope,
  cmScopeTranslations,
  CustomMenuInfo
} from '@shared/models/custom-menu.models';

export interface CustomMenuInfoDialogData {
  // Present = edit mode (name + assignment); absent = create mode.
  menu?: CustomMenuInfo;
}

@Component({
  selector: 'tb-custom-menu-info-dialog',
  templateUrl: './custom-menu-info-dialog.component.html',
  standalone: false
})
export class CustomMenuInfoDialogComponent implements OnInit {

  infoForm: UntypedFormGroup;

  CMScope = CMScope;
  CMAssigneeType = CMAssigneeType;
  EntityType = EntityType;

  private readonly authority = getCurrentAuthState(this.store).authUser.authority;

  isSysAdmin = this.authority === Authority.SYS_ADMIN;
  isEdit = false;

  // Which scopes this user may create a menu for, and the scope-dependent assignee choices.
  allowedScopes = cmAllowedScopes(this.authority);
  allowedAssigneeTypes: CMAssigneeType[] = [];

  cmScopeTranslations = cmScopeTranslations;
  cmAssigneeTypeTranslation = cmAssigneeTypeTranslation;

  private authUser = getCurrentAuthState(this.store).authUser;

  fetchGroupNamesFn = this.fetchGroupNames.bind(this);

  private originalAssigneeType: CMAssigneeType;
  private originalAssigneeIds: string[] = [];
  private originalUserGroupNames: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: CustomMenuInfoDialogData,
              private dialogRef: MatDialogRef<CustomMenuInfoDialogComponent, boolean>,
              protected store: Store<AppState>,
              private customMenuService: CustomMenuService,
              private entityGroupService: EntityGroupService,
              private dialogService: DialogService,
              private translate: TranslateService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.menu;
    const menu = data.menu;
    this.infoForm = this.fb.group({
      name: [menu?.name || '', [Validators.required]],
      // Scope is immutable once created — it decides which built-in menu the config overrides.
      scope: [{
        value: menu?.scope || this.allowedScopes[0],
        disabled: this.isEdit
      }],
      assigneeType: [menu?.assigneeType || CMAssigneeType.NO_ASSIGN],
      assigneeIds: [[]],
      userGroupNames: [menu?.userGroupNames || []]
    });
    this.originalAssigneeType = menu?.assigneeType;
    this.originalUserGroupNames = menu?.userGroupNames || [];
    this.updateAllowedAssigneeTypes(false);
  }

  ngOnInit() {
    this.infoForm.get('scope').valueChanges.subscribe(() => this.updateAllowedAssigneeTypes(true));
    if (this.isEdit && (this.data.menu.assigneeType === CMAssigneeType.CUSTOMERS ||
        this.data.menu.assigneeType === CMAssigneeType.USERS)) {
      this.customMenuService.getCustomMenuAssigneeList(this.data.menu.id.id).subscribe((assignees) => {
        this.originalAssigneeIds = assignees.map((a) => a.id.id);
        this.infoForm.get('assigneeIds').patchValue(this.originalAssigneeIds);
      });
    }
  }

  get assigneeType(): CMAssigneeType {
    return this.infoForm.get('assigneeType').value;
  }

  get scope(): CMScope {
    return this.infoForm.get('scope').value;
  }

  /**
   * A TENANT scope menu is the tenant-admin menu, so it can only be handed to tenant users —
   * CUSTOMERS is rejected by the backend validator. Switching scope therefore has to drop a now
   * invalid assignee type instead of leaving it selected and failing on save.
   */
  private updateAllowedAssigneeTypes(resetInvalid: boolean) {
    this.allowedAssigneeTypes = cmAllowedAssigneeTypes(this.scope);
    if (resetInvalid && !this.allowedAssigneeTypes.includes(this.assigneeType)) {
      this.infoForm.get('assigneeType').patchValue(CMAssigneeType.NO_ASSIGN);
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  /**
   * Groups the editor's own owner directly holds, for a scope the editor can actually resolve to
   * one owner (TENANT scope, or CUSTOMER scope edited by a customer user). When a tenant admin is
   * assigning a CUSTOMER-scope menu, no single owner applies — the menu can target any customer's
   * users — so there is nothing to list from and the picker falls back to the two autogenerated
   * per-customer group names every customer has (PE parity: free text is still allowed for a
   * custom group name shared across customers).
   */
  private fetchGroupNames(searchText?: string): Observable<StringItemsOption[]> {
    const resolvableOwner = this.scope === CMScope.TENANT ||
      (this.scope === CMScope.CUSTOMER && this.authority === Authority.CUSTOMER_USER);
    if (resolvableOwner) {
      const ownerId: EntityId = this.scope === CMScope.TENANT
        ? { entityType: EntityType.TENANT, id: this.authUser.tenantId }
        : { entityType: EntityType.CUSTOMER, id: this.authUser.customerId };
      const pageLink = new PageLink(50, 0, searchText || null, { property: 'name', direction: Direction.ASC });
      return this.entityGroupService.getEntityGroupEntityInfosByOwnerId(pageLink, ownerId, EntityType.USER,
        { ignoreLoading: true }).pipe(
        map((page) => page.data
          .filter((g) => g.name !== GROUP_ALL_NAME)
          .map((g) => ({ name: g.name, value: g.name })))
      );
    }
    const text = (searchText || '').trim().toLowerCase();
    return of([GROUP_CUSTOMER_ADMINS_NAME, GROUP_CUSTOMER_USERS_NAME]
      .filter((name) => name.toLowerCase().includes(text))
      .map((name) => ({ name, value: name })));
  }

  save(): void {
    if (!this.infoForm.valid) {
      this.infoForm.markAllAsTouched();
      return;
    }
    const v = this.infoForm.getRawValue();
    if (this.needsEntityList(v.assigneeType) && !v.assigneeIds?.length) {
      this.infoForm.get('assigneeIds').setErrors({ required: true });
      this.infoForm.get('assigneeIds').markAsTouched();
      return;
    }
    if (this.needsGroupNames(v.assigneeType) && !v.userGroupNames?.length) {
      this.infoForm.get('userGroupNames').setErrors({ required: true });
      this.infoForm.get('userGroupNames').markAsTouched();
      return;
    }
    if (this.isEdit) {
      this.update(v);
    } else {
      this.create(v, false);
    }
  }

  private needsEntityList(assigneeType: CMAssigneeType): boolean {
    return assigneeType === CMAssigneeType.CUSTOMERS || assigneeType === CMAssigneeType.USERS;
  }

  private needsGroupNames(assigneeType: CMAssigneeType): boolean {
    return assigneeType === CMAssigneeType.USER_GROUPS;
  }

  // The backend's "assign to" body serves double duty: entity ids for CUSTOMERS/USERS, entity-group
  // names for USER_GROUPS — whichever the assignee type calls for.
  private assignTargets(v: any): string[] {
    if (this.needsEntityList(v.assigneeType)) {
      return v.assigneeIds;
    }
    if (this.needsGroupNames(v.assigneeType)) {
      return v.userGroupNames;
    }
    return [];
  }

  private create(v: any, force: boolean): void {
    const info: CustomMenuInfo = {
      name: v.name,
      scope: v.scope,
      assigneeType: v.assigneeType,
      userGroupNames: this.needsGroupNames(v.assigneeType) ? v.userGroupNames : undefined
    };
    const assignToIds = this.needsEntityList(v.assigneeType) ? v.assigneeIds : [];
    this.customMenuService.createCustomMenu(info, assignToIds, force, { ignoreErrors: true }).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => this.handleDefaultConflict(err, () => this.create(v, true))
    });
  }

  private update(v: any): void {
    const menu = this.data.menu;
    let chain: Observable<any> = of(null);
    if (v.name !== menu.name) {
      chain = this.customMenuService.updateCustomMenuName(menu.id.id, v.name);
    }
    const assignTargets = this.assignTargets(v);
    const originalTargets = this.originalAssigneeType === CMAssigneeType.USER_GROUPS
      ? this.originalUserGroupNames : this.originalAssigneeIds;
    const assignmentChanged = v.assigneeType !== this.originalAssigneeType ||
      JSON.stringify([...assignTargets].sort()) !== JSON.stringify([...originalTargets].sort());
    if (assignmentChanged) {
      chain = chain.pipe(
        switchMap(() => this.customMenuService.updateCustomMenuAssigneeList(
          menu.id.id, v.assigneeType, assignTargets, false, { ignoreErrors: true }))
      );
    }
    chain.subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => this.handleDefaultConflict(err, () => {
        this.customMenuService.updateCustomMenuAssigneeList(menu.id.id, v.assigneeType, assignTargets, true)
          .subscribe(() => this.dialogRef.close(true));
      })
    });
  }

  // ALL-type default conflict: backend rejects unless force. Offer to demote the existing default.
  private handleDefaultConflict(err: any, retryWithForce: () => void): void {
    if (this.assigneeType === CMAssigneeType.ALL && err?.status === 400) {
      this.dialogService.confirm(
        this.translate.instant('custom-menu.default-conflict-title'),
        this.translate.instant('custom-menu.default-conflict-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')
      ).subscribe((confirmed) => {
        if (confirmed) {
          retryWithForce();
        }
      });
    } else {
      this.store.dispatch(new ActionNotificationShow({
        message: err?.error?.message || err?.message || 'Unknown error',
        type: 'error',
        duration: 3000
      }));
    }
  }
}
