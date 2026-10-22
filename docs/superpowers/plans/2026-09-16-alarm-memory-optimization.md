# 监测与告警后端查询质量优化实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除告警统计场景的全量内存累积，并修复已确认的重复查询、无界并发、模块反向依赖和设备状态口径不一致问题，同时保持现有权限过滤、统计结果和页面行为不变。

**Architecture:** 保留 `findPage` 作为页面列表查询入口，在告警查询服务中增加“逐页消费”入口。统计类服务通过显式分页循环逐页处理，每次只保留当前页 `AlarmInfo`，最终只保留计数、设备 ID、类型汇总和趋势桶等必要聚合状态；不再调用会返回全量 `List<AlarmInfo>` 的 `findAll`。监测侧复用一次性设备上下文，遥测请求采用有界批次；跨模块常量移动到中性工具包，避免 `monitoring` 与 `alarms` 双向依赖。数据库查询仍复用 ThingsBoard 原生服务，不新增业务表、不改变告警接口返回结构。

**Tech Stack:** Java、Spring Service、ThingsBoard `AlarmService`、`AlarmQueryV2`、`TimePageLink`、JUnit/Mockito 现有测试体系。

**Spec:** 本文档即本次告警分页聚合内存优化规格。

## Global Constraints

- 只优化 `application/src/main/java/org/thingsboard/server/service/alarms` 及其现有调用方，不修改前端业务行为。
- 保留客户用户与租户管理员的权限分支，继续分别调用 `findCustomerAlarmsV2` 和 `findAlarmsV2`。
- 统计口径保持不变：周期统计仍按 `AlarmInfo.startTs` 判断，页面分页仍按原生查询返回。
- 统计处理使用显式 `while`/`for` 循环，避免引入新的 Stream 链和大量 lambda。
- 告警聚合分页大小独立于设备分页大小，建议固定为 `500`；该值只限制单页内存，不限制最终统计结果。
- 只把已确认的性能和边界问题列为实施项；争议项不在未确认前改变业务行为。
- 本轮允许补充源码和测试，但不执行 Maven、构建、测试或服务重启；测试程序由用户在本地运行。

## 1. 当前问题与边界

当前 `DeviceAlarmQueryServiceImpl.findAll` 的流程是：

```java
List<AlarmInfo> alarms = new ArrayList<>();
while (pageLink != null) {
    PageData<AlarmInfo> pageData = findPage(user, query);
    alarms.addAll(pageData.getData());
    pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
}
return alarms;
```

这段逻辑虽然避免了固定单页上限造成的统计遗漏，但会把所有历史告警保存到一个 `List`。告警量越大，`AlarmInfo` 对象、字符串、来源对象和集合容量会持续增长；随后分析服务还会再次遍历这份列表。

已确认的生产调用方包括：

- `DeviceAlarmSummaryServiceImpl.countActiveAlarms`
- `DeviceAlarmAnalysisServiceImpl.getLeafAssetCounts`
- `DeviceAlarmAnalysisServiceImpl.getTypeRanking`
- `DeviceAlarmAnalysisServiceImpl.getSeverityRanking`
- `DeviceAlarmAnalysisServiceImpl.collectOverviewData`
- `DeviceAlarmAnalysisServiceImpl.collectAlarmTrend`
- `DeviceAlarmAnalysisServiceImpl.collectActiveCriticalAlarms`
- `SmartLightningDashboardServiceImpl.collectAlarmRecords`
- `DeviceAlarmQueryServiceImpl.findActiveAlarmDeviceIds`

页面列表本身使用 `findPage`，不属于本次问题范围。

## 2. 推荐方案

### 2.1 查询服务增加逐页消费接口

修改：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/alarms/service/DeviceAlarmQueryService.java`

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmQueryServiceImpl.java`

新增一个受检异常可传递的页面访问接口，避免使用 `java.util.function.Consumer`：

```java
@FunctionalInterface
interface AlarmPageVisitor {
    void visit(List<AlarmInfo> page) throws ThingsboardException;
}

void forEachPage(SecurityUser user, AlarmQueryV2 query,
                 AlarmPageVisitor visitor) throws ThingsboardException;
```

实现要求：

1. 校验 `query` 和 `query.getPageLink()`，为空时直接返回。
2. 从第一页开始调用现有 `findPage`。
3. 将当前页交给 `visitor.visit(pageData.getData())`。
4. 使用 `pageData.hasNext()` 和 `TimePageLink.nextPageLink()` 继续翻页。
5. 每轮结束后释放对当前页的局部引用，不建立全量列表。
6. 查询权限分支仍只由 `findPage` 负责，不能在各业务实现中重复判断客户用户/租户管理员。

建议在告警查询实现中使用独立常量：

```java
private static final int ALARM_PAGE_SIZE = 500;
```

