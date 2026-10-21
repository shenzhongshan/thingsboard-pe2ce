# 配置与安全

**本文是权威版。** 英文版 [config-and-security.md](config-and-security.md) 为对照上游 CE 的参照副本，改动请先落在本文。

ThingsBoard 应用服务端经 `thingsboard.yml` 配置，并由 Spring Security 配合 JWT token、RBAC、
OAuth2 和双因子认证来保护。

## 配置文件：`thingsboard.yml`

主配置文件位于：

```
application/src/main/resources/thingsboard.yml
```

它采用结构化的 YAML 层级，并广泛支持环境变量覆盖。所有属性值都使用 `${ENV_VARIABLE:default}` 形式。

### 配置段

| 段 | 环境变量前缀 | 用途 |
|---------|---------------------------|---------|
| `server` | `HTTP_`、`SSL_`、`TB_SERVER_` | HTTP(S) 绑定地址、端口、SSL、WebSocket、REST API 设置、压缩、限流 |
| `app` | （构建期） | 应用版本串 |
| `zk` | `ZOOKEEPER_` | ZooKeeper 服务发现与集群协调 |
| `cluster.stats` | `TB_CLUSTER_` | 节点间消息统计 |
| `plugins` | `PLUGINS_` | 扩展插件的 classpath 扫描包 |
| `security` | `SECURITY_`、`JWT_` | JWT token、API key、用户登录、设备认领、OAuth2、CA 证书、HTTP 安全头 |
| `mail` | `MAIL_` | 邮件服务的 OAuth2 token 刷新、限流 |
| `usage.stats` | `USAGE_STATS_`、`DEVICES_STATS_` | API 用量统计的采集与上报 |
| `ui` | `UI_`、`DASHBOARD_` | 仪表板数据上限、帮助文档基址 |
| `database` | `DATABASE_` | 时序存储类型选择（sql/cassandra/timescale） |
| `cassandra` | `CASSANDRA_`、`TS_KV_` | Cassandra 驱动：集群、keyspace、SSL、凭证、socket、查询 |
| `sql` | `SQL_` | SQL 数据库连接设置 |
| `sql.postgres` | `POSTGRES_` | PostgreSQL 专属设置 |
| `sql.ts` | `SQL_TS_` | SQL 时序存储设置 |
| `sql.ts_latest` | `SQL_TS_LATEST_` | SQL 最新值存储 |
| `cache` | `CACHE_` | 缓存配置（类型、spec） |
| `queue` | `TB_QUEUE_` | 队列类型（kafka/pub-sub/aws-sqs 等）及其设置 |
| `actors` | （actors.*） | Actor 系统设置（见 [actors.zh-CN.md](actors.zh-CN.md)） |
| `transport` | `TRANSPORT_` | 传输服务设置 |
| `edges` | `EDGES_` | Edge 功能设置 |
| `rate_limits` | `TB_RATE_LIMITS_` | 全局限流配置 |
| `audit_log` | `AUDIT_LOG_` | 审计日志设置 |
| `notification` | `NOTIFICATION_` | 通知系统设置 |
| `install` | `INSTALL_` | 安装与升级设置 |
| `state` | `STATE_` | 系统状态持久化设置 |

### 关键环境变量

| 变量 | 默认值 | 用途 |
|----------|---------|---------|
| `DATABASE_TS_TYPE` | `sql` | 时序存储后端：`sql`、`cassandra` 或 `timescale` |
| `DATABASE_TS_LATEST_TYPE` | `sql` | 最新值存储后端 |
| `TB_QUEUE_TYPE` | `kafka` | 消息队列类型 |
| `CACHE_TYPE` | `caffeine` | 缓存实现：`caffeine`（本地）或 `redis` |
| `ZOOKEEPER_ENABLED` | `false` | 是否启用 ZooKeeper 做集群协调 |
| `HTTP_BIND_ADDRESS` | `0.0.0.0` | 服务端绑定地址 |
| `HTTP_BIND_PORT` | `8080` | 服务端绑定端口 |
| `SSL_ENABLED` | `false` | 是否启用 HTTPS |
| `JWT_TOKEN_EXPIRATION_TIME` | `9000` | JWT 访问 token 的 TTL（秒） |
| `JWT_REFRESH_TOKEN_EXPIRATION_TIME` | `604800` | JWT 刷新 token 的 TTL（秒） |
| `JWT_TOKEN_SIGNING_KEY` | `thingsboardDefaultSigningKey` | JWT 签名密钥（Base64） |

