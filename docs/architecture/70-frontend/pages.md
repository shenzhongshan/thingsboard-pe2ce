# Page Modules

The page modules (`src/app/modules/home/pages/`) contain 40 feature directories, each implementing a specific section of the ThingsBoard UI. Pages are lazy-loaded based on routes defined in the home routing module.

> Eight of these directories are additions this fork makes on top of upstream CE (mostly features ported from PE): `device-simulator`, `deviceAlarms`, `entity-group`, `help-center`, `integration`, `reporting`, `smart-lightning`, `solution-templates`. Each is listed below.

## Page Module Architecture

Each page directory typically contains:
- `*.module.ts` -- Angular module with route declarations
- `*-routing.module.ts` -- Route configuration
- Component files (`.ts`, `.html`, `.scss`) for the main views
- `*table-config.resolver.ts` -- Entity table configuration (if entity-table type)

## Page Types

| Type | Pattern | Description |
|------|---------|-------------|
| **entity-table** | `EntitiesTableComponent` + resolver | List page with table, search, actions; detail panel slides out or navigates to detail page |
| **entity-details** | `EntityDetailsPageComponent` | Full-page form with tabs for entity details |
| **settings-form** | Standalone form + `ConfirmOnExitGuard` | Single form for settings (general, mail, security) |
| **dashboard-view** | `DashboardViewComponent` + resolver | Embedded dashboard view (e.g., API usage, gateways) |
| **custom** | Unique component(s) | Complex pages not fitting standard patterns |

## All Page Modules

### account/

Account/profile management for the current user. Re-exports the `profile/` and `security/` components for account settings.

### admin/

System and tenant administration pages.

| File | Purpose |
|------|---------|
| `general-settings.component.ts` | System settings (base URL, queue config) |
| `mail-server.component.ts` | Outgoing mail server configuration |
| `sms-provider.component.ts` | SMS provider settings (Twilio, AWS SNS, etc.) |
| `security-settings.component.ts` | Security settings (password policy, session timeout) |
| `two-factor-auth-settings.component.ts` | Two-factor auth configuration |
| `home-settings.component.ts` | Tenant home dashboard setting |
| `auto-commit-admin-settings.component.ts` | Auto-commit version control settings |
| `repository-admin-settings.component.ts` | Repository connection settings |
| `trendz-settings.component.ts` | Trendz analytics settings |
| `oauth2/` | OAuth2 client/domain management (clients, domains sub-pages) |
| `queue/` | Queue management (Kafka topics, processing strategies) |
| `resource/` | Resource library and JS library sub-pages |

**Type:** Mix of settings-form and entity-table

### ai-model/

AI model configuration for ThingsBoard AI integrations.

| Component | Purpose |
|-----------|---------|
| AiModels table | Entity-table for AI model CRUD |

**Type:** entity-table  
**Resolver:** `AiModelsTableConfigResolver`

### alarm/

Alarms center with two views: alarm list and alarm rules.

| File | Purpose |
|------|---------|
| Alarm table component | Displays all alarms with filtering by severity, status, time |
| Alarm rules table | Manages alarm rule definitions (conditions, schedules, actions) |
| Alarm rule detail | Entity detail page for individual alarm rules |

**Type:** custom (RouterTabsComponent for alarm sections)  
**Resolver (rules):** `AlarmRulesTableConfigResolver`

### api-usage/

API usage statistics dashboard.

**Type:** dashboard-view  
**Dashboard:** Loads `api_usage.json` dashboard definition

### asset/

Asset management.

| File | Purpose |
|------|---------|
| `assets-table-config.resolver.ts` | `AssetsTableConfigResolver` -- defines columns, actions |
| `asset.component.ts` | Asset detail form (extends `EntityComponent<Asset>`) |
| `asset-tabs.component.ts` | Asset tabs (attributes, relations, events, alarms) |

**Type:** entity-table  
**Resolver:** `AssetsTableConfigResolver`  
**Entity component:** `AssetComponent`  
**Tabs component:** `AssetTabsComponent`

### asset-profile/

Asset profile (type) management.

**Type:** entity-table  
**Resolver:** `AssetProfilesTableConfigResolver`  
**Entity component:** `AssetProfileComponent`  
**Tabs component:** `AssetProfileTabsComponent`

