## Solution instructions

Welcome to your new **SCADA Drilling System** solution 👋
This template provides a comprehensive monitoring and control environment tailored for drilling operations, utilizing Modbus communication and IoT Gateway integration.

### 🐳 Step 1: Install Docker Compose 

Follow the instructions in the official [Docker Compose installation guide](https://docs.docker.com/compose/install/) to install Docker Compose on your system.

### 🛢️ Step 2: Launch the Modbus Drilling Emulator

To simulate a comprehensive drilling system, this Docker command launches a Modbus drilling emulator containing 5 separate devices that function as a unified system and communicate via Modbus. 
Execute the following command in your terminal:

```bash
docker run --pull always --rm -d --name tb-drilling-emulator -p 5035-5039:5035-5039 thingsboard/tb-drilling-emulator:1.0-stable && docker logs -f tb-drilling-emulator{:copy-code}
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

### 🖥 Interacting with the SCADA Drilling System

<br>

<a href="${MAIN_DASHBOARD_URL}" target="_blank">SCADA Oil & Gas Drilling system</a> dashboard is designed to visualize and interact with the data from multiple drilling devices. 
It serves as a central hub for operators to monitor rig health and control system components remotely.

**1. Real-time Monitoring**

Monitor drilling performance in real time.

* **Performance Metrics:** Track rotary speed, drilling speed, and well depth.
* **Rig Health:** Track key parameters such as drawwork tension, motor power consumption, and rig vibrations.

**2. Historical Analysis**

Analyze historical data to identify trends and optimize operations.

* **Mud System:** detailed history of mud flow rates, pressure levels, and density.
* **Mechanical Loads:** Review historical mechanical load data to prevent equipment fatigue.

**3. Remote Control**

Control system components remotely directly from the dashboard.

* **Actuators:** Activate pumps and adjust the preventer.
* **Drilling Modes:** Switch between different drilling modes based on operational requirements.

**💡 Tip:** For further customization of the dashboard, refer to the <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

The solution automatically configures the IoT gateway and creates five essential drilling devices.

* **Drill Bit:** Monitors **vibration** and **temperature** levels downhole.
* **Drawwork:** Monitors **lifting speed** (in both Slow and Normal modes), **inclination**, **vibration**, and **tension**.
* **Drilling Mud:** Tracks fluid properties including **density**, **pressure**, **flow rate**, and **temperature**.
* **Drilling Rig:** Monitors general mechanical performance such as **rotary speed**, **hoist speed**, **hook load**, and **pressure**.
* **Preventer (BOP):** Monitors **well pressure**, **flow rate**, and **vibration** to ensure fail-safe operation.

Below is the complete list of devices configured for this solution:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### 📡 Connectivity

This solution utilizes the IoT Gateway and Modbus protocol to communicate with physical drilling equipment. 
For real-time monitoring of device data received from Modbus servers, you can access the <a href="${GATEWAYS_URL}" target="_blank">Gateways</a> page to view the status and data of connected drilling devices.

### 🚨 Alarms

Alarms are configured to monitor critical safety thresholds and operational efficiency. 
For example, the solution monitors the Blowout Preventer (BOP) specifically for **Well Pressure** and **Flow Rate** anomalies to ensure fail-safe operation and reduce the risk of blowouts.

Below is the complete list of <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured for this solution:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>
