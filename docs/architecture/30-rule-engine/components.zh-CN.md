# 规则节点编目

**本文是权威版。** 英文版 [components.md](components.md) 为对照上游 CE 的参照副本，改动请先落在本文。

`rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/` 下全部 29 类规则
节点的编目，按功能域分组。

---

## Filter（过滤）

**目录：** `filter/`

做路由决策的节点 —— 基于数据检查把消息送往不同的关系。

| 类 | 说明 | 关系 |
|-------|-------------|-----------|
| `TbMsgTypeFilterNode` | 按 `TbMsgType` 路由（POST_TELEMETRY_REQUEST、POST_ATTRIBUTES_REQUEST 等） | 每个被选中的消息类型一条关系 |
| `TbMsgTypeSwitchNode` | 把消息切到以消息类型命名的输出 | 动态：每种消息类型一个输出 |
| `TbOriginatorTypeFilterNode` | 按发起者实体类型路由（Device、Asset 等） | 每个被选中的实体类型一条关系 |
| `TbOriginatorTypeSwitchNode` | 按发起者类型切换 | 动态：每种实体类型一个输出 |
| `TbDeviceTypeSwitchNode` | 按设备配置文件名切换 | 动态：每种设备类型一个输出 |
| `TbAssetTypeSwitchNode` | 按资产配置文件名切换 | 动态：每种资产类型一个输出 |
| `TbCheckMessageNode` | 用可配置的谓词（包含、等于、正则、数值比较）对消息 data/元数据求值 | True、False |
| `TbCheckAlarmStatusNode` | 检查发起者是否存在指定类型/严重级别的告警 | True、False |
| `TbCheckRelationNode` | 检查从发起者到目标实体是否存在关系 | True、False |
| `TbJsFilterNode` | 执行用户自定义的 JavaScript 函数，返回 `true`/`false` | True、False |
| `TbJsSwitchNode` | 执行用户自定义 JavaScript，返回下一条关系的名字 | 动态：关系名来自脚本输出 |

**抽象基类：** `TbAbstractTypeSwitchNode` —— 基于类型的节点共用的切换逻辑。

---

## Enrichment / Metadata（富化）

**目录：** `metadata/`

取额外数据并附加到消息上（作为元数据或放进载荷）的节点。

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbGetAttributesNode` | 取某实体（发起者或关联实体）的属性 | Success、Failure |
| `TbGetDeviceAttrNode` | 取设备属性（设备专属查找的快捷方式） | Success、Failure |
| `TbGetTenantAttributeNode` | 取租户级属性 | Success、Failure |
| `TbGetCustomerAttributeNode` | 取客户级属性 | Success、Failure |
| `TbGetRelatedAttributeNode` | 从关联实体取属性 | Success、Failure |
| `TbGetTelemetryNode` | 取某实体的最新时序 | Success、Failure |
| `TbGetOriginatorFieldsNode` | 从发起者取实体字段（名字、类型、附加信息） | Success、Failure |
| `TbGetEntityDataNode` | 通用地取已配置的实体字段 | Success、Failure |
| `TbGetTenantDetailsNode` | 取租户详情（标题、地址等） | Success、Failure |
| `TbGetCustomerDetailsNode` | 取客户详情 | Success、Failure |
| `TbGetMappedDataNode` | 把源数据键映射到目标数据键 | Success、Failure |
| `TbFetchDeviceCredentialsNode` | 取设备凭证（访问令牌、X.509 证书） | Success、Failure |
| `CalculateDeltaNode` | 计算当前与上一次遥测值之间的增量（差值） | Success、Failure |

**抽象基类：**
- `TbAbstractGetEntityDetailsNode` —— 取实体详情字段的共用逻辑
- `TbAbstractGetAttributesNode` —— 取属性的共用逻辑
- `TbAbstractGetEntityDataNode` —— 取实体数据的共用逻辑
- `TbAbstractGetMappedDataNode` —— 键映射的共用逻辑
- `TbAbstractNodeWithFetchTo` —— 取数目标路由的共用逻辑

---

## Transformation（转换）

**目录：** `transform/`

修改消息的节点：载荷、元数据或发起者。

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbTransformMsgNode` | 执行用户自定义 JavaScript 以转换消息载荷和/或元数据 | Success、Failure |
| `TbChangeOriginatorNode` | 把消息发起者改成另一个实体（来源：关联实体、客户、租户或告警发起者） | Success、Failure |
| `TbJsonPathNode` | 在消息载荷上求值 JSONPath 表达式，把结果放入元数据 | Success、Failure |
| `TbRenameKeysNode` | 重命名消息载荷里的键 | Success、Failure |
| `TbDeleteKeysNode` | 从消息载荷里删除键 | Success、Failure |
| `TbCopyKeysNode` | 在载荷与元数据之间复制键（可同时重命名） | Success、Failure |
| `TbSplitArrayMsgNode` | 把含 JSON 数组的消息拆成多条消息，每个数组元素一条 | Success（每个元素一次） |
| `TbAbstractTransformNode` | 简单转换节点的基类 | —— |

