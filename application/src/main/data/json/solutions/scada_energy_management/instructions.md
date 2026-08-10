## Solution instructions

Welcome to your new **SCADA Energy Management** solution 👋
This template provides a comprehensive monitoring and control environment tailored for modern energy infrastructures, utilizing Modbus communication and IoT Gateway integration.

### 🐳 Step 1: Install Docker Compose 

Follow the instructions in the official [Docker Compose installation guide](https://docs.docker.com/compose/install/) to install Docker Compose on your system.

### ⚡ Step 2: Launch the Modbus Energy emulator

To simulate a comprehensive energy management system, this Docker command launches a Modbus energy emulator containing 7 separate devices that function as a unified system and communicate via Modbus. 
Execute the following command in your terminal:

```bash
docker run --pull always --rm -d --name tb-modbus-energy-emulator -p 5040-5046:5040-5046 thingsboard/tb-energy-emulator:1.0-stable && docker logs -f tb-modbus-energy-emulator{:copy-code}
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

### 🖥 Interacting with the SCADA Energy management

<br>

<a href="${MAIN_DASHBOARD_URL}" target="_blank">SCADA Energy management</a> dashboard is designed to visualize and interact with the data from multiple energy devices.



### 🖥 Mastering the dashboards

#### ⚡ SCADA Energy Management

The <a href="${MAIN_DASHBOARD_URL}" target="_blank">SCADA Energy management</a> dashboard is designed to visualize and interact with the data from multiple energy devices.

**1. Generation & Storage**

Monitor the real-time output and health of your power sources.

* **Renewables:** Track **Wind Speed**, **Rotor Speed**, and **Solar Power Output** to optimize green energy usage.
* **Backups:** Monitor **Generator** fuel levels and oil temperature to ensure readiness during outages.
* **Storage:** View **Batteries** levels and real-time charge/discharge currents to manage load shifting.

**2. Grid & Consumption**

Analyze power quality and usage trends.

* **Power Quality:** Monitor 3-phase average voltage and frequency across **Power transformer** and **Consumption** nodes to detect grid instability.
* **Load Analysis:** Track total current (Amps) and detect **System Overload** events before they trip breakers.

**3. Remote Control**

Control system components remotely directly from the dashboard.

* **Device State:** Toggle the **Generator**, **Wind turbine**, or **Inverter** states based on demand.
* **Mode Switching:** Switch the **Batteries** system between "Charger" and "Inverter" modes.

**💡 Tip:** For further customization of the dashboard, refer to the <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

The solution automatically configures the **Energy management gateway** and creates seven key devices representing all layers of the energy infrastructure.

* **Generation:** Solar panels, Wind turbine, Generator.
* **Storage & Conversion:** Batteries, Inverter.
* **Grid & Load:** Power transformer, Consumption.

#### 📡 Connectivity

This solution utilizes the **IoT Gateway** and **Modbus protocol** to communicate with the physical energy equipment.
For real-time monitoring of device data received from Modbus servers, you can access the <a href="${GATEWAYS_URL}" target="_blank">Gateways</a> page to view the status and data of connected devices.

### 🚨 Alarms

Alarms are configured to monitor critical safety thresholds and equipment health across all subsystems.
For example, the solution monitors **Wind turbine** rotor speed, **Generator** oil temperature, and **Solar panels** overheating to prevent catastrophic failures.

Below is the complete list of <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured for this solution:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🧮 Calculated fields

The solution utilizes <a href="${DOCS_BASE_URL}/user-guide/calculated-fields/" target="_blank">calculated fields</a> to compute derived metrics from raw Modbus telemetry.

* **Power Logic:** Calculates **System Overload**, **Net Battery Flow** (Charge vs. Discharge), and total **Output Power**.
* **3-Phase Averages:** Automatically computes average **Voltage**, **Frequency**, and **Temperature** for Inverters, Transformers, and Consumption meters.

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>
