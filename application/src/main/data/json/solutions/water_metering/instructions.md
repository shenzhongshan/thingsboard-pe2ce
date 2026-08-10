## Solution instructions

Welcome to your new **Water Metering** solution 👋
This template is a complete Smart Utility platform designed for municipalities, sub-metering companies, and building managers. Use this solution to:

* **Monitor** real-time water consumption and detect anomalies;
* **Visualize** your entire meter network on an interactive, clustered map;
* **Detect** leaks, pipe bursts, and freezing conditions instantly;
* **Manage** customers and automate sub-metering data collection.

### 🖥 Mastering the dashboards

This solution includes two distinct interfaces:

1. <a href="${MAIN_DASHBOARD_URL}" target="_blank">Water Metering Tenant Dashboard</a>: for the utility provider to manage the entire metering infrastructure and customer base.

2. <a href="${Water Metering User DashboardDASHBOARD_URL}" target="_blank">Water Metering User Dashboard</a>: simplified view for end-users to monitor their own water usage.

You may always customize dashboards using dashboard development <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">guide</a>.

#### 🛠️ Water Metering Tenant Dashboard

This interface allows you to manage the entire hierarchy: from the physical meter to the end-user customer. It is organized into six key tabs.

**1. Total (Overview)**

The default landing page designed for a high-level operational summary.

* **KPI Cards:** Instant metrics for "Consumed per current week", "Active devices", "Low battery", and "Inactive devices".
* **Interactive Map:**
  * **Clustering:** Markers are grouped to handle thousands of meters.
  * **Quick Add:** Use the "+ Add water meter" button directly on the map to provision new water meters.
* **Water Meters:** A quick-access table on the right side showing the most relevant device data.
  * **Columns:** Check "Serial Number", "Latest reading", "Status", and "Battery level" at a glance.
  * **Actions:** Add new sensors via the widget header "+" button. Also, click on "row" will navigate you to specific **water meter details** state.
* **Consumption & Alerts:** A split view showing a "Water Consumption" bar chart (Daily/Weekly) and a list of "Active Alarms" for immediate attention.

**2. Analytics**

Navigate here for deep-dive consumption analysis.

* **Consumption Chart:** A detailed line graph comparing water usage over time (Current vs. Previous interval), helping you identify trends.

**3. Devices**

Navigate to the **"Devices"** tab to manage your physical sensors.

* **Water Meters:** A comprehensive table managing your entire metering network.
  * **Columns:** View "Serial Number", "Latest Reading", "Address", "Status", and "Leakage" status (🟢 **Green** dot means that there is no leak).
  * **Actions:** Use the widget header button "+" to add new meters. Use row buttons to "Edit" ✏️, "Locate" 📍, or "Delete" 🗑️ water meters. As usual, click on "row" will navigate you to specific **water meter details** state.
  * **Assignment:** Click the "Edit" ✏️ button on any row to change the **Device Owner**. This allows you to re-assign a meter from the Tenant (Stock) to a specific Customer (Rental/Lease) instantly.
* **Active Alarms:** A list showing active alarms for all sensors.

**4.Customers**

This solution supports a **Supplier/Rental model**. As a Tenant Admin, you own the meters but can assign them to customers and unassign them by changing ownership.

* **Onboard Customers:** Click the "+" button to create a new customer. This **automatically creates a user account**, under newly created customer, allowing the customer to log in to their personal dashboard immediately.
* **Manage Inventory:** Click on any customer row to **drill down** into their specific state. This opens a view showing only the devices currently assigned to that customer.
* **Provisioning:** While inside a customer's view, you can create a new device to provision a meter straight from the warehouse directly to that customer's account.

**5. Alarms**

A dedicated console for operational health and maintenance allow you
to track critical issues like "Daily/Weekly Consumption Threshold Exceeded"
or "Leakage Detected" or "Device Inactive" etc., across all devices.

**6. Settings**

Configure the business logic of your solution without coding.

* **System Alarms:** Toggle global alerts for "Low Battery", "Low Temperature", "Inactivity" and "Daily/Weekly Consumption Threshold Exceeded".
  * **Thresholds:** Set specific limits (e.g., "Max Daily Consumption = 100") to trigger alerts depends on your sensor limits or business logic.
* **Notifications:** Enable SMS or Email routing for specific alarm types.


