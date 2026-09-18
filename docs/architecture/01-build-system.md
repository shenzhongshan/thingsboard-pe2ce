# Build System

## Overview

ThingsBoard uses **Maven** as its build and dependency management tool for the backend, and **Yarn** for the frontend. The backend targets **Java 25** and compiles with **Spring Boot 3.5.14**. The frontend builds with Angular 20.

- **Root POM**: `pom.xml` (group: `org.thingsboard`, artifact: `thingsboard`, version: `4.4.0-SNAPSHOT`)
- **Java version**: 25 (see `<java.version>` in properties)
- **Default goal**: `mvn clean install`
- **Parallel builds**: Supported with `-T` flag (e.g., `-T6` for 6 threads)

The root POM is of type `pom` and aggregates all submodules. Dependency versions are centralized in `<properties>` and managed in `<dependencyManagement>` using Spring Boot's BOM as the base, with targeted overrides for CVEs.

## Maven Reactor Structure

The `<modules>` section of the root `pom.xml` defines the reactor build order. Modules are listed in dependency order, meaning earlier modules are compiled before later modules that depend on them:

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

### Module Details

#### 1. `netty-mqtt`

- **Purpose**: Custom Netty MQTT 3.1/3.1.1 codec (encoder/decoder) for the MQTT transport
- **Artifact**: `org.thingsboard:netty-mqtt`
- **Technology**: Netty 4.1
- **Depends on**: External libraries only (Netty)
- **Depended on by**: `transport/mqtt`

#### 2. `common`

- **Purpose**: Aggregator POM for 16 shared library submodules
- **Artifact**: POM packaging (aggregator only)
- **Depended on by**: Most other modules

| Submodule | Artifact ID | Purpose |
|---|---|---|
| `data` | `data` | Entity models, DTOs, IDs, enums (Device, Asset, Alarm, Dashboard, etc.) |
| `message` | `message` | Internal message types for rule engine, queue, and clustering |
| `queue` | `queue` | Kafka/pub-sub queue layer abstraction |
| `dao-api` | `dao-api` | DAO interfaces (repository contracts), service-level APIs |
| `cache` | `cache` | Caffeine-based caching infrastructure |
| `cluster-api` | `cluster-api` | Cluster communication interfaces |
| `discovery-api` | `discovery-api` | Service discovery abstractions (ZooKeeper-based) |
| `actor` | `actor` | Actor system abstractions |
| `proto` | `proto` | Protobuf message definitions (compiled via `protobuf-maven-plugin`) |
| `transport` | `transport` | Shared transport-layer classes (transport resources, session interfaces) |
| `coap-server` | `coap-server` | Shared CoAP server utilities |
| `script` | `script-api`, `remote-js-client` | Remote JavaScript execution API and client |
| `util` | `util` | General-purpose utilities |
| `stats` | `stats` | Statistics collection utilities |
| `edge-api` | `edge-api` | Edge computing API interfaces |
| `edqs` | `edqs` | EDQS query model |
| `version-control` | `version-control` | Version control API for bulk export/import |

#### 3. `rule-engine`

- **Purpose**: Aggregator POM for the rule engine subsystem
- **Submodules**:
  - `rule-engine-api` -- interfaces for rule chains, rule nodes, and rule engine messages
  - `rule-engine-components` -- 50+ built-in rule node implementations (filters, transformers, actions, external integrations, AI)
- **Depends on**: `common` (data, message, queue, dao-api, actor, cache, script)
- **Depended on by**: `application`

#### 4. `dao`

- **Purpose**: Data access implementations
- **Artifact**: `org.thingsboard:dao`
- **Structure**: `sql/` subdirectory for JDBC/PostgreSQL (Spring Data JPA), `nosql/` for Cassandra
- **Depends on**: `common` (data, dao-api, cache, queue, message, cluster-api)
- **Depended on by**: `application`, `tools`
- **Also produces**: `test-jar` classifier for shared test fixtures

