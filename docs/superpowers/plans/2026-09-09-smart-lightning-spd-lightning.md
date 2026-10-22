# 智能防雷 SPD 与雷电监测实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 SPD 和雷电监测从 mock 图表改为真实聚合摘要卡片，并保持现有地网模块的布局、权限和独立周期筛选行为。

**Architecture:** 后端新增两个 DTO、两个 Service 接口和对应 `impl` 实现，复用设备权限、属性批量读取和告警查询；前端 API 服务暴露两个摘要请求，Dashboard 组件分别加载并渲染两个独立卡片。

**Tech Stack:** Spring MVC、ThingsBoard `TimeseriesService`/`AlarmService`、Angular、RxJS、SCSS。

**Spec:** `docs/superpowers/specs/2026-09-09-smart-lightning-spd-lightning-design.md`

## Global Constraints

- Controller 只负责 HTTP 参数和 Service 调用。
- 设备必须同时满足 `productCategory=lightning` 与目标 `functionCategory`。
- 日/周/月状态独立保存；设备总数、在线、离线为当前快照。
- 无遥测数据返回 null，前端显示 `--`，不把缺失转换成 0。
- 不修改 Maven、不运行 Maven 命令。

### Task 1: 添加摘要 DTO、Service 接口与服务端查询实现

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/dto/SpdMonitoringSummary.java`
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/dto/LightningCurrentMonitoringSummary.java`
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/SpdMonitoringSummaryService.java`
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/LightningCurrentMonitoringSummaryService.java`
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/SpdMonitoringSummaryServiceImpl.java`
- Create: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/LightningCurrentMonitoringSummaryServiceImpl.java`

**Interfaces:**
- `SpdMonitoringSummary getSummary(SecurityUser user, String period)`
- `LightningCurrentMonitoringSummary getSummary(SecurityUser user, String period)`

- [ ] **Step 1: Write focused pure frontend tests for null formatting and counter delta before changing the UI.**
- [ ] **Step 2: Implement DTOs with Lombok getters and explicit nullable metric fields.**
- [ ] **Step 3: Implement SPD aggregation using visible lightning contexts, alarm query, and latest leakage values.**
- [ ] **Step 4: Implement lightning aggregation using visible lightning contexts, alarm query, and telemetry samples; normalize M1AI kA and L2AI A to A before comparing peaks.**
- [ ] **Step 5: Run Java static inspection with `rg` and inspect imports; do not run Maven.**

### Task 2: Register monitoring endpoints and frontend API contracts

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/controller/SmartLightningMonitoringController.java`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring-api.service.ts`

**Interfaces:**
- `GET /api/smart-lightning/monitoring/spd-summary?period=day|week|month`
- `GET /api/smart-lightning/monitoring/lightning-current-summary?period=day|week|month`

- [ ] **Step 1: Add controller constructor dependencies and two delegating methods.**
- [ ] **Step 2: Add typed frontend response interfaces and API methods.**
- [ ] **Step 3: Verify route strings and DTO property names match exactly.**

### Task 3: Replace mock SPD and lightning cards in the dashboard

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.html`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.scss`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.data.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.data.spec.ts`

- [ ] **Step 1: Add independent `spdSummary` and `lightningSummary` state, loading, and error fields.**
- [ ] **Step 2: Load each summary on initialization and reload only its own period.**
- [ ] **Step 3: Replace chart hosts with summary markup while retaining the existing period switcher.**
- [ ] **Step 4: Add compact responsive styles and tooltip text for coverage/source details.**
- [ ] **Step 5: Remove only the unused SPD/lightning mock rendering paths after the cards are wired.**

### Task 4: Static verification and focused frontend tests

**Files:**
- Test: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-summary.data.spec.ts`
- Test: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.template.spec.ts`

- [ ] **Step 1: Run the focused data/template specs using the repository’s existing frontend test command without Maven.**
- [ ] **Step 2: Run `git diff --check`.**
- [ ] **Step 3: Inspect the final diff for accidental changes outside SPD/lightning implementation.**
- [ ] **Step 4: Report any unrun backend build separately; do not claim Maven compilation.**
