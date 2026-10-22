# 智能防雷与告警页面实时订阅 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 复用 ThingsBoard 现有 WebSocket 封装，为智能防雷与告警页面补齐设备状态、实时遥测和活动告警的自动刷新。

**Architecture:** 新增根级 `SmartLightningRealtimeService`，维护一套全设备 `EntityDataCmd`、活动告警 `AlarmCountCmd` 与活动告警字段 `AlarmDataCmd` 订阅，并以引用计数控制生命周期。页面只接收变更通知，再调用现有 HTTP 聚合接口，保持原有权限过滤、资产树逻辑、排序和 DTO 不变。

**Tech Stack:** Angular 20、RxJS 7、ThingsBoard `TelemetryWebsocketService`、`TelemetrySubscriber`、`EntityDataCmd`、`AlarmCountCmd`。

**Spec:** `docs/superpowers/specs/2026-09-15-smart-lightning-realtime-design.md`

## Global Constraints

- 不新增后端接口。
- 不修改 HTTP 返回结构、资产过滤、告警排序和历史曲线逻辑。
- 不运行 Maven；只进行前端源码检查和可执行的定向验证。
- 所有新增和修改代码使用中文注释说明职责与刷新边界。
- 保留当前工作区已有修改，不回滚、不覆盖无关文件。

---

### Task 1: 共享实时订阅服务

**Files:**
- Create: `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service.ts`
- Test: `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service.spec.ts`

**Interfaces:**
- `acquire(): void` / `release(): void`：页面进入/离开时管理共享订阅。
- `deviceChanged$: Observable<ReadonlySet<string>>`：设备实体或实时遥测发生变化时发出设备 ID 集合。
- `alarmChanged$: Observable<SmartLightningAlarmRealtimeChange>`：活动告警数量或告警状态、派发、确认、清除、备注字段变化时发出失效通知。

- [x] **Step 1: 写共享服务失败测试**
  覆盖引用计数只建立一套订阅、最后一个消费者释放后取消订阅、设备更新能提取设备 ID 三个行为。
- [x] **Step 2: 运行定向测试确认失败**
  使用项目已有 Angular 测试命令执行该 spec，失败原因应为服务文件或方法尚未实现。
- [x] **Step 3: 实现服务**
  复制当前数据监测页已经验证过的订阅命令，统一订阅 `active` 与规范遥测键；增加 `AlarmCountCmd` 和 `AlarmDataCmd`，分别覆盖活动告警数量及告警字段状态变化；将 `EntityDataUpdate` 的 `data.data` 与 `update` 中的 `entityId.id` 提取后发出。
- [ ] **Step 4: 运行定向测试确认通过**（项目未配置 Angular test target，`ng test` 无法确定项目/目标）
- [x] **Step 5: 保留中文职责注释并检查导入路径**

### Task 2: 看板和数据监测统一使用共享订阅

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring.component.ts`

**Interfaces:**
- 看板设备变化触发 `loadDeviceSummary()`、三个实时汇总加载方法；告警变化触发告警列表和告警统计加载。
- 数据监测移除本地重复的两个 `TelemetrySubscriber`，改为监听共享服务。

- [x] **Step 1: 修改前确认现有私有订阅逻辑位置**
- [x] **Step 2: 替换为 `acquire/release` 和变更流订阅**
- [x] **Step 3: 保留周期切换的主动 HTTP 请求，不让 WebSocket 改变周期状态**
- [x] **Step 4: 静态检查重复连接字段和未使用导入**

### Task 3: 设备详情和数据查询接入

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-device-detail.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/query/smart-lightning-data-query.component.ts`

**Interfaces:**
- 详情页只在推送设备 ID 包含当前设备，或服务发出空集合表示全量刷新时，重读实时指标和设备告警；历史曲线不刷新。
- 数据查询页在设备或告警变化时重读设备列表与活动告警标记。

- [x] **Step 1: 确认现有设备详情与数据查询数据映射测试不需要改动**
- [x] **Step 2: 注入共享服务并建立销毁时释放逻辑**
- [x] **Step 3: 使用 `auditTime(600)` 合并设备变化通知**
- [x] **Step 4: 静态检查路由切换和当前设备 ID过滤**

### Task 4: 告警列表、派发、处理和分析接入

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/deviceAlarms/list/smart-lightning-alarm-list.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/deviceAlarms/analysis/smart-lightning-alarm-analysis.component.ts`

**Interfaces:**
- 告警列表组件的三个路由模式共享同一订阅；告警变化重新加载告警行，设备变化只刷新设备上下文和必要的树标记。
- 告警分析告警变化后重新加载统计、站点、类型、严重程度、趋势和重点设备；周期切换逻辑保持不变。

- [x] **Step 1: 注入共享服务并在 `ngOnInit` 获取订阅引用**
- [x] **Step 2: 对告警刷新使用 `auditTime(600)`，避免操作或批量告警造成请求风暴**
- [x] **Step 3: 页面销毁释放订阅**
- [x] **Step 4: 静态检查所有路由模式仍共享原有按钮和筛选行为**

### Task 5: 验证与交付

**Files:**
- Verify only; no additional business files.

- [x] **Step 1: 运行 `git diff --check`**
- [x] **Step 2: 使用 Angular development build 验证新增服务和修改文件无明显类型错误**
- [x] **Step 3: 检查目标目录中每个页面只引用共享订阅，不存在残留重复全量订阅**
- [x] **Step 4: 汇总实际修改文件、未运行 Maven 的事实和剩余风险**