#### 👤 Water Metering User Dashboard

When a Customer User logs in, they are presented with a **full-screen dashboard** that is visually similar to the Tenant view but with restricted permissions and data scoped strictly to their assigned devices.

**Key Differences:**

**1. Total (Customer View)**
* **Simplified KPIs:** Focuses purely on consumption metrics (e.g., "Consumed per day", "Consumed per current week") relevant to their bill, rather than the operational health metrics seen by the Admin.
* **Read-Only Map:** Shows only the meters owned by the customer. Crucially, the "+ Add water meter" button is removed.

**2. Analytics**
* **Personal Tracking:** Users can compare their current usage against previous periods to track their own efficiency.

**3. Devices (Read-Only)**
* **Inventory:** Users can view their list of meters and check status/consumption, but the add "+" and delete 🗑 buttons are removed.
* **Actions:** Row actions are limited to "Edit" ✏️ (change label) and "Locate" 📍.

**4. Settings (Restricted)**

Customers can configure alerts relevant to their property usage, but technical device health settings (managed by the Tenant) are hidden.

* **Visible Settings:** Customers **can** toggle alarms and modify the specific thresholds for "Low Temperature" (Freeze risk), "Daily Consumption", and "Weekly Consumption".
* **Technical Alarms:** Alarms related to device health (e.g., "Low Battery", "Inactivity") are not applicable to the Customer, as maintenance is handled entirely by the Tenant.
* **Notifications:** Customers can toggle SMS or Email notifications, but only for the specific alarms visible to them (Temperature and Consumption).

### 🔌 Devices

We have pre-provisioned **3 Water Meters** and loaded them with demo data. See the device info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

Solution expects that the water meter device will report `pulseCounter`, `temperature`, `battery` and `leakage` values.
The most simple example of the expected payload is in JSON format:

```json
{"pulseCounter":  550, "temperature":  22.0, "battery":  97, "leakage":  false}{:copy-code}
```

#### ⚡ Test it now

To emulate the data upload on behalf of device "WM0000123", one should execute the following command:

```bash
curl -v -X POST -d "{\"pulseCounter\":  550, \"temperature\":  22.0, \"battery\":  97, \"leakage\":  false}" ${BASE_URL}/api/v1/${WM0000123ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Most of the water meters are using LoRaWAN, Sigfox or NB IoT technology. Please check <a href="https://thingsboard.io/docs/user-guide/integrations/" target="_blank">ThingsBoard Integrations</a> for more info.

### 🚨 Alarms

Alarms are generated based on <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the "Water Meter" device profile and thresholds set in the Dashboard Settings.

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🧮 Calculated fields

Solution utilizes <a href="${DOCS_BASE_URL}/user-guide/calculated-fields/" target="_blank">calculated fields</a> to derive daily and weekly water consumption metrics and analytical indicators based on incoming telemetry.

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

**💡 Tip:** The aggregation is done in the UTC time zone by default. You may configure settings like the time zone or interval duration in "Time series data aggregation" calculated fields.

### ⛓️ Rule Chains

The system includes intelligent processing logic:

* **Water Metering Solution Main:** Processes incoming telemetry and route alarm action messages for notifications.

* **Alarm Routing:** Two distinct chains (Tenant and Customer) ensure the right person gets the alert. 
  * **Water Metering Solution Tenant Alarm Routing:** Responsible for sending notifications to the Tenant Administrator. It routes all system alarms, including "Leakage Detected", "Device Inactive", "Low Battery", "Low Temperature", and Consumption Thresholds.
  * **Water Metering Solution Customer Alarm Routing:** Responsible for sending notifications to the Customer User. It routes only personal usage alarms: "Daily Consumption", "Weekly Consumption", and "Low Temperature".

### 👥 Customers and Users

We have pre-created two customers to demonstrate the multi-customer capabilities:

* **Water Metering Customer A:** Assigned devices "WM0000123" and "WM0000124".

* **Water Metering Customer B:** Assigned device "WM0000125".

Each customer has a dedicated User account automatically created for them, with the "Water Metering User Dashboard" shared with them by default. 
You may create more Customers and Users directly via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Water Metering Tenant Dashboard"</a>.

<div class="tb-markdown-view table-wrapper">

${user_list}

</div>

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

${edge_instructions}
