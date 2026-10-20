# 版本管理与系统升级指南

本 fork 如何编号发布、每次发布走哪条升级路径、每条路径各能做什么和不能做什么。
**改版本号或加数据库变更之前先读这篇。**

英文版：[04-upgrade-and-release.md](04-upgrade-and-release.md)。两份内容等价，改一份请同步另一份；
`docs/tools/upgrade_doc_conformance.py` 校验的是英文版。

下面所有结论都是对本仓库代码实测得出的，不沿用上游文档的说法。凡是容易弄错的地方都点出了源文件，
便于你复核而不是只能信这一页。schema 的原始机制（表清单、版本打包格式、时序表布局）见
[SQL Schema & Migration](20-dao/sql-schema.md)。

---

## 1. 定位

本项目是以 CE 为基础、移植了大部分 PE 功能的 fork。两条推论贯穿全文：

- **版本演化保持上游 CE 骨架。** 没有自建迁移链，没有自定义 `product` 标记，
  `ProjectInfo.getProductType()` 返回 `"CE"`。
- **不存在 CE→PE 转换路径。** `upgradeDatabase()` 没有 `fromCe` 参数，PE 那套实体组补建机制在这里
  一个都没有。**不要移植它。**

## 2. 版本号

四段式 `major.minor.maintenance.patch`，以单个 bigint 存入 `tb_schema_settings.schema_version`：

```
major * 1_000_000_000 + minor * 1_000_000 + maintenance * 1_000 + patch
```

所以 `4.4.0.101` 存成 `4004000101`。4.2.1.1 之前的库用的是三段式老编码
（`major * 1e6 + minor * 1e3 + maintenance`），至今仍被解码——见
`DefaultDatabaseSchemaSettingsService.getDbSchemaVersion()`。

**版本号单调且是承重的。** 把基线调小会让迁移区间 `(DB版本, 包版本]` 变成空集，于是迁移被静默跳过
而不是报错。降级有显式拒绝（第 5 节）。

## 3. 两条升级路径

| | 热补丁 | 离线升级 |
|---|---|---|
| 入口 | `SystemPatchApplier`（`@PostConstruct`，后台线程） | `ThingsboardInstallService`，经 `upgrade.sh` / `upgrade.bat` |
| 触发 | 普通启动时，包版本在同一 `major.minor` 内变大 | 运维显式执行，独立 JVM，跑完退出 |
| 停机 | 不需要——换 jar 重启即可 | 需要——必须先停服务 |
| 集群安全 | `pg_try_advisory_lock`，只有一个节点执行 | 运维跑一次 |

`SystemPatchApplier.isVersionIncreased()` 的条件是 `major` 与 `minor` 不变、**且 `maintenance` 或
`patch` 变大**。陷阱就在这里：**bump `maintenance` 并不会强制走离线升级，只有 bump `minor` 才会。**

### 每条路径实际能做什么

实测，非推测。`DefaultDataUpdateService` 和 `DefaultCacheCleanupService` 都带
`@Profile("install")`，所以这两个 bean 在运行中的服务里**根本不存在**；`updateData()` 和
`upgradeRuleNodes()` 各自只有一个调用点，都在 `ThingsboardInstallService` 里。

| 能力 | 热补丁 | 离线升级 |
|---|---|---|
| 建表 / 建索引 | 能，经 `upgrade/lts/schema_update.sql` | 能 |
| 改列、执行 DML | 能，同一文件 | 能 |
| 重建视图（`schema-views.sql`） | 能 | 能 |
| 系统 widget type / bundle / 图片 | 能（缺失的 widget type 还会新建） | 能 |
| 系统邮件模板 | 能 | 能 |
| **规则节点配置迁移**（`TbNode.upgrade()`） | **不能** | 能 |
| **Java 层数据迁移**（`updateData()`） | **不能** | 能 |
| 清缓存 | 不能 | 能 |
| 推进 `schema_version` | **会** | 会 |

最后两行合在一起就是最该记住的失败形态：

> 一次改了 `@RuleNode(version)` 但只 bump 第四段的发布，会被热补丁接走。热补丁**做不到**规则节点
> 配置迁移，**却照样推进** `schema_version`。于是随后的离线升级被以 *"already upgraded"* 拒绝，
> 规则节点永久停在旧的 `configurationVersion`，**全程不打任何错误日志**。

