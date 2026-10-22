# 智能防雷设备批量属性接口改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将智能防雷设备接口从逐台读取 `productCategory` 和 `active` 改为一次批量读取，并让前端直接使用后端返回的设备状态。

**Architecture:** 在 DAO 层增加保留 `entity_id` 的最新服务器属性查询结果类型；控制器一次查询当前用户可见设备，再一次批量读取两个属性并完成分类与在线统计；前端只发起一次设备接口请求，不再逐台请求 `active`。

**Tech Stack:** Spring MVC、ThingsBoard `UserEntitiesService`、PostgreSQL/JPA 属性 DAO、Angular HttpClient/RxJS。

**Spec:** 当前需求：设备接口按 `productCategory=lightning` 过滤，并返回设备具体信息及 `active` 状态；新增代码少用 Lambda/Stream；不得触碰 Maven。

## Global Constraints

- 不运行、不建议运行任何 Maven 命令。
- 新增 Java 逻辑优先使用普通 `for` 循环和具名方法，不使用 Stream 链式处理。
- 保留当前用户租户、客户和实体组授权边界。
- 缺少 `productCategory` 的设备不得进入智能防雷列表。
- 缺少 `active` 时按离线处理，不能让整个接口失败。

### Task 1: 定义批量属性返回结构并添加失败测试

**Files:**
- Create: `common/data/src/main/java/org/thingsboard/server/common/data/kv/EntityAttributeKvEntry.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/attributes/AttributesDao.java`
- Modify: `common/dao-api/src/main/java/org/thingsboard/server/dao/attributes/AttributesService.java`
- Test: `dao/src/test/java/org/thingsboard/server/dao/service/attributes/BaseAttributesServiceTest.java`

**Interfaces:**
- `EntityAttributeKvEntry` 提供 `UUID entityId` 和 `AttributeKvEntry attribute`。
- DAO/Service 提供 `findLatestByEntityIdsAndScopeWithEntityId(TenantId, List<EntityId>, AttributeScope, Collection<String>)`。

- [ ] **Step 1: Write the failing test**

为批量查询增加测试，断言不同设备的同名属性必须分别保留，不能只留下全局最新的一条。

- [ ] **Step 2: Run the test to verify it fails**

只允许使用项目已有的非 Maven 测试方式；若当前环境无法运行 DAO 测试，保留失败测试并记录原因，不执行 Maven。

- [ ] **Step 3: Write minimal implementation**

新增带 `entity_id` 的投影和 DAO/Service 接口转发，返回每个设备每个属性的最新值。

- [ ] **Step 4: Run the test to verify it passes**

执行可用的静态检查或现有 IDE 检查，不运行 Maven。

### Task 2: 修改 PostgreSQL 属性查询为真正批量查询

**Files:**
- Modify: `dao/src/main/java/org/thingsboard/server/dao/sql/attributes/AttributeKvRepository.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/sql/attributes/JpaAttributeDao.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/attributes/BaseAttributesService.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/attributes/CachedAttributesService.java`

**Interfaces:**
- SQL 返回 `(entity_id, attribute_key, value, last_update_ts, version)`。
- 最新值按 `(entity_id, attribute_key)` 去重，而不是当前错误的 `DISTINCT ON (attribute_key)`。

- [ ] **Step 1: Write the failing test**

增加两个设备分别拥有 `productCategory` 的测试，断言批量结果包含两个设备 ID。

- [ ] **Step 2: Run the test to verify it fails**

确认旧查询会丢失设备 ID 或只返回一条全局属性。

- [ ] **Step 3: Write minimal implementation**

使用 PostgreSQL `DISTINCT ON (a.entity_id, a.attribute_key)`，并在投影中显式返回 `a.entity_id`。

- [ ] **Step 4: Run the test to verify it passes**

进行源码级接口一致性检查，不运行 Maven。

### Task 3: 改造智能防雷控制器

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/controller/SmartLightningMonitoringController.java`

**Interfaces:**
- `/api/smart-lightning/monitoring/devices` 一次读取 `productCategory` 和 `active`。
- 返回设备信息、分类和在线状态，缺少 `active` 时返回 `false`。

- [ ] **Step 1: Write the failing test**

增加分类映射测试：只保留值为 `lightning` 的设备，并正确统计 `active=true/false`。

- [ ] **Step 2: Run the test to verify it fails**

确认当前控制器仍会逐台调用 `attributesService.find(...)`。

- [ ] **Step 3: Write minimal implementation**

收集设备 ID，调用新的批量属性方法一次；使用普通 `for` 循环建立 `Map<UUID, Map<String, String>>`，完成过滤和状态计算。

- [ ] **Step 4: Run the test to verify it passes**

做 Java 源码静态检查，不运行 Maven。

### Task 4: 改造前端设备统计

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/monitoring/smart-lightning-monitoring-api.service.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.data.ts`

**Interfaces:**
- 前端一次请求设备接口，直接使用后端 `active` 状态或统计结果。
- 删除逐台 `getEntityAttributes(..., ['active'])` 请求。

- [ ] **Step 1: Write the failing test**

增加数据转换测试：后端返回设备状态列表后，统计总数、在线数、离线数和在线率。

- [ ] **Step 2: Run the test to verify it fails**

确认当前组件仍对每个设备调用一次属性接口。

- [ ] **Step 3: Write minimal implementation**

扩展 `PageData<DeviceInfo>` 响应类型或新增轻量响应类型，组件直接根据后端状态统计。

- [ ] **Step 4: Run the test to verify it passes**

使用 TypeScript 静态转译检查；不运行 Maven。

### Task 5: 复核接口调用和数据边界

**Files:**
- Review: `application/src/main/java/org/thingsboard/server/controller/SmartLightningMonitoringController.java`
- Review: `ui-ngx/src/app/modules/home/pages/smart-lightning/dashboard/smart-lightning-dashboard.component.ts`

- [ ] 确认前端只发起一次设备列表请求。
- [ ] 确认后端不再逐台读取 `productCategory` 或 `active`。
- [ ] 确认客户用户只能看到授权设备。
- [ ] 确认无设备、无分类、无 active 时均能返回合法空结果或离线状态。
