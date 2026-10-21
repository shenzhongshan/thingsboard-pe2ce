# 传输层

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

传输层为 ThingsBoard IoT 平台提供设备连接端点，让设备能通过多种 IoT 协议接入并交换数据。每个
transport 都被打包成独立的 Maven 模块、各有自己的 Spring Boot 应用，从而让部署能把传输服务与核心
应用分开扩容。

## 架构总览

传输层采用分层架构：

```
Device/Client
    |
    v
Transport Protocol Handler (MQTT/HTTP/CoAP/LwM2M/SNMP)
    |
    v
TransportAdaptor (JSON or Protobuf parsing)
    |
    v
TransportService (session management, authentication, routing)
    |
    v
Queue (Kafka / pub-sub)
    |
    v
Rule Engine / Core Application
```

关键设计原则：

- **协议专属处理器**把原始字节解码成 ThingsBoard 的消息格式。
- **TransportAdaptor** 把协议载荷转成内部的 `SessionContext` 以及遥测/属性/RPC 消息。每个
  transport 都同时支持 JSON 与 Protobuf 序列化。
- **TransportService**（定义在 `common/transport/transport-api`）负责设备认证、会话注册，以及经
  队列把消息转发给规则引擎。
- `common/transport/` 下的**共享抽象**让每个 transport 模块复用认证、SSL、限流和会话管理逻辑。

## 消息流

### 上行（设备到 ThingsBoard）

1. 设备经其传输协议发送数据（如 MQTT PUBLISH）。
2. 协议处理器（`MqttTransportHandler`、`DeviceApiController` 等）接收原始消息。
3. `TransportAdaptor`（JSON 或 Protobuf）把原始载荷转成 protobuf 的 `PostTelemetryMsg`、
   `PostAttributeMsg` 或 `ToServerRpcRequestMsg`。
4. `TransportService.process()` 把 protobuf 消息发往队列。
5. 核心应用消费队列消息，并把它路由穿过相应的规则链。

### 下行（ThingsBoard 到设备）

1. 规则引擎或 RPC 调用产生 `ToDeviceRpcRequestMsg` 或属性更新。
2. 队列把消息投递给传输服务。
3. `SessionMsgListener` 收到消息后，由传输协议处理器投递给设备（如 MQTT PUBLISH 到已订阅的 topic、
   CoAP 响应、HTTP 长轮询）。

## 共享传输抽象

`common/transport/transport-api` 模块提供所有 transport 共用的接口与基础实现：

| 文件 | 用途 |
|------|---------|
| `TransportService.java` | 核心接口：会话管理、遥测/属性上报、RPC 处理、设备认证、队列通信 |
| `DefaultTransportService.java` | 默认实现，含队列集成、会话注册表、限流、活动上报 |
| `TransportAdaptor.java` | 把协议专属载荷转成内部消息的通用接口 |
| `SessionMsgListener.java` | 下行消息（RPC 请求、属性更新等）的回调接口 |
| `TransportContext.java` | 传输层配置与共享状态 |
| `auth/DeviceAuthService.java` | 校验设备凭证的接口 |
| `auth/TransportDeviceInfo.java` | 认证期间提取出的设备身份信息 |
| `auth/ValidateDeviceCredentialsResponse.java` | 认证结果，含设备/租户 ID |
| `auth/SessionInfoCreator.java` | 从认证结果构造 protobuf `SessionInfoProto` |
| `session/DeviceAwareSessionContext.java` | 抽象基础会话，持有设备 ID、租户 ID、设备配置文件和连接状态 |
| `session/SessionContext.java` | 会话生命周期接口 |
| `service/DefaultTransportDeviceProfileCache.java` | 供传输节点使用的设备配置文件缓存查询 |
| `service/DefaultTransportResourceCache.java` | 资源缓存查询 |
| `service/ToRuleEngineMsgEncoder.java` | 编码发往规则引擎的消息 |
| `service/ToTransportMsgResponseDecoder.java` | 解码核心回传给 transport 的响应 |
| `activity/ActivityManager.java` | 跟踪设备连接活动 |
| `activity/strategy/` | 活动上报策略（全部事件、首个、末个、首+末） |
| `limits/TransportRateLimitService.java` | 按设备与按租户的消息限流 |
| `config/ssl/` | SSL 凭证管理（PEM 与 Keystore 两种格式） |

