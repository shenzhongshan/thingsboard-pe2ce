# Theme and i18n

ThingsBoard's frontend supports 28 locales through a comprehensive internationalization (i18n) system and provides a theme architecture built on Angular Material with Tailwind CSS integration.

## Internationalization (i18n)

### Supported Locales (28)

All locale files are in `src/assets/locale/`, named `locale.constant-<locale>.json`:

| Locale | Code | File |
|--------|------|------|
| Arabic (UAE) | ar_AE | `locale.constant-ar_AE.json` |
| Catalan (Spain) | ca_ES | `locale.constant-ca_ES.json` |
| Czech | cs_CZ | `locale.constant-cs_CZ.json` |
| Danish | da_DK | `locale.constant-da_DK.json` |
| German | de_DE | `locale.constant-de_DE.json` |
| Greek | el_GR | `locale.constant-el_GR.json` |
| English (US) | en_US | `locale.constant-en_US.json` |
| Spanish | es_ES | `locale.constant-es_ES.json` |
| Persian (Iran) | fa_IR | `locale.constant-fa_IR.json` |
| French | fr_FR | `locale.constant-fr_FR.json` |
| Hindi | hi_IN | `locale.constant-hi_IN.json` |
| Italian | it_IT | `locale.constant-it_IT.json` |
| Japanese | ja_JP | `locale.constant-ja_JP.json` |
| Georgian | ka_GE | `locale.constant-ka_GE.json` |
| Korean | ko_KR | `locale.constant-ko_KR.json` |
| Lithuanian | lt_LT | `locale.constant-lt_LT.json` |
| Latvian | lv_LV | `locale.constant-lv_LV.json` |
| Dutch (Belgium) | nl_BE | `locale.constant-nl_BE.json` |
| Dutch (Netherlands) | nl_NL | `locale.constant-nl_NL.json` |
| Norwegian | no_NO | `locale.constant-no_NO.json` |
| Polish | pl_PL | `locale.constant-pl_PL.json` |
| Portuguese (Brazil) | pt_BR | `locale.constant-pt_BR.json` |
| Romanian | ro_RO | `locale.constant-ro_RO.json` |
| Slovenian | sl_SI | `locale.constant-sl_SI.json` |
| Turkish | tr_TR | `locale.constant-tr_TR.json` |
| Ukrainian | uk_UA | `locale.constant-uk_UA.json` |
| Chinese (Simplified) | zh_CN | `locale.constant-zh_CN.json` |
| Chinese (Traditional) | zh_TW | `locale.constant-zh_TW.json` |

### Translation Architecture

The i18n system uses the following key libraries:

- **`@ngx-translate/core`** (~17.0.0): Angular translation framework. Provides the `TranslateService`, `TranslatePipe`, and `TranslateDirective` used throughout templates.
- **`ngx-translate-messageformat-compiler`** (~7.2.0): Enables ICU MessageFormat for complex translations (pluralization, gender, selection).
- **`@messageformat/core`** (~3.4.0): Core ICU message formatting library.

### Core Translation Services

Located in `core/translate/`:

| File | Purpose |
|------|---------|
| `translate-default-loader.ts` | HTTP loader that fetches locale JSON files from `/assets/locale/locale.constant-<lang>.json`. Supports caching and fallback. |
| `translate-default-compiler.ts` | Compiles ICU MessageFormat strings for interpolation. Supports plural forms, gender inflection, and nested selections. |
| `translate-default-parser.ts` | Parses translated strings with parameter interpolation (e.g., `Hello {{name}}`). |
| `missing-translate-handler.ts` | Handles missing translation keys by logging a warning and returning the key itself as display text during development. |

### ICU Message Format

Translations use ICU message syntax for complex formatting:

```json
{
  "item-count": "{count, plural, =0 {No items} one {# item} other {# items}}",
  "welcome": "{gender, select, male {Welcome, Mr. {name}} female {Welcome, Ms. {name}} other {Welcome, {name}}}"
}
```

### NgRx Integration

The current locale is managed in the NgRx settings state (`core/settings/`):

- **`settings.actions.ts`** -- `ActionSettingsChangeLanguage` action dispatches locale changes
- **`settings.reducer.ts`** -- Stores `userLang` and `locale` in state
- **`settings.effects.ts`** -- Side effects: persists locale to localStorage, reloads translations
- **`settings.selectors.ts`** -- `selectUserLang` selector for the current language

