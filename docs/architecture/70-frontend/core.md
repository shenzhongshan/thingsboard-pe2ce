# Core Module

The Core module (`src/app/core/`) contains singleton services, state management, HTTP interceptors, route guards, and authentication logic. It is imported once in `AppModule` and provides services used throughout the application.

## Module Structure

```
src/app/core/
├── api/                    # WebSocket-based data subscription APIs
├── auth/                   # Authentication state (NgRx) and service
├── css/                    # CSS utility constants
├── guards/                 # Route guards (auth, unsaved changes)
├── http/                   # HTTP services (44 entity services)
├── interceptors/           # HTTP interceptors (auth token, loading, conflict)
├── local-storage/          # Browser localStorage wrapper
├── meta-reducers/          # NgRx meta-reducers (debug, localStorage sync)
├── notification/           # In-app notification state (NgRx)
├── operator/               # RxJS custom operators
├── services/               # Application-wide services (menu, time, dialog, etc.)
├── settings/               # User settings state (NgRx)
├── translate/              # i18n translation loader, compiler, parser
├── ws/                     # WebSocket services
├── core.module.ts          # CoreModule definition
├── core.state.ts           # Core NgRx state interface
├── public-api.ts           # Public API barrel
└── utils.ts                # General utility functions
```

## api/ -- WebSocket Data Subscriptions

Provides real-time data streaming via WebSocket for widgets and dashboards. The API layer manages subscriptions to entity telemetry and alarm data.

| File | Purpose |
|------|---------|
| `entity-data.service.ts` | Main service for subscribing to entity telemetry/attribute data |
| `entity-data-subscription.ts` | Subscription management for entity data streams |
| `alarm-data.service.ts` | Service for subscribing to alarm data streams |
| `alarm-data-subscription.ts` | Subscription management for alarm streams |
| `data-aggregator.ts` | Aggregates telemetry data by time window |
| `alias-controller.ts` | Resolves entity aliases to actual entity IDs |
| `widget-subscription.ts` | Widget-level subscription wrapper |
| `widget-api.models.ts` | TypeScript interfaces for widget API |
| `public-api.ts` | Public API barrel |

## auth/ -- Authentication

Manages login, logout, JWT token handling, OAuth2, and two-factor authentication via NgRx store.

| File | Purpose |
|------|---------|
| `auth.service.ts` | Authentication service: login, logout, token refresh, OAuth2, 2FA |
| `auth.reducer.ts` | Auth state reducer (isAuthenticated, user details, token) |
| `auth.actions.ts` | NgRx actions for auth operations |
| `auth.effects.ts` | Side effects: token persistence, redirect after login/logout |
| `auth.selectors.ts` | Selectors for auth state queries |
| `auth.models.ts` | Auth-related interfaces (login request/response) |
| `public-api.ts` | Public API barrel |

**Authentication flow:**
1. User submits login form --> AuthService.login()
2. Backend validates and returns JWT token
3. Token stored in AuthState (NgRx) and localStorage
4. AuthHttpInterceptor attaches token to all HTTP requests
5. On token expiry, interceptor triggers token refresh or redirects to login

**OAuth2 and 2FA:** The auth service also handles OAuth2 login flows (redirect to provider, callback handling) and two-factor authentication (TOTP, backup codes).

## guards/ -- Route Guards

| File | Purpose |
|------|---------|
| `auth.guard.ts` | Prevents unauthenticated access; redirects to login. Checks for valid JWT and user authority. |
| `confirm-on-exit.guard.ts` | Prompts for confirmation when leaving a form with unsaved changes. Used by settings-form pages. |

## http/ -- HTTP Services

44 service files providing typed HTTP methods for each entity type. Services extend a common base pattern: they inject `HttpClient` and construct API URLs from a base path.

