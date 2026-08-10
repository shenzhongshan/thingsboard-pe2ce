## Solution instructions

Welcome to your new **Assisted Living** solution 👋
We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Assisted Living Administration</a> dashboard for you.
This dashboard is designed for monitoring the health and safety of residents using BLE/LoRa wearables and environmental sensors. Use this solution to:

* **Monitor** residents' vital signs (Heart Rate, Temperature) in real-time;
* **Track** resident locations via BLE beacons and gateways;
* **Detect** critical events like falls (Panic Button), smoke, or open doors;
* **Manage** facility layout, zones, and device assignments.

The solution is built on **BLE or LoRa gateways and devices**. Key technical features include:

* **Room Sensors:** Rooms may be equipped with sensors like room temperature, humidity, indoor air quality (IAQ), leak, smoke, and open/close detectors.
* **Geopositioning:** Resident tracking is done via the beacon in the wristband and a set of nearby gateways.
* **Data Processing:** The platform deduplicates incoming messages from beacons and enriches them with the attributes of nearby gateways.
* **Algorithm:** The location logic is based on the payload's **RSSI parameter**. One may improve the algorithm based on the particular use case.

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/al-scheme.png" alt="Assisted Living">
</div>

### 🖥 Mastering the dashboard

**1. Main State (Overview)**

The default view provides a real-time operational picture of the facility.

* **Interactive Scheme:** View zones and resident markers in real-time.
  * **Floors:** Use the tabs on the top left (**Floor 1 / Floor 2**) to switch between different floor plans.
  * **Resident Markers:** Click any resident marker to view a profile card with real-time vitals (**Heart Rate**, **Temperature**, **Panic Button** status) and their last known location.
  * **Room Markers:** Click on room sensors (e.g., thermometer icons) to see environmental stats like **IAQ**, **Temperature**, and **Humidity**.

* **Resident Alarms:**
  * **Overview:** A detailed list of health/behavior alarms. Tracks "Type", "Name", "Location", and "Severity".
  * **Actions:** You can take actions like **"Call Ambulance"** 🚑 or **"Call Nurse"** 👩‍⚕️ directly from this list.
  * **Locate:** Click on the **Location** name (e.g., "Room 102") in the alarm list to instantly view that specific room on the map popup.
  * **Notification Rules:** Click the **Settings ⚙️** icon to configure alarm thresholds. You can set rules for **Panic Button** presses, **Heart Rate** limits (BPM), **Body Temperature**, and **Noise Levels**.

* **Room Alarms:**
  * **Overview:** Displays infrastructure alerts (Smoke, Leak, Door Open). You can acknowledge these or **"Call Attendant"**.
  * **Locate:** Click on the **Location** name (e.g., "Room 102") in the alarm list to instantly view that specific room on the map popup.
  * **Notification Rules:** Click the **Settings ⚙️** icon to set thresholds for **Temperature**, **Humidity**, **Air Quality**, **Door/Window** open duration, **Battery Levels**, **Water Leaks**, and **Smoke** detection.

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/1-main-state.png" alt="Assisted Living">
</div>

<br>

**2. Residents State**

Access this state by clicking the **"Residents"** button on the Main State.

* **Resident List:** The main view provides a comprehensive roster of all residents.
  * **Columns:** Quickly view the resident's **Name & Avatar**, **Gender**, **Age**, and their assigned location (**Zone** and **Room**).
  * **Actions:** Use the "+" add resident button to onboard new users, or use the row icons to ✏️ "Edit" or 🗑️ "Delete" existing profiles.

* **Resident Profile (Data Blocks):** When creating or editing a resident, information is structured into five key sections:
  * **Personal Info:** Basic details including Name, Gender, Birth Date, Phone Number, and Photo.
  * **Emergency Contact:** Contact details for next of kin (First name, Last name, Role/Relationship, Phone number).
  * **Health Information:** Critical medical data such as **Chronic diseases** and **Allergies**.
  * **Location:** Logic assignment to a specific **Zone** (e.g., Floor 1) and **Room**.
  * **Wristband:** Binds a specific wearable device (by Serial Number) to the resident.

<div class="img-float" style="max-width:50%;margin: 20px auto">
<img src="https://img.thingsboard.io/solutions/assisted_living/2-residents-state.png" alt="Assisted Living">
</div>