> **本仓的本地运行必须覆盖两项默认值**：`SPRING_DATASOURCE_URL` 要指到
> `thingsboard_ce` 库，`EDGES_ENABLED` 要设成 `true`。yml 的默认值会静默连到另一个库、并且
> 把 edge 功能关掉，表现为"功能明明移植了却不生效"。

> **生产环境必须改掉签名密钥。** `JWT_TOKEN_SIGNING_KEY` 的默认值
> `thingsboardDefaultSigningKey` 是公开的常量。不改它等于任何人都能自签出一个 `SYS_ADMIN` 的
> token。它属于上线前的硬性检查项。

## Spring 配置类

位于 `application/src/main/java/org/thingsboard/server/config/`：

| 类 | 用途 |
|-------|---------|
| `ThingsboardSecurityConfiguration` | 主安全配置 —— 过滤器链、认证 provider、CORS、限流过滤器 |
| `ThingsboardMessageConfiguration` | 消息/队列相关的 bean 配置 |
| `WebConfig` | Web MVC 配置（消息转换器、异步支持） |
| `WebSocketConfiguration` | WebSocket 端点注册 |
| `SchedulingConfiguration` | 任务调度器配置 |
| `SwaggerConfiguration` | OpenAPI/Swagger UI 配置 |
| `CryptoConfig` | 加解密工具（BCrypt、AES） |
| `MvcCorsProperties` | CORS 映射配置属性 |
| `HttpSecurityHeadersCustomizer` | HTTP 安全响应头 |
| `HttpSecurityHeadersProperties` | 安全头属性绑定 |
| `RateLimitProcessingFilter` | 限流 servlet 过滤器 |
| `CustomOAuth2AuthorizationRequestResolver` | OAuth2 授权请求定制 |
| `TbHttpClientSettingsComponent` | 外部规则节点调用所用的 HTTP 客户端设置 |
| `TbRuleEngineSecurityConfiguration` | 规则引擎专属的安全设置 |
| `config/mqtt/MqttClientSettingsComponent` | MQTT 客户端配置 |
| `config/mqtt/MqttClientRetransmissionSettingsComponent` | MQTT 重传设置 |
| `config/annotations/ApiOperation` | API 操作元数据的自定义注解 |

## 安全架构

### 认证流程

安全体系建立在 Spring Security 之上，采用无状态架构（`SessionCreationPolicy.STATELESS`）。
`ThingsboardSecurityConfiguration` 中的主过滤器链定义了这些入口：

1. **免认证路径**（`/api/noauth/**`、静态资源、WebSocket）—— 全部放行
2. **登录端点**（`/api/auth/login`、`/api/auth/login/public`）—— 由
   `RestLoginProcessingFilter` / `RestPublicLoginProcessingFilter` 处理
3. **Token 刷新**（`/api/auth/token`）—— 由 `RefreshTokenProcessingFilter` 处理
4. **受保护的 API**（`/api/**`）—— 需要经 JWT 或 API key 认证
5. **WebSocket**（`/api/ws/**`）—— 经 token 查询参数认证
6. **设备 API**（`/api/v1/**`）—— 经设备凭证认证（单独处理）

### 认证 provider

`AuthenticationManager` 按序聚合四个 provider：

| Provider | 用途 |
|----------|---------|
| `RestAuthenticationProvider` | 用户名/密码登录 |
| `JwtAuthenticationProvider` | JWT bearer token 校验 |
| `ApiKeyAuthenticationProvider` | API key 校验（供程序化访问） |
| `RefreshTokenAuthenticationProvider` | 刷新 token 处理 |

