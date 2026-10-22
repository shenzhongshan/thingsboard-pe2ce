# 运维管理 — 任务管理功能设计

- 日期：2026-09-10
- 状态：设计待评审
- 关联：告警处理页（/deviceAlarms）、共享资产树组件（tb-smart-lightning-asset-tree）

## 1. 背景与结论

新增「运维管理」一级模块，首期实现「任务管理」页面：对巡检/维修/消缺/抢修等运维任务进行创建、指派、跟踪与统计。

**结论：新建独立任务表，不动任何原生代码。**

原因：

1. 任务可以不依赖告警独立存在（计划性巡检、保养），原生告警体系没有挂靠点；
2. 任务字段（单号、计划时间、实际时间、签到坐标、执行结果、双角色）与告警字段完全不同；
3. 告警状态流转单向（ack/clear）不可逆，无法承载「待办 → 在办 → 已办」的工单语义；
4. 新增表/接口属于纯增量，后续合并 ThingsBoard 升级零冲突。

## 2. 数据库设计

新增 SQL 文件：`application/src/main/data/sql/maintenance_task.sql`（随自定义 schema 安装执行）。

```sql
CREATE TABLE IF NOT EXISTS maintenance_task (
    id                  uuid PRIMARY KEY,
    tenant_id           uuid NOT NULL,
    customer_id         uuid,
    task_no             varchar(32) NOT NULL UNIQUE,     -- 任务单号，如 2026091001
    title               varchar(255),                    -- 任务标题（可选）
    task_type           varchar(64),                     -- 任务类型：巡检/维修/消缺/抢修
    task_status         varchar(32) NOT NULL,            -- PENDING/PROCESSING/DONE（Java 枚举映射）
    owner_user_id       uuid NOT NULL,                   -- 负责人 = 执行指派动作的登录用户
    executor_user_id    uuid,                            -- 执行人 = 被指派用户（可空：待指派）
    device_id           uuid,                            -- 任务对象（设备，可空）
    alarm_id            uuid,                            -- 来源告警（可空，告警触发的任务才关联）
    plan_start_time     bigint,                          -- 计划开始时间（毫秒）
    plan_end_time       bigint,                          -- 计划结束时间（毫秒）
    actual_start_time   bigint,                          -- 实际开始时间（毫秒）
    actual_end_time     bigint,                          -- 实际结束时间（毫秒）
    execute_result      varchar(255),                    -- 执行结果描述
    checkin_latitude    double precision,                -- 签到纬度
    checkin_longitude   double precision,                -- 签到经度
    created_time        bigint NOT NULL,
    updater_id          uuid,
    update_time         bigint
);

CREATE INDEX IF NOT EXISTS idx_maintenance_task_tenant ON maintenance_task(tenant_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_task_owner ON maintenance_task(tenant_id, owner_user_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_task_executor ON maintenance_task(tenant_id, executor_user_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_task_status ON maintenance_task(tenant_id, task_status);
```

### 2.1 设计要点

| 决策 | 说明 |
| --- | --- |
| `task_status` 用 varchar 而非 PG 原生 ENUM | 与 TB 原生风格一致（alarm.status 也是 varchar）；加状态无需 ALTER TYPE；Java 层用枚举映射 |
| `owner_user_id` = 指派人 | 派发/创建动作的操作者；告警派发即建单，owner 就是执行派发的登录用户；转派只改 executor，owner 不变 |
| `executor_user_id` 可空 | 仅非告警类任务支持先建后派；告警类工单在派发时同步生成，executor = 告警受理人 |
| `alarm_id` 可空 | **告警类与非告警类的唯一区分字段**；告警派发同步生成工单时写入来源告警 ID |
| 用户引用原生 `uuid` | 负责人/执行人直接存 TB 用户 ID，展示姓名/邮箱由接口按用户 ID 批量回填，不复制用户资料 |
| 时间统一 `bigint` 毫秒 | 与 TB 全部时间字段惯例一致；告警类工单的 `created_time` = 派发时间 |

### 2.2 Java 枚举

`common/data/src/main/java/org/thingsboard/server/common/data/maintenance/MaintenanceTaskStatus.java`

```java
public enum MaintenanceTaskStatus {
    PENDING,     // 待办：已创建尚未指派或执行人未开始
    PROCESSING,  // 在办：执行人已开始（写入 actual_start_time）
    DONE         // 已办：执行完成（写入 actual_end_time 与 execute_result）
}
```

