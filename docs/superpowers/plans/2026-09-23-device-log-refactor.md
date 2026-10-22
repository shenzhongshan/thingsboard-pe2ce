# Device Log Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Separate audit event presentation conversion from device log orchestration and scope customer timeline alarms to the selected device without changing the public API.

**Architecture:** `DeviceLogServiceImpl` remains the permission/query/pagination/merge service. A package-private pure `DeviceLogAuditTimelineConverter` handles audit JSON parsing and display formatting from supplied values. Customer alarm candidates use the existing tenant V2 platform query constrained by the selected device, then retain only records belonging to the current customer; pagination continues through source pages when filtering leaves a page short.

**Tech Stack:** Java 25, Spring, Jackson, ThingsBoard alarm/audit/entity services; no new dependency.

**Spec:** `docs/superpowers/specs/2026-09-23-monitoring-device-log-alarms-refactor-design.md`

## Global Constraints

- 不改 Controller 路由、公开 Service 方法签名、DTO/VO 结构、权限入口、周期定义、排序口径或分页格式。
- 查询和翻页使用现有平台服务 API，不修改原生 DAO/Repository。
- 新写业务注释使用中文。
- 当前仓库有大量用户未提交修改。实施必须基于当前工作树逐文件核对，仅修改本计划列出的自定义实现和新增转换器；不能重置、覆盖或顺手提交其他已有修改。
- 项目 `AGENTS.md` 禁止未经当前明确授权运行 Maven 及其生命周期/测试命令。本轮按源码静态检查和差异审查验证；不运行 Maven、测试或构建。
- 故障电弧相关服务逻辑、DTO/VO、键名、遥测映射、计数含义和字段注释；在用户确认标准遥测键以前全部保持原样。

## Review Focus

- 客户用户请求某设备日志时，不返回同客户其他设备的告警，也不返回其他客户的告警。
- 客户 alarm 页面过滤后候选不足一页时，源分页仍向前推进；不能把“当前页没有目标客户告警”误判为“没有更多源数据”。
- 租户用户仍只读取所选设备告警；现有设备 READ 校验仍发生在告警查询前。
- JSON actionData 为 null、非对象、缺字段、嵌套 ID 或脏实体类型时，审计事件转换不抛出运行时异常，回退文案和字段仍保持原样。
- `DeviceLogServiceImpl.toActivityTimelineItems` 的同包静态入口、活动状态去重、倒序结果和最终时间过滤不变。

---

### Task 1: 提取审计时间线纯转换器

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/service/impl/DeviceLogAuditTimelineConverter.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/deviceLog/service/impl/DeviceLogServiceImpl.java`

**Interfaces:**
- Consumes: `AuditLog`, `JsonNode`, 已批量解析的 `Map<EntityId, EntityInfo>` 和现有时间线 DTO。
- Produces: package-static `DeviceLogAuditTimelineConverter.toAuditTimelineItem(AuditLog, Map<EntityId, EntityInfo>)` 以及 `collectRelationEntityIds(List<AuditLog>): Set<EntityId>`；服务继续负责调用 `EntityService` 并传入当前客户/租户范围。

- [x] **Step 1: 创建 package-private converter 并迁移纯审计转换**

迁移 `toAuditTimelineItem`、关系/属性变化字段格式化、关系端点解析、告警 ID 读取、actionData 字段读取、JSON 值格式化、操作者和动作标题格式化。将 `parseEntityId`、关系端点 ID 收集也迁入 converter，并提供纯方法 `collectRelationEntityIds`，让服务只负责把 ID 集合交给 `EntityService.fetchEntityInfos`。

- [x] **Step 2: 将服务改为委托并保留查询边界**

在 `findTimeline` 循环中调用 converter。`resolveRelationEntityInfos` 继续在 `DeviceLogServiceImpl` 内批量查询；converter 不接收 `SecurityUser`、`EntityService` 或数据库依赖。保留 `toAlarmTimelineItem` 的既有输出和 `toActivityTimelineItems` 的 package-static 委托入口；只移动其真正共享的纯 JSON helper。

- [x] **Step 3: 静态核对本任务**

Run: `git diff --check -- application/src/main/java/org/thingsboard/server/service/deviceLog/service/impl/DeviceLogServiceImpl.java application/src/main/java/org/thingsboard/server/service/deviceLog/service/impl/DeviceLogAuditTimelineConverter.java`
Expected: 无空白错误；服务只保留查询和批量名称解析；converter 不含数据库/权限服务字段；`toActivityTimelineItems` 声明仍在服务并委托原转换逻辑。

### Task 2: 修复客户时间线告警设备范围

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/service/deviceLog/service/impl/DeviceLogServiceImpl.java`

**Interfaces:**
- Consumes: 现有 `deviceInfo` 的 READ 权限校验、`AlarmService.findAlarmsV2(TenantId, AlarmQueryV2)`、`AlarmInfo.customerId`、`TimePageLink.nextPageLink()`。
- Produces: 私有 `TimelineAlarmCandidates(List<AlarmInfo> alarms, boolean hasNext)` record 和 `findTimelineAlarmCandidates(SecurityUser, DeviceId, TimePageLink, int)`；公开接口及游标格式不变。

- [x] **Step 1: 建立设备范围的 V2 告警查询条件**

设备 READ 校验成功之后，对指定 `DeviceId` 构造 `AlarmQueryV2`，沿用当前 `pageLink` 和 ANY 状态条件，调用租户 V2 查询。该查询 API 根据 `affectedEntityId` 在平台查询层限定告警来源设备。不要调用 `findCustomerAlarmsV2` 期待它过滤设备；静态核查确认该实现只应用 customerId 和时间/状态过滤。

```java
AlarmQueryV2 query = new AlarmQueryV2(
        entityId, pageLink, null, List.of(AlarmSearchStatus.ANY), null, null);
PageData<AlarmInfo> page = alarmService.findAlarmsV2(user.getTenantId(), query);
```

- [x] **Step 2: 对客户身份增加 customerId 过滤并填满候选页**

租户用户保留目标设备范围内的全部告警。客户用户只保留 `AlarmInfo.getCustomerId()` 与当前 `user.getCustomerId()` 一致的记录；当源页过滤后不足 `fetchSize` 且仍有源页时，用 `TimePageLink.nextPageLink()` 继续查询。`findTimelineAlarmCandidates` 返回 `TimelineAlarmCandidates`，其中 `hasNext` 为已有候选超过 timeline 页面容量或仍有未扫描源页。不得只对原单页过滤后把平台 `hasNext` 直接当作过滤后页的 `hasNext`。

- [x] **Step 3: 移除重复时间范围判断并保留统一过滤**

告警转换结果直接并入 `items`；保留后续统一的正时间戳、startTime、endTime 过滤和排序逻辑。不要在本任务顺手改变告警显示时间、游标精度或其他时间线来源语义。

- [x] **Step 4: 静态核对本任务**

Run: `git diff --check -- application/src/main/java/org/thingsboard/server/service/deviceLog/service/impl/DeviceLogServiceImpl.java`
Expected: 无空白错误；READ 权限校验位于告警查询之前；查询构造含目标 `DeviceId`；客户过滤发生在候选合并前；页链在每次继续时向前推进；最终差异不含 DAO/Repository 路径。
