# 前端架构

**本文是权威版。** 英文版 [README.md](README.md) 为对照上游 CE 的参照副本，改动请先落在本文。

ThingsBoard 前端是一个基于 Angular 20 与 Angular Material 20 构建的单页应用。它为管理 IoT 设备、
资产、仪表板、规则链、告警以及所有其他 ThingsBoard 实体提供了完整的 UI。

前端经 REST API 与 WebSocket 连接和后端通信，使用 JWT token 做认证。

## 技术栈

| 技术 | 版本 | 用途 |
|-----------|---------|---------|
| Angular | 20.3.x | 前端框架 |
| Angular Material | 20.2.x | UI 组件库（Material Design） |
| TypeScript | 5.9.x | 主语言 |
| RxJS | 7.8.x | 响应式编程、异步数据流 |
| NgRx Store | 20.1.x | 状态管理（Redux 模式） |
| @ngx-translate/core | 17.x | 国际化 |
| @messageformat/core | 3.4.x | ICU 消息格式支持 |
| Tailwind CSS | 3.4.x | 原子化优先的 CSS 框架 |
| ESLint | 9.x | 代码检查（配 Angular ESLint + TypeScript ESLint） |
| Karma + Jasmine | —— | 单元测试 |

> **本仓没有可运行的前端测试。** `package.json` 里没有 `"test"` 脚本，也没有配 Karma/Jest/Cypress
> 的 runner。虽存在一个 `auth.service.spec.ts`，但它无法被执行。前端改动只能靠构建加浏览器实跑
> 来验证。

## 构建与开发

```bash
# 安装依赖
cd ui-ngx && yarn install

# 开发服务器，http://localhost:4200
yarn start

# 生产构建
yarn build:prod

# 类型生成（protobuf/API 类型）
yarn build:types

# 代码检查
yarn lint
```

开发服务器会把 API 请求代理到运行中的后端（默认 `http://localhost:8080`）。

## 模块组织

```
src/app/
├── core/                       # Singleton services, guards, interceptors
│   ├── api/                    # WebSocket-based data subscription APIs
│   ├── auth/                   # Authentication (JWT, OAuth2, 2FA)
│   ├── guards/                 # Route guards
│   ├── http/                   # HTTP services (one per entity type)
│   ├── interceptors/           # HTTP interceptors
│   ├── local-storage/          # Browser localStorage wrapper
│   ├── meta-reducers/          # NgRx meta-reducers
│   ├── notification/           # In-app notification state (NgRx)
│   ├── services/               # Core services (menu, time, dialog, etc.)
│   ├── settings/               # User settings state (NgRx)
│   ├── translate/              # i18n translation services
│   ├── ws/                     # WebSocket services
│   └── core.module.ts          # Core module definition
│
├── modules/
│   ├── home/                   # Main authenticated application
│   │   ├── components/          # Reusable home components
│   │   │   ├── entity/          # Entities table, entity details, entity form base
│   │   │   ├── alarm/           # Alarm table, alarm details
│   │   │   ├── dashboard/       # Dashboard viewer, toolbar
│   │   │   ├── widget/          # Widget editor
│   │   │   └── ...
│   │   └── pages/              # Page modules (32 page directories)
│   └── login/                  # Login/reset password/2FA pages
│
└── shared/
    ├── components/              # Shared UI components (20+ groups)
    ├── models/                  # TypeScript interfaces, enums, constants
    ├── pipe/                    # Custom pipes
    ├── directives/              # Custom directives
    ├── decorators/              # Custom decorators
    ├── services/                # Shared utility services
    ├── import-export/           # Data import/export utilities
    ├── adapter/                 # Custom adapters
    └── animations/              # Animation definitions
```

## 路径别名

定义在 `tsconfig.json`：

| 别名 | 映射到 |
|-------|---------|
| `@app/*` | `src/app/*` |
| `@env/*` | `src/environments/*` |
| `@core/*` | `src/app/core/*` |
| `@modules/*` | `src/app/modules/*` |
| `@home/*` | `src/app/modules/home/*` |
| `@shared/*` | `src/app/shared/*` |

导入示例：`import { DeviceService } from '@core/http/device.service';`

## 关键依赖

### 核心框架

- `@angular/core`、`@angular/common`、`@angular/router`、`@angular/forms`
- `@angular/material`、`@angular/cdk` —— Material Design 组件
- `@angular/animations` —— 动画支持

### 状态管理

- `@ngrx/store`、`@ngrx/effects`、`@ngrx/store-devtools` —— Redux 式状态管理
- 认证状态（`auth.reducer.ts`）、设置状态（`settings.reducer.ts`）、通知状态
  （`notification.reducer.ts`）、加载状态（`load.reducer.ts`）

### i18n

- `@ngx-translate/core`、`ngx-translate-messageformat-compiler` —— 支持 ICU 消息格式的翻译
- `@messageformat/core` —— ICU 消息格式化

### 可视化

- `echarts` —— 图表（ThingsBoard 定制分支，版本 5.5.2-TB）
- `@svgdotjs/svg.js` —— SCADA 符号的 SVG 操作
- `canvas-gauges` —— 模拟仪表盘 widget
- `leaflet` —— 地图 widget
- `maplibre-gl` —— MapLibre GL 地图渲染
- `flot` —— 基于 jQuery 的绘图（遗留）

### 编辑器组件

