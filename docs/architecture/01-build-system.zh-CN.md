# 构建系统

**本文是权威版。** 英文版 [01-build-system.md](01-build-system.md) 为对照上游 CE 的参照副本，改动请先落在本文。

## 总览

ThingsBoard 后端用 **Maven** 做构建与依赖管理，前端用 **Yarn**。后端目标 **Java 25**，用
**Spring Boot 3.5.14** 编译。前端用 Angular 20 构建。

- **根 POM**：`pom.xml`（group：`org.thingsboard`，artifact：`thingsboard`，version：`4.4.0-SNAPSHOT`）
- **Java 版本**：25（见 properties 里的 `<java.version>`）
- **默认 goal**：`mvn clean install`
- **并行构建**：支持 `-T` 参数（如 `-T6` 表示 6 线程）

根 POM 的类型是 `pom`，聚合所有子模块。依赖版本集中写在 `<properties>` 里，并在
`<dependencyManagement>` 中以 Spring Boot 的 BOM 为基础进行管理，针对 CVE 做定点覆盖。

## Maven Reactor 结构

根 `pom.xml` 的 `<modules>` 段定义了 reactor 构建顺序。模块按依赖顺序列出，也就是说靠前的模块会
先于依赖它们的后续模块被编译：

```xml
<modules>
    <module>netty-mqtt</module>
    <module>common</module>
    <module>rule-engine</module>
    <module>dao</module>
    <module>edqs</module>
    <module>transport</module>
    <module>ui-ngx</module>
    <module>tools</module>
    <module>application</module>
    <module>msa</module>
    <module>rest-client</module>
    <module>monitoring</module>
</modules>
```

### 模块细节

#### 1. `netty-mqtt`

- **用途**：供 MQTT transport 使用的自研 Netty MQTT 3.1/3.1.1 编解码器
- **Artifact**：`org.thingsboard:netty-mqtt`
- **技术**：Netty 4.1
- **依赖**：仅外部库（Netty）
- **被谁依赖**：`transport/mqtt`

#### 2. `common`

- **用途**：16 个共享库子模块的聚合 POM
- **Artifact**：POM 打包（仅聚合）
- **被谁依赖**：绝大多数其他模块

| 子模块 | Artifact ID | 用途 |
|---|---|---|
| `data` | `data` | 实体模型、DTO、ID、枚举（Device、Asset、Alarm、Dashboard 等） |
| `message` | `message` | 规则引擎、队列与集群用的内部消息类型 |
| `queue` | `queue` | Kafka / 发布订阅队列层抽象 |
| `dao-api` | `dao-api` | DAO 接口（repository 契约）、service 级 API |
| `cache` | `cache` | 基于 Caffeine 的缓存基础设施 |
| `cluster-api` | `cluster-api` | 集群通信接口 |
| `discovery-api` | `discovery-api` | 服务发现抽象（基于 ZooKeeper） |
| `actor` | `actor` | Actor 系统抽象 |
| `proto` | `proto` | Protobuf 消息定义（经 `protobuf-maven-plugin` 编译） |
| `transport` | `transport` | 传输层共享类（传输资源、会话接口） |
| `coap-server` | `coap-server` | CoAP 服务端共享工具 |
| `script` | `script-api`、`remote-js-client` | 远程 JavaScript 执行 API 与客户端 |
| `util` | `util` | 通用工具 |
| `stats` | `stats` | 统计采集工具 |
| `edge-api` | `edge-api` | 边缘计算 API 接口 |
| `edqs` | `edqs` | EDQS 查询模型 |
| `version-control` | `version-control` | 批量导出/导入的版本控制 API |

#### 3. `rule-engine`

- **用途**：规则引擎子系统的聚合 POM
- **子模块**：
  - `rule-engine-api` —— 规则链、规则节点与规则引擎消息的接口
  - `rule-engine-components` —— 50+ 内置规则节点实现（过滤器、转换器、动作、外部集成、AI）
- **依赖**：`common`（data、message、queue、dao-api、actor、cache、script）
- **被谁依赖**：`application`

#### 4. `dao`

- **用途**：数据访问实现
- **Artifact**：`org.thingsboard:dao`
- **结构**：`sql/` 子目录对应 JDBC/PostgreSQL（Spring Data JPA），`nosql/` 对应 Cassandra
- **依赖**：`common`（data、dao-api、cache、queue、message、cluster-api）
- **被谁依赖**：`application`、`tools`
- **另外产出**：带 `test-jar` classifier 的共享测试 fixture

