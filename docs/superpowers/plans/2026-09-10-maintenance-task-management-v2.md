# 运维管理 — 任务管理功能设计

- 日期：2026-09-10
- 关联：告警处理页（/deviceAlarms）、共享资产树组件（tb-smart-lightning-asset-tree）

## 1. 背景与结论

新增「运维管理」一级模块，首期实现「任务管理」页面：对巡检/维修/消缺/抢修等运维任务进行创建、指派、跟踪与统计。

**结论：新建独立任务表与工单历史表，不动任何原生代码。**

原因：

1. 任务可以不依赖告警独立存在（计划性巡检、保养），原生告警体系没有挂靠点；
2. 任务字段（单号、计划时间、实际时间、签到坐标、执行结果、双角色）与告警字段完全不同；
3. 告警状态流转单向（ack/clear）不可逆，无法承载「待办 → 在办 → 已办」的工单语义；
4. 新增表/接口属于纯增量，后续合并 ThingsBoard 升级零冲突。

两个核心模型：

- **告警派发即建单**：告警处理页点「派发」的瞬间同步生成工单，一次操作两处生效（告警出现受理人 + 运维管理出现待办任务）；
- **活动双写 + 聚合读**：工单侧的动作在写工单历史表的同时同步写入告警活动流；告警侧的活动靠聚合读进入工单历史视图，两边都能添加内容、两边都能看到全量活动。

## 2. 数据库设计

新增 SQL 文件：`application/src/main/data/sql/maintenance_task.sql`（随自定义 schema 安装执行）。

### 2.1 工单主表 maintenance_task

```sql
CREATE TABLE IF NOT EXISTS maintenance_task (
    id                  uuid NOT NULL,
    created_time        bigint NOT NULL,
    tenant_id           uuid NOT NULL,
    customer_id         uuid,
    task_no             varchar(32) NOT NULL UNIQUE,     -- 任务单号，如 2026091001
    title               varchar(255),                    -- 任务标题（可选）
    task_type           varchar(64),                     -- 任务类型：巡检/维修/消缺/抢修
    task_status         varchar(32) NOT NULL,            -- PENDING/PROCESSING/DONE（Java 枚举映射）
    owner_user_id       uuid NOT NULL,                   -- 负责人 = 指派人（派发动作的操作者）
    executor_user_id    uuid,                            -- 执行人 = 被指派用户（告警类 = 告警受理人）
    device_id           uuid,                            -- 任务对象（设备，可空）
    alarm_id            uuid,                            -- 来源告警（告警类工单必填，非告警类为空）
    plan_start_time     bigint,                          -- 计划开始时间（毫秒）
    plan_end_time       bigint,                          -- 计划结束时间（毫秒）
    actual_start_time   bigint,                          -- 实际开始时间（毫秒）
    actual_end_time     bigint,                          -- 实际结束时间（毫秒）
    execute_result      varchar(255),                    -- 执行结果描述
    checkin_latitude    double precision,                -- 签到纬度
    checkin_longitude   double precision,                -- 签到经度
    updater_id          uuid,
    update_time         bigint
);

CREATE INDEX IF NOT EXISTS idx_maintenance_task_tenant ON maintenance_task(tenant_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_task_owner ON maintenance_task(tenant_id, owner_user_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_task_executor ON maintenance_task(tenant_id, executor_user_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_task_status ON maintenance_task(tenant_id, task_status);
```

### 2.2 主表设计要点

