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
import { ImageService } from '@core/http/image.service';
import { materialColorPalette } from '@shared/models/material.models';
import {
  LoginWhiteLabelingParams,
  Palette,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';

const WL_THEME_STYLE_ID = 'tb-white-labeling-theme';
const WL_CUSTOM_STYLE_ID = 'tb-white-labeling-custom-css';
const WL_LOGIN_FG_STYLE_ID = 'tb-white-labeling-login-foreground';

const TB_IMAGE_PREFIX = 'tb-image;';
const IMAGES_API_PREFIX = '/api/images/';

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
  private appTitleValue: string = null;

  constructor(private whiteLabelingService: WhiteLabelingService,
              private imageService: ImageService,
              @Inject(DOCUMENT) private document: Document) {
  }

  public get logo(): string {
    return this.logoSubject.value;
  }

  // White-label platform title, used by TitleService in place of env.appTitle when set.
  public get appTitle(): string {
    return this.appTitleValue;
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
    this.appTitleValue = params.appTitle || null;
    this.applyTitle(params.appTitle);
    this.applyFavicon(params.favicon?.url);
    this.applyLogo(params.logoImageUrl);
    this.applyPalette(params.paletteSettings?.primaryPalette, params.paletteSettings?.accentPalette);
    this.applyCustomCss(params.customCss);
  }

  public applyLoginWhiteLabeling(params: LoginWhiteLabelingParams): void {
    if (!params) {
      return;
    }
    this.currentParams = params;
    this.appTitleValue = params.appTitle || null;
    this.applyTitle(params.appTitle);
    // On login pages there is no JWT yet, so gallery image references can't be resolved through the
    // authenticated /api/images/** endpoints — the backend exposes the exact logo/favicon referenced
    // by the login white-labeling config through dedicated noauth endpoints instead (same as PE).
    this.setFaviconHref(this.noauthLoginImageUrl(params.favicon?.url, true));
    this.logoSubject.next(this.noauthLoginImageUrl(params.logoImageUrl, false));
    this.applyPalette(params.paletteSettings?.primaryPalette, params.paletteSettings?.accentPalette);
    this.applyCustomCss(params.customCss);
    const root = this.document.documentElement;
    if (params.pageBackgroundColor) {
      root.style.setProperty('--tb-login-background-color', params.pageBackgroundColor);
    }
    if (params.logoImageHeight) {
      root.style.setProperty('--tb-login-logo-height', `${params.logoImageHeight}px`);
    }
    this.applyLoginForeground(!!params.darkForeground);
  }

  private noauthLoginImageUrl(url: string, faviconElseLogo: boolean): string {
    if (!url) {
      return null;
    }
    if (url.startsWith(TB_IMAGE_PREFIX)) {
      const ref = url.substring(TB_IMAGE_PREFIX.length);
      if (ref.startsWith(IMAGES_API_PREFIX)) {
        const rest = ref.substring(IMAGES_API_PREFIX.length);
        const slash = rest.indexOf('/');
        if (slash > 0) {
          const type = rest.substring(0, slash);
          const key = rest.substring(slash + 1);
          const endpoint = faviconElseLogo ? 'loginFavicon' : 'loginLogo';
          return `/api/noauth/whiteLabel/${endpoint}/${type}/${encodeURIComponent(key)}`;
        }
      }
      return ref;
    }
    return url;
  }

  private applyTitle(appTitle: string): void {
    if (appTitle) {
      // Preserve the " | <page>" suffix set by TitleService for the current route.
      const current = this.document.title;
      const suffixIdx = current.indexOf(' | ');
      this.document.title = suffixIdx >= 0 ? appTitle + current.substring(suffixIdx) : appTitle;
    }
  }

  private applyFavicon(url: string): void {
    if (!url) {
      return;
    }
    // favicon/logo values may reference a platform image resource (`tb-image;/api/images/...`),
    // which isn't a URL a <link>/<img> can load directly — it must be resolved (auth'd fetch +
    // base64) the same way the rest of the app resolves image resources via ImagePipe/ImageService.
    this.imageService.resolveImageUrl(url, false, true).subscribe((resolvedUrl) => {
      this.setFaviconHref(resolvedUrl as string);
    });
  }

  private setFaviconHref(url: string): void {
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

  private applyLogo(url: string): void {
    if (!url) {
      this.logoSubject.next(null);
      return;
    }
    this.imageService.resolveImageUrl(url, false, true).subscribe((resolvedUrl) => {
      this.logoSubject.next((resolvedUrl as string) || null);
    });
  }

  private applyLoginForeground(dark: boolean): void {
    const css = dark ? `
      .tb-login-content mat-card {
        color: rgba(0,0,0,.87) !important;
        --mdc-outlined-text-field-input-text-color: rgba(0,0,0,.87);
        --mdc-outlined-text-field-label-text-color: rgba(0,0,0,.6);
        --mdc-outlined-text-field-hover-label-text-color: rgba(0,0,0,.87);
        --mdc-outlined-text-field-focus-label-text-color: rgba(0,0,0,.87);
        --mdc-outlined-text-field-outline-color: rgba(0,0,0,.38);
        --mdc-outlined-text-field-hover-outline-color: rgba(0,0,0,.87);
        --mdc-outlined-text-field-focus-outline-color: rgba(0,0,0,.87);
        --mat-button-text-label-text-color: rgba(0,0,0,.87);
        --mdc-icon-button-icon-color: rgba(0,0,0,.87);
      }
      .tb-login-content mat-card .mat-icon,
      .tb-login-content mat-card .mat-mdc-button:not(:disabled),
      .tb-login-content mat-card .oauth-container .title {
        color: rgba(0,0,0,.87) !important;
      }
    ` : '';
    this.injectStyle(WL_LOGIN_FG_STYLE_ID, css);
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
        .tb-login-content mat-card { background-color: ${primaryColor} !important; }
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