## 设备认证

每个 transport 用下列受支持的凭证类型之一对设备认证：

| 凭证类型 | 认证方式 | 支持的 transport |
|----------------|---------------------|---------------------|
| 访问令牌 | 令牌串放在载荷/URL 里 | MQTT、HTTP、CoAP |
| Basic（用户名/密码） | 经 MQTT CONNECT / HTTP Basic Auth 传用户名+密码 | MQTT、HTTP |
| X.509 证书 | TLS 客户端证书 | MQTT、HTTP、CoAP、LwM2M |
| LwM2M 凭证 | LwM2M 专属的 PSK、RPK 或 X.509 | LwM2M |

`DeviceAuthService` 接口通过 transport API 队列与核心应用通信来校验凭证。已认证的会话记录在
`TransportService` 的会话注册表里。

## 网关设备支持

ThingsBoard 支持网关设备，它用一条连接为多个受限设备代理遥测。网关支持已内建在 MQTT transport 里，
其他 transport 也可配置：

- 网关设备像普通设备一样连接并认证。
- 当网关代表某个子设备发布数据时，transport 以 `GetOrCreateDeviceFromGatewayRequestMsg` 调用
  `TransportService.process()`。
- 传输核心按名字自动创建或查出子设备，把它分配给该网关，并用子设备的配置文件做后续处理。
- 网关专属会话类：`GatewayDeviceSessionContext`、`GatewaySessionHandler`、
  `AbstractGatewaySessionHandler`。

## Transport 模块

每个 transport 由两个模块构成：
- **顶层模块**（`transport/<name>/`）：很薄的 Spring Boot 应用入口。
- **公共模块**（`common/transport/<name>/`）：与核心共享的协议专属逻辑。

### MQTT Transport

**协议：** MQTT 3.1.1 与 MQTT 5.0  
**默认端口：** 1883（明文）、8883（TLS）  
**基于 Netty：** 使用 Netty 的 `io.netty.handler.codec.mqtt` 编解码  
**模块入口：** `transport/mqtt/` -> `ThingsboardMqttTransportApplication.java`

| 文件 | 用途 |
|------|---------|
| `MqttTransportContext.java` | 传输层配置（SSL、限流、adaptor） |
| `MqttTransportServerInitializer.java` | Netty channel pipeline 搭建，含 MQTT 编解码与 SSL |
| `MqttTransportHandler.java` | 主 Netty handler：处理 CONNECT、PUBLISH、SUBSCRIBE、DISCONNECT |
| `MqttTransportService.java` | 注册/注销会话，管理网关订阅 |
| `MqttSslHandlerProvider.java` | 为 MQTT TLS 连接创建 SSL 上下文 |
| `adaptors/JsonMqttAdaptor.java` | 遥测/属性/RPC 的 JSON 载荷转换 |
| `adaptors/ProtoMqttAdaptor.java` | Protobuf 载荷转换 |
| `adaptors/MqttTransportAdaptor.java` | MQTT 的 adaptor 接口 |
| `adaptors/BackwardCompatibilityAdaptor.java` | 支持遗留的 v1 JSON 载荷格式 |
| `session/MqttDeviceAwareSessionContext.java` | 每设备的 MQTT 会话状态 |
| `session/DeviceSessionCtx.java` | 带 Netty channel 引用的设备会话上下文 |
| `session/GatewayDeviceSessionContext.java` | 挂在网关下的设备会话 |
| `session/GatewaySessionHandler.java` | 处理网关上下线与子设备生命周期 |
| `session/SparkplugDeviceSessionContext.java` | 感知 Sparkplug B 的设备会话 |
| `session/SparkplugNodeSessionHandler.java` | Sparkplug B 边缘节点会话 |
| `gateway/GatewayMetricsService.java` | 跟踪网关会话数与指标 |
| `limits/IpFilter.java` / `SessionLimits.java` | 基于连接/IP 的限流 |
| `util/MqttTopicFilter.java` | 订阅的 topic 过滤（精确、正则、全部） |
| `util/sparkplug/SparkplugTopicService.java` | Sparkplug B topic 解析（v2.2 与 v3.0） |
| `util/sparkplug/SparkplugMetricUtil.java` | Sparkplug B 指标转换工具 |