| 决策 | 说明 |
| --- | --- |
| `task_status` 用 varchar 而非 PG 原生 ENUM | 与 TB 原生风格一致（alarm.status 也是 varchar）；加状态无需 ALTER TYPE；Java 层用枚举映射 |
| `owner_user_id` = 指派人 | 派发/创建动作的操作者；告警派发即建单，owner 就是执行派发的登录用户；转派只改 executor，owner 不变 |
| `executor_user_id` 可空 | 仅非告警类任务支持先建后派；告警类工单在派发时同步生成，executor = 告警受理人 |
| `alarm_id` 可空 | **告警类与非告警类的唯一区分字段**；告警派发同步生成工单时写入来源告警 ID |
| 不设 creator_id 字段 | 告警类工单由派发动作同步生成，创建人 = 负责人（同一人），单独存字段冗余；原型图"创建人"列直接展示负责人 |
| 用户引用原生 `uuid` | 负责人/执行人直接存 TB 用户 ID，展示姓名/邮箱由接口按用户 ID 批量回填，不复制用户资料 |
| 时间统一 `bigint` 毫秒 | 与 TB 全部时间字段惯例一致；告警类工单的 `created_time` = 派发时间 |

### 2.3 工单历史表 maintenance_task_history（参照 alarm_comment 表格式）

表结构完全参照原生 `alarm_comment`（id / created_time / 外键 / user_id / type / comment varchar(10000)）：

```sql
CREATE TABLE IF NOT EXISTS maintenance_task_history (
    id           uuid NOT NULL,
    created_time bigint NOT NULL,
    task_id      uuid NOT NULL,
    user_id      uuid,                            -- 操作人（SYSTEM 记录 = 触发动作的用户）
    type         varchar(255) NOT NULL,           -- SYSTEM（系统流转）/ OTHER（人工备注）
    sub_type     varchar(255),                    -- SYSTEM 记录的动作子类型
    comment      varchar(10000),                  -- 活动描述（SYSTEM 由模板生成，OTHER 为用户输入）
    CONSTRAINT fk_maintenance_task_history_task_id
        FOREIGN KEY (task_id) REFERENCES maintenance_task(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_maintenance_task_history_task_id
    ON maintenance_task_history(task_id);
```

与 `alarm_comment` 的两点刻意差异：

| 差异点 | 原因 |
| --- | --- |
| 增加 `sub_type` 列 | 告警的子类型（ASSIGNED_TO_USER 等）只用于生成文案；工单前端需要按动作类型过滤/渲染图标，显式存列更方便 |
| 不做 `PARTITION BY RANGE (created_time)` | alarm_comment 按时间分区是因为告警量巨大；工单量级远小于告警，普通表 + 索引足够，避免分区表带来的主键限制 |

### 2.4 Java 枚举

`common/data/src/main/java/org/thingsboard/server/common/data/maintenance/` 下：

```java
/** 工单状态（对应 maintenance_task.task_status，varchar 存储）。 */
public enum MaintenanceTaskStatus {
    PENDING,     // 待办：已创建尚未指派或执行人未开始
    PROCESSING,  // 在办：执行人已开始（写入 actual_start_time）
    DONE         // 已办：执行完成（写入 actual_end_time 与 execute_result）
}

/** 工单历史类型（对应 maintenance_task_history.type，语义对齐 AlarmCommentType）。 */
public enum MaintenanceTaskHistoryType {
    SYSTEM,   // 系统流转记录：建单/指派/转派/开始/完成时自动写入
    OTHER     // 人工备注：用户在工单页手动添加
}

/** 系统流转的动作子类型（文案模板对齐 AlarmCommentSubType）。 */
public enum MaintenanceTaskHistorySubType {
    CREATED_BY_USER("工单由用户 %s 创建"),
    ASSIGNED_TO_USER("工单由用户 %s 指派给用户 %s"),
    REASSIGNED_TO_USER("工单由用户 %s 转派给用户 %s"),
    STARTED_BY_USER("工单由用户 %s 开始执行"),
    COMPLETED_BY_USER("工单由用户 %s 完成处理，执行结果：%s");
}
```

非法字符串查询时兜底为 `PENDING`，不抛异常。

### 2.5 历史读写模型（双写 + 聚合读）

