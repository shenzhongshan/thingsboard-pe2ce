# Monitoring 与设备告警后端结构重组实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将自定义 `monitoring` 后端按智能防雷页面职责整理，并把面向全部设备的告警逻辑迁移到平级 `alarms` 包，同时保持接口行为完全兼容。

**Architecture:** 控制器只负责 HTTP、权限和当前用户上下文；每个二级页面由一套页面 Service/Impl 组织；设备可见性、资产树、告警分页和纯计算继续作为公共组件。告警类型统一使用 `DeviceAlarm` 前缀，避免与 ThingsBoard 原生 `AlarmController` 冲突。

**Tech Stack:** Java、Spring MVC、Spring Security、Lombok、ThingsBoard 原生 Service/DAO、JUnit 5、Mockito。

**Spec:** `docs/superpowers/specs/2026-09-15-monitoring-alarms-backend-package-refactor-design.md`

## Global Constraints

- 只整理当前自定义 `monitoring` 包、新增平级 `alarms` 包，以及同步调整两个自定义控制器和相关自定义测试。
- 不修改 ThingsBoard 原生 `AlarmController`、`AlarmCommentController`、`AlarmRuleController`。
- 不修改前端页面、前端请求地址、接口 URL、请求参数、响应 JSON、权限或统计口径。
- 告警统计继续面向当前用户可见的全部设备，不增加 `productCategory=lightning` 过滤。
- 防雷页面原有 `productCategory=lightning` 和 `functionCategory` 过滤保持不变。
- 用户当前已修改及未跟踪源码只迁移和改引用，不回滚、不覆盖业务内容。
- 所有新增或迁移后的类、接口和方法使用中文职责注释。
- 不运行、建议运行或触碰任何 Maven 命令；只进行源码静态检查。
- 当前工作区存在其他改动，不自动执行 Git 提交。

---

## 文件结构锁定

### 新增告警模块

```text
application/src/main/java/org/thingsboard/server/service/alarms/
├─ dto/
│  ├─ DeviceAlarmAnalysisOverviewDTO.java
│  ├─ DeviceAlarmAnalysisSummaryDTO.java
│  ├─ DeviceAlarmDeviceCoverageDTO.java
│  ├─ DeviceAlarmFocusDeviceDTO.java
│  ├─ DeviceAlarmLeafAssetCountDTO.java
│  ├─ DeviceAlarmPeriodSummaryDTO.java
│  ├─ DeviceAlarmSeverityTotalDTO.java
│  ├─ DeviceAlarmStatusTotalDTO.java
│  ├─ DeviceAlarmSummaryDTO.java
│  ├─ DeviceAlarmTrendPointDTO.java
│  └─ DeviceAlarmTypeTotalDTO.java
└─ service/
   ├─ DeviceAlarmAnalysisService.java
   ├─ DeviceAlarmQueryService.java
   ├─ DeviceAlarmSummaryService.java
   └─ impl/
      ├─ DeviceAlarmAnalysisServiceImpl.java
      ├─ DeviceAlarmQueryServiceImpl.java
      └─ DeviceAlarmSummaryServiceImpl.java
```

### 新增自定义控制器

```text
application/src/main/java/org/thingsboard/server/controller/DeviceAlarmController.java
```

### Monitoring 页面 Service

```text
application/src/main/java/org/thingsboard/server/service/monitoring/service/
├─ SmartLightningDashboardService.java
├─ SmartLightningMonitoringService.java
├─ SmartLightningDataQueryService.java
├─ SmartLightningDeviceService.java
├─ SmartLightningMonitoringTreeService.java
├─ DeviceStatusSummaryService.java
└─ impl/
   ├─ SmartLightningDashboardServiceImpl.java
   ├─ SmartLightningMonitoringServiceImpl.java
   ├─ SmartLightningDataQueryServiceImpl.java
   ├─ SmartLightningDeviceServiceImpl.java
   ├─ SmartLightningMonitoringTreeServiceImpl.java
   ├─ SmartLightningMonitoringTreeBuilder.java
   └─ DeviceStatusSummaryServiceImpl.java
```

