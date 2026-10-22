# 智能防雷共享资产树设计

## 目标

将智能防雷模块中数据监测、数据查询、告警列表和设备详情页面重复的资产树模板与交互提取为一个可复用组件，同时保持各页面现有的数据加载、根资产筛选和节点选择业务逻辑。

## 范围

- 共享组件负责资产树的搜索过滤、展开/收起、节点渲染、键盘操作、选中样式和告警标记样式。
- 页面组件继续负责资产树接口调用、选中节点集合、根资产筛选、告警设备集合以及详情页锁定规则。
- 未选中节点不显示左侧标记；选中节点显示 12px 绿色竖条；选中的告警设备显示红色竖条。
- 不改变设备卡片、告警列表或数据查询的数据接口与筛选逻辑。

## 组件接口

组件路径：`ui-ngx/src/app/modules/home/pages/smart-lightning/shared/asset-tree/`

- `@Input() tree: SmartLightningMonitoringAssetTree`
- `@Input() selectedIds: ReadonlySet<string>`
- `@Input() expandedIds: ReadonlySet<string>`
- `@Input() rootAssetId: string`
- `@Input() search: string`
- `@Input() loading: boolean`
- `@Input() loadError: boolean`
- `@Input() alarmDeviceIds: ReadonlySet<string>`
- `@Input() selectableIds: ReadonlySet<string> | null`
- `@Output() searchChange`, `selectNode`, `toggleNode`

组件内部通过 `getSmartLightningMonitoringVisibleRoots` 计算根筛选结果，通过资产树节点递归生成可见列表；页面接收事件后更新自己的状态集合。

## 兼容约束

- 继续使用当前 Angular 模块式组件，不引入 standalone 迁移。
- 共享组件样式使用智能防雷模块现有蓝色、绿色和红色语义色。
- 保留页面原有标题、根资产筛选器和资产数量显示；共享组件只替换搜索框和树内容。
- 设备详情页的 `selectableIds` 只允许当前详情设备被选中。
