## Solution instructions

Welcome to your new **Smart Irrigation** solution 👋 We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Irrigation Management</a> dashboard for you. Use it to:

* **Monitor** field conditions and soil moisture in real-time;
* **Manage** irrigation schedules and automate water valves;
* **Track** alarms for critical dryness or low battery levels;
* **Configure** crop types and moisture thresholds for each zone.

### 🖥 Mastering the dashboard

This solution is designed to simplify farm management by visualizing fields, sensors, and schedules in one centralized interface.

**1. Main State**
The main page gives you a bird's-eye view of your entire operation, allowing you to compare performance across different zones.

* **Interactive Map:** Displays your defined field polygons overlaid on a satellite map.
  * **Create:** Click the **"+ Add field"** button (top center) to draw a new polygon, assign a crop type (e.g., Wheat, Corn), and define specific moisture thresholds.
* **Fields List:** A summary table of all managed fields.
  * **Data Columns:** Quickly scan key metrics including: "Label", "Crop type", "Average moisture" (visual bar), and current "Irrigation" status (On/Off).
  * **Actions:** Use the buttons on the right to "Edit" ✏️ field settings or "Delete" 🗑️ it.
* **Moisture History:** Located at the bottom, this chart compares the historical soil moisture trends of all your fields over the last 7 days, helping you identify drying trends at a glance.

**2. Field Details State (Drill Down)**
To access the detailed management view, click on a "Field Polygon" on the map or click a "Row" in the Fields list.

This state allows for precise control and deep analytics:

* **Control & Status:**
  * **Irrigation status indicator:** View the current state of the water valves (ON/OFF).
  * **KPI Cards:** View real-time "Avg moisture" and "Daily water consumption".
  * **Alarms:** A dedicated button showing active alarms (e.g., "3 Major Alarms"). Click to investigate.
* **Scheduling & Tasks:**
  * **Schedule:** Configure recurring watering windows (e.g., "Morning", "Evening") with specific durations or volume limits.
  * **Irrigation Tasks:** A log tracking the progress and completion of past and upcoming watering events.
* **Sensors List️:** A granular list of every device in this field (Soil Moisture sensors, Valves) showing their individual status and values.
* **Analytics:** A dual-axis chart correlating "Soil Moisture" levels with "Water Consumption" over time to visualize irrigation efficiency.

### 🧬 Device & Asset Profiles

We have pre-configured specific profiles to define the behavior and data structure for your farm's hardware. 
These blueprints determine what telemetry your devices upload, which alarm rules configured for the profile, and which rule chain processes their data.

#### 🌾 SI Field

The field asset profile defines the logical boundary for your irrigation zones. 
It acts as the central hub, aggregating data from all sensors assigned to that specific field to enable zone-specific decision-making and irrigation control.

* **Default Rule Chain:** "SI Field" – responsible for field-level logic and irrigation control.
* **Data Processing Logic:** It automatically aggregates data from all connected sensors to calculate field-wide metrics:
  * **Average Moisture:** Computes the average saturation across all active sensors.
  * **5-min Average:** Smooths out data spikes by calculating a rolling 5-minute average.
  * **Irrigation State:** Updates the status (ON/DONE) based on water usage and duration thresholds. 
  * **Water Consumption:** Tracks the total water used during an active irrigation cycle.

#### 💧 SI Water Meter

This profile is designed for consumption tracking and monitor water meter batter health.

* **Default Rule Chain:** "SI Devices" – Processes telemetry and counts alarms.
* **Telemetry Upload:** It uploads the `pulseCounter` value (used to calculate total volume) and the current `battery` level.
* **Data Processing Logic:** It calculates the water consumption delta between pulses and propagates this data to the parent "SI Field" asset.
* **Alarm Logic:** It automatically raises a "Warning" alarm if the battery level drops below the min threshold (Default: 30).

<br>

**Sample Payload:**

