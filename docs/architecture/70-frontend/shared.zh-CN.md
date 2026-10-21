# Shared 模块

**本文是权威版。** 英文版 [shared.md](shared.md) 为对照上游 CE 的参照副本，改动请先落在本文。

Shared 模块（`src/app/shared/`）提供贯穿全应用使用的可复用组件、模型、pipe、directive 和工具。
它同时被 Home 模块与 Login 模块导入。

## 模块结构

```
src/app/shared/
├── components/   # 20+ component groups (button, color-picker, dialog, entity, image, time, etc.)
├── models/       # TypeScript interfaces, enums, constants
├── pipe/         # Custom pipes (translation, filtering, formatting)
├── directives/   # Custom directives
├── decorators/   # Custom decorators
├── services/     # Shared utility services
├── import-export/ # Data import/export (CSV, JSON)
├── adapter/      # Custom data adapters
├── animations/   # Shared animation definitions
└── legacy/       # Legacy compatibility utilities
```

Shared 模块的所有子目录合计约 402 个 TypeScript 文件。

## components/ —— 共享 UI 组件

### button/

| 文件 | 用途 |
|------|---------|
| 切换按钮、复制按钮、文件输入按钮 | 各类专用按钮组件 |

### color-picker/

| 文件 | 用途 |
|------|---------|
| `color-picker.component.ts` | 内联取色器组件 |
| `color-picker-dialog.component.ts` | 对话框形式的取色器 |
| 颜色相关组件 | 基于 `@iplab/ngx-color-picker` |

### dialog/

| 文件 | 用途 |
|------|---------|
| `dialog.component.ts` | 对话框基础组件 |
| `confirm-dialog.component.ts` | 是/否确认对话框 |
| `alert-dialog.component.ts` | 信息提示对话框 |
| `error-alert-dialog.component.ts` | 错误信息对话框 |
| `color-picker-dialog.component.ts` | 取色器对话框 |
| `material-icons-dialog.component.ts` | Material 图标浏览对话框 |
| `node-script-test-dialog.component.ts` | 规则节点的脚本测试对话框 |
| `object-edit-dialog.component.ts` | JSON 对象编辑对话框 |
| `todo-dialog.component.ts` | 待办/清单对话框 |
| `dynamic/` | 动态对话框组件 |
| `entity-conflict-dialog/` | 实体版本冲突解决对话框 |

### directives/

| 文件 | 用途 |
|------|---------|
| `tb-json-content` | JSON 语法高亮查看器 |
| `tb-json-object-edit` | 可编辑的 JSON 对象组件 |
| `tb-json-object-view` | 只读的 JSON 对象展示 |
| `tb-markdown` | Markdown 编辑器/查看器 |

### entity/

表单与过滤器中使用的实体选择与自动补全组件：

| 选择器 | 用途 |
|----------|---------|
| `tb-entity-autocomplete` | 选单个实体的自动补全输入 |
| `tb-entity-select` | 实体下拉选择 |
| `tb-entity-list` | 多实体的 chip 列表 |
| `tb-entity-list-select` | 带实体列表选择的下拉 |
| `tb-entity-key-autocomplete` | 实体属性/遥测 key 的自动补全 |
| `tb-entity-keys-list` | 多个实体 key 的 chip 列表 |
| `tb-entity-type-select` | 实体类型枚举的下拉 |
| `tb-entity-type-list` | 多实体类型的 chip 列表 |
| `tb-entity-subtype-select` | 实体子类型的下拉 |
| `tb-entity-subtype-list` | 多子类型的 chip 列表 |
| `tb-entity-subtype-autocomplete` | 实体子类型的自动补全 |
| `tb-entity-gateway-select` | 选择网关设备的下拉 |

### grid/

用于仪表板 widget 排布的网格布局组件。与 `angular-gridster2` 集成以支持拖拽式仪表板编辑。

### image/

图片管理组件：

