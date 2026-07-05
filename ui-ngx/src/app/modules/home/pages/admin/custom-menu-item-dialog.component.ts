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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import {
  CMItemLinkType,
  CMItemType,
  CustomMenuItem,
  MenuItemType,
  newCustomMenuItem
} from '@shared/models/custom-menu.models';

export interface CustomMenuItemDialogData {
  item?: CustomMenuItem;
  // Child items of a section can only be links.
  allowSection: boolean;
}

@Component({
  selector: 'tb-custom-menu-item-dialog',
  templateUrl: './custom-menu-item-dialog.component.html',
  standalone: false
})
export class CustomMenuItemDialogComponent {

  itemForm: UntypedFormGroup;

  CMItemType = CMItemType;
  CMItemLinkType = CMItemLinkType;

  // Dashboards are tenant entities: the dashboard link type is meaningless for a sysadmin.
  allowDashboard = getCurrentAuthState(this.store).authUser.authority !== Authority.SYS_ADMIN;

  constructor(@Inject(MAT_DIALOG_DATA) public data: CustomMenuItemDialogData,
              private dialogRef: MatDialogRef<CustomMenuItemDialogComponent, CustomMenuItem>,
              protected store: Store<AppState>,
              public fb: UntypedFormBuilder) {
    const item = data.item || newCustomMenuItem();
    this.itemForm = this.fb.group({
      visible: [item.visible !== false],
      icon: [item.icon || 'star'],
      name: [item.name || '', [Validators.required]],
      menuItemType: [item.menuItemType || CMItemType.LINK],
      linkType: [item.linkType || CMItemLinkType.URL],
      url: [item.url || ''],
      setAccessToken: [item.setAccessToken === true],
      dashboardId: [item.dashboardId || null],
      hideDashboardToolbar: [item.hideDashboardToolbar === true]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (!this.itemForm.valid) {
      this.itemForm.markAllAsTouched();
      return;
    }
    const v = this.itemForm.value;
    const item: CustomMenuItem = {
      type: MenuItemType.CUSTOM,
      visible: v.visible,
      icon: v.icon,
      name: v.name,
      menuItemType: v.menuItemType
    };
    if (v.menuItemType === CMItemType.LINK) {
      if (v.linkType === CMItemLinkType.DASHBOARD && this.allowDashboard) {
        if (!v.dashboardId) {
          this.itemForm.get('dashboardId').setErrors({ required: true });
          this.itemForm.get('dashboardId').markAsTouched();
          return;
        }
        item.linkType = CMItemLinkType.DASHBOARD;
        item.dashboardId = v.dashboardId;
        item.hideDashboardToolbar = v.hideDashboardToolbar;
      } else {
        if (!v.url) {
          this.itemForm.get('url').setErrors({ required: true });
          this.itemForm.get('url').markAsTouched();
          return;
        }
        item.linkType = CMItemLinkType.URL;
        item.url = v.url;
        item.setAccessToken = v.setAccessToken;
      }
    } else {
      item.pages = this.data.item?.pages || [];
    }
    this.dialogRef.close(item);
  }
}