#### 5. `edqs`

- **用途**：实体数据查询服务 —— 面向时序与属性数据的专用查询引擎
- **Artifact**：`org.thingsboard.common:edqs`
- **依赖**：`common`（data、queue、dao-api、cache）
- **被谁依赖**：`application`、`msa/edqs`

#### 6. `transport`

- **用途**：设备连接传输的聚合 POM
- **子模块**：
  - `mqtt` —— MQTT 传输（基于 Netty）
  - `http` —— HTTP 传输（Spring Boot Web）
  - `coap` —— CoAP 传输（基于 Californium）
  - `lwm2m` —— LwM2M 传输（基于 Leshan）
  - `snmp` —— SNMP 传输（基于 snmp4j）
- **依赖**：`netty-mqtt`、`common`（transport、data、message、queue、dao-api、actor、cache、coap-server、proto）
- **被谁依赖**：`application`、`msa/transport`

每个 transport 模块都产出一个可启动 JAR 和一个 Docker 镜像（经 `msa/transport/*` 子模块）。

#### 7. `ui-ngx`

- **用途**：Angular 20 前端应用
- **构建工具**：Yarn + Angular CLI（经 `frontend-maven-plugin` 调用）
- **依赖**：无（独立 Maven 模块）
- **被谁依赖**：`msa/web-ui`

#### 8. `tools`

- **用途**：数据迁移、导出与运维用的实用工具
- **依赖**：`common`、`dao`
- **被谁依赖**：无（叶子模块，test 作用域依赖）

#### 9. `application`

- **用途**：Spring Boot 主应用（控制器、服务、actor、配置）
- **Artifact**：`org.thingsboard:application`（产出 boot JAR）
- **入口**：`org.thingsboard.server.ThingsboardServerApplication`
- **依赖**：`rule-engine`、`dao`、`edqs`、`transport`（全部 transport）、`common`（全部子模块）
- **被谁依赖**：`msa/tb-node`（Docker 打包）

这就是"主节点"——它包含 REST API、WebSocket 端点、规则引擎执行器、actor 系统和所有 service。

#### 10. `msa`

- **用途**：微服务产物（Docker 镜像）的聚合 POM
- **子模块**：

| 子模块 | POM Artifact | 用途 |
|---|---|---|
| `tb-node` | `tb-node` | 主应用节点的 Docker 镜像 |
| `transport/mqtt` | `transport:mqtt`（docker-info） | MQTT transport 的 Docker 镜像 |
| `transport/http` | `transport:http`（docker-info） | HTTP transport 的 Docker 镜像 |
| `transport/coap` | `transport:coap`（docker-info） | CoAP transport 的 Docker 镜像 |
| `transport/lwm2m` | `transport:lwm2m`（docker-info） | LwM2M transport 的 Docker 镜像 |
| `transport/snmp` | `transport:snmp`（docker-info） | SNMP transport 的 Docker 镜像 |
| `edqs` | `edqs` | EDQS 的 Docker 镜像 |
| `js-executor` | `js-executor` | Node.js JS executor sidecar 的 Docker 镜像 |
| `web-ui` | `web-ui` | Angular 前端的 Docker 镜像（nginx） |
| `vc-executor` | `vc-executor` | 版本控制操作的 Docker 镜像 |
| `vc-executor-docker` | `vc-executor-docker` | 版本控制 executor 的 Docker 配置 |
| `black-box-tests` | `black-box-tests` | 用 TestNG + Allure 的集成/E2E 测试 |
| `monitoring` | `monitoring` | 监控配置（Grafana 仪表板） |
| `tb` | `tb` | 便捷的 Docker Compose 聚合镜像（postgres、cassandra、hybrid） |

#### 11. `rest-client`

- **用途**：ThingsBoard API 的 Java REST 客户端 SDK
- **Artifact**：`org.thingsboard:rest-client`
- **依赖**：仅外部库（Spring RestTemplate、Jackson）
- **被谁依赖**：仅 test 作用域（被 `black-box-tests` 使用）

#### 12. `monitoring`

- **用途**：基于 Micrometer 的监控与指标配置
- **依赖**：`common`
- **被谁依赖**：`application`、`msa/monitoring`