---

## Action（动作）

**目录：** `action/`

执行副作用的节点：创建/更新实体、下发命令、记录数据。

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbCreateAlarmNode` | 创建或更新告警（封装 `create_or_update_active_alarm` 逻辑） | Created、Updated、Failure |
| `TbClearAlarmNode` | 清除活跃告警 | Cleared、Failure |
| `TbCreateRelationNode` | 创建实体关系 | Success、Failure |
| `TbDeleteRelationNode` | 删除实体关系 | Success、Failure |
| `TbAssignToCustomerNode` | 把实体分配给客户 | Success、Failure |
| `TbUnassignFromCustomerNode` | 把实体从客户取消分配 | Success、Failure |
| `TbDeviceStateNode` | 管理设备连接状态（已连接/已断开） | Success、Failure |
| `TbSaveToCustomCassandraTableNode` | 把数据存入用户自定义的 Cassandra 表 | Success、Failure |
| `TbLogNode` | 把消息记入服务端日志（用于调试） | Success |
| `TbMsgCountNode` | 统计穿过的消息数（用于监控/限流） | Success |
| `TbCopyAttributesToEntityViewNode` | 把发起者实体的属性复制到它的实体视图 | Success |

**抽象基类：**
- `TbAbstractAlarmNode` —— 共用的告警创建/更新逻辑
- `TbAbstractCustomerActionNode` —— 共用的客户分配逻辑
- `TbAbstractRelationActionNode` —— 共用的关系管理逻辑

---

## Telemetry（遥测）

**目录：** `telemetry/`

保存时序数据或属性的节点。

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbMsgTimeseriesNode` | 把消息 data 作为发起者的时序（遥测）保存 | Success、Failure |
| `TbMsgAttributesNode` | 把消息 data 作为发起者的属性（server 作用域）保存 | Success、Failure |
| `TbMsgDeleteAttributesNode` | 删除发起者的属性 | Success、Failure |
| `TbCalculatedFieldsNode` | 为发起者求值计算字段并保存结果 | Success、Failure |

---

## 外部集成

### REST API 调用

**目录：** `rest/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbRestApiCallNode` | 向外部 REST API 发起 HTTP 请求（GET、POST、PUT、DELETE 等）。支持可配置的 header、body 模板和响应处理 | Success、Failure |
| `TbSendRestApiCallReplyNode` | 把 HTTP 响应回送给正在等待的 REST API 调用（请求-应答模式） | Success、Failure |

### Kafka

**目录：** `kafka/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbKafkaNode` | 把消息发布到可配置的 Kafka topic，带 key、header 和载荷 | Success、Failure |

### MQTT

**目录：** `mqtt/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbMqttNode` | 把消息发布到 MQTT broker（topic、QoS、retain 标志均可配） | Success、Failure |

### RabbitMQ

**目录：** `rabbitmq/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbRabbitMqNode` | 把消息发布到 RabbitMQ exchange，带 routing key | Success、Failure |

### Email（邮件）

**目录：** `mail/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbSendEmailNode` | 发送邮件，收发件人、主题、正文均可配（支持 HTML 与模板变量） | Success、Failure |
| `TbMsgToEmailNode` | 把消息载荷转成邮件（`TbEmail` 对象）供下游处理 | Success、Failure |

### SMS（短信）

**目录：** `sms/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbSendSmsNode` | 经已配置的短信服务商（Twilio、AWS SNS 等）发送短信 | Success、Failure |

### RPC

**目录：** `rpc/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbSendRPCRequestNode` | 向设备下发 RPC 命令（服务端到设备），method、params、超时均可配 | Success、Failure |
| `TbSendRPCReplyNode` | 回应来自设备的待处理 RPC 请求 | Success、Failure |

### AWS

**目录：** `aws/`（子目录：`lambda/`、`sns/`、`sqs/`）

| 子目录 | 说明 |
|-------------|-------------|
| `lambda/` | 调用 AWS Lambda 函数 |
| `sns/` | 把消息发布到 AWS SNS topic |
| `sqs/` | 把消息发送到 AWS SQS 队列 |

