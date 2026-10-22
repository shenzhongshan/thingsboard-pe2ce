# Monitoring 与设备告警后端结构重组设计

## 1. 目标

对项目自定义的 `org.thingsboard.server.service.monitoring` 模块进行职责重组，在其平级位置新增
`org.thingsboard.server.service.alarms`，将面向全部设备的告警业务从智能防雷业务中分离。

本次属于结构迁移，不改变已有功能、接口请求地址、请求参数、响应 JSON 字段、权限边界和统计口径。

## 2. 修改范围

允许修改：

- `application/src/main/java/org/thingsboard/server/service/monitoring`
- 新增 `application/src/main/java/org/thingsboard/server/service/alarms`
- 自定义控制器 `SmartLightningMonitoringController`
- 新增自定义控制器 `DeviceAlarmController`
- 因 Java 包名和类型名迁移而必须同步调整的自定义测试引用

禁止修改：

- ThingsBoard 原生 `AlarmController`、`AlarmCommentController`、`AlarmRuleController`
- 前端页面和前端请求地址
- 其他 ThingsBoard 原生 Service、DAO、实体或配置
- Maven 配置及 Maven 构建流程

## 3. 命名规则

智能防雷专属类保留 `SmartLightning` 前缀。

告警模块统计当前用户可见的全部设备，不使用 `SmartLightning` 前缀。为避免与 ThingsBoard 原生
`AlarmController` 冲突，自定义告警类型统一采用 `DeviceAlarm` 前缀：

- `DeviceAlarmController`
- `DeviceAlarmAnalysisService`
- `DeviceAlarmAnalysisServiceImpl`
- `DeviceAlarmQueryService`
- `DeviceAlarmQueryServiceImpl`
- `DeviceAlarmSummaryService`
- `DeviceAlarmSummaryServiceImpl`
- `DeviceAlarmAnalysisSummaryDTO` 等告警 DTO

## 4. 页面专属 Service

| 页面 | 页面 Service | 职责 |
|---|---|---|
| 智能防雷看板 | `SmartLightningDashboardService` | 组织地网、SPD、雷电流及看板摘要数据 |
| 数据监测 | `SmartLightningMonitoringService` | 组织资产选择后的实时分类、统计和设备监测数据 |
| 数据查询 | `SmartLightningDataQueryService` | 组织设备分页和设备上下文；历史遥测继续调用 TB 原生能力 |
| 告警分析 | `DeviceAlarmAnalysisService` | 日月年摘要、末级站点、类型、级别、状态、趋势和重点设备统计 |
| 告警处理 | TB 原生告警服务 | 派发、确认、清除和活动仍复用原生能力，不重复创建业务实现 |

原有告警分析的多个接口和实现合并到 `DeviceAlarmAnalysisService` 与
`DeviceAlarmAnalysisServiceImpl`，以一个二级页面对应一套 Service/Impl。

## 5. 公共组件及使用页面

| 公共组件 | 使用页面 |
|---|---|
| `SmartLightningDeviceService` | 智能防雷看板、数据监测、数据查询、告警分析 |
| `SmartLightningMonitoringTreeService` | 数据监测、数据查询、设备详情、告警处理 |
| `SmartLightningMonitoringTreeBuilder` | 资产树公共服务内部 |
| `DeviceAlarmQueryService` | 智能防雷看板、数据监测、告警分析、顶部状态统计 |
| `DeviceAlarmSummaryService` | 智能防雷看板告警摘要；保留为独立告警聚合能力 |
| `DeviceStatusSummaryService` | 系统顶部正常/异常设备统计 |
| `MonitoringPeriodWindow` | 地网、SPD、雷电流、告警分析的日/月/年或日/周/月窗口 |
| `MonitoringValueParser` | 地网、SPD、雷电流遥测值读取 |
| `DeviceStatusSummaryCalculator` | 顶部状态统计纯计算 |
| `GroundResistanceRangeCalculator` | 地网阻值纯计算 |
| `SpdMonitoringCalculator` | SPD 指标纯计算 |