- `ace-builds` —— 规则节点脚本的代码编辑器
- `tinymce` —— 富文本编辑器
- `ngx-markdown`、`marked` —— Markdown 渲染
- `angular-gridster2` —— 仪表板网格布局

### 工具库

- `dayjs`、`moment`、`moment-timezone` —— 日期/时间处理
- `rxjs` —— 响应式扩展
- `jszip` —— ZIP 文件生成
- `html2canvas` —— 截图抓取
- `qrcode` —— 二维码生成
- `screenfull` —— 全屏 API 包装
- `split.js` —— 可调整大小的分栏面板
- `libphonenumber-js` —— 电话号码校验
- `tinycolor2` —— 颜色处理

## 实体表格模式

ThingsBoard 的大多数页面都遵循一套标准化的**实体表格模式**，每种实体类型对应三个文件：

```
pages/<entity>/
├── <entity>-table-config.resolver.ts   # Column definitions, actions, component wiring
├── <entity>.component.ts + .html       # Entity detail form (extends EntityComponent<T>)
└── <entity>-tabs.component.ts + .html  # Additional detail tabs
```

### 框架组件

| 组件 | 文件 | 用途 |
|-----------|------|---------|
| `tb-entities-table` | `home/components/entity/entities-table.component.ts` | 通用表格：渲染列、动作、分页、搜索、行选择 |
| `tb-entity-details-page` | `home/components/entity/entity-details-page.component.ts` | 带 tab 的整页实体详情 |
| `tb-entity-details-panel` | `home/components/entity/entity-details-panel.component.ts` | 实体详情的滑出抽屉 |
| `EntityComponent<T>` | `home/components/entity/entity.component.ts` | 所有实体表单继承的抽象基 directive |
| `TableConfigResolver` | —— | 表格配置提供方的接口 |

### Resolver 模式

每种实体类型都有一个 `TableConfigResolver`，返回一个 `EntityTableConfig<T>`，其中含：

- `entityType` —— 枚举值（如 `EntityType.DEVICE`）
- `columns` —— 列定义数组（表头 key、单元格属性/模板、宽度、排序）
- `cellActionDescriptors` —— 行级动作按钮（编辑、删除等）
- `groupActionDescriptors` —— 批量选择的动作按钮
- `headerActionDescriptors` —— 工具栏动作按钮（新增、导入、刷新）
- `entityComponent` —— 详情表单组件类的引用
- `entityTabsComponent` —— tab 组件类的引用
- `addDialogComponent` —— 可选，新增/创建流程的对话框

### 实体详情表单的生命周期

所有实体详情表单都继承 `EntityComponent<T>`，并遵循如下生命周期：

1. **`buildForm(entity: T): UntypedFormGroup`** —— 在构造函数中调用。创建含控件、校验器和初始值
   的响应式表单。
2. **`updateForm(entity: T): void`** —— 在实体输入变化时调用。用 `patchValue()` 填充表单而不触发
   变更事件。
3. **`updateFormState(): void`** —— 在 `isEdit` 变化时调用，启用或禁用表单。
4. **`entityFormValue(): any`** —— 返回 `form.getRawValue()`（含被禁用的字段），再经
   `prepareFormValue()` 处理。
5. **保存** —— 合并 `{...this.entity, ...this.entityFormValue()}` 后调用 HTTP service。

表单用 `[fieldset [disabled]="isEntityReadonly"]` 实现只读的视觉效果，并用 `entityForm.disable()`
做程序上的强制。

> **用 `@if` / `*ngIf` 条件显示的控件，不要在模板里写 `required` 属性。** 控件被销毁时
> `required` 校验器会残留在 FormGroup 上，使表单永久 invalid 且该错误不可见——表现为"点保存
> 没反应、也没有报错"。要在条件分支里改用 `setValidators()` / `clearValidators()` 显式管理。

## WebSocket 通信

前端维持长连的 WebSocket 以获取实时更新：

| Service | 文件 | 用途 |
|---------|------|---------|
| `TelemetryWebsocketService` | `core/ws/telemetry-websocket.service.ts` | 仪表板的实时遥测数据订阅 |
| `NotificationWebsocketService` | `core/ws/notification-websocket.service.ts` | 实时通知更新 |
| `WebSocketService` | `core/ws/websocket.service.ts` | WebSocket 连接管理基类 |

## 路由架构

路由分两层定义：
- **顶层路由**（`app-routing.module.ts`）—— 登录 对 home（已认证）
- **home 路由**（`modules/home/pages/home-links/`）—— 懒加载的功能模块

`home-links/` 模块定义侧边栏导航菜单，并按用户 authority（SYS_ADMIN、TENANT_ADMIN、
CUSTOMER_USER）懒加载页面模块。

> **CE 的 `AuthGuard` 只检查最深层被激活路由的 `data.auth`，不检查每一级祖先路由。** 要给某个
> 页面收紧或放开权限，必须改叶子路由，改父级的分组路由不生效。

## 关联

- [Core 模块](core.zh-CN.md) —— 单例 service、guard、拦截器、认证
- [Shared 模块](shared.zh-CN.md) —— 可复用组件、模型、pipe、directive
- [页面模块](pages.zh-CN.md) —— 全部 32 个页面模块及其结构
- [主题与 i18n](theme-i18n.zh-CN.md) —— SCSS 主题、Tailwind 集成、28 种语言
- [核心应用](../50-application/README.zh-CN.md) —— 前端消费的后端 REST API
- [支撑模块](../60-supporting/README.zh-CN.md) —— web-ui Docker 镜像打包