非法字符串查询时兜底为 `PENDING`，不抛异常。

## 3. 后端设计

完全沿用 monitoring 包的分层惯例：Controller 薄、service 接口 + impl 子目录、DTO 独立目录、少用 lambda/stream。

```
application/src/main/java/org/thingsboard/server/
├── controller/MaintenanceTaskController.java          # REST 入口，只做参数与权限
├── service/maintenance/
│   ├── dto/
│   │   ├── MaintenanceTaskDTO.java                    # 列表/详情行（含用户姓名等展示字段）
│   │   ├── MaintenanceTaskRequest.java                # 创建/更新入参
│   │   └── MaintenanceTaskStatistics.java             # 状态页签计数（全部/待办/在办/已办）
│   ├── service/
│   │   └── MaintenanceTaskService.java                # 接口
│   ├── service/impl/
│   │   └── MaintenanceTaskServiceImpl.java            # 实现
│   └── util/
│       └── MaintenanceTaskNoGenerator.java            # 任务单号生成（日期 + 序列）
└── dao 层：JpaRepository + JpaExecutorService 惯例（task 子包）
```

### 3.1 REST 接口清单

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/maintenance/tasks` | 分页查询，参数：`pageSize,page,textSearch,taskStatus,taskType,executorId,ownerId,deviceId,startTime,endTime` | TENANT_ADMIN / CUSTOMER_USER |
| GET | `/api/maintenance/tasks/statistics` | 四个状态页签的计数（全部/待办/在办/已办） | 同上 |
| GET | `/api/maintenance/tasks/{id}` | 任务详情 | 同上 |
| POST | `/api/maintenance/tasks/from-alarm/{alarmId}` | **告警派发即建单（原子接口）**：内部调用原生 `AlarmService.assign()` 完成告警受理人分配，并在同一事务中生成工单（owner = 当前登录用户，executor = 被派发的受理人，`alarm_id` = 来源告警）。告警处理页派发弹窗改调此接口 | TENANT_ADMIN / CUSTOMER_USER |
| POST | `/api/maintenance/tasks` | 创建非告警类任务（巡检/保养等）；`owner_user_id` 取当前登录用户 | 同上 |
| PUT | `/api/maintenance/tasks/{id}` | 更新任务基础信息 | 同上 |
| POST | `/api/maintenance/tasks/{id}/assign/{userId}` | 转派执行人（owner 不变，仅更新 executor） | 同上 |
| POST | `/api/maintenance/tasks/{id}/start` | 开始执行：状态 → PROCESSING，写 `actual_start_time` | 同上 |
| POST | `/api/maintenance/tasks/{id}/complete` | 完成任务：状态 → DONE，写 `actual_end_time` + `execute_result` | 同上 |
| DELETE | `/api/maintenance/tasks/{id}` | 删除（仅负责人或租户管理员） | 同上 |

### 3.2 关键规则

1. **告警派发即建单**：告警处理页点「派发」时，前端改调 `POST /api/maintenance/tasks/from-alarm/{alarmId}`；后端在同一事务中完成「原生告警 assign + 工单生成」，不存在独立的"创建"步骤。内部调用原生 `AlarmService` 服务层（复用而非修改原生代码）。
2. **指派（assign）**：`owner_user_id` 保持生成/创建时写入不变；转派仅更新 `executor_user_id`，并重置状态为 `PENDING`。
3. **状态机校验**：`start` 仅允许 PENDING → PROCESSING；`complete` 仅允许 PROCESSING → DONE；非法流转返回 400。
4. **权限过滤**：列表查询一律先经 `UserEntitiesService` 模式获取当前用户可见范围；CUSTOMER_USER 只能看本客户任务；执行人额外可见「派给自己」的任务。
5. **租户隔离**：所有查询带 `tenant_id`；`customer_id` 在生成时按当前用户归属写入。
6. **单号生成**：`yyyyMMdd + 4 位当日序列`（查当日最大号 + 1），并发场景加租户级同步锁。
7. **展示字段回填**：列表 DTO 的负责人/执行人姓名通过 `UserService.findUserByIds` 批量回填（一次查询，避免逐行查库）；原型图"创建人"列展示负责人姓名。

## 4. 前端设计

沿用告警列表页（/deviceAlarms）的结构与工程模式：页面组件 + `.data.ts` 纯函数 + node assert 单测 + 共享资产树。

### 4.1 路由与菜单

```
路由：/maintenance/tasks（任务管理）
未来：/maintenance/records（巡检记录）、/maintenance/defects（缺陷记录）等同结构扩展
菜单：后端菜单系统配置一级目录「运维管理」，子项「任务管理」指向上述路由
权限：TENANT_ADMIN / CUSTOMER_USER
```

### 4.2 页面结构（对照原型）

```
maintenance/
└── tasks/
    ├── maintenance-task-list.component.ts/html/scss
    ├── maintenance-task-list.data.ts          # 行映射/筛选/排序纯函数
    ├── maintenance-task-list.data.spec.ts     # node assert 单测
    └── maintenance-task-dialog.component.ts   # 创建/指派/完成弹窗（按需拆分）
