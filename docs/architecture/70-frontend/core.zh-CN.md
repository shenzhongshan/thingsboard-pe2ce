# Core 模块

**本文是权威版。** 英文版 [core.md](core.md) 为对照上游 CE 的参照副本，改动请先落在本文。

Core 模块（`src/app/core/`）含单例 service、状态管理、HTTP 拦截器、路由 guard 和认证逻辑。它在
`AppModule` 中只被导入一次，并提供贯穿整个应用使用的 service。

## 模块结构

```
src/app/core/
├── api/                    # WebSocket-based data subscription APIs
├── auth/                   # Authentication state (NgRx) and service
├── css/                    # CSS utility constants
├── guards/                 # Route guards (auth, unsaved changes)
├── http/                   # HTTP services (44 entity services)
├── interceptors/           # HTTP interceptors (auth token, loading, conflict)
├── local-storage/          # Browser localStorage wrapper
├── meta-reducers/          # NgRx meta-reducers (debug, localStorage sync)
├── notification/           # In-app notification state (NgRx)
├── operator/               # RxJS custom operators
├── services/               # Application-wide services (menu, time, dialog, etc.)
├── settings/               # User settings state (NgRx)
├── translate/              # i18n translation loader, compiler, parser
├── ws/                     # WebSocket services
├── core.module.ts          # CoreModule definition
├── core.state.ts           # Core NgRx state interface
├── public-api.ts           # Public API barrel
└── utils.ts                # General utility functions
```

## api/ —— WebSocket 数据订阅

为 widget 与仪表板提供经 WebSocket 的实时数据流。该 API 层管理对实体遥测与告警数据的订阅。

| 文件 | 用途 |
|------|---------|
| `entity-data.service.ts` | 订阅实体遥测/属性数据的主 service |
| `entity-data-subscription.ts` | 实体数据流的订阅管理 |
| `alarm-data.service.ts` | 订阅告警数据流的 service |
| `alarm-data-subscription.ts` | 告警流的订阅管理 |
| `data-aggregator.ts` | 按时间窗聚合遥测数据 |
| `alias-controller.ts` | 把实体别名解析成实际的实体 ID |
| `widget-subscription.ts` | widget 级的订阅包装 |
| `widget-api.models.ts` | widget API 的 TypeScript 接口 |
| `public-api.ts` | 公开 API 的汇总导出 |

## auth/ —— 认证

经 NgRx store 管理登录、登出、JWT token 处理、OAuth2 和双因子认证。

| 文件 | 用途 |
|------|---------|
| `auth.service.ts` | 认证 service：登录、登出、token 刷新、OAuth2、2FA |
| `auth.reducer.ts` | 认证状态 reducer（isAuthenticated、用户详情、token） |
| `auth.actions.ts` | 认证操作的 NgRx action |
| `auth.effects.ts` | 副作用：token 持久化、登录/登出后的跳转 |
| `auth.selectors.ts` | 查询认证状态的 selector |
| `auth.models.ts` | 认证相关接口（登录请求/响应） |
| `public-api.ts` | 公开 API 的汇总导出 |

**认证流程：**
1. 用户提交登录表单 --> AuthService.login()
2. 后端校验并返回 JWT token
3. Token 存入 AuthState（NgRx）与 localStorage
4. AuthHttpInterceptor 把 token 附加到所有 HTTP 请求上
5. Token 过期时，拦截器触发 token 刷新或跳回登录页

**OAuth2 与 2FA：** 该认证 service 同时处理 OAuth2 登录流程（跳转到 provider、回调处理）和双因子
认证（TOTP、备用码）。

## guards/ —— 路由 guard

| 文件 | 用途 |
|------|---------|
| `auth.guard.ts` | 阻止未认证访问并跳转登录页。检查 JWT 有效性与用户 authority。 |
| `confirm-on-exit.guard.ts` | 离开含未保存改动的表单时弹确认框。被 settings-form 类页面使用。 |

> **`auth.guard.ts` 只读最深层被激活路由的 `data.auth`。** 父级分组路由上的 `data.auth` 不参与
> 判断，所以调整页面权限必须改叶子路由。

## http/ —— HTTP service

44 个 service 文件，为每种实体类型提供带类型的 HTTP 方法。这些 service 遵循统一的基础模式：注入
`HttpClient`，并从一个基路径拼出 API URL。

