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
import { TranslateService } from '@ngx-translate/core';
import { RoleService } from '@core/http/role.service';
import {
  Role,
  roleOperationTranslation,
  roleResourceTranslation,
  roleTypeTranslations,
  RoleType
} from '@shared/models/role.models';

export interface RoleViewDialogData {
  roleId: string;
}

/**
 * Read-only peek at a role referenced from a group permission row — lets someone reviewing "who has
 * access to this group" (or "what roles this user group holds") check what a role actually grants
 * without leaving the tab to navigate to the role's own page. PE parity: `viewRole()`.
 */
@Component({
    selector: 'tb-role-view-dialog',
    templateUrl: './role-view-dialog.component.html',
    standalone: false
})
export class RoleViewDialogComponent implements OnInit {

  role: Role;
  RoleType = RoleType;
  roleTypeTranslations = roleTypeTranslations;
  roleResourceTranslation = roleResourceTranslation;
  roleOperationTranslation = roleOperationTranslation;

  genericRows: {resource: string; operations: string[]}[] = [];
  groupOperations: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: RoleViewDialogData,
              private dialogRef: MatDialogRef<RoleViewDialogComponent>,
              private roleService: RoleService,
              private translate: TranslateService) {
  }

  ngOnInit() {
    this.roleService.getRole(this.data.roleId).subscribe((role) => {
      this.role = role;
      if (role.type === RoleType.GENERIC && role.permissions && !Array.isArray(role.permissions)) {
        this.genericRows = Object.entries(role.permissions).map(([resource, operations]) =>
          ({resource, operations: operations as string[]}));
      } else if (Array.isArray(role.permissions)) {
        this.groupOperations = role.permissions as string[];
      }
    });
  }

  /** Template expressions can't nest a pipe inside an array callback, so the translate+join happens
   * here instead of `operations.map(op => (op | translate)).join(', ')` in the template. */
  operationsLabel(operations: string[]): string {
    return (operations || []).map((op) => this.translate.instant(roleOperationTranslation(op))).join(', ');
  }

  close(): void {
    this.dialogRef.close();
  }

}
