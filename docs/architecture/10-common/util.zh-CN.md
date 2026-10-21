# Common Util（`common/util`）

**本文是权威版。** 英文版 [util.md](util.md) 为对照上游 CE 的参照副本，改动请先落在本文。

**Maven artifact：** `org.thingsboard.common:util`

`common/util` 模块提供 ThingsBoard 各模块通用的工具类，覆盖 JSON 处理、地理空间计算、并发、SSL、
校验和系统级辅助。

## 架构

所有工具类都是无状态且线程安全的。它们遵循函数式/静态方法风格，依赖极少（主要是 Jackson、SLF4J
和标准 Java 库）。

## 工具类

### JSON 与序列化

| 类 | 用途 |
|-------|---------|
| `JacksonUtil` | 基于 Jackson ObjectMapper 的核心 JSON 工具 —— 序列化、反序列化、树操作、美化输出 |
| `JsonSchemaUtils` | 用 networknt/json-schema-validator 做 JSON Schema 校验 |
| `ExpressionUtils` | 供脚本求值使用的表达式/变量解析工具 |

### 并发

| 类 | 用途 |
|-------|---------|
| `ThingsBoardExecutors` | 按 ThingsBoard 命名与线程池约定创建 executor 的工厂 |
| `ThingsBoardThreadFactory` | 自定义 ThreadFactory，带 ThingsBoard 线程命名与守护线程配置 |
| `ThingsBoardForkJoinWorkerThreadFactory` | 并行处理用的 ForkJoinPool 线程工厂 |
| `ThingsBoardScheduledThreadPoolExecutor` | 遵循 ThingsBoard 约定的定时 executor |
| `ListeningExecutor` | 支持 listenable future 的 executor 接口 |
| `AbstractListeningExecutor` | 支持 listenable future 的 executor 基类 |
| `DirectListeningExecutor` | 把直接（同线程）执行包装成 `ListenableFuture` 的 executor |
| `ExecutorProvider` | 获取 executor 的 provider 接口 |
| `DonAsynchron` | 用带回调包装的方式异步执行任务的工具（类名为法语命名） |
| `NoOpFutureCallback` | 空实现的 `FutureCallback` |
| `RecoveryAware` | 感知恢复/重启事件的组件接口 |

### 缓存与集合

| 类 | 用途 |
|-------|---------|
| `CachedValue` | 带懒初始化与 TTL 的通用线程安全缓存值 |
| `SetCache` | 带 TTL 过期的简单内存 set |
| `LinkedHashMapRemoveEldest` | 溢出时移除最旧条目的类 LRU LinkedHashMap |
| `TbStringPool` | 字符串驻留池，降低重复字符串的内存占用 |
| `TbBytePool` | 字节数组池，降低 GC 压力 |

### 地理空间

| 类 | 用途 |
|-------|---------|
| `GeoUtil` | 地理距离计算、坐标校验 |
| `Coordinates` | 经纬度对 |
| `Perimeter` | 地理边界接口 |
| `PerimeterDefinition` | 边界定义（DTO） |
| `PerimeterDefinitionSerializer` / `PerimeterDefinitionDeserializer` | 边界类型的 Jackson 序列化器 |
| `PerimeterType` | 枚举：`CIRCLE`、`POLYGON` |
| `CirclePerimeterDefinition` | 圆形地理围栏（圆心 + 半径） |
| `PolygonPerimeterDefinition` | 多边形地理围栏（坐标点数组） |
| `RangeUnit` | 距离单位枚举：`METER`、`KILOMETER`、`FOOT`、`MILE`、`NAUTICAL_MILE` |

### 监控与调试

| 类 | 用途 |
|-------|---------|
| `TbStopWatch` | 用于性能测量的高精度计时器 |
| `DebugModeUtil` | 调试模式检测与日志工具 |
| `SystemUtil` | 系统信息（OS、内存、进程信息） |
| `ExceptionUtil` | 异常辅助工具 |

### 校验与安全

| 类 | 用途 |
|-------|---------|
| `SsrfProtectionValidator` | 针对规则节点发起的 HTTP 调用做服务端请求伪造（SSRF）防护 |
| `SslUtil` | SSL/TLS 工具方法（加载密钥库、处理证书） |
| `RegexUtils` | 正则编译与匹配工具 |
| `NumberUtils` | 数字解析与比较工具 |
| `DeduplicationUtil` | 实体/遥测去重逻辑 |
| `KvUtil` | 键值数据操作工具 |

### 外部服务工具

| 类 | 用途 |
|-------|---------|
| `AzureIotHubUtil` | Azure IoT Hub 集成辅助 |
| （geo 包） | 供基于位置的规则使用的地理围栏工具 |

## 关键用法模式

### JacksonUtil

集中所有 JSON 操作。提供：
- `OBJECT_MAPPER` —— 共享的、已预配置的 ObjectMapper 实例
- `toString(Object o)` / `toPrettyString(Object o)` —— 序列化
- `fromString(String json, Class<T> clazz)` —— 反序列化
- `toJsonNode(String json)` / `fromJsonNode(JsonNode, Class<T>)` —— 树操作
- `ObjectMapper` 配置：`FAIL_ON_UNKNOWN_PROPERTIES=false`、Java 8 时间模块、
  `ACCEPT_SINGLE_VALUE_AS_ARRAY` 等

### Executor 用法

```java
// Create a fixed thread pool with ThingsBoard naming
ExecutorService executor = ThingsBoardExecutors.newFixedThreadPool(4, "my-task");

// Create a scheduled executor
ScheduledExecutorService scheduler = ThingsBoardExecutors.newScheduledThreadPool(2, "my-scheduler");

// Create a work-stealing pool (ForkJoinPool)
ExecutorService forkJoin = ThingsBoardExecutors.newWorkStealingPool(8, "my-forkjoin");
```

`ThingsBoardThreadFactory` 创建的线程带有：
- 线程组：`thingsboard`
- 命名：`prefix-{counter}`
- 守护线程：可配
- 未捕获异常处理器：可配

### GeoUtil

被规则引擎的地理围栏功能使用：
- `distance(Coordinates c1, Coordinates c2, RangeUnit unit)` —— 计算两点间距离
- `contains(Perimeter perimeter, Coordinates point)` —— 判断某点是否在地理围栏内

## 依赖关系

- **依赖：** Jackson、SLF4J、标准 Java 库 —— 依赖极少
- **被谁依赖：** 几乎项目中的每个模块（所有公共模块里被使用最广的一个）
- **Maven groupId：** `org.thingsboard.common`
- **Maven artifactId：** `util`
