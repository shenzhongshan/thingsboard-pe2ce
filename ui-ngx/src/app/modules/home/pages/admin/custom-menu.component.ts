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
import { ActivatedRoute } from '@angular/router';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { buildUserMenu, MenuSection } from '@core/services/menu.models';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { MenuService } from '@core/services/menu.service';
import {
  CMItemType,
  CustomMenuConfig,
  CustomMenuItem,
  DefaultMenuItem,
  MenuItem,
  MenuItemType
} from '@shared/models/custom-menu.models';
import {
  CustomMenuItemDialogComponent,
  CustomMenuItemDialogData
} from '@home/pages/admin/custom-menu-item-dialog.component';
import {
  CustomMenuDefaultItemDialogComponent,
  CustomMenuDefaultItemDialogData,
  CustomMenuDefaultItemDialogResult
} from '@home/pages/admin/custom-menu-default-item-dialog.component';

interface EditorMenuItem {
  type: MenuItemType;
  id?: string;              // DEFAULT: MenuId
  baseName?: string;        // DEFAULT: translated base label
  baseIcon?: string;        // DEFAULT: original base icon, to detect an override
  icon?: string;
  name: string;             // rename override ('' = none for DEFAULT)
  visible: boolean;
  isHome?: boolean;
  // CUSTOM link payload:
  custom?: CustomMenuItem;
  pages: EditorMenuItem[];
}

@Component({
  selector: 'tb-custom-menu',
  templateUrl: './custom-menu.component.html',
  styleUrls: ['./custom-menu.component.scss', './settings-card.scss'],
  standalone: false
})
export class CustomMenuComponent extends PageComponent implements OnInit, HasConfirmForm {

  // Only used to drive ConfirmOnExitGuard dirtiness.
  dirtyForm: UntypedFormGroup;

  items: EditorMenuItem[] = [];
  showHidden = true;

  customMenuId: string;
  menuName = '';

  constructor(protected store: Store<AppState>,
              private customMenuService: CustomMenuService,
              private menuService: MenuService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private route: ActivatedRoute,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.dirtyForm = this.fb.group({});
    this.customMenuId = this.route.snapshot.paramMap.get('customMenuId');
    this.customMenuService.getCustomMenuInfoById(this.customMenuId).subscribe(
      (info) => {
        this.menuName = info.name;
      }
    );
    this.load();
  }

  private load() {
    this.customMenuService.getCustomMenuConfigById(this.customMenuId).subscribe(
      (config) => {
        this.items = this.buildEditorItems(config);
        this.dirtyForm.markAsPristine();
      }
    );
  }

  private baseSections(): MenuSection[] {
    return buildUserMenu(getCurrentAuthState(this.store));
  }

  private sectionToEditorItem(section: MenuSection): EditorMenuItem {
    return {
      type: MenuItemType.DEFAULT,
      id: section.id as string,
      baseName: this.translate.instant(section.name),
      baseIcon: section.icon,
      icon: section.icon,
      name: '',
      visible: true,
      isHome: section.id === 'home',
      pages: (section.pages || []).map((child) => this.sectionToEditorItem(child))
    };
  }

  private buildEditorItems(config: CustomMenuConfig): EditorMenuItem[] {
    const base = this.baseSections().map((s) => this.sectionToEditorItem(s));
    if (!config?.items?.length) {
      return base;
    }
    return this.overlay(base, config.items);
  }

  private overlay(base: EditorMenuItem[], configItems: MenuItem[]): EditorMenuItem[] {
    const remaining = [...base];
    const result: EditorMenuItem[] = [];
    for (const item of configItems) {
      if (item.type === MenuItemType.CUSTOM) {
        result.push(this.customToEditorItem(item as CustomMenuItem));
      } else {
        const def = item as DefaultMenuItem;
        const idx = remaining.findIndex((b) => b.id === def.id);
        if (idx >= 0) {
          const row = remaining.splice(idx, 1)[0];
          row.visible = def.visible !== false;
          row.name = def.name || '';
          row.icon = def.icon || row.baseIcon;
          if (def.pages?.length) {
            row.pages = this.overlay(row.pages, def.pages);
          }
          result.push(row);
        }
      }
    }
    // Base entries not mentioned in the config (e.g. features added after the config was saved)
    // are inserted at their natural position, after the nearest preceding base sibling.
    for (const missing of remaining) {
      const baseIdx = base.indexOf(missing);
      let insertAt = result.length;
      for (let i = baseIdx - 1; i >= 0; i--) {
        const pos = result.findIndex((r) => r.id === base[i].id && r.type === MenuItemType.DEFAULT);
        if (pos >= 0) {
          insertAt = pos + 1;
          break;
        }
      }
      result.splice(insertAt, 0, missing);
    }
    return result;
  }

  private customToEditorItem(item: CustomMenuItem): EditorMenuItem {
    return {
      type: MenuItemType.CUSTOM,
      icon: item.icon || 'link',
      baseName: this.translate.instant(item.menuItemType === CMItemType.SECTION ?
        'custom-menu.item-type-section' : 'custom-menu.item-type-link'),
      name: item.name,
      visible: item.visible !== false,
      custom: item,
      pages: (item.pages || []).map((child) => this.customToEditorItem(child))
    };
  }