### audit-log/

Audit log viewer with filtering by entity, user, action type, and time range.

**Type:** custom (audit log table)

### calculated-fields/

Calculated field management for post-processing telemetry data.

**Type:** custom (custom table component) / entity-details  
**Resolver:** `CalculatedFieldsTableConfigResolver`

### customer/

Customer management.

**Type:** entity-table  
**Resolver:** `CustomersTableConfigResolver`  
**Entity component:** `CustomerComponent`  
**Tabs component:** `CustomerTabsComponent`

Also includes nested tables for a customer's users, devices, assets, dashboards, and edges.

### dashboard/

Dashboard management.

| File | Purpose |
|------|---------|
| `dashboards-table-config.resolver.ts` | `DashboardsTableConfigResolver` |
| `dashboard-form.component.ts` | Dashboard metadata form |
| `dashboard-tabs.component.ts` | Dashboard tabs |
| `import-dashboard-file-dialog.component.ts` | JSON dashboard import dialog |
| `make-dashboard-public-dialog.component.ts` | Public dashboard URL sharing |
| `manage-dashboard-customers-dialog.component.ts` | Customer assignment dialog |

**Type:** entity-table / custom (DashboardPageComponent for viewing)

### device/

Device management.

| File | Purpose |
|------|---------|
| `devices-table-config.resolver.ts` | `DevicesTableConfigResolver` |
| `device.component.ts` | Device detail form (name, type, profile, gateway toggle) |
| `device-tabs.component.ts` | Device tabs (attributes, telemetry, relations, alarms, events) |
| `device-credentials-dialog.component.ts` | Device credentials dialog (token, MQTT basic, X.509, LwM2M) |
| `device-check-connectivity-dialog.component.ts` | Device connectivity test dialog |

**Type:** entity-table  
**Resolver:** `DevicesTableConfigResolver`  
**Entity component:** `DeviceComponent`  
**Tabs component:** `DeviceTabsComponent`

### device-profile/

Device profile (type) management with transport configuration.

**Type:** entity-table  
**Resolver:** `DeviceProfilesTableConfigResolver`  
**Entity component:** `DeviceProfileComponent`  
**Tabs component:** `DeviceProfileTabsComponent`

### device-simulator/ (fork addition)

Device Simulator -- a standalone visual telemetry simulator for configuring and driving simulated devices, reusing the emulator JSON format from solution templates. Supports both live and historical modes.

| File | Purpose |
|------|---------|
| `device-simulators.component.ts` | Simulator list and start/stop controls |
| `device-simulator-dialog.component.ts` | Simulator configuration dialog |
| `value-strategy-editor.component.ts` | Value strategy editor (random, sine, incrementing, etc.) |

**Type:** custom

### deviceAlarms/ (fork addition)

Device alarm list (`deviceAlarms/list`) belonging to the Smart Lightning feature.

**Type:** custom (`SmartLightningAlarmListComponent`)

### edge/

Edge instance management for the ThingsBoard Edge system.

**Type:** entity-table  
**Resolver:** `EdgesTableConfigResolver`  
**Entity component:** `EdgeComponent`  
**Tabs component:** `EdgeTabsComponent`

Includes nested tables for edge-assigned entities (assets, devices, entity views, dashboards, rule chains).

### entities/

Entities section routing module that groups device, asset, entity view, and gateway tabs under `/entities/`.

### entity-group/ (fork addition)

Entity groups and customer hierarchy -- the entity-group system ported from PE.

| File | Purpose |
|------|---------|
| `customer-hierarchy.component.ts` | Customer hierarchy tree view |
| `entity-group-dialog.component.ts` | Entity group create/edit dialog |
| `entity-group-columns-tab.component.ts` | Group column configuration tab |
| `entity-group-actions-tab.component.ts` | Group action configuration tab |
| `add-group-entities-dialog.component.ts` | Dialog for adding entities to a group |
| `entity-group-details-config.util.ts` | Tab wiring for the group details drawer |

**Type:** entity-table / custom

### entity-view/

Entity view (virtual entity) management.

**Type:** entity-table  
**Resolver:** `EntityViewsTableConfigResolver`  
**Entity component:** `EntityViewComponent`  
**Tabs component:** `EntityViewTabsComponent`

### features/

