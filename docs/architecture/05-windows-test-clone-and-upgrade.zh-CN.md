# Windows：复制生产版本建立测试环境，再演练升级

适用场景：同一台 Windows 服务器上，生产 ThingsBoard 继续运行；先复制生产程序及数据库建立旧版测试环境，再将测试环境升级到当前代码版本 `4.4.0.1`。

依据：用户提供的《04-upgrade-and-release.zh-CN.md》，并核对本仓库 Windows 服务配置、安装/升级脚本、数据库版本服务和系统补丁实现。本文不包含源码打包命令；开始升级前应准备好新版 Windows 分发包。

**核心顺序：复制旧程序 → 备份并恢复为独立测试库 → 隔离连接与服务 → 验证旧版测试环境 → 备份测试基线 → 部署新版 → 验证升级 → 必要时恢复旧程序和旧测试库。**

> 不能只复制文件夹就启动：复制品仍可能连接生产数据库、EMQX、消息队列、缓存、邮件及第三方接口。本文命令是操作说明，编写本文时没有连接或更改服务器。

## 1. 先确定实际环境信息

以下为示例，生产路径沿用此前提供的路径；其余值需要在服务器核实。

| 项目 | 生产环境 | 测试环境示例 |
|---|---|---|
| 程序目录 | `C:\appdir\Thingsboard` | `C:\appTest\Thingsboard` |
| Windows 服务名 | `thingsboard` | `thingsboard-test` |
| HTTP 端口 | 以实际配置为准，常见 8080 | 18080 |
| 数据库 | 核实实际库名，可能是 `thingsboardpe` | `thingsboard_test` |
| 数据库账号 | 生产账号 | 新建 `tb_test` |
| 缓存 | 生产配置 | 测试先使用 caffeine |
| 消息队列 | 生产配置 | 单体测试先使用 in-memory |
| EMQX | 生产 Broker、账号、主题 | 首次启动禁用集成，后续使用测试 Broker/主题 |

以管理员身份打开 PowerShell，确认生产服务指向哪里：

```powershell
Get-CimInstance Win32_Service -Filter "Name='thingsboard'" |
    Select-Object Name, State, PathName
Get-Content 'C:\appdir\Thingsboard\thingsboard.xml'
```

查看生产 `conf\thingsboard.yml` 的数据库、监听端口、队列、缓存配置，以及 `thingsboard.xml` 中的 `<env>`、JVM 参数和绝对路径。密码只在服务器本地核对，不要贴进聊天或写进发布记录。

同时检查系统环境变量是否覆盖配置：

```powershell
Get-ChildItem Env: |
    Where-Object Name -Match '^(SPRING_DATASOURCE|HTTP_|MQTT_|TB_QUEUE_|CACHE_|REDIS_|JAVA_HOME)' |
    Select-Object Name
```

只列变量名；实际值在本机查看。Windows 服务继承的环境可能与当前终端不同，应结合服务 XML 和启动日志判断最终配置。

## 2. 记录生产数据库版本，决定升级路线