所有统计查询创建 `TimePageLink` 时使用该大小。不要复用当前 `MonitoringQueryDefaults.PAGE_SIZE = 10_000` 作为告警聚合页大小，避免单页仍然过大。

### 2.2 处理 `findAll` 的兼容策略

推荐分两步：

1. 先将上述生产调用方全部迁移到 `forEachPage`。
2. 全项目搜索确认没有生产代码继续依赖 `findAll` 后，再从 `DeviceAlarmQueryService` 移除 `findAll`。

如果当前测试仍需要验证跨页合并行为，可以把测试改为验证 `forEachPage` 的逐页回调；不建议继续保留一个容易被误用的全量列表接口。若为了短期兼容必须保留，则应在 JavaDoc 中明确标记为过渡方法，并禁止统计服务调用。

## 3. 各调用方迁移方式

### 3.1 `DeviceAlarmQueryServiceImpl.findActiveAlarmDeviceIds`

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmQueryServiceImpl.java`

保留唯一结果集合：

```java
Set<UUID> deviceIds = new HashSet<>();
forEachPage(user, query, page -> {
    for (AlarmInfo alarm : page) {
        if (alarm.getOriginator() != null
                && alarm.getOriginator().getEntityType() == EntityType.DEVICE) {
            deviceIds.add(alarm.getOriginator().getId());
        }
    }
});
```

实际实现可改为显式的内部处理方法，以减少 lambda；关键点是只保留去重后的设备 ID。

### 3.2 `DeviceAlarmSummaryServiceImpl.countActiveAlarms`

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmSummaryServiceImpl.java`

保留 `count` 和 `countsByType`，每页处理后立即丢弃 `AlarmInfo`：

- 过滤非设备来源和不可见设备；
- `count` 递增；
- 按规范化告警类型累加；
- 不保存告警对象、不保存全量告警列表。

### 3.3 `DeviceAlarmAnalysisServiceImpl`

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmAnalysisServiceImpl.java`

以下方法统一改为逐页处理：

- `getLeafAssetCounts`：只保留父资产计数器和设备到直属父资产的映射；
- `getTypeRanking`：只保留 `Map<String, Integer>`；
- `getSeverityRanking`：只保留 `EnumMap<AlarmSeverity, Integer>`；
- `collectOverviewData`：只保留状态计数、告警设备 ID 集合和未清除告警设备 ID 集合；
- `collectAlarmTrend`：只保留趋势时间桶；
- `collectActiveCriticalAlarms`：只保留按设备聚合的重点设备累加器。

这些方法的业务筛选条件保持原样，尤其是：

- 仍按 `alarm.startTs` 判断周期；
- 仍过滤 `EntityType.DEVICE`；
- 仍按设备 ID 去重；
- 仍保持告警类型、严重程度和处理率的原有排序。

### 3.4 `SmartLightningDashboardServiceImpl.collectAlarmRecords`

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java`

将 `for (AlarmInfo alarm : alarmQueryService.findAll(...))` 改为逐页处理。`AlarmRecordAggregate` 继续只保存：

- 当前周期告警数量；
- 告警设备 ID 集合；
- 按类型累计的 Map。

地网、SPD、雷电流三个模块共享的聚合结构和返回 DTO 不变。

## 4. 查询窗口与数据库压力控制

本次优化不是单纯把 `List` 换成另一个集合，还要控制查询范围：

1. 有明确日/周/月窗口的接口，优先把 `startTime/endTime` 传入 `TimePageLink`。
2. 由于原生查询按 `createdTime`，而业务可能按 `startTs`，仍需保留现有“查询候选 + 用 `startTs` 精确判断”的逻辑。
3. 没有时间范围的 ACTIVE 状态统计，只读取 ACTIVE 告警，但仍需分页遍历；不能为了省内存偷偷截断结果。
4. 查询只需要设备来源时，后续可在 DAO 层增加按 `originator_type` 的过滤；本轮先不改 DAO，避免改变 ThingsBoard 原生权限行为。
5. 单页大小从 10,000 降到 500，减少单次数据库返回和 JVM 瞬时对象数量；分页次数增加属于可接受的 CPU/IO 换内存。

## 5. 测试与验收标准

新增或调整以下测试，测试代码只验证逐页消费和业务结果，不依赖真实数据库：

### 5.1 查询服务测试

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/test/java/org/thingsboard/server/service/alarms/DeviceAlarmQueryServiceTest.java`

验收点：

- 两页告警会触发两次 `findPage`；
- visitor 每次只收到当前页；
- 所有页面都处理后才结束；
- `hasNext=false` 时不发起额外查询；
- query 或 pageLink 为空时不查询。

### 5.2 聚合调用方测试

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/test/java/org/thingsboard/server/service/alarms/DeviceAlarmAnalysisServiceTest.java`

