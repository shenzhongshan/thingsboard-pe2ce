## Solution instructions

Welcome to your new **Waste Management** solution 👋 We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Waste Management Administration</a> dashboard for you. You may use the dashboard to:

* **Monitor** the real-time fullness of trash bins across the city;
* **Visualize** bin locations and critical status on an interactive map;
* **Manage** sensor health, battery levels, and maintenance alarms;
* **Optimize** collection routes by prioritizing full bins.

### 🖥 Mastering the dashboard

This solution provides a comprehensive **Waste Management Administration** dashboard intended for monitoring the fullness of trash bins, viewing fullness statistics, and managing devices.

**1. Main State (City Overview)**

The central hub for waste operations, giving you a bird's-eye view of all deployed assets.

* **Interactive Map:** Displays the precise location of every garbage bin.
  * **Status Indicators:** Markers change color based on fullness (e.g., 🔴 **Red** indicates a bin is critically full and needs an immediate collection).
  * **Quick Info:** Click any marker to open a pop-up with key details like "Fullness %" and "Battery Level." Click "Details" to drill down into that specific bin.
* **Interactive KPI Cards:** Located at the top of the list, these cards summarize the fleet's health ("Total bins", "Fullness" > configurable threshold (default: 90%)", "Low Battery", "Offline").
  * **Global Filtering:** Click any card to instantly filter the **entire dashboard state**. This updates both the "Map" markers and the "Bins" list to show only the devices matching that category (e.g., clicking "Fullness" isolates only the critical bins for route planning).
* **Bins List:** A sortable table managing your entire inventory.
  * **Columns:** View essential data like "Serial Number", "Address", "Connection" status, "Fullness", and "Battery Level".
  * **Actions:** Add new sensors via the widget header "+" button (supports bulk CSV upload). Also, you can "Edit" ✏️ or "Delete" 🗑️ existing ones using "row" action buttons.
* **Alarms Console:**
  A dedicated section for active alarms.
  * **Triggers:** Automatically lists events like "Bin Full" (>= 90%) or "Low Battery" (< 30%).
  * **Configuration:** Click the settings icon ⚙️ "Alarm rules" to adjust global thresholds for when alarms should fire.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/waste_monitoring/waste-monitoring-1.png" alt="Waste Management">
</div>

**2. Bin State (Drill Down)**

To access this detailed view, click on a "Bin Marker" on the map, then "Details" or click on a "Row" in the "Bins" list. This view focuses on the health and history of a single collection point.

* **Sensor Details:** Displays static data (Serial Number, Address) and real-time status (Connection, Last Update). Use the "Edit" button to update location or tags.
* **Location Management:** An embedded map allowing you to manually drag-and-drop the bin marker to correct its physical location.
* **Real-Time Telemetry:**
  * **Fullness Chart:** History chart showing how quickly the bin fills up over time.
  * **Battery Chart:** Monitors the sensor's power level to prevent device outages.
* **Alarms:** A filtered log showing only the alarms relevant to this specific bin.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/waste_monitoring/waste-monitoring-2.png" alt="Waste Management">
</div>

### 🔌 Devices

We have pre-provisioned 10 Waste Sensors and loaded them with demo data to help you explore the solution immediately.

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard come alive? You can simulate a full bin right now. The solution expects that the sensor device will upload `fullLevel` and `batteryLevel`. 

The most simple example of the expected payload is in JSON format:

```json
{"batteryLevel": 77, "fullLevel": 91 }{:copy-code}
```

<br>

To emulate the data upload on behalf of waste sensor: "389021001264", one should execute the following command:

```bash
curl -v -X POST -d "{\"batteryLevel\":  77, \"fullLevel\":  91 }" ${BASE_URL}/api/v1/${389021001264ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard — you should see the values update instantly 🚀

### 🚨 Alarms

Alarms are generated based on <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the "Waste Sensor" device profile. We have pre-configured two core rules:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

User may configure the alarm rules via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Waste Management Administration</a> dashboard using "Alarm rules" form.

### ⛓️ Rule Chains

The **Waste Sensor Rule Chain** is processing all incoming messages from waste sensors. This rule chain is responsible for counting alarms of both types and updating the status of the garbage bin by fullness and battery levels.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/waste_monitoring/rule-chain.png" alt="Waste Management">
</div>

### 📦 Solution entities

The following entities were automatically created to power this solution:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

### 📚 Examples & Scenarios

#### Scenario 1: Managing a Critical Overflow Event 🗑️

**Goal:** Simulate a situation where a bin reaches 100% capacity and triggers an urgent collection request.

**1. Identify the bin:** Let's select waste sensor: "389021001241". Currently, it is at 17% capacity (Normal state).

<br>

<div class="img-float" style="width:40%;margin: 20px auto">
<img src="https://img.thingsboard.io/solutions/waste_monitoring/example-1-1.png" alt="Waste Management">
</div>

<br>

**2. Verify alarm rules:** Check that the alarm rule is active. By default, the system triggers an alarm if fullness is ≥ 90%.

<br>

<div class="img-float" style="max-width:20%;margin: 20px auto">
<img src="https://img.thingsboard.io/solutions/waste_monitoring/example-1-2.png" alt="Waste Management">
</div>

<br>

**3. The Action:** Simulate a "Full Bin" event by sending a value of 100%:

```bash
curl -v -X POST -d "{\"fullLevel\": 100 }" ${BASE_URL}/api/v1/${389021001241ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**4. Result:** The dashboard instantly updates. The bin's marker turns 🔴 **Red**, and a "Fullness Level" alarm appears in the Alarms Console, notifying the fleet manager to dispatch a truck.

<div class="img-float" style="max-width:50%;margin: 20px auto">
<img src="https://img.thingsboard.io/solutions/waste_monitoring/example-1-3.png" alt="Waste Management">
</div>
