### 💡 Solution Description

The **Smart Retail** template is a production-ready accelerator for Smart Supermarket projects. It solves the complexity of managing distributed retail chains by providing a unified interface for infrastructure provisioning, floor plan visualization, and device lifecycle management.

#### 🌟 Why use this template?

* **🤝 Built for Solution Providers:** This solution supports a B2B model where **You** (the Solution Provider) manage the global device pool, while **Your Customers** (Retail Companies) manage their specific supermarkets and assets.
* **🛡️ Proactive Compliance:** Ensuring food safety and security is critical. The solution comes with pre-configured profiles that automatically raise alarms for temperature excursions or unauthorized access, requiring zero initial coding.
* **🗺️ Visual Operations:** Tables are hard to read; maps are intuitive. This solution transforms raw telemetry into **Interactive Floor Plans**, allowing facility managers to pinpoint exactly where a "Chiller" or "Smart Shelf" is located within the store.

#### ✨ Key Features

* **Multi-Customer Provisioning:** Create new Retail Companies and assign a specific pool of IoT devices to them directly from the Administration dashboard.
* **Interactive Installation:** Either you or your customer can physically install devices and logically place them on the custom floor plan using simple **drag-and-drop** actions.
* **Configurable Logic:** While alarm rules are pre-configured, Customer users retain the flexibility to tune specific thresholds (e.g., min/max temperature) for each device to match their local requirements.

#### 🌍 Real-World Applications

This template serves as a robust foundation for various sectors:

* **🛒 Supermarket Chains:**
  Deploy a standardized monitoring solution across hundreds of stores to ensure cold chain integrity and stock availability.

* **🏪 Convenience Stores:**
  Provide store owners with a simple app to monitor security sensors and energy usage in smaller retail footprints.

* **🏭 Smart Warehousing:**
  Visualize sensor placement in large distribution centers to track environmental conditions and asset location.

#### 🔌 How to connect real devices?

* **IoT Gateway:** The most popular approach is to use an [IoT gateway](https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/) per supermarket. Chillers, freezers, and HVAC may be connected using [Modbus](https://thingsboard.io/docs/iot-gateway/config/modbus/), [BACnet](https://thingsboard.io/docs/iot-gateway/config/bacnet/), and other supported protocols.
* **MQTT API:** Alternatively, use your own IoT Gateway and connect it to ThingsBoard using the [MQTT Gateway API](https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/).
* **Integrations:** You may also connect existing LoRaWAN, NB IoT, SigFox, and other sensors through [Platform Integrations](https://thingsboard.io/docs/user-guide/integrations/).

