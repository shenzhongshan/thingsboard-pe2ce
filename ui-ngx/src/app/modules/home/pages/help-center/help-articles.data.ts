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

export interface HelpArticle {
  title: string;
  markdown: string;
}

// Condensed, self-hosted replacements for the links that used to point at the
// public documentation site. Content is summarized from ThingsBoard's official
// user guide and adapted to what is actually available in this deployment.
export const HELP_ARTICLES: { [key: string]: HelpArticle } = {

  'getting-started': {
    title: 'Getting started',
    markdown:
`A short guided tour to go from an empty account to a working dashboard.

## For system administrators

1. [Create a tenant and a tenant administrator](/home/help/tenants)
2. [Configure the mail server](/home/help/mail-settings) so users can activate accounts and receive alarm notifications
3. Optionally configure [SMS](/home/help/sms-provider-settings), [two-factor authentication](/home/help/two-factor-authentication), [OAuth 2.0 / SSO](/home/help/oauth-2-support) and [Slack notifications](/home/help/slack-settings)

## For tenant administrators

1. [Provision your first device](/home/help/provision-device)
2. [Connect the device and publish telemetry](/home/help/connect-device)
3. [Create a dashboard](/home/help/create-dashboard) to visualize the data
4. [Configure an alarm rule](/home/help/alarm-rules) and [trigger an alarm](/home/help/create-alarm)
5. [Create a customer and assign the dashboard](/home/help/assign-customer) to it

Each of these topics also has a dedicated "Getting started" card on the home page that walks through the same steps interactively.`
  },

  'tenants': {
    title: 'How to create a Tenant & Tenant Administrator',
    markdown:
`A tenant is an individual or organization that owns devices and assets. Each tenant can have its own tenant administrators, customers, devices and dashboards, fully isolated from other tenants.

## Create a tenant

1. Open **Tenants** in the left menu.
2. Click **+ Add tenant**.
3. Enter a **Title** (required) and, optionally, a tenant profile, address, phone and contact email.
4. Click **Add**.

## Create a tenant administrator

1. From the Tenants list, open the tenant and click **Manage tenant admins**.
2. Click **+ Add user**.
3. Enter the administrator's **email address** (required), and optionally first/last name and phone.
4. Choose how the account is activated: send an activation email (requires the mail server to be configured) or copy the activation link and share it manually.
5. Click **Add**.

**Tip:** assign a [tenant profile](/home/help/tenant-profiles) to control the resource limits (devices, API usage, storage) available to that tenant.`
  },

  'tenant-profiles': {
    title: 'Tenant profiles',
    markdown:
`A **tenant profile** is a template of resource limits and quotas that is applied to one or more tenants. It is how a multi-tenant deployment keeps one tenant from starving the others.

## What it controls

- Entity limits: max devices, assets, dashboards, users, integrations, reports
- API usage: rule engine executions and transport (telemetry/attribute) messages per time window
- Data retention (TTL) for telemetry, alarms and archived data
- Email/SMS notification quotas
- File size limits for resources and OTA packages
- WebSocket session and subscription limits
- Rate limiting for message throughput

## How to use it

1. Open **Tenant profiles** in the left menu.
2. Click **+ Add tenant profile**, give it a name and configure the limits that matter for your deployment.
3. Click **Add** to save it.
4. To apply it, open a tenant's details and set its **Tenant profile** field, then save.

Changes to a profile apply immediately to every tenant assigned to it. New tenants use the default profile unless a different one is chosen at creation time.`
  },

  'api': {
    title: 'REST API',
    markdown:
`ThingsBoard exposes a full REST API for everything you can do from the UI (devices, telemetry, alarms, dashboards, users, and more).

## Authentication

1. `+ "`POST /api/auth/login`" + ` with a JSON body of `+"`{\"username\": \"...\", \"password\": \"...\"}`"+` to obtain a JWT **token**.
2. Send that token on every subsequent request as the `+"`X-Authorization: Bearer <token>`"+` header.
3. Tokens expire; use the returned `+"`refreshToken`"+` against `+"`POST /api/auth/token`"+` to get a new one without re-authenticating.

## Where to explore endpoints

The full, interactive Swagger/OpenAPI reference is served by this instance at **/swagger-ui.html**. It lists every endpoint grouped by entity type (Device, Telemetry, Alarm, Dashboard, ...), with example payloads.

## Common areas

- **Device management** — create/list/delete devices and their credentials
- **Telemetry & attributes** — post and query time-series data and key/value attributes
- **Entity queries** — search/filter entities with complex criteria

A Java and a Python REST client SDK are also available if you prefer not to call the HTTP API directly.`
  },

  'widget-library': {
    title: 'Widget library',
    markdown:
`The **Widget Library** is the catalog of building blocks used to compose dashboards: charts, cards, maps, gauges, tables and controls, grouped into bundles by purpose.

## Browsing it

1. Open **Widget library** in the left menu to browse all bundles, or open it from inside the dashboard editor when adding a widget.
2. Bundles group widgets by function — for example *Charts*, *Cards*, *Maps*, *Gauges*, *Control widgets*, *Alarm widgets*, *SCADA symbols*.
3. Click a bundle to see its widgets, and a widget to preview it.

## Adding a widget to a dashboard

1. Open a dashboard in edit mode and click **+ Add widget**.
2. Pick a bundle, then a widget.
3. Configure its **data source** (which entity/telemetry key(s) to show) and appearance settings.
4. Click **Add** and arrange it on the grid.

You can also create your own custom widget types from the Widget library if none of the built-in ones fit your use case.`
  },

  'rule-engine-getting-started': {
    title: 'Rule engine basics',
    markdown:
`The **Rule Engine** is where incoming data gets processed: every telemetry update, attribute change, device event or RPC call becomes a *message* that flows through a *rule chain*.

## Core concepts

- **Rule chain** — a graph of rule nodes that processes messages. Every tenant has a **Root Rule Chain**, the default entry point for all of its messages.
- **Rule node** — a single processing step: filter (e.g. "message type switch"), transform (e.g. "script"), enrich (e.g. "originator attributes"), or act (e.g. "save timeseries", "create alarm", "send email").
- **Relations** — nodes are connected by labeled links (Success, Failure, True/False, ...) that decide where a message goes next.

## Opening the root rule chain

1. Open **Rule chains** in the left menu.
2. Click the row for **Root Rule Chain** to open the visual editor.
3. Double-click any node to view/edit its configuration.
4. Drag from a node's output to another node to add a new connection.
5. Click **Apply changes** to save.

Rule chains can also call other rule chains, which is how large processing pipelines are kept manageable.`
  },

  'device-profiles': {
    title: 'Device profiles',
    markdown:
`A **device profile** is a configuration template shared by every device of the same kind, so you don't have to repeat the same setup per device.

## What it configures

- **Transport type** — MQTT, HTTP, CoAP, LwM2M or SNMP, and the payload format devices use
- **Alarm rules** — conditions evaluated against telemetry/attributes of every device in the profile (e.g. raise a Critical alarm when temperature > 40°C)
- **Rule chain & queue** — which rule chain processes messages from these devices
- **Calculated fields** — derived metrics computed from raw telemetry
- **Provisioning** — how new devices are allowed to self-register
- **Firmware / software** — OTA package assignment for the whole profile

## Creating one

1. Open **Device profiles** in the left menu.
2. Click **+ Add device profile → Create new device profile**.
3. Enter a **name** (the only required field).
4. Configure transport, alarm rules, provisioning etc. as needed.
5. Click **Add**.

Editing a profile's rule chain, alarm rules or calculated fields immediately affects every device already assigned to it.`
  },

  'white-labeling': {
    title: 'How to configure white labeling',
    markdown:
`White labeling lets you customize the look of the platform for your company or product — no code changes and no service restart required, changes apply live.

## General white labeling

1. Open **White labeling → General**.
2. Set the **application title**, **logo** (upload an image or set an image URL) and **favicon**.
3. Set the **color palette**: primary/accent colors used across the UI.
4. Optionally set a custom **application help/support link** and **hide the ThingsBoard-branded elements** (powered-by links, etc.).
5. Click **Save** — the changes apply immediately, without a restart.

## Login page white labeling

1. Open **White labeling → Login**.
2. Customize the login page separately from the app itself: background, logo, page title and show/hide the sign-up link.
3. This can be scoped to a specific **domain**, so different domains pointing at the same instance can show different branding.
4. Click **Save**.

A tenant administrator can white label their own tenant (visible only to their users); a system administrator's settings are used as the platform-wide default.`
  },

  'mail-settings': {
    title: 'How to configure the mail server',
    markdown:
`The outgoing mail server is used for account activation emails, password recovery and email-based alarm/system notifications.

## Steps

1. Open **Settings → Mail server**.
2. Enter the **From** address used as the sender.
3. Pick a provider template (**SendGrid**, **Google**, **Office 365**) to auto-fill connection details, or choose **Custom** for any other SMTP server.
4. Fill in **SMTP protocol** (SMTP/SMTPS), **host**, **port** and **timeout**; enable **TLS** if required by your provider.
5. Choose an authentication method: **Basic** (username/password) or **OAuth 2.0** (Google/Office 365 only), and enter the credentials.
6. Click **Send test email** to confirm everything works before relying on it.
7. Click **Save**.

These settings apply system-wide to all tenants. A tenant administrator can still override the sender for a specific flow using a **Send Email** rule node with its own SMTP configuration.`
  },

  'sms-provider-settings': {
    title: 'How to configure the SMS provider',
    markdown:
`SMS delivery is used to notify users of alarms and other events by text message.

## Steps

1. Open **Settings → Notifications**.
2. In **SMS provider type**, choose **Amazon SNS**, **Twilio** or **SMPP**.
3. Fill in the fields for your provider:
   - **Amazon SNS**: AWS Access Key ID, AWS Secret Access Key, AWS Region
   - **Twilio**: Account SID, Account Token, Phone Number From
   - **SMPP**: System ID, Password, SMPP host, SMPP port
4. Click **Save**.
5. Click **Send test SMS**, enter a recipient number in E.164 format (e.g. `+ "`+12015550123`" + `) and a message, then send it to confirm delivery.

If the test fails, double check the provider credentials first, then the sender phone number/registration with the provider.`
  },

  'two-factor-authentication': {
    title: 'How to configure two-factor authentication (2FA)',
    markdown:
`2FA adds a second verification step after a correct password, protecting accounts even if a password leaks.

## Supported methods

- **Authenticator app (TOTP)** — time-based codes from apps like Google Authenticator, no network needed
- **Email** — one-time code sent to the user's email
- **SMS** — one-time code sent by text message (requires the [SMS provider](/home/help/sms-provider-settings) to be configured)
- **Backup codes** — single-use codes generated in advance, for when the primary method isn't available

## Configuring it

1. Open **Security settings → Two-factor authentication**.
2. Enable the verification methods you want to offer.
3. For each method, set the code lifetime, verification time window, resend period, max failed attempts and rate limiting.
4. Optionally customize the email/SMS message templates.
5. Click **Save**.

## Enforcing it

In the same page, under **Enforce two-factor authentication**, choose whether it is required for **all users**, only **system administrators**, or only **tenant administrators** (optionally scoped to specific tenant profiles).`
  },

  'oauth-2-support': {
    title: 'How to configure OAuth 2.0 / Single Sign-On',
    markdown:
`OAuth 2.0 lets users log in with an external identity provider (Google, Azure AD, Okta, a custom OpenID Connect provider, ...) instead of a ThingsBoard password.

## Steps

1. Open **Security settings → OAuth 2.0**.
2. Under **OAuth 2.0 clients**, click **+ Add** and enter the **Client ID** / **Client secret** issued by your identity provider, and pick a **Provider type** (or *Custom*).
3. For a custom provider, fill in the **Authorization URI**, **Access token URI**, **User info URI** and (if used) **JSON Web Key URI** under *Advanced settings*.
4. Enable **Allow user creation** if new users should be provisioned automatically on first login, and **Activate user** if they should be usable immediately.
5. Configure a **Mapper** — *Basic* for a standard OpenID Connect provider (maps email/first name/last name), or *Custom* to post the user info to your own HTTP endpoint for more complex logic.
6. Go to the **Domains** tab, add the domain(s) this login should be available on, attach the client(s) you created, and enable it.
7. Register the **Redirect URI** shown on that page with your identity provider.

Once saved, an OAuth 2.0 login button appears on the login page for the configured domain.`
  },

  'slack-settings': {
    title: 'How to configure Slack notifications',
    markdown:
`Slack can be used as a delivery channel for notification rules (alarms, entity actions, system events, ...).

## 1. Create a Slack app

1. Go to **api.slack.com/apps** and create an app for your workspace.
2. Under **OAuth & Permissions**, add the scopes needed to post messages and read channel/user info (e.g. `+"`chat:write`, `channels:join`, `groups:read`, `users:read`"+`).
3. Install the app to your workspace and copy the **Bot User OAuth Token** (starts with `+"`xoxb-`"+`).

## 2. Configure ThingsBoard

1. Open **Settings → Notifications**.
2. Paste the bot token into **Slack API token** and click **Save**.

## 3. Invite the bot and notify

1. In Slack, invite the bot to the target channel (`+"`/invite @your-bot`"+`).
2. Create a **Notification rule** and add a **Slack** recipient pointing to that channel or a direct message.
3. The bot needs to already be a member of any channel it posts to.`
  },

  'provision-device': {
    title: 'How to create a device',
    markdown:
`1. Open **Devices** in the left menu.
2. Click **+ Add device → Add new device**.
3. Enter a **name**, optionally pick a **device profile** and a **label**.
4. Click **Add**.

The device is created with an auto-generated **access token** credential, which is the simplest way to authenticate it when publishing data (see [Connect device](/home/help/connect-device)).

You can also **bulk-import** many devices at once from a CSV/Excel file, or let devices **self-provision** by connecting for the first time with credentials matching a device profile's provisioning configuration.`
  },

  'connect-device': {
    title: 'How to connect a device',
    markdown:
`## Get the access token

1. Open the device's row in the **Devices** table to open its details.
2. Click **Copy access token** (or check the **Credentials** tab for other credential types).

## Publish telemetry over HTTP

Replace `+"`$ACCESS_TOKEN`"+` with the device's token and run:

    curl -v -X POST -d "{\\"temperature\\": 25}" \\
      http://<host>/api/v1/$ACCESS_TOKEN/telemetry \\
      --header "Content-Type:application/json"

A successful call returns HTTP 200 with no body, and the value shows up under the device's **Latest telemetry** tab within a few seconds.

## Other protocols

HTTP is the simplest option for testing. Real devices more commonly use **MQTT** (publish to `+"`v1/devices/me/telemetry`"+` with the access token as the MQTT username), **CoAP**, **LwM2M** or **SNMP** — pick whichever matches your hardware/firmware.`
  },

  'create-dashboard': {
    title: 'How to create a dashboard',
    markdown:
`1. Open **Dashboards** in the left menu.
2. Click **+ Add dashboard → Create new dashboard**, give it a title, and click **Add**.
3. Open it and click **Edit mode**, then **+ Add widget**.
4. Pick a widget from the [Widget library](/home/help/widget-library) (for example a **Time series line chart** or a **Value card**).
5. Configure its **data source**: choose the device (or an alias/filter that resolves to it) and the telemetry key(s) to plot.
6. Arrange and resize widgets on the grid as needed, then click **Apply changes** and **Save**.

Dashboards can also embed **states** (sub-views reachable by clicking a widget) and can later be assigned to a customer so their users only see their own data.`
  },

  'alarm-rules': {
    title: 'How to configure alarm rules',
    markdown:
`Alarm rules automatically raise, update and clear alarms based on telemetry or attribute conditions, and live on a **device profile** (or asset profile) so they apply to every entity using it.

## Steps

1. Open the relevant **Device profile** (or **Alarms → Alarm rules** for a global rule) and go to its **Alarm rules** tab.
2. Click **+ Add alarm rule**, give it an **alarm type** (e.g. "High temperature").
3. Add one or more **arguments** — the telemetry key or attribute to evaluate (e.g. latest telemetry `+"`temperature`"+` of the current device).
4. Add a **create (trigger) condition**: pick a **severity** (Critical, Major, Minor, Warning, Indeterminate), then build an expression such as `+"`temperature > 40`"+`. Choose whether it fires immediately, after a duration, or after repeated occurrences.
5. Optionally add a **clear condition**, e.g. `+"`temperature <= 35`"+`, so the alarm resolves itself automatically.
6. Choose **propagation** — whether the alarm should also be visible on related entities, the owner, or the whole tenant.
7. Click **Save**.

If a new trigger fires at a higher severity than an existing active alarm, ThingsBoard upgrades that alarm instead of creating a duplicate.`
  },

  'create-alarm': {
    title: 'How to trigger an alarm',
    markdown:
`Once an [alarm rule](/home/help/alarm-rules) is configured (for example: Critical when temperature > 25°C), you can trigger it by publishing a telemetry value that satisfies the condition.

Replace `+"`$ACCESS_TOKEN`"+` with your device's token:

    curl -v -X POST -d "{\\"temperature\\": 26}" \\
      http://<host>/api/v1/$ACCESS_TOKEN/telemetry \\
      --header "Content-Type:application/json"

Open **Alarms** in the left menu (or the device's **Alarms** tab) — the new alarm should appear within a few seconds, with the severity and details defined by the rule. Publishing a value that satisfies the clear condition (e.g. 24°C) resolves it automatically if one was configured.`
  },

  'assign-customer': {
    title: 'How to create a customer and assign a dashboard',
    markdown:
`Assigning entities to a **customer** lets that customer's users see only their own devices and dashboards — data belonging to other customers stays hidden.

## Create a customer

1. Open **Customers** in the left menu.
2. Click **+ Add customer**, enter a **title**, and optionally address/contact details.
3. Click **Add**.

## Assign a device and a dashboard

1. From the **Devices** table, select the device(s), then **Assign to customer** and pick the customer.
2. From the **Dashboards** table, do the same: select the dashboard, **Assign to customer**.

## Give the customer a login

1. Open the customer's details and click **Manage customer users**.
2. Click **+ Add user**, enter an email, and send/copy an activation link as with any other user.

Once activated, that customer user can only see the devices and dashboards assigned to their customer.`
  }
};