```
写入（工单侧，我们自己的接口，同一事务双写）：
  建单/指派/转派/开始/完成/工单页加备注
      → 写 maintenance_task_history
      → alarm_id 非空时，同步调原生 TbAlarmCommentService 写一条告警活动

写入（告警侧，原生行为不变）：
  告警页加备注/确认/清除/指派 → 只写 alarm_comment（原生接口零改动）

读取（工单历史视图 = 聚合读）：
  GET /api/maintenance/tasks/{id}/history
      → history 表记录 ∪ 该工单关联告警的 alarm_comment 记录
      → 映射为统一 DTO（带 source 标记 task/alarm），按 createdTime 归并排序分页返回
```

各事件在两张表的落位：

| 事件 | maintenance_task_history | alarm_comment（告警活动流） |
| --- | --- | --- |
| 告警派发（建单） | ✅ CREATED / ASSIGNED_TO_USER | ✅ 原生自动写（指派）+ 我们写一条建单 |
| 转派 | ✅ REASSIGNED_TO_USER | ✅ 双写 |
| 开始执行 | ✅ STARTED_BY_USER | ✅ 双写 |
| 完成 + 执行结果 | ✅ COMPLETED_BY_USER | ✅ 双写 |
| 工单页加备注 | ✅ OTHER | ✅ 双写 |
| 告警页加备注 | ❌（仅聚合读可见） | ✅ 原生 |
| 告警确认/清除 | ❌ | ✅ 原生 |

用户视角：两边都能添加内容、两边都能看到全量活动；存储层各归其位，零原生改动。

## 3. 后端设计

完全沿用 monitoring 包的分层惯例：Controller 薄、service 接口 + impl 子目录、DTO 独立目录、少用 lambda/stream。

```
application/src/main/java/org/thingsboard/server/
├── controller/MaintenanceTaskController.java          # REST 入口，只做参数与权限
├── service/maintenance/
│   ├── dto/
│   │   ├── MaintenanceTaskDTO.java                    # 列表/详情行（含用户姓名等展示字段）
│   │   ├── MaintenanceTaskRequest.java                # 创建/更新入参
│   │   ├── MaintenanceTaskStatistics.java             # 状态页签计数（全部/待办/在办/已办）
│   │   └── MaintenanceTaskHistoryDTO.java             # 历史条目（含 source 标记与用户姓名）
│   ├── service/
│   │   └── MaintenanceTaskService.java                # 接口
│   ├── service/impl/
│   │   └── MaintenanceTaskServiceImpl.java            # 实现（注入原生 TbAlarmCommentService 做双写）
│   └── util/
│       └── MaintenanceTaskNoGenerator.java            # 任务单号生成（日期 + 序列）
└── dao 层：maintenance 子包（task + task_history 两个实体/Repository）
```