### 模块依赖图（简化）

```
netty-mqtt
  └─► transport/mqtt
        └─► transport (aggregator)
              └─► application

common (16 submodules)
  ├─► dao
  │     ├─► application
  │     └─► tools
  ├─► rule-engine (api + components)
  │     └─► application
  ├─► edqs
  │     └─► application
  ├─► transport (aggregator)
  │     └─► application
  ├─► monitoring
  │     └─► application
  └─► rest-client (standalone, test-scoped)

application
  └─► msa/tb-node
        └─► msa (Docker images)

ui-ngx
  └─► msa/web-ui
```

## 构建命令

### 完整构建（跳过测试、跳过打包）

```bash
mvn clean install -T6 -DskipTests -Dpkg.skip=true
```

这是最快的完整构建。`-T6` 启用 6 个并行线程。`-DskipTests` 跳过测试的编译与执行。
`-Dpkg.skip=true` 跳过全部打包（boot JAR、.deb、.rpm、.zip），能大幅缩短构建时间。

### 完整构建并产出全部打包

```bash
mvn clean install -T6 -DskipTests
```

产出完整产物集：boot JAR、.deb 包、.rpm 包，以及 Windows 用的 .zip 压缩包。构建 Docker 镜像时必需。

### 不跑 application/DAO/UI 测试的构建（编译更快）

```bash
mvn clean install -T6 -DskipTests -Dpkg.skip=true -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui'
```

### 前端开发

```bash
cd ui-ngx
yarn start              # 开发服务器，http://localhost:4200
yarn build:prod         # 生产构建（输出到 target/generated-resources/public）
yarn lint               # ESLint
```

前端 Maven 模块用 `frontend-maven-plugin` 在 Maven 构建期间调用 Yarn。执行 `mvn install` 时，
该插件会下载 Node.js 并运行 `yarn install` + `yarn build:prod`。

### 单独跳过某种打包类型

| 参数 | 效果 |
|---|---|
| `-Dpkg.skip=true` | 跳过**全部**打包（bootjar + deb + rpm + zip）。便捷别名。 |
| `-Dpkg.skip.bootjar=true` | 跳过 Spring Boot repackage（即 `-boot.jar`）。同时导致 .deb 无法产出，因为 buildDeb 依赖 boot JAR。 |
| `-Dpkg.skip.deb=true` | 跳过 Gradle `buildDeb` 任务。不产出 .deb。 |
| `-Dpkg.skip.rpm=true` | 跳过 Gradle `buildRpm` 任务。不产出 .rpm。 |
| `-Dpkg.skip.zip=true` | 跳过 Maven Assembly Plugin 的 Windows ZIP。 |

设置 `-Dpkg.skip=true` 时会自动激活 `skip-pkg` profile，它把上面四个单独的 skip 标志全部置为 `true`。

只构建 Windows ZIP（不要 .deb、不要 .rpm）：

```bash
mvn clean install -T6 -DskipTests -Dpkg.skip.deb=true -Dpkg.skip.rpm=true -Dpkg.skip.zip=false
# -> application/target/thingsboard-windows.zip
```

## Maven Profile

### `default`（默认激活）

始终激活。无特殊行为。

### `download-dependencies`

为所有依赖启用 source 与 javadoc 下载。用法：

```bash
mvn package -Pdownload-dependencies -Dclassifier=sources dependency:copy-dependencies
```

把 source 与 javadoc 下载到 `target/dependencies` 下。

### `skip-deb`

设置 `-Dpkg.skip.deb=true` 时自动激活。它把 `pkg.deb.phase=none`，这样
`build-helper-maven-plugin` 的 `attach-artifact` goal 就不会因为 .deb 文件缺失而失败。

### `skip-pkg`

设置 `-Dpkg.skip=true` 时自动激活。便捷 profile，把所有打包 skip 标志置为 `true`，再加上
`pkg.deb.phase=none` 和 `pkg.dist.phase=none`。一个开关关掉全部打包。

### `packaging`（始终激活）

它由 `pkg.packaging.disabled` 属性的**缺失**来激活，所以除非你显式传
`-Dpkg.packaging.disabled=true`，否则每次构建它都开着。