| 选择器 | 用途 |
|----------|---------|
| `tb-image-gallery` | 已上传图片的网格图库 |
| `tb-image-gallery-dialog` | 图库的对话框包装 |
| `tb-image-input` | 单图上传输入 |
| `tb-gallery-image-input` | 打开图库进行选择的图片输入 |
| `tb-multiple-gallery-image-input` | 带图库的多图输入 |
| `tb-upload-image-dialog` | 上传对话框 |
| `tb-embed-image-dialog` | 嵌入外部图片的对话框 |
| `tb-scada-symbol-input` | SCADA 符号选择输入 |

### time/

仪表板与遥测视图中使用的时间范围选择组件：

| 选择器 | 用途 |
|----------|---------|
| `tb-timewindow` | 时间窗选择器（实时/历史） |
| `tb-timewindow-panel` | 时间窗配置的展开面板 |
| `tb-timewindow-config-dialog` | 时间窗高级设置的对话框 |
| `tb-datetime` | 日期时间选择输入 |
| `tb-datetime-period` | 日期区间选择 |
| `tb-timezone` | 时区展示 |
| `tb-timezone-select` | 时区选择下拉 |
| `tb-quick-time-interval` | 快捷时间区间预设选择 |
| `tb-timeinterval` | 时长输入（数值 + 单位） |
| `tb-datapoints-limit` | 数据点上限的数值输入 |
| `tb-aggregation-type-select` | 聚合类型下拉（AVG、SUM、MIN、MAX、COUNT、NONE） |
| `tb-history-selector` | 历史导航选择器 |

### notification/

通知中心的通知展示组件。含通知铃铛、通知列表和通知详情视图。

### ota-package/

OTA（空中升级）包组件：固件/软件上传、版本管理、文件校验和验证。

### queue/

供系统管理员使用的队列配置组件。展示 Kafka topic 配置、处理策略和重试设置。

### relation/

实体关系管理组件。提供创建、查看和删除实体间关系的 UI。

### resource/

资源库组件，用于管理 ThingsBoard 资源（JS 模块、图片等）。

### rule-chain/

规则链可视化编辑器组件：

| 选择器 | 用途 |
|----------|---------|
| 规则节点面板 | 规则节点类型浏览器 |
| 规则节点配置 | 单节点的配置面板 |
| 规则节点连线 | 节点间的连接及连线标签 |
| 调试模式控件 | 脚本测试与消息追踪 |

### table/

表格相关的共享组件，含数据表格、表头和分页控件。

### vc/

实体版本管理的版本控制组件。含 diff 查看器、提交历史和分支管理 UI。

### 其他（顶层组件）

| 选择器 | 用途 |
|----------|---------|
| `tb-breadcrumb` | 面包屑导航栏 |
| `tb-icon` | 自定义图标组件（MDI + Font Awesome） |
| `tb-logo` | ThingsBoard logo |
| `tb-copy-button` | 复制到剪贴板按钮 |
| `tb-toggle-password` | 密码可见性切换 |
| `tb-file-input` | 文件上传输入 |
| `tb-phone-input` | 国际电话输入（`libphonenumber-js`） |
| `tb-unit-input` | 度量单位输入 |
| `tb-value-input` | 带类型选择的通用值输入 |
| `tb-color-input` | 取色器输入 |
| `tb-key-val-map` | 键值对映射编辑器 |
| `tb-nav-tree` | 树形导航组件 |
| `tb-user-menu` | 用户资料下拉菜单 |
| `tb-notification-bell` | 带未读角标的通知铃铛图标 |
| `tb-footer` | 页脚 |
| `tb-scroll-grid` | 虚拟滚动网格 |
| `tb-social-share-panel` | 社交分享按钮 |
| `tb-fab-toolbar` | 浮动操作按钮工具栏 |
| `tb-footer-fab-buttons` | 页脚 FAB 按钮 |
| `tb-hotkeys` | 键盘快捷键处理 |
| `tb-fullscreen` | 全屏切换 directive |
| `tb-circular-progress` | 环形进度指示器 |
| `tb-cheatsheet` | 键盘快捷键速查表 |
| `tb-contact` | 联系/支持链接 |
| `tb-help` | 帮助面板 |
| `tb-help-popup` | 帮助弹窗 |
| `tb-help-markdown` | 带 markdown 的帮助内容 |
| `tb-hint-tooltip-icon` | 带提示 tooltip 的图标 |
| `tb-css` | CSS 编辑器组件 |
| `tb-html` | HTML 编辑器组件 |
| `tb-js-func` | JavaScript 函数编辑器 |
| `tb-js-func-modules` | JS 函数模块列表 |
| `tb-js-func-module-row` | JS 函数模块行 |

