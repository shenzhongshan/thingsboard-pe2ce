## Solution instructions

Welcome to your new **Swimming Pool SCADA System** solution 👋
This template provides a comprehensive monitoring and control environment tailored for swimming pool facilities, utilizing Modbus communication and IoT Gateway integration.

### 🐳 Step 1: Install Docker Compose 

Follow the instructions in the official [Docker Compose installation guide](https://docs.docker.com/compose/install/) to install Docker Compose on your system.

### 🏊 Step 2: Launch the Modbus Pool Emulator

To simulate a comprehensive swimming pool system, this Docker command launches a Modbus pool emulator containing 14 separate devices that function as a unified system and communicate via Modbus. 
Execute the following command in your terminal: 

```bash
docker run --pull always --rm -d --name tb-modbus-pool-emulator -p 5021-5034:5021-5034 thingsboard/tb-modbus-pool-emulator:1.0-stable && docker logs -f tb-modbus-pool-emulator{:copy-code}
```

### 🚀 Step 3: Launch the IoT Gateway

Create a `docker-compose.yml` file with the necessary configurations:

```bash 
${DOCKER_CONFIG}
{:copy-code}
```

Use Docker Compose to pull and run the IoT Gateway:

```bash
docker compose up{:copy-code}
```

### Interacting with the Swimming Pool SCADA System

<br>

The <a href="${MAIN_DASHBOARD_URL}" target="_blank">Swimming Pool SCADA system</a> dashboard is designed to visualize and interact with the data from multiple pool components.

**1. Real-time Monitoring**

View sensor data and their real-time states to ensure optimal operation.
* **Water Quality:** Monitor water levels using the **Water level meter** and check filtration status via the **Filter PH sensor**.
* **System Health:** Track vibration and rotation speeds for the **Heat pump** and **Sand filter** to prevent mechanical failures.

**2. Historical Analysis**

Analyze historical data to identify trends and optimize energy usage.
* **Energy Management:** Review power consumption history for the **Heat pump** to ensure efficiency.
* **Pressure Monitoring:** Track **Sand filter** pressure trends to determine when maintenance or backwashing is required.

**3. Remote Control**

Control system components remotely directly from the dashboard.
* **Actuation:** Open/Close valves like the **Main intake valve**, **Pool drain valve**, or **Throughpass valve**.
* **Operational Modes:** Switch between different filtration and heating modes.

**💡 Tip:** For further customization of the dashboard, refer to the <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

The solution automatically configures the **Pool System Gateway** and creates 14 devices representing the physical components of the pool.

* **Core Components:** "Heat pump", "Sand filter".
* **Sensors:** "Water level meter", "Filter PH sensor".
* **Valves:** "Main intake valve", "Throughpass valve", "Heat pump intake valve", "Heat pump outgoing valve", "Pool intake valve", "Pool weir valve", "Pool drain valve", "Water pump outgoing valve", "Drain valve".

#### 📡 Connectivity

This solution utilizes the **Pool System Gateway** and **Modbus protocol** to communicate with the physical devices.
For real-time monitoring of device data received from Modbus servers, you can access the <a href="${GATEWAYS_URL}" target="_blank">Gateways</a> page to view the status and data of connected devices.

### 🚨 Alarms

Alarms are configured to monitor equipment health and operational thresholds across the system.
For example, the **Heat pump** triggers a critical alarm if the compressor temperature exceeds **200°C**, while the **Sand filter** alerts if pressure drops below **3.0** or exceeds **6.0**.

Below is the complete list of <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured for this solution:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🧮 Calculated fields

The solution utilizes <a href="${DOCS_BASE_URL}/user-guide/calculated-fields/" target="_blank">calculated fields</a> to derive the real-time flow status of different pipe segments based on valve positions and pump states.

* **Flow Logic:** Scripts like "Filter Segment Flowing" or "Heat Segment Flowing" determine if water is currently moving through specific sections of the infrastructure.

Below is the complete list of calculated fields configured for this solution:

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>