#### 5. `edqs`

- **Purpose**: Entity Data Query Service -- a dedicated query engine for time-series and attribute data
- **Artifact**: `org.thingsboard.common:edqs`
- **Depends on**: `common` (data, queue, dao-api, cache)
- **Depended on by**: `application`, `msa/edqs`

#### 6. `transport`

- **Purpose**: Aggregator POM for device connectivity transports
- **Submodules**:
  - `mqtt` -- MQTT transport (Netty-based)
  - `http` -- HTTP transport (Spring Boot Web)
  - `coap` -- CoAP transport (Californium-based)
  - `lwm2m` -- LwM2M transport (Leshan-based)
  - `snmp` -- SNMP transport (snmp4j-based)
- **Depends on**: `netty-mqtt`, `common` (transport, data, message, queue, dao-api, actor, cache, coap-server, proto)
- **Depended on by**: `application`, `msa/transport`

Each transport module produces a bootable JAR and a Docker image (via the `msa/transport/*` submodules).

#### 7. `ui-ngx`

- **Purpose**: Angular 20 frontend application
- **Build tool**: Yarn + Angular CLI (invoked via `frontend-maven-plugin`)
- **Depends on**: None (independent Maven module)
- **Depended on by**: `msa/web-ui`

#### 8. `tools`

- **Purpose**: Utility tools for data migration, export, and administration
- **Depends on**: `common`, `dao`
- **Depended on by**: None (leaf module, test-scope dependency)

#### 9. `application`

- **Purpose**: Spring Boot main application (controllers, services, actors, configuration)
- **Artifact**: `org.thingsboard:application` (produces a boot JAR)
- **Entry point**: `org.thingsboard.server.ThingsboardServerApplication`
- **Depends on**: `rule-engine`, `dao`, `edqs`, `transport` (all transports), `common` (all submodules)
- **Depended on by**: `msa/tb-node` (Docker packaging)

This is the "main node" -- it includes the REST API, WebSocket endpoints, rule engine executor, actor system, and all services.

#### 10. `msa`

- **Purpose**: Aggregator POM for microservice artifacts (Docker images)
- **Submodules**:

| Submodule | POM Artifact | Purpose |
|---|---|---|
| `tb-node` | `tb-node` | Docker image for the main application node |
| `transport/mqtt` | `transport:mqtt` (docker-info) | Docker image for MQTT transport |
| `transport/http` | `transport:http` (docker-info) | Docker image for HTTP transport |
| `transport/coap` | `transport:coap` (docker-info) | Docker image for CoAP transport |
| `transport/lwm2m` | `transport:lwm2m` (docker-info) | Docker image for LwM2M transport |
| `transport/snmp` | `transport:snmp` (docker-info) | Docker image for SNMP transport |
| `edqs` | `edqs` | Docker image for EDQS |
| `js-executor` | `js-executor` | Docker image for the Node.js JS executor sidecar |
| `web-ui` | `web-ui` | Docker image for the Angular frontend (nginx) |
| `vc-executor` | `vc-executor` | Docker image for version control operations |
| `vc-executor-docker` | `vc-executor-docker` | Docker configuration for version control executor |
| `black-box-tests` | `black-box-tests` | Integration/E2E tests using TestNG + Allure |
| `monitoring` | `monitoring` | Monitoring configuration (Grafana dashboards) |
| `tb` | `tb` | Convenience Docker Compose aggregator images (postgres, cassandra, hybrid) |

#### 11. `rest-client`

- **Purpose**: Java REST client SDK for the ThingsBoard API
- **Artifact**: `org.thingsboard:rest-client`
- **Depends on**: External libraries only (Spring RestTemplate, Jackson)
- **Depended on by**: Test scope only (used by `black-box-tests`)

#### 12. `monitoring`

