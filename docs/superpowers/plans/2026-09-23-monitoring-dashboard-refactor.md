# Smart Lightning Monitoring Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Extract the three independent dashboard panels into focused handlers and make monitoring device context/count handling safe at existing DTO boundaries.

**Architecture:** Keep `SmartLightningDashboardServiceImpl` as the one Spring service and facade. Move Ground, SPD, and Lightning Current into package-private handler files and move only their shared query helpers into `SmartLightningDashboardSupport`; do not create more beans. Keep all public signatures and DTO fields unchanged.

**Tech Stack:** Java 25, Spring, Lombok, ThingsBoard service APIs; no new dependency.

**Spec:** `docs/superpowers/specs/2026-09-23-monitoring-device-log-alarms-refactor-design.md`

## Global Constraints

- 不改 Controller 路由、公开 Service 方法签名、DTO/VO 结构、权限入口、周期定义、排序口径或分页格式。
- 故障电弧相关服务逻辑、DTO/VO、键名、遥测映射、计数含义和字段注释；在用户确认标准遥测键以前全部保持原样。
- 新写业务注释使用中文。
- 当前仓库有大量用户未提交修改。实施必须基于当前工作树逐文件核对，仅修改本计划列出的自定义实现及必要的新处理器文件；不能重置、覆盖或顺手提交其他已有修改。
- 项目 `AGENTS.md` 禁止未经当前明确授权运行 Maven 及其生命周期/测试命令。本轮按源码静态检查和差异审查验证；不运行 Maven、测试或构建。

## Review Focus

- 设备没有服务器属性映射时，仍按现有 productCategory 规则处理，且构造上下文不发生空指针。
- 坐标为 NaN、Infinity、非数字或越界值时仍返回 null。
- 单设备读取 Future 失败仍作为该设备空遥测处理；线程中断时恢复中断标记并沿用现有异常映射。
- 多类告警总数超过 int 上限时，中间累加保持 long，现有 int DTO 边界按饱和规则输出。
- 三个服务入口、查询键、窗口及 DTO 字段不因处理器提取而变化；故障电弧相关文件不进入差异。

---

### Task 1: 提取共享看板支持逻辑和地网处理器

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardSupport.java`
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/GroundSummaryHandler.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java`

**Interfaces:**
- Consumes: 当前服务中的 `readTelemetryInBatches`, `collectAlarmRecords`, `findVisibleDevicesByFunctionCategory` 和 `AlarmRecordAggregate`。
- Produces: 同包共享静态方法 `readTelemetryInBatches(TimeseriesService, SecurityUser, List<MonitoringDeviceContextBO>, List<ReadTsKvQuery>, String) throws ThingsboardException`, `collectAlarmRecords(SecurityUser, Set<UUID>, MonitoringPeriodWindow, AlarmStatisticsService)` 和 `findVisibleDevicesByFunctionCategory(SmartLightningDeviceService, SecurityUser, String) throws ThingsboardException`；同包结果类型 `AlarmRecordAggregate` 暴露 `long count`、`int alarmDeviceCount` 和 `List<AlarmTypeTotalDTO> typeTotals`；`GroundSummaryHandler(SmartLightningDeviceService, AlarmStatisticsService, TimeseriesService)`。

- [x] **Step 1: 建立支持类并迁移公共逻辑**

将批量时序读取、按功能分类筛选、告警聚合和聚合结果类型原样迁入 package-private `SmartLightningDashboardSupport`。支持类只暴露给同包 handlers 使用的静态方法；不要引入 Spring 注解或复制方法实现。

共享告警聚合保留 `long` 的总告警数，按现有 `AlarmTypeTotalDTO` 的 `int` 字段对每条类型计数调用 `CountValueSupport.toIntCount`。告警设备数仍在 DTO 既有 `int` 边界转换。handler 创建地网、SPD、雷电流 DTO 时，仅将 `AlarmRecordAggregate.count` 转成 DTO 所需的 `int`。

```java
static AlarmRecordAggregate collectAlarmRecords(
        SecurityUser user, Set<UUID> deviceIds, MonitoringPeriodWindow window,
        AlarmStatisticsService alarmStatisticsService) {
    AlarmRecordAggregate result = AlarmRecordAggregate.empty();
    if (deviceIds.isEmpty()) {
        return result;
    }
    List<AlarmDimensionCountBO> rows = alarmStatisticsService.countByType(
            user, deviceIds, window.getStartTs(), window.getEndTs(), false);
    for (AlarmDimensionCountBO row : rows) {
        result.count += row.getCount();
        result.typeTotals.add(new AlarmTypeTotalDTO(
                row.getDimension(), CountValueSupport.toIntCount(row.getCount())));
    }
    AlarmOverviewAggregateBO overview = alarmStatisticsService.getOverview(
            user, deviceIds, window.getStartTs(), window.getEndTs());
    result.alarmDeviceCount = CountValueSupport.toIntCount(overview.getAlarmedDeviceCount());
    return result;
}
```

- [x] **Step 2: 提取 GroundSummaryHandler 并连接 facade**

将现有 Ground handler 的常量、查询、阻值聚合和 DTO 构造完整移入新文件。改为调用 `SmartLightningDashboardSupport` 的公共静态方法。将 `SmartLightningDashboardServiceImpl.getGroundSummary` 转发到新 handler，并删除已迁移的嵌套 Ground 类。

