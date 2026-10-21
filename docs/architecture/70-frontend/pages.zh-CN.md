# 页面模块

**本文是权威版。** 英文版 [pages.md](pages.md) 为对照上游 CE 的参照副本，改动请先落在本文。

页面模块（`src/app/modules/home/pages/`）含 40 个功能目录，每个实现 ThingsBoard UI 的一个特定
区块。页面按 home 路由模块中定义的路由被懒加载。

> 其中 8 个目录是本 fork 在上游 CE 之外新增的（多数为移植自 PE 的功能）：`device-simulator`、
> `deviceAlarms`、`entity-group`、`help-center`、`integration`、`reporting`、`smart-lightning`、
> `solution-templates`。下文已逐个列出。

## 页面模块架构

每个页面目录通常含：
- `*.module.ts` —— 带路由声明的 Angular 模块
- `*-routing.module.ts` —— 路由配置
- 主视图的组件文件（`.ts`、`.html`、`.scss`）
- `*table-config.resolver.ts` —— 实体表格配置（如属于 entity-table 类型）

## 页面类型

| 类型 | 形态 | 说明 |
|------|---------|-------------|
| **entity-table** | `EntitiesTableComponent` + resolver | 带表格、搜索、动作的列表页；详情可滑出面板或跳转到详情页 |
| **entity-details** | `EntityDetailsPageComponent` | 带 tab 的整页实体详情表单 |
| **settings-form** | 独立表单 + `ConfirmOnExitGuard` | 单个设置表单（通用、邮件、安全） |
| **dashboard-view** | `DashboardViewComponent` + resolver | 嵌入式仪表板视图（如 API 用量、网关） |
| **custom** | 独有组件 | 不套用标准模式的复杂页面 |

## 全部页面模块

### account/

当前用户的账号/资料管理。为账号设置重新导出 `profile/` 与 `security/` 组件。

### admin/

系统与租户的管理页面。

| 文件 | 用途 |
|------|---------|
| `general-settings.component.ts` | 系统设置（基址 URL、队列配置） |
| `mail-server.component.ts` | 发件邮件服务器配置 |
| `sms-provider.component.ts` | 短信 provider 设置（Twilio、AWS SNS 等） |
| `security-settings.component.ts` | 安全设置（密码策略、会话超时） |
| `two-factor-auth-settings.component.ts` | 双因子认证配置 |
| `home-settings.component.ts` | 租户首页仪表板设置 |
| `auto-commit-admin-settings.component.ts` | 自动提交的版本控制设置 |
| `repository-admin-settings.component.ts` | 仓库连接设置 |
| `trendz-settings.component.ts` | Trendz 分析设置 |
| `oauth2/` | OAuth2 客户端/域名管理（clients、domains 子页） |
| `queue/` | 队列管理（Kafka topic、处理策略） |
| `resource/` | 资源库与 JS 库子页 |

**类型：** settings-form 与 entity-table 混合

### ai-model/

ThingsBoard AI 集成的 AI 模型配置。

| 组件 | 用途 |
|-----------|---------|
| AiModels 表格 | AI 模型 CRUD 的 entity-table |

**类型：** entity-table
**Resolver：** `AiModelsTableConfigResolver`

### alarm/

告警中心，含两个视图：告警列表与告警规则。

| 文件 | 用途 |
|------|---------|
| 告警表格组件 | 展示全部告警，支持按严重度、状态、时间过滤 |
| 告警规则表格 | 管理告警规则定义（条件、排程、动作） |
| 告警规则详情 | 单条告警规则的实体详情页 |

**类型：** custom（告警各区块用 RouterTabsComponent）
**Resolver（规则）：** `AlarmRulesTableConfigResolver`

### api-usage/

API 用量统计仪表板。

**类型：** dashboard-view
**仪表板：** 加载 `api_usage.json` 仪表板定义

### asset/

资产管理。

| 文件 | 用途 |
|------|---------|
| `assets-table-config.resolver.ts` | `AssetsTableConfigResolver` —— 定义列与动作 |
| `asset.component.ts` | 资产详情表单（继承 `EntityComponent<Asset>`） |
| `asset-tabs.component.ts` | 资产 tab（属性、关系、事件、告警） |

**类型：** entity-table
**Resolver：** `AssetsTableConfigResolver`
**实体组件：** `AssetComponent`
**Tab 组件：** `AssetTabsComponent`

### asset-profile/

资产配置（类型）管理。

**类型：** entity-table
**Resolver：** `AssetProfilesTableConfigResolver`
**实体组件：** `AssetProfileComponent`
**Tab 组件：** `AssetProfileTabsComponent`