Advanced features section routing for OTA updates and version control.

### gateways/

Gateway connectivity dashboard.

**Type:** dashboard-view  
**Dashboard:** Loads gateway status dashboard

### help-center/ (fork addition)

Help Center -- built-in browsing and reading of help articles.

| File | Purpose |
|------|---------|
| `help-article.component.ts` | Renders a single help article |
| `help-articles.data.ts` | Help article content data |

**Type:** custom

### home-links/

Home page with quick-link cards and dashboard resolver. Defines the sidebar navigation menu structure and lazy-loads all page modules.

**Type:** custom (HomeLinksComponent)

### integration/ (fork addition)

Integration Center -- ported from PE, covering all 29 integration protocol types plus uplink/downlink data converters.

| File | Purpose |
|------|---------|
| `integration/` | Integration list, details, and editing |
| `converter/` | Uplink/downlink converters (with decoder/encoder editors and test dialogs) |
| `integration-details-panel.component.ts` | Integration details side panel |
| `integration-entity-details-page.component.ts` | Full-page integration details view |

**Type:** entity-table / custom

### mobile/

Mobile application center.

| Sub-page | Type | Purpose |
|----------|------|---------|
| `bundles/` | entity-table | Mobile app bundle management (`MobileBundleTableConfigResolver`) |
| `applications/` | entity-table | Mobile application management (`MobileAppTableConfigResolver`) |
| `qr-code-widget/` | settings-form | QR code widget configuration |

### notification/

Notification center with inbox, sent, recipients, templates, and rules.

| Sub-page | Type | Resolver |
|----------|------|----------|
| `inbox/` | entity-table | `InboxTableConfigResolver` |
| `sent/` | entity-table | `SentTableConfigResolver` |
| `recipient/` | entity-table | `RecipientTableConfigResolver` |
| `template/` | entity-table | `TemplateTableConfigResolver` |
| `rule/` | entity-table | `RuleTableConfigResolver` |

### ota-update/

OTA (Over-The-Air) firmware/software package management.

**Type:** entity-table  
**Resolver:** `OtaUpdateTableConfigResolve`  
**Entity component:** `OtaUpdateComponent`  
**Tabs component:** `OtaUpdateTabsComponent`

### profile/

User profile page (part of Account section). Editable profile information.

**Type:** settings-form  
**Component:** `ProfileComponent`

### profiles/

Profiles section routing that groups device profiles and asset profiles under `/profiles/`.

### reporting/ (fork addition)

Report engine -- ported from PE, supporting PDF/CSV output, charts, dashboard embedding, and scheduled generation.

| Sub-page | Purpose |
|----------|---------|
| `report/` | Report list and generate dialog |
| `report-template/` | Report template list, create dialog, and visual editor |
| `schedule/` | Report scheduling configuration |

**Type:** entity-table / custom

### rulechain/

Rule chain management with visual editor.

| File | Purpose |
|------|---------|
| `rulechains-table-config.resolver.ts` | `RuleChainsTableConfigResolver` |
| `rulechain.component.ts` | Rule chain detail form |
| `rulechain-tabs.component.ts` | Rule chain tabs |
| `rulechain-page.component.ts` | Visual rule chain editor (drag-and-drop node graph) |
| `rulechain-page.module.ts` | Lazy-loaded module for the editor |
| `rulenode.component.ts` | Rule node visual representation |
| `rule-node-details.component.ts` | Rule node detail/config panel |
| `rule-node-config.component.ts` | Per-node configuration form |
| `rule-node-link.component.ts` | Connection between nodes |
| `link-labels.component.ts` | Link label display |
| `rulenote.component.ts` | Annotation notes on the canvas |
| `add-rule-node-dialog.component.ts` | Add rule node dialog (type selector) |
| `add-rule-node-link-dialog.component.ts` | Add link between nodes dialog |

**Type:** entity-table / custom (visual editor)  
**Resolver:** `RuleChainsTableConfigResolver`

### scada-symbol/

SCADA symbol editor for creating and editing SCADA symbols using SVG.

**Type:** custom (`ScadaSymbolComponent`)

### security/

Account security page (part of Account section). Manages 2FA, password change, and session management.

**Type:** settings-form  
**Component:** `SecurityComponent`

### smart-lightning/ (fork addition)