```json
{"battery": 99, "pulseCounter": 123000}{:copy-code}
```

<br>

#### 🌵 SI Soil Moisture Sensor

* **Default Rule Chain:** "SI Devices" – Processes telemetry and counts alarms.
* **Telemetry Upload:** It uploads the soil `moisture` saturation level (0-100%) and the `battery` status.
* **Data Processing Logic:** It propagates the latest moisture reading to the parent "SI Field" asset for aggregation.
* **Alarm Logic:** It monitors three critical conditions:
  * **Low Battery:** Warning if `battery` level < Min Threshold (Default: 30).
  * **High Moisture:** Critical if level > Max Threshold (Default: 100).
  * **Low Moisture:** Critical if level < Min Threshold (Default: 0).

<br>

**Sample Payload:**

```json
{"battery": 99, "moisture": 57}{:copy-code}
```

#### 🚰 SI Smart Valve

This profile manages water flow control.

* **Default Rule Chain:** "SI Devices" – Processes telemetry and counts alarms.
* **Telemetry Upload:** It continuously uploads the `battery` status to ensure operational readiness.
* **RPC Control:** It accepts remote commands (`TURN_ON` / `TURN_OFF`) to physically enable or disable the irrigation line.
* **Alarm Logic:** It automatically raises a "Warning" alarm if the battery level drops below the min threshold (Default: 30).

<br>

**Sample Telemetry Payload:**

```json
{"battery": 99}{:copy-code}
```

<br>

**Sample RPC command:**

```json
{"method": "TURN_ON", "params": {}}{:copy-code}
```

### 🚨 Alarms

Your solution monitors field health based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the Device Profiles detailed in the previous section.

You can review the complete list of configured alarms in the table below:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🧮 Calculated fields

Solution utilizes <a href="${DOCS_BASE_URL}/user-guide/calculated-fields/" target="_blank">calculated fields</a> to derive irrigation-related metrics and indicators based on incoming device telemetry.

Key logic implemented includes:

* **Smart Aggregation:** The system computes the Average Field Moisture and a 5-minute Rolling Average to prevent erratic triggering from sensor spikes.
* **Irrigation Controller:** It actively tracks the Irrigation State (ON/DONE) by comparing the duration and water consumption against your defined targets.
* **Consumption Tracking:** It calculates the delta between water meter pulses to determine exact Water Consumption per session.
* **Data Propagation:** It automatically pushes configuration changes (like moisture thresholds) from the Dashboard down to individual sensors.

You can view the complete list of calculation scripts below:

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

**💡 Tip:** The aggregation is done in the UTC time zone by default. You may configure settings like the time zone or interval duration in "Time series data aggregation" calculated fields.

### ⛓️ Rule Chains

* **SI Devices** rule chain processes telemetry from all device types, including soil moisture sensors, water meters, and smart valves.
  This rule chain is responsible for telemetry ingestion and alarm counting.
* **SI Field** rule chain is responsible for field-level logic and irrigation control.
  It processes aggregated events related to a field and performs actions such as:
  * starting and stopping irrigation;
  * controlling smart valve devices via RPC;
  * reacting to water consumption limits or irrigation duration rules.

### 🔌 Devices

We have pre-provisioned 12 devices with demo data across 2 fields to simulate a working farm. See device info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard react? Simulate a "Critical Dryness" event for Field 1.
Solution expects that the device telemetry will correspond to the samples provided in device profile section of the instruction.

The most simple example of the moisture sensor payload is in JSON format:

```json
{"moisture": 77}{:copy-code}
```

<br>

To emulate the data upload on behalf of device "SI Soil Moisture 1" located inside field "Field 1", one should execute the following command to raise the Critical Alarm for Field 1:

```bash
curl -v -X POST -d "{\"moisture\":  77}" ${BASE_URL}/api/v1/${SI Soil Moisture 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a> for simplicity of demonstration.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard — you should see the values update instantly 🚀

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

${edge_instructions}