**会话管理：** 会话按 client ID 记录在并发 map 中。CONNECT 时 transport 经 `TransportService`
认证、创建 `DeviceSessionCtx` 并注册订阅。DISCONNECT 或 channel 关闭时注销会话。

**网关模式：** 网关设备订阅 `v1/gateway/connect` 与 `v1/gateway/disconnect` topic，并把设备数据
发布到 `v1/gateway/telemetry`、`v1/gateway/attributes` 等。`GatewaySessionHandler` 管理子设备会话
生命周期。

**Sparkplug B 支持：** 原生解析 Sparkplug B v2.2/v3.0 的 topic 结构。`SparkplugNodeSessionHandler`
管理 NBIRTH/DBIRTH/NDEATH 消息。指标值从 Sparkplug 数据类型转成 ThingsBoard 遥测。

### HTTP Transport

**协议：** HTTP/HTTPS（REST API）  
**默认端口：** 8081（明文）、8443（TLS）  
**基于 Spring MVC：** 使用 Spring Boot 内嵌 Tomcat  
**模块入口：** `transport/http/` -> `ThingsboardHttpTransportApplication.java`

| 文件 | 用途 |
|------|---------|
| `DeviceApiController.java` | REST 控制器：`/api/v1/{accessToken}/telemetry`、`/attributes`、`/rpc` 等 |
| `HttpTransportContext.java` | HTTP transport 配置与共享状态 |
| `config/TransportSecurityConfiguration.java` | 设备 API 端点的 Spring Security 配置 |
| `config/PayloadSizeFilter.java` | 请求载荷大小限制过滤器 |

**端点：**
- `POST /api/v1/{accessToken}/telemetry` —— 上报遥测数据（JSON 或 Protobuf）
- `POST /api/v1/{accessToken}/attributes` —— 上报属性更新
- `GET /api/v1/{accessToken}/attributes` —— 取 client/server/shared 属性
- `POST /api/v1/{accessToken}/rpc` —— 发送 RPC 响应
- `GET /api/v1/{accessToken}/rpc` —— 长轮询 RPC 请求（超时可选）
- `POST /api/v1/{accessToken}/claim` —— 认领设备
- `POST /api/v1/provision` —— 设备开通
- `GET /api/v1/{accessToken}/firmware` —— OTA 固件下载

**会话管理：** HTTP 是无状态的；会话按请求创建，或为 RPC 做长轮询。RPC 长轮询用
`TransportService.registerSyncSession()`，超时可配。

### CoAP Transport

**协议：** 基于 UDP 的 CoAP（Constrained Application Protocol），RFC 7252  
**默认端口：** 5683（明文）、5684（DTLS）  
**库：** Eclipse Californium  
**模块入口：** `transport/coap/` -> `ThingsboardCoapTransportApplication.java`