> 它刻意**不**用 `activeByDefault`。只要同一个 POM 里有任何其他 profile 被激活，Maven 的
> `DefaultProfileSelector` 就会丢弃该 POM 中所有 `activeByDefault` 的 profile —— 而上面的
> `skip-deb` / `skip-pkg` 恰恰是靠普通 `-D` 属性激活的。在旧的 `activeByDefault` 写法下，
> `-Dpkg.skip.deb=true` 会静默删掉整个 `pluginManagement` 块：`maven-assembly-plugin` 既没有
> execution 也没有 descriptor，于是无论 `pkg.skip.zip` 怎么设都不会产出 Windows ZIP；
> `spring-boot-maven-plugin` 丢掉了 `repackage` execution，于是 boot JAR 也不会产出 ——
> 两种情况都不报任何错。（这个规则是按 POM 生效的：`ui-ngx` 的 `activeByDefault` profile
> `yarn-build` 在 `ui-ngx/pom.xml` 里，不受根 POM 的 profile 激活影响。）
>
> 用 `mvn -N help:active-profiles -Dpkg.skip.deb=true` 检查 —— `skip-deb` 和 `packaging` 必须都在列表里。

定义所有模块共用的打包插件配置。包含如下插件的 `pluginManagement` 块：
- **`maven-resources-plugin`**：为 Linux/Windows 打包复制配置、脚本、control 文件和 init 脚本
- **`maven-dependency-plugin`**：为 Windows 打包复制 WinSW 服务封装可执行文件。绑定到 `pkg.dist.phase`
- **`maven-jar-plugin`**：配置 manifest 条目（Implementation-Title、Implementation-Version）
- **`spring-boot-maven-plugin`**：构建可启动 JAR（repackage + build-info goal）。`pkg.skip.bootjar=true` 时跳过
- **`gradle-maven-plugin`**：调用 `packaging/java/` 内的 Gradle 构建以产出 .deb 和 .rpm 包。绑定到
  `pkg.dist.phase`；通过 `pkg.skip.deb` 与 `pkg.skip.rpm` 跳过单个任务（作为 `-PskipDeb` / `-PskipRpm`
  传给 Gradle，由 `build.gradle` 里的 `onlyIf` 应用）。注意它们只跳过任务 —— 除非
  `pkg.dist.phase=none`，Gradle 本身的调用仍会发生
- **`maven-assembly-plugin`**：产出 Windows ZIP 发行包。经 `pkg.skip.zip` 跳过
- **`build-helper-maven-plugin`**：把 .deb 产物附加到 Maven reactor，以便下游模块引用。
  其 phase 由 `pkg.deb.phase` 控制

## 打包系统

### Boot JAR（Spring Boot）

`spring-boot-maven-plugin` 把应用 JAR 重新打包成包含全部依赖的可执行 boot JAR：

```
target/thingsboard-4.4.0-SNAPSHOT-boot.jar
```

`<classifier>` 设为 `boot`，以便与普通的库 JAR 区分。boot JAR 是 Gradle buildDeb 任务的必需输入。

### Debian 包（.deb）

由自定义的 `gradle-maven-plugin` 产出，它会调用 `packaging/java/` 内的 Gradle 构建。该 Gradle
工程用 `nebula.ospackage` 插件（或类似插件）组装 Debian 包。

Maven 插件调用的 Gradle 任务：`build`、`buildDeb`、`buildRpm`。

关键 Gradle 参数：
- `-PmainJar=` —— Spring Boot JAR 的路径
- `-PpkgName=` —— 包名（如 `thingsboard`）
- `-PpkgUser=` —— 系统用户（`thingsboard`）
- `-PpkgInstallFolder=` —— 安装目录（`/usr/share/thingsboard`）
- `-PskipDeb=` / `-PskipRpm=` —— 控制各任务是否执行

产出的 .deb 经 `build-helper-maven-plugin:attach-artifact` 以 classifier `deb`、type `deb` 附加到
Maven reactor。

### RPM 包（.rpm）

由同一个 Gradle 构建产出。没有任何 Maven reactor 模块依赖 RPM 产物，所以
`-Dpkg.skip.rpm=true` 可以随意开关。

### Windows ZIP 压缩包

由 `maven-assembly-plugin` 用 `packaging/java/assembly/windows.xml` 这个 descriptor 产出，写到
`application/target/thingsboard-windows.zip`（`-windows` 后缀来自 descriptor 的 `<id>`）。布局：

