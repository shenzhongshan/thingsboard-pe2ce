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

import { Component, forwardRef, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { RoleService } from '@core/http/role.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { GroupPermission, Role, roleTypeTranslations } from '@shared/models/role.models';
import { PageLink } from '@shared/models/page/page-link';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import {
  SelfRegistrationPermissionDialogComponent
} from '@home/pages/admin/self-registration-permission-dialog.component';

/**
 * The 用户组角色 table of the self-registration settings page.
 *
 * Purely in-memory: the rows are a slice of the self-registration params blob and are only
 * materialised into real group_permission rows by SignUpController when someone signs up. It
 * must therefore never call roleService.saveGroupPermission — doing so would create permissions
 * against a user group that does not exist yet.
 */
@Component({
  selector: 'tb-self-registration-permissions',
  templateUrl: './self-registration-permissions.component.html',
  styleUrls: ['./self-registration-permissions.component.scss'],
  standalone: false,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SelfRegistrationPermissionsComponent),
    multi: true
  }]
})
export class SelfRegistrationPermissionsComponent implements ControlValueAccessor, OnInit {

  permissions: GroupPermission[] = [];
  searchText = '';
  disabled = false;

  roleTypeTranslations = roleTypeTranslations;
  entityTypeTranslations = entityTypeTranslations;

  private roleNames = new Map<string, Role>();
  private groupNames = new Map<string, string>();

  private propagateChange: (value: GroupPermission[]) => void = () => {};

  constructor(private roleService: RoleService,
              private entityGroupService: EntityGroupService,
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.loadDisplayNames();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: GroupPermission[]): void {
    this.permissions = Array.isArray(value) ? [...value] : [];
    this.loadDisplayNames();
  }

  /** Role/group names are not stored in the blob, only ids — resolve them for display. */
  private loadDisplayNames() {
    this.roleService.getRoles(new PageLink(1024, 0), {ignoreErrors: true}).subscribe(page => {
      (page.data || []).forEach(r => this.roleNames.set(r.id.id, r));
    });
    const groupIds = this.permissions.map(p => p.entityGroupId?.id).filter(id => !!id);
    if (groupIds.length) {
      this.entityGroupService.getEntityGroupsByIds(groupIds, {ignoreErrors: true}).subscribe(groups => {
        (groups || []).forEach(g => this.groupNames.set(g.id.id, g.name));
      });
    }
  }

  get visiblePermissions(): GroupPermission[] {
    const q = this.searchText.trim().toLowerCase();
    if (!q) {
      return this.permissions;
    }
    return this.permissions.filter(p =>
      this.roleName(p).toLowerCase().includes(q) || this.groupName(p).toLowerCase().includes(q));
  }

  roleName(permission: GroupPermission): string {
    return this.roleNames.get(permission.roleId?.id)?.name || permission.roleId?.id || '';
  }

  roleTypeLabel(permission: GroupPermission): string {
    const type = this.roleNames.get(permission.roleId?.id)?.type;
    return type ? roleTypeTranslations.get(type) : '';
  }

  groupName(permission: GroupPermission): string {
    const id = permission.entityGroupId?.id;
    return id ? (this.groupNames.get(id) || id) : '';
  }

  addPermission() {
    this.dialog.open<SelfRegistrationPermissionDialogComponent, void, GroupPermission>(
      SelfRegistrationPermissionDialogComponent, {disableClose: true, panelClass: ['tb-dialog', 'tb-fullscreen-dialog']}
    ).afterClosed().subscribe(permission => {
      if (permission) {
        this.permissions = [...this.permissions, permission];
        this.loadDisplayNames();
        this.propagateChange(this.permissions);
      }
    });
  }

  deletePermission(permission: GroupPermission) {
    this.permissions = this.permissions.filter(p => p !== permission);
    this.propagateChange(this.permissions);
  }

  refresh() {
    this.loadDisplayNames();
  }

}