Smart Lightning -- a scenario-specific feature section with dedicated dashboards, device monitoring, and query views.

| Sub-page | Purpose |
|----------|---------|
| `dashboard/` | Smart Lightning dashboard with map config and period switcher |
| `monitoring/` | Per-luminaire device detail monitoring |
| `query/` | Query view |
| `smart-lightning-root-asset-filter.component.ts` | Root asset filter |

**Type:** custom

### solution-templates/ (fork addition)

Solution templates -- ported from PE, supporting template browsing, one-click install/uninstall, and bundled telemetry emulators.

| File | Purpose |
|------|---------|
| `solution-templates.component.ts` | Template browse list |
| `solution-template-card.component.ts` | Individual template card |
| `solution-template-details.component.ts` | Template details |
| `solution-install-dialog.component.ts` | Install/uninstall dialog |

**Type:** custom

### tenant/

Tenant management (SYS_ADMIN only).

**Type:** entity-table  
**Resolver:** `TenantsTableConfigResolver`  
**Entity component:** `TenantComponent`  
**Tabs component:** `TenantTabsComponent`

### tenant-profile/

Tenant profile management (SYS_ADMIN only).

**Type:** entity-table  
**Resolver:** `TenantProfilesTableConfigResolver`  
**Entity component:** `TenantProfileComponent`  
**Tabs component:** `TenantProfileTabsComponent`

### user/

User management.

**Type:** entity-table  
**Resolver:** `UsersTableConfigResolver`  
**Entity component:** `UserComponent`  
**Tabs component:** `UserTabsComponent`

Users can be managed at both system level (SYS_ADMIN) and tenant level (TENANT_ADMIN), with appropriate permission filtering.

### vc/

Version Control (entity versioning). Provides commit history, branch management, and entity version comparison.

**Type:** custom (`VersionControlComponent`)

### widget/

Widget library management.

| Sub-page | Type | Purpose |
|----------|------|---------|
| Widget Types | entity-table | `WidgetTypesTableConfigResolver` |
| Widgets Bundles | entity-table | `WidgetsBundlesTableConfigResolver` |
| Widgets Bundle Detail | custom | Bundle content list (`WidgetsBundleWidgetsComponent`) |
| Widget Editor | custom | Visual widget editor (`WidgetEditorComponent`) |

## Login Module

The login module (`src/app/modules/login/`) handles unauthenticated access:

| Component | Purpose |
|-----------|---------|
| `LoginComponent` | Username/password login form |
| `ResetPasswordRequestComponent` | Request password reset email |
| `ResetPasswordComponent` | Set new password (from reset link) |
| `CreatePasswordComponent` | Create initial password (from activation link) |
| `TwoFactorAuthLoginComponent` | 2FA code verification |

## Reusable Home Components

The `home/components/` directory contains framework components used by multiple page modules:

| Component | Purpose |
|-----------|---------|
| `tb-entities-table` | Main entity table (columns, sort, pagination, search, actions) |
| `tb-entity-details-panel` | Slide-out drawer for entity details |
| `tb-entity-details-page` | Full-page entity details view |
| `tb-add-entity-dialog` | Generic add-entity dialog |
| `tb-entity-filter` | Entity data filter component |
| `tb-entity-chips` | Chip display for entity references |
| `tb-alarm-table` | Alarm list table |
| `tb-alarm-details-dialog` | Alarm detail dialog |
| `tb-audit-log-table` | Audit log table |
| `tb-attribute-table` | Entity attribute key-value table |
| `tb-relation-table` | Entity relation table |
| `tb-event-table` | Entity event table |
| `tb-dashboard` | Dashboard renderer |
| `tb-dashboard-page` | Dashboard page with toolbar |
| `tb-edit-widget` | Widget editor |
| `tb-router-tabs` | Tab navigation via router |
| `tb-device-credentials` | Device credentials form |
| `tb-details-panel` | Base details panel |

## Relationships

- [Frontend Overview](README.md) -- Overall frontend architecture and entity table pattern
- [Core Module](core.md) -- HTTP services and route guards consumed by pages
- [Shared Module](shared.md) -- Shared components (entity selectors, dialogs, time pickers) used by pages
- [Theme & i18n](theme-i18n.md) -- All pages use the theme and translation system
