# 设备日志页面实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新增完全自定义的设备日志页面，主页面只显示设备名称、设备配置类型和操作，点击后从右侧打开服务器属性、最新遥测和事件时间轴。

**Architecture:** 前端新建 `pages/log` 模块，后端新建 `service/deviceLog` 模块。前端只调用自定义 API Service；后端聚合平台已有设备、属性、遥测、告警和审计能力，并执行当前用户可见范围校验。原生设备页面、原生审计组件和原生 Controller 不修改。

**Tech Stack:** Angular NgModule、HttpClient/RxJS、ThingsBoard Spring MVC、现有 DeviceService、AttributesService、TimeseriesService、AlarmService、AuditLogService。

**Spec:** `docs/superpowers/specs/2026-09-17-device-log-design.md`

## Global Constraints

- 前端目录必须是 `ui-ngx/src/app/modules/home/pages/log`，与 `deviceAlarms` 平级。
- 只新增自定义设备日志代码，不修改 ThingsBoard 原生设备页面、审计组件和原生数据结构。
- Controller 只负责路由、权限入口和 DTO→VO 转换；业务聚合放入 Service/Impl。
- 普通遥测上报不自动生成时间轴事件；最新遥测只展示当前快照。
- 所有设备、属性、遥测、告警和事件必须按当前用户可见范围过滤。
- 未经用户当前请求明确授权，不运行 Maven。

---

### Task 1: 后端契约与转换对象

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/dto/DeviceLogDeviceDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/dto/DeviceLogDeviceDetailDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/dto/DeviceLogServerAttributeDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/dto/DeviceLogLatestTelemetryDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/dto/DeviceLogEventDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/bo/DeviceLogEventBO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/vo/DeviceLogDeviceVO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/vo/DeviceLogDeviceDetailVO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/vo/DeviceLogEventVO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/service/DeviceLogService.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/converter/DeviceLogVOConverter.java`
- Test: `application/src/test/java/org/thingsboard/server/service/deviceLog/DeviceLogVOConverterTest.java`

**Interfaces:**
- `findDevices(PageLink pageLink)` returns `PageData<DeviceLogDeviceDTO>`。
- `getDeviceDetail(DeviceId deviceId)` returns `DeviceLogDeviceDetailDTO`。
- `findEvents(DeviceId deviceId, TimePageLink pageLink)` returns `PageData<DeviceLogEventDTO>`。
- Converter 只做 DTO→VO 映射，不做查询和业务判断。

- [ ] 定义 DTO、BO、VO 和中文职责注释。
- [ ] 编写转换器测试，覆盖嵌套详情和空集合。
- [ ] 做类型名、文件名和 import 静态检查。

### Task 2: 后端聚合实现与自定义 Controller

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/service/impl/DeviceLogServiceImpl.java`
- Create: `application/src/main/java/org/thingsboard/server/service/deviceLog/util/DeviceLogEventMapper.java`
- Create: `application/src/main/java/org/thingsboard/server/controller/DeviceLogController.java`
- Test: `application/src/test/java/org/thingsboard/server/service/deviceLog/DeviceLogEventMapperTest.java`

**Interfaces:**
- `GET /api/device-log/devices?pageSize=&page=&textSearch=`
- `GET /api/device-log/devices/{deviceId}`
- `GET /api/device-log/devices/{deviceId}/events?pageSize=&page=&startTime=&endTime=`

- [ ] 复用平台服务读取可见设备、SERVER_SCOPE 属性和最新遥测。
- [ ] 聚合告警/审计事件为统一时间轴事件并按时间倒序分页。
- [ ] 对空状态、空来源和无历史来源进行安全处理。
- [ ] 只新增自定义 Controller，不修改原生 Controller。

### Task 3: 新建前端 `log` 模块和设备列表

**Files:**
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log.module.ts`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-routing.module.ts`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log.models.ts`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-api.service.ts`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-list.component.ts`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-list.component.html`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-list.component.scss`
- Modify: `ui-ngx/src/app/modules/home/pages/home-pages.module.ts`

**Interfaces:**
- Route: `/device-log`。
- 主表只包含设备名称、设备配置类型、操作三列。
- 操作按钮打开右侧自定义详情抽屉。

- [ ] 注册自定义模块和路由，不改原生设备路由。
- [ ] 实现筛选、重置、加载、空数据、错误和分页状态。
- [ ] 长设备名称和配置类型使用悬浮提示。

### Task 4: 新建右侧抽屉和三个页签

**Files:**
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-drawer.component.ts/html/scss`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-server-attributes.component.ts/html/scss`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-latest-telemetry.component.ts/html/scss`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log-event-timeline.component.ts/html/scss`

- [ ] 完全自定义实现抽屉，不导入原生设备详情组件或 `tb-audit-log-table`。
- [ ] 实现“服务器属性”“最新遥测”“事件时间轴”三个页签。
- [ ] 时间轴显示时间、分类、级别、标题、说明、来源和结果。
- [ ] 支持键盘关闭、焦点状态、加载更多和错误提示。

### Task 5: 设备日志实时增量和验证

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/log/device-log-api.service.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/log/device-log-drawer.component.ts`
- Create: `ui-ngx/src/app/modules/home/pages/log/device-log.models.spec.ts`

- [ ] 只订阅当前抽屉选中设备的增量事件。
- [ ] 关闭抽屉或切换设备时取消旧订阅。
- [ ] 按事件 ID 去重并将新事件插入时间轴顶部。
- [ ] 做前端静态检查、diff check 和原生文件范围检查。
- [ ] 不运行 Maven，最终如实报告未做后端构建验证。