<br>

**3. Zones State**

Access this state by clicking the **"Zones"** button on the Main State.

* **Purpose:** Manage the high-level facility hierarchy (floors), which serves as the foundation for rooms and device placement.
* **Actions:**
  * **Add Zone:** Click **"Add zone"** to create a new area. You will need to specify a name (e.g., "Floor 1") and upload a **mapping scheme** (image/floor plan).
  * **Drill Down:** Click on any zone row to navigate to the detailed **Zone State** for that specific area.

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/3-zones-state.png" alt="Assisted Living">
</div>

<br>

**4. Zone Details State**

This state allows you to map the physical environment on the floor plan you uploaded.

* **Create Rooms:** Define specific rooms and place them on the zone map.
* **Assign Devices:** Create devices of the appropriate type and attach them to specific rooms to establish the connection.

<br>

<div class="img-float" style="max-width:50%;margin: 20px auto">
<img src="https://img.thingsboard.io/solutions/assisted_living/4-zone-state.png" alt="Assisted Living">
</div>

<br>

* **⚠️ Important:** You can only select devices that currently exist in the **"Device Groups" -> "Unassigned Devices"** list. To add a new device to the dashboard, you must first create it on the **"Customers hierarchy"** page.

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/5-customer-hierarchy.png" alt="Assisted Living" style="border: 1px solid #eee;">
</div>

### 🚨 Alarms

The solution uses pre-configured <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> assigned to device profiles.
Administrators can configure specific thresholds for **Major** and **Critical** severity levels directly from the dashboard settings (as described in the **Mastering the Dashboard** section).

#### ⌚ Resident Alarms (Wristband)

These alarms monitor the health and safety of the resident.

* **Panic Button:** Triggers a **Major** alarm on a single press and a **Critical** alarm if pressed 2 or more times.
* **Heart Rate:** Triggers if the `pulse` value falls outside the safe range (lower/upper limits).
* **Body Temperature:** Triggers if the `temperature` value falls outside the safe range.
* **High Noise:** Triggers if the `noise` level exceeds the configured decibel limit, indicating a potential fall or distress.
* **Low Battery:** Triggers if the wristband `battery` level drops below the configured percentage.

#### 🏢 Infrastructure Alarms (Room Sensors)

These alarms monitor the safety and environmental conditions of the facility.

* **Security (Door/Window):** Triggers if a `doorOpen` or `windowOpen` event lasts longer than the allowed duration.
* **Critical Hazards:** Triggers immediately if **Smoke** (`smoke` = TRUE) or a **Water Leak** (`waterLeak` = TRUE) is detected.
* **Environment:** Triggers if **Air Quality** (`roomIaq`), **Temperature** (`roomTemperature`), or **Humidity** (`roomHumidity`) breach the defined comfort ranges.
* **Sensor Battery:** Triggers if any room sensor's `battery` level drops below the configured limit.

<br>

Below is the complete list of alarm rules configured for this solution:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🔌 Devices

The solution includes wearable devices and room sensors connected via gateways.

* **Resident wristbands:** Track health vitals (Heart Rate, Temp) and location via BLE beacons.
* **Room sensors:** Monitor environmental conditions including Temperature, Humidity, IAQ, Smoke, Water Leaks, and Door/Window status.
* **Gateways:** BLE or LoRa gateways that collect data from nearby sensors and wristbands to forward to the platform.

Each device sends telemetry that is processed by rule chains and evaluated against alarm rules.
We have already created devices and loaded some demo data for them. See device info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

### ⛓️ Rule Chains

* **AL Gateway Rule Chain:** This chain handles the core processing of data arriving from gateways: deduplication, device identification, and location enrichment.
  * **Enrichment:** The "Fetch Room attributes" node adds the gateway's location to the incoming message.
  * **Device Lookup:** The "Change Owner from Gateway to Device" node identifies the specific device (wristband or sensor) using the serial number in the payload.
  * **Routing:** The "Switch by Device Type" node directs the message to either the "Room" or "Wristband" processing chain.
  * **Location Logic:** The chain uses "Deduplicate From Multiple Gateways" to combine duplicate messages and "Use msg with Max RSSI" to pinpoint the resident's location based on the strongest signal (closest gateway).