### 自动补全 / 选择类组件

| 选择器 | 用途 |
|----------|---------|
| `tb-dashboard-autocomplete` | 仪表板选择的自动补全 |
| `tb-dashboard-select` | 仪表板下拉 |
| `tb-dashboard-state-autocomplete` | 仪表板 state 的自动补全 |
| `tb-country-autocomplete` | 国家自动补全 |
| `tb-string-autocomplete` | 通用字符串自动补全 |
| `tb-message-type-autocomplete` | 规则引擎消息类型自动补全 |
| `tb-queue-autocomplete` | 队列名自动补全 |
| `tb-relation-type-autocomplete` | 关系类型自动补全 |
| `tb-resource-autocomplete` | 资源自动补全 |
| `tb-ota-package-autocomplete` | OTA 包自动补全 |
| `tb-branch-autocomplete` | Git 分支自动补全 |
| `tb-rule-chain-select` | 规则链选择下拉 |
| `tb-widgets-bundle-select` | Widget 包选择下拉 |
| `tb-template-autocomplete` | 通知模板自动补全 |
| `tb-material-icon-select` | Material 图标选择器 |
| `tb-slack-conversation-autocomplete` | Slack 会话自动补全 |

> **给这些组件定制样式时，`::ng-deep` 不能嵌在 `:host` 里。** `mat-autocomplete` /
> `mat-select` / `mat-menu` 的面板是经 CDK overlay 投射到 body 下的，不在组件的 DOM 子树内，
> 嵌套在 `:host` 内的 `::ng-deep` 永远到不了它们。规则必须写成顶层、不带宿主限定的形式；验证也
> 要用 `element.matches()` 实测，不能只看 styleSheets 里有没有这条规则。

> **新建组件前先全树 grep 一遍它的类名和 selector。** `ng build` 不会报出跨 NgModule 的
> 同名 selector 冲突，要到运行时才以 NG0300 爆出来。

## models/ —— TypeScript 模型

### id/

实体 ID 模型。为 API 调用与 URL 路由中使用的 ThingsBoard 实体 ID（DeviceId、AssetId、CustomerId
等）提供带类型的包装。

### page/

分页 API 响应的页数据模型。含 `PageData<T>`、`PageLink`、`TimePageLink` 以及排序/过滤参数。

### query/

实体数据查询模型。定义告警规则与仪表板数据源中使用的过滤谓词、key 过滤器和实体过滤器类型。

### telemetry/

遥测数据模型。定义时序数据结构、属性模型（client/server/shared）、最新值和聚合类型。

### time/

时间相关模型：`Timewindow`、`AggregationType`、`HistoryWindowType`。供时间选择组件与遥测查询使用。

### ace/

ACE（Ajax.org Cloud9 Editor）代码编辑器的配置模型。供规则节点脚本编辑器与 widget JS 编辑器使用。

### websocket/

实时通信的 WebSocket 消息类型。定义订阅命令、遥测更新消息和通知事件。

### widget/

Widget 模型：widget 类型定义、widget 包配置、仪表板 state 模型。

### 其他模型文件