公共组件必须具有两个以上消费者，或者承担独立、可复用的数据访问/纯计算职责。页面专属业务不得放入公共组件。

## 6. 目标目录

```text
org/thingsboard/server/service/
├─ monitoring/
│  ├─ dto/
│  ├─ service/
│  │  ├─ SmartLightningDashboardService.java
│  │  ├─ SmartLightningMonitoringService.java
│  │  ├─ SmartLightningDataQueryService.java
│  │  ├─ SmartLightningDeviceService.java
│  │  ├─ SmartLightningMonitoringTreeService.java
│  │  ├─ DeviceStatusSummaryService.java
│  │  └─ impl/
│  │     ├─ SmartLightningDashboardServiceImpl.java
│  │     ├─ SmartLightningMonitoringServiceImpl.java
│  │     ├─ SmartLightningDataQueryServiceImpl.java
│  │     ├─ SmartLightningDeviceServiceImpl.java
│  │     ├─ SmartLightningMonitoringTreeServiceImpl.java
│  │     ├─ SmartLightningMonitoringTreeBuilder.java
│  │     └─ DeviceStatusSummaryServiceImpl.java
│  └─ util/
└─ alarms/
   ├─ dto/
   └─ service/
      ├─ DeviceAlarmAnalysisService.java
      ├─ DeviceAlarmQueryService.java
      ├─ DeviceAlarmSummaryService.java
      └─ impl/
         ├─ DeviceAlarmAnalysisServiceImpl.java
         ├─ DeviceAlarmQueryServiceImpl.java
         └─ DeviceAlarmSummaryServiceImpl.java
```

## 7. 控制器拆分

`SmartLightningMonitoringController` 只保留智能防雷看板、数据监测、数据查询和公共资产树相关入口。

新增 `DeviceAlarmController`，承接以下现有路径：

- `/api/smart-lightning/monitoring/alarm-summary`
- `/api/smart-lightning/monitoring/alarm-analysis-summary`
- `/api/smart-lightning/monitoring/alarm-analysis/leaf-assets`
- `/api/smart-lightning/monitoring/alarm-analysis/types`
- `/api/smart-lightning/monitoring/alarm-analysis/severities`
- `/api/smart-lightning/monitoring/alarm-analysis/overview`

新控制器暂时继续使用原 URL，保证前端无须修改。控制器只完成参数接收、权限声明、当前用户获取和 Service 调用，禁止保留业务计算。

## 8. 数据流与依赖方向

```text
前端页面
  -> 自定义 Controller
  -> 页面 Service
  -> 公共设备/资产树/告警查询 Service
  -> ThingsBoard 原生 Service 与 DAO
```

告警分析 Service 可以使用智能防雷模块中的公共设备可见性和资产树能力，但不得依赖智能防雷看板、数据监测或数据查询的页面 Service。

## 9. 兼容性要求

- 所有现有接口路径不变。
- 所有 `@RequestParam` 名称和默认值不变。
- 所有 `@PreAuthorize` 权限不变。
- DTO JSON 属性名、类型及空值行为不变。
- 告警统计继续面向当前用户可见的全部设备，不增加 `productCategory=lightning` 过滤。
- 防雷看板和数据监测原有产品分类过滤保持不变。
- 已修改和未跟踪的用户代码只做迁移和引用调整，不回滚、不覆盖其业务内容。

## 10. 校验方式

不运行 Maven。仅执行以下静态校验：

- 检查原 `monitoring` 中不再残留 `SmartLightningAlarm*` 页面实现。
- 检查 `alarms` 包内不存在 `SmartLightning` 前缀类型。
- 检查所有迁移类型的 import、构造注入和方法调用均已更新。
- 检查两个自定义控制器没有重复映射或遗漏现有 URL。
- 检查 DTO 字段与迁移前一致。
- 检查告警实现未新增防雷产品过滤。
- 检查原生告警控制器和原生告警服务没有改动。

