## Solution instructions

Welcome to your new **Air Quality Monitoring** solution 👋 We have generated two robust dashboards (Public & Administration) for you. Use this solution to:

* **Monitor** real-time Air Quality Index (AQI) and specific pollutants;
* **Visualize** city-wide pollution levels on an interactive map;
* **Manage** sensor health, battery levels, and connectivity alarms;
* **Inform** the public with automated health recommendations.

### 🖥 Mastering the dashboards

This solution includes two distinct interfaces: one for the **Public** (End Users) and one for **Tenant Administrators**.

#### 🌍 Public Air Quality Monitoring Dashboard 

This dashboard is designed for end-users and requires no login. It is pre-configured to monitor **Los Angeles** and operates in two main views within the same interface.
The dashboard layout consists of an interactive map on the right and a dynamic information sidebar on the left.

**1. City State (Aggregate View)**

This view represents the overall air pollution monitoring of the specific city. It calculates values based on data received from all city sensors.

<div class="img-float" style="max-width:50%; margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-city-state.png" alt="AQI Public Dashboard - City State">
</div>

* **City Details:** Displays the name of the current city (Los Angeles) along with real-time "Temperature" and "Humidity".
* **Pollution Status:** Shows the current status according to "EPA standards" (e.g., "Clean", "Unhealthy").
* **AQI Scale:** Displays the average "AQI value" based on all sensors in the city, visualized on a color-coded scale for convenient viewing.
* **Legend Info:** Click the ℹ️ "info-icon" under the AQI value to open a pop-up with the full legend of pollutants.
* **Recommendations:** A dedicated section provides general recommendations for sensitive groups of people regarding the current level of pollution.
* **History:** A chart displaying the AQI level trends in the **Live**, **weekly**, and **monthly** range.
* **Interactive Map:** Shows air pollution monitoring stations across the city. The markers are **color-coded** depending on their specific AQI level.

**Interaction:** Click on any sensor marker on the map to **instantly update the dashboard** with data for that specific district (Sensor State). You can switch between districts by clicking different markers.

<br>

**2. Sensor State (District View)**

When a marker is selected (e.g., Beverly Hills), the sidebar updates to show the local state of that specific district.

* **Local Data:** Displays the same metrics as the City state (Temperature🌡️, Humidity 💧, Recommendations), but the values are specific to the selected sensor.
* **Specific Pollutants:** A dedicated section of tiles displaying values for **PM2.5, PM10, NO2, CO, SO2,** and **O3** 🧪.
    * *Pop-ups:* Click on any tile to view a description, specific recommendations, and statistics for that pollutant.

<div class="img-float" style="max-width:50%; margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-sensor-state.png" alt="AQI Public Dashboard - Unhealthy State (Beverly Hills)">
</div>

<br>

**💡 Tip:** Want to share this? You can embed the public dashboard on your website using this code:

```html
<iframe src="${BASE_URL}${MAIN_DASHBOARD_PUBLIC_URL}" style="position:fixed; inset:0; width:100%; height:100%; border:none;"></iframe>{:copy-code}
```

#### 🛠️ Air Quality Monitoring Administration Dashboard

This <a href="${Air Quality Monitoring AdministrationDASHBOARD_URL}" target="_blank">**dashboard**</a> is designed for tenant administrators to oversee the entire sensor fleet. It allows you to add new devices, monitor technical health (battery, connectivity), and configure alarm rules.

**1. Main State**

The central hub for device management, divided into three key areas:

* **Sensors List:** a comprehensive table displaying the technical status of every device.
    * **Columns:** Quickly check the "Sensor Label", "Connection" status (Connected/Disconnected), "Battery level", and "Last AQI" reading.
    * **Actions:** Use the icons on the right to "Edit" ✏️ or "Delete" 🗑️ sensors. You can also provision new devices using the "+" table header button.
* **Alarms Console:** a real-time log of all active and cleared alerts across the system.
    * **Data:** Shows the "Created time", "Type" of alarm (e.g., Low Battery Level, Inactive), "Sensor id" and current alarm "Status".
    * **Configuration:** Click the settings icon ⚙️ "Alarm rules" in the header to configure global thresholds, such as the minimum **Battery Level %** or the timeout duration for **Connection Loss**.
* **Interactive Map:** visualizes the physical location and status of your AQI sensors.
    * **Actions:** Click the "+ Add sensor" button at the top of the map to provision a new device and place it in the map.
    * **Markers:** Color-coded based on the sensor's real-time status: 
      * **🔴 Red:** Has active alarms (Critical state).
      * **🟢 Green:** Normal state (Healthy and reporting data).
      * **⚪ Gray:** No data available (Sensor has not reported AQI or Battery level yet).
    * **Pop-ups:** Click any marker to see a summary card with the Sensor ID, Battery, and Last AQI. Click the "Details" link to drill down into that specific sensor details state.