### 3.1 REST 接口清单

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/maintenance/tasks` | 分页查询，参数：`pageSize,page,textSearch,taskStatus,taskType,executorId,ownerId,deviceId,startTime,endTime` | TENANT_ADMIN / CUSTOMER_USER |
| GET | `/api/maintenance/tasks/statistics` | 四个状态页签的计数（全部/待办/在办/已办） | 同上 |
| GET | `/api/maintenance/tasks/{id}` | 任务详情 | 同上 |
| POST | `/api/maintenance/tasks/from-alarm/{alarmId}` | **告警派发即建单（原子接口）**：内部调用原生 `AlarmService.assign()` 完成受理人分配，并在同一事务中生成工单 + 写历史记录。告警处理页派发弹窗改调此接口 | TENANT_ADMIN / CUSTOMER_USER |
| POST | `/api/maintenance/tasks` | 创建非告警类任务（巡检/保养等）；`owner_user_id` 取当前登录用户 | 同上 |
| PUT | `/api/maintenance/tasks/{id}` | 更新任务基础信息 | 同上 |
| POST | `/api/maintenance/tasks/{id}/assign/{userId}` | 转派执行人（owner 不变，仅更新 executor），双写历史 | 同上 |
| POST | `/api/maintenance/tasks/{id}/start` | 开始执行：状态 → PROCESSING，写 `actual_start_time`，双写历史 | 同上 |
| POST | `/api/maintenance/tasks/{id}/complete` | 完成任务：状态 → DONE，写 `actual_end_time` + `execute_result`，双写历史 | 同上 |
| GET | `/api/maintenance/tasks/{id}/history` | **聚合历史视图**：工单历史表 ∪ 关联告警的活动流，归并排序分页返回 | 同上 |
| POST | `/api/maintenance/tasks/{id}/history` | 工单页添加人工备注（OTHER），有告警时双写到告警活动流 | 同上 |
| DELETE | `/api/maintenance/tasks/{id}/history/{historyId}` | 删除备注（仅 OTHER 且本人或租户管理员；SYSTEM 记录不可删） | 同上 |
| DELETE | `/api/maintenance/tasks/{id}` | 删除工单（仅负责人或租户管理员；历史表随外键级联删除） | 同上 |

### 3.2 关键规则

1. **告警派发即建单**：告警处理页点「派发」时，前端改调 `POST /api/maintenance/tasks/from-alarm/{alarmId}`；后端在同一事务中完成「原生告警 assign + 工单生成 + 历史记录」，不存在独立的"创建"步骤。内部调用原生 `AlarmService` 服务层（复用而非修改原生代码）。
2. **指派（assign）**：`owner_user_id` 保持生成/创建时写入不变；转派仅更新 `executor_user_id`，并重置状态为 `PENDING`。
3. **状态机校验**：`start` 仅允许 PENDING → PROCESSING；`complete` 仅允许 PROCESSING → DONE；非法流转返回 400。
4. **活动双写**：工单侧所有动作在写 `maintenance_task_history` 的同时，若 `alarm_id` 非空则调用原生 `TbAlarmCommentService` 写告警活动（SYSTEM 类型，文案用 SubType 模板）。双写在同一事务，告警活动写失败则整体回滚。
5. **聚合读**：`GET .../history` 返回 `history` 表记录与关联告警 `alarm_comment` 记录的并集，统一 DTO 带 `source` 标记（task/alarm），按 `createdTime` 归并分页。告警页添加的内容无需回写历史表。
6. **权限过滤**：列表与历史查询一律先经 `UserEntitiesService` 模式获取当前用户可见范围；CUSTOMER_USER 只能看本客户任务；执行人额外可见「派给自己」的任务。
7. **租户隔离**：所有查询带 `tenant_id`；`customer_id` 在生成时按当前用户归属写入。
8. **单号生成**：`yyyyMMdd + 4 位当日序列`（查当日最大号 + 1），并发场景加租户级同步锁。
9. **展示字段回填**：列表与历史的负责人/执行人/操作人姓名通过 `UserService.findUserByIds` 批量回填（一次查询，避免逐行查库）；原型图"创建人"列展示负责人姓名。

## 4. 告警类工单完整流程（示例）

以管理员 A 处理设备 DEA2-001 的告警、派给运维人员 B 为例：

```
10:00  设备触发「漏电流越限」告警（ACTIVE_UNACK，无受理人）—— 此时无工单

10:05  A 在告警处理页点「派发」选 B（调 from-alarm 原子接口）
       ├ 告警：受理人 = B，活动流自动 +1（"由 A 指派给 B"）
       ├ 工单：生成 T-2026091001（owner=A, executor=B, 状态=待办）
       └ 历史：+1（SYSTEM / ASSIGNED_TO_USER）

10:10  C 在告警页加备注"已电话联系现场"
       └ 告警活动 +1（OTHER）——仅告警活动表，聚合读可见

13:00  B 在工单页点「开始执行」
       ├ 工单：待办 → 在办，写 actual_start_time
       └ 双写：历史 +1（STARTED_BY_USER）＋ 告警活动 +1

