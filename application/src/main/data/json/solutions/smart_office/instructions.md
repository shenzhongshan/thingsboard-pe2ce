## Solution instructions

Welcome to your new **Smart Office** monitoring solution 👋 We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Smart office</a> dashboard for you. Use it to:

* **Observe** office sensors and their locations;
* **Browse** indoor temperature and power consumption history;
* **Monitor** temperature alarms;
* **Control** HVAC systems remotely (requires connected device);
* **Manage** devices details.

### 🖥 Mastering the dashboard

The dashboard has multiple states. The **Main** state displays the list of devices and their map location. Click on any row to drill down to the **Device Details** state (specific to each device type).

You can always customize this dashboard using our <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

We have pre-provisioned an "Office" asset and 4 related devices with demo data. You can find their info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard come alive? You can simulate a real device right now. Solution expects specific telemetry based on the device type.
You may find payload examples and commands to send the data on behalf of the devices below. Please note that the examples below use <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>. 
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

<br>

**⚡ Energy meter**

<br>

Payload example:

```json
{"voltage":  220, "frequency":  60, "amperage": 16, "power": 3000, "energy": 300 }{:copy-code}
```

<br>

To emulate the data upload on behalf of device "Energy meter", one should execute the following command:

```bash
curl -v -X POST -d "{\"voltage\":  220, \"frequency\":  60, \"amperage\": 16, \"power\": 3000, \"energy\": 300}" ${BASE_URL}/api/v1/${Energy meterACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**💧 Water meter**

<br>

Payload example:

```json
{"water": 2.3, "voltage": 3.9 }{:copy-code}
```

<br>

To emulate the data upload on behalf of device "Water meter", one should execute the following command:

```bash
curl -v -X POST -d "{\"water\": 2.3, \"voltage\": 3.9 }" ${BASE_URL}/api/v1/${Water meterACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**🌡 Smart sensor**

<br>

Payload example:

```json
{"co2": 500, "tvoc": 0.3, "temperature": 22.5, "humidity": 50, "occupancy": true}{:copy-code}
```

<br>

To emulate the data upload on behalf of device "Smart sensor", one should execute the following command:

```bash
curl -v -X POST -d "{\"co2\": 500, \"tvoc\": 0.3, \"temperature\": 22.5, \"humidity\": 50, \"occupancy\": true}" ${BASE_URL}/api/v1/${Smart sensorACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

**❄️ HVAC**

<br>

Payload example:

```json
{"airFlow": 300, "targetTemperature": 21.5, "enabled": true}{:copy-code}
```

<br>

To emulate the data upload on behalf of device "HVAC", one should execute the following command:

```bash
curl -v -X POST -d "{\"airFlow\": 300, \"targetTemperature\": 21.5, \"enabled\": true}" ${BASE_URL}/api/v1/${HVACACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
``` 

<br>

**💡 Tip:** HVAC device also accepts commands from the dashboard to enable/disable air conditioning as well as set target temperature.
The commands are sent using the platform <a href="${DOCS_BASE_URL}/user-guide/rpc/" target="_blank">RPC API</a>.

<br>

Go check your dashboard — you should see the values update instantly 🚀

### 🚨 Alarms

Your solution monitors data based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> 
configured in the "smart-sensor" device profile:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 📦 Solution entities

The following entities were automatically created to power this solution:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

${edge_instructions}
