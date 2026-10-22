# Alarm Analysis Count Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Keep alarm analytics cohesive while preventing intermediate integer overflow and removing two confirmed redundancies.

**Architecture:** Do not split `DeviceAlarmAnalysisServiceImpl`. Accumulate database `long` counts as `long`, convert only where the existing DTO uses `int`, directly read the immutable tree-index mapping, and use one implementation for `AlarmPeriodWindow` accessors.

**Tech Stack:** Java 25, Spring, Lombok, existing `CountValueSupport`; no new dependency.

**Spec:** `docs/superpowers/specs/2026-09-23-monitoring-device-log-alarms-refactor-design.md`

## Global Constraints

- 不改公开 DTO 字段类型、API、数据库表或统计口径。
- 新写业务注释使用中文。
- 当前仓库有大量用户未提交修改。实施必须基于当前工作树逐文件核对，仅修改本计划列出的自定义实现；不能重置、覆盖或顺手提交其他已有修改。
- 项目 `AGENTS.md` 禁止未经当前明确授权运行 Maven 及其生命周期/测试命令。本轮按源码静态检查和差异审查验证；不运行 Maven、测试或构建。
- 故障电弧相关服务逻辑、DTO/VO、键名、遥测映射、计数含义和字段注释；在用户确认标准遥测键以前全部保持原样。

## Review Focus

- 单行数据库计数大于 `Integer.MAX_VALUE` 时，末级资产聚合不提前截断到 int。
- 多个告警类型合计超过 `Integer.MAX_VALUE` 时，active 总数不会发生 int 回绕，DTO 输出按既有饱和规则限制。
- 每种告警类型自身仍按现有 int DTO 边界转换，不改变维度名称或排序。
- `VisibleAssetTreeIndex.getParentAssetByDevice()` 被只读使用；移除复制后不对其写入。
- 删除手写 getter 后 Lombok 仍生成所有当前访问的 getter，类型、命名及返回值不变。

---

### Task 1: 修正计数累加并删除重复实现

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmAnalysisServiceImpl.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmSummaryServiceImpl.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/alarms/util/AlarmPeriodWindow.java`

**Interfaces:**
- Consumes: `AlarmDeviceCountBO` / `AlarmDimensionCountBO` long getters、`CountValueSupport.toIntCount(long)`、不可变 `VisibleAssetTreeIndex` parent index。
- Produces: 不产生新公开类型或接口；所有现有 service/DTO/getter 方法签名保持不变。

- [x] **Step 1: 保持末级资产告警统计为 long**

在 `getLeafAssetCounts` 中直接累加 `deviceCount.getAlarmCount()` 和 `deviceCount.getHandledCount()` 到现有 long 计数器，移除累加前的 `CountValueSupport.toIntCount`。将 `new HashMap<>(treeIndex.getParentAssetByDevice())` 改为直接读取该索引映射；只查询，不写入。

```java
counter.alarmCount += deviceCount.getAlarmCount();
counter.handledCount += deviceCount.getHandledCount();
```

- [x] **Step 2: 将摘要总数的窄化延后到 DTO 边界**

在 `DeviceAlarmSummaryServiceImpl` 中用 `long activeCount` 累加原始 `row.getCount()`。每种类型仍使用 `CountValueSupport.toIntCount(row.getCount())` 创建现有类型 DTO；创建摘要 DTO 时仅对最终 active 总数调用一次 `CountValueSupport.toIntCount(activeCount)`。

```java
long activeCount = 0L;
for (AlarmDimensionCountBO row : rows) {
    activeCount += row.getCount();
    typeTotals.add(new DeviceAlarmTypeTotalDTO(
            row.getDimension(), CountValueSupport.toIntCount(row.getCount())));
}
return new DeviceAlarmSummaryDTO(CountValueSupport.toIntCount(activeCount), typeTotals);
```

- [x] **Step 3: 保留一种 AlarmPeriodWindow getter 实现**

保留 `@Getter`，删除类中与 Lombok 生成方法完全等价的手写 getter。不要改变 `final` 字段、构造逻辑、时区或窗口边界。

- [x] **Step 4: 静态核对本任务**

Run: `git diff --check -- application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmAnalysisServiceImpl.java application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmSummaryServiceImpl.java application/src/main/java/org/thingsboard/server/service/alarms/util/AlarmPeriodWindow.java`
Expected: 无空白错误；`rg` 确认末级资产中间累加不调用 `toIntCount`，摘要总数以 long 累加且仅在 DTO 边界转换，`AlarmPeriodWindow` 每个 getter 只有 Lombok 一份实现。检查现有调用点的 getter 名称均由字段名生成。