## 4. 发布判定规则

| 这次发布包含 | 版本怎么 bump | 升级方式 |
|---|---|---|
| 只有幂等 DDL、视图、widget、系统资源、DML | 第四段——`4.4.0.N` | 换 jar 重启 |
| 任何 `@RuleNode(version)` 变化，或 `updateData()` 里有内容 | **`minor`**——`4.5.0.0` | `upgrade.sh`，且要把前一个版本族加进白名单 |

不要凭记忆判断，跑检查脚本：

```bash
python docs/tools/release_upgrade_path_check.py [仓库根] [基线 ref]
```

它比对的是基线 vs **工作树**（含未提交改动，所以"忘了提交"混不过去）。退出码 0 表示你设的版本号
与这批改动相符。八条阻塞规则：

| # | 规则 | 为什么阻塞 |
|---|---|---|
| 1 | 版本号必须变化 | 否则两条升级路径都不触发 |
| 2 | 版本号不得变小 | 迁移会反向执行 |
| 3 | 改了 `@RuleNode(version)` ⇒ 必须 bump `minor` | 热补丁做不到规则节点迁移（第 3 节） |
| 4 | `updateData()` 非空 ⇒ 必须 bump `minor` | 热补丁根本不调用它 |
| 5 | `schema-entities.sql` 里不得有 `ADD COLUMN` | 新装时是 no-op，热补丁又到不了 |
| 6 | `basic` 的每个列变更都要在 `lts` 里 | 否则换 jar 升级的库会缺列 |
| 7 | `basic` 与 `lts` 的声明必须逐字相同 | 先跑的那个定型，另一个被静默作废 |
| 8 | bump `minor` 时前一版本族必须在白名单内 | 否则 `validateSchemaSettings()` 会拒绝升级 |

读不到基线 `pom.xml`、`git diff` 失败、认不出 `updateData()` 函数体、或解析不了
`SUPPORTED_VERSIONS_FOR_UPGRADE` 时，它也会**响亮失败而不是静默通过**——一个无法评估输入的检查
绝不能报告成功。

两个参数都可省：仓库根默认按脚本位置上推两级，基线默认取最近的 `v*` tag。本仓库目前还没有自己的
发布 tag，所以**暂时请显式传基线**。

脚本位于 [`docs/tools/`](../tools/README.md)，同目录还有它的自测和 `upgrade_doc_conformance.py`
（后者会把本页每条事实性断言回算一遍）。调用方式、退出码、以及"自测会改写工作树文件"这个警告见
[docs/tools/README.md](../tools/README.md)。

## 5. 升级门禁

`DefaultDatabaseSchemaSettingsService.validateSchemaSettings()`，按顺序：

1. `SKIP_SCHEMA_VERSION_CHECK=true` 跳过以下全部。
2. 库里的 `product` 必须等于 `ProjectInfo.getProductType()`（即 `"CE"`）。
3. 库版本不能已经等于包版本——否则报 *"already upgraded"*。
4. **库版本不得大于包版本**——降级被拒绝。
5. 库版本的前缀必须出现在 `SUPPORTED_VERSIONS_FOR_UPGRADE` 中。

```java
private static final Map<String, String> SUPPORTED_VERSIONS_FOR_UPGRADE = Map.of(
        "4.3.0", "4.3.0.x",
        "4.3.1", "4.3.1.x",
        "4.4.0", "4.4.0.x"   // 同族条目，理由见下
);
```

key 是 `major.minor.maintenance` 前缀，用 `startsWith` 匹配四段式的库版本。

**为什么要列当前版本族。** 没有 `4.4.0` 这一条，**同族两个补丁版本之间的离线升级就不可能**——而那
恰好是"某次发布事后发现需要规则节点迁移"时唯一的补救手段。**为什么需要第 4 道门禁。** 正是这一条
会让 `4.4.0.5 -> 4.4.0.2` 通过其余所有检查并反向执行迁移；这个类里再没有别的地方比较版本大小。

**发布清单项：** 第一次把 `minor` bump 到 `4.5.0.0` 时，要把 `"4.4.0"` 作为前驱加进白名单。检查
脚本的规则 8 会拦住缺失的前驱。

