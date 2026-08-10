## Solution instructions

Welcome to your new **Site Fleet Tracking** solution 👋
We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Mine site monitoring</a> dashboard for you. Use it to:

* **Observe** real-time positions of excavators and haul trucks;
* **Monitor** geofencing events and alarms;
* **Browse** movement history and fuel levels.

### 🖥 Mastering the dashboard

The **Main** state provides a complete overview of your mining operations:
* **KPI Cards:** Monitor real-time counts of machines in Loading, Unloading, or Restricted zones, along with daily fuel consumption.
* **Map:** Visualize geofenced zones and the live location of your machinery.
* **Lists:** View detailed tables for **Zones**, **Machines**, and active **Alarms**.

**Drill Down features:**
* **Machine Details:** Click any machine row in the table or click the "See Details" button in a map tooltip to inspect specific alarms, hydraulic pressure (for excavators), and load weight (for haul trucks) history.
* **Zone Details:** Click any zone row in the table to view a list of all machines currently inside that zone, along with their alarms.

**Interactive Simulation:** 

The map markers are **movable**! You can drag and drop a machine from one zone to another directly on the map.
This simulates a real-time coordinate change (`latitude`/`longitude`), automatically triggering the corresponding 
**Geofencing** calculated fields logic to update the machine's status and generate alarms.

You can always customize this dashboard using our <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

We have pre-provisioned two excavators and three haul trucks with demo data. You can find their info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard come alive? You can simulate a machine right now.
The solution expects telemetry like `latitude`, `longitude`, `speed`, `fuelLevel`, and machine-specific data (`loadWeight` for haul trucks and `hydraulicPressure` for excavators).

The most simple example of the expected payload is in JSON format:

```json
{
  "latitude": 36.215322,
  "longitude": -88.665637,
  "speed": 18.5,
  "fuelLevel": 72.3,
  "loadWeight": 56000
}{:copy-code}
```

<br>

To emulate data upload on behalf of device "Haul truck A", execute the following command:

```bash
curl -v -X POST -d "{\"latitude\": 36.215322,\"longitude\": -88.665637,\"speed\": 18.5,\"fuelLevel\": 72.3,\"loadWeight\": 56000}" ${BASE_URL}/api/v1/${Haul truck AACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard — you should see the values update instantly 🚀

### 🚨 Alarms

Your solution monitors data based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the "Excavator" and "Haul truck" device profiles:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🧮 Calculated fields

Solution utilizes <a href="${DOCS_BASE_URL}/user-guide/calculated-fields/" target="_blank">calculated fields</a> to derive new telemetry values and geofencing events based on incoming data.
They are configured in the "Excavator" and "Haul truck" device profiles and in the "Mine site" asset profile. The configured calculated fields are listed below:

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

**💡 Tip:** The aggregation is done in the UTC time zone by default. You may configure settings like the time zone or interval duration in "Time series data aggregation" calculated fields.

### 📦 Solution entities

The following entities were automatically created to power this solution:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>
