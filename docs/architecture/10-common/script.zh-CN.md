# Common Script（`common/script`）

**本文是权威版。** 英文版 [script.md](script.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:script`（父 POM，type 为 `pom`）

`common/script` 模块为 ThingsBoard 规则引擎提供脚本执行引擎抽象。它支持两种脚本语言（TBEL 与
JavaScript），并可在本地执行，或经 Node.js sidecar 远程执行。

## 子模块

| 子模块 | Maven artifact | 用途 |
|-----------|---------------|---------|
| `script-api` | `org.thingsboard.common:script-api` | 核心脚本调用服务接口，TBEL 与 JS 实现 |
| `remote-js-client` | `org.thingsboard.common:remote-js-client` | 在远程 JS executor（Node.js sidecar）上调用脚本的客户端 |

## 架构

### 脚本语言

| 语言 | 枚举 | 实现 | 适用场景 |
|----------|------|---------------|---------|
| TBEL | `ScriptType.TBEL` | `DefaultTbelInvokeService` | 规则链、过滤器、转换（默认） |
| JavaScript（Nashorn） | `ScriptType.JS` | `NashornJsInvokeService` / `RemoteJsInvokeService` | 遗留/复杂脚本 |

TBEL（ThingsBoard Expression Language）是一门轻量表达式语言，针对 JSON 处理和遥测操作做了优化。
它是推荐且默认的脚本语言。

保留 JavaScript 支持是为了向后兼容，可本地运行（Nashorn，内嵌）或远程运行（Node.js sidecar）以获得
沙箱隔离。

### 脚本调用流程

1. **脚本编译** —— 脚本源码被编译/校验（`JsCompileRequest` 或 TBEL 解析）
2. **脚本缓存** —— 编译后的脚本按哈希缓存，避免重复编译
3. **执行** —— `TbScriptExecutionTask` 把一次脚本调用连同超时和参数包装起来
4. **结果** —— 执行返回 JSON 字符串结果，或抛出 `TbScriptException`

## script-api

### 核心接口与类

| 类/接口 | 用途 |
|----------------|---------|
| `ScriptInvokeService` | 脚本执行的顶层接口 |
| `AbstractScriptInvokeService` | 带缓存和阻塞检测的基础实现 |
| `ScriptType` | 枚举：`TBEL`、`JS`（JavaScript） |
| `TbScriptExecutionTask` | 携带脚本、超时、参数、回调的执行任务 |
| `TbScriptException` | 脚本编译/运行错误的异常 |
| `ScriptStatCallback` | 脚本执行统计的回调 |
| `BlockedScriptInfo` | 跟踪被阻塞/陈旧的脚本 |
| `RuleNodeScriptFactory` | 创建规则节点脚本的接口 |

### JavaScript（JS）

| 类/接口 | 用途 |
|----------------|---------|
| `JsInvokeService` | JavaScript 专属的调用服务接口 |
| `AbstractJsInvokeService` | JS 基础实现 |
| `JsScriptInfo` | 编译后的 JS 脚本元数据（函数名、MJS 模块） |
| `JsScriptExecutionTask` | JS 专属执行任务 |
| `JsValidator` | JS 脚本语法校验 |
| `NashornJsInvokeService` | 经 Nashorn 引擎的进程内 JS 执行 |

### TBEL（ThingsBoard Expression Language）

| 类/接口 | 用途 |
|----------------|---------|
| `TbelInvokeService` | TBEL 专属的调用服务接口 |
| `DefaultTbelInvokeService` | TBEL 执行引擎实现 |
| `TbelScript` | TBEL 脚本表示 |
| `TbelScriptExecutionTask` | TBEL 专属执行任务 |
| `TbDate` | 供 TBEL 使用的日期操作工具 |
| `TbJson` | 供 TBEL 使用的 JSON 操作工具 |
| `TbUtils` | TBEL 通用工具 |
| `TbTimeWindow` | 聚合用的时间窗配置 |
| `DateTimeFormatOptions` | 日期格式化选项 |

#### 计算字段参数（TBEL）

这些类型定义了从计算字段向 TBEL 脚本传参的方式：

| 类 | 用途 |
|-------|---------|
| `TbelCfCtx` | 计算字段执行上下文 |
| `TbelCfArg` | 参数基接口 |
| `TbelCfSingleValueArg` | 单个遥测值参数 |
| `TbelCfTsDoubleVal` | 时序 double 值对（ts、value） |
| `TbelCfTsMultiDoubleVal` | 多个时序 double 值 |
| `TbelCfTsRollingArg` | 滚动窗口时序参数 |
| `TbelCfTsRollingData` | 滚动窗口数据容器 |
| `TbelCfGeofencingArg` | 地理围栏区域参数 |
| `TbelCfPropagationArg` | 传播参数（关联实体） |
| `TbelCfRelatedEntitiesArgumentValue` | 关联实体查询结果 |
| `TbelCfObject` | 通用 CF 对象 |

## remote-js-client

提供远程 JS 执行协议的客户端侧（经 protobuf/Kafka 与 `msa/js-executor` 通信）：

| 类 | 用途 |
|-------|---------|
| `RemoteJsInvokeService` | 经队列发送 JS 调用请求的客户端 |
| `JsExecutorService` | JavaScript 执行接口 |
| `RemoteJsRequestEncoder` | 为队列编码 `JsInvokeProtos.RemoteJsRequest` |
| `RemoteJsResponseDecoder` | 从队列解码 `JsInvokeProtos.RemoteJsResponse` |

远程执行流程：
1. `RemoteJsInvokeService` 构造一个 `JsInvokeProtos.RemoteJsRequest`
2. 请求带关联 key 发布到 JS executor 的 Kafka topic
3. `RemoteJsResponseDecoder` 反序列化响应
4. 结果返回给调用方

基于 protobuf 的协议（见 [proto.zh-CN.md](proto.zh-CN.md) 的 `jsinvoke.proto`）支持：
- `JsCompileRequest` / `JsCompileResponse` —— 编译并缓存脚本
- `JsInvokeRequest` / `JsInvokeResponse` —— 带参数与超时执行脚本
- `JsReleaseRequest` / `JsReleaseResponse` —— 释放已缓存的脚本

错误码：`COMPILATION_ERROR`、`RUNTIME_ERROR`、`TIMEOUT_ERROR`、`NOT_FOUND_ERROR`。

## 依赖关系

- **依赖：** `common/data`、`common/message`、`common/proto`（js invoke protobuf）、`common/cache`、
  Jackson、GraalJS（JavaScript 引擎）或 Nashorn、ANTLR（TBEL 解析器）
- **被谁依赖：** `rule-engine/rule-engine-components`、`application`
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `script`（父）、`script-api`、`remote-js-client`