### audit-log/

审计日志查看器，支持按实体、用户、动作类型和时间范围过滤。

**类型：** custom（审计日志表格）

### calculated-fields/

用于遥测数据后处理的计算字段管理。

**类型：** custom（自定义表格组件）/ entity-details
**Resolver：** `CalculatedFieldsTableConfigResolver`

### customer/

客户管理。

**类型：** entity-table
**Resolver：** `CustomersTableConfigResolver`
**实体组件：** `CustomerComponent`
**Tab 组件：** `CustomerTabsComponent`

同时含客户下的用户、设备、资产、仪表板和 edge 的嵌套表格。

### dashboard/

仪表板管理。

| 文件 | 用途 |
|------|---------|
| `dashboards-table-config.resolver.ts` | `DashboardsTableConfigResolver` |
| `dashboard-form.component.ts` | 仪表板元信息表单 |
| `dashboard-tabs.component.ts` | 仪表板 tab |
| `import-dashboard-file-dialog.component.ts` | JSON 仪表板导入对话框 |
| `make-dashboard-public-dialog.component.ts` | 公开仪表板的 URL 分享 |
| `manage-dashboard-customers-dialog.component.ts` | 客户分配对话框 |

**类型：** entity-table / custom（查看时用 DashboardPageComponent）

### device/

设备管理。

| 文件 | 用途 |
|------|---------|
| `devices-table-config.resolver.ts` | `DevicesTableConfigResolver` |
| `device.component.ts` | 设备详情表单（名称、类型、配置、网关开关） |
| `device-tabs.component.ts` | 设备 tab（属性、遥测、关系、告警、事件） |
| `device-credentials-dialog.component.ts` | 设备凭证对话框（token、MQTT basic、X.509、LwM2M） |
| `device-check-connectivity-dialog.component.ts` | 设备连通性测试对话框 |

**类型：** entity-table
**Resolver：** `DevicesTableConfigResolver`
**实体组件：** `DeviceComponent`
**Tab 组件：** `DeviceTabsComponent`

### device-profile/

带传输配置的设备配置（类型）管理。

**类型：** entity-table
**Resolver：** `DeviceProfilesTableConfigResolver`
**实体组件：** `DeviceProfileComponent`
**Tab 组件：** `DeviceProfileTabsComponent`

### device-simulator/（本 fork 新增）

设备模拟器——一个独立的可视化遥测模拟器，用于配置并驱动模拟设备发数据，复用解决方案模板所用的
emulator JSON 格式。支持实时与历史两种模式。

| 文件 | 用途 |
|------|---------|
| `device-simulators.component.ts` | 模拟器列表与启停控制 |
| `device-simulator-dialog.component.ts` | 模拟器配置对话框 |
| `value-strategy-editor.component.ts` | 取值策略编辑器（如随机、正弦、递增） |

**类型：** custom

### deviceAlarms/（本 fork 新增）

Smart Lightning 功能下的设备告警列表（`deviceAlarms/list`）。

**类型：** custom（`SmartLightningAlarmListComponent`）

### edge/

ThingsBoard Edge 体系的 edge 实例管理。

**类型：** entity-table
**Resolver：** `EdgesTableConfigResolver`
**实体组件：** `EdgeComponent`
**Tab 组件：** `EdgeTabsComponent`

含分配给 edge 的实体（资产、设备、实体视图、仪表板、规则链）的嵌套表格。

### entities/

实体区块的路由模块，把设备、资产、实体视图和网关各 tab 归到 `/entities/` 下。

### entity-group/（本 fork 新增）

实体分组与客户层级——移植自 PE 的实体组体系。

| 文件 | 用途 |
|------|---------|
| `customer-hierarchy.component.ts` | 客户层级树视图 |
| `entity-group-dialog.component.ts` | 实体组的新建/编辑对话框 |
| `entity-group-columns-tab.component.ts` | 组的列配置 tab |
| `entity-group-actions-tab.component.ts` | 组的动作配置 tab |
| `add-group-entities-dialog.component.ts` | 向组内添加实体的对话框 |
| `entity-group-details-config.util.ts` | 组详情抽屉的 tab 装配 |

**类型：** entity-table / custom

### entity-view/

实体视图（虚拟实体）管理。

**类型：** entity-table
**Resolver：** `EntityViewsTableConfigResolver`
**实体组件：** `EntityViewComponent`
**Tab 组件：** `EntityViewTabsComponent`

### features/

高级功能区块的路由，覆盖 OTA 升级与版本控制。