On locale change:
1. Dispatch `ActionSettingsChangeLanguage({ userLang })`
2. Effect calls `TranslateService.use(userLang)`
3. `TranslateDefaultLoader` fetches the new locale file
4. All templates using `translate` pipe or directive automatically update

### Template Usage

```html
<!-- Simple translation -->
<h2 translate>device.devices</h2>
<p>{{ 'device.name' | translate }}</p>

<!-- With parameters -->
<span>{{ 'device.created-by' | translate:{user: creatorName} }}</span>

<!-- Using directive with inner HTML -->
<div [translate]="'dashboard.help-text'"></div>
```

## Theme Architecture

### Angular Material Theming

The application uses Angular Material 20's theming system with a custom ThingsBoard theme:

- **Material Design 3 (M3) theming:** Uses CSS custom properties for colors, typography, and density.
- **Custom TB theme:** Built on Angular Material's `define-theme()` API with ThingsBoard-specific color palettes.
- **Component-level theming:** Each component inherits theme variables via CSS custom properties.

### SCSS Architecture

SCSS files are organized alongside their components (`.component.scss`). Global styles include:

- **Theme variables:** Color tokens, spacing, typography scales
- **Material overrides:** Custom density, shape, and color tweaks
- **Utility classes:** Spacing, flexbox, display helpers
- **Layout:** Sidebar, toolbar, content area structure

### Tailwind CSS Integration

Tailwind CSS 3.4.x is integrated alongside Angular Material:

- **Configuration:** `tailwind.config.js` in the project root
- **Directives:** Uses `@tailwind base`, `@tailwind components`, `@tailwind utilities`
- **Preflight disabled:** Tailwind's CSS reset is disabled (`preflight: false`) to avoid conflicts with Angular Material
- **PostCSS:** Uses `postcss` and `autoprefixer` for CSS processing

Key Tailwind classes are available in any component template for rapid layout and styling:

```html
<div class="flex items-center gap-2 p-4">
  <mat-icon class="text-primary">info</mat-icon>
  <span class="text-sm text-gray-600">{{ description }}</span>
</div>
```

### MDI Icons

The application uses Material Design Icons (MDI) via `@mdi/svg` for its icon system. The `tb-icon` component renders MDI icons and supports custom color, size, and hover effects. Font Awesome (`font-awesome`) is also available for backward compatibility.

### SCADA Symbol Theming

SCADA symbols use `@svgdotjs/svg.js` for SVG manipulation. Symbols are themeable and can reference dashboard color constants for dynamic recoloring.

### Typography

- **Primary font:** Roboto (via `typeface-roboto`)
- **Monospace font:** Used in code editors (ACE editor, JSON editors)
- **Font scaling:** Responsive typography via Angular Material's typography scale

## Linting

### ESLint Configuration

Located at `ui-ngx/eslint.config.mjs` (ESLint 9 flat config format):

**Plugins:**
- `angular-eslint` (~20.7.0) -- Angular-specific lint rules (template, component selector, lifecycle hooks)
- `typescript-eslint` (~8.54.0) -- TypeScript lint rules
- `eslint-plugin-import` -- Import order and validation
- `eslint-plugin-jsdoc` -- JSDoc comment validation
- `eslint-plugin-prefer-arrow` -- Arrow function preference
- `eslint-plugin-tailwindcss` -- Tailwind CSS class validation and ordering

**Key rules:**
- Angular template rules: accessibility (alt text, labels), no autofocus, no positive tabindex
- TypeScript rules: prefer `const`, prefer optional chaining, no `any`
- Import rules: ordered imports (Angular first, then third-party, then local)
- Tailwind rules: no duplicate classes, no unnecessary arbitrary values

Run with:
```bash
cd ui-ngx && yarn lint
```

## Build System Details

### Angular CLI with esbuild

The application uses Angular's esbuild-based build system (`@angular/build`) for faster builds:

- **Development:** `ng serve` with HMR (Hot Module Replacement)
- **Production:** `ng build --configuration production` with optimization (minification, tree-shaking, dead code elimination)
- **Type generation:** `yarn build:types` generates TypeScript interfaces from protobuf definitions
- **Icon metadata:** `yarn build:icon-metadata` generates icon lookup data

### Environment Configuration

Environment-specific settings in `src/environments/`:

- `environment.ts` -- Development configuration
- `environment.prod.ts` -- Production configuration

## Relationships

- [Frontend Overview](README.md) -- Overall frontend architecture
- [Core Module](core.md) -- Core translate services and NgRx settings state
- [Shared Module](shared.md) -- Shared components styled with the theme system
- [Page Modules](pages.md) -- Pages use translations and theme variables
