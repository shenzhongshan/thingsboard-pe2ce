### 💡 Solution Description

The **Swimming Pool SCADA System** template is a production-ready accelerator for swimming pool facility management. It bridges the gap between mechanical pool equipment and modern IoT platforms, providing a unified SCADA interface for water quality, temperature control, and energy management.

#### 🌟 Why use this template?

* **💧 Water Quality Management:** Automate the monitoring of the **Water level meter** and **Filter PH sensor** to ensure sanitary conditions without constant manual checking.
* **🌡️ Smart Temperature Control:** Integrate **Heat pump** telemetry to intelligently monitor compressor temperature and refrigerant pressure, ensuring consistent water temperature while optimizing energy usage.
* **⚙️ Equipment Health:** Prevent downtime by tracking the performance of the **Sand filter** and pumps through real-time monitoring of vibration, rotation speed, and pressure.

#### ✨ Key Features

* **Unified SCADA View:** Consolidate data from 14 distinct subsystems (Valves, Pumps, Sensors, Heaters) into a single, real-time command center.
* **Complex Flow Logic:** Utilizes **Calculated Fields** to dynamically determine the flow status of specific pipe segments (Drain, Filter, Heat) based on the state of valves like the **Main intake valve** and **Pool weir valve**.
* **Remote Management:** Enable operators to remotely alter system states, such as switching operational modes or opening/closing specific valves.

#### 🌍 Real-World Applications

This template serves as a robust foundation for various sectors:

* **🏊 Public Swimming Pools:**
  Centralize monitoring for municipal pools to reduce maintenance costs and ensure public safety.
* **🏨 Hotels & Spas:**
  Maintain premium guest experiences by ensuring perfect water temperature and clarity automatically.
* **🏞️ Water Parks:**
  Manage complex networks of pumps and filtration systems across multiple attractions from a single dashboard.

#### 🔌 How to connect real devices?

* **IoT Gateway:** The solution is designed to use the [ThingsBoard IoT Gateway](https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/) to connect Modbus TCP/UDP or RTU devices.
* **Modbus Protocol:** Direct integration with PLCs controlling valves, pumps, and heaters via [Modbus Connector](https://thingsboard.io/docs/iot-gateway/config/modbus/).
* **MQTT API:** Alternatively, integrate custom controllers using the [MQTT Gateway API](https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/).
