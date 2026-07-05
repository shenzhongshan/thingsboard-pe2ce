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

import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, take } from 'rxjs/operators';
import { buildUserHome, buildUserMenu, HomeSection, MenuId, MenuSection } from '@core/services/menu.models';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { AuthState } from '@core/auth/auth.models';
import { NavigationEnd, Router } from '@angular/router';
import { CustomMenuService } from '@core/http/custom-menu.service';
import {
  CMItemLinkType,
  CMItemType,
  CustomMenuItem,
  DefaultMenuItem,
  MenuItem,
  MenuItemType
} from '@shared/models/custom-menu.models';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  private currentMenuSections: Array<MenuSection>;
  private menuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private homeSections$: Subject<Array<HomeSection>> = new ReplaySubject<Array<HomeSection>>(1);
  private availableMenuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private availableMenuLinks$ = this.menuSections$.pipe(
    map((items) => this.allMenuLinks(items))
  );

  constructor(private store: Store<AppState>,
              private customMenuService: CustomMenuService,
              private router: Router) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        this.updateOpenedMenuSections();
      }
    );
  }

  private buildMenu() {
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          this.currentMenuSections = buildUserMenu(authState);
          this.publishMenu(authState);
          // Apply the custom menu configuration (system/tenant white-labeling): reorder,
          // hide and rename built-in entries and append custom links/sections.
          this.customMenuService.getCustomMenu({ ignoreErrors: true, ignoreLoading: true }).subscribe({
            next: (customMenu) => {
              if (customMenu?.items?.length) {
                this.currentMenuSections = this.overlaySections(buildUserMenu(authState), customMenu.items);
                this.publishMenu(authState);
              }
            },
            error: () => { /* no custom menu configured */ }
          });
        }
      }
    );
  }

  public reloadMenu() {
    this.buildMenu();
  }

  private publishMenu(authState: AuthState) {
    this.updateOpenedMenuSections();
    this.menuSections$.next(this.currentMenuSections);
    const availableMenuSections = this.allMenuSections(this.currentMenuSections);
    this.availableMenuSections$.next(availableMenuSections);
    const homeSections = buildUserHome(authState, availableMenuSections);
    this.homeSections$.next(homeSections);
  }

  private overlaySections(base: Array<MenuSection>, items: MenuItem[]): Array<MenuSection> {
    const remaining = [...base];
    const result: Array<MenuSection> = [];
    let customIdx = 0;
    for (const item of items) {
      if (item.type === MenuItemType.CUSTOM) {
        const custom = item as CustomMenuItem;
        if (custom.visible !== false) {
          result.push(this.mapCustomItem(custom, customIdx++));
        }
      } else {
        const def = item as DefaultMenuItem;
        const idx = remaining.findIndex((b) => b.id === def.id);
        if (idx >= 0) {
          // Clone: menuSectionMap entries may be shared by reference.
          const section: MenuSection = { ...remaining.splice(idx, 1)[0] };
          if (def.visible === false) {
            continue;
          }
          if (def.name) {
            section.name = def.name;
            section.customTranslate = true;
          }
          if (def.pages?.length && section.pages?.length) {
            section.pages = this.overlaySections(section.pages, def.pages);
          }
          result.push(section);
        }
      }
    }
    // Built-in entries not mentioned in the config (e.g. platform features added AFTER the
    // config was saved) are inserted at their natural default-menu position — right after the
    // nearest preceding base sibling already in the result — instead of being dumped at the end.
    for (const missing of remaining) {
      const baseIdx = base.indexOf(missing);
      let insertAt = result.length;
      for (let i = baseIdx - 1; i >= 0; i--) {
        const pos = result.findIndex((s) => s.id === base[i].id);
        if (pos >= 0) {
          insertAt = pos + 1;
          break;
        }
      }
      result.splice(insertAt, 0, missing);
    }
    return result;
  }

  private mapCustomItem(item: CustomMenuItem, idx: number): MenuSection {
    // Every MenuSection needs a real, unique, non-empty path: RouterTabsComponent.findRootSection
    // matches routes via `sectionPath.endsWith(section.path)`, and an empty string is a suffix of
    // ANY string in JS — an href-only link left with path:'' would then be (mis)matched as the
    // "root section" for every route, and since it has no `.pages`, that crashes tab rendering
    // for unrelated pages (e.g. Alarms, OAuth2). This synthetic path is never a real app route,
    // so it can never accidentally match one.
    const uniquePath = `/custom-menu-item/${idx}`;
    const base: MenuSection = {
      id: `custom_menu_${idx}_${item.name}`,
      name: item.name,
      type: 'link',
      path: uniquePath,
      icon: item.icon || 'link',
      customTranslate: true
    };
    if (item.menuItemType === CMItemType.SECTION) {
      base.type = 'toggle';
      base.pages = (item.pages || [])
        .filter((child) => child.visible !== false)
        .map((child, childIdx) => this.mapCustomItem(child, idx * 100 + childIdx + 1));
      return base;
    }
    if (item.linkType === CMItemLinkType.DASHBOARD && item.dashboardId) {
      base.path = `/dashboards/${item.dashboardId}`;
    } else if (item.url) {
      base.href = item.url;
      base.openInNewTab = true;
    }
    return base;
  }

  private updateOpenedMenuSections() {
    const url = this.router.url;
    const openedMenuSections = getCurrentOpenedMenuSections(this.store);
    if (this.currentMenuSections?.length) {
      this.currentMenuSections.filter(section => section.type === 'toggle' &&
        (url.startsWith(section.path) || openedMenuSections.includes(section.path))).forEach(
        section => section.opened = true
      );
    }
  }

  private allMenuLinks(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      if (section.type === 'link') {
        result.push(section);
      }
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuLinks(section.pages));
      }
    }
    return result;
  }

  private allMenuSections(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      result.push(section);
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuSections(section.pages));
      }
    }
    return result;
  }

  public menuSections(): Observable<Array<MenuSection>> {
    return this.menuSections$;
  }

  public homeSections(): Observable<Array<HomeSection>> {
    return this.homeSections$;
  }

  public availableMenuLinks(): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$;
  }

  public availableMenuSections(): Observable<Array<MenuSection>> {
    return this.availableMenuSections$;
  }

  public menuLinkById(id: MenuId | string): Observable<MenuSection | undefined> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.find(link => link.id === id))
    );
  }

  public menuLinksByIds(ids: string[]): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.filter(link => ids.includes(link.id)).sort((a, b) => {
        const i1 = ids.indexOf(a.id);
        const i2 = ids.indexOf(b.id);
        return i1 - i2;
      }))
    );
  }

}
