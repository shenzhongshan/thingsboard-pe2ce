### 💡 Solution Description

The **SCADA Drilling System** template is a production-ready accelerator for the Oil & Gas industry. 
It bridges the gap between traditional industrial equipment and modern IoT platforms, providing a unified SCADA interface for real-time drilling operations.

#### 🌟 Why use this template?

* **🛡️ Operational Safety:** Drilling involves high-risk machinery. This solution includes pre-configured monitoring for the **Blowout Preventer (BOP)** and **Rig Health**, ensuring critical safety thresholds are never breached.
* **🔌 Seamless Integration:** Legacy equipment often speaks **Modbus**. This template comes with a pre-configured **IoT Gateway** setup to instantly connect, decode, and visualize data from industrial controllers.
* **📉 Predictive Maintenance:** Instead of waiting for failure, use real-time telemetry from **Drill Bits** and **Drawworks** to detect wear, vibration, and mechanical stress before they cause downtime.

#### ✨ Key Features

* **Unified SCADA View:** Consolidate data from 5 distinct subsystems (Drill Bit, Mud, Drawworks, Rig, BOP) into a single, real-time command center.
* **Remote Control:** Go beyond monitoring. The dashboard includes write-attributes and RPC commands to remotely control actuators, pumps, and preventer valves.
* **Digital Twin Simulation:** Includes a Dockerized **Modbus Emulator** that simulates a full drilling rig, allowing you to test logic and alarms without risking physical hardware.

#### 🌍 Real-World Applications

This template serves as a robust foundation for various sectors:

* **🛢️ Onshore & Offshore Rigs:**
  Centralize monitoring for distributed drilling sites to optimize extraction and ensure worker safety.
* **🔧 Equipment Manufacturers:**
  Offer remote diagnostics and "Rig-as-a-Service" capabilities to customers by embedding IoT connectivity.
* **🧪 Training & Simulation:**
  Use the included emulator to train operators on SCADA systems without the cost of running actual machinery.

#### 🔌 How to connect real devices?

* **IoT Gateway:** The solution is designed to use the [ThingsBoard IoT Gateway](https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/) to connect Modbus TCP/UDP or RTU devices.
* **Modbus Protocol:** Direct integration with PLCs and controllers for Drill Bits, HVAC, and pumps via [Modbus Connector](https://thingsboard.io/docs/iot-gateway/config/modbus/).
* **MQTT API:** Alternatively, integrate custom SCADA servers or proprietary gateways using the [MQTT Gateway API](https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/).