| 文件 | 用途 |
|------|---------|
| `CoapTransportResource.java` | 处理遥测、属性、RPC 的主 CoAP 资源 |
| `AbstractCoapTransportResource.java` | CoAP 资源基类，含共享的请求处理 |
| `CoapTransportService.java` | 带 CoAP 专属会话管理的传输服务 |
| `CoapTransportContext.java` | CoAP transport 配置 |
| `OtaPackageTransportResource.java` | 投递 OTA 固件的 CoAP 资源 |
| `adaptors/JsonCoapAdaptor.java` | CoAP 的 JSON 载荷转换 |
| `adaptors/ProtoCoapAdaptor.java` | Protobuf 载荷转换 |
| `adaptors/CoapTransportAdaptor.java` | 共享 adaptor 接口 |
| `callback/CoapDeviceAuthCallback.java` | 异步认证回调 |
| `callback/CoapResponseCallback.java` | 异步响应回调 |
| `callback/ToServerRpcSyncSessionCallback.java` | 同步 RPC 会话回调 |
| `callback/GetAttributesSyncSessionCallback.java` | 同步属性请求回调 |
| `callback/CoapNoOpCallback.java` | 空实现回调占位 |
| `client/CoapClientContext.java` | CoAP observe 与出站请求的客户端侧上下文 |
| `client/NoSecClient.java` | 无加密 CoAP 客户端 |
| `client/SecureClientNoAuth.java` | 不做证书认证的 DTLS 客户端 |
| `client/SecureClientX509.java` | 带 X.509 证书的 DTLS 客户端 |
| `client/TbCoapClientState.java` | 跟踪每客户端状态 |
| `client/TbCoapObservationState.java` | 跟踪 CoAP observe 关系 |
| `efento/CoapEfentoTransportResource.java` | Efento 传感器的 CoAP 资源 |
| `efento/adaptor/EfentoCoapAdaptor.java` | Efento 私有协议适配器 |

**会话管理：** CoAP 会话按客户端地址与 token 记录。同步会话经带超时的 `registerSyncSession()`
支持阻塞式请求（取属性、RPC 响应）。observe 关系经 `TbCoapObservationState` 管理。

**Efento 支持：** 原生支持使用私有 CoAP 协议的 Efento 无线传感器。`CoapEfentoTransportResource`
处理 Efento 专属资源路径，`EfentoCoapAdaptor` 解码其自定义二进制载荷格式。

### LwM2M Transport

**协议：** 基于 CoAP 的 Lightweight M2M（LwM2M）1.0 / 1.1  
**默认端口：** 5685（明文）、5686（DTLS）  
**库：** Eclipse Leshan  
**模块入口：** `transport/lwm2m/` -> `ThingsboardLwm2mTransportApplication.java`

| 文件 | 用途 |
|------|---------|
| `server/DefaultLwM2mTransportService.java` | 集成 Leshan server 的主传输服务 |
| `server/AbstractLwM2mTransportResource.java` | LwM2M 资源处理基类 |
| `server/adaptors/LwM2MTransportAdaptor.java` | 把 LwM2M 对象/资源转成遥测 |
| `server/adaptors/LwM2MJsonAdaptor.java` | JSON 载荷转换 |
| `server/client/LwM2mClient.java` | LwM2M 客户端表示 |
| `server/client/LwM2mClientContext.java` | 客户端上下文，含注册与 observation 状态 |
| `server/client/LwM2mClientContextImpl.java` | 客户端上下文实现 |
| `server/client/ModelObject.java` | LwM2M 对象模型定义 |
| `server/client/ResourceValue.java` / `ResourceUpdateResult.java` | 资源数据跟踪 |
| `server/attributes/DefaultLwM2MAttributesService.java` | LwM2M 的属性服务 |
| `secure/LwM2mCredentialsSecurityInfoValidator.java` | PSK、RPK、X.509 凭证校验 |
| `secure/TbLwM2MAuthorizer.java` | 供 Leshan 使用的 LwM2M 授权 provider |
| `secure/TbLwM2MDtlsCertificateVerifier.java` | DTLS 证书校验 |
| `secure/TbX509DtlsSessionInfo.java` | X.509 DTLS 会话元数据 |
| `secure/credentials/LwM2MClientCredentials.java` | 客户端凭证模型 |
| `bootstrap/LwM2MTransportBootstrapService.java` | bootstrap 服务端实现 |
| `bootstrap/store/LwM2MInMemoryBootstrapConfigStore.java` | 内存式 bootstrap 配置存储 |
| `bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java` | bootstrap 会话管理 |
| `config/TbLwM2mVersion.java` | LwM2M 版本枚举（1.0、1.1） |
| `config/LwM2MSecureServerConfig.java` | 安全服务端配置 |
| `config/LwM2MTransportServerConfig.java` | 传输服务端配置 |