- **Purpose**: Micrometer-based monitoring and metrics configuration
- **Depends on**: `common`
- **Depended on by**: `application`, `msa/monitoring`

### Module Dependency Graph (Simplified)

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

## Build Commands

### Full Build (Skip Tests, Skip Packaging)

```bash
mvn clean install -T6 -DskipTests -Dpkg.skip=true
```

This is the fastest full build. The `-T6` flag enables 6 parallel threads. `-DskipTests` skips compilation and execution of tests. `-Dpkg.skip=true` skips all packaging (boot JAR, .deb, .rpm, .zip), which dramatically reduces build time.

### Full Build with All Packaging

```bash
mvn clean install -T6 -DskipTests
```

Produces the full set of artifacts: boot JAR, .deb package, .rpm package, and .zip archive for Windows. Required when building Docker images.

### Build Without Running Application/DAO/UI Tests (Faster Compile)

```bash
mvn clean install -T6 -DskipTests -Dpkg.skip=true -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui'
```

### Frontend Development

```bash
cd ui-ngx
yarn start              # Development server at http://localhost:4200
yarn build:prod         # Production build (output to target/generated-resources/public)
yarn lint               # ESLint
```

The frontend Maven module uses `frontend-maven-plugin` to invoke Yarn during the Maven build. When running `mvn install`, the plugin downloads Node.js and runs `yarn install` + `yarn build:prod`.

### Skip Individual Packaging Types

| Flag | Effect |
|---|---|
| `-Dpkg.skip=true` | Skip ALL packaging (bootjar + deb + rpm + zip). Convenience alias. |
| `-Dpkg.skip.bootjar=true` | Skip Spring Boot repackage (the `-boot.jar`). Also prevents .deb since buildDeb depends on the boot JAR. |
| `-Dpkg.skip.deb=true` | Skip Gradle `buildDeb` task. No .deb artifact produced. |
| `-Dpkg.skip.rpm=true` | Skip Gradle `buildRpm` task. No .rpm artifact produced. |
| `-Dpkg.skip.zip=true` | Skip Maven Assembly Plugin Windows ZIP. |

The `skip-pkg` profile is activated automatically when `-Dpkg.skip=true` is set, and it sets all four individual skip flags to `true`.

To build only the Windows ZIP (no .deb, no .rpm):

```bash
mvn clean install -T6 -DskipTests -Dpkg.skip.deb=true -Dpkg.skip.rpm=true -Dpkg.skip.zip=false
# -> application/target/thingsboard-windows.zip
```

## Maven Profiles

### `default` (active by default)

Always active. No special behavior.

### `download-dependencies`

Activates source and javadoc download for all dependencies. Usage:

```bash
mvn package -Pdownload-dependencies -Dclassifier=sources dependency:copy-dependencies
```

Downloads sources and javadocs under `target/dependencies`.

### `skip-deb`

Activated automatically when `-Dpkg.skip.deb=true`. Sets `pkg.deb.phase=none` so the `build-helper-maven-plugin` `attach-artifact` goal does not fail on a missing .deb file.

### `skip-pkg`

Activated automatically when `-Dpkg.skip=true`. Convenience profile that sets all packaging skip flags to `true`, plus `pkg.deb.phase=none` and `pkg.dist.phase=none`. This is a single-toggle to disable all packaging.

### `packaging` (always active)

Activated by the **absence** of the `pkg.packaging.disabled` property, so it is on for every build unless you explicitly pass `-Dpkg.packaging.disabled=true`.