### GCP

**目录：** `gcp/`（子目录：`pubsub/`）

| 子目录 | 说明 |
|-------------|-------------|
| `pubsub/` | 把消息发布到 Google Cloud Pub/Sub topic |

---

## AI

**目录：** `ai/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbAiNode` | 调用某个 AI 模型（在系统的 AI 模型中配置）做对话补全、嵌入或文本生成。支持经 LangChain4j 做结构化输出 | Success、Failure |

配套文件：
- `TbAiNodeConfiguration` —— 节点配置（模型选择、提示词模板、响应格式）
- `TbResponseFormat` —— 枚举：TEXT、JSON
- `Langchain4jJsonSchemaAdapter` —— 经 LangChain4j 做结构化 JSON 输出的适配器

---

## Math（数学）

**目录：** `math/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbMathNode` | 对消息数据执行数学运算（ADD、SUBTRACT、MULTIPLY、DIVIDE 等）。支持自定义表达式，参数可来自载荷/元数据 | Success、Failure |

配套文件：
- `TbRuleNodeMathFunctionType` —— 受支持运算的枚举
- `TbMathArgument` / `TbMathArgumentValue` / `TbMathResult` —— 数据包装

---

## Geo / 地理围栏

**目录：** `geo/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbGpsGeofencingFilterNode` | 检查消息里的 GPS 坐标是否在已配置的围栏多边形或半径之内/之外 | True、False |
| `TbGpsGeofencingActionNode` | 基于围栏事件（进入、离开、在内、在外）触发动作 | Success、Failure |

**抽象基类：** `AbstractGeofencingNode` —— 共用的围栏求值逻辑（多边形的点在多边形内判定、圆半径检查）。

---

## Deduplication（去重）

**目录：** `deduplication/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbMsgDeduplicationNode` | 按可配置的键策略（整条消息、发起者、租户或自定义字段）去重。使用基于时间的窗口 | Unique、Duplicate、Failure |

配套文件：
- `DeduplicationStrategy` —— 去重策略枚举
- `DeduplicationData` / `DeduplicationId` —— 去重状态的数据包装

---

## Delay（延迟）

**目录：** `delay/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbMsgDelayNode` | 把消息处理延迟一段可配置的时间（固定时长或从消息数据推导）。经 Kafka 做定时投递 | Success |

---

## Flow Control（流程控制）

**目录：** `flow/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbRuleChainInputNode` | 规则链的入口。接收来自 transport 或父链的消息 | Success |
| `TbRuleChainOutputNode` | 出口 —— 把消息转发给父链或结束处理 | Success |
| `TbAckNode` | 显式确认消息已处理（标记为成功处理） | Success |
| `TbCheckpointNode` | 为消息处理创建检查点。在复杂链里用于事务一致性 | Success |

---

## Transaction / 同步

**目录：** `transaction/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbSynchronizationBeginNode` | 标记同步事务块的开始。开始累积消息 | Success |
| `TbSynchronizationEndNode` | 标记同步事务块的结束。释放累积的消息 | Success |

---

## Edge

**目录：** `edge/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbMsgPushToEdgeNode` | 把消息（实体更新）推给已连接的 edge 实例做同步 | Success、Failure |
| `TbMsgPushToCloudNode` | 把消息从 edge 实例上推到云端 | Success、Failure |

**抽象基类：** `AbstractTbMsgPushNode` / `BaseTbMsgPushNodeConfiguration`

---

## Profile / 告警规则

**目录：** `profile/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbDeviceProfileNode` | 用设备配置文件的告警规则对入站遥测求值。按阈值条件、时间表和动态谓词创建/清除告警 | Success、Failure |

配套文件（告警规则状态机）：
- `AlarmRuleState` / `AlarmState` / `DeviceState` —— 状态跟踪枚举
- `AlarmEvalResult` —— 告警求值结果
- `DataSnapshot` / `SnapshotUpdate` —— 供告警求值用的遥测快照
- `EntityKeyValue` —— 供动态谓词用的实体键值
- `DynamicPredicateValueCtx` —— 解析动态谓词的上下文
- `ProfileState` —— 聚合后的配置文件规则状态
- `state/` 子目录 —— 配置文件告警规则的持久化状态管理

---

## Notifications（通知）

**目录：** `notification/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbNotificationNode` | 经通知中心发送通知（站内、webhook 等） | Success、Failure |
| `TbSlackNode` | 经 webhook 向 Slack 频道发消息 | Success、Failure |

