# Frontend Architecture

The ThingsBoard frontend is a single-page application built with Angular 20 and Angular Material 20. It provides a rich UI for managing IoT devices, assets, dashboards, rule chains, alarms, and all other ThingsBoard entities.

The frontend communicates with the backend via REST APIs and WebSocket connections, using JWT tokens for authentication.

## Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Angular | 20.3.x | Frontend framework |
| Angular Material | 20.2.x | UI component library (Material Design) |
| TypeScript | 5.9.x | Primary language |
| RxJS | 7.8.x | Reactive programming, async data streams |
| NgRx Store | 20.1.x | State management (Redux pattern) |
| @ngx-translate/core | 17.x | Internationalization |
| @messageformat/core | 3.4.x | ICU message format support |
| Tailwind CSS | 3.4.x | Utility-first CSS framework |
| ESLint | 9.x | Linting (with Angular ESLint + TypeScript ESLint) |
| Karma + Jasmine | -- | Unit testing |

## Build and Development

```bash
# Install dependencies
cd ui-ngx && yarn install

# Development server at http://localhost:4200
yarn start

# Production build
yarn build:prod

# Type generation (protobuf/API types)
yarn build:types

# Linting
yarn lint
```

The development server proxies API requests to a running backend (default `http://localhost:8080`).

## Module Organization

```
src/app/
├── core/                       # Singleton services, guards, interceptors
│   ├── api/                    # WebSocket-based data subscription APIs
│   ├── auth/                   # Authentication (JWT, OAuth2, 2FA)
│   ├── guards/                 # Route guards
│   ├── http/                   # HTTP services (one per entity type)
│   ├── interceptors/           # HTTP interceptors
│   ├── local-storage/          # Browser localStorage wrapper
│   ├── meta-reducers/          # NgRx meta-reducers
│   ├── notification/           # In-app notification state (NgRx)
│   ├── services/               # Core services (menu, time, dialog, etc.)
│   ├── settings/               # User settings state (NgRx)
│   ├── translate/              # i18n translation services
│   ├── ws/                     # WebSocket services
│   └── core.module.ts          # Core module definition
│
├── modules/
│   ├── home/                   # Main authenticated application
│   │   ├── components/          # Reusable home components
│   │   │   ├── entity/          # Entities table, entity details, entity form base
│   │   │   ├── alarm/           # Alarm table, alarm details
│   │   │   ├── dashboard/       # Dashboard viewer, toolbar
│   │   │   ├── widget/          # Widget editor
│   │   │   └── ...
│   │   └── pages/              # Page modules (32 page directories)
│   └── login/                  # Login/reset password/2FA pages
│
└── shared/
    ├── components/              # Shared UI components (20+ groups)
    ├── models/                  # TypeScript interfaces, enums, constants
    ├── pipe/                    # Custom pipes
    ├── directives/              # Custom directives
    ├── decorators/              # Custom decorators
    ├── services/                # Shared utility services
    ├── import-export/           # Data import/export utilities
    ├── adapter/                 # Custom adapters
    └── animations/              # Animation definitions
```

## Path Aliases

Defined in `tsconfig.json`:

| Alias | Maps to |
|-------|---------|
| `@app/*` | `src/app/*` |
| `@env/*` | `src/environments/*` |
| `@core/*` | `src/app/core/*` |
| `@modules/*` | `src/app/modules/*` |
| `@home/*` | `src/app/modules/home/*` |
| `@shared/*` | `src/app/shared/*` |

Import example: `import { DeviceService } from '@core/http/device.service';`

## Key Dependencies

### Core Framework

- `@angular/core`, `@angular/common`, `@angular/router`, `@angular/forms`
- `@angular/material`, `@angular/cdk` -- Material Design components
- `@angular/animations` -- Animation support

### State Management

- `@ngrx/store`, `@ngrx/effects`, `@ngrx/store-devtools` -- Redux state management
- Auth state (`auth.reducer.ts`), settings state (`settings.reducer.ts`), notification state (`notification.reducer.ts`), loading state (`load.reducer.ts`)

### i18n

- `@ngx-translate/core`, `ngx-translate-messageformat-compiler` -- Translation with ICU message format
- `@messageformat/core` -- ICU message formatting

### Visualization

- `echarts` -- Charts (custom ThingsBoard fork at 5.5.2-TB)
- `@svgdotjs/svg.js` -- SVG manipulation for SCADA symbols
- `canvas-gauges` -- Analog gauge widgets
- `leaflet` -- Map widgets
- `maplibre-gl` -- MapLibre GL map rendering
- `flot` -- jQuery-based plotting (legacy)

