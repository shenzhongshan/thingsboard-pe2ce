## Solution instructions

Welcome to your new **Smart Retail** solution 🛒
As part of this solution, we have generated 2 dashboards, 8 device profiles, and 20+ test devices with simulated data. 
We have also created sample Customers and Users to demonstrate the workflow.

Use this solution to:
* **Provision** retail companies and supermarkets;
* **Manage** floor plans and device placement;
* **Monitor** critical alarms (Temperature, Smoke, Intrusion);
* **Track** shelf weights and bin fullness levels.

### 🖥 Mastering the dashboards

#### 🛠️ Smart Supermarket Administration

<br>

The <a href="${Smart Supermarket AdministrationDASHBOARD_URL}" target="_blank">Smart Supermarket Administration</a> dashboard is your central hub for provisioning customers, their users, supermarkets, and devices.

<br>

**1. Main State (Companies Overview)**

This state lists the "Retail Companies" (Customers) that own the supermarkets. 
We assume that the customer is a retail company that own one or multiple supermarkets.
We have provisioned two "fake" retail companies with number of supermarkets for demonstration purposes.

* **Retail companies:** View all provisioned retail companies.
  * **Add company:** Click the "+" button in the top right corner of the retail companies table to create a new customer. You may input the company name, address, and other information.
  * **Manage devices:** Click the "Manage devices" button in the corresponding company row to open the device management state.
  * **Manage users:** Click the "Manage users" button in the corresponding company row to open the user management state.
  * **Manage supermarkets:** Click the "Manage supermarkets" button or simply the corresponding company row to open the supermarket management state.

**2. Device Management State**

This state allows you to manage devices in the scope of the retail company (customer).
You may provision new devices or delete existing devices. The state displays a table with all devices assigned to this retail company.
This means that Tenant Administrator or Supermarket Administrator will be able to use those devices to position them in the Supermarket.
You may treat this list as a pool of devices that are available for installation in the Supermarkets of the Customer.

* **Devices:** View all provisioned devices in scope of the selected retail company (customer).
  * **Add Device:** Click the "+" button in the top right corner of the **Devices** table to create a new device. You will be able to input device name (unique identifier), label (e.g., "Fruits shelf"), and type.
  * **Device Type:** The drop-down list of available device types corresponds to the names of pre-configured device profiles for this solution (see **Device profiles** for details).
  * **Edit:** Click the "Edit" ✏️ button in the corresponding device row to edit the device.
  * **Delete:** Click the "Delete" 🗑️ button in the corresponding device row to delete the device.

**💡 Note:** Instead of provisioning devices manually, you may navigate to the "Supermarket Devices" group of the customer and use the bulk provisioning feature.

<br>

**3. Supermarket Management State**

Supermarket management state allows you to manage supermarkets in the scope of the retail company (customer).
The dashboard state displays supermarkets on the map and a list of supermarkets in the table.
Supermarkets are assets that may contain multiple devices and few attributes: floor plan and address.

* **Map:** View the geographical location of your stores (Left side).
    * **Add Supermarket:** Click the "+ Add supermarket" button to provision a new supermarket asset. The marker will be placed in a default location on the map. You will be able to drag the market to define the precise location of the asset.
    * **Markers:** Click a supermarket marker to open the details panel and a popup containing links to **"Supermarket devices"** and **"Delete"**.
    * **Details Panel:** When a marker is selected, the right panel displays the supermarket's details panel instead of the default view of supermarket lists. You can provision the floor plan and define the both address and location here.
    * **💡 Tip:** Although it is possible to lookup location based on address, this feature requires API key for Google Map or similar service to be defined and is not included in the solution out-of-the-box.

* **List:** View the supermarket roster in a table (Right side).
    * **Overview:** Displays the "Name" and "Address" of each supermarket.
    * **Actions:** You can click the 🔌 "Supermarket devices" button in the table row to navigate to the **"Supermarket devices"** state, or click the 🗑 "Delete" button to remove the supermarket asset.

**4. Supermarket Devices State**

The Supermarket devices state displays an indoor map with the floor plan of the supermarket and device markers.
You may drag-and-drop the device markers to define the precise location of the device in the supermarket.