```
thingsboard/lib/thingsboard.jar          the boot JAR
thingsboard/thingsboard.exe              WinSW service wrapper
thingsboard/thingsboard.xml              service definition
thingsboard/install.bat|uninstall.bat|upgrade.bat
thingsboard/conf/ data/ install/ logs/
```

用 `-Dpkg.skip.zip=true` 跳过。

### Docker 镜像

Docker 镜像构建由 `msa/` 子模块负责。每个微服务模块（如 `msa/tb-node`、`msa/transport/mqtt`）用
`dockerfile-maven-plugin` 或自定义 Docker 构建流程产出多架构（amd64/arm64）镜像。来自
`application` 或 transport 模块的 `.deb` 产物会被复制进 Docker 镜像作为服务载荷。

本地构建 Docker 时设置 `DOCKER_BUILDKIT=0`。

## 测试执行策略

### 全局测试设置

```bash
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max_old_space_size=4096"
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"
```

Surefire 插件配置了用于字符串去重和 G1 GC 停顿调优的 JVM 参数：

```
-XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200
-XX:+EnableDynamicAgentLoading
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
```

每个 fork 的 RocksDB 路径通过 `-Dqueue.edqs.local.rocksdb_path` 和
`-Dqueue.calculated_fields.rocks_db_path` 设定，以保证并行测试 fork 之间相互隔离。

### 先编译再测试的策略

务必先不带测试地编译一遍，避免多模块并行测试执行时的 classpath 问题：

```bash
# 第 1 步：编译全部（不跑测试）
mvn clean install -T6 -DskipTests -Dpkg.skip=true

# 第 2 步：跑测试
mvn test -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui' -T4
```

### 按模块的测试命令

```bash
# DAO 模块（按包并行，4 个 fork）
mvn test -pl dao -Dparallel=packages -DforkCount=4

# application 的 controller 测试（按类并行，6 个 fork）
# 排除 NoSQL 专属测试，启用不稳定测试重试
mvn test -pl application \
  -Dtest='!**/nosql/**,org.thingsboard.server.controller.**' \
  -DforkCount=6 -Dparallel=classes \
  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5

# 单个测试类
mvn test -pl application -Dtest=DeviceControllerTest -DfailIfNoTests=false
```

### 测试依赖

application 的测试用 **Testcontainers** 拉起 Docker 容器，包括：
- PostgreSQL（数据库）
- Kafka（消息队列）
- 可选：Cassandra、Valkey/Redis、ZooKeeper

构建机上必须有 Docker 在运行。若遇到 Docker API 版本不兼容，在 `/etc/docker/daemon.json` 里加
`"min-api-version": "1.32"`。若 testcontainers 找不到 Docker，删掉 `~/.testcontainers.properties`。

### 黑盒测试

位于 `msa/black-box-tests`。这是完整的集成/E2E 测试，使用：
- **TestNG** 作为测试框架
- **Allure** 做测试报告
- **Testcontainers** 提供基础设施
- 由同一个 Maven reactor 构建出的 Docker 镜像

## 构建插件

| 插件 | 用途 |
|---|---|
| `maven-compiler-plugin` | Java 25 编译，含 Lombok 注解处理 |
| `maven-surefire-plugin` | 测试执行，支持并行 fork |
| `maven-jar-plugin` | JAR 打包，含 manifest 条目 |
| `spring-boot-maven-plugin` | 可启动 JAR 打包（repackage goal） |
| `gradle-maven-plugin` | 调用 Gradle 做 .deb/.rpm 打包 |
| `frontend-maven-plugin` | 下载 Node.js，运行 Yarn 构建 Angular |
| `maven-assembly-plugin` | Windows ZIP 发行包 |
| `maven-resources-plugin` | 打包期间复制配置、脚本和数据文件 |
| `maven-dependency-plugin` | 为 Protobuf 编译复制 protoc 二进制，为 Windows 打包复制 WinSW |
| `protobuf-maven-plugin` | 把 `.proto` 文件编译成 Java 源码（protobuf-java + gRPC） |
| `build-helper-maven-plugin` | 把 .deb 产物附加到 reactor，添加生成的源码目录 |
| `git-commit-id-maven-plugin` | 生成带构建元数据的 `git.properties` |
| `license-maven-plugin` | 强制 Apache 2.0 许可证头 |
| `maven-enforcer-plugin` | 强制构建环境约束 |
| `os-maven-plugin` | 检测 OS/平台以选择 protoc 二进制 |