> It is deliberately *not* `activeByDefault`. Maven's `DefaultProfileSelector` drops every `activeByDefault` profile of a POM as soon as any other profile of that **same** POM activates — and `skip-deb` / `skip-pkg` above activate on plain `-D` properties. Under the old `activeByDefault` form, `-Dpkg.skip.deb=true` silently deleted this entire `pluginManagement` block: `maven-assembly-plugin` was left with no executions and no descriptor, so no Windows ZIP was produced regardless of `pkg.skip.zip`, and `spring-boot-maven-plugin` lost its `repackage` execution so no boot JAR was produced either — with no error in either case. (The rule is per-POM: `ui-ngx`'s `activeByDefault` `yarn-build` profile lives in `ui-ngx/pom.xml` and is unaffected by root-POM profile activation.)
>
> Check with `mvn -N help:active-profiles -Dpkg.skip.deb=true` — both `skip-deb` and `packaging` must be listed.

Defines the packaging plugin configuration used by all modules. Contains `pluginManagement` blocks for:
- **`maven-resources-plugin`**: Copies configuration, scripts, control files, and init scripts for Linux/Windows packaging
- **`maven-dependency-plugin`**: Copies the WinSW service wrapper executable for Windows packaging. Bound to `pkg.dist.phase`
- **`maven-jar-plugin`**: Configures manifest entries (Implementation-Title, Implementation-Version)
- **`spring-boot-maven-plugin`**: Builds the bootable JAR (repackage + build-info goals). Skips when `pkg.skip.bootjar=true`
- **`gradle-maven-plugin`**: Invokes the Gradle build inside `packaging/java/` to produce .deb and .rpm packages. Bound to `pkg.dist.phase`; skips individual tasks via `pkg.skip.deb` and `pkg.skip.rpm` (passed to Gradle as `-PskipDeb` / `-PskipRpm`, applied by `onlyIf` in `build.gradle`). Note these only skip the tasks — the Gradle invocation itself still happens unless `pkg.dist.phase=none`
- **`maven-assembly-plugin`**: Produces a Windows ZIP distribution. Skipped via `pkg.skip.zip`
- **`build-helper-maven-plugin`**: Attaches the .deb artifact to the Maven reactor so downstream modules can reference it. Phase is controlled by `pkg.deb.phase`

## Packaging System

### Boot JAR (Spring Boot)

The `spring-boot-maven-plugin` repackages the application JAR into an executable boot JAR containing all dependencies:

```
target/thingsboard-4.4.0-SNAPSHOT-boot.jar
```

The `<classifier>` is set to `boot` to distinguish it from the plain library JAR. The boot JAR is the mandatory input to the Gradle buildDeb task.

### Debian Package (.deb)

Produced by the custom `gradle-maven-plugin` which invokes a Gradle build in `packaging/java/`. The Gradle project uses the `nebula.ospackage` plugin (or similar) to assemble a Debian package.

The Maven plugin invokes Gradle tasks: `build`, `buildDeb`, `buildRpm`.

Key Gradle arguments:
- `-PmainJar=` -- path to the Spring Boot JAR
- `-PpkgName=` -- package name (e.g., `thingsboard`)
- `-PpkgUser=` -- system user (`thingsboard`)
- `-PpkgInstallFolder=` -- installation directory (`/usr/share/thingsboard`)
- `-PskipDeb=` / `-PskipRpm=` -- controls whether each task runs

The resulting .deb is attached to the Maven reactor via `build-helper-maven-plugin:attach-artifact` with classifier `deb` and type `deb`.

### RPM Package (.rpm)

Produced by the same Gradle build. No Maven reactor module depends on the RPM artifact, so `-Dpkg.skip.rpm=true` can be toggled freely.

### Windows ZIP Archive