  get visibleItems(): EditorMenuItem[] {
    return this.showHidden ? this.items : this.items.filter((i) => i.visible);
  }

  visibleChildren(item: EditorMenuItem): EditorMenuItem[] {
    return this.showHidden ? item.pages : item.pages.filter((i) => i.visible);
  }

  drop(event: CdkDragDrop<EditorMenuItem[]>, list: EditorMenuItem[]) {
    if (event.previousIndex !== event.currentIndex) {
      // Indices refer to the rendered (possibly filtered) list; map back to the model list.
      const rendered = this.showHidden ? list : list.filter((i) => i.visible);
      const moved = rendered[event.previousIndex];
      const target = rendered[event.currentIndex];
      const from = list.indexOf(moved);
      const to = list.indexOf(target);
      moveItemInArray(list, from, to);
      this.dirtyForm.markAsDirty();
    }
  }

  toggleVisible(item: EditorMenuItem, visible: boolean) {
    item.visible = visible;
    this.dirtyForm.markAsDirty();
  }

  nameChanged() {
    this.dirtyForm.markAsDirty();
  }

  hideAll() {
    // “隐藏全部”现在包含原生首页，用户可以再单独打开需要保留的入口。
    const hide = (items: EditorMenuItem[]) => items.forEach((i) => {
      i.visible = false;
      hide(i.pages);
    });
    hide(this.items);
    this.dirtyForm.markAsDirty();
  }

  addCustomItem(parent?: EditorMenuItem) {
    this.dialog.open<CustomMenuItemDialogComponent, CustomMenuItemDialogData, CustomMenuItem>(
      CustomMenuItemDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { allowSection: !parent }
      }).afterClosed().subscribe((item) => {
      if (item) {
        const row = this.customToEditorItem(item);
        if (parent) {
          parent.pages.push(row);
        } else {
          this.items.push(row);
        }
        this.dirtyForm.markAsDirty();
      }
    });
  }

  editCustomItem(row: EditorMenuItem) {
    this.dialog.open<CustomMenuItemDialogComponent, CustomMenuItemDialogData, CustomMenuItem>(
      CustomMenuItemDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { item: this.editorItemToCustom(row), allowSection: row.custom?.menuItemType === CMItemType.SECTION }
      }).afterClosed().subscribe((item) => {
      if (item) {
        const updated = this.customToEditorItem(item);
        updated.pages = row.pages;
        Object.assign(row, updated);
        this.dirtyForm.markAsDirty();
      }
    });
  }

  editDefaultItem(row: EditorMenuItem) {
    this.dialog.open<CustomMenuDefaultItemDialogComponent, CustomMenuDefaultItemDialogData,
      CustomMenuDefaultItemDialogResult>(CustomMenuDefaultItemDialogComponent, {
      disableClose: true,
      autoFocus: false,
      data: { baseName: row.baseName, icon: row.icon, name: row.name, visible: row.visible }
    }).afterClosed().subscribe((result) => {
      if (result) {
        row.icon = result.icon || row.baseIcon;
        row.name = result.name;
        row.visible = result.visible;
        this.dirtyForm.markAsDirty();
      }
    });
  }

  removeCustomItem(list: EditorMenuItem[], row: EditorMenuItem) {
    const idx = list.indexOf(row);
    if (idx >= 0) {
      list.splice(idx, 1);
      this.dirtyForm.markAsDirty();
    }
  }

  hasDefaultOverride(row: EditorMenuItem): boolean {
    return row.icon !== row.baseIcon || !!row.name || row.visible !== true;
  }

  resetDefaultItem(row: EditorMenuItem) {
    row.icon = row.baseIcon;
    row.name = '';
    row.visible = true;
    this.dirtyForm.markAsDirty();
  }

  // Reset the editor to the built-in menu (local only — takes effect on save).
  reset() {
    this.items = this.baseSections().map((s) => this.sectionToEditorItem(s));
    this.dirtyForm.markAsDirty();
  }

  cancel() {
    this.load();
  }

  private editorItemToCustom(row: EditorMenuItem): CustomMenuItem {
    const item: CustomMenuItem = {
      ...(row.custom || { type: MenuItemType.CUSTOM, name: row.name, menuItemType: CMItemType.LINK }),
      name: row.name,
      icon: row.icon,
      visible: row.visible
    };
    item.pages = row.pages.map((child) => this.editorItemToCustom(child));
    return item;
  }

  private editorItemToConfigItem(row: EditorMenuItem): MenuItem {
    if (row.type === MenuItemType.CUSTOM) {
      return this.editorItemToCustom(row);
    }
    const def: DefaultMenuItem = {
      type: MenuItemType.DEFAULT,
      id: row.id,
      visible: row.visible
    };
    if (row.name) {
      def.name = row.name;
    }
    if (row.icon && row.icon !== row.baseIcon) {
      def.icon = row.icon;
    }
    if (row.pages.length) {
      def.pages = row.pages.map((child) => this.editorItemToConfigItem(child));
    }
    return def;
  }

  save() {
    const config: CustomMenuConfig = {
      items: this.items.map((row) => this.editorItemToConfigItem(row))
    };
    this.customMenuService.updateCustomMenuConfig(this.customMenuId, config).subscribe(
      () => {
        this.dirtyForm.markAsPristine();
        this.menuService.reloadMenu();
      }
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.dirtyForm;
  }
}