* **AL Wristband Device Rule Chain:**
  This chain functions similarly to the default platform rule chain (saving telemetry). Uniquely, it also counts the number of active alarms and propagates this value to the corresponding resident user entity for dashboard display.

* **AL Room Device Rule Chain:**
  Very similar to the Wristband chain but dedicated to infrastructure sensors. It processes data and alarms but does not propagate alarm counts to specific user entities.

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

### 📚 Examples & Scenarios

#### 💓 Scenario 1: Triggering a Heart Rate Alarm

**Goal:** Simulate a critical health event where a resident's heart rate drops below the safety threshold.

**1. Context:** We will use resident "William Harris" (Current vital heart rate: 95 BPM).

<br>

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-1-1.png" alt="Assisted Living">
</div>

<br>

**2. Check Rules:** By clicking the **Settings (⚙️)** icon in the Resident Alarms section, we see that a pulse lower than **60 BPM** triggers an alarm.

<br>

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-1-2.png" alt="Assisted Living">
</div>

<br>

**3. Action:** Run the following command to send a telemetry update with a pulse of **55 BPM**:

```bash
curl -v -X POST -d "{\"serial\": \"C00000025FE2\", \"data\":{\"pulse\":55}}" ${BASE_URL}/api/v1/${D00000020002ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**Result:** The resident marker for William Harris turns Red on the map, and a new "Heart Rate" alarm appears in the active alarms list.

<br>

<div class="img-float" style="max-width: 55%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-1-3.png" alt="Assisted Living">
</div>

#### 🚶 Scenario 2: Tracking Resident Movement

**Goal:** Demonstrate how the system uses signal strength (RSSI) to locate a resident moving between rooms.

**1. Context:** Resident "Isabella Davis" is currently located on Floor 1.

<br>

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-2-1.png" alt="Assisted Living">
</div>

<br>

**2. Logic:** The system receives signals from multiple gateways. The gateway with the highest (least negative) RSSI value determines the location.

**3. Action:** We will simulate her wristband sending signals to two gateways simultaneously:
  * **Room 103 Gateway:** RSSI -10 (Strong Signal)
  * **Room 104 Gateway:** RSSI -70 (Weak Signal)

```bash
curl -v -X POST -d "{\"serial\": \"C00000066F66\", \"rssi\": -10, \"data\":{\"batteryLevel\":55}}" ${BASE_URL}/api/v1/${D00000030003ACCESS_TOKEN}/telemetry --header "Content-Type:application/json" && curl -v -X POST -d "{\"serial\": \"C00000066F66\", \"rssi\": -70, \"data\":{\"batteryLevel\":55}}" ${BASE_URL}/api/v1/${D00000040004ACCESS_TOKEN}/telemetry --header "Content-Type:application/json" {:copy-code}
```

<br>

**Result:** The system compares the signals (-10 vs -70) and updates Isabella's location to Room 103 on the interactive map.

<br>

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-2-2.png" alt="Assisted Living">
</div>

##### 🌫️ Scenario 3: Room Air Quality Alert

**Goal:** Trigger an infrastructure alarm due to poor indoor air quality (IAQ).

**1. Context:** We are monitoring Room 101, which has a normal IAQ level of 67.

<br>

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-3-2.png" alt="Assisted Living">
</div>

<br>

**2. Check Rules:** In the Room Alarms settings, the threshold for a "High IAQ" alarm is set to > 150.

<br>

<div class="img-float" style="max-width: 40%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-3-1.png" alt="Assisted Living">
</div>

<br>

**3. Action:** Run the command below to simulate a sensor reading of 160 IAQ:

```bash
curl -v -X POST -d "{\"serial\": \"E00000015FE1\", \"rssi\": -50, \"data\":{\"IAQ\":160}}" ${BASE_URL}/api/v1/${D00000010001ACCESS_TOKEN}/telemetry --header "Content-Type:application/json" {:copy-code}
```

<br>

**Result:** Because 160 exceeds the limit of 150, a new "Air Quality" alarm is generated for Room 101, and the room's status indicator changes to warn staff.

<br>

<div class="img-float" style="max-width: 50%;margin: 20px auto;">
<img src="https://img.thingsboard.io/solutions/assisted_living/example-3-3.png" alt="Assisted Living">
</div>

${edge_instructions}