* **Add Device:** Click the **"+ Add device"** button to start the provisioning process.
    * **Set Location:** First, click on the specific spot on the floor plan where you want to place the device.
    * **Select Device:** Once the location is set, a dialog will open allowing you to select a new device from the pool of available devices and customize its label.
    * **Note:** You may add more devices to the pool via the **Device management** state.

* **Marker Actions:** Click a device marker on the map to open a popup with management links:
    * **"Update label":** Allows you to edit the custom label for the device.
    * **"Remove device":** Unassigns the device from the supermarket and makes it available for use in other supermarkets belonging to the customer.

#### 2. 🏪 Smart Supermarket (Manager View)

<br>

The <a href="${MAIN_DASHBOARD_URL}" target="_blank">Smart Supermarket</a> dashboard is designed for supermarket managers to monitor the state of the supermarkets and react to alarms.

**1. Main State (Regional Overview)**

This state contains a map of the supermarkets and a list of alarms.
Alarms are propagated from devices to the corresponding supermarket, and the platform calculates the state of each supermarket based on the highest severity of the propagated alarms.

* **Global State Filters:** Located at the top of the map, these toggle switches (Normal, Major, Critical) allow you to filter the **entire dashboard state**.
    * **Toggling a filter** (e.g., "Critical") updates not only the map markers but also filters the **Supermarket List** and the **Alarms List** to show only entities matching that state.
* **Selection & Drill-Down:** Click a supermarket marker on the map or a row in the list to select a specific store. This action updates the entire dashboard context:
    * **Devices List:** The right-side panel transforms from a list of all supermarkets to a table displaying the **Devices** inside the selected store.
    * **Filtered Alarms:** The bottom alarms list automatically filters to show only alerts related to that specific supermarket. You can clear and acknowledge the alarms in that list.
* **Severity Indicators:** Once a supermarket is selected, specific "Critical" and "Major" summary buttons become active at the top of the map (they are inactive by default). Click these active buttons to open a popup table listing the specific alarms corresponding to that severity.
* **Alarms List:** The bottom section displays a real-time list of alerts. You can use the specific actions in each row to "Clear" ❌ or "Acknowledge" ✅ the alarms directly from this view.
* **Navigation:** To return to the global view, click the **"←" (Go back)** arrow button located in the header of the Devices list or header of the Map.
* **Open Floor Plan:** To view the detailed indoor map of the selected store, click the **"Open details"** button in the top right corner or click the **"Details"** link in the map popup of the selected supermarket marker.


**2. Floor Plan State (Store Monitoring)**

The Floor Plan state contains an indoor map with the supermarket layout and device markers.
Besides the map, the state features persistent filters and summary controls to help you visualize specific data.

* **Severity Indicators:** Located at the top of the map (similar to the Main State), these buttons display the total count of active **Critical** or **Major** alarms for this specific store.
    * **Click these buttons** to open a popup table listing the specific alarms corresponding to that severity.

* **Persistent Filters:** Located below the map, these settings are saved on the user level.
    * **Device Type Filter:** Show or hide specific hardware groups (e.g., toggle "Freezers" on and "Smart Shelves" off).
    * **State Filter:** Filter devices based on their current health severity (e.g., display only devices that have at least one **Critical** alarm).

* **Device Details:** Click on a specific device marker to open the **Device Details Panel** on the right side of the dashboard.
    * **Visualization:** Content is specific to the device type (e.g., Freezers show temperature line charts; Smart Bins show fullness bar charts).
    * **Header Info:** Displays the current state and battery level (if a device is battery powered).
    * **Settings:** Access the configuration menu from the header to adjust specific **alarm thresholds** for that device.
    * **Device Alarms:** A list of active alarms specific to the selected device appears at the bottom of the panel.

### 🔌 🚨 Device Profiles & Alarms

The solution includes 8 pre-configured device profiles. Each profile comes with default alarm thresholds, which are common for all devices of that type.

* **⚙️ Tuning:** Supermarket manager may tune alarm thresholds for each specific device by navigating to device details via <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Smart Supermarket"</a> dashboard.
* **💡 Note:** Many profiles include battery monitoring rules (e.g., raising alarms when `batteryLevel` drops below 30% or 10%). If your sensor is not battery powered, you can simply ignore these specific rules.

#### ❄️ Cold Chain Monitoring

##### ❄️ Freezer