验收点：

- 两页数据的类型、严重程度、状态、趋势数量与原全量列表算法一致；
- 同一设备多条告警仍按设备集合去重；
- 重点设备只保留必要累加结果；
- 空页和跨页边界不会丢数据；
- 客户用户和租户管理员的权限分支未改变。

### 5.3 静态验收

- 全项目搜索后，生产代码不再调用 `alarmQueryService.findAll`。
- 统计实现中不再出现 `List<AlarmInfo> alarms` 这种全量累积集合。
- 页面列表接口仍调用 `findPage`，分页、排序和筛选行为不变。
- 不执行 Maven 或服务重启；后续获得明确授权后再按项目既有方式运行聚焦测试。

## 6. 风险与回滚

### 风险

- 分页大小降低后，数据库查询次数增加，响应时间可能略有变化；应通过日志或监控观察页数和耗时。
- 某些旧测试可能直接 mock `findAll`，迁移后需要改为 mock `findPage` 或逐页 visitor。
- 统计结果若依赖全量列表的排序副作用，迁移时必须明确在聚合完成后重新排序，不能依赖输入列表顺序。

### 回滚

- 保留 `findPage` 不变，因此可以单独回滚逐页消费实现和调用方迁移。
- 不涉及数据库表、规则链、前端路由和 DTO 字段，不需要数据迁移。
- 若出现统计差异，优先对比同一查询条件下各页数量、`startTs` 过滤数量和最终聚合 Map，不直接恢复全量内存加载。

## 7. 实施顺序

- [x] 在 `DeviceAlarmQueryService` 定义逐页访问接口及中文职责注释。
- [x] 在 `DeviceAlarmQueryServiceImpl` 实现分页循环，单页大小限制为 500，不累积全量列表。
- [x] 迁移 `findActiveAlarmDeviceIds` 和 `DeviceAlarmSummaryServiceImpl`。
- [x] 迁移 `DeviceAlarmAnalysisServiceImpl` 的六个聚合入口。
- [x] 迁移 `SmartLightningDashboardServiceImpl.collectAlarmRecords`。
- [x] 更新 `DeviceAlarmQueryServiceTest` 和 `DeviceAlarmAnalysisServiceTest` 的分页测试。
- [x] 全项目静态搜索确认生产代码不再调用 `findAll`，并移除告警查询接口中的全量方法。
- [ ] 在用户本地执行聚焦验证；本轮不执行 Maven。

## 8. 其他已确认的必要优化

以下问题与“全量告警加载内存”不同，但会在设备数或告警量增大后造成明显的数据库压力、线程压力或模块维护成本，因此一并纳入本方案。

### 8.1 消除资产树关系查询的 N+1

当前文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningMonitoringTreeServiceImpl.java`

当前 `getTree` 对每个可见资产单独执行一次 `relationService.findByFrom`。优化顺序：

1. 在 `RelationDao` 增加按租户、关系组和多个 `from_id` 一次查询的批量方法。
2. 在 `BaseRelationService`/`RelationService` 暴露同名批量服务方法，保持现有单实体方法兼容。
3. `SmartLightningMonitoringTreeServiceImpl.getTree` 一次传入所有可见资产 ID，再按 `from_id` 在内存中分组。
4. `SmartLightningMonitoringTreeBuilder` 继续负责树构建和权限端点过滤，不把 DAO 结果格式泄漏到前端 DTO。

验收标准：资产数量为 N 时，关系数据库查询从 N 次降为 1 次；树节点、设备挂载、告警标记和绝对路径结果与原逻辑一致。

涉及文件：

- `E:/CNBProject/thingsboard-pe2ce/dao/src/main/java/org/thingsboard/server/dao/relation/RelationDao.java`
- `E:/CNBProject/thingsboard-pe2ce/dao/src/main/java/org/thingsboard/server/dao/relation/BaseRelationService.java`
- `E:/CNBProject/thingsboard-pe2ce/dao/src/main/java/org/thingsboard/server/dao/sql/relation/JpaRelationDao.java`
- `E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningMonitoringTreeServiceImpl.java`

### 8.2 限制遥测请求并发，不一次创建数千个 Future

涉及文件：

- `E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningMonitoringServiceImpl.java`
- `E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java`

当前实现为每台设备创建一个遥测 Future，再统一 `Futures.allAsList`。改为固定批次处理：

1. 设备 ID 按 50 台一批切分。
2. 每批最多创建 50 个遥测请求。
3. 等待当前批次完成后再处理下一批；地网和雷电流在批次内立即聚合，SPD 因同一批记录需要被多个现有指标复用，暂时只限制 Future 并发并保留原有结果结构。
4. 批次大小作为模块常量，后续可按数据库和遥测服务吞吐量调整。
5. 单台请求失败只记录该设备失败并使用现有空值回退，不让整批结果丢失。

这样不会改变结果 DTO，只降低瞬时线程、Future 和连接占用。若 `TimeseriesService` 后续提供批量 latest 接口，再把批次内部请求替换为批量调用，外层聚合代码不变。

### 8.3 统一设备 `active` 回退规则

涉及文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDeviceServiceImpl.java`