**会话管理：** LwM2M 会话映射到 Leshan 的客户端注册。设备注册时，transport 创建
`LwM2mClientContext`、对设备认证，并把 LwM2M 对象实例映射到 ThingsBoard 遥测键。被 observe 的资源
会自动把更新作为遥测推上来。

**Bootstrap 服务端：** LwM2M transport 内含一个 bootstrap 服务端
（`LwM2MTransportBootstrapService`），用于下发设备安全与服务端配置。bootstrap 配置存在内存里，
按设备配置文件管理。

**安全：** 支持 PSK（预共享密钥）、RPK（裸公钥）和基于 X.509 证书的认证。`TbLwM2MAuthorizer` 用
ThingsBoard 中存储的设备凭证来校验。

### SNMP Transport

**协议：** SNMP v1、v2c、v3  
**默认端口：** 161（标准 SNMP）  
**库：** SNMP4J  
**模块入口：** `transport/snmp/` -> `ThingsboardSnmpTransportApplication.java`

| 文件 | 用途 |
|------|---------|
| `SnmpTransportContext.java` | SNMP transport 配置 |
| `SnmpTransportService.java` | 主 SNMP 传输服务 |
| `service/SnmpAuthService.java` | SNMP 凭证校验（community 串、USM） |
| `service/PduService.java` | SNMP PDU 处理（GET、GETNEXT、GETBULK、SET） |
| `service/ProtoTransportEntityService.java` | 为 SNMP 配置取实体数据 |
| `service/SnmpTransportBalancingService.java` | 在多个 SNMP transport 实例间做负载均衡 |
| `session/DeviceSessionContext.java` | 每设备的 SNMP 会话 |
| `session/ScheduledTask.java` | 定时 SNMP 轮询任务 |
| `event/SnmpTransportListChangedEvent.java` | transport 列表变化事件 |
| `event/SnmpTransportListChangedEventListener.java` | transport 列表变化监听器 |

**会话管理：** 与其他 transport 不同，SNMP 本质是轮询协议。SNMP transport 管理定时任务，周期性
轮询支持 SNMP 的设备。会话按轮询周期创建。该 transport 也会把收到的 SNMP trap（v1/v2c/v3）作为
遥测事件处理。

**负载均衡：** `SnmpTransportBalancingService` 把 SNMP 设备轮询分散到集群中的多个 transport 实例，
确保每台设备恰好被一个 transport 节点轮询。

## Transport 配置

每个 transport 从 Spring 环境读取自身配置（通常是 `thingsboard.yml` 或环境变量）。常见配置属性：

- `transport.<protocol>.bind_address` —— 绑定的网络接口
- `transport.<protocol>.bind_port` —— 监听端口
- `transport.<protocol>.timeout` —— 连接/会话超时（毫秒）
- `transport.<protocol>.ssl.enabled` —— 是否启用 TLS/DTLS
- `transport.<protocol>.ssl.credentials` —— SSL 证书配置
- `transport.<protocol>.rate_limits` —— 消息限流配置

## 关联关系

- [规则引擎](../30-rule-engine/README.zh-CN.md) —— transport 经队列把数据送往规则引擎
- [队列系统](../10-common/queue.zh-CN.md) —— transport 与核心之间基于 Kafka 的消息总线
- [支撑模块](../60-supporting/README.zh-CN.md) —— Netty MQTT 编解码、EDQS、监控工具
- [主应用](../50-application/README.zh-CN.md) —— 消费 transport 消息，承载规则引擎 actor
