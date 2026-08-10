## Solution instructions

Welcome to your new **Fuel Level Monitoring** solution 👋 
We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Fuel Level Monitoring</a> dashboard for you. Use it to:

* **Monitor** tank locations and real-time status on an interactive map;
* **Track** fuel levels, consumption history, and refilling events;
* **Manage** alarms for low fuel, temperature limits, or battery issues;
* **Configure** tank shapes, dimensions, and sensor parameters.

### 🖥 Mastering the dashboard

This dashboard is designed to monitor remaining fuel, view consumption statistics, manage tanks, and handle alarms.

**1. Main State**

The main state is designed to monitor the remaining fuel and control the placement of tanks, device management, and the alarm system. This page contains the following sections:

* **Interactive map** displays the location of the tanks with the help of color-coded markers. 
  * The **marker** informs about the current status of the tank sensor: 
    * 🟢 **Green** - sensor is in a normal state, and the rules for triggering alarms are not applied;  
    * 🟡 **Yellow** - sensor has a low battery level;
    * 🔴 **Red** - sensor reported low fuel level or high/low temperature data readings
    * ⚪ **Gray** - sensor is in the offline mode. 
  * More information about the tank status inside a tooltip of each marker.

* **Filter alarms** allows you to filter which tanks are displayed across the dashboard state.
By toggling the switches, you can hide or show tanks based on specific criteria, such as "No alarms", "Low battery", "Offline", or active alarm types.

* **Tanks** section is a list designed to display all existing tanks. You can delete or edit existing ones. 
  * **Data Columns:** Each row displays key metrics including "Tank" label, "Remaining %", "Temperature", "Battery", and "Connection" status.
  * **Actions:** Use the action buttons on the right side of each row to edit ✏️ or delete 🗑️ a tank.
  * **Add Sensor:** Click the "+" button in the table header to create a new tank.
    * **Creation Wizard:** The process involves three steps: "General info", "Tank info," and "Set location".
    * **Note:** The solution supports 9 pre-defined tank types, allowing for automatic volume calculation based on geometric parameters. For more details, see the **Tank Creation** section below.

* **Alarms:** section designed to display all active alarms related to remaining fuel, temperature, and battery levels. 
You can set the specific conditions for triggering alarms by clicking the "Alarm Rules" button.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/fuel_level_monitoring/fuel-monitoring-1.png" alt="Fuel level monitoring">
</div>

**2. Tank Details State**

You can access the detailed view for any tank by clicking its row in the Tanks list or by clicking the "Details" button in the map popup.
This state provides a deep dive into specific tank metrics and allows for individual management:

* **Fuel Level widget**: large visual gauge displaying the current fuel volume (L/Gal) and fill percentage.
* **Tank Information:** A detailed info card displaying "Liquid type", "Tank temperature", "Battery level", "Connection" status, and "Last update".
  * **Actions:** Use the Edit ✏️ button to modify tank details (e.g., name, serial number) or the Map 🗺️ icon to update the tank's location.
* **Consumption and remaining fuel:** a historical table tracking fuel activity.
  * It logs the Timestamp, Remaining fuel level, Refilling amounts (fuel added), and Consumption (fuel used) for specific time intervals.
* **Remaining Chart:** A line graph visualizing fuel level trends (in %) over time.
* **Alarms:** A filtered list showing only the active alarms for this specific tank.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/fuel_level_monitoring/fuel-monitoring-3.png" alt="Fuel level monitoring">
</div>

### 🛢️ Tank creation

We have included **9 pre-defined geometric tank shapes** to automatically calculate volume based on dimensions. This allows easy use of ready-made templates to calculate the volume of your tank.

<div class="img-float" style="max-width:45%;margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/tank-shapes.png" alt="Fuel level monitoring">
</div>

To create a tank, you need to go through **next steps**:

**1. General Info**

The user must fill in mandatory fields: "Serial number" and "Sensor label".

*Click "Next" to proceed.*

**2. Tank Info**

This step contains basic information about the tank's parameters, sensor, and fuel type. The entered data directly affects the final volume calculation.

* **Tank Shape:** Select one of the 9 available shapes.
* **Measurement System:** Choose "Metric" or "Imperial".
* **Dimension Units:** Select the specific units (e.g., mm, cm, ft). 
  * **💡 Note:** The required fields (length, height, etc.) automatically adjust based on the selected shape.
* **Capacity Output:** Select the unit for the final volume calculation to see the tank "Capacity".

<div class="img-float" style="max-width:35%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/2-2-1.png" alt="Fuel level monitoring">
</div>

* **Sensor Info:** Choose how the sensor measures the level:
  * **Fill height:** Direct height of the liquid from the bottom (e.g., float sensor).
  * **Remaining space:** Height of empty space from the top (e.g., ultrasonic sensor).
    * **💡 Note:** If "Remaining space" is selected, a "Sensor gap" field appears to exclude technical offsets (like a tank neck) from the calculation.
* **Liquid Info:** Select the desired type of fuel from the list.

<div class="img-float" style="max-width:35%; margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/2-2-3.png" alt="Fuel level monitoring">
</div>

**3. Set Location (optional)**

The stage at which the user can choose the location of the tank. The interactive map allows you to select a point on the map manually.

<div class="img-float" style="max-width:35%; margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/2-3.png" alt="Fuel level monitoring">
</div>