使用 PostgreSQL 自带的 psql。下面假设安装于 `C:\Program Files\PostgreSQL\18\bin`，不一致时修改路径；命令会提示输入密码。

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\18\bin'
& "$pgBin\psql.exe" -h localhost -p 5432 -U postgres -d thingsboardpe -W -c "SELECT current_database(), current_user; SELECT schema_version, product FROM tb_schema_settings;"
```

这里必须把 `thingsboardpe` 替换为生产配置中的真实库名。这一步只查询，不修改生产库。

四段式编码为 `major × 1000000000 + minor × 1000000 + maintenance × 1000 + patch`：

| schema_version | 对应版本 |
|---|---|
| 4004000000 | 4.4.0.0 |
| 4004000001 | 4.4.0.1 |

`4.4.0-SNAPSHOT` 的数值版本通常是 `4.4.0.0`；不要用程序文件夹日期推断数据库版本。老编码需要按原文档中的兼容规则确认，不要直接套上表。

### 本次目标 4.4.0.1 的前提

- 若旧库是 `4.4.0.0`，且这批变更没有规则节点配置版本迁移、没有需要执行的 Java `updateData()` 迁移，可以按补丁路径升级。
- 若涉及上述任一种迁移，按原文档要求应改升 minor，例如 `4.5.0.0`，并核对升级白名单；当前 `4.4.0.1` 不能仅凭版本号断言适合。
- 若旧库已经是 `4.4.0.1`，不会再次自动触发版本补丁；若旧库更高，不得拿 `4.4.0.1` 覆盖运行。
- 若旧库不在 4.4 族，先确认受支持的离线升级路线，不能直接套用本次补丁操作。

在源码电脑上，以**生产包对应的真实 Git 提交或标签**为基线进行静态发布检查：

```powershell
python docs/tools/release_upgrade_path_check.py . <生产版本对应的提交或标签>
```

尖括号部分是占位符，必须替换。退出码必须为 0；不知道生产提交时，应先找回发布记录，不能随便用当前 HEAD 代替。本文没有替你完成这个比较，因此路线仍须由该检查和生产版本记录共同确认。

## 3. 复制生产程序，但先不要启动

确认目标目录尚不存在，然后执行：

```powershell
$prodDir = 'C:\appdir\Thingsboard'
$testDir = 'C:\appTest\Thingsboard'
if (Test-Path -LiteralPath $testDir) { throw '测试目录已存在，请先核实用途，不要直接覆盖。' }
Copy-Item -LiteralPath $prodDir -Destination $testDir -Recurse
```

生产运行时日志可能变化；复制程序不等于复制业务数据。后面的 PostgreSQL 逻辑备份才是数据库快照。证书、扩展、外部文件存储也要盘点：测试只能读取副本，不能指向生产的可写共享目录。

不要双击复制出的 `install.bat`，也不要启动复制出的 exe。

## 4. 导出生产库并恢复到测试库

### 4.1 导出生产数据库

PostgreSQL `pg_dump` 支持对运行中的库进行一致性逻辑备份，不要求停止生产 TB，但会增加数据库 I/O；选低峰期，备份文件存入新的独立目录。客户端版本应与服务器兼容，优先使用同一大版本工具。

```powershell
$backupDir = 'C:\appTest\tb-test-backups'
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$prodDump = Join-Path $backupDir ('production-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump')
& "$pgBin\pg_dump.exe" -h localhost -p 5432 -U postgres -W -Fc -f $prodDump thingsboardpe
if ($LASTEXITCODE -ne 0) { throw '生产库导出失败，停止后续操作。' }
& "$pgBin\pg_restore.exe" --list $prodDump | Select-Object -First 20
```

替换真实生产库名。不要复制 PostgreSQL 正在使用的 data 目录，也不要把 SQL 导入生产库。备份含生产数据和凭据，应限制访问。

### 4.2 建独立账号和空测试库

连接管理数据库：

```powershell
& "$pgBin\psql.exe" -h localhost -p 5432 -U postgres -d postgres -W
```

在 psql 中逐条执行：

```sql
CREATE ROLE tb_test LOGIN;
\password tb_test
CREATE DATABASE thingsboard_test OWNER tb_test TEMPLATE template0 ENCODING 'UTF8';
\q
```

`\password` 会交互设置密码。若账号或库已存在，先检查用途；不要删除重建未知的库。测试账号不得授予超级用户、生产表权限或生产角色成员关系。同实例默认 CONNECT 权限不等于业务数据访问权；若需要严格网络及数据库隔离，应使用独立 PostgreSQL 实例。

### 4.3 恢复数据库

```powershell
& "$pgBin\pg_restore.exe" -h localhost -p 5432 -U tb_test -W -d thingsboard_test --no-owner --no-acl --exit-on-error $prodDump
if ($LASTEXITCODE -ne 0) { throw '恢复失败，请排查错误，不要启动测试服务。' }
& "$pgBin\psql.exe" -h localhost -p 5432 -U tb_test -W -d thingsboard_test -c "SELECT current_database(), current_user; SELECT schema_version, product FROM tb_schema_settings;"
```

恢复目标必须是刚建的空测试库。若生产使用扩展，可能需管理员先在测试库安装相应扩展；恢复失败后不要对已部分恢复的库重复执行，先核实后另建空测试库再恢复。不要添加 `--create` 或让恢复过程自动选择生产库名。

恢复后的用户、密码、设备、资产、菜单、规则链及历史记录来自备份；因此登录使用生产快照时已有的 TB 账号，而非重新安装生成的账号。测试库不应再运行 `install.bat` 初始化。

## 5. 第一次启动前完成隔离

### 5.1 暂停复制过来的集成

先查看测试库表结构：

```powershell
& "$pgBin\psql.exe" -h localhost -p 5432 -U tb_test -W -d thingsboard_test
```

在 psql 中确认库名与表结构：

```sql
SELECT current_database();
\d public.integration
```

只有确认当前库是 `thingsboard_test`，且存在 `enabled` 列后，才执行：

```sql
BEGIN;
UPDATE public.integration SET enabled = false WHERE enabled = true;
COMMIT;
SELECT name, enabled FROM public.integration;
\q
```

当前仓库支持此表；生产旧版是否同样支持需现场确认。表不存在时不要照抄 SQL。

**禁用集成并不能隔离所有外部行为。** 生产快照中的规则链、定时任务、报表、邮件、短信、Webhook、RPC、远程集成仍需检查。首次启动前，最好让测试进程位于只能访问测试依赖的网络/账号隔离环境；若同机无法区分生产与测试的出站连接，先在测试库中逐项禁用已知外部动作，未排查清楚前不要启动。不要为了隔离测试去封禁生产共用的 java.exe 或 EMQX。

### 5.2 修改测试配置

编辑 **测试目录**的 `conf\thingsboard.yml`，按原有层级修改现有键，不要重复追加同名 YAML 节点。参考值如下：

```yaml
server:
  address: "127.0.0.1"
  port: "18080"