### JWT token 体系

位于 `service/security/model/token/`：

| 类 | 用途 |
|-------|---------|
| `JwtTokenFactory` | 创建并校验 JWT 的"访问 + 刷新"token 对 |
| `AccessJwtToken` | 访问 JWT 的包装 |
| `RawAccessJwtToken` | 原始 JWT token 表示 |
| `OAuth2AppTokenFactory` | OAuth2 应用 token 创建 |
| `ApiKeyAuthRequest` | API key 认证请求模型 |

Token 流程：
1. **登录** —— `POST /api/auth/login` 带用户名/密码，返回访问 token + 刷新 token
2. **已认证请求** —— 带上 `Authorization: Bearer <token>` 或 `X-Authorization: Bearer <token>` 头
3. **Token 刷新** —— `POST /api/auth/token` 带刷新 token，返回新的 token 对
4. **Token 过期** —— 访问 token 在 `JWT_TOKEN_EXPIRATION_TIME` 秒后过期（默认 9000 = 2.5 小时）
5. **刷新 token** —— 在 `JWT_REFRESH_TOKEN_EXPIRATION_TIME` 秒后过期（默认 604800 = 1 周）

Token 提取器（位于 `service/security/auth/extractor/`）：
- `JwtHeaderTokenExtractor` —— 从 HTTP 头提取 JWT
- `JwtQueryTokenExtractor` —— 从查询参数提取 JWT（WebSocket 用）
- `ApiKeyHeaderTokenExtractor` —— 从 HTTP 头提取 API key

### RBAC 模型

基于角色的访问控制经 Spring Security 的 authority 实现：

| Authority | 说明 |
|-----------|-------------|
| `SYS_ADMIN` | 系统管理员 —— 全平台访问 |
| `TENANT_ADMIN` | 租户管理员 —— 管理某租户内的实体 |
| `CUSTOMER_USER` | 客户用户 —— 对被分配实体的受限访问 |

其他 authority 类型：
- `MFA_CONFIGURATION_TOKEN` —— 用于双因子认证的配置流程
- 各类基于权限的 authority，用于细粒度访问控制

`AccessControlService`（由 `DefaultAccessControlService` 实现）用 `Resource` + `Operation` 模型
强制权限：

- **Resource** —— `DEVICE`、`ASSET`、`ALARM`、`CUSTOMER`、`TENANT`、`USER`、`DASHBOARD`、
  `RULE_CHAIN`、`DEVICE_PROFILE`、`ASSET_PROFILE`、`TENANT_PROFILE`、`ENTITY_VIEW`、`EDGE`、
  `WIDGET_TYPE`、`WIDGETS_BUNDLE`、`TB_RESOURCE`、`OTA_PACKAGE`、`QUEUE`、`OAUTH2_CLIENT`、
  `DOMAIN`、`MOBILE_APP`、`MOBILE_APP_BUNDLE`、`NOTIFICATION`、`CALCULATED_FIELD`、`AI_MODEL`、
  `API_KEY` 等
- **Operation** —— `READ`、`WRITE`、`CREATE`、`DELETE`、`ASSIGN_TO_CUSTOMER`、
  `UNASSIGN_FROM_CUSTOMER`、`ASSIGN_TO_TENANT`、`CLAIM_DEVICES`、`READ_CREDENTIALS`、
  `WRITE_CREDENTIALS` 等

权限类定义了"哪个 authority 对哪个 resource 拥有哪些 operation"：
- `SysAdminPermissions` —— 系统管理员拥有全部访问权
- `TenantAdminPermissions` —— 租户管理员管理本租户的实体
- `CustomerUserPermissions` —— 客户用户对被分配实体有读/受限访问
- `MfaConfigurationPermissions` —— MFA 配置权限

`PermissionChecker` 接口支持动态权限检查（例如检查实体归属）。

> **新增 `Resource` 枚举值不足以让它可用。** 还必须在 `SysAdminPermissions` /
> `TenantAdminPermissions` 里注册它，否则该 resource 上的每个请求都会 403。这是本仓移植
> PE 功能时反复踩到的一条。

