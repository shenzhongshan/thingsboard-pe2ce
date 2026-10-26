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

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { MenuService } from '@core/services/menu.service';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import {
  cmAssigneeTypeTranslation,
  cmScopeTranslations,
  CustomMenuInfo
} from '@shared/models/custom-menu.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import {
  CustomMenuInfoDialogComponent,
  CustomMenuInfoDialogData
} from '@home/pages/admin/custom-menu-info-dialog.component';

@Component({
  selector: 'tb-custom-menu-list',
  templateUrl: './custom-menu-list.component.html',
  styleUrls: ['./custom-menu-list.component.scss', './settings-card.scss'],
  standalone: false
})
export class CustomMenuListComponent extends PageComponent implements OnInit {

  menus: CustomMenuInfo[] = [];

  cmScopeTranslations = cmScopeTranslations;
  cmAssigneeTypeTranslation = cmAssigneeTypeTranslation;

  private readonly authority = getCurrentAuthState(this.store).authUser.authority;

  /**
   * The system administrator manages exactly the three seeded per-scope defaults: adding,
   * renaming, assigning and deleting are all meaningless there (and the backend refuses to delete
   * them), so only the "edit items" action is offered. Follows PE's table config.
   */
  manageEnabled = this.authority !== Authority.SYS_ADMIN;

  // A customer only ever owns CUSTOMER scope menus, so the scope column carries no information;
  // the system administrator's menus are never assigned to anyone, so that column is dropped too.
  columns: string[] = ['createdTime', 'name',
    ...(this.authority === Authority.CUSTOMER_USER ? [] : ['scope']),
    ...(this.authority === Authority.SYS_ADMIN ? [] : ['assigneeType']),
    'actions'];

  constructor(protected store: Store<AppState>,
              private customMenuService: CustomMenuService,
              private menuService: MenuService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
    this.reload();
  }

  reload() {
    const pageLink = new PageLink(1024, 0, null, { property: 'createdTime', direction: Direction.DESC } as SortOrder);
    this.customMenuService.getCustomMenuInfos(pageLink).subscribe(
      (page) => {
        this.menus = page.data;
      }
    );
  }

  addMenu() {
    this.dialog.open<CustomMenuInfoDialogComponent, CustomMenuInfoDialogData, boolean>(
      CustomMenuInfoDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {}
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
        this.menuService.reloadMenu();
      }
    });
  }

  editMenuInfo($event: Event, menu: CustomMenuInfo) {
    $event?.stopPropagation();
    this.dialog.open<CustomMenuInfoDialogComponent, CustomMenuInfoDialogData, boolean>(
      CustomMenuInfoDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { menu }
      }).afterClosed().subscribe((changed) => {
      if (changed) {
        this.reload();
        this.menuService.reloadMenu();
      }
    });
  }

  openMenuEditor($event: Event, menu: CustomMenuInfo) {
    $event?.stopPropagation();
    this.router.navigate(['/white-labeling/custom-menu', menu.id.id]);
  }

  deleteMenu($event: Event, menu: CustomMenuInfo) {
    $event?.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('custom-menu.delete-menu-title', {menuName: menu.name}),
      this.translate.instant('custom-menu.delete-menu-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.customMenuService.deleteCustomMenu(menu.id.id).subscribe((result) => {
          if (result.success) {
            this.reload();
            this.menuService.reloadMenu();
          } else {
            const assignees = result.assigneeList.map((a) => a.name).join(', ');
            this.dialogService.confirm(
              this.translate.instant('custom-menu.delete-menu-assigned-title', {menuName: menu.name}),
              this.translate.instant('custom-menu.delete-menu-assigned-text', {assigneeList: assignees}),
              this.translate.instant('action.no'),
              this.translate.instant('action.yes')
            ).subscribe((forced) => {
              if (forced) {
                this.customMenuService.deleteCustomMenu(menu.id.id, true).subscribe(() => {
                  this.reload();
                  this.menuService.reloadMenu();
                });
              }
            });
          }
        });
      }
    });
  }

}