---

### Task 1: 迁移公共告警分页查询服务

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/service/DeviceAlarmQueryService.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmQueryServiceImpl.java`
- Delete after migration: `application/src/main/java/org/thingsboard/server/service/monitoring/service/MonitoringAlarmQueryService.java`
- Delete after migration: `application/src/main/java/org/thingsboard/server/service/monitoring/service/impl/MonitoringAlarmQueryServiceImpl.java`
- Move/update test: `application/src/test/java/org/thingsboard/server/service/alarms/DeviceAlarmQueryServiceTest.java`

**Interfaces:**
- Consumes: ThingsBoard `AlarmService`、`SecurityUser`、`AlarmQueryV2`。
- Produces:

```java
PageData<AlarmInfo> findPage(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException;
List<AlarmInfo> findAll(SecurityUser user, AlarmQueryV2 query) throws ThingsboardException;
```

- [ ] **Step 1: 建立新的告警查询接口**

将原接口方法签名原样迁移，包名改为 `org.thingsboard.server.service.alarms.service`，类名改为 `DeviceAlarmQueryService`，中文注释明确其统计范围为当前用户可见的全部设备。

- [ ] **Step 2: 迁移实现且不改分页逻辑**

将 `MonitoringAlarmQueryServiceImpl` 内容迁移为 `DeviceAlarmQueryServiceImpl`。保留客户用户调用 `findCustomerAlarmsV2`、其他用户调用 `findAlarmsV2`、按照 `PageData.hasNext()` 翻页的现有写法。

- [ ] **Step 3: 迁移测试类型名和包名**

把 `MonitoringAlarmQueryServiceTest` 改为 `DeviceAlarmQueryServiceTest`，断言仍覆盖客户用户和租户用户两个查询分支，不改变 Mockito 行为。

- [ ] **Step 4: 更新所有消费者 import**

在地网、SPD、雷电流、实时分析、设备状态、告警摘要和告警分析实现中，把 `MonitoringAlarmQueryService` 注入类型改为 `DeviceAlarmQueryService`。

- [ ] **Step 5: 静态确认旧类型清零**

Run:

```powershell
rg -n "MonitoringAlarmQueryService" application/src/main/java application/src/test/java
```

Expected: 无输出。

---

### Task 2: 迁移并重命名全部设备告警 DTO

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmAnalysisOverviewDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmAnalysisSummaryDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmDeviceCoverageDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmFocusDeviceDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmLeafAssetCountDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmPeriodSummaryDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmSeverityTotalDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmStatusTotalDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmSummaryDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmTrendPointDTO.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/dto/DeviceAlarmTypeTotalDTO.java`
- Delete after migration: the corresponding eleven `SmartLightningAlarm*.java` and `SmartLightningFocusAlarmDeviceDTO.java` files under `service/monitoring/dto`.

**Interfaces:**
- Consumes: existing DTO fields, constructor order and Lombok annotations.
- Produces: identical JSON property names with only Java type/package names changed.

- [ ] **Step 1: 按一对一映射创建新 DTO**

```text
SmartLightningAlarmAnalysisOverviewDTO -> DeviceAlarmAnalysisOverviewDTO
SmartLightningAlarmAnalysisSummaryDTO  -> DeviceAlarmAnalysisSummaryDTO
SmartLightningAlarmDeviceCoverageDTO   -> DeviceAlarmDeviceCoverageDTO
SmartLightningFocusAlarmDeviceDTO      -> DeviceAlarmFocusDeviceDTO
SmartLightningLeafAssetAlarmCountDTO   -> DeviceAlarmLeafAssetCountDTO
SmartLightningAlarmPeriodSummaryDTO    -> DeviceAlarmPeriodSummaryDTO
SmartLightningAlarmSeverityTotalDTO    -> DeviceAlarmSeverityTotalDTO
SmartLightningAlarmStatusTotalDTO      -> DeviceAlarmStatusTotalDTO
SmartLightningAlarmSummaryDTO          -> DeviceAlarmSummaryDTO
SmartLightningAlarmTrendPointDTO       -> DeviceAlarmTrendPointDTO
SmartLightningAlarmTypeTotalDTO        -> DeviceAlarmTypeTotalDTO
```

- [ ] **Step 2: 保持字段和构造器完全一致**

只改 package、类名、同组 DTO 的泛型和构造参数类型。不得修改 `currentCount`、`previousCount`、`changePercent`、`alarmCount`、`handledCount`、`handledRate` 等字段名称和类型。

- [ ] **Step 3: 更新防雷摘要 DTO 的跨包引用**

`LightningCurrentMonitoringSummary` 当前引用告警类型统计 DTO，改为导入 `DeviceAlarmTypeTotalDTO`；地网的 `GroundAlarmTypeTotalDTO` 和 SPD 的 `SpdAlarmTypeTotalDTO` 继续留在 monitoring。

- [ ] **Step 4: 静态比对字段名**

Run:

```powershell
rg -n "private final" application/src/main/java/org/thingsboard/server/service/alarms/dto
```

Expected: 新 DTO 字段集合与迁移前一致，不出现业务字段删除或改名。

---

### Task 3: 合并告警分析页面 Service

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/service/DeviceAlarmAnalysisService.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmAnalysisServiceImpl.java`
- Delete after migration:
  - `service/monitoring/service/SmartLightningAlarmAnalysisService.java`
  - `service/monitoring/service/SmartLightningLeafAssetAlarmService.java`
  - `service/monitoring/service/SmartLightningAlarmTypeRankingService.java`
  - `service/monitoring/service/SmartLightningAlarmSeverityRankingService.java`
  - `service/monitoring/service/SmartLightningAlarmAnalysisOverviewService.java`
  - corresponding five implementation files.
- Rename/update test: `application/src/test/java/org/thingsboard/server/service/alarms/DeviceAlarmAnalysisServiceTest.java`

**Interfaces:**
- Consumes: `SmartLightningDeviceService`、`SmartLightningMonitoringTreeService`、`DeviceAlarmQueryService`、`MonitoringPeriodWindow`。
- Produces:

```java
DeviceAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;
List<DeviceAlarmLeafAssetCountDTO> getLeafAssetCounts(SecurityUser user, String period) throws ThingsboardException;
List<DeviceAlarmTypeTotalDTO> getTypeRanking(SecurityUser user, String period) throws ThingsboardException;
List<DeviceAlarmSeverityTotalDTO> getSeverityRanking(SecurityUser user, String period) throws ThingsboardException;
DeviceAlarmAnalysisOverviewDTO getOverview(SecurityUser user, String period) throws ThingsboardException;
```

- [ ] **Step 1: 建立一个页面级接口**

接口中的五个方法与当前五套告警分析接口逐一对应，方法名称区分业务含义，避免多个含糊的 `getRanking()`。

- [ ] **Step 2: 合并现有实现代码**

把日月年摘要、末级设备父资产统计、类型排行、严重程度排行、状态覆盖率的现有代码原样迁入一个实现类。仅处理重复常量、重复依赖字段和重名私有方法；不改告警查询条件、排序、去重及处理率公式。

- [ ] **Step 3: 保留当前用户全部可见设备口径**

实现仍调用 `SmartLightningDeviceService.getVisibleDeviceIds(user)`；不得改成 `getLightningDeviceIds(user)`，不得增加产品分类过滤。

- [ ] **Step 4: 更新告警类型排行测试**

原测试断言必须继续成立：同一设备三条“现场报警”按三条记录累计，隐藏设备告警不计入，结果按数量降序。

- [ ] **Step 5: 静态检查旧页面服务清零**

Run:

```powershell
rg -n "SmartLightning(AlarmAnalysis|LeafAssetAlarm|AlarmTypeRanking|AlarmSeverityRanking|AlarmAnalysisOverview)Service" application/src/main/java application/src/test/java
```

Expected: 无输出。

---

### Task 4: 迁移看板告警摘要服务

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/service/DeviceAlarmSummaryService.java`
- Create: `application/src/main/java/org/thingsboard/server/service/alarms/service/impl/DeviceAlarmSummaryServiceImpl.java`
- Delete after migration: `service/monitoring/service/SmartLightningAlarmSummaryService.java`
- Delete after migration: `service/monitoring/service/impl/SmartLightningAlarmSummaryServiceImpl.java`

**Interfaces:**
- Consumes: `SmartLightningDeviceService`、`DeviceAlarmQueryService`。
- Produces:

```java
DeviceAlarmSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;
```

- [ ] **Step 1: 迁移接口及实现**

保持未清除告警查询、可见设备过滤、按告警类型累计和数量降序排列逻辑不变，仅修改类名、包名和 DTO 类型。

- [ ] **Step 2: 修正职责注释**

删除“当前用户可见防雷设备”描述，改为“当前用户可见全部设备”；代码继续使用 `getVisibleDeviceIds(user)`。

- [ ] **Step 3: 检查不含防雷过滤**

Run:

```powershell
rg -n "getLightningDeviceIds|productCategory|functionCategory" application/src/main/java/org/thingsboard/server/service/alarms
```

Expected: 告警模块中不出现防雷产品过滤调用。

---

### Task 5: 新增告警控制器并保持接口地址兼容

**Files:**
- Create: `application/src/main/java/org/thingsboard/server/controller/DeviceAlarmController.java`
- Modify: `application/src/main/java/org/thingsboard/server/controller/SmartLightningMonitoringController.java`

**Interfaces:**
- Consumes: `DeviceAlarmSummaryService`、`DeviceAlarmAnalysisService`。
- Produces: 原有六个告警 URL，参数、权限和响应模型不变。

- [ ] **Step 1: 创建自定义告警控制器**

控制器使用 `@RestController`、`@TbCoreComponent`、`@RequiredArgsConstructor`，保留基地址：

```java
@RequestMapping("/api/smart-lightning/monitoring")
public class DeviceAlarmController extends BaseController
```

- [ ] **Step 2: 迁移六个端点**

```text
GET /alarm-summary
GET /alarm-analysis-summary
GET /alarm-analysis/leaf-assets?period=day
GET /alarm-analysis/types?period=day
GET /alarm-analysis/severities?period=day
GET /alarm-analysis/overview?period=day
```

每个端点保留 `hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')`，方法体只调用 Service。

- [ ] **Step 3: 清理原监测控制器**

从 `SmartLightningMonitoringController` 删除六个告警方法、告警 DTO import 和告警 Service 注入，只保留防雷看板、数据监测、数据查询、资产树及顶部状态入口。

- [ ] **Step 4: 检查映射没有重复或遗漏**

Run:

```powershell
rg -n "@(GetMapping|RequestMapping).*alarm" application/src/main/java/org/thingsboard/server/controller/SmartLightningMonitoringController.java application/src/main/java/org/thingsboard/server/controller/DeviceAlarmController.java
```

Expected: 六个告警 GET 路径只在 `DeviceAlarmController` 出现一次。

---

### Task 6: 建立智能防雷看板页面 Service

**Files:**
- Create: `service/monitoring/service/SmartLightningDashboardService.java`
- Create: `service/monitoring/service/impl/SmartLightningDashboardServiceImpl.java`
- Delete after logic migration:
  - `GroundMonitoringSummaryService.java`
  - `SpdMonitoringSummaryService.java`
  - `LightningCurrentMonitoringSummaryService.java`
  - corresponding three implementation files.

**Interfaces:**
- Consumes: `SmartLightningDeviceService`、`DeviceAlarmQueryService`、`TimeseriesService`、monitoring util calculators。
- Produces:

```java
GroundMonitoringSummary getGroundSummary(SecurityUser user, String period) throws ThingsboardException;
SpdMonitoringSummary getSpdSummary(SecurityUser user, String period) throws ThingsboardException;
LightningCurrentMonitoringSummary getLightningCurrentSummary(SecurityUser user, String period) throws ThingsboardException;
```

- [ ] **Step 1: 创建看板页面接口**

三个方法分别对应现有三个接口，返回类型及异常声明不变。

- [ ] **Step 2: 合并三个实现**

把地网、SPD、雷电流实现迁入 `SmartLightningDashboardServiceImpl` 的三个公共方法及各自私有方法区。保留所有遥测键、单位、周期边界、累计计数器差值、最值详情和告警设备计数逻辑。

- [ ] **Step 3: 只合并重复基础方法**

允许合并重复的周期参数解析、异常包装、设备 ID 提取和告警查询构造；不得把地网、SPD、雷电流各自的业务键判断混在同一方法中。

- [ ] **Step 4: 更新控制器注入**

`SmartLightningMonitoringController` 只注入一个 `SmartLightningDashboardService`，原三个端点分别调用 `getGroundSummary`、`getSpdSummary`、`getLightningCurrentSummary`。

- [ ] **Step 5: 更新纯计算测试引用**

`GroundResistanceRangeCalculatorTest` 和 `SpdMonitoringCalculatorTest` 保持不变，继续验证合并后实现使用的纯计算组件。

---

### Task 7: 建立数据监测页面 Service

**Files:**
- Create: `service/monitoring/service/SmartLightningMonitoringService.java`
- Create: `service/monitoring/service/impl/SmartLightningMonitoringServiceImpl.java`
- Delete after migration: `SmartLightningRealtimeAnalysisService.java`
- Delete after migration: `SmartLightningRealtimeAnalysisServiceImpl.java`

**Interfaces:**
- Consumes: `SmartLightningDeviceService`、`SmartLightningMonitoringTreeService`、`DeviceAlarmQueryService`、`TimeseriesService`。
- Produces:

```java
SmartLightningRealtimeAnalysisDTO getRealtimeAnalysis(SecurityUser user, UUID assetId)
        throws ThingsboardException;
```

- [ ] **Step 1: 迁移实时分析接口及实现**

把原 `getAnalysis` 逻辑迁入新页面 Service，方法名改为 `getRealtimeAnalysis`，内部设备范围、资产后代、分类和遥测统计逻辑保持不变。

- [ ] **Step 2: 更新控制器调用**

`GET /realtime-analysis` 的 URL 和 `assetId` 参数不变，仅把注入类型及调用方法改成页面 Service。

- [ ] **Step 3: 检查旧类型清零**

Run:

```powershell
rg -n "SmartLightningRealtimeAnalysisService" application/src/main/java application/src/test/java
```

Expected: 无输出。

---

### Task 8: 建立数据查询页面 Service

**Files:**
- Create: `service/monitoring/service/SmartLightningDataQueryService.java`
- Create: `service/monitoring/service/impl/SmartLightningDataQueryServiceImpl.java`
- Preserve: `SmartLightningDeviceService.java`
- Preserve: `SmartLightningDeviceServiceImpl.java`

**Interfaces:**
- Consumes: 公共 `SmartLightningDeviceService`。
- Produces:

```java
PageData<DeviceInfo> getDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException;
PageData<SmartLightningDeviceContextDTO> getDeviceContexts(SecurityUser user, PageLink pageLink)
        throws ThingsboardException;
```

- [ ] **Step 1: 创建数据查询页面契约**

页面 Service 只暴露 `/devices` 和 `/device-contexts` 对应的两个分页方法；历史遥测不新增后端封装，继续复用 TB 原生接口。

- [ ] **Step 2: 实现页面级委托**

`SmartLightningDataQueryServiceImpl` 调用公共 `SmartLightningDeviceService.getLightningDevices` 和 `getLightningDeviceContexts`，不复制权限过滤、属性批量查询和分页代码。

- [ ] **Step 3: 更新控制器注入**

两个现有 GET 端点改为调用 `SmartLightningDataQueryService`，URL、`pageSize`、`page`、`textSearch` 和 `PageLink` 构造保持不变。

- [ ] **Step 4: 保留公共设备测试**

`SmartLightningDeviceServiceTest` 继续验证公共设备服务跨页读取全部设备 ID 的行为；不为纯委托类复制同一套测试。

---

### Task 9: 保留并标注真正的公共组件

**Files:**
- Modify comments only where needed:
  - `SmartLightningDeviceService.java`
  - `SmartLightningMonitoringTreeService.java`
  - `SmartLightningMonitoringTreeBuilder.java`
  - `DeviceStatusSummaryService.java`
  - `MonitoringPeriodWindow.java`
  - `MonitoringValueParser.java`

**Interfaces:**
- Consumes: existing public signatures.
- Produces: unchanged public signatures and clearer Chinese responsibility comments.

- [ ] **Step 1: 标注公共设备服务消费者**

注释说明 `SmartLightningDeviceService` 被看板、数据监测、数据查询和告警分析复用，区分 `getVisibleDeviceIds` 与防雷过滤方法。

- [ ] **Step 2: 标注公共资产树职责**

注释说明资产树服务被数据监测、数据查询、设备详情和告警处理复用；不改树构建行为。

- [ ] **Step 3: 保留纯工具类**

不移动 `DeviceStatusSummaryCalculator`、`GroundResistanceRangeCalculator`、`MonitoringPeriodWindow`、`MonitoringValueParser`、`SpdMonitoringCalculator`，只修正因 DTO 或 Service 迁移产生的 import。

---

### Task 10: 完成全量静态兼容性检查

**Files:**
- Inspect all modified files under:
  - `application/src/main/java/org/thingsboard/server/service/monitoring`
  - `application/src/main/java/org/thingsboard/server/service/alarms`
  - `application/src/main/java/org/thingsboard/server/controller/SmartLightningMonitoringController.java`
  - `application/src/main/java/org/thingsboard/server/controller/DeviceAlarmController.java`
  - corresponding custom tests.

**Interfaces:**
- Consumes: all outputs from Tasks 1-9.
- Produces: no stale imports, duplicate routes or forbidden native changes.

- [ ] **Step 1: 检查告警包命名**

Run:

```powershell
rg -n "class SmartLightning|interface SmartLightning" application/src/main/java/org/thingsboard/server/service/alarms
```

Expected: 无输出。

- [ ] **Step 2: 检查 monitoring 残留告警页面类型**

Run:

```powershell
rg -n "SmartLightningAlarmAnalysis|SmartLightningAlarmSummary|SmartLightningLeafAssetAlarm|SmartLightningAlarmTypeRanking|SmartLightningAlarmSeverityRanking" application/src/main/java/org/thingsboard/server/service/monitoring
```

Expected: 无输出；地网/SPD 自己的告警类型 DTO 名称允许保留。

- [ ] **Step 3: 检查 Java 文件名和 public 类型一致**

逐个读取新增 Java 文件，确认 `public class/interface` 名称与文件名完全相同，package 与目录一致。

- [ ] **Step 4: 检查接口 URL 唯一性**

Run:

```powershell
rg -n "alarm-summary|alarm-analysis-summary|alarm-analysis/leaf-assets|alarm-analysis/types|alarm-analysis/severities|alarm-analysis/overview" application/src/main/java/org/thingsboard/server/controller
```

Expected: 每条自定义路径只出现一次，均位于 `DeviceAlarmController`。

- [ ] **Step 5: 检查前端请求地址未改**

Run:

```powershell
git diff -- ui-ngx/src/app/modules/home/pages/smart-lightning ui-ngx/src/app/modules/home/pages/deviceAlarms
```

Expected: 本次重构没有新增前端差异。

- [ ] **Step 6: 检查原生告警控制器未改**

Run:

```powershell
git diff -- application/src/main/java/org/thingsboard/server/controller/AlarmController.java application/src/main/java/org/thingsboard/server/controller/AlarmCommentController.java application/src/main/java/org/thingsboard/server/controller/AlarmRuleController.java
```

Expected: 无输出。

- [ ] **Step 7: 输出未执行构建的风险说明**

明确记录：因当前请求禁止触碰 Maven，本次只能证明包名、引用、路由和源代码结构静态一致；Java 编译和 Spring Bean 装配仍需用户之后自行构建确认。