<div class="img-float" style="max-width: 50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-admin-state-1.png" alt="AQI Administration Dashboard - Sensor State">
</div>

**2. Sensor State (Drill Down)** 

Accessed by clicking a table row or the "Details" link in the tooltip of the selected sensor on the map.
This view provides deep diagnostics for a single sensor.

* **Diagnostics:** Detailed charts for **Battery Level** history and **Connection Status** uptime.
* **Telemetry:** A live stream of all incoming data points (PM2.5, CO, NO2, etc.).
* **Location Management:** An embedded map allowing you to drag-and-drop the sensor to update its precise coordinates.
* **Device Alarms:** A filtered list showing only the alarms relevant to this specific sensor.

<div class="img-float" style="max-width: 50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-admin-state-2.png" alt="AQI Administration Dashboard - Sensor State">
</div>

### 🔌 Devices

We have pre-provisioned 5 Air Quality Sensors and loaded them with demo data to help you explore the solution immediately.

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

The solution expects the sensor to upload pollution values (`pm25`, `co`, `no2`, etc.), weather data (`temperature`, `humidity`), and `batteryLevel`. The most simple example of the expected payload is in JSON format:

```json
{"temperature": 42, "humidity": 73, "pm25": 24.4, "pm10": 30, "no2": 13, "co": 2.8, "so2": 7, "o3": 0.164, "batteryLevel": 77 }{:copy-code}
```

<br>

Want to see the dashboard update in real-time? Execute the following command to push a full telemetry packet to "Air Quality Sensor 1":

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  0.164, \"batteryLevel\":  77 }" ${BASE_URL}/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard — you should see the values update instantly 🚀

### 🚨 Alarms

Your solution monitors device health based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the "AQI Sensor" device profile:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

You can manually turn alarms on/off or configure specific thresholds
(e.g., Battery Level limit) via the Administration Dashboard by clicking the settings ⚙️ icon "Alarm rules" in the Alarms section.

### 🧮 Calculated fields

Solution utilizes <a href="${DOCS_BASE_URL}/user-guide/calculated-fields/" target="_blank">calculated fields</a> to compute AQI values and derived indicators based on incoming telemetry.

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

### 📂 Entity Groups

The solution organizes your infrastructure into clear logical containers to keep your tenant structured:

* **AQI city (Asset Group):** Stores all city entities managed by your tenant.
* **AQI Sensor (Device Group):** Stores all physical air quality sensors belonging to those cities.

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

### 📚 Examples & Scenarios

#### Scenario 1: Triggering a Low Battery Alarm 🔋

**Goal:** Verify the alarm system by simulating a critical battery drop on Air Quality Sensor 1 (Hollywood). Current battery level value: 43.73%

<br>

<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-1.png" alt="">
</div>

<br>

**1. Configure Threshold:** First, let's make the rule stricter. Go to the Admin Dashboard, click the settings ⚙️ icon "Alarm rules" in the Alarms section. Set the Battery Level threshold to 10% and click Save.

<br>

<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-2.png" alt="">
</div>

<div class="img-float" style="max-width: fit-content;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-3.png" alt="">
</div>

<br>

**2. The Action:** Simulate a battery level of 9% (below the new limit) using this command:

```bash
curl -v -X POST -d "{\"batteryLevel\": 9 }" ${BASE_URL}/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**3. Result:** The system detects the value (9%) is lower than the limit (10%) and triggers a Low Battery Alarm. You will see this appear in the Alarms Console and on the Map.

<br>

<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-4.png" alt="">
</div>


##### Scenario 2: Reporting Hazardous Air Quality 😷

**Goal:** Simulate a dangerous pollution event using PM2.5 data on Air Quality Sensor 2 (Downtown).

**1. Current State:** The sensor currently reports a moderate level (e.g., PM 2.5 in AQI equivalent is 52).

<div class="img-float" style="max-width: 60%;margin:10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-2-1.png" alt="">
</div>

<br>

**2. The Action:** Simulate a massive spike in PM2.5 to 400 μg/m³ (a Hazardous level):

```bash
curl -v -X POST -d "{\"pm25\": 400 }" ${BASE_URL}/api/v1/${Air Quality Sensor 2ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**3. Result:** The Calculated Fields logic processes the PM2.5 reading, computes a new AQI of 433, and instantly updates the dashboard status to "Hazardous" (Purple).

<br>

<div class="img-float" style="max-width: 50%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-2-2.png" alt="">
</div>
<div class="img-float" style="max-width: 50%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-2-3.png" alt="">
</div>


${edge_instructions}