## 6. 数据库变更该写在哪

有三个文件可以装 DDL，而它们**到达范围不同**：

| 文件 | 全新安装 | 离线升级 | 热补丁 |
|---|---|---|---|
| `dao/src/main/resources/sql/schema-entities.sql` | 会 | 会 | **不会** |
| `application/src/main/data/upgrade/basic/schema_update.sql` | 不会 | 会 | **不会** |
| `application/src/main/data/upgrade/lts/schema_update.sql` | 不会 | 会 | 会 |

### 约定

**每个列恰好声明两次，各管一条路径：**

1. 写进 `schema-entities.sql` 里对应的 `CREATE TABLE` 块——服务全新安装。
2. 写进 `upgrade/lts/schema_update.sql`——服务存量库，**两条**升级路径都能到。

**不要**往 `schema-entities.sql` 尾部追加 `ALTER TABLE x ADD COLUMN IF NOT EXISTS y`。新装时它是
no-op（`CREATE TABLE` 已经建好了列），对存量库又到不了热补丁，两头落空。检查脚本会拒绝这种写法。

**不要**往 `upgrade/basic` 加新的列变更。它只在离线升级跑，而 `lts` 在那条路径上也会跑。`basic` 里
现存的 21 条重复条目是**刻意保留**的：那是上游文件，删行会在每次合并上游时制造冲突，而重复本身无害
——`lts` 在同一条路径上、在它之后执行。检查脚本强制两者文本一致，因为真正会咬人的失败形态是两者
**不一致**：那时先跑的定型，后跑的被自己的 `IF NOT EXISTS` 静默作废。

`lts` 里的一切必须**幂等**——同族内每次版本变大它都会重跑。用 `IF NOT EXISTS`、`ADD` 前先
`DROP ... IF EXISTS`、DML 带 `WHERE` 守卫，避免冲掉运维的改动。

### 两条无法遵守该约定的语句

两者都留在 `schema-entities.sql` 里，并在 `lts` 中镜像一份：

- `ota_package` 的 `fk_device_profile_ota_package`。`device_profile` 与 `ota_package` 互相引用，
  所以这个约束只能等两张表都建好之后再加，因此包在 `DO $$ ... IF NOT EXISTS ... $$` 块里。注意它是
  `lts` 里**唯一**的 dollar-quoted 块，而整个文件是一次 `jdbcTemplate.execute()` 执行的——pgJDBC
  绝不能把块内的分号当成语句分隔符。已由一次真实的热补丁运行验证。
- `edge_event.seq_id SET CYCLE`。改的是已有 identity 列的序列属性，不是加列。

## 7. 需要警惕的失败形态

以下全部是**静默的**——不产生任何错误，这正是它们代价高的原因。

| 症状 | 原因 |
|---|---|
| 规则节点卡在旧的 `configurationVersion` | 改了节点 version，却只 bump 了第四段（第 3 节） |
| 某列只在"换 jar 升级过"的库上缺失 | DDL 落在 `schema-entities.sql` 或 `basic`，没落 `lts` |
| 某列在新装库与升级库上类型不同 | `basic` 与 `lts` 的声明漂移了 |
| 新增的邮件模板在存量租户那里永远不出现 | 如果 `updateMailTemplates()` 只接了两条路径中的一条，就会这样 |
| EDQS 对某个新常驻实体类型的查询从空状态作答 | 如果 `DefaultEdqsService` 没有 `syncedTypes` 记账，就会这样 |
| 整个热补丁被跳过 | `applyPatchIfNeeded()` 里的异常被 `init()` 捕获并记录；在启动日志里 grep `Failed to apply system data patch updates`，并确认 `System data patch update completed successfully` 这行存在 |

补丁版本"什么都没干"时，最后一行是第一个该查的地方。

## 8. 验证升级类改动

本轮工作中验证有效的几种手法：

- **用两个临时库证明 DDL 等价。** 把旧版和新版 `schema-entities.sql` 分别应用到两个空库，然后 diff
  完整的 `information_schema.columns` 指纹（表、列、类型、长度、精度、可空性、默认值）。删掉 19 条
  冗余 `ALTER` 就是这样确认安全的：829 列，逐行一致。
