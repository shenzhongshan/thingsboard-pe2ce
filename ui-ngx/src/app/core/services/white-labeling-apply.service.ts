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
import { catchError, distinctUntilChanged, map, tap } from 'rxjs/operators';
import tinycolor from 'tinycolor2';
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

type PaletteColors = {[hue: string]: string};

/**
 * Colors to swap while re-emitting the compiled theme: keyed by the lowercase hex literal and by
 * the `r,g,b` triple, since the Sass output writes the same color in both notations.
 */
interface ColorSubstitutions {
  hex: Map<string, string>;
  rgb: Map<string, string>;
}

/** A `#rrggbb` literal, or the color part of an `rgb()`/`rgba()` literal. */
const colorLiteralRegex = /#[0-9a-f]{6}(?![0-9a-f])|(rgba?\()\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*(?=[,)])/gi;

/**
 * The palettes `theme.scss` compiles the Material theme with: `$tb-mat-indigo` for primary and the
 * stock Material deep-orange for accent. Every color below appears literally in the generated
 * stylesheet, which is what makes runtime re-theming by substitution possible.
 */
const defaultPrimaryPaletteColors: PaletteColors = {
  50: '#e8eaf6',
  100: '#c5cae9',
  200: '#9fa8da',
  300: '#7986cb',
  400: '#5c6bc0',
  500: '#305680',
  600: '#527dad',
  700: '#303f9f',
  800: '#283593',
  900: '#1a237e',
  A100: '#a7c1de',
  A200: '#536dfe',
  A400: '#3d5afe',
  A700: '#304ffe'
};

const defaultAccentPaletteColors: PaletteColors = materialColorPalette['deep-orange'];

