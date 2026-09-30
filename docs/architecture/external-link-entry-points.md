# ThingsBoard 外部链接入口点分析

## 一、概述

本文档记录 ThingsBoard 系统中所有指向外部域名的链接、API 调用和集成端点。按功能域分类，标注每个链接的用途、触发方式和安全影响。

---

## 二、文档与帮助链接

### 2.1 帮助系统基地址

| 位置 | URL | 说明 |
|------|-----|------|
| `ui-ngx/src/app/shared/models/constants.ts:90` | `https://allshen.org` | 前端帮助链接基础 URL（`[tb-help]` 已始终隐藏，不再可从 UI 触发，见下） |
| `application/src/main/resources/thingsboard.yml:297` | 默认 `/assets`（本地内置离线帮助文档），可通过 `UI_HELP_BASE_URL` 覆盖为外部 URL；外部 URL 不可用时前端自动回退到 `/assets` | 后端 `ui.help.base-url`，供 `HelpService` 加载帮助弹窗 Markdown 内容 |
| `ui-ngx/src/app/shared/components/help.component.ts` | 已禁用（组件恒不渲染） | 原用于 `window.open(helpUrl, '_blank')` 打开外部帮助页的按钮，现始终隐藏，不再发起该请求 |

### 2.2 首页/仪表板文档链接

**Getting Started 部件** (`getting-started-widget.component.html`):
| URL | 说明 |
|-----|------|
| `https://allshen.org/docs/user-guide/ui/tenants/` | 租户管理 |
| `https://allshen.org/docs/user-guide/ui/mail-settings/` | 邮件设置 |
| `https://allshen.org/docs/user-guide/ui/sms-provider-settings/` | 短信设置 |
| `https://allshen.org/docs/user-guide/two-factor-authentication/` | 双因素认证 |
| `https://allshen.org/docs/user-guide/oauth-2-support/` | OAuth2 |
| `https://allshen.org/docs/user-guide/ui/slack-settings/` | Slack 设置 |
| `https://allshen.org/docs/getting-started-guides/helloworld/` | 快速入门 |
| `https://allshen.org/docs/user-guide/alarm-rules/` | 告警规则 |

**Doc Links 部件** (`doc-links-widget.component.ts`):
| URL | 说明 |
|-----|------|
| `https://allshen.org/docs/getting-started-guides/helloworld/` | 入门指南 |
| `https://allshen.org/docs/user-guide/tenant-profiles/` | 租户配置 |
| `https://allshen.org/docs/api/` | API 文档 |
| `https://allshen.org/docs/user-guide/ui/widget-library/` | 部件库 |
| `https://allshen.org/docs/user-guide/rule-engine-2-0/re-getting-started/` | 规则引擎 |
| `https://allshen.org/docs/user-guide/device-profiles/` | 设备配置 |

### 2.3 设备连接检查对话框

**文件**: `device-check-connectivity-dialog.component.html`
| URL | 说明 |
|-----|------|
| `https://allshen.org/docs/reference/mqtt-sparkplug-api/` | MQTT Sparkplug |
| `https://allshen.org/docs/reference/mqtt-api/` | MQTT API |
| `https://allshen.org/docs/user-guide/mqtt-over-ssl/` | MQTT SSL |
| `https://allshen.org/docs/user-guide/ssl/coap-access-token/` | CoAP SSL |
| `https://allshen.org/docs/user-guide/ssl/coap-x509-certificates/` | CoAP X.509 |
| `https://allshen.org/docs/reference/snmp-api/` | SNMP API |
| `https://allshen.org/docs/reference/lwm2m-api/` | LwM2M API |

### 2.4 规则引擎节点内置文档链接

每个规则引擎节点的 Java 文件中都有 `docUrl` 字段，格式为 `https://allshen.org/docs/user-guide/rule-engine-2-0/nodes/{node-type}/`，共 40+ 个节点。例如：
- `TbTransformMsgNode.java` — 变换节点
- `TbSendRPCRequestNode.java` — RPC 请求节点
- `TbSendEmailNode.java` — 邮件节点
- `TbSlackNode.java` — Slack 节点
- `TbAiNode.java` — AI 请求节点
- 等等

---

## 三、GitHub 集成

### 3.1 前端 GitHub 入口

| 位置 | URL | 说明 |
|------|-----|------|
| `github-badge.component.html:21` | `https://github.com/thingsboard/thingsboard` | GitHub Badge Star 链接 |
| `git-hub.service.ts:33` | `https://api.github.com/repos/thingsboard/thingsboard` | REST API 获取 Star 数量 |

### 3.2 GitHub OAuth2

**文件**: `oauth2_config_templates/github_config.json`
| URL | 说明 |
|-----|------|
| `https://github.com/login/oauth/access_token` | Token 端点 |
| `https://github.com/login/oauth/authorize` | 授权端点 |
| `https://api.github.com/user` | 用户信息端点 |
| `https://docs.github.com/en/developers/apps/creating-an-oauth-app` | 帮助链接 |

