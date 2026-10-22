# 智能防雷告警列表（与 smart-lightning 平级）

## 目标

新增 `ui-ngx/src/app/modules/home/pages/deviceAlarms/list` 页面目录。页面通过 `/deviceAlarms/list` 访问，和现有 `smart-lightning` 页面目录平级；路由在原生 `alarms` 路由之前直接匹配，避免被 `RouterTabsComponent` 接管。

## 实施步骤

1. 先补充告警列表纯数据转换与筛选排序测试，覆盖严重程度、工单状态、资产子树过滤。
2. 新增告警列表数据模型与转换函数，复用智能防雷资产树模型、资产树接口和 ThingsBoard 原生告警查询接口。
3. 新增告警列表组件、模板和样式：左侧资产树，右侧筛选区、状态标签、20 条分页表格和操作按钮。
4. 将组件声明到现有 `AlarmModule`，并在 `AlarmRoutingModule` 中加入优先匹配的顶层 `deviceAlarms/list` 路由，因此最终 URL 为 `/deviceAlarms/list`。
5. 只做 TypeScript 静态转译、模板结构检查和路由引用检查，不运行 Maven。

## 约束

- 资产树选择一个节点时，列表只显示该节点及全部下属设备的告警。
- 默认每页 20 条；未处理/活动告警优先，随后按严重程度（严重、重要、次要、警告、不确定）排序；已处理告警按时间倒序。
- 不复制全局侧栏和顶栏，沿用系统外壳。
- 查询失败、加载中和空列表都要有明确状态。