当前列表过滤在缺少服务器属性 `active` 时按离线处理，而设备上下文转换会回退到 `Device.isActive()`，造成列表、卡片和汇总口径不一致。

统一为一个私有方法：

```java
private boolean resolveActive(Map<String, String> attributes, Device device) {
    String active = attributes.get("active");
    if (active != null) {
        return "true".equalsIgnoreCase(active) || "1".equals(active);
    }
    return device != null && device.isActive();
}
```

所有设备列表、上下文和状态汇总都调用该方法。服务器属性存在时以属性为准；属性缺失时使用平台设备状态，保持现有页面可用。

### 8.4 消除 `getLightningDeviceIds` 的重复全量上下文查询

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDeviceServiceImpl.java`

当前 `getLightningDeviceIds` 分页调用 `getLightningDeviceContexts`，后者每页重新读取设备属性和设备配置；设备页数越多，重复读取越多。

改为：

1. 一次调用已有的 `getAllLightningDeviceContexts`。
2. 遍历上下文提取非空 `deviceId` 到 `Set<UUID>`。
3. 删除分页方法内部的二次属性读取。

该方法返回类型和防雷分类规则不变，复杂度从“多页重复批量读取”降为“一次批量读取后遍历”。

### 8.5 拆除 `monitoring` 与 `alarms` 的反向包依赖

当前告警实现直接引用：

`org.thingsboard.server.service.monitoring.util.MonitoringQueryDefaults`

而监测实现又引用告警服务，形成双向模块依赖。建议新增中性配置类：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/query/util/ServiceQueryDefaults.java`

迁移步骤：

1. 将通用分页默认值移动到 `ServiceQueryDefaults`。
2. 告警和监测分别静态导入该中性类。
3. 删除 `MonitoringQueryDefaults` 中被两个模块共享的常量。
4. 业务分类常量仍留在各自模块，不把告警逻辑放入监测工具包。

目标是依赖方向变为：`monitoring -> query.util`、`alarms -> query.util`，不再互相引用。

### 8.6 清理树关系排序前的空端点风险

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningMonitoringTreeBuilder.java`

关系排序或比较前先过滤 `relation.getFrom()`、`relation.getTo()` 和 ID 为空的脏关系，再执行名称/ID 排序。过滤逻辑放在 `build` 的关系预处理阶段，`addTreeRelation` 继续只处理已验证的关系。

验收标准：空端点关系被忽略，正常关系排序和树结构不变，不产生新的空指针。

### 8.7 明确告警周期时间口径

文件：

`E:/CNBProject/thingsboard-pe2ce/application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java`

告警查询底层分页按 `createdTime`，页面业务统计按 `AlarmInfo.startTs`。保留两层口径，但写清楚实现约束：

1. `TimePageLink` 只作为候选范围，避免扫描无限历史。
2. 业务层继续以 `startTs` 做最终日/周/月分桶。
3. 查询范围要覆盖可能延迟入库的告警，不能用过窄的 `createdTime` 窗口漏掉触发时间落入周期内的告警。
4. 在测试中加入“createdTime 晚于周期、startTs 落入周期”的边界样例。

这项只修正时间口径说明和边界测试，不改变前端日期筛选名称。

## 9. 不纳入本轮强制改造的争议项

以下内容在当前上下文中存在口径争议，本方案不直接修改：

- 资产树是否必须跨页读取：当前实现使用 10,000 的统一查询上限，是否需要跨页取决于用户实际资产规模；在确认数据规模和接口上限前不改变行为。
- `AlarmInfo.getStatus()` 是否允许为空：如果平台数据模型保证状态非空，则不把它当成当前缺陷；仅保留已有状态统计逻辑。

## 10. 扩展后的实施顺序

- [x] 先实现告警逐页消费，迁移所有 `findAll` 生产调用方。
- [x] 为告警分页聚合补充跨页结果一致性测试。
- [x] 统一 `active` 回退规则，并修复 `getLightningDeviceIds` 的重复上下文查询。
- [x] 清理树关系空端点，并实现批量关系查询，消除资产树 N+1。
- [x] 将遥测 Future 改为固定批次，并对单设备失败使用空结果回退。
- [x] 把分页常量移到中性 query 工具包，解除 `monitoring`/`alarms` 双向依赖。
- [x] 补充告警时间口径说明、startTs 二次校验和各模块静态引用检查。
- [ ] 用户本地执行聚焦验证；本轮不执行 Maven。
