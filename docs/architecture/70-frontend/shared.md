# Shared Module

The Shared module (`src/app/shared/`) provides reusable components, models, pipes, directives, and utilities that are used across the application. It is imported by both the Home module and the Login module.

## Module Structure

```
src/app/shared/
├── components/   # 20+ component groups (button, color-picker, dialog, entity, image, time, etc.)
├── models/       # TypeScript interfaces, enums, constants
├── pipe/         # Custom pipes (translation, filtering, formatting)
├── directives/   # Custom directives
├── decorators/   # Custom decorators
├── services/     # Shared utility services
├── import-export/ # Data import/export (CSV, JSON)
├── adapter/      # Custom data adapters
├── animations/   # Shared animation definitions
└── legacy/       # Legacy compatibility utilities
```

The shared module contains approximately 402 TypeScript files across all subdirectories.

## components/ -- Shared UI Components

### button/

| File | Purpose |
|------|---------|
| Toggle buttons, copy button, file input button | Various specialized button components |

### color-picker/

| File | Purpose |
|------|---------|
| `color-picker.component.ts` | Inline color picker component |
| `color-picker-dialog.component.ts` | Color picker in a dialog |
| Color-related components | Uses `@iplab/ngx-color-picker` |

### dialog/

| File | Purpose |
|------|---------|
| `dialog.component.ts` | Base dialog component |
| `confirm-dialog.component.ts` | Yes/No confirmation dialog |
| `alert-dialog.component.ts` | Information alert dialog |
| `error-alert-dialog.component.ts` | Error message dialog |
| `color-picker-dialog.component.ts` | Color picker dialog |
| `material-icons-dialog.component.ts` | Material icon browser dialog |
| `node-script-test-dialog.component.ts` | Script testing dialog for rule nodes |
| `object-edit-dialog.component.ts` | JSON object editor dialog |
| `todo-dialog.component.ts` | Todo/checklist dialog |
| `dynamic/` | Dynamic dialog components |
| `entity-conflict-dialog/` | Entity version conflict resolution dialog |

### directives/

| File | Purpose |
|------|---------|
| `tb-json-content` | JSON syntax-highlighted viewer |
| `tb-json-object-edit` | Editable JSON object component |
| `tb-json-object-view` | Read-only JSON object display |
| `tb-markdown` | Markdown editor/viewer |

### entity/

Entity selection and autocompletion components used in forms and filters:

| Selector | Purpose |
|----------|---------|
| `tb-entity-autocomplete` | Autocomplete input for selecting a single entity |
| `tb-entity-select` | Dropdown select for entity |
| `tb-entity-list` | Chip list for multiple entities |
| `tb-entity-list-select` | Dropdown with entity list selection |
| `tb-entity-key-autocomplete` | Autocomplete for entity attribute/telemetry keys |
| `tb-entity-keys-list` | Chip list for multiple entity keys |
| `tb-entity-type-select` | Dropdown for entity type enum |
| `tb-entity-type-list` | Chip list for multiple entity types |
| `tb-entity-subtype-select` | Dropdown for entity subtype |
| `tb-entity-subtype-list` | Chip list for multiple subtypes |
| `tb-entity-subtype-autocomplete` | Autocomplete for entity subtype |
| `tb-entity-gateway-select` | Dropdown for selecting a gateway device |

### grid/

Grid layout components for dashboard widget arrangement. Integrates with `angular-gridster2` for drag-and-drop dashboard editing.

### image/

Image management components:

| Selector | Purpose |
|----------|---------|
| `tb-image-gallery` | Grid gallery of uploaded images |
| `tb-image-gallery-dialog` | Dialog wrapper for image gallery |
| `tb-image-input` | Single image upload input |
| `tb-gallery-image-input` | Image input that opens gallery for selection |
| `tb-multiple-gallery-image-input` | Multi-image input with gallery |
| `tb-upload-image-dialog` | Upload dialog |
| `tb-embed-image-dialog` | Embed external image dialog |
| `tb-scada-symbol-input` | SCADA symbol selector input |

### time/

Time range selection components used in dashboards and telemetry views:

| Selector | Purpose |
|----------|---------|
| `tb-timewindow` | Timewindow selector (realtime/history) |
| `tb-timewindow-panel` | Expanded panel for timewindow config |
| `tb-timewindow-config-dialog` | Dialog for advanced timewindow settings |
| `tb-datetime` | Date-time picker input |
| `tb-datetime-period` | Date range picker |
| `tb-timezone` | Timezone display |
| `tb-timezone-select` | Dropdown for timezone selection |
| `tb-quick-time-interval` | Quick interval preset selector |
| `tb-timeinterval` | Duration input (number + unit) |
| `tb-datapoints-limit` | Numeric input for data point limits |
| `tb-aggregation-type-select` | Dropdown for aggregation type (AVG, SUM, MIN, MAX, COUNT, NONE) |
| `tb-history-selector` | History navigation selector |