## 关键 Maven 属性

| 属性 | 默认值 | 用途 |
|---|---|---|
| `maven.compiler.source` | `25` | Java source 版本 |
| `maven.compiler.target` | `25` | Java target 版本 |
| `spring-boot.version` | `3.5.14` | Spring Boot BOM 版本 |
| `spring-boot-test.version` | `3.5.13` | 钉住的测试产物版本（规避某个回归） |
| `kafka.version` | `3.9.2` | Apache Kafka 客户端版本 |
| `protobuf.version` | `3.25.5` | Protobuf 版本（注意：gRPC 尚不支持 Protobuf 4.x） |
| `grpc.version` | `1.76.0` | gRPC 库版本 |
| `netty.version` | `4.1.133.Final` | Netty 版本（覆盖 Spring Boot 管理的版本） |
| `cassandra.version` | `4.17.0` | Cassandra driver 版本 |
| `postgresql.version` | `42.7.11` | PostgreSQL JDBC driver |
| `jjwt.version` | `0.12.5` | JWT 库 |
| `thingsboard.client.version` | `4.4.0` | Java REST 客户端版本（与服务端解耦） |
| `lombok.version` | `1.18.46` | Lombok（必须与 Spring Boot 保持同步） |
| `pkg.user` | `thingsboard` | Linux 包的系统用户 |
| `pkg.installFolder` | `/usr/share/thingsboard` | .deb/.rpm 的安装目录 |
| `pkg.unixLogFolder` | `/var/log/thingsboard` | Linux 包的日志目录 |
| `pkg.skip.bootjar` | `false` | 跳过 boot JAR 重打包 |
| `pkg.skip.deb` | `false` | 跳过 .deb 打包 |
| `pkg.skip.rpm` | `false` | 跳过 .rpm 打包 |
| `pkg.skip.zip` | `false` | 跳过 Windows ZIP 打包 |
| `pkg.deb.phase` | `${pkg.package.phase}` | 内部：`attach-deb` execution 的 phase；跳过 .deb 时为 `none` |
| `pkg.dist.phase` | `${pkg.package.phase}` | 内部：Gradle 调用与 WinSW 下载的 phase；`-Dpkg.skip=true` 下为 `none` |
| `pkg.packaging.disabled` | （未设置） | 设为任意值即完全停用 `packaging` profile |

## 关键外部依赖（已钉版本）

这些依赖覆盖了 Spring Boot BOM 管理的版本，通常是为了修 CVE 或兼容性：

| 库 | 版本 | 原因 |
|---|---|---|
| `netty-bom` | 4.1.133.Final | 修复 CVE-2026-42579、CVE-2026-42583、CVE-2026-42584、CVE-2026-42587 |
| `tomcat-embed-core` | 10.1.55 | 修复 CVE-2026-41284、CVE-2026-43512 |
| `commons-lang3` | 3.18.0 | 修复 CVE-2025-48924 |
| `postgresql` | 42.7.11 | 修复 CVE-2026-42198 |
| `kafka-clients` | 3.9.2 | 修复 CVE-2026-35554 |
| `bouncycastle` | 1.84 | 修复 CVE-2026-5588、CVE-2026-5598 |
| `cassandra-all` | 5.0.7 | 修复 CVE-2026-27314 |
| `opennlp-tools` | 2.5.9 | 修复 CVE-2026-40682、CVE-2026-42027 |
| `nimbus-jose-jwt` | 10.0.2 | 修复 CVE-2023-52428、CVE-2025-53864 |
| `zookeeper` | 3.9.5 | 修复 CVE-2026-24308、CVE-2026-24281 |

## 仓库

| 仓库 ID | URL | 用途 |
|---|---|---|
| `central` | `https://repo1.maven.org/maven2/` | Maven Central |
| `thingsboard-public-repo` | `https://repo.allshen.org/artifactory/libs-release-public` | ThingsBoard 产物与自定义构建 |
| `spring-snapshots` | `https://repo.spring.io/snapshot` | Spring snapshot 构建 |
| `spring-milestones` | `https://repo.spring.io/milestone` | Spring milestone 构建 |
| `typesafe` | `https://repo.typesafe.com/typesafe/releases/` | Typesafe/Akka 依赖 |