spring:
  datasource:
    url: "jdbc:postgresql://localhost:5432/thingsboard_test"
    username: "tb_test"
    password: "此处填写测试账号密码"
cache:
  type: "caffeine"
queue:
  type: "in-memory"
```

这是需要合并到现有配置的片段，不能用它覆盖整个文件。`spring` 下其余配置必须保留。in-memory 适合本次单体功能演练，不能代表生产 Kafka 的可靠性和性能；若要演练生产拓扑，应另建独立 Kafka/Redis 等测试实例。

修改测试 `thingsboard.xml`：

```xml
<id>thingsboard-test</id>
<name>ThingsBoard Test</name>
<description>ThingsBoard 独立测试环境</description>
```

在 `<service>` 内添加或修改以下 `<env>`，同名项只留一份。显式覆盖有助于避免继承生产环境变量：

```xml
<env name="SPRING_DATASOURCE_URL" value="jdbc:postgresql://localhost:5432/thingsboard_test" />
<env name="SPRING_DATASOURCE_USERNAME" value="tb_test" />
<env name="HTTP_BIND_ADDRESS" value="127.0.0.1" />
<env name="HTTP_BIND_PORT" value="18080" />
<env name="CACHE_TYPE" value="caffeine" />
<env name="TB_QUEUE_TYPE" value="in-memory" />
<env name="MQTT_ENABLED" value="false" />
<env name="MQTT_SSL_ENABLED" value="false" />
<env name="COAP_ENABLED" value="false" />
<env name="LWM2M_ENABLED" value="false" />
<env name="SNMP_ENABLED" value="false" />
<env name="EDGES_ENABLED" value="false" />
<env name="INTEGRATIONS_RPC_ENABLED" value="false" />
<env name="ZOOKEEPER_ENABLED" value="false" />
<env name="TB_EDQS_SYNC_ENABLED" value="false" />
<env name="TB_EDQS_API_SUPPORTED" value="false" />
<env name="TB_EDQS_ROCKSDB_PATH" value="%BASE%\data\test-edqs" />
<env name="TB_QUEUE_CF_ROCKS_DB_PATH" value="%BASE%\data\test-cf-states" />
```

测试密码可以保存在测试 YAML 中；若机器上存在 `SPRING_DATASOURCE_PASSWORD`，必须在服务 XML 中另设同名 `<env>` 为测试密码，避免它覆盖 YAML。XML 中密码含 `&`、`<`、`"` 时需正确转义。配置文件仅授权给运维与服务账号。

