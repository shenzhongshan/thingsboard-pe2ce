# 发布与升级工具

三个脚本，用来强制执行 [版本管理与系统升级指南](../architecture/04-upgrade-and-release.zh-CN.md)
里的规则。纯 Python 3，无依赖，不需要虚拟环境。三者都强制 UTF-8 输出，不受 Windows 控制台代码页影响。

**本文是权威版。** 英文版 [README.md](README.md) 为参照副本，改动请先落在本文。

| 脚本 | 什么时候跑 | 会动工作树吗 |
|---|---|---|
| `release_upgrade_path_check.py` | 每次发布前，以及 CI 里 | 不会——只读 |
| `release_upgrade_path_check_test.py` | 改过检查脚本之后 | **会**——见下面的警告 |
| `upgrade_doc_conformance.py` | 改过指南或升级相关代码之后 | 不会——只读 |

---

## `release_upgrade_path_check.py`

判定你设的版本号对这批改动是否合法，以及这批改动能否到达每一个数据库。**这是发布门禁。**

```bash
python docs/tools/release_upgrade_path_check.py [仓库根] [基线 ref]

# 典型用法：在仓库根执行，基线取上一个发布
python docs/tools/release_upgrade_path_check.py . v4.4.0.101
```

两个参数都可省。仓库根默认按脚本位置上推两级；基线默认取最近可达的 `v*` tag。
**目前请显式传基线**——本仓库的 tag 全是从上游继承来的（`v2.5`、`v3.x`），默认会解析到
`v2.5` 这种毫无意义的东西。它会把选中的 tag 打印出来，所以你能看见这件事发生。

比对的是基线 vs **工作树**，不是 vs `HEAD`，所以未提交的改动也算在内，"忘了提交"混不过门禁。

**退出码：** `0` 版本号与改动相符 · `1` 至少一条阻塞规则失败 · `2` 参数不可用。

八条阻塞规则及各自的理由见
[指南第 4 节](../architecture/04-upgrade-and-release.zh-CN.md)。读不到自己的输入时它也会失败而不是
通过（基线 `pom.xml` 缺失、`git diff` 失败、认不出 `updateData()` 函数体、白名单解析不了）——
一个无法评估输入的检查绝不能报告成功。

## `release_upgrade_path_check_test.py`

检查脚本的自测：8 个场景，每个构造一种条件并断言检查脚本的退出码与输出信息。

```bash
python docs/tools/release_upgrade_path_check_test.py
```

> **它会改写工作树里的真实文件**——`pom.xml`、`TbRestApiCallNode.java`、
> `DefaultDataUpdateService.java`、两个 `schema_update.sql` 以及
> `DefaultDatabaseSchemaSettingsService.java`——并在 `finally` 块里用内存中的备份还原。
> 工作树有未提交改动时跑它是安全的，但**运行期间不要编辑这些文件**；若它被中途杀掉，
> 继续干活前先看一眼 `git status`。

场景 3 是最要紧的那个：bump `minor` 且改了规则节点，**必须不**失败。没有它，一个被写死成
永远失败的检查脚本和一个正确的检查脚本看起来完全一样。

如果 `HEAD~5` 在你的克隆里不是有效 ref，用 `RELEASE_CHECK_TEST_BASELINE` 覆盖基线。各场景靠
改写工作树自行构造条件，所以基线的具体内容无关紧要——它只需要存在。

## `upgrade_doc_conformance.py`

把指南里每条事实性断言从源码重新算一遍，这样文档就无法悄悄漂移。

```bash
python docs/tools/upgrade_doc_conformance.py
```

53 条断言：内部链接可解析、点名的源文件存在、`productType` 为 `"CE"`、白名单恰好是文档所述的
那几项、降级门禁存在、打包公式逐字符一致、热补丁的四项能力边界成立（只跑 `lts`、不迁移规则
节点、从不调用 `updateData()`、仍然推进 `schema_version`）、离线路径把版本号写在最后、
`schema-entities.sql` 里没有 `ADD COLUMN`、`basic` 的条目全部存在于 `lts`、`lts` 里恰好只有一个
dollar-quoted 块、章节编号连续、检查脚本八条规则的提示信息都还在。

它已经两次抓出指南里的错误数字，否则那些数字会无限期地留在那儿——一次是规则条数，一次是被
`target/` 下构建产物抬高的 `pom.xml` 计数。它失败时，除非代码真的变了，否则该改的是文档。

## `doc_translation_parity.py`

校验 `docs/` 下中英两版的结构对齐，防止翻译版静默过期。

```bash
python docs/tools/doc_translation_parity.py [docs 目录]
```

它不理解内容，只比结构：每篇 `.md` 是否有对应的 `.zh-CN.md`、两版的二级/三级标题数、编号节
序列、表格数、代码块围栏数是否一致、中文版的相对链接是否都能解析、中文版开头是否声明了
权威性。结构对上不保证译文准确，但结构对不上就一定是一边改了另一边没跟。

---

## 新增一条规则

检查脚本和指南必须一起改，而自测是维持两者诚实的那一环：

1. 在 `release_upgrade_path_check.py` 里用 `problems.append(...)` 加上判定条件。
2. 在
   [指南第 4 节](../architecture/04-upgrade-and-release.zh-CN.md) 的阻塞规则表里加一行。
3. 在 `release_upgrade_path_check_test.py` 里加一个能触发它的场景——**并确认仍有既有场景通过**，
   否则你分辨不出真规则和卡死的规则。
4. 在 `upgrade_doc_conformance.py` 里加一条断言，把该规则的提示信息与指南绑起来。

注意检查脚本在扫 SQL 前会先剥掉 `--` 行注释。第一版没剥，结果 `schema-entities.sql` 自己文件头
里那段被注释掉的示例 DDL 把自己报成了违规。