### gateways/

网关连通性仪表板。

**类型：** dashboard-view
**仪表板：** 加载网关状态仪表板

### help-center/（本 fork 新增）

帮助中心——内置的帮助文章浏览与阅读。

| 文件 | 用途 |
|------|---------|
| `help-article.component.ts` | 单篇帮助文章的渲染 |
| `help-articles.data.ts` | 帮助文章的内容数据 |

**类型：** custom

### home-links/

含快捷卡片与仪表板 resolver 的首页。定义侧边栏导航菜单结构，并懒加载全部页面模块。

**类型：** custom（HomeLinksComponent）

### integration/（本 fork 新增）

集成中心（Integration Center）——移植自 PE，覆盖全部 29 种集成协议类型，以及上行/下行数据转换器。

| 文件 | 用途 |
|------|---------|
| `integration/` | 集成列表、详情与编辑 |
| `converter/` | 上行/下行转换器（含 decoder/encoder 编辑器与测试对话框） |
| `integration-details-panel.component.ts` | 集成详情的侧边面板 |
| `integration-entity-details-page.component.ts` | 集成的整页详情视图 |

**类型：** entity-table / custom

### mobile/

移动应用中心。

| 子页 | 类型 | 用途 |
|----------|------|---------|
| `bundles/` | entity-table | 移动应用包管理（`MobileBundleTableConfigResolver`） |
| `applications/` | entity-table | 移动应用管理（`MobileAppTableConfigResolver`） |
| `qr-code-widget/` | settings-form | 二维码 widget 配置 |

### notification/

通知中心，含收件箱、已发送、收件人、模板和规则。

| 子页 | 类型 | Resolver |
|----------|------|----------|
| `inbox/` | entity-table | `InboxTableConfigResolver` |
| `sent/` | entity-table | `SentTableConfigResolver` |
| `recipient/` | entity-table | `RecipientTableConfigResolver` |
| `template/` | entity-table | `TemplateTableConfigResolver` |
| `rule/` | entity-table | `RuleTableConfigResolver` |

### ota-update/

OTA（空中升级）固件/软件包管理。

**类型：** entity-table
**Resolver：** `OtaUpdateTableConfigResolve`
**实体组件：** `OtaUpdateComponent`
**Tab 组件：** `OtaUpdateTabsComponent`

### profile/

用户资料页（账号区块的一部分）。可编辑的资料信息。

**类型：** settings-form
**组件：** `ProfileComponent`

### profiles/

配置区块的路由，把设备配置与资产配置归到 `/profiles/` 下。

### reporting/（本 fork 新增）

报表引擎——移植自 PE，支持 PDF/CSV 输出、图表、仪表板嵌入和定时生成。

| 子页 | 用途 |
|----------|---------|
| `report/` | 报表列表与生成对话框 |
| `report-template/` | 报表模板列表、创建对话框与可视化编辑器 |
| `schedule/` | 报表的排程配置 |

**类型：** entity-table / custom

### rulechain/

带可视化编辑器的规则链管理。

| 文件 | 用途 |
|------|---------|
| `rulechains-table-config.resolver.ts` | `RuleChainsTableConfigResolver` |
| `rulechain.component.ts` | 规则链详情表单 |
| `rulechain-tabs.component.ts` | 规则链 tab |
| `rulechain-page.component.ts` | 可视化规则链编辑器（拖拽式节点图） |
| `rulechain-page.module.ts` | 编辑器的懒加载模块 |
| `rulenode.component.ts` | 规则节点的可视化表示 |
| `rule-node-details.component.ts` | 规则节点详情/配置面板 |
| `rule-node-config.component.ts` | 单节点的配置表单 |
| `rule-node-link.component.ts` | 节点间连线 |
| `link-labels.component.ts` | 连线标签展示 |
| `rulenote.component.ts` | 画布上的注释便签 |
| `add-rule-node-dialog.component.ts` | 添加规则节点的对话框（类型选择） |
| `add-rule-node-link-dialog.component.ts` | 添加节点间连线的对话框 |

**类型：** entity-table / custom（可视化编辑器）
**Resolver：** `RuleChainsTableConfigResolver`

### scada-symbol/

SCADA 符号编辑器，用 SVG 创建与编辑 SCADA 符号。

**类型：** custom（`ScadaSymbolComponent`）

### security/

账号安全页（账号区块的一部分）。管理 2FA、改密码和会话管理。

**类型：** settings-form
**组件：** `SecurityComponent`

### smart-lightning/（本 fork 新增）

智慧照明——一套面向具体场景的功能区块，含专用仪表板、设备监控和查询视图。