| Service | 实体/领域 |
|---------|------------|
| `device.service.ts` | 设备 CRUD、凭证、遥测、属性 |
| `device-profile.service.ts` | 设备配置 CRUD、传输配置 |
| `asset.service.ts` | 资产 CRUD、遥测、属性 |
| `asset-profile.service.ts` | 资产配置 CRUD |
| `entity-view.service.ts` | 实体视图 CRUD |
| `customer.service.ts` | 客户 CRUD |
| `user.service.ts` | 用户 CRUD、激活 |
| `tenant.service.ts` | 租户 CRUD（SYS_ADMIN） |
| `tenant-profile.service.ts` | 租户配置 CRUD |
| `dashboard.service.ts` | 仪表板 CRUD、发布给客户 |
| `rule-chain.service.ts` | 规则链 CRUD、导入/导出、测试脚本 |
| `edge.service.ts` | Edge 实例 CRUD、同步 |
| `alarm.service.ts` | 告警查询、确认、清除 |
| `alarm-comment.service.ts` | 告警评论 CRUD |
| `alarm-rules.service.ts` | 告警规则 / 告警用计算字段 |
| `ota-package.service.ts` | OTA 包 CRUD、固件上传 |
| `calculated-fields.service.ts` | 计算字段 CRUD |
| `widget.service.ts` | Widget 类型与 widget 包 CRUD |
| `resource.service.ts` | 资源 CRUD、下载 |
| `queue.service.ts` | 队列配置（SYS_ADMIN） |
| `ai-model.service.ts` | AI 模型 CRUD |
| `notification.service.ts` | 通知 CRUD、收件箱 |
| `oauth2.service.ts` | OAuth2 客户端/域名 CRUD |
| `domain.service.ts` | 域名 CRUD |
| `mobile-app.service.ts` | 移动应用 CRUD |
| `entity.service.ts` | 通用实体操作（按 ID 查、计数等） |
| `entity-relation.service.ts` | 实体关系 CRUD |
| `attribute.service.ts` | 实体属性 CRUD |
| `event.service.ts` | 实体事件查询 |
| `audit-log.service.ts` | 审计日志查询 |
| `admin.service.ts` | 系统/管理设置（邮件、短信、通用、安全） |
| `api-key.service.ts` | API key 管理 |
| `image.service.ts` | 图库上传/下载 |
| `component-descriptor.service.ts` | 规则节点组件描述符 |
| `entities-version-control.service.ts` | 实体版本控制 |
| `two-factor-authentication.service.ts` | 2FA 账号设置 |
| `user-settings.service.ts` | 用户偏好设置 |
| `usage-info.service.ts` | API 用量统计 |
| `trendz-settings.service.ts` | Trendz 分析设置 |
| `git-hub.service.ts` | GitHub 集成 |
| `mobile-application.service.ts` | 移动应用设置 |
| `ui-settings.service.ts` | UI 偏好 |
| `http-utils.ts` | HTTP 工具函数（URL 拼装、参数编码） |
| `public-api.ts` | 公开 API 的汇总导出 |

**模式：** 每个 service 拼出形如 `/api/devices/${deviceId}` 的 API 路径，并返回带类型的
Observable。一个典型 service 方法的模式示例：

```typescript
getDevice(deviceId: string): Observable<Device> {
  return this.http.get<Device>(`/api/device/${deviceId}`, defaultHttpOptionsFromConfig(config));
}
```

## interceptors/ —— HTTP 拦截器

| 文件 | 用途 |
|------|---------|
| `global-http-interceptor.ts` | 主拦截器，向下委派给各类子拦截器。负责注入认证 token、错误处理、加载状态。 |
| `interceptor-config.ts` | 拦截器的配置模型（ignoreLoading、ignoreErrors 等） |
| `interceptor-http-params.ts` | 带拦截器配置标志的自定义 HttpParams |
| `interceptor.util.ts` | 拦截器工具函数 |
| `entity-conflict.interceptor.ts` | 检测 409 Conflict 响应并打开实体冲突解决对话框 |
| `load.actions.ts` | 加载状态的 NgRx action |
| `load.reducer.ts` | 加载状态 reducer |
| `load.selectors.ts` | 加载状态的 selector |
| `load.models.ts` | 加载状态模型 |

全局拦截器链：
1. **注入认证 token** —— 把状态中的 JWT 附到 Authorization 头
2. **加载指示** —— 为长耗时请求派发加载状态变更
3. **错误处理** —— 处理 HTTP 错误、弹出 toast 提示
4. **实体冲突** —— 检测 409 错误并打开解决对话框

## services/ —— 应用级 service

