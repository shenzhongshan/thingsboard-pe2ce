# 自定义页面 WebSocket 刷新优化计划

## 状态

实施完成：阶段 0 至阶段 4 已完成；浏览器运行时验收仍待执行。

## 目标

对已扫描的自定义前端页面，优先减少“收到实时事件后又请求 HTTP 重载整块数据”的情况；实时值尽量由 WebSocket 推送直接更新。初次加载、周期切换、历史查询、分页结果和权限敏感的聚合仍以现有 HTTP 接口为准，避免为了减少请求而丢失历史数据或绕过服务端权限范围。

## 范围

- `ui-ngx/src/app/modules/home/pages/smart-lightning/`
- `ui-ngx/src/app/modules/home/pages/deviceAlarms/`
- `ui-ngx/src/app/modules/home/pages/homeOverview/`
- `ui-ngx/src/app/modules/home/pages/log/`
- `ui-ngx/src/app/modules/home/pages/report/`（本次扫描未发现 WebSocket 使用，记录为无需改造）

## 架构原则

继续复用 `smart-lightning/shared/realtime/smart-lightning-realtime.service.ts` 的共享订阅和引用计数，不给每个页面另建全局连接。按事件语义分流：

- `deviceTelemetryChanged$`：包含设备 ID、遥测键、值和时间戳，是实时值直接更新的依据。
- `deviceChanged$`：设备状态或实体变化的失效通知；只有变化影响当前视图时才重新查询，不应无条件触发整页重载。
- `alarmChanged$`：告警数据变化通知；告警明细、分页列表及后端聚合仍通过现有接口校准。
- 属性、设备上下文、设备列表和资产列表变更：仅刷新依赖对应信息的视图。

HTTP 继续承担首次快照、用户切换设备/资产/周期后的查询、历史数据、分页和连接恢复后的校准。WebSocket 到达后不为每个遥测点单独发 GET；高频事件按设备和键合并，避免 UI 高频重绘。不得新增后端接口，除非后续验证证明现有接口无法满足且另行确认。

## 优先级总览

| 优先级 | 页面/区域 | 当前行为 | 建议优化 | 原因 |
| --- | --- | --- | --- | --- |
| P0 | 数据监测顶部实时统计 | 卡片遥测已有直接订阅；部分设备/告警变化仍触发实时分析 HTTP 查询 | 将当前范围内可由遥测确定的实时状态及最值，基于初始 HTTP 快照和 WebSocket 增量在前端更新；告警变化仍只刷新告警相关数据 | 页面定位是实时监测，遥测每次变化都重新请求汇总的收益最大 |
| P1 | 智能防雷看板地网、SPD、雷电流最值 | `deviceChanged$` 触发设备摘要/分类聚合 GET | 对已经订阅且属于当前视图范围的键直接增量更新最值及其设备/时间/相位或极性；保留初始、切换周期、切换范围和恢复连接时的 HTTP 查询 | 避免最值因每次上报反复查询；仍需 HTTP 提供周期历史基线 |
| P2 | 告警列表、数据查询 | 较宽泛的设备变更通知可能引发整组设备/告警 HTTP 重载 | 按页面实际展示字段过滤事件，只在相关设备状态、资产上下文或设备列表变化时重查；告警新增/确认/清除仍由 `alarmChanged$` 驱动现有告警查询 | 避免无关遥测导致列表重载，同时不改变告警分页和筛选语义 |
| P3 | 自定义首页 | 设备、资产、告警事件分别触发统计数据 HTTP 刷新，告警数量变化还存在延迟补刷 | 先记录请求频率与实际依赖；仅对可由本地已加载且权限范围完整的数据做小范围增量更新，复杂分类/资产统计继续使用现有接口 | 全局统计和权限范围容易受影响，需先证据验证，风险高于收益 |
| 保持现状 | 设备详情、设备日志、告警分析、报表 | 设备详情/日志已直接消费实时遥测；告警明细、历史分析走 HTTP；报表没有 WebSocket | 不做“为了统一而改造”。告警变更时可继续刷新告警明细；历史分析不因普通遥测刷新；报表维持现状 | 这些 HTTP 查询提供分页、历史或服务端聚合，当前职责合理 |

## 分阶段实施计划

### 阶段 0：固定事件与字段边界

- [x] 复核共享实时服务的事件载荷、订阅键和页面引用计数，列出每个页面实际依赖的设备 ID、遥测键及事件类型。
- [x] 给候选 HTTP 请求标注触发源：首次加载、遥测、设备属性/状态、告警、筛选变化或重连；静态基线按源码事件触发链记录。浏览器 Network 每分钟请求量仍需运行环境现场验收。
- [x] 确认当前资产树、设备类型、租户/客户权限过滤的边界；增量计算只作用于当前已加载范围，不自行扩展数据范围。

涉及文件：

- `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service.ts`
- `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring.component.ts`
- `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.ts`
- `ui-ngx/src/app/modules/home/pages/deviceAlarms/list/smart-lightning-alarm-list.component.ts`
- `ui-ngx/src/app/modules/home/pages/smart-lightning/query/smart-lightning-data-query.component.ts`
- `ui-ngx/src/app/modules/home/pages/homeOverview/home-overview.component.ts`

验收：事件来源和页面依赖可逐项对应；在没有数据变化时不出现重复的整页 HTTP 查询。

### 阶段 1（P0）：数据监测实时统计增量更新