### Editor Components

- `ace-builds` -- Code editor for rule node scripts
- `tinymce` -- Rich text editor
- `ngx-markdown`, `marked` -- Markdown rendering
- `angular-gridster2` -- Dashboard grid layout

### Utilities

- `dayjs`, `moment`, `moment-timezone` -- Date/time handling
- `rxjs` -- Reactive extensions
- `jszip` -- ZIP file generation
- `html2canvas` -- Screenshot capture
- `qrcode` -- QR code generation
- `screenfull` -- Fullscreen API wrapper
- `split.js` -- Resizable split panels
- `libphonenumber-js` -- Phone number validation
- `tinycolor2` -- Color manipulation

## The Entity Table Pattern

Most ThingsBoard pages follow a standardized **entity table pattern** with a 3-file structure per entity type:

```
pages/<entity>/
├── <entity>-table-config.resolver.ts   # Column definitions, actions, component wiring
├── <entity>.component.ts + .html       # Entity detail form (extends EntityComponent<T>)
└── <entity>-tabs.component.ts + .html  # Additional detail tabs
```

### Framework Components

| Component | File | Purpose |
|-----------|------|---------|
| `tb-entities-table` | `home/components/entity/entities-table.component.ts` | Generic table: renders columns, actions, pagination, search, row selection |
| `tb-entity-details-page` | `home/components/entity/entity-details-page.component.ts` | Full-page entity details with tabs |
| `tb-entity-details-panel` | `home/components/entity/entity-details-panel.component.ts` | Slide-out drawer for entity details |
| `EntityComponent<T>` | `home/components/entity/entity.component.ts` | Abstract base directive all entity forms extend |
| `TableConfigResolver` | -- | Interface for table configuration providers |

### Resolver Pattern

Each entity type has a `TableConfigResolver` that returns an `EntityTableConfig<T>` containing:

- `entityType` -- enum value (e.g., `EntityType.DEVICE`)
- `columns` -- array of column definitions (header key, cell property/template, width, sorting)
- `cellActionDescriptors` -- per-row action buttons (edit, delete, etc.)
- `groupActionDescriptors` -- bulk-selection action buttons
- `headerActionDescriptors` -- toolbar action buttons (add, import, refresh)
- `entityComponent` -- reference to the detail form component class
- `entityTabsComponent` -- reference to the tabs component class
- `addDialogComponent` -- optional dialog for add/create flow

### Entity Detail Form Lifecycle

All entity detail forms extend `EntityComponent<T>` and follow this lifecycle:

1. **`buildForm(entity: T): UntypedFormGroup`** -- Called in constructor. Creates the reactive form with controls, validators, and initial values.
2. **`updateForm(entity: T): void`** -- Called when entity input changes. Uses `patchValue()` to populate the form without triggering change events.
3. **`updateFormState(): void`** -- Called when `isEdit` changes. Enables or disables the form.
4. **`entityFormValue(): any`** -- Returns `form.getRawValue()` (includes disabled fields), passed through `prepareFormValue()`.
5. **Saving:** Merges `{...this.entity, ...this.entityFormValue()}` and calls the HTTP service.

Forms use `[fieldset [disabled]="isEntityReadonly"]` for visual readonly state, with `entityForm.disable()` for programmatic enforcement.

## WebSocket Communication

The frontend maintains persistent WebSocket connections for real-time updates:

| Service | File | Purpose |
|---------|------|---------|
| `TelemetryWebsocketService` | `core/ws/telemetry-websocket.service.ts` | Real-time telemetry data subscriptions for dashboards |
| `NotificationWebsocketService` | `core/ws/notification-websocket.service.ts` | Real-time notification updates |
| `WebSocketService` | `core/ws/websocket.service.ts` | Base WebSocket connection management |

## Routing Architecture

Routes are defined in two levels:
- **Top-level routing** (`app-routing.module.ts`): Login vs. home (authenticated)
- **Home routing** (`modules/home/pages/home-links/`): Lazy-loaded feature modules

The `home-links/` module defines the sidebar navigation menu and lazy-loads page modules based on user authority (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER).

## Relationships

- [Core Module](core.md) -- Singleton services, guards, interceptors, auth
- [Shared Module](shared.md) -- Reusable components, models, pipes, directives
- [Page Modules](pages.md) -- All 32 page modules and their structure
- [Theme & i18n](theme-i18n.md) -- SCSS theming, Tailwind integration, 28 locales
- [Core Application](../50-application/README.md) -- Backend REST API consumed by frontend
- [Supporting Modules](../60-supporting/README.md) -- web-ui Docker image packaging