| 文件 | 用途 |
|------|---------|
| `alarm.models.ts` | 告警严重度、状态、搜索过滤 |
| `alarm-rule.models.ts` | 告警规则的条件与排程定义 |
| `alias.models.ts` | 实体别名解析类型 |
| `api-usage.models.ts` | API 用量统计模型 |
| `asset.models.ts` | Asset 与 AssetInfo 接口 |
| `audit-log.models.ts` | 审计日志条目模型 |
| `authority.enum.ts` | 用户 authority 角色（SYS_ADMIN、TENANT_ADMIN、CUSTOMER_USER 等） |
| `base-data.ts` | 基础实体接口（hasId、hasName 等） |
| `calculated-field.models.ts` | 计算字段配置 |
| `color.models.ts` | 颜色/设置模型 |
| `common.ts` | 通用工具类型 |
| `component-descriptor.models.ts` | 规则节点组件描述符 |
| `constants.ts` | 应用级常量 |
| `contact-based.model.ts` | 含联系信息（地址、电话、邮箱）的基础模型 |
| `country.models.ts` | 国家数据模型 |
| `customer.model.ts` | Customer 接口 |
| `dashboard.models.ts` | 仪表板配置 |
| `device.models.ts` | Device 与 DeviceInfo 接口 |
| `dynamic-form.models.ts` | 动态表单字段配置 |
| `edge.models.ts` | Edge 实例模型 |
| `entity-type.models.ts` | 实体类型枚举 |
| `entity-view.models.ts` | 实体视图模型 |
| `entity.models.ts` | 通用实体模型 |
| `error.models.ts` | 错误响应模型 |
| `event.models.ts` | 实体事件模型 |
| `icon.models.ts` | 图标配置 |
| `js-function.models.ts` | JS 函数模块模型 |
| `limited-api.models.ts` | 受限流 API 的模型 |
| `login.models.ts` | 登录请求/响应 |
| `material.models.ts` | Material Design 常量 |
| `mobile-app.models.ts` | 移动应用配置 |
| `notification.models.ts` | 通知模型 |
| `ota-package.models.ts` | OTA 包模型 |
| `queue.models.ts` | 队列配置模型 |
| `relation.models.ts` | 实体关系模型 |
| `resource.models.ts` | 资源库模型 |
| `rpc.models.ts` | RPC 请求/响应模型 |
| `rule-chain.models.ts` | 规则链与规则节点模型 |
| `rule-node.models.ts` | 规则节点配置模型 |
| `settings.models.ts` | 系统/管理设置模型 |
| `tenant.model.ts` | Tenant 接口 |
| `user.model.ts` | User 接口 |

## pipe/ —— 自定义 pipe

用于模板转换的自定义 Angular pipe：

- **翻译类 pipe** —— 与 @ngx-translate 集成，做模板级翻译
- **过滤类 pipe** —— 数组与对象过滤
- **格式化类 pipe** —— 日期、数值、文件大小、时长的格式化
- **枚举类 pipe** —— 把枚举值转成展示标签
- **高亮 pipe** —— 在文本中高亮搜索词

## directives/ —— 自定义 directive

用于 DOM 操作与行为增强的自定义属性型 directive：

- 滚动、tooltip、popover、autofocus 等
- 输入掩码与校验
- 点击外部区域检测
- 拖放

## decorators/ —— 自定义装饰器

用于增强类与方法的 TypeScript 装饰器：

- `@OnDestroy()` —— 自动退订 RxJS 订阅
- `@CoerceBoolean()` —— 把 Input 属性强制转为 boolean

## services/ —— 共享 service

供各功能模块消费的共享工具 service：

- 数据转换与格式化
- 文件下载辅助
- 平台探测工具
- 剪贴板集成

## import-export/ —— 数据导入/导出

| 文件 | 用途 |
|------|---------|
| CSV 导入/导出 | 在实体数据与 CSV 格式之间转换，带列映射 |
| JSON 导入/导出 | 在实体数据与 JSON 格式之间转换 |

供批量建实体、仪表板导入和数据迁移功能使用。

## adapter/ —— 自定义适配器

在 API 响应与组件数据模型之间实现自定义转换逻辑的数据适配器。

## animations/ —— 共享动画

贯穿全应用使用的可复用 Angular 动画定义（淡入淡出、滑动、展开折叠）。

## 关联

- [Core 模块](core.zh-CN.md) —— core service 用共享模型做类型定义
- [页面模块](pages.zh-CN.md) —— 页面模块导入共享组件（实体选择、对话框、时间选择器）
- [主题与 i18n](theme-i18n.zh-CN.md) —— 共享组件使用主题体系