### notification/

Notification display components for the notification center. Includes notification bell, notification list, and notification detail views.

### ota-package/

OTA (Over-The-Air) package components: firmware/software upload, version management, file checksum verification.

### queue/

Queue configuration components for the system administrator. Displays Kafka topic configurations, processing strategies, and retry settings.

### relation/

Entity relationship management components. Provides UI for creating, viewing, and deleting relations between entities.

### resource/

Resource library components for managing ThingsBoard resources (JS modules, images, etc.).

### rule-chain/

Rule chain visual editor components:

| Selector | Purpose |
|----------|---------|
| Rule node palette | Rule node type browser |
| Rule node configuration | Per-node configuration panel |
| Rule node link | Connection between nodes with link labels |
| Debug mode controls | Script testing and message tracing |

### table/

Table-related shared components including data tables, table headers, and pagination controls.

### vc/

Version control components for entity version management. Includes diff viewer, commit history, and branch management UI.

### Others (top-level components)

| Selector | Purpose |
|----------|---------|
| `tb-breadcrumb` | Breadcrumb navigation bar |
| `tb-icon` | Custom icon component (MDI + Font Awesome) |
| `tb-logo` | ThingsBoard logo |
| `tb-copy-button` | Copy-to-clipboard button |
| `tb-toggle-password` | Password visibility toggle |
| `tb-file-input` | File upload input |
| `tb-phone-input` | International phone input (`libphonenumber-js`) |
| `tb-unit-input` | Measurement unit input |
| `tb-value-input` | Generic value input with type selection |
| `tb-color-input` | Color picker input |
| `tb-key-val-map` | Key-value pair map editor |
| `tb-nav-tree` | Tree navigation component |
| `tb-user-menu` | User profile dropdown menu |
| `tb-notification-bell` | Notification bell icon with unread badge |
| `tb-footer` | Page footer |
| `tb-scroll-grid` | Virtual-scroll grid |
| `tb-social-share-panel` | Social sharing buttons |
| `tb-fab-toolbar` | Floating action button toolbar |
| `tb-footer-fab-buttons` | Footer FAB buttons |
| `tb-hotkeys` | Keyboard shortcut handler |
| `tb-fullscreen` | Fullscreen toggle directive |
| `tb-circular-progress` | Circular progress indicator |
| `tb-cheatsheet` | Keyboard shortcut cheatsheet |
| `tb-contact` | Contact/support link |
| `tb-help` | Help panel |
| `tb-help-popup` | Help popup |
| `tb-help-markdown` | Help content with markdown |
| `tb-hint-tooltip-icon` | Icon with hint tooltip |
| `tb-css` | CSS editor component |
| `tb-html` | HTML editor component |
| `tb-js-func` | JavaScript function editor |
| `tb-js-func-modules` | JS function module list |
| `tb-js-func-module-row` | JS function module row |

### Autocomplete / Select Components

| Selector | Purpose |
|----------|---------|
| `tb-dashboard-autocomplete` | Autocomplete for dashboard selection |
| `tb-dashboard-select` | Dropdown for dashboard |
| `tb-dashboard-state-autocomplete` | Autocomplete for dashboard states |
| `tb-country-autocomplete` | Country autocomplete |
| `tb-string-autocomplete` | Generic string autocomplete |
| `tb-message-type-autocomplete` | Rule engine message type autocomplete |
| `tb-queue-autocomplete` | Queue name autocomplete |
| `tb-relation-type-autocomplete` | Relation type autocomplete |
| `tb-resource-autocomplete` | Resource autocomplete |
| `tb-ota-package-autocomplete` | OTA package autocomplete |
| `tb-branch-autocomplete` | Git branch autocomplete |
| `tb-rule-chain-select` | Rule chain select dropdown |
| `tb-widgets-bundle-select` | Widgets bundle select dropdown |
| `tb-template-autocomplete` | Notification template autocomplete |
| `tb-material-icon-select` | Material icon picker |
| `tb-slack-conversation-autocomplete` | Slack conversation autocomplete |

## models/ -- TypeScript Models

### id/

Entity ID models. Provides typed wrappers for ThingsBoard entity IDs (DeviceId, AssetId, CustomerId, etc.) used in API calls and URL routing.

### page/

Page data models for paginated API responses. Includes `PageData<T>`, `PageLink`, `TimePageLink`, and sorting/filtering parameters.

### query/

Entity data query models. Defines filter predicates, key filters, and entity filter types used in alarm rules and dashboard data sources.

