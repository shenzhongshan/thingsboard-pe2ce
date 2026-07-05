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

import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { materialColorPalette } from '@shared/models/material.models';
import {
  LoginWhiteLabelingParams,
  Palette,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';

const WL_THEME_STYLE_ID = 'tb-white-labeling-theme';
const WL_CUSTOM_STYLE_ID = 'tb-white-labeling-custom-css';

/**
 * Applies white-labeling params to the running application at runtime.
 *
 * CE bakes Angular Material palettes at compile time (theme.scss), so we cannot fully swap
 * the Material theme at runtime. Instead we override the most visible branding surfaces via
 * CSS custom properties + an injected stylesheet, and expose the logo through an observable
 * consumed by the toolbar/login components. This mirrors the visible effect of PE white labeling
 * (logo, title, favicon, primary color, custom CSS) without a full runtime Material rebuild.
 */
@Injectable({
  providedIn: 'root'
})
export class WhiteLabelingApplyService {

  // null => no white-label logo configured; consumers fall back to their own default logo.
  private readonly logoSubject = new BehaviorSubject<string>(null);
  public readonly logo$: Observable<string> = this.logoSubject.asObservable();

  private currentParams: WhiteLabelingParams = null;

  constructor(private whiteLabelingService: WhiteLabelingService,
              @Inject(DOCUMENT) private document: Document) {
  }

  public get logo(): string {
    return this.logoSubject.value;
  }

  public loadAndApply(): Observable<WhiteLabelingParams> {
    return this.whiteLabelingService.getMergedWhiteLabelParams({ ignoreErrors: true, ignoreLoading: true }).pipe(
      tap((params) => this.applyWhiteLabeling(params)),
      catchError(() => of(null))
    );
  }

  public loadAndApplyLogin(): Observable<LoginWhiteLabelingParams> {
    return this.whiteLabelingService.getLoginWhiteLabelParams({ ignoreErrors: true, ignoreLoading: true }).pipe(
      tap((params) => this.applyLoginWhiteLabeling(params)),
      catchError(() => of(null))
    );
  }

  public applyWhiteLabeling(params: WhiteLabelingParams): void {
    if (!params) {
      return;
    }
    this.currentParams = params;
    this.applyTitle(params.appTitle);
    this.applyFavicon(params.favicon?.url);
    this.logoSubject.next(params.logoImageUrl || null);
    this.applyPalette(params.paletteSettings?.primaryPalette, params.paletteSettings?.accentPalette);
    this.applyCustomCss(params.customCss);
  }

  public applyLoginWhiteLabeling(params: LoginWhiteLabelingParams): void {
    if (!params) {
      return;
    }
    this.applyWhiteLabeling(params);
    if (params.pageBackgroundColor) {
      this.document.documentElement.style.setProperty('--tb-login-background-color', params.pageBackgroundColor);
    }
  }

  private applyTitle(appTitle: string): void {
    if (appTitle) {
      this.document.title = appTitle;
    }
  }

  private applyFavicon(url: string): void {
    if (!url) {
      return;
    }
    let link: HTMLLinkElement = this.document.querySelector('link[rel~="icon"]');
    if (!link) {
      link = this.document.createElement('link');
      link.rel = 'icon';
      this.document.head.appendChild(link);
    }
    link.href = url;
  }

  private paletteColor(palette: Palette, hue: string): string {
    if (!palette) {
      return null;
    }
    if (palette.colors && palette.colors[hue]) {
      return palette.colors[hue];
    }
    // Predefined Material palette, or a custom palette falling back to its base.
    const paletteKey = palette.type === 'custom' ? palette.extends : palette.type;
    return materialColorPalette[paletteKey]?.[hue] || null;
  }

  private applyPalette(primary: Palette, accent: Palette): void {
    const primaryColor = this.paletteColor(primary, '500');
    const accentColor = this.paletteColor(accent, '500');
    const root = this.document.documentElement;
    let css = '';
    if (primaryColor) {
      root.style.setProperty('--tb-primary-500', primaryColor);
      css += `
        .mat-toolbar.mat-primary { background-color: ${primaryColor} !important; }
        .mat-mdc-raised-button.mat-primary, .mat-mdc-unelevated-button.mat-primary,
        .mat-mdc-fab.mat-primary, .mat-mdc-mini-fab.mat-primary { background-color: ${primaryColor} !important; }
        .mat-mdc-button.mat-primary, .mat-mdc-icon-button.mat-primary { --mdc-text-button-label-text-color: ${primaryColor}; color: ${primaryColor} !important; }
      `;
    }
    if (accentColor) {
      root.style.setProperty('--tb-accent-500', accentColor);
      css += `
        .mat-mdc-raised-button.mat-accent, .mat-mdc-unelevated-button.mat-accent,
        .mat-mdc-fab.mat-accent, .mat-mdc-mini-fab.mat-accent { background-color: ${accentColor} !important; }
      `;
    }
    this.injectStyle(WL_THEME_STYLE_ID, css);
  }

  private applyCustomCss(customCss: string): void {
    this.injectStyle(WL_CUSTOM_STYLE_ID, customCss || '');
  }

  private injectStyle(id: string, css: string): void {
    let style: HTMLStyleElement = this.document.getElementById(id) as HTMLStyleElement;
    if (!css) {
      if (style) {
        style.remove();
      }
      return;
    }
    if (!style) {
      style = this.document.createElement('style');
      style.id = id;
      style.type = 'text/css';
      this.document.head.appendChild(style);
    }
    style.textContent = css;
  }
}