### OAuth2 集成

OAuth2 支持允许经外部身份提供方登录。关键组件：

| 组件 | 用途 |
|-----------|---------|
| `OAuth2Configuration` | 从数据库读取的 OAuth2 客户端配置 |
| `CustomOAuth2AuthorizationRequestResolver` | 定制授权请求 |
| `HttpCookieOAuth2AuthorizationRequestRepository` | 把 OAuth2 state 存在 cookie 中 |
| `OAuth2Controller` | OAuth2 客户端管理的 REST 端点 |
| `OAuth2ConfigTemplateController` | 常见 provider 的 OAuth2 配置模板 |
| `OAuth2ClientService`（DAO） | 持久化 OAuth2 客户端配置 |

OAuth2 登录流程：
1. 租户管理员配置 OAuth2 客户端
2. 登录页展示各 OAuth2 provider 的按钮
3. 用户被重定向到外部 provider
4. 回调时 ThingsBoard 创建/关联用户账号
5. OAuth2 的 `loginProcessingUrl` 可配置（默认：`/login/oauth2/code/`）

### 双因子认证（2FA）

双因子认证实现在 `service/security/auth/mfa/`：

| 类 | 用途 |
|-------|---------|
| `TwoFactorAuthService` | 2FA service 接口 |
| `DefaultTwoFactorAuthService` | 2FA 实现 |
| `TwoFaConfigManager` | 2FA 配置管理 |
| `DefaultTwoFaConfigManager` | 2FA 配置持久化 |

`mfa/provider/impl/` 下的 provider 实现：
- `TotpTwoFaProvider` —— 经认证器 App 的基于时间的一次性密码（TOTP）
- `EmailTwoFaProvider` —— 经邮件发送的一次性验证码
- `SmsTwoFaProvider` —— 经短信发送的一次性验证码
- `BackupCodeTwoFaProvider` —— 预生成的备用码

### API key 认证

API key 允许不经 JWT token 的程序化访问，经 `ApiKeyController` 管理：

- Key 带一个可配置的前缀（默认：`tb_`）
- Key 值长度可配置（默认 64 字节）
- Key 关联到某个用户，并有可配置的过期时间
- 认证经 `X-Authorization: ApiKey <key>` 头

## 安装 profile 与初始化

`install/` service 包负责数据库初始化：

1. **Schema 创建** —— `SqlEntityDatabaseSchemaService` 创建 SQL schema；`CassandraKeyspaceService`
   创建 Cassandra keyspace
2. **系统数据** —— `DefaultSystemDataLoaderService` 创建系统管理员、默认租户配置、默认设备/资产
   配置、系统规则链、widget 包以及其他初始数据
3. **升级** —— `SqlDatabaseUpgradeService` 与 `AbstractCassandraDatabaseUpgradeService` 负责
   版本间的 schema 迁移
4. **数据迁移** —— `CassandraTsLatestToSqlMigrateService` 负责跨库数据迁移

安装过程在首次启动或升级之后自动运行，由 `install` 配置段控制。

> **本仓有两条升级路径，这里只覆盖其中一条。** 上述 `install/` 下的 bean 都标了
> `@Profile("install")`，只在 `upgrade.sh` 的离线升级路径下激活。普通启动走的是
> `SystemPatchApplier` 的热补丁路径，它跑不到 `updateData()` 也跑不到规则节点配置迁移。改升级
> 逻辑时两条路径都要接上。完整说明见 [版本管理与系统升级](../04-upgrade-and-release.zh-CN.md)。

## 相关文档

- [README](README.zh-CN.md) —— Application 模块总览
- [控制器](controllers.zh-CN.md) —— 带权限注解的 REST API 控制器
- [服务](services.zh-CN.md) —— 含安全 service 的 service 层
- [Actors](actors.zh-CN.md) —— Actor 系统配置
- [版本管理与系统升级](../04-upgrade-and-release.zh-CN.md) —— 两条升级路径的权威说明