### telemetry/

Telemetry data models. Defines timeseries data structures, attribute models (client/server/shared), latest values, and aggregation types.

### time/

Time-related models: `Timewindow`, `AggregationType`, `HistoryWindowType`. Used by the time selection components and telemetry queries.

### ace/

ACE (Ajax.org Cloud9 Editor) code editor configuration models. Used by rule node script editors and widget JS editors.

### websocket/

WebSocket message types for real-time communication. Defines subscription commands, telemetry update messages, and notification events.

### widget/

Widget models: widget type definitions, widget bundle configurations, dashboard state models.

### Other Model Files

| File | Purpose |
|------|---------|
| `alarm.models.ts` | Alarm severity, status, search filters |
| `alarm-rule.models.ts` | Alarm rule condition and schedule definitions |
| `alias.models.ts` | Entity alias resolution types |
| `api-usage.models.ts` | API usage statistics models |
| `asset.models.ts` | Asset and AssetInfo interfaces |
| `audit-log.models.ts` | Audit log entry models |
| `authority.enum.ts` | User authority roles (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER, etc.) |
| `base-data.ts` | Base entity interfaces (hasId, hasName, etc.) |
| `calculated-field.models.ts` | Calculated field configuration |
| `color.models.ts` | Color/settings models |
| `common.ts` | Common utility types |
| `component-descriptor.models.ts` | Rule node component descriptor |
| `constants.ts` | Application-wide constants |
| `contact-based.model.ts` | Base model with contact info (address, phone, email) |
| `country.models.ts` | Country data models |
| `customer.model.ts` | Customer interface |
| `dashboard.models.ts` | Dashboard configuration |
| `device.models.ts` | Device and DeviceInfo interfaces |
| `dynamic-form.models.ts` | Dynamic form field configuration |
| `edge.models.ts` | Edge instance models |
| `entity-type.models.ts` | Entity type enum |
| `entity-view.models.ts` | Entity view models |
| `entity.models.ts` | Generic entity models |
| `error.models.ts` | Error response models |
| `event.models.ts` | Entity event models |
| `icon.models.ts` | Icon configuration |
| `js-function.models.ts` | JS function module models |
| `limited-api.models.ts` | Rate-limited API models |
| `login.models.ts` | Login request/response |
| `material.models.ts` | Material Design constants |
| `mobile-app.models.ts` | Mobile app configuration |
| `notification.models.ts` | Notification models |
| `ota-package.models.ts` | OTA package models |
| `queue.models.ts` | Queue configuration models |
| `relation.models.ts` | Entity relation models |
| `resource.models.ts` | Resource library models |
| `rpc.models.ts` | RPC request/response models |
| `rule-chain.models.ts` | Rule chain and rule node models |
| `rule-node.models.ts` | Rule node configuration models |
| `settings.models.ts` | System/admin settings models |
| `tenant.model.ts` | Tenant interface |
| `user.model.ts` | User interface |

## pipe/ -- Custom Pipes

Custom Angular pipes for template transformations:

- **Translation pipes:** Integration with @ngx-translate for template-level translations
- **Filtering pipes:** Array and object filtering
- **Formatting pipes:** Date, number, file size, duration formatting
- **Enum pipes:** Convert enum values to display labels
- **Highlight pipe:** Search term highlighting in text

## directives/ -- Custom Directives

Custom attribute directives for DOM manipulation and behavior enhancement:

- Scroll, tooltip, popover, autofocus, etc.
- Input masking and validation
- Click-outside detection
- Drag and drop

## decorators/ -- Custom Decorators

TypeScript decorators for class and method enhancement:

- `@OnDestroy()` -- Automatically unsubscribes RxJS subscriptions
- `@CoerceBoolean()` -- Coerces Input properties to boolean

## services/ -- Shared Services

Shared utility services consumed across feature modules:

- Data conversion and formatting
- File download helpers
- Platform detection utilities
- Clipboard integration

## import-export/ -- Data Import/Export

| File | Purpose |
|------|---------|
| CSV import/export | Convert entity data to/from CSV format with column mapping |
| JSON import/export | Convert entity data to/from JSON format |

Used by bulk entity creation, dashboard import, and data migration features.

## adapter/ -- Custom Adapters

Data adapters that implement custom transformation logic between API responses and component data models.

## animations/ -- Shared Animations

Reusable Angular animation definitions (fade in/out, slide, expand/collapse) used across the application.

## Relationships

- [Core Module](core.md) -- Core services use shared models for type definitions
- [Page Modules](pages.md) -- Page modules import shared components (entity selects, dialogs, time pickers)
- [Theme & i18n](theme-i18n.md) -- Shared components use the theme system