保留工作目录、LOADER_PATH、SQL_DATA_FOLDER、JAR 和日志路径中的 `%BASE%`，将复制后仍指向生产的绝对路径改成测试路径。`<executable>` 建议填写本机真实 JDK 25 的 java.exe 绝对路径。根据机器剩余内存设置测试堆上限，不能挤占生产服务内存。

这些开关禁用的是 TB 自身协议监听，**不会禁止 MQTT 集成主动连接 EMQX**，后者依靠 5.1 处理。后续需要测试直连设备协议时，再单独启用并指定不冲突的测试端口。

初期只绑定 127.0.0.1，在服务器浏览器访问；需要远程访问时再改测试监听地址并只向测试人员开放 18080，不要改生产端口规则。

## 6. 注册并验证旧版测试服务

确认 18080 没被占用，并且服务名不存在：

```powershell
Get-NetTCPConnection -State Listen -LocalPort 18080 -ErrorAction SilentlyContinue
Get-Service -Name thingsboard-test -ErrorAction SilentlyContinue
```

有结果时先排查，不要终止未知进程。没有冲突且隔离已完成后，在管理员 PowerShell 中执行：

```powershell
Set-Location 'C:\appTest\Thingsboard'
.\thingsboard.exe install
if ($LASTEXITCODE -ne 0) { throw '测试服务注册失败。' }
Set-Service -Name thingsboard-test -StartupType Manual
Start-Service -Name thingsboard-test
Get-Service -Name thingsboard,thingsboard-test
```

这里调用的是 **exe install（仅注册 Windows 服务）**，不是 `install.bat`（初始化数据库后注册服务）。复制测试环境不需要再初始化数据库。

检查日志与页面：

```powershell
Get-ChildItem 'C:\appTest\Thingsboard\logs' | Sort-Object LastWriteTime -Descending
Get-Content 'C:\appTest\Thingsboard\logs\thingsboard.log' -Tail 100
Get-NetTCPConnection -State Listen -LocalPort 18080 -ErrorAction SilentlyContinue
```

日志文件名以目录实际生成文件为准。访问 `http://127.0.0.1:18080`，确认：

- 旧版正常登录，原菜单、设备和资产能读取。
- 数据库连接确实是 `thingsboard_test`，生产服务仍正常运行。
- 没有连接生产 EMQX，没有抢占生产 MQTT Client ID，没有发送真实通知或下行控制。
- 测试所需集成改为测试 Broker、测试账号、测试主题、独立 Client ID 后，才逐个启用；不要使用会分流生产消息的共享订阅组。

到这里，才完成“生产旧版本的独立测试副本”。

## 7. 冻结旧版测试基线

停止的只能是测试服务：

```powershell
Stop-Service -Name thingsboard-test
Get-Service -Name thingsboard,thingsboard-test
```

确认测试停止后，备份**已隔离配置的旧程序目录**和测试数据库：

```powershell
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$oldProgramBackup = "C:\appTest\tb-test-backups\old-test-program-$stamp"
Copy-Item -LiteralPath 'C:\appTest\Thingsboard' -Destination $oldProgramBackup -Recurse
$testBaseline = "C:\appTest\tb-test-backups\test-before-upgrade-$stamp.dump"
& "$pgBin\pg_dump.exe" -h localhost -p 5432 -U tb_test -W -Fc -f $testBaseline thingsboard_test
if ($LASTEXITCODE -ne 0) { throw '测试基线备份失败，停止升级。' }
```

记录旧库 schema_version、旧包版本、旧提交、备份路径、新包版本 `4.4.0.1`、目标提交及包文件 SHA-256：

```powershell
Get-FileHash 'C:\appTest\packages\thingsboard-windows-4.4.0.1.zip' -Algorithm SHA256
```

包名是示例，按实际文件名修改。保存发布记录，不要只记录“今天的包”。

## 8. 更新测试程序

将新版 Windows ZIP 解压到独立暂存目录，例如 `C:\appTest\staging\thingsboard-4.4.0.1`，确认其中直接包含 `lib`、`data`、`conf`、`install`、`upgrade.bat` 等。

