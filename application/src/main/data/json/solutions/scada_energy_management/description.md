### 💡 Solution Description

The **SCADA Energy Management** template is a production-ready accelerator for smart grid and facility management projects. It bridges the gap between industrial power systems and modern IoT platforms, providing a unified SCADA interface for generation, storage, and consumption monitoring.

#### 🌟 Why use this template?

* **⚡ Holistic Visibility:** Fragmented systems hide inefficiencies. This solution brings **Solar panels**, **Wind turbine**, **Batteries**, and **Grid** data into a single pane of glass for total situational awareness.
* **🔋 Active Control:** Don't just watch — act. The dashboard allows you to remotely toggle generators, switch inverter modes, and manage battery storage to respond to peak demand.
* **🧠 Intelligent Metrics:** Raw data isn't enough. The system uses **Calculated Fields** to automatically compute complex metrics like **3-Phase Averages** and **System Overload** status, giving you actionable insights instantly.

#### ✨ Key Features

* **Unified SCADA View:** Consolidate data from 7 distinct subsystems (Generation, Storage, Conversion, Load) into a real-time command center.
* **Smart Alarming:** Includes 30+ pre-configured alarm rules covering everything from **Generator** fuel levels to **Wind turbine** rotor speed.
* **Power Quality Analysis:** Monitor granular data points like **Input/Output Voltage** and **Frequency** across the **Power transformer** and **Inverter** to ensure grid compliance.

#### 🌍 Real-World Applications

This template serves as a robust foundation for various sectors:

* **🏭 Industrial Plants:**
  Optimize on-site generation and manage peak loads to reduce energy costs.
* **🏢 Commercial Buildings:**
  Monitor backup power systems (Generators/Batteries) to ensure business continuity.
* **🏘️ Smart Microgrids:**
  Balance renewable inputs (Solar/Wind) with storage capacity for sustainable off-grid operation.

#### 🔌 How to connect real devices?

* **IoT Gateway:** The solution is designed to use the [ThingsBoard IoT Gateway](https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/) to connect Modbus TCP/UDP or RTU devices.
* **Modbus Protocol:** Direct integration with Inverters, Power Meters, and PLCs via [Modbus Connector](https://thingsboard.io/docs/iot-gateway/config/modbus/).
* **MQTT API:** Alternatively, integrate custom Energy Management Systems (EMS) using the [MQTT Gateway API](https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/).
