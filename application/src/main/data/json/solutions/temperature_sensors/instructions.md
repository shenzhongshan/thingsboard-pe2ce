## Solution instructions

Welcome to your new **Temperature & Humidity** monitoring solution 👋 We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Temperature & Humidity</a> dashboard that displays data from multiple sensors. You may use the dashboard to:

* **Add** and locate sensors on the map;
* **Configure** alarm thresholds;
* **Browse** historical data;
* **Manage** sensor settings.

### 🖥 Mastering the dashboard

The dashboard has two states. The **Main** state displays the list of sensors and their map location. 
Click on any row to drill down to the **Sensor Details** state to see history and change settings.
You can always customize this dashboard using our <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

We have pre-provisioned two demo sensors for you. You can find their info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard come alive? You can simulate a real device right now.
The solution expects the device to upload `temperature` and `humidity` values in JSON format:


```json
{"temperature": 42, "humidity": 73}{:copy-code}
```

<br>

To emulate the data upload on behalf of device "Sensor T1", one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\": 42, \"humidity\": 73}" ${BASE_URL}/api/v1/${Sensor T1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard — you should see the values update instantly 🚀

### 🚨 Alarms 

Your solution monitors data based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured in the "Temperature Sensor" device profile:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

**💡 Tip:** You can enable/disable alarms and configure their thresholds anytime directly from the <a href="${MAIN_DASHBOARD_URL}" target="_blank">dashboard</a> using the "Edit Sensor" button.

### 🔐 Managing Users & Access

We created a sample customer, "Customer D", to demonstrate how you can isolate data for different clients. "Sensor C1" has been explicitly assigned to this customer.

The following users belong to "Customer D" and have read-only access to the <a href="${MAIN_DASHBOARD_URL}" target="_blank">Temperature & Humidity</a> dashboard. When they log in, they will only see "Sensor C1".

<div class="tb-markdown-view table-wrapper">

${user_list}

</div>

**💡 Note:** You can create more Customers and Users via the administration UI. You can also change the owner of other sensors (like Sensor T1) to "Customer D" to make them visible to these users.

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

${edge_instructions}