测试服务保持停止。将暂存目录中以下内容更新到 `C:\appTest\Thingsboard`：

| 内容 | 处理方式 |
|---|---|
| `lib` | 使用新版整套内容；移走旧库文件，避免混装 |
| `data` 下发行包自带资源 | 使用新版，特别是 `data\upgrade`、`data\sql` 和系统资源 |
| `install`、`upgrade.bat` 等发行脚本 | 使用新版 |
| `conf` | 对比合并新增配置，保留测试数据库、端口、日志及密码配置 |
| `thingsboard.xml` | 对比合并新版参数，保留测试服务 id/name、隔离环境变量、JDK 与路径 |
| `extensions` | 按新版本兼容性处理，保留需要的自定义扩展，避免同一扩展多个旧版本并存 |
| 测试运行数据目录 | 保留，不要被发行包覆盖，例如独立 RocksDB、上传文件 |

推荐做法是先在暂存目录完成新版资源与测试配置合并，再在测试服务停止时替换工作目录内容；保留已注册服务使用的 `C:\appTest\Thingsboard\thingsboard.exe` 路径。覆盖前再复查测试 URL 和数据库账号。

**不要只换 JAR 然后遗留旧 `data\upgrade`**：本项目补丁会读取外置 `data\upgrade\lts\schema_update.sql`。原文档的“换 JAR 重启”描述的是升级入口，实际交付时仍须保证该 JAR 配套的 SQL 和系统资源也已更新。

不需要重新运行 `install.bat`，通常也不需要重新注册相同路径、相同服务名的测试服务。

## 9. 执行升级：两条路线只能按变更性质选择

### A. 同族补丁，例如 4.4.0.0 → 4.4.0.1

仅当第 2 节已确认适用时，直接启动新版测试服务：

```powershell
Start-Service -Name thingsboard-test
Get-Content 'C:\appTest\Thingsboard\logs\thingsboard.log' -Tail 150 -Wait
```

观察完按 Ctrl+C 退出日志跟踪。后台补丁可能在 Web 服务启动后才完成，所以不能只看服务为 Running。

在新启动时间段内查找：

```text
Version increased from ... to ... Starting system data update.
System data patch update completed successfully
```

若出现 `Failed to apply system data patch updates`，停止验收并排查；成功日志必须属于本次启动，不能拿旧日志证明。

完成后查询测试库：

```powershell
& "$pgBin\psql.exe" -h localhost -p 5432 -U tb_test -W -d thingsboard_test -c "SELECT current_database(), schema_version, product FROM tb_schema_settings;"
```

本次预期 `current_database = thingsboard_test`、`schema_version = 4004000001`、`product = CE`。CE 是本 fork 的产品标记，不要改成 PE。

### B. 需要规则节点迁移或 Java 数据迁移的离线升级

按原文档先提高 minor 并核对升级白名单、重新准备匹配的新包，再走本路线；不要把当前 `4.4.0.1` 当成已通过迁移审核的包。

1. 保持测试服务停止，禁止先启动新版触发补丁推进版本。
2. 确认新版测试 YAML 中连接测试库，当前终端环境变量也连接测试库。
3. 在测试目录运行新版 `upgrade.bat`。

```powershell
Set-Location 'C:\appTest\Thingsboard'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/thingsboard_test'
$env:SPRING_DATASOURCE_USERNAME = 'tb_test'
# 用本机安全方式设置当前会话的 SPRING_DATASOURCE_PASSWORD 为测试密码，或清除继承的该变量并使用测试 YAML。
# 同时将队列/缓存设为已确认的独立测试配置；XML 中的 env 不会自动应用于 upgrade.bat。
$env:TB_QUEUE_TYPE = 'in-memory'
$env:CACHE_TYPE = 'caffeine'
.\upgrade.bat
if ($LASTEXITCODE -ne 0) { throw '离线升级失败，不要启动测试服务。' }
```

使用具有相同隔离限制的升级会话，并确保 Java 25。脚本读取测试 `conf`；升级 JVM 不会继承服务 XML 的隔离设置，因此环境变量、外部路径也必须单独核对。当前实现从数据库读取旧版本，普通升级无需手填 `--fromVersion`。此结论仅针对已核对的当前仓库脚本。