| Service | Entity/Area |
|---------|------------|
| `device.service.ts` | Device CRUD, credentials, telemetry, attributes |
| `device-profile.service.ts` | Device profile CRUD, transport configuration |
| `asset.service.ts` | Asset CRUD, telemetry, attributes |
| `asset-profile.service.ts` | Asset profile CRUD |
| `entity-view.service.ts` | Entity view CRUD |
| `customer.service.ts` | Customer CRUD |
| `user.service.ts` | User CRUD, activation |
| `tenant.service.ts` | Tenant CRUD (SYS_ADMIN) |
| `tenant-profile.service.ts` | Tenant profile CRUD |
| `dashboard.service.ts` | Dashboard CRUD, publish to customer |
| `rule-chain.service.ts` | Rule chain CRUD, import/export, test script |
| `edge.service.ts` | Edge instance CRUD, sync |
| `alarm.service.ts` | Alarm queries, ack, clear |
| `alarm-comment.service.ts` | Alarm comment CRUD |
| `alarm-rules.service.ts` | Alarm rules / calculated fields for alarm |
| `ota-package.service.ts` | OTA package CRUD, firmware upload |
| `calculated-fields.service.ts` | Calculated fields CRUD |
| `widget.service.ts` | Widget type and widget bundle CRUD |
| `resource.service.ts` | Resource CRUD, download |
| `queue.service.ts` | Queue configuration (SYS_ADMIN) |
| `ai-model.service.ts` | AI model CRUD |
| `notification.service.ts` | Notification CRUD, inbox |
| `oauth2.service.ts` | OAuth2 client/domain CRUD |
| `domain.service.ts` | Domain CRUD |
| `mobile-app.service.ts` | Mobile app CRUD |
| `entity.service.ts` | Generic entity operations (find by ID, count, etc.) |
| `entity-relation.service.ts` | Entity relation CRUD |
| `attribute.service.ts` | Entity attribute CRUD |
| `event.service.ts` | Entity event queries |
| `audit-log.service.ts` | Audit log queries |
| `admin.service.ts` | System/admin settings (mail, SMS, general, security) |
| `api-key.service.ts` | API key management |
| `image.service.ts` | Image gallery upload/download |
| `component-descriptor.service.ts` | Rule node component descriptors |
| `entities-version-control.service.ts` | Version control for entities |
| `two-factor-authentication.service.ts` | 2FA account settings |
| `user-settings.service.ts` | User preference settings |
| `usage-info.service.ts` | API usage statistics |
| `trendz-settings.service.ts` | Trendz analytics settings |
| `git-hub.service.ts` | GitHub integration |
| `mobile-application.service.ts` | Mobile application settings |
| `ui-settings.service.ts` | UI preferences |
| `http-utils.ts` | HTTP utility functions (URL building, param encoding) |
| `public-api.ts` | Public API barrel |

**Pattern:** Each service constructs API paths like `/api/devices/${deviceId}` and returns typed Observables. Example pattern from a typical service method:

```typescript
getDevice(deviceId: string): Observable<Device> {
  return this.http.get<Device>(`/api/device/${deviceId}`, defaultHttpOptionsFromConfig(config));
}
```

## interceptors/ -- HTTP Interceptors

| File | Purpose |
|------|---------|
| `global-http-interceptor.ts` | Master interceptor that delegates to typed sub-interceptors. Handles auth token injection, error processing, loading state. |
| `interceptor-config.ts` | Configuration model for interceptors (ignoreLoading, ignoreErrors, etc.) |
| `interceptor-http-params.ts` | Custom HttpParams with interceptor configuration flags |
| `interceptor.util.ts` | Interceptor utility functions |
| `entity-conflict.interceptor.ts` | Detects 409 Conflict responses and opens entity conflict resolution dialog |
| `load.actions.ts` | NgRx actions for loading state |
| `load.reducer.ts` | Loading state reducer |
| `load.selectors.ts` | Selectors for loading state |
| `load.models.ts` | Loading state models |

The global interceptor chain:
1. **Auth token injection** -- Attaches JWT from state to Authorization header
2. **Loading indicator** -- Dispatches loading state changes for long requests
3. **Error handling** -- Processes HTTP errors, shows toast notifications
4. **Entity conflict** -- Detects 409 errors and opens resolution dialog