| 子页 | 用途 |
|----------|---------|
| `dashboard/` | 智慧照明仪表板，含地图配置与周期切换器 |
| `monitoring/` | 单灯设备详情监控 |
| `query/` | 查询视图 |
| `smart-lightning-root-asset-filter.component.ts` | 根资产过滤器 |

**类型：** custom

### solution-templates/（本 fork 新增）

解决方案模板——移植自 PE，支持浏览模板、一键安装/卸载，并自带遥测模拟器。

| 文件 | 用途 |
|------|---------|
| `solution-templates.component.ts` | 模板浏览列表 |
| `solution-template-card.component.ts` | 单个模板卡片 |
| `solution-template-details.component.ts` | 模板详情 |
| `solution-install-dialog.component.ts` | 安装/卸载对话框 |

**类型：** custom

### tenant/

租户管理（仅 SYS_ADMIN）。

**类型：** entity-table
**Resolver：** `TenantsTableConfigResolver`
**实体组件：** `TenantComponent`
**Tab 组件：** `TenantTabsComponent`

### tenant-profile/

租户配置管理（仅 SYS_ADMIN）。

**类型：** entity-table
**Resolver：** `TenantProfilesTableConfigResolver`
**实体组件：** `TenantProfileComponent`
**Tab 组件：** `TenantProfileTabsComponent`

### user/

用户管理。

**类型：** entity-table
**Resolver：** `UsersTableConfigResolver`
**实体组件：** `UserComponent`
**Tab 组件：** `UserTabsComponent`

用户可在系统级（SYS_ADMIN）与租户级（TENANT_ADMIN）两处管理，并带相应的权限过滤。

### vc/

版本控制（实体版本管理）。提供提交历史、分支管理和实体版本对比。

**类型：** custom（`VersionControlComponent`）

### widget/

Widget 库管理。

| 子页 | 类型 | 用途 |
|----------|------|---------|
| Widget 类型 | entity-table | `WidgetTypesTableConfigResolver` |
| Widget 包 | entity-table | `WidgetsBundlesTableConfigResolver` |
| Widget 包详情 | custom | 包内容列表（`WidgetsBundleWidgetsComponent`） |
| Widget 编辑器 | custom | 可视化 widget 编辑器（`WidgetEditorComponent`） |

## Login 模块

Login 模块（`src/app/modules/login/`）处理未认证访问：

| 组件 | 用途 |
|-----------|---------|
| `LoginComponent` | 用户名/密码登录表单 |
| `ResetPasswordRequestComponent` | 请求发送密码重置邮件 |
| `ResetPasswordComponent` | 设置新密码（经重置链接进入） |
| `CreatePasswordComponent` | 创建初始密码（经激活链接进入） |
| `TwoFactorAuthLoginComponent` | 2FA 验证码校验 |

## 可复用的 home 组件

`home/components/` 目录含被多个页面模块共用的框架组件：

| 组件 | 用途 |
|-----------|---------|
| `tb-entities-table` | 主实体表格（列、排序、分页、搜索、动作） |
| `tb-entity-details-panel` | 实体详情的滑出抽屉 |
| `tb-entity-details-page` | 整页实体详情视图 |
| `tb-add-entity-dialog` | 通用的新增实体对话框 |
| `tb-entity-filter` | 实体数据过滤组件 |
| `tb-entity-chips` | 实体引用的 chip 展示 |
| `tb-alarm-table` | 告警列表表格 |
| `tb-alarm-details-dialog` | 告警详情对话框 |
| `tb-audit-log-table` | 审计日志表格 |
| `tb-attribute-table` | 实体属性的键值表格 |
| `tb-relation-table` | 实体关系表格 |
| `tb-event-table` | 实体事件表格 |
| `tb-dashboard` | 仪表板渲染器 |
| `tb-dashboard-page` | 带工具栏的仪表板页 |
| `tb-edit-widget` | Widget 编辑器 |
| `tb-router-tabs` | 经路由实现的 tab 导航 |
| `tb-device-credentials` | 设备凭证表单 |
| `tb-details-panel` | 详情面板基类 |

## 关联

- [前端总览](README.zh-CN.md) —— 前端整体架构与实体表格模式
- [Core 模块](core.zh-CN.md) —— 页面消费的 HTTP service 与路由 guard
- [Shared 模块](shared.zh-CN.md) —— 页面使用的共享组件（实体选择、对话框、时间选择器）
- [主题与 i18n](theme-i18n.zh-CN.md) —— 所有页面都使用主题与翻译体系