---

## Debug（调试）

**目录：** `debug/`

| 类 | 说明 | 输出 |
|-------|-------------|--------|
| `TbMsgGeneratorNode` | 生成合成消息以测试规则链。消息类型、载荷和元数据均可配 | Success |

---

## Credentials（凭证）

**目录：** `credentials/`

管理外部集成（HTTP、MQTT、Kafka 等）所用的凭证类型。

| 类 | 说明 |
|-------|-------------|
| `AnonymousCredentials` | 不认证 |
| `BasicCredentials` | 用户名/密码认证 |
| `CertPemCredentials` | 基于证书（PEM）的认证 |
| `ClientCredentials` | Client ID/secret（OAuth2 风格）认证 |
| `CredentialsType` | 枚举：ANONYMOUS、BASIC、CERT_PEM、CLIENT |

---

## Utilities（工具）

**目录：** `util/`

多个规则节点分类共用的工具：

| 类 | 说明 |
|-------|-------------|
| `TbMsgSource` | 消息数据来源的抽象（元数据、载荷或发起者属性） |
| `TenantIdLoader` | 从消息上下文加载租户 ID 的工具 |
| `EntitiesByNameAndTypeLoader` | 按名字与类型异步加载实体 |
| `EntitiesFieldsAsyncLoader` | 异步加载实体字段 |
| `EntitiesRelatedEntityIdAsyncLoader` | 异步加载关联实体 ID |
| `EntitiesAlarmOriginatorIdAsyncLoader` | 异步加载告警发起者 ID |
| `ContactBasedEntityDetails` | 实体详情节点（租户、客户）的共享数据 |
| `GpsGeofencingEvents` | GPS 地理围栏事件常量 |
| `SemaphoreWithTbMsgQueue` | 基于信号量的消息队列，用于限流 |
| `DataToFetch` | 控制取哪类数据的枚举（LATEST_TELEMETRY、ATTRIBUTES、FIELDS 等） |
| `FetchMode` | 枚举：FIRST、LAST、ALL |

---

## Data（查询类型）

**目录：** `data/`

基于关系的查询所用的共享数据类：

| 类 | 说明 |
|-------|-------------|
| `RelationsQuery` | 定义一次关系查询（方向、关系类型、实体类型） |
| `DeviceRelationsQuery` | 带设备配置文件过滤的设备专属关系查询 |

---

## 小结：按组件类型分类

| 分类 | 类型 | 数量 |
|----------|------|-------|
| `filter/` | FILTER | 约 11 个节点 |
| `metadata/` | ENRICHMENT | 约 13 个节点 |
| `transform/` | TRANSFORMATION | 约 8 个节点 |
| `action/` | ACTION | 约 11 个节点 |
| `telemetry/` | ACTION | 约 4 个节点 |
| `rest/` | EXTERNAL | 约 2 个节点 |
| `kafka/` | EXTERNAL | 1 个节点 |
| `mqtt/` | EXTERNAL | 1 个节点 |
| `rabbitmq/` | EXTERNAL | 1 个节点 |
| `mail/` | EXTERNAL | 约 2 个节点 |
| `sms/` | EXTERNAL | 1 个节点 |
| `rpc/` | ACTION | 约 2 个节点 |
| `aws/` | EXTERNAL | 3 个子分类（Lambda、SNS、SQS） |
| `gcp/` | EXTERNAL | 1 个子分类（Pub/Sub） |
| `ai/` | EXTERNAL | 1 个节点 |
| `math/` | TRANSFORMATION | 1 个节点 |
| `geo/` | FILTER + ACTION | 2 个节点 |
| `deduplication/` | FILTER | 1 个节点 |
| `delay/` | ACTION | 1 个节点 |
| `flow/` | FLOW | 4 个节点 |
| `transaction/` | FLOW | 2 个节点 |
| `edge/` | ACTION | 2 个节点 |
| `profile/` | FILTER + ACTION | 1 个节点 |
| `notification/` | ACTION | 2 个节点 |
| `debug/` | ACTION | 1 个节点 |
| `credentials/` | ——（支撑） | 5 种凭证类型 |
| `util/` | ——（支撑） | 约 12 个工具 |
| `data/` | ——（支撑） | 2 个查询类 |

**合计：29 个分类下 223 个 Java 文件**

## 相关文档

- [规则引擎总览](README.zh-CN.md) —— 架构、消息生命周期、API 参考
- [DAO 层总览](../20-dao/README.zh-CN.md) —— 规则节点如何访问数据库
