# 雷击次数事件通知配置计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to execute this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 将防雷设备累计雷击次数的增长处理为逐条通知事件，不再依赖一个需要清除后才能再次触发的持续告警。

**方案：** 在防雷设备实际使用的默认规则链中，在保存原始遥测前用 `Calculate Delta` 计算相邻两次 `StrikeCnt` 的增量。只有 `StrikeCntDelta > 0` 时通过 `Send Notification` 发送 ThingsBoard 通知；所有分支最终仍回到原有遥测保存节点。通知内容包含设备名、当前累计值、本次增量和上报时间。

**平台：** ThingsBoard PE 规则链、通知中心、TBEL。

**Spec：** 本文档的“目标”“方案”和“全局约束”部分；依据本对话中已确认的事件通知方向及用户提供的现有计算字段/告警配置。

**方案来源：** 本对话中已确认的雷击事件通知方案，以及用户提供的计算字段和告警配置。当前防雷设备规范遥测键为 `StrikeCnt`；累计次数原始字段为 `LtNum`，实施前仍须在设备最新遥测和规则链消息中核对转换结果。

## 全局约束

- 不运行 Maven，也不构建或重启 ThingsBoard。
- 只修改 DESA/防雷设备实际使用的默认规则链；保留现有消息处理和保存路径。
- 分类依据使用 `productCategory = lightning`；共享规则链必须先限制到防雷设备。
- 规则链差值节点必须在保存当前 `StrikeCnt` 的 `Save Timeseries` 节点之前。
- 通知测试通过后再清理旧配置；不删除原始 `StrikeCnt` 遥测。
- 先确认当前 ThingsBoard PE 版本的 UI 中存在 `Calculate Delta` 和 `Send Notification` 节点；若节点不可用，暂停并按当前版本重新设计，不要直接套用 5 分钟滚动脚本。

## Review Focus / 重点验收场景

- 已知上一值为 0，收到 1：产生一条通知，增量为 1。
- `1 → 2`：再次产生一条通知，增量为 1，不受上一条通知状态影响。
- `1 → 3`：产生一条通知，增量为 2；一次上报对应一条通知，不拆成两条。
- 重复上报相同值：不发送通知，但仍沿原路径保存遥测。
- 首次无历史值：差值按 0 处理，不发送通知。
- 计数器回退/清零：负差值不触发雷击通知；原始报文仍保存，并验证回退后下一条递增数据可正常计算。
- 共用规则链中的非防雷设备：不得发送雷击通知。
- 通知发送失败：原始遥测仍须保存。

---

### 任务 1：确认设备键和默认规则链

**涉及对象：** ThingsBoard UI 中的 DESA 防雷设备、设备 Profile、Default Rule Chain。

- [ ] 打开设备最新遥测，确认平台键名是 `StrikeCnt`，记录当前累计值。
- [ ] 检查设备 `SERVER_SCOPE` 属性，确认 `productCategory` 为 `lightning`。
- [ ] 从设备使用的 Profile 找到实际的 `Default Rule Chain`；不要仅凭名称假设使用 Root Rule Chain。
- [ ] 记录现有 `POST_TELEMETRY_REQUEST` 路径、`Save Timeseries` 节点和后续连接；确认从哪个节点插入新逻辑而不破坏原流程。
- [ ] 确认规则链的 `POST_TELEMETRY_REQUEST` 消息中，设备名、时间戳及 `StrikeCnt` 的实际位置和值。

### 任务 2：停用旧的雷击增量与持续告警计算

**涉及对象：** 当前 5 分钟 rolling `StrikeCntDelta` Script Calculated Field，以及读取 `StrikeCntDelta` 并创建 `WARNING` 告警的 Alarm Calculated Field。

- [ ] 记录或导出两项旧配置，便于必要时恢复。
- [ ] 暂停旧 Script Calculated Field，避免与规则链同时写入 `StrikeCntDelta`。
- [ ] 暂停旧 Alarm Calculated Field，避免规则链新写入的增量继续创建持续告警。
- [ ] 保留旧配置但先不删除；待新通知链路验证通过后再决定是否清理。

### 任务 3：在规则链计算相邻报文增量

**新增节点：** `Calculate Delta`。