| 文件 | 用途 |
|------|---------|
| `menu.service.ts` | 按用户 authority 构建侧边栏导航菜单 |
| `menu.models.ts` | 菜单项接口（分区、页面、链接） |
| `time.service.ts` | 管理时间窗状态（实时/最近 N/时间区间） |
| `dialog.service.ts` | 对话框打开与设置管理 |
| `toast-notification.service.ts` | Toast 提示展示（成功、错误、警告、信息） |
| `broadcast.service.ts` | 应用级事件总线，用于跨组件通信 |
| `broadcast.models.ts` | 广播事件类型模型 |
| `utils.service.ts` | 通用工具函数（深拷贝、guid 等） |
| `window.service.ts` | Window/DOM 工具 service，带 SSR 安全检查 |
| `item-buffer.service.ts` | 剪贴板式缓冲，用于在页面间复制实体 |
| `help.service.ts` | 内联帮助内容管理 |
| `dynamic-component-factory.service.ts` | 运行期动态创建组件 |
| `resources.service.ts` | 资源文件加载（CSS、JS） |
| `title.service.ts` | 页面标题管理 |
| `mobile.service.ts` | 移动端专属的 UI 调整 |
| `dashboard-utils.service.ts` | 仪表板工具函数 |
| `calculated-field-form.service.ts` | 计算字段表单构建器 |
| `active-component.service.ts` | 跟踪当前活跃组件 |
| `unit.service.ts` | 度量单位换算与格式化 |
| `raf.service.ts` | requestAnimationFrame 包装 |
| `script/node-script-test.service.ts` | 规则节点脚本测试 |
| `public-api.ts` | 公开 API 的汇总导出 |

## settings/ —— 用户设置（NgRx）

| 文件 | 用途 |
|------|---------|
| `settings.reducer.ts` | 设置状态 reducer（语言、用户偏好） |
| `settings.actions.ts` | 设置的 NgRx action |
| `settings.effects.ts` | 设置的副作用 |
| `settings.selectors.ts` | 设置的 selector |
| `settings.models.ts` | 设置状态模型 |
| `settings.utils.ts` | 设置工具函数 |

## notification/ —— 应用内通知状态（NgRx）

| 文件 | 用途 |
|------|---------|
| `notification.reducer.ts` | 通知状态 reducer |
| `notification.actions.ts` | 通知的 NgRx action |
| `notification.effects.ts` | 通知的副作用 |
| `notification.models.ts` | 通知模型 |

## ws/ —— WebSocket service

| 文件 | 用途 |
|------|---------|
| `websocket.service.ts` | WebSocket 基础 service：连接管理、认证、重连、消息派发 |
| `telemetry-websocket.service.ts` | 遥测 WebSocket：订阅实体时序数据以实时刷新仪表板 |
| `notification-websocket.service.ts` | 通知 WebSocket：订阅实时通知事件与计数更新 |
| `public-api.ts` | 公开 API 的汇总导出 |

该 WebSocket service 用 JWT token 维持一条已认证的连接，支持带指数退避的自动重连。消息按订阅 ID
路由，派发给相应的订阅处理器。

## translate/ —— i18n 翻译

| 文件 | 用途 |
|------|---------|
| `translate-default-loader.ts` | 从 `/assets/locale/` 加载语言 JSON 文件 |
| `translate-default-compiler.ts` | 编译 ICU MessageFormat 翻译串 |
| `translate-default-parser.ts` | 解析带参数插值的翻译串 |
| `missing-translate-handler.ts` | 处理缺失的翻译 key（记警告，返回该 key） |

> **语言文件里不能出现字面量 `{...}`。** ICU 编译器会把花括号当成消息格式语法，字面花括号会让
> 该串在每次渲染时抛异常。需要占位时用 `{{param}}` 插值形式。改完 i18n 文件后要读一遍浏览器
> 控制台。

## local-storage/ —— 浏览器存储

| 文件 | 用途 |
|------|---------|
| `local-storage.service.ts` | 对 `window.localStorage` 的带类型包装，含 JSON 序列化 |

## meta-reducers/ —— NgRx meta-reducer

| 文件 | 用途 |
|------|---------|
| `init-state-from-local-storage.reducer.ts` | 应用启动时从 localStorage 还原 NgRx 初始状态 |
| `debug.reducer.ts` | 记录 NgRx action 与状态变化（仅开发期） |

## 与其他模块的关系

- [Shared 模块](shared.zh-CN.md) —— core service 使用的共享组件与模型
- [页面模块](pages.zh-CN.md) —— 页面模块注入 core 的 HTTP service 并使用 core 的 guard
- [主题与 i18n](theme-i18n.zh-CN.md) —— core 的 translate 模块加载语言数据