After all, stages have been completed, the user can save all changes by pressing the "Save" 
button — this tank will be placed in the general list of tanks, and the corresponding marker will be displayed on the interactive map.
For the completeness of the picture, you can test the functionality and simulate the data of the tank. To do this, go to the **Examples & Scenarios** section to find practical examples and instructions.

### ⛓️ Rule Chains

The **"Fuel Monitoring"** rule chain is processing all incoming messages from tank sensors. 
This rule chain is responsible for counting alarms of all types (temperature, battery, fuel level, fuel height) 
and updating the status of the tank sensor based on alarms count and connectivity of the device.

<div class="img-float" style="max-width:35%; margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/rule-chain.png" alt="Fuel level monitoring">
</div>

### 🚨 Alarms

Your solution monitors fuel tank conditions based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the "Tank" device profile:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

**Default Thresholds:** The solution comes pre-configured with the following safety limits:

* **Low Fuel:** Triggered if the fuel level drops to 10% or less.
* **Low Battery:** Triggered if the battery drops to 20% or less.
* **Temperature:** Triggered if the temperature rises above 80°C or drops below 0°C.

**💡 Tip:** The alarm rules use dynamic thresholds (attributes). This means you don't need to edit the Device Profile logic to change these limits! 
You can simply adjust the values using the "Alarm rules" form directly on the <a href="${MAIN_DASHBOARD_URL}" target="_blank">dashboard</a>.

### 🔌 Devices

We have pre-provisioned 4 sensors with demo data. You can find their info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard come alive? You can simulate a tank sensor right now.
The solution expects telemetry for `battery`, `fuelLevel`, `temperature`, and `fuelHeight`.

**Option 1: Send basic time series measurements**

```json
{"battery": 77, "fuelLevel": 91, "fuelHeight": 125, "temperature": 32 }{:copy-code}
```

<br>

Run the following command:

```bash
curl -v -X POST -d "{\"battery\":  77, \"fuelLevel\":  91, \"temperature\": 32 }" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**Option 2: Send "fuelHeight" measurement**

Run the following command:

```bash
curl -v -X POST -d "{\"fuelHeight\":  100}" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard—you should see the values update instantly 🚀

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

### 📚 Examples & Scenarios

#### Scenario 1: Eliminating Alarms ✅

**The Goal:** clear all active alarms for tank "001273" by simulating "healthy" data.

**Current State:** As shown below, the tank has 3 active alarms due to critical telemetry:
* **High Temperature:** 82°C (Limit: 80°C)
* **Low Battery:** 9% (Limit: 20%)
* **Low Fuel:** 5% (Limit: 10%)

<div class="img-float" style="max-width:60%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-1-2.png" alt="Fuel level monitoring">
</div>

<div class="img-float" style="max-width:60%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-1-1.png" alt="Fuel level monitoring">
</div>

<div class="img-float" style="max-width:60%;margin: 10px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/fuel-monitoring-2.png" alt="Fuel level monitoring">
</div>

**The Fix:** we will send a telemetry update with values that fall within the "safe" range defined in the alarm rules:
* **Temperature:** 25°C
* **Battery:** 100%
* **Fuel Level:** 100%

Run the following command:

```bash
curl -v -X POST -d "{\"battery\":  100, \"fuelLevel\":  100, \"temperature\": 25 }" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**Result:** the system processes the new data, recognizes the tank is healthy, and automatically clears the alarms.

<div class="img-float" style="max-width:60%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-1-3.png" alt="Fuel level monitoring">
</div>

#### 📏 Scenario 2: Volume Calculation

The dashboard calculates liquid volume based on the tank shape and the height reading. 
Let's see how this works for two different sensor types using Tank "001273" (Height: 200cm, Capacity: 1570.8L).

**Case A: Fill Height (Float Sensor) Logic:** Measures liquid from the bottom up.

To simulate a 50% fill level, we simply send half the tank's height (100 cm):

<div class="img-float" style="max-width:45%;margin: 20px auto">
<img src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-1.png" alt="Fuel level monitoring">
</div>

<br>

Run the following command:

```bash
curl -v -X POST -d "{\"fuelHeight\":  100}" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**Result:** The dashboard calculates 785L (50%).

<div class="img-float" style="max-width:65%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-2.png" alt="Fuel level monitoring">
</div>

**Case B: Remaining Space (Ultrasonic Sensor) Logic:** Measures empty space from the top down.

**⚠️ Prerequisite:** Update the tank configuration to "Remaining space" before running this command.

In this scenario, we must also account for the "Sensor Gap" (e.g., a 10cm tank neck). To simulate the same 50% fill level:

* Empty space required: 100 cm
* Add Sensor Gap: +10 cm
* Value to send: 110 cm

<div class="img-float" style="max-width:45%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-1-1.png" alt="Fuel level monitoring">
</div>

<br>

Run the following command:

```bash
curl -v -X POST -d "{\"fuelHeight\":  110}" ${BASE_URL}/api/v1/${001273ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**Result:** The system subtracts the gap (110 - 10 = 100) and correctly calculates the volume as 50%.

<div class="img-float" style="max-width:65%;margin: 20px auto">
<img style="border: 1px solid #d7d7d7;" src="https://img.thingsboard.io/solutions/fuel_level_monitoring/ex-2-2.png" alt="Fuel level monitoring">
</div>

<br>

We have demonstrated two efficient ways to calculate tank volume. We encourage you to practice with different options and simulate various scenarios to fully understand the operation of the **Fuel Level Monitoring** solution template.