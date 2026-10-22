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
import {CustomMenuTransferService} from '@core/http/custom-menu-transfer.service';
import {ImportExportService} from '@shared/import-export/import-export.service';
import {ImportDialogComponent, ImportDialogData} from '@shared/import-export/import-dialog.component';
import {CustomMenuTransferError, parseCustomMenuTransfer} from '@shared/models/custom-menu-transfer.data';
import {ActionNotificationShow} from '@core/notification/notification.actions';
import {getCurrentAuthState} from '@core/auth/auth.selectors';
import {Authority} from '@shared/models/authority.enum';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import {
  cmAssigneeTypeTranslations,
  cmScopeTranslations,
  CustomMenuInfo
} from '@shared/models/custom-menu.models';
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
  cmAssigneeTypeTranslations = cmAssigneeTypeTranslations;
  /** 导入弹窗和单次导出互斥，避免重复点击产生多个传输流程。 */
  transferBusy = false;

  constructor(protected store: Store<AppState>,
              private customMenuService: CustomMenuService,
              private menuService: MenuService,
              private dialogService: DialogService,
              private transferService: CustomMenuTransferService,
              private importExport: ImportExportService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
    this.reload();
  }

  /** 复用系统JSON导入弹窗，再复用菜单信息弹窗编辑名称；取消文件选择不会写入后端。 */
  importMenu(): void {
    if (this.transferBusy) {
      return;
    }
    this.transferBusy = true;
    this.dialog.open<ImportDialogComponent, ImportDialogData, unknown>(ImportDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {importTitle: 'custom-menu.transfer-import', importFileLabel: 'custom-menu.transfer-file'}
    }).afterClosed().subscribe(value => {
      if (value == null) {
        this.transferBusy = false;
        return;
      }
      try {
        const importedMenu = parseCustomMenuTransfer(value);
        if (importedMenu.scope === 'SYSTEM' &&
            getCurrentAuthState(this.store).authUser.authority !== Authority.SYS_ADMIN) {
          throw new CustomMenuTransferError('custom-menu.transfer-scope-denied');
        }
        this.dialog.open<CustomMenuInfoDialogComponent, CustomMenuInfoDialogData, boolean>(
          CustomMenuInfoDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
            data: {importedMenu}
          }).afterClosed().subscribe(() => {
            this.transferBusy = false;
            // 部分失败也刷新列表，使残留的新菜单可被定位和处理。
            this.reload();
          });
      } catch (error) {
        this.transferBusy = false;
        this.showTransferError(error);
      }
    });
  }

  /** 从服务端读取完整配置并沿用系统下载工具，不导出实体身份或分配关系。 */
  exportMenu(event: Event, menu: CustomMenuInfo): void {
    event.stopPropagation();
    if (this.transferBusy) {
      return;
    }
    this.transferBusy = true;
    this.transferService.exportMenu(menu.id.id).subscribe({
      next: payload => {
        try {
          this.importExport.exportJson(payload, `thingsboard-custom-menu-${payload.name}`);
        } catch (error) {
          this.showTransferError(error);
        } finally {
          this.transferBusy = false;
        }
      },
      error: error => {
        this.transferBusy = false;
        this.showTransferError(error);
      }
    });
  }

  /** 校验错误使用本地化文案；接口失败沿用系统错误消息，不伪装为成功导出。 */
  private showTransferError(error: unknown): void {
    this.store.dispatch(new ActionNotificationShow({
      message: error instanceof CustomMenuTransferError
        ? this.translate.instant(error.messageKey)
        : this.translate.instant('custom-menu.transfer-export-failed'),
      type: 'error'
    }));
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