```java
@RequiredArgsConstructor
final class GroundSummaryHandler {
    private final SmartLightningDeviceService smartLightningDeviceService;
    private final AlarmStatisticsService alarmStatisticsService;
    private final TimeseriesService timeseriesService;
}
```

用 `CountValueSupport.toIntCount(aggregate.count)` 在构造现有 DTO 时完成总数边界转换，不改 DTO 定义。

- [x] **Step 3: 静态核对本任务**

Run: `git diff --check -- application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardSupport.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/GroundSummaryHandler.java`
Expected: 无空白错误；Ground 方法在 facade 中只委托一次；批量读取和告警聚合实现各只有一份。

### Task 2: 提取 SPD 看板处理器

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SpdSummaryHandler.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java`

**Interfaces:**
- Consumes: Task 1 的 `SmartLightningDashboardSupport` 静态方法和既有三项服务依赖。
- Produces: 同包 `SpdSummaryHandler(SmartLightningDeviceService, AlarmStatisticsService, TimeseriesService)`；facade 中 `getSpdSummary` 转发到该 handler。

- [x] **Step 1: 迁移 SPD handler 全部职责**

将 SPD 常量、遥测查询构造、状态解析、最大漏电流和极值计算、周期浪涌计数及 DTO 映射迁入独立文件。继续调用共享支持逻辑处理设备范围、分批读取及告警统计。SPD 保持一个完整 handler，不按指标继续拆类。

构造现有 SPD DTO 时，以 `CountValueSupport.toIntCount(alarmAggregate.count)` 进入现有 `int` 告警数字段。

- [x] **Step 2: 更新 facade 并删除原嵌套实现**

把当前私有嵌套 `SpdSummaryHandler` 删除，保留 `getSpdSummary(SecurityUser, String)` 的公开签名和调用结果。

- [x] **Step 3: 静态核对本任务**

Run: `git diff --check -- application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SpdSummaryHandler.java`
Expected: 无空白错误；SPD 查询键和查询窗口与迁移前逐项一致；facade 只负责委托。

### Task 3: 提取雷电流看板处理器

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/LightningCurrentSummaryHandler.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java`

**Interfaces:**
- Consumes: Task 1 的 `SmartLightningDashboardSupport` 静态方法和既有三项服务依赖。
- Produces: 同包 `LightningCurrentSummaryHandler(SmartLightningDeviceService, AlarmStatisticsService, TimeseriesService)`；facade 中 `getLightningCurrentSummary` 转发到该 handler。

- [x] **Step 1: 迁移雷电流板块职责**

将当前 Lightning Current handler 的设备过滤、周期遥测查询、峰值范围、浪涌增量和极性归类完整迁入独立文件。共享设备查询、批量时序和告警统计统一调用 `SmartLightningDashboardSupport`。

构造现有雷电流 DTO 时，以 `CountValueSupport.toIntCount(alarmAggregate.count)` 进入现有 `int` 告警数字段；`eventCount` 等原有 long 字段保持 long。

- [x] **Step 2: 更新 facade 并删除原嵌套实现**

保留 `getLightningCurrentSummary(SecurityUser, String)` 对外签名，移除原私有嵌套实现。不要修改 `SmartLightningMonitoringServiceImpl` 或任何故障电弧文件。

- [x] **Step 3: 静态核对本任务**

Run: `git diff --check -- application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/LightningCurrentSummaryHandler.java`
Expected: 无空白错误；雷电流窗口、遥测键和 DTO 参数顺序保持一致；三个处理器都为同包普通类，服务仍只有原有一个 Spring bean。

### Task 4: 修正设备上下文空属性和坐标解析

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDeviceServiceImpl.java`

**Interfaces:**
- Consumes: 现有 `MonitoringValueParser.parseFiniteDouble(String)` 和 productCategory 匹配逻辑。
- Produces: 无公开接口变化；缺失属性映射按空映射读取，坐标依旧受经纬度范围校验。

- [x] **Step 1: 让上下文转换接受缺失属性**

在 `toDeviceContext` 内把 null 属性映射正规化为 `Map.of()`，后续属性读取统一使用正规化映射。不要把“缺少 lightning 分类”误改为通过防雷设备过滤；`matchesProductCategory` 的既有规则保持原样。

- [x] **Step 2: 复用有限数值解析**

以 `MonitoringValueParser.parseFiniteDouble(value)` 取代重复的 parse/catch 实现；仅在值非 null 且位于参数 `min` 与 `max` 的闭区间时返回坐标，否则返回 null。

```java
Double coordinate = MonitoringValueParser.parseFiniteDouble(value);
return coordinate != null && coordinate >= min && coordinate <= max ? coordinate : null;
```

- [x] **Step 3: 静态核对本任务和整体结构**

Run: `git diff --check -- application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDeviceServiceImpl.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SmartLightningDashboardSupport.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/GroundSummaryHandler.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SpdSummaryHandler.java application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/LightningCurrentSummaryHandler.java`
Expected: 无空白错误；`rg` 检查确认原 facade 不再声明私有嵌套 handlers，共享 helper 只留在支持类，Arc 服务/DTO/VO 不在本次变更文件清单。