**thingsboard.yml:193**: `https://api.github.com/user/emails` — GitHub OAuth2 邮箱映射

### 3.3 代码编辑器文档链接

ACE 编辑器中的类型提示跳转到 GitHub 源码：
- `service-completion.models.ts` — 30+ 个 GitHub blob 链接
- `widget-completion.models.ts` — 40+ 个 GitHub blob 链接
- `widget-editor.models.ts` — 部件上下文类型文档链接

### 3.4 网关仪表板同步

**thingsboard.yml:1464**: `https://github.com/thingsboard/gateway-management-extensions-dist.git` — Git 仓库同步

---

## 四、OAuth2 第三方登录

### 4.1 Google

| URL | 说明 |
|-----|------|
| `https://accounts.google.com/o/oauth2/v2/auth` | 授权端点 |
| `https://oauth2.googleapis.com/token` | Token 端点 |
| `https://www.googleapis.com/oauth2/v3/certs` | JWK 密钥集 |
| `https://openidconnect.googleapis.com/v1/userinfo` | 用户信息 |
| `https://developers.google.com/adwords/api/docs/guides/authentication` | 帮助 |

### 4.2 Facebook

| URL | 说明 |
|-----|------|
| `https://www.facebook.com/v2.8/dialog/oauth` | 授权端点 |
| `https://graph.facebook.com/v2.8/oauth/access_token` | Token 端点 |
| `https://graph.facebook.com/me?fields=id,name,first_name,last_name,email` | 用户信息 |
| `https://developers.facebook.com/docs/facebook-login/web#logindialog` | 帮助 |

### 4.3 Apple

| URL | 说明 |
|-----|------|
| `https://appleid.apple.com/auth/authorize?response_mode=form_post` | 授权端点 |
| `https://appleid.apple.com/auth/token` | Token 端点 |
| `https://appleid.apple.com/auth/keys` | JWK 密钥集 |
| `https://developer.apple.com/sign-in-with-apple/get-started/` | 帮助 |

---

## 五、地图服务

### 5.1 OpenStreetMap

| 文件 | URL | 说明 |
|------|-----|------|
| `map.models.ts:1193` | `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png` | 默认地图瓦片 |
| `thingsboard.yml` | `https://tile.openstreetmap.org` | CSP 白名单 |

### 5.2 ArcGIS

| 文件 | URL | 说明 |
|------|-----|------|
| `assets/map/*.json` | `https://basemaps.arcgis.com/...` | 底图样式资源 |
| `assets/map/*.json` | `https://cdn.arcgis.com/...` | 图标/Sprite 资源 |
| `thingsboard.yml` | `https://*.arcgis.com`, `https://*.arcgisonline.com` | CSP 白名单 |

### 5.3 Google Maps

| 文件 | URL |
|------|-----|
| `google-map.ts:54` | `https://maps.googleapis.com/maps/api/js?key=${apiKey}` |
| `map-layer.ts:286` | `https://maps.googleapis.com/maps/api/js?key=${apiKey}&loading=async` |

---

## 六、移动端推送与 App Store

### 6.1 Firebase Cloud Messaging

**文件**: `DefaultFirebaseService.java` — 通过 Firebase SDK 通信至 `fcm.googleapis.com`（运行时，SDK 内部控制）

### 6.2 移动 App 深链接

**文件**: `QrCodeSettingsController.java:90`
```
DEEP_LINK_PATTERN = "https://%s/api/noauth/qr?secret=%s&ttl=%s"
```
域名由 `mobileApp.domain` 配置，默认 `demo.allshen.org`

### 6.3 应用商店链接

**thingsboard.yml:2227-2229**:
| URL | 说明 |
|-----|------|
| `https://play.google.com/store/apps/details?id=org.thingsboard.demo.app` | Google Play |
| `https://apps.apple.com/us/app/thingsboard-live/id1594355695` | Apple App Store |

在 `/api/noauth/qr` 端点根据 User-Agent 自动重定向到对应商店。

---

## 七、AI 服务集成

### 7.1 OpenAI

**文件**: `OpenAiProviderConfig.java:33`
| URL | 说明 |
|-----|------|
| `https://api.openai.com/v1` | 默认 OpenAI API 基地址 |

支持租户自定义，可配置为 Azure OpenAI 端点:
- `https://my-resource.openai.azure.com/`

---

## 八、版本更新检查

**文件**: `DefaultUpdateService.java`
| URL | 说明 |
|------|------|
| `https://updates.allshen.org/api/v2/thingsboard/updates` | 版本更新检查 |
| `https://updates.allshen.org/api/v1/edge/installMapping` | Edge 安装映射 |
| `https://updates.allshen.org/api/v1/edge/upgradeMapping` | Edge 升级映射 |
| `https://allshen.org/docs/reference/releases` | 降级显示用发行说明 |

---

## 九、控件触发的用户可配置外部链接

以下入口点允许用户或租户配置任意外部 URL，打开时不经校验：

