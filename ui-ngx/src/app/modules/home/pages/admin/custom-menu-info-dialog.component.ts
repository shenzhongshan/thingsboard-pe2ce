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
import { Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { CustomMenuService } from '@core/http/custom-menu.service';
import {
  CMAssigneeType,
  CMScope,
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

  isSysAdmin = getCurrentAuthState(this.store).authUser.authority === Authority.SYS_ADMIN;
  isEdit = false;

  private originalAssigneeType: CMAssigneeType;
  private originalAssigneeIds: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: CustomMenuInfoDialogData,
              private dialogRef: MatDialogRef<CustomMenuInfoDialogComponent, boolean>,
              protected store: Store<AppState>,
              private customMenuService: CustomMenuService,
              private dialogService: DialogService,
              private translate: TranslateService,
              public fb: UntypedFormBuilder) {
    this.isEdit = !!data.menu;
    const menu = data.menu;
    this.infoForm = this.fb.group({
      name: [menu?.name || '', [Validators.required]],
      scope: [{
        value: menu?.scope || (this.isSysAdmin ? CMScope.SYSTEM : CMScope.TENANT),
        disabled: this.isEdit
      }],
      assigneeType: [menu?.assigneeType || CMAssigneeType.NO_ASSIGN],
      assigneeIds: [[]]
    });
    this.originalAssigneeType = menu?.assigneeType;
  }

  ngOnInit() {
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

  // Assigning to specific customers/users only makes sense at tenant level.
  get allowEntityAssignees(): boolean {
    return !this.isSysAdmin;
  }

  cancel(): void {
    this.dialogRef.close(false);
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
    if (this.isEdit) {
      this.update(v);
    } else {
      this.create(v, false);
    }
  }

  private needsEntityList(assigneeType: CMAssigneeType): boolean {
    return assigneeType === CMAssigneeType.CUSTOMERS || assigneeType === CMAssigneeType.USERS;
  }

  private create(v: any, force: boolean): void {
    const info: CustomMenuInfo = {
      name: v.name,
      scope: v.scope,
      assigneeType: v.assigneeType
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
    const assigneeIds: string[] = this.needsEntityList(v.assigneeType) ? v.assigneeIds : [];
    const assignmentChanged = v.assigneeType !== this.originalAssigneeType ||
      JSON.stringify([...assigneeIds].sort()) !== JSON.stringify([...this.originalAssigneeIds].sort());
    if (assignmentChanged) {
      chain = chain.pipe(
        switchMap(() => this.customMenuService.updateCustomMenuAssigneeList(
          menu.id.id, v.assigneeType, assigneeIds, false, { ignoreErrors: true }))
      );
    }
    chain.subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => this.handleDefaultConflict(err, () => {
        this.customMenuService.updateCustomMenuAssigneeList(menu.id.id, v.assigneeType, assigneeIds, true)
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