const contrastColor = (color: string): string =>
  tinycolor(color).isLight() ? 'rgba(0, 0, 0, 0.87)' : '#ffffff';

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

  // Emits every time white-labeling params are (re)applied, so that components bound to
  // white-labeling driven flags (help links, ...) refresh without a page reload.
  private readonly paramsSubject = new BehaviorSubject<WhiteLabelingParams>(null);
  public readonly params$: Observable<WhiteLabelingParams> = this.paramsSubject.asObservable();

  private currentParams: WhiteLabelingParams = null;
  private appTitleValue: string = null;
  private appliedPalettes: { primary: Palette; accent: Palette } = null;
  private stylesheetObserver: MutationObserver = null;
  private recolorScheduled = false;

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
    this.paramsSubject.next(params);
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
    this.paramsSubject.next(params);
  }

  /**
   * White labeling "Enable help links" flag. Defaults to true when white labeling has not been
   * loaded yet or the flag was never configured (the system-level params always carry it).
   */
  public isEnableHelpLinks(): boolean {
    return this.currentParams ? this.currentParams.enableHelpLinks !== false : true;
  }

  public isEnableHelpLinks$(): Observable<boolean> {
    return this.params$.pipe(
      map(() => this.isEnableHelpLinks()),
      distinctUntilChanged()
    );
  }

  // Base URL that replaces the built-in documentation host in every help link.
  public getHelpLinkBaseUrl(): string {
    return this.currentParams?.helpLinkBaseUrl || '';
  }

  // Base URL the in-app help (markdown) content is loaded from.
  public getUiHelpBaseUrl(): string {
    return this.currentParams?.uiHelpBaseUrl || '';
  }

  public getUiHelpBaseUrl$(): Observable<string> {
    return this.params$.pipe(
      map(() => this.getUiHelpBaseUrl()),
      distinctUntilChanged()
    );
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

  /**
   * Full hue spectrum of a configured palette: a custom palette is its `extends` base with the
   * edited hues applied on top, a predefined one is the Material palette itself, and anything
   * unset falls back to the palette the theme was compiled with.
   */
  private resolvePaletteColors(palette: Palette, fallback: PaletteColors): PaletteColors {
    if (!palette?.type || palette.type === 'tb-primary' || palette.type === 'tb-accent') {
      return fallback;
    }
    if (palette.type === 'custom') {
      const base = materialColorPalette[palette.extends] || fallback;
      return { ...base, ...(palette.colors || {}) };
    }
    return materialColorPalette[palette.type] || fallback;
  }

  private applyPalette(primary: Palette, accent: Palette): void {
    this.appliedPalettes = { primary, accent };
    const primaryColors = this.resolvePaletteColors(primary, defaultPrimaryPaletteColors);
    const accentColors = this.resolvePaletteColors(accent, defaultAccentPaletteColors);
    // PE exposes the whole spectrum as CSS variables (--tb-primary-<hue>/--tb-primary-contrast-<hue>);
    // keep that contract so component styles can consume them.
    this.applyPaletteVariables('--tb-primary-', primaryColors);
    this.applyPaletteVariables('--tb-accent-', accentColors);
    const substitutions: ColorSubstitutions = { hex: new Map(), rgb: new Map() };
    this.collectPaletteSubstitutions(substitutions, defaultPrimaryPaletteColors, primaryColors);
    this.collectPaletteSubstitutions(substitutions, defaultAccentPaletteColors, accentColors);
    let css = '';
    if (substitutions.hex.size) {
      // CE compiles the Material theme with Sass (m2-define-palette), so every themed component
      // carries the default palette colors as literal values in ~90 rules of styles.css. Re-emit
      // exactly those declarations with the configured colors substituted; the generated sheet is
      // appended last, so at equal specificity it wins over the compiled theme.
      css = this.recolorThemeCss(substitutions) + this.paletteContrastCss(primaryColors, accentColors);
      // Lazy-loaded feature modules bring their own stylesheets, dozens of which hardcode the
      // brand color; re-run the substitution whenever new ones are appended.
      this.observeStylesheets();
    }
    this.injectStyle(WL_THEME_STYLE_ID, css);
  }

  private observeStylesheets(): void {
    if (this.stylesheetObserver) {
      return;
    }
    this.stylesheetObserver = new MutationObserver((mutations) => {
      const themeStylesAdded = mutations.some((mutation) =>
        Array.from(mutation.addedNodes).some((node) => {
          const element = node as HTMLElement;
          return (element.tagName === 'STYLE' || element.tagName === 'LINK') &&
            !(element.id || '').startsWith('tb-white-labeling');
        }));
      if (themeStylesAdded && !this.recolorScheduled) {
        this.recolorScheduled = true;
        setTimeout(() => {
          this.recolorScheduled = false;
          if (this.appliedPalettes) {
            this.applyPalette(this.appliedPalettes.primary, this.appliedPalettes.accent);
          }
        });
      }
    });
    this.stylesheetObserver.observe(this.document.head, { childList: true });
  }

  private applyPaletteVariables(prefix: string, colors: PaletteColors): void {
    const root = this.document.documentElement;
    for (const hue of Object.keys(colors)) {
      root.style.setProperty(`${prefix}${hue}`, colors[hue]);
      root.style.setProperty(`${prefix}contrast-${hue}`, contrastColor(colors[hue]));
    }
  }

  private collectPaletteSubstitutions(substitutions: ColorSubstitutions,
                                      defaults: PaletteColors, colors: PaletteColors): void {
    for (const hue of Object.keys(defaults)) {
      const from = defaults[hue];
      const to = colors[hue];
      if (!to || from.toLowerCase() === to.toLowerCase()) {
        continue;
      }
      const fromRgb = tinycolor(from).toRgb();
      const toRgb = tinycolor(to).toRgb();
      substitutions.hex.set(from.toLowerCase(), to);
      substitutions.rgb.set(`${fromRgb.r},${fromRgb.g},${fromRgb.b}`, `${toRgb.r}, ${toRgb.g}, ${toRgb.b}`);
    }
  }

  private recolorThemeCss(substitutions: ColorSubstitutions): string {
    // Single pass over each value: a sequential replace could re-substitute a color it just wrote
    // (e.g. when the configured primary happens to be the default accent color).
    const substitute = (value: string): string =>
      value.replace(colorLiteralRegex, (match, fn, r, g, b) => {
        if (!fn) {
          return substitutions.hex.get(match.toLowerCase()) || match;
        }
        const replacement = substitutions.rgb.get(`${parseInt(r, 10)},${parseInt(g, 10)},${parseInt(b, 10)}`);
        return replacement ? `${fn}${replacement}` : match;
      });
    let css = '';
    const wrap = (body: string, condition: string): string => condition ? `${condition}{${body}}` : body;
    const collect = (rules: CSSRuleList, condition: string): void => {
      for (let i = 0; i < rules.length; i++) {
        const rule = rules[i] as CSSStyleRule & { conditionText?: string; cssRules?: CSSRuleList };
        if (rule.selectorText && rule.style) {
          const declarations: string[] = [];
          for (let j = 0; j < rule.style.length; j++) {
            const property = rule.style.item(j);
            const value = rule.style.getPropertyValue(property);
            const substituted = substitute(value);
            if (substituted !== value) {
              const priority = rule.style.getPropertyPriority(property) ? ' !important' : '';
              declarations.push(`${property}:${substituted}${priority};`);
            }
          }
          if (declarations.length) {
            css += wrap(`${rule.selectorText}{${declarations.join('')}}`, condition);
          }
        } else if (rule.cssRules) {
          let nested = condition;
          if (rule instanceof CSSMediaRule) {
            nested = `@media ${rule.conditionText}`;
          } else if (typeof CSSSupportsRule !== 'undefined' && rule instanceof CSSSupportsRule) {
            nested = `@supports ${rule.conditionText}`;
          }
          collect(rule.cssRules, nested);
        }
      }
    };
    for (const sheet of Array.from(this.document.styleSheets)) {
      const ownerId = (sheet.ownerNode as HTMLElement)?.id;
      if (ownerId && ownerId.startsWith('tb-white-labeling')) {
        continue;
      }
      let rules: CSSRuleList;
      try {
        rules = sheet.cssRules;
      } catch {
        continue; // cross-origin stylesheet
      }
      collect(rules, '');
    }
    return css;
  }

  /**
   * Surfaces painted with the primary color that the compiled theme gives a fixed foreground
   * (white) — unreadable once a light palette is configured — plus the login card, which is not
   * a themed Material component.
   */
  private paletteContrastCss(primaryColors: PaletteColors, _accentColors: PaletteColors): string {
    const primaryColor = primaryColors['500'];
    if (!primaryColor) {
      return '';
    }
    const contrast = contrastColor(primaryColor);
    return `
      .mat-toolbar.mat-primary { --mat-toolbar-container-text-color: ${contrast}; color: ${contrast}; }
      .mat-mdc-raised-button.mat-primary, .mat-mdc-unelevated-button.mat-primary,
      .mat-mdc-fab.mat-primary, .mat-mdc-mini-fab.mat-primary { color: ${contrast}; }
      .tb-login-content mat-card { background-color: ${primaryColor} !important; }
    `;
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
    }
    style.textContent = css;
    // Angular appends the styles of lazily-loaded feature modules to <head>; re-append ours so they
    // stay last and keep winning against same-specificity rules from those modules.
    this.document.head.appendChild(style);
    for (const lastStyleId of [WL_CUSTOM_STYLE_ID, WL_LOGIN_FG_STYLE_ID]) {
      const lastStyle = lastStyleId === id ? null : this.document.getElementById(lastStyleId);
      if (lastStyle) {
        this.document.head.appendChild(lastStyle);
      }
    }
  }
}
