# 智能防雷 SPD 与雷电监测模块设计

## 目标

在现有地网监测卡片的布局和权限边界基础上，增加真实数据驱动的 SPD 监测与雷电监测摘要卡片。两个模块分别按 `functionCategory` 过滤设备，拥有独立的日/周/月筛选状态，设备状态使用当前快照，周期指标使用选定时间窗口。

## 设备范围

- SPD：`productCategory=lightning` 且 `functionCategory=spd_monitor`，覆盖 DESA、DES4、DES9、DEM2、DEM4、DEA2、DEA4、ESM0、ESM3。
- 雷电：`productCategory=lightning` 且 `functionCategory=lightning_current`，覆盖 M1AI、L2AI。

## 卡片数据

### SPD 监测

- 当前快照：设备总数、在线、离线。
- 周期统计：告警数量，按告警 `startTs` 统计。
- 核心指标：选定窗口内 `leakageA/leakageB/leakageC`（兼容 I1/I2/I3）的最大值；无测点设备跳过，全部缺失返回 null。

### 雷电监测

- 当前快照：设备总数、在线、离线。
- 周期统计：告警数量、雷电事件数。
- 核心指标：选定窗口内 `abs(peakCurrent)` 的最大值和对应设备、时间、极性；M1AI 的 F 按 kA 读取后换算为 A，L2AI 的 F 按 A 读取，统一为 A 后再比较。
- `surgeCount` 为累计计数器，按相邻采样值计算正向增量；检测到计数器回退时按设备重启处理，避免把累计值直接相加。

## 后端边界

Controller 只接收 `period` 并委托 Service；Service 接口位于 `service`，实现位于 `service/impl`，设备权限和属性查询复用现有 `SmartLightningDeviceService`，告警分页复用 `MonitoringAlarmQueryService`。DTO 显式区分无数据的 null 与真实的 0，并返回遥测覆盖设备数。

## 前端边界

在 `smart-lightning-dashboard` 中将 SPD 和雷电两个 mock 图表替换为摘要卡片。保留现有地网、告警、工单和地图，不改变其它卡片。请求加载失败显示错误状态并保留卡片高度；无遥测显示 `--`。两个模块的筛选状态互不联动。

## 非目标

- 不新增根资产计算字段。
- 不改动 Maven 配置或运行 Maven 构建。
- 不把 `lifePercent` 放入 SPD 总览；该字段继续用于设备详情。