- [ ] 将节点插在当前 `Save Timeseries` 之前。
- [ ] 配置 `Input value key = StrikeCnt`、`Output value key = StrikeCntDelta`、小数位数 `0`。
- [ ] 开启 `Use caching`、`Exclude zero deltas from outbound message` 和 `Tell Failure if delta is negative`。
- [ ] 将 `Success` 接到任务 4 的过滤节点。
- [ ] 将 `Other` 和 `Failure` 接回原有 `Save Timeseries` 路径；负增量不得进入通知支路。
- [ ] 若设备共用规则链，先通过防雷设备专用链或 `SERVER_SCOPE productCategory` 检查，只让 `productCategory = lightning` 的设备进入差值节点。其他设备继续原有保存流程。
- [ ] 若必须在共用规则链中区分设备，在 `Calculate Delta` 前用 `Originator Attributes` 读取 `SERVER_SCOPE` 的 `productCategory`，再用 `Script Filter` 判断 `metadata.productCategory == 'lightning'` 且消息含 `StrikeCnt`；`True` 才进入差值节点，`False` 回原保存流程。若设备 Profile 已使用独立防雷默认链，则不增加此过滤。
- [ ] 保留已有 `Save Timeseries` 节点，保证原始 `StrikeCnt` 继续保存；成功分支后续产生的 `StrikeCntDelta` 也要经过保存节点。

> `Calculate Delta` 使用当前值减去上一值。文档特别要求把它放在保存当前输入值的节点之前；否则从数据库读取上一值时可能读到当前值，差值变成 0。开启缓存时，其状态是本地服务器内存缓存，集群部署时需重点验证节点迁移、缓存丢失和重复/乱序报文行为。

### 任务 4：筛选正增量并发送通知

**新增节点：** `Script Filter`、`Send Notification`。

- [ ] 在 `Calculate Delta` 的 `Success` 后添加 `Script Filter`，语言选 TBEL，脚本为：

```tbel
return msg.containsKey("StrikeCntDelta") && msg.StrikeCntDelta > 0;
```

- [ ] `False` 连接到原有 `Save Timeseries`，保证零增量消息照常保存。
- [ ] `True` 连接到 `Send Notification`。
- [ ] `Send Notification` 的 `Success` 和 `Failure` 都连接回原有 `Save Timeseries`，确保通知服务异常不阻断遥测保存。
- [ ] 不额外建立 Notification Center 的触发规则；发送时机由规则链过滤节点决定。

建议接线：

```text
POST_TELEMETRY_REQUEST
  → Calculate Delta
      Success → Script Filter
          True  → Send Notification ─┬→ 原有 Save Timeseries
                                     └(Failure 也回到保存节点)
          False ───────────────────────→ 原有 Save Timeseries
      Other ──────────────────────────→ 原有 Save Timeseries
      Failure ────────────────────────→ 原有 Save Timeseries
```

### 任务 5：创建模板和接收人

- [ ] 在 **Monitor → Notifications → Recipients** 建立接收人组，并添加需要收到雷击事件的用户。
- [ ] 在 **Monitor → Notifications → Templates** 新建模板，类型选 `Rule Node`；初次验证先选 Web 渠道。
- [ ] 模板标题填写：`雷击事件：${deviceName}`。
- [ ] 模板内容填写：

```text
设备：${deviceName}
事件：检测到累计雷击次数增加
本次新增：${StrikeCntDelta} 次
当前累计：${StrikeCnt} 次
上报时间：${ts}
```

- [ ] 在规则链 `Send Notification` 节点选择此模板和接收人组。
- [ ] 通过规则链调试信息确认 `${ts}` 是设备上报时间还是平台接收时间。若值缺失或格式不适合阅读，先记录实际消息元数据，再决定是否需要增加时间格式转换；不要猜测时间字段。

### 任务 6：验证、观察并清理旧告警

- [ ] 保存规则链后，记录设备当前值 `n`；发送/等待同值 `n`，确认不发通知且遥测仍保存。
- [ ] 让设备从 `n` 增至 `n+1`，确认只产生一条通知，显示设备名、增量 1 和时间。
- [ ] 让设备从 `n+1` 增至 `n+3`，确认只产生一条通知，显示增量 2。
- [ ] 再发相同的 `n+3`，确认不重复通知。
- [ ] 检查规则链节点事件、通知收件箱/已发送列表和设备时序，确认失败分支及其他设备均没有误发。
- [ ] 单独验证计数器回退后再递增的行为；若下一条正增量未恢复，先暂停上线并重新确认缓存/基准值处理方式。
- [ ] 验证通过后清除现存的旧“雷击”活动告警；停用旧配置不会自动清除已创建的告警。
- [ ] 保留或删除旧计算字段前，确认没有其他看板、规则或查询仍依赖旧 `StrikeCntDelta` 输出。

## 官方参考

- [Calculate Delta 节点](https://thingsboard.io/docs/pe/reference/rule-engine/nodes/enrichment/calculate-delta/)
- [Script Filter 节点](https://thingsboard.io/docs/pe/reference/rule-engine/nodes/filter/script/)
- [Send Notification 节点](https://thingsboard.io/docs/pe/reference/rule-engine/nodes/external/send-notification/)
- [通知模板参数（Rule Node）](https://thingsboard.io/docs/pe/reference/notification-system/template-parameters/)