- **校验和要在写回之前算，不是之后。** 动数据前先快照每行的 `md5(列)`，改完 diff。在库外重建某个值时，
  先算它的校验和与基线比对，**对上了再写回**。注意 `COPY ... TO STDOUT` 输出的是转义文本，
  **不是可原样回写的快照**。
- **不 bump 版本也能演练热补丁**，用 `SKIP_PATCH_VERSION_CHECK=true`。比完整离线升级窄得多：不重写
  规则节点，不跑 `updateData()`。
- **加负向对照。** 一个只会报错的检查，和一个被写死成永远报错的检查，表现完全一样。检查脚本里那就是
  场景 3（bump minor 且改了规则节点，**必须不**失败）。
- **用变异测试证明用例非空洞。** 故意破坏逻辑，确认失败的恰好是预期的那几个用例。

## 9. 发布线与分支

### 升级机制施加的硬约束

分支模型在这里**不能随便选**。代码的四条性质约束了它：

1. **`schema_version` 全局单调，且降级被拒绝**（第 5 节）。`4.4.0.105` 的库永远无法由 `4.4.0.103`
   的包服务。因此并行发布线必须占据**不相交的版本区间**——不能交错。
2. **同一 `major.minor` 内每次发布都走热补丁**（第 3 节）。所以维护分支只能发热补丁能做的东西：
   不能做规则节点配置迁移，不能做 `updateData()`。需要这两者之一的修复**无法在本线内发布**。
3. **`maintenance` 变大同样是热补丁**（`SystemPatchApplier.isVersionIncreased()`）。所以
   `4.4.0.5 -> 4.4.1.0` **不是**开新线，它看起来就是一次补丁。**只有 `minor` 能标识一条线。**
4. **`lts/schema_update.sql` 是累积式、按族划分的。** 追加式文件在每次跨分支合并时都会冲突，
   所以合并方向必须由约定固定，不能临场决定。

### 模型

```
major . minor . maintenance . patch
        ^^^^^   ^^^^^^^^^^^   ^^^^^
        发布线   冻结为 0      线内唯一的计数器
```

| 分支 | 生命周期 | 用途 |
|---|---|---|
| `mydev-pe` | 长期 | 开发，改动首先落在这里 |
| `master` | 长期 | 集成，`mydev-pe` 合并到此 |
| `release/4.4` | 长期 | 4.4 线——发 `4.4.0.101`、`.102`…… |
| `release/4.5` | 长期 | bump `minor` 时从 `master` 切出 |

每次发布在其发布分支上打 `v<完整版本号>` tag（如 `v4.4.0.101`）。**tag 比分支更重要**：发布分支会随
热修不断前移，只有 tag 能让你精确回到当时发布的内容。本仓库现有 35 个上游 tag、自己的发布 tag
**为零**，所以打 tag 是需要新建立的纪律，不是既有做法。

建议用 `release/<major>.<minor>` 取代现在那个按日期命名的 `arelease-pro-<日期>` 分支：日期说不出它
属于哪条线，而多线并存时那恰好是你真正需要答案的问题。

### 修复传播：永远向前合并

在**最老的受影响线**上改，然后向前合并：

```
release/4.4  ──┐
               ├─→ release/4.5 ──→ master ──→ mydev-pe
热修落在这里 ──┘
```

**绝不要**把同一个修复独立 cherry-pick 进多条线。同一处 `lts/schema_update.sql` 改动被独立
cherry-pick 两次，会产生两份文本不同的追加式文件，永久冲突，并且可能声明漂移——检查脚本的规则 7
正是为捕捉这种情况而存在（见第 4 节）。

### 每条线各自的义务

每个发布分支持有以下三样东西的**独立副本**，它们不可互换：

| 项 | 为什么必须按线独立 |
|---|---|
| `SUPPORTED_VERSIONS_FOR_UPGRADE` | `release/4.5` 必须把 `"4.4.0"` 列为前驱；`release/4.4` 不得列入 `4.5` |
| `upgrade/basic/schema_update.sql` | 在 `release/4.5` 上它是 4.4→4.5 的增量；在 `release/4.4` 上是 4.3→4.4 的 |
| `upgrade/lts/schema_update.sql` | 只在本线内累积 |

