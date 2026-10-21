# Common Transport（`common/transport`）

**本文是权威版。** 英文版 [transport.md](transport.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:transport`（父 POM，type 为 `pom`）

`common/transport` 模块是一个父 POM，包含若干传输共享子模块。每个子模块为某种特定的设备通信协议
提供共享抽象。具体的 transport 实现（在 `transport/*` 下）继承这些共享类。

## 子模块

| 子模块 | Maven artifact | 用途 |
|-----------|---------------|---------|
| `transport-api` | `org.thingsboard.common:transport-api` | 核心传输抽象（会话、凭证、上下文） |
| `coap` | `org.thingsboard.common:transport-coap` | CoAP 传输共享类 |
| `http` | `org.thingsboard.common:transport-http` | HTTP 传输共享类 |
| `mqtt` | `org.thingsboard.common:transport-mqtt` | MQTT 传输共享类 |
| `lwm2m` | `org.thingsboard.common:transport-lwm2m` | LwM2M 传输共享类 |
| `snmp` | `org.thingsboard.common:transport-snmp` | SNMP 传输共享类 |

## transport-api

提供所有协议实现共用的核心传输抽象。主要类包括：

| 类 | 用途 |
|-------|---------|
| `TransportContext` | 传输层上下文（设备配置文件、凭证、会话信息） |
| `DeviceSessionInfo` | 设备会话状态（会话 ID、最后活动时间、订阅） |
| `TransportService` | 传输层操作接口 |
| `TransportAdaptor` | 在原始协议消息与内部 TbMsg 之间转换的适配器 |
| `DeviceAuthCallback` | 设备认证结果的回调 |
| `TransportResource` | 抽象的资源端点定义 |

## transport/coap

提供建立在 Eclipse Californium 库之上的 CoAP（Constrained Application Protocol）传输抽象。

| 类 | 用途 |
|-------|---------|
| `AbstractCoapTransportResource` | 处理设备请求的 CoAP 资源基类 |
| `CoapTransportResource` | 主 CoAP 资源端点（遥测、属性、RPC） |
| `CoapTransportContext` | CoAP 专属传输上下文 |
| `CoapTransportService` | CoAP 传输服务接口 |
| `CoapTransportAdaptor` | 把 CoAP 消息转成 TbMsg 的接口 |
| `JsonCoapAdaptor` | JSON 载荷适配器 |
| `ProtoCoapAdaptor` | Protobuf 载荷适配器 |
| `TbCoapMessageObserver` | CoAP observe 回调接口 |
| `TransportConfigurationContainer` | CoAP 传输配置 |
| `OtaPackageTransportResource` | OTA 固件的 CoAP 端点 |

### CoAP 客户端

| 类 | 用途 |
|-------|---------|
| `CoapClientContext` | 管理服务端发起请求所用的 CoAP 客户端 |
| `DefaultCoapClientContext` | CoAP 客户端上下文的实现 |
| `TbCoapClientState` | 跟踪每个客户端的状态（地址、请求） |
| `TbCoapObservationState` | 跟踪 CoAP observe 关系 |

客户端支持多种安全模式：
- `NoSecClient` —— 无 DTLS（明文 CoAP）
- `NoSecObserveClient` —— 无安全的 observe
- `SecureClientNoAuth` —— DTLS 但不做客户端认证
- `SecureClientX509` —— 带 x509 证书认证的 DTLS

### CoAP 回调

| 类 | 用途 |
|-------|---------|
| `CoapDeviceAuthCallback` | 认证结果处理 |
| `CoapResponseCallback` | 通用响应处理 |
| `CoapResponseCodeCallback` | 响应码处理 |
| `GetAttributesSyncSessionCallback` | 同步属性拉取 |
| `ToServerRpcSyncSessionCallback` | 同步 RPC 调用 |
| `CoapEfentoCallback` | Efento 传感器回调 |
| `CoapNoOpCallback` | 空实现回调占位 |

### Efento 集成

`efento/` 子包为 Efento 传感器提供 CoAP 传输支持：
- `CoapEfentoTransportResource` —— Efento 专属资源端点
- `EfentoCoapAdaptor` —— Efento 消息适配器
- `CoapEfentoUtils` / `PulseCounterType` —— Efento 协议工具

## transport/http

提供 HTTP 传输抽象：

| 类 | 用途 |
|-------|---------|
| `DeviceApiController` | 设备 API 的 REST 控制器（遥测、属性、RPC） |
| `HttpTransportContext` | HTTP 专属传输上下文 |
| `PayloadSizeFilter` | 请求载荷大小过滤 |
| `TransportSecurityConfiguration` | HTTP 安全配置 |

## transport/mqtt

提供设备连接用的 MQTT 传输抽象（基于 Netty）。负责 topic 解析、会话管理和 MQTT 专属的消息转换。

## transport/lwm2m

提供 LwM2M（Lightweight M2M）传输抽象，用于通过 OMA LwM2M 协议做 IoT 设备管理：

| 类 | 用途 |
|-------|---------|
| `LwM2MTransportBootstrapService` | LwM2M 设备的 bootstrap 接口 |
| `LwM2MBootstrapConfig` | bootstrap 服务端配置 |
| `LwM2MBootstrapSecurityStore` | bootstrap 安全凭证存储 |
| `LwM2MInMemoryBootstrapConfigStore` | 内存式 bootstrap 存储 |
| `LwM2MConfigurationChecker` | bootstrap 配置校验 |
| `LwM2MSecureServerConfig` / `LwM2MTransportBootstrapConfig` | LwM2M 配置类型 |
| `TbLwM2MAuthorizer` | LwM2M 授权接口 |
| `LwM2MCredentialsSecurityInfoValidator` | 凭证校验 |
| `TbLwM2mVersion` | LwM2M 版本枚举 |

## transport/snmp

提供用于轮询支持 SNMP 的设备的传输抽象。负责 OID 映射、SNMP 数据转换和设备发现。

## 依赖关系

- **依赖：** `common/data`、`common/message`、`common/proto`、`common/queue`、`common/cache`
  （各子模块略有差异）
- **被谁依赖：** `transport/mqtt`、`transport/http`、`transport/coap`、`transport/lwm2m`、
  `transport/snmp`（具体的 transport 实现）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `transport`（父）、`transport-api`、`transport-coap`、`transport-http`、
  `transport-mqtt`、`transport-lwm2m`、`transport-snmp`