Produced by `maven-assembly-plugin` using the descriptor at `packaging/java/assembly/windows.xml`, written to `application/target/thingsboard-windows.zip` (the `-windows` suffix is the descriptor's `<id>`). Layout:

```
thingsboard/lib/thingsboard.jar          the boot JAR
thingsboard/thingsboard.exe              WinSW service wrapper
thingsboard/thingsboard.xml              service definition
thingsboard/install.bat|uninstall.bat|upgrade.bat
thingsboard/conf/ data/ install/ logs/
```

Skipped with `-Dpkg.skip.zip=true`.

### Docker Images

Docker image builds are handled by the `msa/` submodules. Each microservice module (e.g., `msa/tb-node`, `msa/transport/mqtt`) uses the `dockerfile-maven-plugin` or a custom Docker build process to produce multi-arch (amd64/arm64) images. The `.deb` artifact from the `application` or transport module is copied into the Docker image as the service payload.

For local Docker builds, set `DOCKER_BUILDKIT=0`.

## Test Execution Strategies

### Global Test Settings

```bash
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max_old_space_size=4096"
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"
```

The Surefire plugin is configured with JVM args for string deduplication and G1 GC pause tuning:

```
-XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200
-XX:+EnableDynamicAgentLoading
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
```

Per-fork RocksDB paths are set via `-Dqueue.edqs.local.rocksdb_path` and `-Dqueue.calculated_fields.rocks_db_path` to ensure isolation between parallel test forks.

### Compile-Then-Test Strategy

Always compile first without tests to avoid classpath issues in multi-module parallel test execution:

```bash
# Step 1: Compile everything (no tests)
mvn clean install -T6 -DskipTests -Dpkg.skip=true

# Step 2: Run tests
mvn test -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui' -T4
```

### Module-Specific Test Commands

```bash
# DAO module (parallel by packages, 4 forks)
mvn test -pl dao -Dparallel=packages -DforkCount=4

# Application controller tests (parallel by classes, 6 forks)
# Excludes NoSQL-specific tests, includes flaky-test retry
mvn test -pl application \
  -Dtest='!**/nosql/**,org.thingsboard.server.controller.**' \
  -DforkCount=6 -Dparallel=classes \
  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5

# Single test class
mvn test -pl application -Dtest=DeviceControllerTest -DfailIfNoTests=false
```

### Test Dependencies

Application tests use **Testcontainers** to spin up Docker containers for:
- PostgreSQL (database)
- Kafka (message queue)
- Optional: Cassandra, Valkey/Redis, ZooKeeper

Docker must be running on the build machine. If Docker API version incompatibility occurs, add `"min-api-version": "1.32"` to `/etc/docker/daemon.json`. If testcontainers cannot locate Docker, remove `~/.testcontainers.properties`.

### Black-Box Tests

Located in `msa/black-box-tests`. These are full integration/E2E tests using:
- **TestNG** as the test framework
- **Allure** for test reporting
- **Testcontainers** for infrastructure
- Docker images built from the same Maven reactor

## Build Plugins

| Plugin | Purpose |
|---|---|
| `maven-compiler-plugin` | Java 25 compilation with Lombok annotation processing |
| `maven-surefire-plugin` | Test execution with parallel forking |
| `maven-jar-plugin` | JAR packaging with manifest entries |
| `spring-boot-maven-plugin` | Bootable JAR packaging (repackage goal) |
| `gradle-maven-plugin` | Invokes Gradle for .deb/.rpm packaging |
| `frontend-maven-plugin` | Downloads Node.js, runs Yarn for Angular build |
| `maven-assembly-plugin` | Windows ZIP distribution |
| `maven-resources-plugin` | Copies config, scripts, and data files during packaging |
| `maven-dependency-plugin` | Copies protoc binary for Protobuf compilation, WinSW for Windows packaging |
| `protobuf-maven-plugin` | Compiles `.proto` files to Java sources (protobuf-java + gRPC) |
| `build-helper-maven-plugin` | Attaches .deb artifact to reactor, adds generated sources |
| `git-commit-id-maven-plugin` | Generates `git.properties` with build metadata |
| `license-maven-plugin` | Enforces Apache 2.0 license headers |
| `maven-enforcer-plugin` | Enforces build environment constraints |
| `os-maven-plugin` | Detects OS/platform for protoc binary selection |

## Key Maven Properties

| Property | Default | Purpose |
|---|---|---|
| `maven.compiler.source` | `25` | Java source version |
| `maven.compiler.target` | `25` | Java target version |
| `spring-boot.version` | `3.5.14` | Spring Boot BOM version |
| `spring-boot-test.version` | `3.5.13` | Pinned test artifact version (workaround for regression) |
| `kafka.version` | `3.9.2` | Apache Kafka client version |
| `protobuf.version` | `3.25.5` | Protobuf version (note: gRPC does not yet support Protobuf 4.x) |
| `grpc.version` | `1.76.0` | gRPC libraries version |
| `netty.version` | `4.1.133.Final` | Netty version (overrides Spring Boot managed version) |
| `cassandra.version` | `4.17.0` | Cassandra driver version |
| `postgresql.version` | `42.7.11` | PostgreSQL JDBC driver |
| `jjwt.version` | `0.12.5` | JWT library |
| `thingsboard.client.version` | `4.4.0` | Java REST client version (decoupled from server) |
| `lombok.version` | `1.18.46` | Lombok (must stay in sync with Spring Boot) |
| `pkg.user` | `thingsboard` | System user for Linux packages |
| `pkg.installFolder` | `/usr/share/thingsboard` | Installation directory for .deb/.rpm |
| `pkg.unixLogFolder` | `/var/log/thingsboard` | Log directory for Linux packages |
| `pkg.skip.bootjar` | `false` | Skip boot JAR repackaging |
| `pkg.skip.deb` | `false` | Skip .deb packaging |
| `pkg.skip.rpm` | `false` | Skip .rpm packaging |
| `pkg.skip.zip` | `false` | Skip Windows ZIP packaging |
| `pkg.deb.phase` | `${pkg.package.phase}` | Internal: phase of the `attach-deb` execution; `none` when the .deb is skipped |
| `pkg.dist.phase` | `${pkg.package.phase}` | Internal: phase of the Gradle invocation and the WinSW download; `none` under `-Dpkg.skip=true` |
| `pkg.packaging.disabled` | (unset) | Set to any value to deactivate the `packaging` profile entirely |

## Key External Dependencies (Version-Pinned)

These dependencies override the versions managed by Spring Boot's BOM, typically for CVE fixes or compatibility:

| Library | Version | Reason |
|---|---|---|
| `netty-bom` | 4.1.133.Final | Fixes CVE-2026-42579, CVE-2026-42583, CVE-2026-42584, CVE-2026-42587 |
| `tomcat-embed-core` | 10.1.55 | Fixes CVE-2026-41284, CVE-2026-43512 |
| `commons-lang3` | 3.18.0 | Fixes CVE-2025-48924 |
| `postgresql` | 42.7.11 | Fixes CVE-2026-42198 |
| `kafka-clients` | 3.9.2 | Fixes CVE-2026-35554 |
| `bouncycastle` | 1.84 | Fixes CVE-2026-5588, CVE-2026-5598 |
| `cassandra-all` | 5.0.7 | Fixes CVE-2026-27314 |
| `opennlp-tools` | 2.5.9 | Fixes CVE-2026-40682, CVE-2026-42027 |
| `nimbus-jose-jwt` | 10.0.2 | Fixes CVE-2023-52428, CVE-2025-53864 |
| `zookeeper` | 3.9.5 | Fixes CVE-2026-24308, CVE-2026-24281 |

## Repositories

| Repository ID | URL | Purpose |
|---|---|---|
| `central` | `https://repo1.maven.org/maven2/` | Maven Central |
| `thingsboard-public-repo` | `https://repo.allshen.org/artifactory/libs-release-public` | ThingsBoard artifacts and custom builds |
| `spring-snapshots` | `https://repo.spring.io/snapshot` | Spring snapshot builds |
| `spring-milestones` | `https://repo.spring.io/milestone` | Spring milestone builds |
| `typesafe` | `https://repo.typesafe.com/typesafe/releases/` | Typesafe/Akka dependencies |
