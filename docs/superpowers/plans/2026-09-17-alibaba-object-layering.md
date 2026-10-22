# 阿里对象分层规范改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将自定义 `monitoring`、`alarms` 后端模块的数据对象严格区分为 VO、BO 与 Repository Projection，同时保持 HTTP JSON 契约和业务逻辑不变。

**Architecture:** Controller 对外展示对象统一放入 `vo` 并使用 `VO` 后缀；Service 内部业务上下文及聚合结果放入 `bo` 并使用 `BO` 后缀；JPA 查询接口继续使用 `repository` 下的 Projection。页面级 Service 作为应用服务可直接返回 VO，Controller 不增加字段拼装或业务判断。

**Tech Stack:** Java、Spring MVC、Spring Service、Spring Data JPA、Lombok、JUnit。

**Spec:** 当前用户要求“严格按照阿里规范来”。

## Global Constraints

- 只调整自定义 `monitoring`、`alarms` 模块及其直接引用。
- 不修改 HTTP 路径、JSON 字段、统计口径与查询逻辑。
- 不运行或触碰 Maven。
- 所有源码修改后只执行静态引用和格式检查。

---

### Task 1: 展示对象统一为 VO

**Files:**
- Move: `application/src/main/java/org/thingsboard/server/service/monitoring/dto/*.java`
- Move: `application/src/main/java/org/thingsboard/server/service/alarms/dto/*.java`
- Modify: 两个模块的 Controller、Service、实现类和测试引用

**Interfaces:**
- Consumes: 现有 HTTP 返回类型和构造参数。
- Produces: `monitoring.vo.*VO`、`alarms.vo.*VO`，JSON 字段保持不变。

- [ ] 将所有直接或嵌套返回给前端的对象移动到 `vo` 包。
- [ ] 将类型名统一为 `VO` 后缀并同步构造器、泛型和方法签名。
- [ ] 静态扫描旧 `dto` 包及 `DTO` 类型名残留。

### Task 2: 内部业务对象统一为 BO

**Files:**
- Move: `application/src/main/java/org/thingsboard/server/service/monitoring/model/MonitoringDeviceContext.java`
- Move: `application/src/main/java/org/thingsboard/server/service/alarms/model/*.java`
- Modify: Service、实现类、过滤器和测试引用

**Interfaces:**
- Consumes: 当前内部查询与聚合对象。
- Produces: `MonitoringDeviceContextBO`、`AlarmDeviceCountBO`、`AlarmDimensionCountBO`、`AlarmOverviewAggregateBO`。

- [ ] 将内部业务对象移动到 `bo` 包并统一 `BO` 后缀。
- [ ] 保持字段、构造参数和统计逻辑不变。
- [ ] 静态扫描旧 `model` 类型残留。

### Task 3: 转换器职责规范化

**Files:**
- Move: `MonitoringDeviceMapper.java` to `converter/MonitoringDeviceVOConverter.java`
- Modify: `SmartLightningDataQueryServiceImpl.java`
- Modify: `MonitoringDeviceMapperTest.java`

**Interfaces:**
- Consumes: `MonitoringDeviceContextBO`。
- Produces: `SmartLightningDeviceContextVO`。

- [ ] 将 Mapper 改名为 VO Converter。
- [ ] 将 `toDto` 改为 `toVO`，保持逐字段映射。
- [ ] 更新映射测试，确保所有字段不丢失。

### Task 4: 静态验收

**Files:**
- Verify: `application/src/main/java/org/thingsboard/server/service/monitoring/**`
- Verify: `application/src/main/java/org/thingsboard/server/service/alarms/**`
- Verify: 直接引用上述类型的 Controller 与测试

**Interfaces:**
- Consumes: Tasks 1-3 的最终包结构。
- Produces: 无旧 DTO/Model 引用、无缺失文件、无差异格式错误的源码树。

- [ ] 检查 `dto` 和旧 `model` 包引用为 0。
- [ ] 检查所有 VO、BO 文件名与公开类名一致。
- [ ] 检查 Controller 只调用 Service，不新增业务逻辑。
- [ ] 执行 `git diff --check`，不运行 Maven。
