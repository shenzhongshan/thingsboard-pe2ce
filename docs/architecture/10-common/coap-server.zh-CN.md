# Common CoAP Server（`common/coap-server`）

**本文是权威版。** 英文版 [coap-server.md](coap-server.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:coap-server`

`common/coap-server` 模块提供 ThingsBoard CoAP transport 共用的 CoAP 服务端基础设施。它建立在
Eclipse Californium（Cf）CoAP 框架之上，负责 DTLS 安全、会话管理和消息处理。

## 架构

该模块把 CoAP 服务端的搭建抽象成可复用组件。真正的 CoAP transport（在 `transport/coap/` 下）继承
这些类来构造面向设备的 CoAP 端点。

### 核心组件

| 类/接口 | 用途 |
|----------------|---------|
| `CoapServerService` | 管理 CoAP 服务端生命周期的接口 |
| `DefaultCoapServerService` | 基于 Californium `CoapServer` 的默认实现 |
| `CoapServerContext` | CoAP 服务端的上下文配置（host、端口、DTLS 设置） |
| `TbCoapServerComponent` | CoAP 服务端的组件包装 |
| `TbCoapTransportComponent` | 继承 TbCoapServerComponent、做设备专属处理的传输组件 |
| `TbCoapServerMessageDeliverer` | 自定义消息投递器，把 CoAP 请求路由到传输资源 |

### DTLS 安全

该模块为加密的 CoAP 通信提供 DTLS（Datagram Transport Layer Security）：

| 类 | 用途 |
|-------|---------|
| `TbCoapDtlsSettings` | DTLS 配置（密钥库、信任库、加密套件） |
| `TbCoapDtlsCertificateVerifier` | DTLS 握手的证书校验逻辑 |
| `TbCoapDtlsSessionInfo` | DTLS 会话元数据（加密套件、对端证书、会话 ID） |
| `TbCoapDtlsSessionKey` | DTLS 会话身份的复合键 |
| `TbCoapDtlsSessionInMemoryStorage` | 内存式 DTLS 会话存储 |

DTLS 会话存储把会话键映射到会话信息，从而支持会话恢复，以及从一个进行中的 DTLS 连接查出设备身份。

### CoAP 服务端生命周期

1. **配置** —— `CoapServerContext` 定义 host、端口、DTLS 设置、线程池
2. **初始化** —— `DefaultCoapServerService` 按配置的端点创建 `CoapServer`
3. **资源注册** —— 传输资源（遥测、属性、RPC 端点）经 `TbCoapServerMessageDeliverer` 注册
4. **运行** —— 入站 CoAP 请求被派发到已注册的 `CoapResource` 实例
5. **关闭** —— 服务端停止接受新连接并排空现有连接

### 消息流

```
Device
  |
  | CoAP Request (GET/POST)
  v
Californium CoapServer
  |
  v
TbCoapServerMessageDeliverer
  |
  v
CoapTransportResource (telemetry/attributes/RPC)
  |
  v
CoapTransportAdaptor -> TbMsg -> Queue -> Rule Engine
```

## 关键类详解

### DefaultCoapServerService

```java
public class DefaultCoapServerService implements CoapServerService {
    void init(CoapServerContext context);
    CoapServer getServer();
    void addResources(CoapResource... resources);
    void shutdown();
}
```

管理 Californium 的 `CoapServer` 实例，包括端点配置（UDP、DTLS）与资源注册。

### TbCoapDtlsSessionInMemoryStorage

以 `TbCoapDtlsSessionKey`（由对端身份派生）为索引存储 DTLS 会话信息。用于在认证期间把一个入站 DTLS
连接关联到某个 ThingsBoard 设备。

### TbCoapServerMessageDeliverer

自定义的 Californium `MessageDeliverer`，按 URI 路径把 CoAP 请求路由到相应的 `CoapResource`。
支持标准的 CoAP 资源约定：

| URI 路径 | 资源 |
|----------|------|
| `/api/v1/{token}/telemetry` | 遥测上传 |
| `/api/v1/{token}/attributes` | 属性拉取/更新 |
| `/api/v1/{token}/rpc` | RPC 请求/响应 |
| `/fw/{token}` | OTA 固件下载 |

## 依赖关系

- **依赖：** Californium（Eclipse CoAP 框架）、`common/data`、SLF4J
- **被谁依赖：** `transport/coap`（具体的 CoAP transport 实现）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `coap-server`