- [x] 保留页面首次加载的现有 HTTP 汇总结果作为初始状态。
- [x] 使用 `deviceTelemetryChanged$` 中的设备 ID、标准遥测键、原始值和时间戳，只更新当前资产/设备范围内受影响的统计项；状态值遵循现有 `0/0.0=正常、1/1.0=异常` 规则。
- [x] 告警变化仅触发告警数据刷新，不连带重查与告警无关的遥测最值。
- [x] 设备切换、资产树变化、类型筛选、页面重新进入和 WebSocket 重连时，继续调用原接口重新校准。
- [x] 若最值所依据的周期窗口发生边界滑动，或无法从当前缓存可靠撤销已过期的最值，才请求现有接口重算；不能只靠最新一点推断历史窗口。

涉及文件：

- `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring.component.ts`
- `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service.ts`

验收：连续遥测推送能直接更新相关卡片/最值；单次遥测不产生对应汇总 GET；范围变化及重连后数据仍能与现有接口结果对齐。

### 阶段 2（P1）：看板周期最值增量更新

- [x] 从现有 HTTP 周期摘要初始化各最值及其设备名称、参数键、时间戳和相位/极性信息。
- [x] 只对当前已选周期、当前可见设备范围内的实时推送做增量比较，不改变服务端告警统计、设备数或周期计算口径。
- [x] 极值元信息必须与产生该极值的样本一起更新，不能出现数值是新值但设备/时间/相位仍是旧值。
- [x] 日/周/月切换、资产范围变化和重连继续使用原 HTTP 查询；周期窗口边界导致极值可能失效时也重新校准。

涉及文件：

- `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.ts`
- `ui-ngx/src/app/modules/home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service.ts`

验收：地网电阻、SPD 温度/漏电流/电压、雷电流峰值的实时推送能更新对应最值及悬浮信息；非相关键不触发统计刷新；日/周/月基线切换正确。

### 阶段 3（P2）：缩小告警列表和数据查询的失效刷新范围

- [x] 分开处理告警事件、设备列表/上下文事件和遥测事件，避免任意 `deviceChanged$` 都重新请求整组设备与告警。
- [x] 告警列表只在告警新增、确认、清除等影响当前查询结果的事件后调用现有告警接口；设备列表只在名称、归属、状态等实际展示字段改变时重载。
- [x] 数据查询页面保持历史遥测查询行为；只有设备状态或资产标记所依赖的数据变化时刷新相应部分，不因无关测点变化重载设备表。
- [x] 若共享事件目前无法区分变化字段，先在前端按现有事件载荷缩小触发范围；不得臆造新的 WebSocket 协议或后端接口。

涉及文件：

- `ui-ngx/src/app/modules/home/pages/deviceAlarms/list/smart-lightning-alarm-list.component.ts`
- `ui-ngx/src/app/modules/home/pages/smart-lightning/query/smart-lightning-data-query.component.ts`
- 必要时：`ui-ngx/src/app/modules/home/pages/smart-lightning/shared/realtime/smart-lightning-realtime.service.ts`

验收：无关遥测不触发整表重载；新增/确认/清除告警仍及时反映；筛选、分页、资产树选择和用户权限表现与改造前一致。

### 阶段 4（P3）：自定义首页按证据优化

- [x] 按源码梳理 `deviceRefresh$`、`assetRefresh$`、`alarmRefresh$` 的触发源及页面指标依赖；稳定运行时的请求频率仍需通过浏览器 Network 现场验收。
- [x] 对当前用户首屏接口已返回的设备，仅将 `active` 遥测增量应用到在线/离线数量和地图设备状态；设备总量、分类统计、坐标统计、告警统计及权限范围仍由原接口校准。
- [x] 保留告警数量变化后的 800ms 一次补刷，使用 `switchMap` 合并连续计数事件，避免每个计数事件各自创建延迟 GET。

涉及文件：

- `ui-ngx/src/app/modules/home/pages/homeOverview/home-overview.component.ts`
- 必要时：共享实时服务

验收：静态增量测试和 TypeScript 语义检查通过；总量、分类数量、未配置数量与地图设备范围仍由原权限接口提供。运行时 Network 请求量及页面数值对照仍需现场验收。

## 明确保留的 HTTP / WebSocket 行为

- 首屏和筛选/周期切换后的 HTTP 是权威快照；WebSocket 是实时增量，不替代历史查询。
- 告警列表、重点告警明细、告警分析趋势/排行榜继续使用现有 HTTP 数据接口，并由告警事件触发刷新；普通遥测不触发历史告警分析重算。
- 设备详情和日志抽屉已有的实时遥测订阅继续直接更新页面；时间线审计/历史记录继续按现有接口加载。
- 报表页当前没有 WebSocket 使用，不为其新增订阅。
- 不新增接口、不更改后端 DTO、不修改 ThingsBoard 原生文件，不运行 Maven。后续实现只改本项目自定义前端文件，并按用户要求不等待前端构建结果。

## 总体验收清单

- [ ] 浏览器 Network 验证：稳定遥测流下不再每个点都触发相应摘要 GET；确需校准的请求只出现在初始化、筛选/周期变化、重连或窗口边界场景。
- [ ] 对比 WebSocket 增量后的页面数值与现有 HTTP 接口结果，覆盖正常值、异常值、重复推送、乱序时间戳及同一设备多温度键。
- [ ] 验证离开页面释放共享订阅，重复进入不会产生重复订阅或重复事件处理。
- [ ] 验证资产树、产品分类、用户权限、告警分页和历史周期口径不变。