## services/ -- Application Services

| File | Purpose |
|------|---------|
| `menu.service.ts` | Builds sidebar navigation menu based on user authority |
| `menu.models.ts` | Menu item interfaces (section, page, link) |
| `time.service.ts` | Manages timewindow state (realtime/last N/time range) |
| `dialog.service.ts` | Dialog open/settings management |
| `toast-notification.service.ts` | Toast notification display (success, error, warning, info) |
| `broadcast.service.ts` | Application-wide event bus for cross-component communication |
| `broadcast.models.ts` | Broadcast event type models |
| `utils.service.ts` | General utility functions (deep clone, guid, etc.) |
| `window.service.ts` | Window/DOM utility service with SSR-safe checks |
| `item-buffer.service.ts` | Clipboard-style buffer for copying entities between pages |
| `help.service.ts` | Inline help content management |
| `dynamic-component-factory.service.ts` | Runtime dynamic component creation |
| `resources.service.ts` | Resource file loading (CSS, JS) |
| `title.service.ts` | Page title management |
| `mobile.service.ts` | Mobile-specific UI adjustments |
| `dashboard-utils.service.ts` | Dashboard utility functions |
| `calculated-field-form.service.ts` | Calculated field form builder |
| `active-component.service.ts` | Tracks the currently active component |
| `unit.service.ts` | Measurement unit conversion and formatting |
| `raf.service.ts` | requestAnimationFrame wrapper |
| `script/node-script-test.service.ts` | Rule node script testing |
| `public-api.ts` | Public API barrel |

## settings/ -- User Settings (NgRx)

| File | Purpose |
|------|---------|
| `settings.reducer.ts` | Settings state reducer (locale, user preferences) |
| `settings.actions.ts` | Settings NgRx actions |
| `settings.effects.ts` | Settings side effects |
| `settings.selectors.ts` | Settings selectors |
| `settings.models.ts` | Settings state models |
| `settings.utils.ts` | Settings utility functions |

## notification/ -- In-App Notification State (NgRx)

| File | Purpose |
|------|---------|
| `notification.reducer.ts` | Notification state reducer |
| `notification.actions.ts` | Notification NgRx actions |
| `notification.effects.ts` | Notification side effects |
| `notification.models.ts` | Notification models |

## ws/ -- WebSocket Services

| File | Purpose |
|------|---------|
| `websocket.service.ts` | Base WebSocket service: connection management, authentication, reconnection, message dispatch |
| `telemetry-websocket.service.ts` | Telemetry WebSocket: subscribes to entity time-series data for real-time dashboard updates |
| `notification-websocket.service.ts` | Notification WebSocket: subscribes to real-time notification events and count updates |
| `public-api.ts` | Public API barrel |

The WebSocket service maintains an authenticated connection using the JWT token. It supports automatic reconnection with exponential backoff. Messages are dispatched to the appropriate subscription handlers based on subscription ID routing.

## translate/ -- i18n Translation

| File | Purpose |
|------|---------|
| `translate-default-loader.ts` | Loads locale JSON files from `/assets/locale/` |
| `translate-default-compiler.ts` | Compiles ICU MessageFormat translation strings |
| `translate-default-parser.ts` | Parses translated strings with parameter interpolation |
| `missing-translate-handler.ts` | Handles missing translation keys (logs warning, returns key) |

## local-storage/ -- Browser Storage

| File | Purpose |
|------|---------|
| `local-storage.service.ts` | Typed wrapper around `window.localStorage` with JSON serialization |

## meta-reducers/ -- NgRx Meta-Reducers

| File | Purpose |
|------|---------|
| `init-state-from-local-storage.reducer.ts` | Hydrates initial NgRx state from localStorage on app startup |
| `debug.reducer.ts` | Logs NgRx actions and state changes (development only) |

## Relationship to Other Modules

- [Shared Module](shared.md) -- Shared components and models used by core services
- [Page Modules](pages.md) -- Page modules inject core HTTP services and use core guards
- [Theme & i18n](theme-i18n.md) -- Core translate module loads locale data