* **Goal:** Ensure products stay frozen within a safe range.
* **Alarm Rules:**
  * **Major:** Temperature is above **-2°** or below **-25°**.
  * **Critical:** Temperature is above **-1°** or below **-30°**.

**Payload:** 

```json
{"temperature": -5.4}{:copy-code}
```

##### 🌡️ Chiller

* **Goal:** Ensure products stay chilled (similar to Freezer logic but with different default thresholds).

**Payload:** 

```json
{"temperature": 6.2}{:copy-code}
```

#### 📦 Inventory & Stock

##### ⚖️ Smart Shelf

* **Goal:** Monitor product availability based on weight.
* **Alarm Rules:**
  * **Major:** Weight drops below **20 units** (kg/lbs).
  * **Critical:** Weight drops below **10 units**.

**Payload:**

```json
{"weight": 42}{:copy-code}
```

##### ♻️ Smart Bin

* **Goal:** Optimize waste management by tracking fullness.
* **Alarm Rules:**
  * **Major:** Fullness level exceeds **70%**.
  * **Critical:** Fullness level exceeds **90%**.
  * **Battery:** Low battery alerts (< 30% Major / < 10% Critical). 

**Payload:** 

```json
{"level": 35, "batteryLevel": 89}{:copy-code}
```

##### 💧 Liquid Level Sensor

* **Goal:** Track levels of sanitizers or soap dispensers.
* **Alarm Rules:**
  * **Major:** Level drops below **30%**.
  * **Critical:** Level drops below **10%**.
  * **Battery:** Low battery alerts (< 30% Major / < 10% Critical). 

**Payload:** 

```json
{"level": 85, "batteryLevel": 89}{:copy-code}
```

#### 🛡️ Security & Infrastructure

##### 🚪 Door Sensor

* **Goal:** Detect unauthorized access or doors left ajar.
* **Alarm Rules:**
  * **Left Open:** Major if is left open > **30 mins**; Critical if is left opened > **1 hour**.
  * **Intrusion:** Critical alarm if opened during **non-working hours** (configurable schedule).
  * **Battery:** Low battery alerts (< 30% Major / < 10% Critical).

**Payload:**

```json
{"open": true, "batteryLevel": 99}{:copy-code}
```

##### 🏃 Motion Sensor

* **Goal:** Detect movement in restricted areas or after hours.
* **Alarm Rules:**
  * **Intrusion:** Critical alarm if motion is detected during **non-working hours** (configurable schedule in the alarm rule of the device profile).
  * **Battery:** Low battery alerts (< 30% Major / < 10% Critical).

**Payload:**

```json
{"motion": true, "batteryLevel": 99}{:copy-code}
```

##### 🔥 Smoke Sensor

* **Goal:** Immediate fire detection.
* **Alarm Rules:**
  * **Fire:** Critical alarm if smoke is detected (`"alarm": true`).
  * **Battery:** Low battery alerts (< 30% Major / < 10% Critical).

**Payload:**

```json
{"alarm": false, "batteryLevel": 99}{:copy-code}
```

##### ⏱️ Occupancy Sensor

* **Goal:** Monitor room usage duration.
* **Alarm Rules:**
  * **Prolonged Use:** Major if occupied > **30 mins**; Critical if occupied > **1 hour**.
  * **Battery:** Low battery alerts (< 30% Major / < 10% Critical).

**Payload:**

```json
{"occupied": true, "batteryLevel": 99}{:copy-code}
```

Below is the complete list of alarm rules configured for this solution:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🔌 Devices

We have already created 20+ devices and loaded some demo data for them. See device info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Solution expects that the device telemetry will correspond to the samples provided in device profile section of the instruction.
The most simple example of the freezer payload is in JSON format:

```json
{"temperature": -5.4}{:copy-code}
```

<br>

To emulate the data upload on behalf of device "Freezer 1" located inside supermarket "Supermarket S1", one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  -5.4}" ${BASE_URL}/api/v1/${Freezer 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a> for simplicity of demonstration.

#### 📡 Production Connectivity & Data

For real-world deployments, supermarket devices are typically connected via gateways or specialized networks. 
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for full details, o
r consider these common options:

* **IoT Gateway:** Use the open-source <a href="https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/" target="_blank">ThingsBoard IoT Gateway</a> to connect local legacy devices (Modbus, BACnet, BLE, etc.).
* **Custom Gateway:** Develop your own gateway using the <a href="https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/" target="_blank">ThingsBoard MQTT Gateway API</a>.
* **Integrations:** Seamlessly connect LoRaWAN, Sigfox, and NB-IoT devices via <a href="https://thingsboard.io/docs/user-guide/integrations/" target="_blank">Platform Integrations</a>.

### 🎭 Roles

* **Smart Retail Read Only:** Created to share read-only access to **Smart Supermarket** dashboards with all users of all customers.
* **Smart Retail User:** Designed for **Supermarket Users**. This role allows read-only access to all entities and write access to alarms and device/asset attributes.
* **Smart Retail Administrator:** Designed for **Supermarket Administrators**. This role allows write access to devices and assets within a specific Customer (if assigned to a Customer User) or the entire Tenant (if assigned to a Tenant User).

### 📂 Entity Groups

**1. Customer Level**
Each Customer is provisioned with the following groups to organize their specific entities:

* **Supermarkets (Asset Group):** Stores all supermarkets that belong to this customer.
* **Supermarket Devices (Device Group):** Stores all devices that belong to this customer.
* **Unassigned Devices (Device Group):** Stores devices that are not yet assigned to any specific supermarket (the installation pool).
* **Smart Retail Users (User Group):** Stores users assigned the "Smart Retail User" role.
* **Smart Retail Administrators (User Group):** Stores users assigned the "Smart Retail Administrator" role.

**2. Tenant Level**
The Tenant maintains specific dashboard groups to manage sharing policies:

* **Supermarket Users Shared:** Shares the **Smart Supermarket** dashboard in read-only mode with all Customer user groups named "Smart Retail Users".
* **Supermarket Admins Shared:** Shares the **Smart Supermarket Administration** dashboard in read-only mode with all Customer user groups named "Smart Retail Administrators".

### ⛓️ Rule Chains

* **Supermarket Devices:** rule chain is responsible for processing all telemetry from devices.
    * **Alarm Count Propagation:** This chain uses the "alarms count" node to calculate and propagate the total number of active alarms to the Tenant, Customer, and Supermarket assets, ensuring the dashboard severity indicators are accurate.

**💡 Note:** Alarms raised automatically based on the **alarm rules** defined in the device profiles.


### 🏢 Customers

The solution comes with pre-provisioned data to demonstrate multi-tenancy:
* **Retail Company A:** Contains Supermarkets "S1" and "S2".
* **Retail Company B:** Contains Supermarket "S3".

**User Structure**

Both "Retail Company A" and "Retail Company B" are configured with two distinct users to demonstrate different access levels:

**1. Supermarket Manager:** Assigned the **Smart Retail Administrator** role and defaults to the **Smart Supermarket Administration** dashboard.

**2. Supermarket User:** Assigned the **Smart Retail User** role and defaults to the **Smart Supermarket** dashboard.

**💡 Tip:** You may create more Customers and Users via the <a href="${Smart Supermarket AdministrationDASHBOARD_URL}" target="_blank">Smart Supermarket Administration</a> dashboard.

<div class="tb-markdown-view table-wrapper">

${user_list}

</div>

### 🛠️ Implementation Details

#### 📊 Dashboards

Experienced users may notice that the solution dashboards utilize several advanced features introduced in ThingsBoard version 3.3.3.

* **Embedded States:** The most significant feature is the ability to embed multiple dashboard states into a single widget. This is achieved via the new **"Dashboard State"** widget or by using the `<tb-dashboard-state>` component within Markdown card widgets.
* **Modular Architecture:** The dashboard consists of **30+ states** that serve as building blocks for more advanced views.
* **Dynamic Loading:** For an advanced usage example, review the **device_card_details** state. It uses a markdown value function to dynamically load specific states based on the device type.

#### 🔌 Device Profiles

* **Dynamic Thresholds:** The alarm thresholds tuned by the supermarket administrator are stored as **Server-Side Attributes** on the device.
* **Example:** Smart Shelf devices use `lowWeightMajorThreshold` and `lowWeightCriticalThreshold`. These attributes are referenced in the **Alarm Rules** of the corresponding device profile as the source for dynamic thresholds.

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

${edge_instructions}