检查本次升级日志存在 `Upgrade finished successfully!`，并核实数据库版本后，再启动 `thingsboard-test`。若提示 already upgraded，先检查实际库版本与上一次启动记录，不能通过手改 schema_version 或跳过检查强行重跑。

两条路线都不要使用 `SKIP_SCHEMA_VERSION_CHECK` 或 `SKIP_PATCH_VERSION_CHECK` 绕过正式演练检查。

## 10. 验收与记录

- 生产服务持续正常，生产页面及数据接收没有异常。
- 测试服务监听 18080，测试库版本与新包一致。
- 登录、角色权限、菜单、资产树、设备列表、告警展示正常。
- 防雷和电气安全设备详情、指标中文名与单位正常。
- 雷电流波形为 kA、瞬态电流波形为 A；第一个点为 0 μs、相邻点为 2.5 μs。
- 最新波形和历史波形查询正常，旧历史数据仍能读取。
- 测试 EMQX 数据只进入测试库；生产集成没有掉线或被顶替。
- 未发送真实邮件/短信/Webhook，没有触发生产设备控制。
- 日志无补丁失败、数据库缺列、依赖加载失败等异常。

测试首次访问时可使用无痕窗口，避免旧前端缓存；这不能替代后端升级成功检查。记录版本、提交、包哈希、旧/新数据库版本、升级路线、备份文件和验收结果。

## 11. 测试升级失败如何恢复

**回退必须恢复程序和数据库这一对基线，不能只换旧 JAR。** 原文档明确不支持用旧程序服务已升级的高版本库。

1. 停止 `thingsboard-test`，保留失败日志和当前测试库用于分析，生产服务继续运行。
2. 将第 7 节的升级前测试备份恢复到另一个新空库，例如 `thingsboard_test_rollback_20261021`，owner 仍为 `tb_test`。
3. 恢复第 7 节保存的旧测试程序和隔离配置到原测试服务路径。
4. 将旧程序的 YAML、服务 XML 中数据库 URL 全部改成该回退库，防止仍连接已升级库。
5. 查询回退库 schema_version 与旧包匹配后启动测试服务，再验证菜单与数据。

恢复示例（先通过 psql 管理连接建好指定空库）：

```sql
CREATE DATABASE thingsboard_test_rollback_20261021 OWNER tb_test TEMPLATE template0 ENCODING 'UTF8';
```

```powershell
& "$pgBin\pg_restore.exe" -h localhost -p 5432 -U tb_test -W -d thingsboard_test_rollback_20261021 --no-owner --no-acl --exit-on-error $testBaseline
if ($LASTEXITCODE -ne 0) { throw '回退库恢复失败，不要启动旧程序。' }
```

`$testBaseline` 是第 7 节真实备份文件路径；换了终端后需重新赋值。不要把失败后的新库备份当作升级前基线，不要手改 `tb_schema_settings` 来伪装版本回退。

## 12. 对照依据和已知边界

- 主依据：[版本管理与系统升级指南](04-upgrade-and-release.zh-CN.md)。其中源码 POM 数量属于文档编写时快照，不用于判断本次部署是否完整。
- Windows 服务模板：`packaging/java/scripts/windows/service.xml`。
- Windows 初始化与升级入口：`packaging/java/scripts/windows/install.bat`、`upgrade.bat`。
- Windows 包结构：`packaging/java/assembly/windows.xml`。
- 配置键：`application/src/main/resources/thingsboard.yml`。
- 实际升级流程：`ThingsboardInstallService`、`SystemPatchApplier`。
- PostgreSQL 备份/恢复参数：[pg_dump 官方文档](https://www.postgresql.org/docs/18/app-pgdump.html)、[pg_restore 官方文档](https://www.postgresql.org/docs/18/app-pgrestore.html)。

本文静态核对了当前仓库，未连接服务器，未识别生产包真实提交，也未执行数据库恢复、服务启停或升级。开始操作前，必须补齐第 1、2 节中的现场信息；生产可能与当前源码不同，遇到配置或表结构不符时应停在对应步骤核对。