13:20  A 在工单详情加备注"注意断电操作"
       └ 双写：历史 +1（OTHER）＋ 告警活动 +1

15:30  B 在工单页点「完成」，填执行结果"更换SPD模块，测试正常"
       ├ 工单：在办 → 已办，写 actual_end_time + execute_result
       └ 双写：历史 +1（COMPLETED_BY_USER）＋ 告警活动 +1

15:35  A 在告警页点「清除」→ 告警闭环（原生行为，与工单状态互不干涉）
```

最终视角：

| 视角 | 看到的内容 |
| --- | --- |
| 告警页 | 受理人 B、已清除；活动流完整（指派/备注/开始/完成/清除全在） |
| 任务列表（A） | 已办页签下 T-2026091001，负责人 A、执行人 B、执行结果完整 |
| 工单详情历史（聚合读） | 工单历史 ∪ 告警活动，按时间归并的完整时间线，一条不漏 |

## 5. 前端设计

沿用告警列表页（/deviceAlarms）的结构与工程模式：页面组件 + `.data.ts` 纯函数 + node assert 单测 + 共享资产树。

```
maintenance/
└── tasks/
    ├── maintenance-task-list.component.ts/html/scss
    ├── maintenance-task-list.data.ts          # 行映射/筛选/排序纯函数
    ├── maintenance-task-list.data.spec.ts     # node assert 单测
    └── maintenance-task-dialog.component.ts   # 创建/指派/完成弹窗（按需拆分）
```

- 左侧：`tb-smart-lightning-asset-tree` 共享资产树（显示设备节点）+ 根资产筛选。
- 右侧筛选区：任务类型、计划开始时间区间、执行结果、负责人、执行人、设备名称 + 查询/重置。
- 工具栏：导出 CSV、新增任务（绿色主按钮）、状态页签（全部 N 条 / 待办 / 在办 / 已办，计数走 statistics 接口）。
- 表格列：勾选、任务单号、任务类型、任务对象（设备名）、负责人、执行人、执行结果、签到坐标、计划开始/结束、实际开始/结束、创建人（展示负责人）、创建时间、任务状态、操作。
- 行内操作按钮按状态渲染：待办 → 指派/开始；在办 → 完成；已办 → 只读。
- 工单详情/历史时间线：每条记录带 source 标记（工单/告警小图标），交互对齐告警活动流。

## 6. 实施步骤

1. 建表 SQL（两张表）+ Java 枚举（Status / HistoryType / HistorySubType）
2. dao 层实体与 Repository（maintenance 子包，task + task_history）
3. 后端 DTO/Service/Controller + 单号生成器 + 历史双写与聚合读
4. 后端纯逻辑单测（状态机流转、单号生成、权限过滤、双写字段映射）
5. 前端任务管理页面组件 + `.data.ts` 纯函数 + spec
6. 告警派发弹窗改调 from-alarm 建单接口
7. 共享资产树接入与菜单配置（后端菜单系统下发 /maintenance/tasks）
8. IDEA 启动验证（后端重启加载新表，前端热更新）

## 7. 验收标准

- 告警处理页点「派发」后：告警出现受理人 + 运维管理自动生成待办工单（owner = 派发人、executor = 受理人），一次操作两处生效；
- 告警活动流与工单历史双向可见：工单页的动作出现在告警活动流，告警页的备注出现在工单聚合历史；
- 任务 CRUD、指派（含转派）、开始、完成全流程可用，状态机非法流转被拒绝；
- 待办/在办/已办页签计数与列表一致；
- 负责人视角能看到自己指派的全部任务（含转派后），执行人视角能看到派给自己的任务；
- 客户用户只能看到本客户任务；
- 资产树筛选、CSV 导出、分页与告警列表页交互一致；
- 原生代码零改动（`git status` 中不出现 TB 原生文件的修改）。