从 `master` 切出 `release/4.5` 时：把 `"4.4.0"` 加进白名单，并决定新线的 `basic` 该放什么内容。
前驱条目缺失时检查脚本的规则 8 会失败。

### 客户能做和不能做的迁移

| 迁移 | 可行？ |
|---|---|
| `4.4.0.101 -> 4.4.0.105` | 可以，热补丁——换 jar 重启 |
| `4.4.0.105 -> 4.5.0.0` | 可以，离线升级——4.5 的包必须把 `4.4.0` 列入白名单 |
| `4.5.0.0 -> 4.4.0.106` | **不行**——被降级门禁拒绝，且没有任何受支持的回退路径 |
| `4.4.0.101 -> 4.6.0.0` 跳过 4.5 | 仅当 `release/4.6` 也把 `4.4.0` 列入白名单 |

第三行是升级前必须告知运维的：**把库迁到新线是单向的。** 先备份数据库。

## 10. 定版

版本号**不是集中定义的**，它重复出现在 **77 处**：

- 76 个 `pom.xml`——根 `<version>` 加上每个模块的 `<parent><version>`，后者是 Maven 要求逐字重复的
  （本项目未配置 `${revision}` 间接层）。这是树里全部源码 pom；用 `grep` 还会多命中
  `msa/web-ui/target/` 下的第 77 份，那是构建产物，不是要编辑的文件。
- `ui-ngx/package.json`——目前是 `"4.4.0"`，注意它**不带** pom 里那个 `-SNAPSHOT` 后缀。

构建里既没声明 `versions-maven-plugin` 也没声明 `maven-release-plugin`，所以定版要么手改 77 处，
要么按坐标直接调用插件：

```bash
# 一次改完 76 个 pom；然后手动改 ui-ngx/package.json
mvn -q versions:set -DnewVersion=4.4.0.101 -DgenerateBackupPoms=false
```

### qualifier 不携带版本信息

`ProjectInfo.getProjectVersion()` 会剥掉所有非数字、非点的字符，所以：

| Maven 版本 | 得到的 schema 版本 |
|---|---|
| `4.4.0.102-SNAPSHOT` | `4.4.0.102` |
| `4.4.0-SNAPSHOT` | `4.4.0.0` |
| `4.3.1.1-PE-SNAPSHOT` | `4.3.1.1` |

**因此"去掉 `-SNAPSHOT`"本身不构成一次发布。** 先发 `4.4.0-SNAPSHOT` 再发 `4.4.0`，在数值上是同一个
版本：两条升级路径都不触发，而离线路径会以 *"already upgraded"* 拒绝它。**bump 必须落在数字上。**
由 `ProjectInfoVersionTest` 钉住。

### 发布序列

```
1.  mvn versions:set -DnewVersion=4.4.0.101 -DgenerateBackupPoms=false
2.  把 ui-ngx/package.json 改成 4.4.0.101
3.  python docs/tools/release_upgrade_path_check.py . <上一个发布 tag>
        -> 必须退出 0；若它要求 bump minor，回到第 1 步改用 4.5.0.0
4.  mvn clean install -T6 -DskipTests          （打包参数见 01-build-system.md）
5.  提交，打 tag v4.4.0.101，推送 tag
6.  mvn versions:set -DnewVersion=4.4.0.102-SNAPSHOT -DgenerateBackupPoms=false
7.  改 ui-ngx/package.json，以 "back to snapshot" 提交
```

第 3 步在第 4 步之前，不是之后：检查脚本决定你刚设的号对这批改动是否合法，先构建是浪费。

第 6 步之所以重要，是因为上面那条剥离规则——把 pom 留在已发布的号上，会让下一个开发构建与发布版
无法区分，于是开发者本地库被打上一个从未发布过的版本号。

## 11. 相关文档

- [docs/tools/README.md](../tools/README.md) —— 三个脚本的调用方式、退出码、注意事项
- [SQL Schema & Migration](20-dao/sql-schema.md) —— 表清单、版本打包格式、索引与函数文件
- [Build System](01-build-system.md) —— Maven reactor、打包、`upgrade.sh` 的来源
- [Deployment Modes](02-deployment-modes.md) —— `install` profile 与部署拓扑