| 入口 | 文件 | 触发方式 |
|------|------|----------|
| 仪表板部件 "Open URL" 动作 | `widget.component.ts:1181` | `window.open(url, '_blank'/'_self')` |
| 通知动作按钮 | `notification.component.ts:134` | `window.open(link, '_blank')` |
| OTA 包 URL | `ota-update-table-config.resolve.ts:149` | `window.open(url, '_blank')` |
| 邮件 OAuth2 Token URI | `TbMailSender.java` | SMTP 连接 token 刷新 |
| REST API Call 规则节点 | `TbRestApiCallNode.java` | 规则引擎调用任意外部 API |
| Slack 规则节点 | `TbSlackNode.java` | Slack Webhook 调用 |

> ⚠️ **安全注意**: 这些入口点的 URL 由租户管理员或规则链设计者配置，可实现 SSRF 或钓鱼攻击。建议在部署层面进行出站网络限制。

---

## 十、内容安全策略 (CSP)

**thingsboard.yml:249** 默认 CSP 中的外部源：

```
img-src:
  - https://img.allshen.org
  - https://tile.openstreetmap.org
  - https://*.tile.openstreetmap.org
  - https://*.arcgis.com

connect-src:
  - https://*.arcgis.com
  - https://*.arcgisonline.com
```

> ⚠️ **注意**: 如果部署中添加了新的外部集成（如 Slack、OpenAI），需要同步更新 CSP 配置。

---

## 十一、构建系统外部依赖

### 11.1 Maven 仓库

| URL | 用途 |
|-----|------|
| `https://repo.allshen.org/artifactory/libs-release-public` | ThingsBoard 私有仓库 |
| `https://repo1.maven.org/maven2/` | Maven Central |
| `https://repo.spring.io/snapshot` | Spring 快照 |
| `https://repo.spring.io/milestone` | Spring 里程碑 |
| `https://repo.typesafe.com/typesafe/releases/` | Typesafe 仓库 |

### 11.2 Docker 构建

| URL | 用途 |
|-----|------|
| `https://www.postgresql.org/media/keys/ACCC4CF8.asc` | PostgreSQL GPG 密钥 |
| `http://apt.postgresql.org/pub/repos/apt/` | PostgreSQL APT 仓库 |
| `https://downloads.apache.org/cassandra/KEYS` | Cassandra GPG 密钥 |
| `https://debian.cassandra.apache.org` | Cassandra APT 仓库 |

### 11.3 其他配置

**thingsboard.yml:168**: JWT Token Issuer 默认值 = `allshen.org`
**thingsboard.yml:1712-1719**: Swagger API 联系方式 = `https://allshen.org`

---

## 十二、所有外部域名汇总

| 域名 | 用途分类 |
|------|----------|
| `allshen.org` | 文档 / 发布说明 / Swagger / Token Issuer |
| `updates.allshen.org` | 版本更新检查 |
| `img.allshen.org` | CSP 图片白名单 |
| `github.com` / `api.github.com` | 源码链接 / OAuth2 / Star API |
| `raw.githubusercontent.com` | 帮助系统资源（默认不再使用，仅当管理员将 `UI_HELP_BASE_URL` / 白标 "UI 帮助基础 URL" 显式配置为该域名时才会请求） |
| `tile.openstreetmap.org` | 地图瓦片 |
| `basemaps.arcgis.com` / `cdn.arcgis.com` | 地图服务 |
| `maps.googleapis.com` | Google Maps SDK |
| `accounts.google.com` / `oauth2.googleapis.com` / `openidconnect.googleapis.com` | Google OAuth2 |
| `graph.facebook.com` / `www.facebook.com` | Facebook OAuth2 |
| `appleid.apple.com` | Apple OAuth2 |
| `api.openai.com` / `openai.azure.com` | AI 服务 |
| `fcm.googleapis.com` | Firebase 推送 (运行时) |
| `play.google.com` / `apps.apple.com` | 应用商店 |
| `sqs.us-east-1.amazonaws.com` | AWS SQS (示例模式) |
| `repo.allshen.org` | Maven 仓库 |
| `repo1.maven.org` | Maven Central |
| `repo.spring.io` | Spring 仓库 |
| `postgresql.org` / `apache.org` | Docker 镜像构建 |

---

## 十三、安全建议

1. **CSP 策略**: 配置生产环境 CSP 时，仅包含实际使用的外部域名
2. **出站防火墙**: 限制 ThingsBoard 服务器只能访问必要的出站端口和 IP
3. **用户可配置 URL**: 对部件的 "Open URL" 动作、通知链接、REST API Call 节点等，考虑在部署层添加 URL 白名单机制
4. **更新服务**: 如果不需要自动更新检查，可通过环境变量 `UPDATES_ENABLED=false` 禁用到 `updates.allshen.org` 的请求
5. **文档链接**: 所有 `allshen.org/docs/` 链接为只读文档，但可通过代理或拦截列表过滤

---

文档生成日期: 2026-06-18