```

- 左侧：`tb-smart-lightning-asset-tree` 共享资产树（设备节点显示）+ 根资产筛选。
- 右侧筛选区：任务类型、计划开始时间区间、执行结果、负责人、执行人、设备名称 + 查询/重置。
- 工具栏：导出 CSV、新增任务（绿色主按钮）、状态页签（全部 N 条 / 待办 / 在办 / 已办，计数走 statistics 接口）。
- 表格列：勾选、任务单号、任务类型、任务对象（设备名）、负责人、执行人、执行结果、签到坐标、计划开始/结束、实际开始/结束、创建人、创建时间、任务状态、操作。
- 状态页签过滤与页签计数均基于 `task_status`；表格行内操作按钮按状态渲染（待办 → 指派/开始；在办 → 完成；已办 → 只读）。

### 4.3 数据层纯函数清单（.data.ts）

| 函数 | 职责 |
| --- | --- |
| `mapMaintenanceTaskRow` | 后端 DTO → 表格行（状态中文、时间格式化、用户名回显） |
| `filterMaintenanceTaskRows` | 页签 + 筛选表单的行过滤 |
| `sortMaintenanceTaskRows` | 待办优先 → 计划开始时间升序 |
| `formatMaintenanceTime` | 时间格式化，空值显示 `--` |
| `buildMaintenanceCsvRows` | CSV 导出行构造 |

## 5. 告警联动（核心行为：派发即建单）

告警类工单没有独立的"创建"步骤——**告警处理页点「派发」的瞬间同步生成工单**：

1. 前端改造：告警派发弹窗（复用 `tb-alarm-assignee`）把原来的 `assignAlarm` 调用改为调 `POST /api/maintenance/tasks/from-alarm/{alarmId}`；
2. 后端原子完成两件事：调用原生 `AlarmService.assign()` 分配受理人（复用服务层，不改原生代码）+ 生成工单（owner = 指派人 A，executor = 受理人 B，`alarm_id` = 来源告警，`created_time` = 派发时间）；
3. 任务列表行根据 `alarm_id` 显示「来源告警」标记，可跳回告警详情；
4. 告警的 ack/clear 仍走原生接口，与工单状态（待办/在办/已办）各自独立，互不强制。

这样告警派发后：告警侧有受理人（原生机制不变），运维管理侧自动出现一条待办任务（owner = A 的任务视图），一次操作两处生效。

## 6. 实施步骤

1. 建表 SQL + Java 枚举（MaintenanceTaskStatus）
2. dao 层实体与 Repository（照 dao 模块现有 JPA 惯例）
3. 后端 DTO/Service/Controller + 单号生成器（含告警派发即建单的原子接口）
4. 后端纯逻辑单测（状态机流转、单号生成、权限过滤、告警建单字段映射）
5. 前端任务管理页面组件 + `.data.ts` 纯函数 + spec
6. 告警派发弹窗改调建单接口（派发即建单）
7. 共享资产树接入与菜单配置（后端菜单系统下发 /maintenance/tasks）
8. IDEA 启动验证（后端重启加载新表，前端热更新）

## 7. 验收标准

- 告警处理页点「派发」后：告警出现受理人 + 运维管理自动生成一条待办工单（owner = 派发人、executor = 受理人），一次操作两处生效；
- 任务 CRUD、指派（含转派）、开始、完成全流程可用，状态机非法流转被拒绝；
- 待办/在办/已办页签计数与列表一致；
- 负责人视角：能看到自己指派的全部任务（含转派后的）；执行人视角：能看到派给自己的任务；
- 客户用户只能看到本客户任务；
- 资产树筛选、CSV 导出、分页与告警列表页交互一致；
- 原生代码零改动（`git status` 中不出现 TB 原生文件的修改）。
